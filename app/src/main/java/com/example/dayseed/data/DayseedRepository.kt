package com.example.dayseed.data

import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseLockedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.math.BigInteger
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

class DayseedRepository(
    context: Context,
    val clock: DayseedClock = SystemDayseedClock(),
    databaseName: String? = DayseedDatabaseHelper.DATABASE_NAME
) {
    private val appContext = context.applicationContext
    val dbHelper = DayseedDatabaseHelper(
        context = appContext,
        databaseName = databaseName,
        migrationEvaluationDateProvider = { clock.todayLocal() }
    )
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()

    private val _habitVersions = MutableStateFlow<List<HabitVersion>>(emptyList())
    val habitVersions: StateFlow<List<HabitVersion>> = _habitVersions.asStateFlow()

    private val _habitEntries = MutableStateFlow<List<HabitEntry>>(emptyList())
    val habitEntries: StateFlow<List<HabitEntry>> = _habitEntries.asStateFlow()

    private val _habitDayOutcomes = MutableStateFlow<List<HabitDayOutcome>>(emptyList())
    val habitDayOutcomes: StateFlow<List<HabitDayOutcome>> = _habitDayOutcomes.asStateFlow()

    private val _habitDateNotes = MutableStateFlow<List<HabitDateNote>>(emptyList())
    val habitDateNotes: StateFlow<List<HabitDateNote>> = _habitDateNotes.asStateFlow()

    private val _habitDateExceptions = MutableStateFlow<List<HabitDateException>>(emptyList())
    val habitDateExceptions: StateFlow<List<HabitDateException>> = _habitDateExceptions.asStateFlow()

    private val _habitStreakRuns = MutableStateFlow<List<HabitStreakRun>>(emptyList())
    val habitStreakRuns: StateFlow<List<HabitStreakRun>> = _habitStreakRuns.asStateFlow()

    private val _weekStartHistory = MutableStateFlow<List<WeekStartPolicyEntry>>(emptyList())
    val weekStartHistory: StateFlow<List<WeekStartPolicyEntry>> = _weekStartHistory.asStateFlow()

    private val _habitSummaries = MutableStateFlow<Map<String, HabitSummary>>(emptyMap())
    val habitSummaries: StateFlow<Map<String, HabitSummary>> = _habitSummaries.asStateFlow()

    private val _journalEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val journalEntries: StateFlow<List<JournalEntry>> = _journalEntries.asStateFlow()

    private val _journalDrafts = MutableStateFlow<List<JournalDraft>>(emptyList())
    val journalDrafts: StateFlow<List<JournalDraft>> = _journalDrafts.asStateFlow()

    private val _appSettings = MutableStateFlow<Map<String, AppSetting>>(emptyMap())
    val appSettings: StateFlow<Map<String, AppSetting>> = _appSettings.asStateFlow()

    private val _comebackInfo = MutableStateFlow<ComebackInfo?>(null)
    val comebackInfo: StateFlow<ComebackInfo?> = _comebackInfo.asStateFlow()

    private val _growthMetrics = MutableStateFlow(GrowthMetrics())
    val growthMetrics: StateFlow<GrowthMetrics> = _growthMetrics.asStateFlow()

    private val _restoreConflicts = MutableStateFlow<List<RestoreConflict>>(emptyList())
    val restoreConflicts: StateFlow<List<RestoreConflict>> = _restoreConflicts.asStateFlow()

    private val _activeUndoToken = MutableStateFlow<UndoToken?>(null)
    val activeUndoToken: StateFlow<UndoToken?> = _activeUndoToken.asStateFlow()
    private val consumedUndoTokenIds = mutableSetOf<String>()

    fun clearActiveUndoToken() {
        _activeUndoToken.value = null
    }

    init {
        refreshAllSync()
        processCancelOutboxSync()
        evaluateComebackSync()
        try {
            // (Re)align the reminder alarm schedule with current settings/habits (no-op when
            // reminders are not opted in). Repairs scheduling after app start, restores, and
            // settings changes; never throws out of construction.
            ReminderScheduler.scheduleAll(appContext, this)
        } catch (_: Exception) {
            // Scheduling repairs are best-effort; the periodic reconcile worker covers gaps.
        }
    }

    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        refreshAllSync()
    }

    fun refreshAllSync() {
        val db = dbHelper.writableDatabase
        loadAppSettingsSync(db)
        loadWeekStartHistorySync(db)
        loadHabitVersionsSync(db)
        loadHabitsSync(db)
        loadHabitEntriesSync(db)
        loadHabitDayOutcomesSync(db)
        loadHabitDateNotesSync(db)
        loadHabitDateExceptionsSync(db)
        loadJournalEntriesSync(db)
        loadJournalDraftsSync(db)
        loadRestoreConflictsSync(db)
        recomputeAllStreakRunsAndSummariesSync(db)
        computeGrowthMetricsSync()
    }

    fun reloadAllSync() = refreshAllSync()

    fun recomputeAllStreakRunsAndSummariesSync() = refreshAllSync()

    fun loadHabitsSync(): List<Habit> = _habits.value
    fun loadHabitSummariesSync(): Map<String, HabitSummary> = _habitSummaries.value
    fun loadHabitEntriesSync(): List<HabitEntry> = _habitEntries.value
    fun loadStreakRunsSync(): List<HabitStreakRun> = _habitStreakRuns.value
    fun loadJournalEntriesSync(): List<JournalEntry> = _journalEntries.value

    fun processCancelOutboxSync(): Int {
        val db = dbHelper.writableDatabase
        val notifManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return ReminderEngine.processNotificationCancelOutbox(
            db = db,
            nowUtcMs = clock.nowUtcMs(),
            platformCancelCallback = { tag, id ->
                notifManager?.cancel(tag, id)
                true
            }
        )
    }

    /** Ledger status for a delivery key ("DELIVERED", "SUPPRESSED", ...), null when never evaluated. */
    fun getReminderDeliveryStatus(deliveryKey: String): String? {
        val db = dbHelper.writableDatabase
        db.rawQuery(
            "SELECT delivery_status FROM ${DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES} WHERE delivery_key = ?",
            arrayOf(deliveryKey)
        ).use { c ->
            return if (c.moveToFirst()) c.getString(0) else null
        }
    }

    /**
     * Confirms an actual Android notification post: promotes the reservation row to DELIVERED
     * only when `NotificationManager.notify` succeeded, so the ledger never claims delivery
     * just because a decision row was inserted.
     */
    fun markReminderDeliveryDelivered(deliveryKey: String, nowUtcMs: Long) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("delivery_status", "DELIVERED")
            put("delivered_at_utc_ms", nowUtcMs)
            putNull("suppression_reason")
            put("attempted_at_utc_ms", nowUtcMs)
        }
        db.update(
            DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES,
            cv,
            "delivery_key = ?",
            arrayOf(deliveryKey)
        )
    }

    /**
     * Corrects the ledger when a reminder was evaluated as deliverable but the Android
     * notification post itself failed (or a SCHEDULED reservation is abandoned), so delivery
     * records always match reality.
     */
    fun markReminderDeliveryNotDelivered(deliveryKey: String, suppressionReason: String, nowUtcMs: Long) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("delivery_status", "SUPPRESSED")
            putNull("delivered_at_utc_ms")
            put("suppression_reason", suppressionReason)
            put("attempted_at_utc_ms", nowUtcMs)
        }
        db.update(
            DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES,
            cv,
            "delivery_key = ?",
            arrayOf(deliveryKey)
        )
    }

    /**
     * Crash-recovery for incomplete delivery reservations (Phase 1 remediation).
     *
     * Database reservation and `NotificationManager` posting are separate steps, so a process
     * death between them can leave a `SCHEDULED` reservation row that never became `DELIVERED`.
     * This sweep finalises every stale reservation exactly once:
     * - if the reservation is fresh (no older than [maxPendingAgeMs]) the receiver's own
     *   in-flight post is still responsible for it — nothing is done here;
     * - if it is stale, it is re-posted only when it is still fully eligible at recovery time
     *   (global opt-in, Android permission, habit still active with a reminder, same target
     *   date, the D-229 reminder gates still open — the day is still a scheduled opportunity
     *   and the weekly target, if any, is not yet met — user not already logged, outside
     *   quiet hours, cap slot still valid), and the row becomes `DELIVERED` only after the
     *   real post succeeded; a stale reservation whose D-229 gate is now closed is honestly
     *   corrected to `SUPPRESSED`/`NOT_SCHEDULED` or `SUPPRESSED`/`WEEKLY_TARGET_MET`;
     * - otherwise it is honestly corrected to `SUPPRESSED`/`PENDING_EXPIRED` — a stale
     *   reservation must never linger forever and never silently claims delivery.
     */
    fun reconcilePendingReminderPostsSync(maxPendingAgeMs: Long = 30L * 60L * 1000L) {
        val db = dbHelper.writableDatabase
        val rows = mutableListOf<Triple<String, Long, Long>>()
        db.rawQuery(
            "SELECT delivery_key, scheduled_at_utc_ms, attempted_at_utc_ms FROM " +
                DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES +
                " WHERE delivery_status = 'SCHEDULED'",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val key = c.getString(0)
                val scheduledMs = if (c.isNull(1)) 0L else c.getLong(1)
                val attemptedMs = if (c.isNull(2)) 0L else c.getLong(2)
                rows.add(Triple(key, scheduledMs, attemptedMs))
            }
        }
        if (rows.isEmpty()) return

        val nowMs = clock.nowUtcMs()
        val today = clock.todayLocal()
        val timeNow = clock.nowLocalTime().let { String.format("%02d:%02d", it.hour, it.minute) }
        val quietStart = getSettingString("quiet_hours_start", ReminderEngine.DEFAULT_QUIET_START)
        val quietEnd = getSettingString("quiet_hours_end", ReminderEngine.DEFAULT_QUIET_END)

        for ((deliveryKey, scheduledMs, attemptedMs) in rows) {
            val parts = deliveryKey.split(":", limit = 3)
            if (parts.size != 3 || parts[0] != "HABIT_DAILY") {
                markReminderDeliveryNotDelivered(deliveryKey, "PENDING_EXPIRED", nowMs)
                continue
            }
            val habitId = parts[1]
            val targetDate = try { LocalDate.parse(parts[2]) } catch (_: Exception) { null }

            val isFresh = nowMs - maxOf(attemptedMs, scheduledMs) < maxPendingAgeMs
            val eligibleHabit = getEffectiveHabitVersion(habitId, today)
            val alreadyLogged = hasQualifyingActivityOnDate(habitId, today) ||
                getExceptionForHabitOnDate(habitId, today) != null
            val androidAllowsPost = ContextCompat.checkSelfPermission(
                appContext, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val globallyEnabled = ReminderScheduler.isGlobalRemindersEnabled(this)

            // D-229: recompute the reminder gates at recovery time — a stale reservation must
            // never be re-posted on a day that is no longer a scheduled opportunity for the
            // habit (e.g. the schedule was edited after reservation) or after the habit's
            // weekly target became met. Same semantics and helper as the fresh evaluation.
            var scheduledOpportunity = false
            var weeklyTargetMet = false
            if (eligibleHabit != null) {
                val (opportunity, met) = computeScheduledOpportunityAndWeeklyMet(eligibleHabit, today)
                scheduledOpportunity = opportunity
                weeklyTargetMet = met
            }

            val canDeliver = !isFresh &&
                targetDate == today &&
                globallyEnabled &&
                androidAllowsPost &&
                eligibleHabit != null &&
                eligibleHabit.lifecycleState == HabitLifecycleState.ACTIVE &&
                eligibleHabit.reminderMode != "OFF" &&
                scheduledOpportunity &&
                !weeklyTargetMet &&
                !alreadyLogged &&
                !ReminderEngine.isWithinQuietHours(timeNow, quietStart, quietEnd)

            if (canDeliver) {
                val habitName = _habits.value.firstOrNull { it.habitId == habitId }?.name ?: "your habit"
                val posted = ReminderNotifier.postHabitReminder(
                    appContext,
                    ReminderEngine.platformTagFor(habitId),
                    ReminderEngine.platformIdFor(habitId),
                    habitName
                )
                if (posted) {
                    markReminderDeliveryDelivered(deliveryKey, clock.nowUtcMs())
                } else {
                    markReminderDeliveryNotDelivered(deliveryKey, "POST_FAILED", clock.nowUtcMs())
                }
            } else if (!isFresh) {
                // Honest, specific correction for the D-229 gates; everything else keeps the
                // pre-existing stale-outcome label. Suppressed rows never consume the daily cap.
                val reason = when {
                    !scheduledOpportunity -> "NOT_SCHEDULED"
                    weeklyTargetMet -> "WEEKLY_TARGET_MET"
                    else -> "PENDING_EXPIRED"
                }
                markReminderDeliveryNotDelivered(deliveryKey, reason, nowMs)
            }
        }
    }

    // =========================================================================
    // APP SETTINGS, WEEK-START POLICY HISTORY (U-10) & CLOCK UNCERTAINTY
    // =========================================================================

    private fun loadAppSettingsSync(db: SQLiteDatabase) {
        val map = mutableMapOf<String, AppSetting>()
        db.query(DayseedDatabaseHelper.TABLE_APP_SETTINGS, null, null, null, null, null, null).use { c ->
            val kIdx = c.getColumnIndexOrThrow("setting_key")
            val tIdx = c.getColumnIndexOrThrow("value_type")
            val vIdx = c.getColumnIndexOrThrow("value_text")
            val sIdx = c.getColumnIndexOrThrow("schema_version")
            val uIdx = c.getColumnIndexOrThrow("updated_at_utc_ms")
            while (c.moveToNext()) {
                val setting = AppSetting(
                    settingKey = c.getString(kIdx),
                    valueType = c.getString(tIdx),
                    valueText = c.getString(vIdx),
                    schemaVersion = c.getValidatedInt(sIdx, "schema_version"),
                    updatedAtUtcMs = c.getLong(uIdx)
                )
                map[setting.settingKey] = setting
            }
        }
        _appSettings.value = map
    }

    private fun loadWeekStartHistorySync(db: SQLiteDatabase) {
        val list = mutableListOf<WeekStartPolicyEntry>()
        db.query(
            DayseedDatabaseHelper.TABLE_WEEK_START_HISTORY,
            null, null, null, null, null,
            "effective_from_local_date ASC"
        ).use { c ->
            val effIdx = c.getColumnIndexOrThrow("effective_from_local_date")
            val dayIdx = c.getColumnIndexOrThrow("week_start_iso_day")
            val baseIdx = c.getColumnIndexOrThrow("is_legacy_baseline")
            val crIdx = c.getColumnIndexOrThrow("created_at_utc_ms")
            while (c.moveToNext()) {
                val eff = LocalDateValidator.requireValid(c.getString(effIdx), "effective_from_local_date")
                list.add(
                    WeekStartPolicyEntry(
                        effectiveFromLocalDate = eff,
                        weekStartIsoDay = c.getValidatedInt(dayIdx, "week_start_iso_day"),
                        isLegacyBaseline = c.getValidatedInt(baseIdx, "is_legacy_baseline") == 1,
                        createdAtUtcMs = c.getLong(crIdx)
                    )
                )
            }
        }
        _weekStartHistory.value = list
    }

    fun getSettingString(key: String, default: String): String {
        return _appSettings.value[key]?.valueText ?: default
    }

    fun getSettingBoolean(key: String, default: Boolean): Boolean {
        val raw = _appSettings.value[key]?.valueText ?: return default
        return raw.equals("true", ignoreCase = true) || raw == "1"
    }

    fun getSettingInt(key: String, default: Int): Int {
        return _appSettings.value[key]?.valueText?.toIntOrNull() ?: default
    }

    fun getWeekStartDay(forDate: LocalDate = clock.todayLocal()): DayOfWeek {
        val fallback = when (getSettingString("week_start", "MONDAY").uppercase()) {
            "SUNDAY", "7" -> DayOfWeek.SUNDAY
            "SATURDAY", "6" -> DayOfWeek.SATURDAY
            else -> DayOfWeek.MONDAY
        }
        return WeekBoundaryHelper.resolveWeekStartPolicyForDate(
            date = forDate,
            history = _weekStartHistory.value,
            fallbackDay = fallback
        )
    }

    /**
     * Records a week-start preference change under U-10 Option A:
     * Finishes the current 7-day period under the active policy, schedules the new week-start
     * on the first matching ISO weekday strictly after the current week ends, and preserves
     * all historical week boundaries.
     */
    fun setWeekStartSync(newWeekStart: DayOfWeek): WeekStartPolicyEntry {
        val db = dbHelper.writableDatabase
        val nowMs = clock.nowUtcMs()
        val today = clock.todayLocal()
        val currentWeekStart = getWeekStartDay(today)
        var effectiveDateStr = today.toString()

        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("setting_key", "week_start")
                put("value_type", "STRING")
                put("value_text", newWeekStart.name)
                put("schema_version", 1)
                put("updated_at_utc_ms", nowMs)
            }
            db.insertWithOnConflict(
                DayseedDatabaseHelper.TABLE_APP_SETTINGS,
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
            )

            if (newWeekStart != currentWeekStart) {
                val (nextEffDate, _) = WeekBoundaryHelper.computeNextWeekStartEffectiveDate(
                    today = today,
                    currentWeekStart = currentWeekStart,
                    newWeekStart = newWeekStart
                )
                effectiveDateStr = nextEffDate.toString()
                // Remove any future unreached week_start_history entries after today before inserting the new schedule
                db.delete(
                    DayseedDatabaseHelper.TABLE_WEEK_START_HISTORY,
                    "effective_from_local_date > ? AND is_legacy_baseline = 0",
                    arrayOf(today.toString())
                )
                val wCv = ContentValues().apply {
                    put("effective_from_local_date", effectiveDateStr)
                    put("week_start_iso_day", newWeekStart.value)
                    put("is_legacy_baseline", 0)
                    put("created_at_utc_ms", nowMs)
                }
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_WEEK_START_HISTORY,
                    null,
                    wCv,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            } else {
                effectiveDateStr = _weekStartHistory.value.lastOrNull()?.effectiveFromLocalDate ?: today.toString()
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        refreshAllSync()
        return _weekStartHistory.value.find { it.effectiveFromLocalDate == effectiveDateStr }
            ?: WeekStartPolicyEntry(
                effectiveFromLocalDate = effectiveDateStr,
                weekStartIsoDay = newWeekStart.value,
                isLegacyBaseline = false,
                createdAtUtcMs = nowMs
            )
    }

    suspend fun setWeekStart(newWeekStart: DayOfWeek): WeekStartPolicyEntry = withContext(Dispatchers.IO) {
        setWeekStartSync(newWeekStart)
    }

    fun setSettingSync(key: String, valueType: String, valueText: String) {
        if (key == "week_start") {
            val parsedDay = when (valueText.trim().uppercase()) {
                "SUNDAY", "7" -> DayOfWeek.SUNDAY
                "SATURDAY", "6" -> DayOfWeek.SATURDAY
                "TUESDAY", "2" -> DayOfWeek.TUESDAY
                "WEDNESDAY", "3" -> DayOfWeek.WEDNESDAY
                "THURSDAY", "4" -> DayOfWeek.THURSDAY
                "FRIDAY", "5" -> DayOfWeek.FRIDAY
                else -> DayOfWeek.MONDAY
            }
            if (_weekStartHistory.value.all { it.isLegacyBaseline }) {
                val db = dbHelper.writableDatabase
                val nowMs = clock.nowUtcMs()
                db.beginTransaction()
                try {
                    val cv = ContentValues().apply {
                        put("setting_key", "week_start")
                        put("value_type", "ENUM")
                        put("value_text", parsedDay.name)
                        put("schema_version", 1)
                        put("updated_at_utc_ms", nowMs)
                    }
                    db.insertWithOnConflict(
                        DayseedDatabaseHelper.TABLE_APP_SETTINGS,
                        null,
                        cv,
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                    val baselineCv = ContentValues().apply {
                        put("week_start_iso_day", parsedDay.value)
                    }
                    db.update(
                        DayseedDatabaseHelper.TABLE_WEEK_START_HISTORY,
                        baselineCv,
                        "is_legacy_baseline = 1",
                        null
                    )
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
                refreshAllSync()
            } else {
                setWeekStartSync(parsedDay)
            }
            return
        }
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("setting_key", key)
            put("value_type", valueType)
            put("value_text", valueText)
            put("schema_version", 1)
            put("updated_at_utc_ms", clock.nowUtcMs())
        }
        db.insertWithOnConflict(
            DayseedDatabaseHelper.TABLE_APP_SETTINGS,
            null,
            cv,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        loadAppSettingsSync(db)
        computeGrowthMetricsSync()
    }

    suspend fun setSetting(key: String, valueType: String, valueText: String) = withContext(Dispatchers.IO) {
        setSettingSync(key, valueType, valueText)
    }

    fun isClockUncertainSync(): Boolean {
        val lastKnownMs = _appSettings.value["last_observed_utc_ms"]?.valueText?.toLongOrNull() ?: 0L
        return clock.isClockUncertain(lastKnownMs)
    }

    fun recordObservedClockSync() {
        val now = clock.nowUtcMs()
        val lastKnownMs = _appSettings.value["last_observed_utc_ms"]?.valueText?.toLongOrNull() ?: 0L
        if (now >= lastKnownMs) {
            setSettingSync("last_observed_utc_ms", "INT", now.toString())
        }
    }

    // =========================================================================
    // HABITS & EFFECTIVE-DATED HABIT VERSIONS (D-194, D-195, D-216, U-9, U-12)
    // =========================================================================

    private fun loadHabitVersionsSync(db: SQLiteDatabase) {
        val list = mutableListOf<HabitVersion>()
        db.query(
            DayseedDatabaseHelper.TABLE_HABIT_VERSIONS,
            null, null, null, null, null,
            "effective_from_local_date ASC, created_at_utc_ms ASC"
        ).use { c ->
            val vIdIdx = c.getColumnIndexOrThrow("version_id")
            val hIdIdx = c.getColumnIndexOrThrow("habit_id")
            val effIdx = c.getColumnIndexOrThrow("effective_from_local_date")
            val nameIdx = c.getColumnIndexOrThrow("name")
            val descIdx = c.getColumnIndexOrThrow("description")
            val modeIdx = c.getColumnIndexOrThrow("tracking_mode")
            val unitIdx = c.getColumnIndexOrThrow("unit_code")
            val perIdx = c.getColumnIndexOrThrow("target_period")
            val valIdx = c.getColumnIndexOrThrow("target_value")
            val schIdx = c.getColumnIndexOrThrow("schedule_kind")
            val sMaskIdx = c.getColumnIndexOrThrow("scheduled_weekdays_mask")
            val pMaskIdx = c.getColumnIndexOrThrow("preferred_weekdays_mask")
            val lifeIdx = c.getColumnIndexOrThrow("lifecycle_state")
            val remMIdx = c.getColumnIndexOrThrow("reminder_mode")
            val remTIdx = c.getColumnIndexOrThrow("reminder_time_local")
            val tinyIdx = c.getColumnIndex("tiny_description")
            val usualIdx = c.getColumnIndex("usual_description")
            val extraIdx = c.getColumnIndex("extra_description")
            val colorIdx = c.getColumnIndex("color_hex")
            val sortIdx = c.getColumnIndex("sort_order")
            val crIdx = c.getColumnIndexOrThrow("created_at_utc_ms")

            while (c.moveToNext()) {
                val effDate = LocalDateValidator.requireValid(c.getString(effIdx), "effective_from_local_date")
                val rawDesc = if (c.isNull(descIdx)) null else c.getString(descIdx)
                val parsedLegacy = parseHabitMetadataDescription(rawDesc)

                val tiny = if (tinyIdx >= 0 && !c.isNull(tinyIdx)) c.getString(tinyIdx) else parsedLegacy.tiny
                val usual = if (usualIdx >= 0 && !c.isNull(usualIdx)) c.getString(usualIdx) else parsedLegacy.usual
                val extra = if (extraIdx >= 0 && !c.isNull(extraIdx)) c.getString(extraIdx) else parsedLegacy.extra
                val color = if (colorIdx >= 0 && !c.isNull(colorIdx)) c.getString(colorIdx) else parsedLegacy.colorHex
                val sortOrder = if (sortIdx >= 0 && !c.isNull(sortIdx)) c.getValidatedInt(sortIdx, "sort_order") else 0

                list.add(
                    HabitVersion(
                        versionId = c.getString(vIdIdx),
                        habitId = c.getString(hIdIdx),
                        effectiveFromLocalDate = effDate,
                        name = c.getString(nameIdx),
                        description = parsedLegacy.cleanDescription.ifEmpty { null },
                        trackingMode = TrackingMode.fromDb(c.getString(modeIdx)),
                        unitCode = if (c.isNull(unitIdx)) null else c.getString(unitIdx),
                        targetPeriod = TargetPeriod.valueOf(c.getString(perIdx)),
                        targetValue = c.getNullableValidatedInt(valIdx, "target_value"),
                        scheduleKind = ScheduleKind.valueOf(c.getString(schIdx)),
                        scheduledWeekdaysMask = c.getNullableValidatedInt(sMaskIdx, "scheduled_weekdays_mask"),
                        preferredWeekdaysMask = c.getNullableValidatedInt(pMaskIdx, "preferred_weekdays_mask"),
                        lifecycleState = HabitLifecycleState.valueOf(c.getString(lifeIdx)),
                        reminderMode = c.getString(remMIdx),
                        reminderTimeLocal = if (c.isNull(remTIdx)) null else c.getString(remTIdx),
                        tinyDescription = tiny,
                        usualDescription = usual,
                        extraDescription = extra,
                        colorHex = color,
                        sortOrder = sortOrder,
                        createdAtUtcMs = c.getLong(crIdx)
                    )
                )
            }
        }
        _habitVersions.value = list
    }

    private fun loadHabitsSync(db: SQLiteDatabase) {
        val todayStr = clock.todayLocal().toString()
        val versionsByHabit = _habitVersions.value.groupBy { it.habitId }
        val list = mutableListOf<Habit>()

        db.query(
            DayseedDatabaseHelper.TABLE_HABITS,
            null, null, null, null, null,
            "created_at_utc_ms ASC"
        ).use { c ->
            val idIdx = c.getColumnIndexOrThrow("habit_id")
            val crIdx = c.getColumnIndexOrThrow("created_at_utc_ms")
            val upIdx = c.getColumnIndexOrThrow("updated_at_utc_ms")
            val catIdx = c.getColumnIndex("category")
            val goalIdx = c.getColumnIndex("lifetime_goal_days")

            while (c.moveToNext()) {
                val habitId = c.getString(idIdx)
                val createdAt = c.getLong(crIdx)
                val updatedAt = c.getLong(upIdx)
                val category = if (catIdx >= 0 && !c.isNull(catIdx)) c.getString(catIdx) else null
                val lifetimeGoal = if (goalIdx >= 0) c.getNullableValidatedInt(goalIdx, "lifetime_goal_days") else null

                val versions = versionsByHabit[habitId].orEmpty()
                val effective = versions
                    .filter { it.effectiveFromLocalDate <= todayStr }
                    .maxWithOrNull(compareBy<HabitVersion> { it.effectiveFromLocalDate }.thenBy { it.createdAtUtcMs })
                    ?: versions.lastOrNull()

                if (effective != null) {
                    val schedDays = when (effective.scheduleKind) {
                        ScheduleKind.DAILY -> DayOfWeek.values().toSet()
                        ScheduleKind.SELECTED_WEEKDAYS -> WeekdayMask.fromMask(effective.scheduledWeekdaysMask ?: WeekdayMask.ALL_DAYS_MASK)
                        ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> {
                            val prefMask = effective.preferredWeekdaysMask
                            if (prefMask != null && prefMask in 1 until WeekdayMask.ALL_DAYS_MASK) {
                                WeekdayMask.fromMask(prefMask)
                            } else {
                                emptySet()
                            }
                        }
                    }
                    list.add(
                        Habit(
                            habitId = habitId,
                            createdAtUtcMs = createdAt,
                            updatedAtUtcMs = updatedAt,
                            category = category,
                            lifetimeGoalDays = lifetimeGoal,
                            currentVersion = effective,
                            name = effective.name,
                            description = effective.description ?: "",
                            trackingMode = effective.trackingMode,
                            unitCode = effective.unitCode,
                            targetPeriod = effective.targetPeriod,
                            targetValue = effective.targetValue,
                            scheduleKind = effective.scheduleKind,
                            scheduledWeekdaysMask = effective.scheduledWeekdaysMask,
                            preferredWeekdaysMask = effective.preferredWeekdaysMask,
                            lifecycleState = effective.lifecycleState,
                            reminderMode = effective.reminderMode,
                            reminderTimeLocal = effective.reminderTimeLocal,
                            tinyDescription = effective.tinyDescription,
                            usualDescription = effective.usualDescription,
                            extraDescription = effective.extraDescription,
                            scheduleDays = schedDays,
                            colorHex = effective.colorHex,
                            sortOrder = effective.sortOrder
                        )
                    )
                }
            }
        }
        _habits.value = list.sortedWith(
            compareBy<Habit> { it.sortOrder }
                .thenBy { it.createdAtUtcMs }
                .thenBy { it.habitId }
        )
    }

    private data class ParsedHabitMeta(
        val cleanDescription: String,
        val tiny: String,
        val usual: String,
        val extra: String,
        val colorHex: String
    )

    private fun parseHabitMetadataDescription(raw: String?): ParsedHabitMeta {
        if (raw.isNullOrBlank()) {
            return ParsedHabitMeta(
                cleanDescription = "",
                tiny = "Deliberately small action",
                usual = "The ordinary intended action",
                extra = "Optional stretch action",
                colorHex = "#286143"
            )
        }
        if (raw.startsWith("DAYSEED_META:")) {
            return try {
                val obj = JSONObject(raw.removePrefix("DAYSEED_META:"))
                ParsedHabitMeta(
                    cleanDescription = obj.optString("desc", ""),
                    tiny = obj.optString("tiny", "Deliberately small action"),
                    usual = obj.optString("usual", "The ordinary intended action"),
                    extra = obj.optString("extra", "Optional stretch action"),
                    colorHex = obj.optString("color", "#286143")
                )
            } catch (_: Exception) {
                ParsedHabitMeta(raw, "Deliberately small action", "The ordinary intended action", "Optional stretch action", "#286143")
            }
        }
        return ParsedHabitMeta(
            cleanDescription = raw,
            tiny = "Deliberately small action",
            usual = "The ordinary intended action",
            extra = "Optional stretch action",
            colorHex = "#286143"
        )
    }

    /**
     * Resolves the effective HabitVersion for a given habitId on targetDate:
     * version with MAX(effective_from_local_date) <= targetDate.
     */
    fun getEffectiveHabitVersion(habitId: String, targetDate: LocalDate): HabitVersion? {
        val targetStr = targetDate.toString()
        val versions = _habitVersions.value.filter { it.habitId == habitId }
        return versions
            .filter { it.effectiveFromLocalDate <= targetStr }
            .maxWithOrNull(compareBy<HabitVersion> { it.effectiveFromLocalDate }.thenBy { it.createdAtUtcMs })
    }

    /**
     * Resolves the governing [HabitVersion] for [habitId] on [evaluationDate]:
     * - For weekly habits (`targetPeriod == TargetPeriod.WEEK` or `scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET`),
     *   resolves the frozen start-of-week version under U-12 Option B (`StreakAndGroveEngine.resolveFrozenWeekVersion`).
     * - For daily habits, resolves the effective version on [evaluationDate] (`StreakAndGroveEngine.resolveEffectiveVersion`).
     */
    fun getGoverningHabitVersion(habitId: String, evaluationDate: LocalDate): HabitVersion? {
        val versions = _habitVersions.value.filter { it.habitId == habitId }
        if (versions.isEmpty()) return null
        val dateVersion = StreakAndGroveEngine.resolveEffectiveVersion(versions, evaluationDate)
            ?: versions.minWithOrNull(compareBy<HabitVersion> { it.effectiveFromLocalDate }.thenBy { it.createdAtUtcMs })
            ?: return null
        val firstHabitDate = versions
            .mapNotNull { runCatching { LocalDate.parse(it.effectiveFromLocalDate) }.getOrNull() }
            .minOrNull() ?: evaluationDate
        val weekStartIso = WeekBoundaryHelper.resolveWeekStartPolicyForDate(evaluationDate, _weekStartHistory.value)
        val (weekStart, _) = WeekBoundaryHelper.weekBounds(evaluationDate, weekStartIso)
        val anchorDate = if (firstHabitDate.isAfter(weekStart)) firstHabitDate else weekStart
        val weekStartVersion = StreakAndGroveEngine.resolveEffectiveVersion(versions, anchorDate)
            ?: dateVersion
        return if (
            weekStartVersion.targetPeriod == TargetPeriod.WEEK ||
            weekStartVersion.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET ||
            dateVersion.targetPeriod == TargetPeriod.WEEK ||
            dateVersion.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET
        ) {
            StreakAndGroveEngine.resolveFrozenWeekVersion(
                sortedVersions = versions,
                weekStart = weekStart,
                firstHabitDate = firstHabitDate,
                fallbackDate = evaluationDate,
                fallbackVersion = dateVersion
            ) ?: dateVersion
        } else {
            dateVersion
        }
    }

    /**
     * Returns true if the habit has positive qualifying activity on targetDate.
     */
    fun hasQualifyingActivityOnDate(habitId: String, targetDate: LocalDate): Boolean {
        val dStr = targetDate.toString()
        val hasEntry = _habitEntries.value.any { it.habitId == habitId && it.localDate == dStr && it.valueUnits > 0 }
        val hasOutcome = _habitDayOutcomes.value.any { it.habitId == habitId && it.localDate == dStr }
        return hasEntry || hasOutcome
    }

    /**
     * Returns true if the habit has any recorded activity on targetDate for D-194 version lock protection,
     * including explicit zero (`value_units == 0`) or any outcome row.
     * Note: `REST_DAY` exceptions are NOT recorded activity.
     */
    fun hasRecordedActivityForVersionLockOnDate(habitId: String, targetDate: LocalDate): Boolean {
        val dStr = targetDate.toString()
        val hasAnyEntry = _habitEntries.value.any { it.habitId == habitId && it.localDate == dStr }
        val hasOutcome = _habitDayOutcomes.value.any { it.habitId == habitId && it.localDate == dStr }
        return hasAnyEntry || hasOutcome
    }

    /**
     * Returns derived pause ranges for [habitId] from `habit_versions.lifecycle_state` (U-9 Option A).
     */
    fun getDerivedPauseRangesForHabit(habitId: String): List<DerivedPauseRange> {
        val versions = _habitVersions.value.filter { it.habitId == habitId }
        return derivePauseRanges(versions)
    }

    /**
     * Saves a new habit or creates/updates an effective-dated HabitVersion (D-194, D-195, D-216, U-12).
     *
     * Rules enforced inside the SQLite write transaction:
     * 1. New habits must use `TrackingMode.creatableModes` (`QUANTITY`, `DURATION`, `COMPLETION_ONLY`) unless
     *    `allowLegacyEffortModeCreation == true` (for legacy migration/test fixtures).
     * 2. Once recorded activity (including explicit zero `value_units = 0`) exists on `today`,
     *    today's effective evaluation configuration (`trackingMode`, `unitCode`, `targetPeriod`, `targetValue`,
     *    `scheduleKind`, `scheduledWeekdaysMask`, `preferredWeekdaysMask`) is locked:
     *    - If `policy == HabitConfigEffectivePolicy.APPLY_TODAY` or `effectiveDateOverride == today`,
     *      throws [HabitConfigurationLockedTodayException].
     *    - If `applyTodayWhenLogged == false` or `policy == HabitConfigEffectivePolicy.APPLY_TOMORROW`,
     *      creates/updates the version effective `today.plusDays(1)` (or next weekly boundary under U-12).
     * 3. Historical effective-date protection: editing an existing habit cannot backdate an effective version
     *    earlier than existing effective versions or earlier than `today` once versions exist.
     * 4. U-12 Option B: For weekly habits (`targetPeriod == WEEK`), a `unitCode` or `trackingMode` change is
     *    automatically deferred to the next weekly boundary (`currentWeekEnd.plusDays(1)`).
     */
    fun saveHabitSync(
        habit: Habit,
        effectiveDateOverride: LocalDate? = null,
        applyTodayWhenLogged: Boolean = true,
        policy: HabitConfigEffectivePolicy? = null,
        allowLegacyEffortModeCreation: Boolean = false
    ): HabitVersion {
        require(habit.name.trim().length in 1..80) { "Habit name must be between 1 and 80 characters." }
        if (habit.lifetimeGoalDays != null) {
            require(habit.lifetimeGoalDays in 1..36500) { "lifetimeGoalDays must be between 1 and 36500." }
        }

        val db = dbHelper.writableDatabase
        val nowMs = clock.nowUtcMs()
        val today = clock.todayLocal()
        val todayStr = today.toString()

        val normalizedMode = habit.trackingMode.canonical()
        val normalizedScheduleKind = when {
            habit.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET || habit.targetPeriod == TargetPeriod.WEEK ->
                ScheduleKind.FLEXIBLE_WEEKLY_TARGET
            else -> habit.scheduleKind
        }
        val normalizedTargetPeriod = when (normalizedScheduleKind) {
            ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> TargetPeriod.WEEK
            ScheduleKind.DAILY, ScheduleKind.SELECTED_WEEKDAYS -> TargetPeriod.DAY
        }
        val normalizedTargetValue: Int? = when (normalizedMode) {
            TrackingMode.QUANTITY, TrackingMode.DURATION -> {
                val rawTarget = habit.targetValue ?: 1
                require(rawTarget >= 1) { "Target value must be >= 1." }
                rawTarget
            }
            TrackingMode.COMPLETION_ONLY, TrackingMode.EFFORT_AWARE -> {
                if (normalizedTargetPeriod == TargetPeriod.WEEK) {
                    val rawTarget = habit.targetValue ?: 1
                    if (normalizedMode == TrackingMode.COMPLETION_ONLY) {
                        require(rawTarget in 1..7) { "Weekly check-off target must be between 1 and 7 days per week." }
                    } else {
                        require(rawTarget >= 1) { "Weekly target value must be >= 1." }
                    }
                    rawTarget
                } else {
                    null
                }
            }
            else -> null
        }
        val normalizedUnitCode: String? = when (normalizedMode) {
            TrackingMode.DURATION -> habit.unitCode?.takeIf { DurationNormalizer.isSupportedDurationUnit(it) } ?: "min"
            TrackingMode.QUANTITY -> habit.unitCode?.ifBlank { "count" } ?: "count"
            else -> null
        }
        val scheduledMask: Int? = when (normalizedScheduleKind) {
            ScheduleKind.SELECTED_WEEKDAYS -> {
                val m = habit.scheduledWeekdaysMask ?: WeekdayMask.toMask(habit.scheduleDays)
                m.coerceIn(1, WeekdayMask.ALL_DAYS_MASK)
            }
            else -> null
        }
        val preferredMask: Int? = when (normalizedScheduleKind) {
            ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> {
                val m = habit.preferredWeekdaysMask ?: habit.scheduledWeekdaysMask ?: WeekdayMask.toMask(habit.scheduleDays)
                if (m in 1 until WeekdayMask.ALL_DAYS_MASK) m else null
            }
            else -> null
        }
        val remMode = if (habit.reminderMode in listOf("OFF", "AT_TIME")) habit.reminderMode else "OFF"
        val remTime = if (remMode == "AT_TIME") (habit.reminderTimeLocal ?: "09:00") else null
        val cleanDesc = habit.description.trim().ifEmpty { null }
        val tinyDesc = habit.tinyDescription.trim().ifEmpty { "Deliberately small action" }
        val usualDesc = habit.usualDescription.trim().ifEmpty { "The ordinary intended action" }
        val extraDesc = habit.extraDescription.trim().ifEmpty { "Optional stretch action" }
        val colorHex = habit.colorHex.trim().ifEmpty { "#286143" }

        var savedVersion: HabitVersion? = null
        db.beginTransaction()
        try {
            var existingHabitInDb = false
            var existingCreatedAtMs = nowMs
            db.rawQuery(
                "SELECT created_at_utc_ms FROM ${DayseedDatabaseHelper.TABLE_HABITS} WHERE habit_id = ? LIMIT 1",
                arrayOf(habit.habitId)
            ).use { c ->
                if (c.moveToFirst()) {
                    existingHabitInDb = true
                    existingCreatedAtMs = c.getLong(0)
                }
            }

            if (!existingHabitInDb) {
                if (!allowLegacyEffortModeCreation && !normalizedMode.isCreatableForNewHabit()) {
                    throw IllegalArgumentException(
                        "New habits must use QUANTITY, DURATION, or COMPLETION_ONLY tracking mode (EFFORT_AWARE is legacy-only under D-216)."
                    )
                }
            }

            // Query current effective version on today and earliest/latest effective dates inside transaction
            var currentEffectiveToday: HabitVersion? = null
            var earliestEffectiveDateStr: String? = null
            db.rawQuery(
                """
                SELECT version_id, habit_id, effective_from_local_date, name, description,
                       tracking_mode, unit_code, target_period, target_value, schedule_kind,
                       scheduled_weekdays_mask, preferred_weekdays_mask, lifecycle_state,
                       reminder_mode, reminder_time_local, tiny_description, usual_description,
                       extra_description, color_hex, sort_order, created_at_utc_ms
                FROM ${DayseedDatabaseHelper.TABLE_HABIT_VERSIONS}
                WHERE habit_id = ?
                ORDER BY effective_from_local_date ASC, created_at_utc_ms ASC
                """.trimIndent(),
                arrayOf(habit.habitId)
            ).use { c ->
                while (c.moveToNext()) {
                    val v = HabitVersion(
                        versionId = c.getString(0),
                        habitId = c.getString(1),
                        effectiveFromLocalDate = c.getString(2),
                        name = c.getString(3),
                        description = if (c.isNull(4)) null else c.getString(4),
                        trackingMode = TrackingMode.fromDb(c.getString(5)),
                        unitCode = if (c.isNull(6)) null else c.getString(6),
                        targetPeriod = TargetPeriod.valueOf(c.getString(7)),
                        targetValue = c.getNullableValidatedInt(8, "target_value"),
                        scheduleKind = ScheduleKind.valueOf(c.getString(9)),
                        scheduledWeekdaysMask = c.getNullableValidatedInt(10, "scheduled_weekdays_mask"),
                        preferredWeekdaysMask = c.getNullableValidatedInt(11, "preferred_weekdays_mask"),
                        lifecycleState = HabitLifecycleState.valueOf(c.getString(12)),
                        reminderMode = c.getString(13),
                        reminderTimeLocal = if (c.isNull(14)) null else c.getString(14),
                        tinyDescription = c.getString(15),
                        usualDescription = c.getString(16),
                        extraDescription = c.getString(17),
                        colorHex = c.getString(18),
                        sortOrder = c.getValidatedInt(19, "sort_order"),
                        createdAtUtcMs = c.getLong(20)
                    )
                    if (earliestEffectiveDateStr == null || v.effectiveFromLocalDate < earliestEffectiveDateStr!!) {
                        earliestEffectiveDateStr = v.effectiveFromLocalDate
                    }
                    if (v.effectiveFromLocalDate <= todayStr) {
                        currentEffectiveToday = v
                    }
                }
            }

            // Check inside transaction whether recorded activity (including explicit zero) exists on today
            var hasRecordedActivityTodayInDb = false
            db.rawQuery(
                """
                SELECT 1 FROM ${DayseedDatabaseHelper.TABLE_HABIT_ENTRIES}
                WHERE habit_id = ? AND local_date = ?
                UNION ALL
                SELECT 1 FROM ${DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES}
                WHERE habit_id = ? AND local_date = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(habit.habitId, todayStr, habit.habitId, todayStr)
            ).use { c ->
                hasRecordedActivityTodayInDb = c.moveToFirst()
            }

            // Determine whether any evaluation-affecting configuration changed relative to currentEffectiveToday
            val evalConfigChanged = currentEffectiveToday != null && (
                currentEffectiveToday!!.trackingMode.canonical() != normalizedMode ||
                    currentEffectiveToday!!.unitCode != normalizedUnitCode ||
                    currentEffectiveToday!!.targetPeriod != normalizedTargetPeriod ||
                    currentEffectiveToday!!.targetValue != normalizedTargetValue ||
                    currentEffectiveToday!!.scheduleKind != normalizedScheduleKind ||
                    currentEffectiveToday!!.scheduledWeekdaysMask != scheduledMask ||
                    currentEffectiveToday!!.preferredWeekdaysMask != preferredMask
                )

            // Historical effective-date protection (D-194 / T1-08):
            if (existingHabitInDb && effectiveDateOverride != null && effectiveDateOverride.isBefore(today)) {
                throw IllegalArgumentException(
                    "Cannot backdate habit configuration to $effectiveDateOverride on an existing habit (D-194)."
                )
            }

            // U-12 Option B: Check if weekly habit is changing unitCode or trackingMode mid-week,
            // or changing same-unit target mid-week (which takes effect tomorrow while current week freezes its start-of-week target)
            val isWeeklyUnitOrModeChange = existingHabitInDb &&
                currentEffectiveToday != null &&
                currentEffectiveToday!!.targetPeriod == TargetPeriod.WEEK &&
                normalizedTargetPeriod == TargetPeriod.WEEK &&
                (currentEffectiveToday!!.unitCode != normalizedUnitCode ||
                    currentEffectiveToday!!.trackingMode.canonical() != normalizedMode)

            val (currentWeekStart, currentWeekEnd) = WeekBoundaryHelper.weekBounds(today, getWeekStartDay(today))
            val isWeeklySameUnitMidWeekEvalChange = existingHabitInDb &&
                currentEffectiveToday != null &&
                currentEffectiveToday!!.targetPeriod == TargetPeriod.WEEK &&
                normalizedTargetPeriod == TargetPeriod.WEEK &&
                !isWeeklyUnitOrModeChange &&
                currentEffectiveToday!!.targetValue != normalizedTargetValue &&
                today.isAfter(currentWeekStart) &&
                policy == null &&
                effectiveDateOverride == null

            val resolvedEffectiveDate: LocalDate = when {
                !existingHabitInDb -> effectiveDateOverride ?: today
                isWeeklyUnitOrModeChange || policy == HabitConfigEffectivePolicy.DEFER_TO_NEXT_WEEK_BOUNDARY -> {
                    currentWeekEnd.plusDays(1)
                }
                hasRecordedActivityTodayInDb && evalConfigChanged -> {
                    if (policy == HabitConfigEffectivePolicy.APPLY_TODAY ||
                        effectiveDateOverride == today ||
                        (policy == null && applyTodayWhenLogged && effectiveDateOverride == null)
                    ) {
                        throw HabitConfigurationLockedTodayException(habit.habitId, todayStr)
                    }
                    effectiveDateOverride?.takeIf { it.isAfter(today) } ?: today.plusDays(1)
                }
                isWeeklySameUnitMidWeekEvalChange || policy == HabitConfigEffectivePolicy.APPLY_TOMORROW || (!applyTodayWhenLogged && effectiveDateOverride == null) -> {
                    today.plusDays(1)
                }
                effectiveDateOverride != null -> effectiveDateOverride
                else -> today
            }
            val effectiveDateStr = LocalDateValidator.requireValid(resolvedEffectiveDate.toString(), "effective_from_local_date")

            if (!existingHabitInDb) {
                val createdMs = if (habit.createdAtUtcMs in 1..nowMs) habit.createdAtUtcMs else nowMs
                val updatedMs = maxOf(createdMs, nowMs)
                val hCv = ContentValues().apply {
                    put("habit_id", habit.habitId)
                    put("created_at_utc_ms", createdMs)
                    put("updated_at_utc_ms", updatedMs)
                    if (!habit.category.isNullOrBlank()) put("category", habit.category.trim()) else putNull("category")
                    if (habit.lifetimeGoalDays != null) put("lifetime_goal_days", habit.lifetimeGoalDays) else putNull("lifetime_goal_days")
                }
                db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABITS, null, hCv)
            } else {
                val updatedMs = maxOf(existingCreatedAtMs, nowMs)
                val hCv = ContentValues().apply {
                    put("updated_at_utc_ms", updatedMs)
                    if (!habit.category.isNullOrBlank()) put("category", habit.category.trim()) else putNull("category")
                    if (habit.lifetimeGoalDays != null) put("lifetime_goal_days", habit.lifetimeGoalDays) else putNull("lifetime_goal_days")
                }
                db.update(DayseedDatabaseHelper.TABLE_HABITS, hCv, "habit_id = ?", arrayOf(habit.habitId))
            }

            // If applying today and a REST_DAY exception exists on today, clear it if today is no longer an active scheduled day
            if (effectiveDateStr == todayStr) {
                val candidateVersion = HabitVersion(
                    versionId = "temp",
                    habitId = habit.habitId,
                    effectiveFromLocalDate = effectiveDateStr,
                    name = habit.name.trim(),
                    description = cleanDesc,
                    trackingMode = normalizedMode,
                    unitCode = normalizedUnitCode,
                    targetPeriod = normalizedTargetPeriod,
                    targetValue = normalizedTargetValue,
                    scheduleKind = normalizedScheduleKind,
                    scheduledWeekdaysMask = scheduledMask,
                    preferredWeekdaysMask = preferredMask,
                    lifecycleState = habit.lifecycleState,
                    reminderMode = remMode,
                    reminderTimeLocal = remTime,
                    tinyDescription = tinyDesc,
                    usualDescription = usualDesc,
                    extraDescription = extraDesc,
                    colorHex = colorHex,
                    sortOrder = habit.sortOrder,
                    createdAtUtcMs = nowMs
                )
                if (habit.lifecycleState != HabitLifecycleState.ACTIVE ||
                    !StreakAndGroveEngine.isScheduledOpportunityOnDate(candidateVersion, today)
                ) {
                    db.delete(
                        DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS,
                        "habit_id = ? AND local_date = ?",
                        arrayOf(habit.habitId, todayStr)
                    )
                }
            }

            var existingVersionId: String? = null
            db.rawQuery(
                "SELECT version_id FROM ${DayseedDatabaseHelper.TABLE_HABIT_VERSIONS} WHERE habit_id = ? AND effective_from_local_date = ?",
                arrayOf(habit.habitId, effectiveDateStr)
            ).use { c ->
                if (c.moveToFirst()) {
                    existingVersionId = c.getString(0)
                }
            }

            val versionId = existingVersionId ?: UUID.randomUUID().toString()
            val vCv = ContentValues().apply {
                put("version_id", versionId)
                put("habit_id", habit.habitId)
                put("effective_from_local_date", effectiveDateStr)
                put("name", habit.name.trim())
                if (cleanDesc != null) put("description", cleanDesc) else putNull("description")
                put("tracking_mode", normalizedMode.name)
                if (normalizedUnitCode != null) put("unit_code", normalizedUnitCode) else putNull("unit_code")
                put("target_period", normalizedTargetPeriod.name)
                if (normalizedTargetValue != null) put("target_value", normalizedTargetValue) else putNull("target_value")
                put("schedule_kind", normalizedScheduleKind.name)
                if (scheduledMask != null) put("scheduled_weekdays_mask", scheduledMask) else putNull("scheduled_weekdays_mask")
                if (preferredMask != null) put("preferred_weekdays_mask", preferredMask) else putNull("preferred_weekdays_mask")
                put("lifecycle_state", habit.lifecycleState.name)
                put("reminder_mode", remMode)
                if (remTime != null) put("reminder_time_local", remTime) else putNull("reminder_time_local")
                put("tiny_description", tinyDesc)
                put("usual_description", usualDesc)
                put("extra_description", extraDesc)
                put("color_hex", colorHex)
                put("sort_order", habit.sortOrder)
                put("created_at_utc_ms", nowMs)
            }

            if (existingVersionId != null) {
                db.update(
                    DayseedDatabaseHelper.TABLE_HABIT_VERSIONS,
                    vCv,
                    "version_id = ?",
                    arrayOf(versionId)
                )
            } else {
                db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_VERSIONS, null, vCv)
            }

            savedVersion = HabitVersion(
                versionId = versionId,
                habitId = habit.habitId,
                effectiveFromLocalDate = effectiveDateStr,
                name = habit.name.trim(),
                description = cleanDesc,
                trackingMode = normalizedMode,
                unitCode = normalizedUnitCode,
                targetPeriod = normalizedTargetPeriod,
                targetValue = normalizedTargetValue,
                scheduleKind = normalizedScheduleKind,
                scheduledWeekdaysMask = scheduledMask,
                preferredWeekdaysMask = preferredMask,
                lifecycleState = habit.lifecycleState,
                reminderMode = remMode,
                reminderTimeLocal = remTime,
                tinyDescription = tinyDesc,
                usualDescription = usualDesc,
                extraDescription = extraDesc,
                colorHex = colorHex,
                sortOrder = habit.sortOrder,
                createdAtUtcMs = nowMs
            )

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        refreshAllSync()
        rescheduleReminderAlarms()
        return savedVersion!!
    }

    /** Best-effort alarm realignment after any mutation that can change reminder eligibility. */
    private fun rescheduleReminderAlarms() {
        try {
            ReminderScheduler.scheduleAll(appContext, this)
        } catch (_: Exception) {
            // Scheduling repairs are best-effort; the periodic reconcile worker covers gaps.
        }
    }

    suspend fun saveHabit(
        habit: Habit,
        effectiveDateOverride: LocalDate? = null,
        applyTodayWhenLogged: Boolean = true,
        policy: HabitConfigEffectivePolicy? = null
    ): HabitVersion = withContext(Dispatchers.IO) {
        saveHabitSync(habit, effectiveDateOverride, applyTodayWhenLogged, policy)
    }

    fun updateHabitLifecycleSync(
        habitId: String,
        newState: HabitLifecycleState,
        effectiveDate: LocalDate = clock.todayLocal()
    ) {
        val currentHabit = _habits.value.find { it.habitId == habitId } ?: return
        saveHabitSync(
            habit = currentHabit.copy(lifecycleState = newState),
            effectiveDateOverride = effectiveDate,
            applyTodayWhenLogged = true
        )
    }

    suspend fun toggleHabitArchive(habitId: String, archived: Boolean) = withContext(Dispatchers.IO) {
        val state = if (archived) HabitLifecycleState.ARCHIVED else HabitLifecycleState.ACTIVE
        updateHabitLifecycleSync(habitId, state)
    }

    /**
     * Reorders a habit up or down and persists contiguous, duplicate-free `sort_order` (`0..N-1`)
     * across `habit_versions` inside an atomic SQLite transaction (§9.D).
     */
    fun reorderHabitSync(habitId: String, moveUp: Boolean) {
        val currentOrdered = _habits.value.toMutableList()
        val idx = currentOrdered.indexOfFirst { it.habitId == habitId }
        if (idx == -1) return
        val targetIdx = if (moveUp) idx - 1 else idx + 1
        if (targetIdx !in currentOrdered.indices) return

        val swapped = currentOrdered[idx]
        currentOrdered[idx] = currentOrdered[targetIdx]
        currentOrdered[targetIdx] = swapped

        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            currentOrdered.forEachIndexed { newSortOrder, h ->
                val cv = ContentValues().apply {
                    put("sort_order", newSortOrder)
                }
                db.update(
                    DayseedDatabaseHelper.TABLE_HABIT_VERSIONS,
                    cv,
                    "habit_id = ?",
                    arrayOf(h.habitId)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshAllSync()
    }

    suspend fun reorderHabit(habitId: String, moveUp: Boolean) = withContext(Dispatchers.IO) {
        reorderHabitSync(habitId, moveUp)
    }

    // =========================================================================
    // HABIT ENTRIES, OUTCOMES, DATE NOTES, REST_DAY EXCEPTIONS & GUARDED UNDO
    // =========================================================================

    private fun loadHabitEntriesSync(db: SQLiteDatabase) {
        val list = mutableListOf<HabitEntry>()
        db.query(
            DayseedDatabaseHelper.TABLE_HABIT_ENTRIES,
            null, null, null, null, null,
            "local_date DESC, created_at_utc_ms ASC"
        ).use { c ->
            val idIdx = c.getColumnIndexOrThrow("entry_id")
            val hIdx = c.getColumnIndexOrThrow("habit_id")
            val dIdx = c.getColumnIndexOrThrow("local_date")
            val vIdx = c.getColumnIndexOrThrow("value_units")
            val nIdx = c.getColumnIndexOrThrow("entry_note")
            val crIdx = c.getColumnIndexOrThrow("created_at_utc_ms")
            val upIdx = c.getColumnIndexOrThrow("updated_at_utc_ms")
            val kIdx = c.getColumnIndexOrThrow("idempotency_key")

            while (c.moveToNext()) {
                val dateStr = LocalDateValidator.requireValid(c.getString(dIdx), "local_date")
                list.add(
                    HabitEntry(
                        entryId = c.getString(idIdx),
                        habitId = c.getString(hIdx),
                        localDate = dateStr,
                        valueUnits = c.getValidatedInt(vIdx, "value_units"),
                        entryNote = if (c.isNull(nIdx)) null else c.getString(nIdx),
                        createdAtUtcMs = c.getLong(crIdx),
                        updatedAtUtcMs = c.getLong(upIdx),
                        idempotencyKey = c.getString(kIdx)
                    )
                )
            }
        }
        _habitEntries.value = list
    }

    private fun loadHabitDayOutcomesSync(db: SQLiteDatabase) {
        val list = mutableListOf<HabitDayOutcome>()
        db.query(
            DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES,
            null, null, null, null, null,
            "local_date DESC, created_at_utc_ms ASC"
        ).use { c ->
            val idIdx = c.getColumnIndexOrThrow("outcome_id")
            val hIdx = c.getColumnIndexOrThrow("habit_id")
            val dIdx = c.getColumnIndexOrThrow("local_date")
            val oIdx = c.getColumnIndexOrThrow("outcome_kind")
            val eIdx = c.getColumnIndexOrThrow("effort_level")
            val kIdx = c.getColumnIndexOrThrow("idempotency_key")
            val crIdx = c.getColumnIndexOrThrow("created_at_utc_ms")
            val upIdx = c.getColumnIndexOrThrow("updated_at_utc_ms")

            while (c.moveToNext()) {
                val dateStr = LocalDateValidator.requireValid(c.getString(dIdx), "local_date")
                list.add(
                    HabitDayOutcome(
                        outcomeId = c.getString(idIdx),
                        habitId = c.getString(hIdx),
                        localDate = dateStr,
                        outcomeKind = c.getString(oIdx),
                        effortLevel = if (c.isNull(eIdx)) null else c.getString(eIdx),
                        idempotencyKey = c.getString(kIdx),
                        createdAtUtcMs = c.getLong(crIdx),
                        updatedAtUtcMs = c.getLong(upIdx)
                    )
                )
            }
        }
        _habitDayOutcomes.value = list
    }

    private fun loadHabitDateNotesSync(db: SQLiteDatabase) {
        val list = mutableListOf<HabitDateNote>()
        db.query(
            DayseedDatabaseHelper.TABLE_HABIT_DATE_NOTES,
            null, null, null, null, null,
            "local_date DESC"
        ).use { c ->
            val idIdx = c.getColumnIndexOrThrow("note_id")
            val hIdx = c.getColumnIndexOrThrow("habit_id")
            val dIdx = c.getColumnIndexOrThrow("local_date")
            val bIdx = c.getColumnIndexOrThrow("body")
            val crIdx = c.getColumnIndexOrThrow("created_at_utc_ms")
            val upIdx = c.getColumnIndexOrThrow("updated_at_utc_ms")

            while (c.moveToNext()) {
                val dateStr = LocalDateValidator.requireValid(c.getString(dIdx), "local_date")
                list.add(
                    HabitDateNote(
                        noteId = c.getString(idIdx),
                        habitId = c.getString(hIdx),
                        localDate = dateStr,
                        body = c.getString(bIdx),
                        createdAtUtcMs = c.getLong(crIdx),
                        updatedAtUtcMs = c.getLong(upIdx)
                    )
                )
            }
        }
        _habitDateNotes.value = list
    }

    private fun loadHabitDateExceptionsSync(db: SQLiteDatabase) {
        val list = mutableListOf<HabitDateException>()
        db.query(
            DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS,
            null, null, null, null, null,
            "local_date DESC"
        ).use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val hIdx = c.getColumnIndexOrThrow("habit_id")
            val dIdx = c.getColumnIndexOrThrow("local_date")
            val kIdx = c.getColumnIndexOrThrow("exception_kind")
            val nIdx = c.getColumnIndexOrThrow("note")
            val crIdx = c.getColumnIndexOrThrow("created_at_utc_ms")
            val upIdx = c.getColumnIndexOrThrow("updated_at_utc_ms")

            while (c.moveToNext()) {
                val dateStr = LocalDateValidator.requireValid(c.getString(dIdx), "local_date")
                list.add(
                    HabitDateException(
                        id = c.getString(idIdx),
                        habitId = c.getString(hIdx),
                        localDate = dateStr,
                        exceptionKind = HabitDateExceptionKind.valueOf(c.getString(kIdx)),
                        note = if (c.isNull(nIdx)) null else c.getString(nIdx),
                        createdAtUtcMs = c.getLong(crIdx),
                        updatedAtUtcMs = c.getLong(upIdx)
                    )
                )
            }
        }
        _habitDateExceptions.value = list
    }

    /**
     * Creates a one-off `REST_DAY` exception for today (D-218, U-1 Option A).
     * Rejected for past or future dates, unscheduled dates, paused/archived/completed habits,
     * or dates that already have recorded activity.
     */
    fun setRestDayExceptionSync(
        habitId: String,
        date: LocalDate = clock.todayLocal(),
        note: String? = null
    ): Pair<HabitDateException, UndoToken> {
        val today = clock.todayLocal()
        require(date == today) {
            "REST_DAY exceptions are only permitted for today ($today); retroactive or future date $date is rejected (U-1)."
        }

        val eff = getEffectiveHabitVersion(habitId, date)
            ?: throw IllegalArgumentException("Habit '$habitId' has no effective version on $date.")
        require(eff.lifecycleState == HabitLifecycleState.ACTIVE) {
            "Cannot take a rest day when habit lifecycle state is ${eff.lifecycleState} (U-1 / U-9)."
        }
        require(StreakAndGroveEngine.isScheduledOpportunityOnDate(eff, date)) {
            "Cannot take a rest day on an unscheduled date ($date)."
        }
        check(!hasRecordedActivityForVersionLockOnDate(habitId, date)) {
            "Cannot take a rest day on $date because activity is already recorded."
        }

        val dateStr = date.toString()
        val existing = _habitDateExceptions.value.find { it.habitId == habitId && it.localDate == dateStr }
        val prevJson = existing?.let {
            JSONObject().apply {
                put("id", it.id)
                put("exception_kind", it.exceptionKind.name)
                put("note", it.note ?: JSONObject.NULL)
                put("created_at_utc_ms", it.createdAtUtcMs)
                put("updated_at_utc_ms", it.updatedAtUtcMs)
            }.toString()
        }

        val nowMs = clock.nowUtcMs()
        val id = existing?.id ?: UUID.randomUUID().toString()
        val createdAt = existing?.createdAtUtcMs ?: nowMs
        val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }

        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("id", id)
            put("habit_id", habitId)
            put("local_date", dateStr)
            put("exception_kind", HabitDateExceptionKind.REST_DAY.name)
            if (cleanNote != null) put("note", cleanNote) else putNull("note")
            put("created_at_utc_ms", createdAt)
            put("updated_at_utc_ms", nowMs)
        }
        if (existing != null) {
            db.update(DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS, cv, "id = ?", arrayOf(id))
        } else {
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS, null, cv)
        }

        recordObservedClockSync()
        refreshAllSync()

        val saved = HabitDateException(
            id = id,
            habitId = habitId,
            localDate = dateStr,
            exceptionKind = HabitDateExceptionKind.REST_DAY,
            note = cleanNote,
            createdAtUtcMs = createdAt,
            updatedAtUtcMs = nowMs
        )
        val token = UndoToken(
            tokenId = UUID.randomUUID().toString(),
            entityType = UndoEntityType.REST_DAY_EXCEPTION,
            entityId = id,
            expectedUpdatedAtUtcMs = nowMs,
            previousPayloadJson = prevJson,
            createdAtUtcMs = nowMs
        )
        _activeUndoToken.value = token
        return saved to token
    }

    suspend fun setRestDayException(
        habitId: String,
        date: LocalDate = clock.todayLocal(),
        note: String? = null
    ): Pair<HabitDateException, UndoToken> = withContext(Dispatchers.IO) {
        setRestDayExceptionSync(habitId, date, note)
    }

    fun clearRestDayExceptionSync(habitId: String, date: LocalDate = clock.todayLocal()) {
        val db = dbHelper.writableDatabase
        db.delete(
            DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS,
            "habit_id = ? AND local_date = ?",
            arrayOf(habitId, date.toString())
        )
        refreshAllSync()
    }

    suspend fun clearRestDayException(habitId: String, date: LocalDate = clock.todayLocal()) =
        withContext(Dispatchers.IO) {
            clearRestDayExceptionSync(habitId, date)
        }

    fun getExceptionForHabitOnDate(habitId: String, date: LocalDate): HabitDateException? {
        val dStr = date.toString()
        return _habitDateExceptions.value.find { it.habitId == habitId && it.localDate == dStr }
    }

    /**
     * Logs a multi-entry QUANTITY or DURATION habit entry (D-189, D-190, D-218).
     * When `valueUnits > 0`, automatically clears any same-date `REST_DAY` exception inside the transaction.
     */
    fun logHabitEntrySync(
        habitId: String,
        date: LocalDate,
        valueUnits: Int,
        entryNote: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Pair<HabitEntry, UndoToken> {
        require(valueUnits >= 0) { "value_units must be >= 0" }
        val dateStr = LocalDateValidator.requireValid(date.toString(), "local_date")
        val today = clock.todayLocal()
        require(!date.isAfter(today)) {
            "Cannot log habit entry for future date $dateStr (today is $today; D-188)."
        }
        val db = dbHelper.writableDatabase

        // Check idempotent retry in habit_entries
        val existingByKey = _habitEntries.value.find { it.idempotencyKey == idempotencyKey }
        if (existingByKey != null && existingByKey.habitId == habitId && existingByKey.localDate == dateStr) {
            val token = UndoToken(
                tokenId = UUID.randomUUID().toString(),
                entityType = UndoEntityType.HABIT_ENTRY,
                entityId = existingByKey.entryId,
                expectedUpdatedAtUtcMs = existingByKey.updatedAtUtcMs,
                previousPayloadJson = null,
                createdAtUtcMs = clock.nowUtcMs()
            )
            _activeUndoToken.value = token
            return existingByKey to token
        }

        val eff = getEffectiveHabitVersion(habitId, date)
        if (_habitVersions.value.any { it.habitId == habitId } && eff == null) {
            throw IllegalArgumentException(
                "Cannot log habit entry on $dateStr before the habit's earliest effective creation date (D-188)."
            )
        }
        if (eff != null && eff.lifecycleState != HabitLifecycleState.ACTIVE) {
            throw IllegalStateException("Cannot log habit entry on $dateStr while habit is ${eff.lifecycleState}.")
        }

        val existingRestDay = _habitDateExceptions.value.find { it.habitId == habitId && it.localDate == dateStr }
        val prevPayloadJson = existingRestDay?.let {
            JSONObject().apply {
                put("restored_rest_day_id", it.id)
                put("restored_rest_day_note", it.note ?: JSONObject.NULL)
                put("restored_rest_day_created_at", it.createdAtUtcMs)
                put("restored_rest_day_updated_at", it.updatedAtUtcMs)
            }.toString()
        }

        val nowMs = clock.nowUtcMs()
        val entryId = UUID.randomUUID().toString()
        db.beginTransaction()
        try {
            if (existingRestDay != null) {
                db.delete(
                    DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS,
                    "habit_id = ? AND local_date = ?",
                    arrayOf(habitId, dateStr)
                )
            }
            val cv = ContentValues().apply {
                put("entry_id", entryId)
                put("habit_id", habitId)
                put("local_date", dateStr)
                put("value_units", valueUnits)
                if (!entryNote.isNullOrBlank()) put("entry_note", entryNote.trim()) else putNull("entry_note")
                put("created_at_utc_ms", nowMs)
                put("updated_at_utc_ms", nowMs)
                put("idempotency_key", idempotencyKey)
            }
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_ENTRIES, null, cv)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        recordObservedClockSync()
        refreshAllSync()

        val entry = HabitEntry(
            entryId = entryId,
            habitId = habitId,
            localDate = dateStr,
            valueUnits = valueUnits,
            entryNote = entryNote?.trim()?.takeIf { it.isNotEmpty() },
            createdAtUtcMs = nowMs,
            updatedAtUtcMs = nowMs,
            idempotencyKey = idempotencyKey
        )
        val token = UndoToken(
            tokenId = UUID.randomUUID().toString(),
            entityType = UndoEntityType.HABIT_ENTRY,
            entityId = entryId,
            expectedUpdatedAtUtcMs = nowMs,
            previousPayloadJson = prevPayloadJson,
            createdAtUtcMs = nowMs
        )
        _activeUndoToken.value = token
        return entry to token
    }

    suspend fun logHabitEntry(
        habitId: String,
        date: LocalDate,
        valueUnits: Int,
        entryNote: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Pair<HabitEntry, UndoToken> = withContext(Dispatchers.IO) {
        logHabitEntrySync(habitId, date, valueUnits, entryNote, idempotencyKey)
    }

    fun updateHabitEntrySync(
        entryId: String,
        newValueUnits: Int,
        newEntryNote: String?
    ): UndoToken? {
        require(newValueUnits >= 0) { "value_units must be >= 0" }
        val existing = _habitEntries.value.find { it.entryId == entryId } ?: return null
        val prevJson = JSONObject().apply {
            put("value_units", existing.valueUnits)
            put("entry_note", existing.entryNote ?: JSONObject.NULL)
            put("updated_at_utc_ms", existing.updatedAtUtcMs)
        }.toString()

        val nowMs = clock.nowUtcMs()
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("value_units", newValueUnits)
            if (!newEntryNote.isNullOrBlank()) put("entry_note", newEntryNote.trim()) else putNull("entry_note")
            put("updated_at_utc_ms", nowMs)
        }
        db.update(DayseedDatabaseHelper.TABLE_HABIT_ENTRIES, cv, "entry_id = ?", arrayOf(entryId))
        refreshAllSync()

        val token = UndoToken(
            tokenId = UUID.randomUUID().toString(),
            entityType = UndoEntityType.HABIT_ENTRY,
            entityId = entryId,
            expectedUpdatedAtUtcMs = nowMs,
            previousPayloadJson = prevJson,
            createdAtUtcMs = nowMs
        )
        _activeUndoToken.value = token
        return token
    }

    suspend fun updateHabitEntry(entryId: String, newValueUnits: Int, newEntryNote: String?): UndoToken? =
        withContext(Dispatchers.IO) {
            updateHabitEntrySync(entryId, newValueUnits, newEntryNote)
        }

    fun deleteHabitEntrySync(entryId: String) {
        val db = dbHelper.writableDatabase
        db.delete(DayseedDatabaseHelper.TABLE_HABIT_ENTRIES, "entry_id = ?", arrayOf(entryId))
        refreshAllSync()
    }

    suspend fun deleteHabitEntry(entryId: String) = withContext(Dispatchers.IO) {
        deleteHabitEntrySync(entryId)
    }

    /**
     * Deletes an individual session (`habit_entries` row) and issues a guarded 8-second UndoToken (§9.C).
     * Sibling entries on the same habit and date remain completely intact.
     */
    fun deleteHabitEntryWithUndoSync(entryId: String): UndoToken? {
        val existing = _habitEntries.value.find { it.entryId == entryId } ?: return null
        val snapJson = JSONObject().apply {
            put("deleted_entry_restore", true)
            put("entry_id", existing.entryId)
            put("habit_id", existing.habitId)
            put("local_date", existing.localDate)
            put("value_units", existing.valueUnits)
            put("entry_note", existing.entryNote ?: JSONObject.NULL)
            put("created_at_utc_ms", existing.createdAtUtcMs)
            put("updated_at_utc_ms", existing.updatedAtUtcMs)
            put("idempotency_key", existing.idempotencyKey)
        }.toString()

        val nowMs = clock.nowUtcMs()
        val db = dbHelper.writableDatabase
        val deletedRows = db.delete(
            DayseedDatabaseHelper.TABLE_HABIT_ENTRIES,
            "entry_id = ?",
            arrayOf(entryId)
        )
        if (deletedRows != 1) return null

        refreshAllSync()
        val token = UndoToken(
            tokenId = UUID.randomUUID().toString(),
            entityType = UndoEntityType.HABIT_ENTRY,
            entityId = entryId,
            expectedUpdatedAtUtcMs = existing.updatedAtUtcMs,
            previousPayloadJson = snapJson,
            createdAtUtcMs = nowMs
        )
        _activeUndoToken.value = token
        return token
    }

    suspend fun deleteHabitEntryWithUndo(entryId: String): UndoToken? = withContext(Dispatchers.IO) {
        deleteHabitEntryWithUndoSync(entryId)
    }

    /**
     * Logs a COMPLETION_ONLY or EFFORT_AWARE outcome in habit_day_outcomes (D-188, D-189, D-190, D-218).
     * Automatically clears any same-date `REST_DAY` exception inside the transaction.
     */
    fun logHabitDayOutcomeSync(
        habitId: String,
        date: LocalDate,
        outcomeKind: String,
        effortLevel: String? = null,
        note: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Pair<HabitDayOutcome, UndoToken> {
        val dateStr = LocalDateValidator.requireValid(date.toString(), "local_date")
        val today = clock.todayLocal()
        require(!date.isAfter(today)) {
            "Cannot log habit outcome for future date $dateStr (today is $today; D-188)."
        }
        val db = dbHelper.writableDatabase

        // Idempotent retry check
        val existingByKey = _habitDayOutcomes.value.find { it.idempotencyKey == idempotencyKey }
        if (existingByKey != null && existingByKey.habitId == habitId && existingByKey.localDate == dateStr) {
            val token = UndoToken(
                tokenId = UUID.randomUUID().toString(),
                entityType = UndoEntityType.HABIT_DAY_OUTCOME,
                entityId = existingByKey.outcomeId,
                expectedUpdatedAtUtcMs = existingByKey.updatedAtUtcMs,
                previousPayloadJson = null,
                createdAtUtcMs = clock.nowUtcMs()
            )
            _activeUndoToken.value = token
            return existingByKey to token
        }

        val eff = getEffectiveHabitVersion(habitId, date)
        if (_habitVersions.value.any { it.habitId == habitId } && eff == null) {
            throw IllegalArgumentException(
                "Cannot log habit outcome on $dateStr before the habit's earliest effective creation date (D-188)."
            )
        }
        if (eff != null && eff.lifecycleState != HabitLifecycleState.ACTIVE) {
            throw IllegalStateException("Cannot log habit outcome on $dateStr while habit is ${eff.lifecycleState}.")
        }

        val existingForDate = _habitDayOutcomes.value.find { it.habitId == habitId && it.localDate == dateStr }
        val existingRestDay = _habitDateExceptions.value.find { it.habitId == habitId && it.localDate == dateStr }

        val prevJson = when {
            existingForDate != null -> JSONObject().apply {
                put("outcome_id", existingForDate.outcomeId)
                put("outcome_kind", existingForDate.outcomeKind)
                put("effort_level", existingForDate.effortLevel ?: JSONObject.NULL)
                put("idempotency_key", existingForDate.idempotencyKey)
                put("created_at_utc_ms", existingForDate.createdAtUtcMs)
                put("updated_at_utc_ms", existingForDate.updatedAtUtcMs)
            }.toString()
            existingRestDay != null -> JSONObject().apply {
                put("restored_rest_day_id", existingRestDay.id)
                put("restored_rest_day_note", existingRestDay.note ?: JSONObject.NULL)
                put("restored_rest_day_created_at", existingRestDay.createdAtUtcMs)
                put("restored_rest_day_updated_at", existingRestDay.updatedAtUtcMs)
            }.toString()
            else -> null
        }

        val nowMs = clock.nowUtcMs()
        val outcomeId = existingForDate?.outcomeId ?: UUID.randomUUID().toString()
        val createdAt = existingForDate?.createdAtUtcMs ?: nowMs

        db.beginTransaction()
        try {
            if (existingRestDay != null) {
                db.delete(
                    DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS,
                    "habit_id = ? AND local_date = ?",
                    arrayOf(habitId, dateStr)
                )
            }
            val cv = ContentValues().apply {
                put("outcome_id", outcomeId)
                put("habit_id", habitId)
                put("local_date", dateStr)
                put("outcome_kind", outcomeKind)
                if (effortLevel != null) put("effort_level", effortLevel) else putNull("effort_level")
                put("idempotency_key", idempotencyKey)
                put("created_at_utc_ms", createdAt)
                put("updated_at_utc_ms", nowMs)
            }
            if (existingForDate != null) {
                db.update(
                    DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES,
                    cv,
                    "outcome_id = ?",
                    arrayOf(outcomeId)
                )
            } else {
                db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES, null, cv)
            }

            if (!note.isNullOrBlank()) {
                saveHabitDateNoteInternal(db, habitId, dateStr, note.trim(), nowMs)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        recordObservedClockSync()
        refreshAllSync()

        val outcome = HabitDayOutcome(
            outcomeId = outcomeId,
            habitId = habitId,
            localDate = dateStr,
            outcomeKind = outcomeKind,
            effortLevel = effortLevel,
            idempotencyKey = idempotencyKey,
            createdAtUtcMs = createdAt,
            updatedAtUtcMs = nowMs
        )
        val token = UndoToken(
            tokenId = UUID.randomUUID().toString(),
            entityType = UndoEntityType.HABIT_DAY_OUTCOME,
            entityId = outcomeId,
            expectedUpdatedAtUtcMs = nowMs,
            previousPayloadJson = prevJson,
            createdAtUtcMs = nowMs
        )
        _activeUndoToken.value = token
        return outcome to token
    }

    suspend fun logHabitDayOutcome(
        habitId: String,
        date: LocalDate,
        outcomeKind: String,
        effortLevel: String? = null,
        note: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Pair<HabitDayOutcome, UndoToken> = withContext(Dispatchers.IO) {
        logHabitDayOutcomeSync(habitId, date, outcomeKind, effortLevel, note, idempotencyKey)
    }

    fun removeHabitDayOutcomeSync(habitId: String, date: LocalDate) {
        val db = dbHelper.writableDatabase
        db.delete(
            DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES,
            "habit_id = ? AND local_date = ?",
            arrayOf(habitId, date.toString())
        )
        refreshAllSync()
    }

    fun removeRecordSync(habitId: String, date: LocalDate) {
        val dStr = date.toString()
        val db = dbHelper.writableDatabase
        db.delete(DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES, "habit_id = ? AND local_date = ?", arrayOf(habitId, dStr))
        db.delete(DayseedDatabaseHelper.TABLE_HABIT_ENTRIES, "habit_id = ? AND local_date = ?", arrayOf(habitId, dStr))
        db.delete(DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS, "habit_id = ? AND local_date = ?", arrayOf(habitId, dStr))
        refreshAllSync()
    }

    suspend fun removeRecord(habitId: String, date: LocalDate) = withContext(Dispatchers.IO) {
        removeRecordSync(habitId, date)
    }

    private fun saveHabitDateNoteInternal(
        db: SQLiteDatabase,
        habitId: String,
        localDate: String,
        body: String,
        nowMs: Long
    ): Pair<HabitDateNote, UndoToken> {
        val existing = _habitDateNotes.value.find { it.habitId == habitId && it.localDate == localDate }
        val prevJson = existing?.let {
            JSONObject().apply {
                put("note_id", it.noteId)
                put("body", it.body)
                put("created_at_utc_ms", it.createdAtUtcMs)
                put("updated_at_utc_ms", it.updatedAtUtcMs)
            }.toString()
        }
        val noteId = existing?.noteId ?: UUID.randomUUID().toString()
        val createdAt = existing?.createdAtUtcMs ?: nowMs

        val cv = ContentValues().apply {
            put("note_id", noteId)
            put("habit_id", habitId)
            put("local_date", localDate)
            put("body", body)
            put("created_at_utc_ms", createdAt)
            put("updated_at_utc_ms", nowMs)
        }
        if (existing != null) {
            db.update(DayseedDatabaseHelper.TABLE_HABIT_DATE_NOTES, cv, "note_id = ?", arrayOf(noteId))
        } else {
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_DATE_NOTES, null, cv)
        }

        val saved = HabitDateNote(
            noteId = noteId,
            habitId = habitId,
            localDate = localDate,
            body = body,
            createdAtUtcMs = createdAt,
            updatedAtUtcMs = nowMs
        )
        val token = UndoToken(
            tokenId = UUID.randomUUID().toString(),
            entityType = UndoEntityType.HABIT_DATE_NOTE,
            entityId = noteId,
            expectedUpdatedAtUtcMs = nowMs,
            previousPayloadJson = prevJson,
            createdAtUtcMs = nowMs
        )
        return saved to token
    }

    fun saveHabitDateNoteSync(habitId: String, date: LocalDate, body: String): Pair<HabitDateNote, UndoToken> {
        val db = dbHelper.writableDatabase
        val nowMs = clock.nowUtcMs()
        val dateStr = LocalDateValidator.requireValid(date.toString(), "local_date")
        val result = saveHabitDateNoteInternal(db, habitId, dateStr, body.trim(), nowMs)
        refreshAllSync()
        _activeUndoToken.value = result.second
        return result
    }

    suspend fun saveHabitDateNote(habitId: String, date: LocalDate, body: String): Pair<HabitDateNote, UndoToken> =
        withContext(Dispatchers.IO) {
            saveHabitDateNoteSync(habitId, date, body)
        }

    /**
     * Guarded 8-second Undo execution (D-209, AC-023, Revision 7 §9.1):
     * Supports HABIT_ENTRY, HABIT_DAY_OUTCOME, HABIT_DATE_NOTE, REST_DAY_EXCEPTION, and HABIT_DELETION.
     */
    fun executeUndoSync(token: UndoToken): Boolean {
        if (consumedUndoTokenIds.contains(token.tokenId)) {
            if (_activeUndoToken.value?.tokenId == token.tokenId) {
                _activeUndoToken.value = null
            }
            return false
        }
        val nowMs = clock.nowUtcMs()
        if (nowMs > token.expiresAtUtcMs) {
            consumedUndoTokenIds.add(token.tokenId)
            if (_activeUndoToken.value?.tokenId == token.tokenId) {
                _activeUndoToken.value = null
            }
            return false
        }

        val db = dbHelper.writableDatabase
        var undone = false
        db.beginTransaction()
        try {
            when (token.entityType) {
                UndoEntityType.HABIT_ENTRY -> {
                    val prevObj = token.previousPayloadJson?.let { JSONObject(it) }
                    if (prevObj != null && prevObj.optBoolean("deleted_entry_restore", false)) {
                        val entryId = prevObj.getString("entry_id")
                        val habitId = prevObj.getString("habit_id")
                        val localDate = LocalDateValidator.requireValid(prevObj.getString("local_date"), "local_date")
                        val idempKey = prevObj.getString("idempotency_key")

                        // Guard 1: Parent habit must still exist
                        val parentExists = habitExistsInDb(db, habitId)
                        // Guard 2: No primary-key or idempotency_key collision in habit_entries/habit_day_outcomes
                        var hasCollision = false
                        db.rawQuery(
                            """
                            SELECT 1 FROM ${DayseedDatabaseHelper.TABLE_HABIT_ENTRIES} WHERE entry_id = ? OR idempotency_key = ?
                            UNION ALL
                            SELECT 1 FROM ${DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES} WHERE idempotency_key = ?
                            LIMIT 1
                            """.trimIndent(),
                            arrayOf(entryId, idempKey, idempKey)
                        ).use { c ->
                            hasCollision = c.moveToFirst()
                        }
                        // Guard 3: If a REST_DAY exception was added on the same date after deletion, do not overwrite it
                        var hasRestDayCollision = false
                        db.rawQuery(
                            "SELECT 1 FROM ${DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS} WHERE habit_id = ? AND local_date = ? LIMIT 1",
                            arrayOf(habitId, localDate)
                        ).use { c ->
                            hasRestDayCollision = c.moveToFirst()
                        }

                        if (parentExists && !hasCollision && !hasRestDayCollision) {
                            val cv = ContentValues().apply {
                                put("entry_id", entryId)
                                put("habit_id", habitId)
                                put("local_date", localDate)
                                put("value_units", prevObj.getInt("value_units"))
                                if (prevObj.isNull("entry_note")) putNull("entry_note") else put("entry_note", prevObj.getString("entry_note"))
                                put("created_at_utc_ms", prevObj.getLong("created_at_utc_ms"))
                                put("updated_at_utc_ms", prevObj.getLong("updated_at_utc_ms"))
                                put("idempotency_key", idempKey)
                            }
                            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_ENTRIES, null, cv)
                            undone = true
                        }
                    } else {
                        val current = _habitEntries.value.find { it.entryId == token.entityId }
                        if (current != null && current.updatedAtUtcMs == token.expectedUpdatedAtUtcMs) {
                            if (prevObj == null || prevObj.has("restored_rest_day_id")) {
                                val deleted = db.delete(
                                    DayseedDatabaseHelper.TABLE_HABIT_ENTRIES,
                                    "entry_id = ? AND updated_at_utc_ms = ?",
                                    arrayOf(token.entityId, token.expectedUpdatedAtUtcMs.toString())
                                )
                                if (deleted == 1 && prevObj != null && prevObj.has("restored_rest_day_id")) {
                                    val rCv = ContentValues().apply {
                                        put("id", prevObj.getString("restored_rest_day_id"))
                                        put("habit_id", current.habitId)
                                        put("local_date", current.localDate)
                                        put("exception_kind", HabitDateExceptionKind.REST_DAY.name)
                                        if (prevObj.isNull("restored_rest_day_note")) {
                                            putNull("note")
                                        } else {
                                            put("note", prevObj.getString("restored_rest_day_note"))
                                        }
                                        put("created_at_utc_ms", prevObj.getLong("restored_rest_day_created_at"))
                                        put("updated_at_utc_ms", prevObj.getLong("restored_rest_day_updated_at"))
                                    }
                                    db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS, null, rCv)
                                }
                                undone = deleted == 1
                            } else {
                                val cv = ContentValues().apply {
                                    put("value_units", prevObj.getInt("value_units"))
                                    if (prevObj.isNull("entry_note")) putNull("entry_note") else put("entry_note", prevObj.getString("entry_note"))
                                    put("updated_at_utc_ms", prevObj.getLong("updated_at_utc_ms"))
                                }
                                val updated = db.update(
                                    DayseedDatabaseHelper.TABLE_HABIT_ENTRIES,
                                    cv,
                                    "entry_id = ? AND updated_at_utc_ms = ?",
                                    arrayOf(token.entityId, token.expectedUpdatedAtUtcMs.toString())
                                )
                                undone = updated == 1
                            }
                        }
                    }
                }

                UndoEntityType.HABIT_DAY_OUTCOME -> {
                    val current = _habitDayOutcomes.value.find { it.outcomeId == token.entityId }
                    if (current != null && current.updatedAtUtcMs == token.expectedUpdatedAtUtcMs) {
                        val prevObj = token.previousPayloadJson?.let { JSONObject(it) }
                        if (prevObj == null || prevObj.has("restored_rest_day_id")) {
                            val deleted = db.delete(
                                DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES,
                                "outcome_id = ? AND updated_at_utc_ms = ?",
                                arrayOf(token.entityId, token.expectedUpdatedAtUtcMs.toString())
                            )
                            if (deleted == 1 && prevObj != null && prevObj.has("restored_rest_day_id")) {
                                val rCv = ContentValues().apply {
                                    put("id", prevObj.getString("restored_rest_day_id"))
                                    put("habit_id", current.habitId)
                                    put("local_date", current.localDate)
                                    put("exception_kind", HabitDateExceptionKind.REST_DAY.name)
                                    if (prevObj.isNull("restored_rest_day_note")) {
                                        putNull("note")
                                    } else {
                                        put("note", prevObj.getString("restored_rest_day_note"))
                                    }
                                    put("created_at_utc_ms", prevObj.getLong("restored_rest_day_created_at"))
                                    put("updated_at_utc_ms", prevObj.getLong("restored_rest_day_updated_at"))
                                }
                                db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS, null, rCv)
                            }
                            undone = deleted == 1
                        } else {
                            val cv = ContentValues().apply {
                                put("outcome_kind", prevObj.getString("outcome_kind"))
                                if (prevObj.isNull("effort_level")) putNull("effort_level") else put("effort_level", prevObj.getString("effort_level"))
                                put("idempotency_key", prevObj.getString("idempotency_key"))
                                put("created_at_utc_ms", prevObj.getLong("created_at_utc_ms"))
                                put("updated_at_utc_ms", prevObj.getLong("updated_at_utc_ms"))
                            }
                            val updated = db.update(
                                DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES,
                                cv,
                                "outcome_id = ? AND updated_at_utc_ms = ?",
                                arrayOf(token.entityId, token.expectedUpdatedAtUtcMs.toString())
                            )
                            undone = updated == 1
                        }
                    }
                }

                UndoEntityType.HABIT_DATE_NOTE -> {
                    val current = _habitDateNotes.value.find { it.noteId == token.entityId }
                    if (current != null && current.updatedAtUtcMs == token.expectedUpdatedAtUtcMs) {
                        if (token.previousPayloadJson == null) {
                            val deleted = db.delete(
                                DayseedDatabaseHelper.TABLE_HABIT_DATE_NOTES,
                                "note_id = ? AND updated_at_utc_ms = ?",
                                arrayOf(token.entityId, token.expectedUpdatedAtUtcMs.toString())
                            )
                            undone = deleted == 1
                        } else {
                            val prev = JSONObject(token.previousPayloadJson)
                            val cv = ContentValues().apply {
                                put("body", prev.getString("body"))
                                put("updated_at_utc_ms", prev.getLong("updated_at_utc_ms"))
                            }
                            val updated = db.update(
                                DayseedDatabaseHelper.TABLE_HABIT_DATE_NOTES,
                                cv,
                                "note_id = ? AND updated_at_utc_ms = ?",
                                arrayOf(token.entityId, token.expectedUpdatedAtUtcMs.toString())
                            )
                            undone = updated == 1
                        }
                    }
                }

                UndoEntityType.REST_DAY_EXCEPTION -> {
                    val current = _habitDateExceptions.value.find { it.id == token.entityId }
                    if (current != null && current.updatedAtUtcMs == token.expectedUpdatedAtUtcMs) {
                        if (token.previousPayloadJson == null) {
                            val deleted = db.delete(
                                DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS,
                                "id = ? AND updated_at_utc_ms = ?",
                                arrayOf(token.entityId, token.expectedUpdatedAtUtcMs.toString())
                            )
                            undone = deleted == 1
                        } else {
                            val prev = JSONObject(token.previousPayloadJson)
                            val cv = ContentValues().apply {
                                put("exception_kind", prev.getString("exception_kind"))
                                if (prev.isNull("note")) putNull("note") else put("note", prev.getString("note"))
                                put("updated_at_utc_ms", prev.getLong("updated_at_utc_ms"))
                            }
                            val updated = db.update(
                                DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS,
                                cv,
                                "id = ? AND updated_at_utc_ms = ?",
                                arrayOf(token.entityId, token.expectedUpdatedAtUtcMs.toString())
                            )
                            undone = updated == 1
                        }
                    }
                }

                UndoEntityType.HABIT_DELETION -> {
                    val payloadStr = token.previousPayloadJson
                    if (payloadStr != null) {
                        undone = restoreDeletedHabitFromSnapshotInTx(db, JSONObject(payloadStr))
                    }
                }
            }
            if (undone) {
                db.setTransactionSuccessful()
            }
        } finally {
            db.endTransaction()
        }

        consumedUndoTokenIds.add(token.tokenId)
        if (_activeUndoToken.value?.tokenId == token.tokenId) {
            _activeUndoToken.value = null
        }
        if (undone) {
            refreshAllSync()
            // Re-arm the restored habit's alarm immediately instead of waiting for the periodic
            // reconcile worker (same hook discipline as save/delete-permanent/restore, D-226).
            if (token.entityType == UndoEntityType.HABIT_DELETION) {
                rescheduleReminderAlarms()
            }
        }
        return undone
    }

    suspend fun executeUndo(token: UndoToken): Boolean = withContext(Dispatchers.IO) {
        executeUndoSync(token)
    }

    // =========================================================================
    // STREAK & GROVE RECOMPUTATION + HABIT SUMMARIES (Revision 7 §8)
    // =========================================================================

    private fun recomputeAllStreakRunsAndSummariesSync(db: SQLiteDatabase) {
        val today = clock.todayLocal()
        val nowMs = clock.nowUtcMs()
        val versionsByHabit = _habitVersions.value.groupBy { it.habitId }
        val entriesByHabit = _habitEntries.value.groupBy { it.habitId }
        val outcomesByHabit = _habitDayOutcomes.value.groupBy { it.habitId }
        val exceptionsByHabit = _habitDateExceptions.value.groupBy { it.habitId }

        // Read existing runs so run_id and qualified_at_utc_ms are preserved across recomputations
        val existingRunsByHabit = mutableMapOf<String, MutableList<HabitStreakRun>>()
        db.query(DayseedDatabaseHelper.TABLE_HABIT_STREAK_RUNS, null, null, null, null, null, "start_local_date ASC").use { c ->
            val rIdx = c.getColumnIndexOrThrow("run_id")
            val hIdx = c.getColumnIndexOrThrow("habit_id")
            val sIdx = c.getColumnIndexOrThrow("start_local_date")
            val eIdx = c.getColumnIndexOrThrow("end_local_date")
            val cIdx = c.getColumnIndexOrThrow("completed_streak_count")
            val elIdx = c.getColumnIndexOrThrow("elapsed_active_span_count")
            val uIdx = c.getColumnIndexOrThrow("streak_unit")
            val trIdx = c.getColumnIndexOrThrow("terminal_reason")
            val actIdx = c.getColumnIndexOrThrow("is_active")
            val qIdx = c.getColumnIndexOrThrow("qualified_for_grove")
            val qaIdx = c.getColumnIndexOrThrow("qualified_at_utc_ms")
            val brIdx = c.getColumnIndexOrThrow("broken_on_local_date")
            val tsIdx = c.getColumnIndexOrThrow("tree_state")
            val crIdx = c.getColumnIndexOrThrow("created_at_utc_ms")
            val upIdx = c.getColumnIndexOrThrow("updated_at_utc_ms")
            while (c.moveToNext()) {
                val run = HabitStreakRun(
                    runId = c.getString(rIdx),
                    habitId = c.getString(hIdx),
                    startLocalDate = LocalDateValidator.requireValid(c.getString(sIdx), "start_local_date"),
                    endLocalDate = LocalDateValidator.requireValid(c.getString(eIdx), "end_local_date"),
                    completedStreakCount = c.getValidatedInt(cIdx, "completed_streak_count"),
                    elapsedActiveSpanCount = c.getValidatedInt(elIdx, "elapsed_active_span_count"),
                    streakUnit = StreakUnit.valueOf(c.getString(uIdx)),
                    terminalReason = if (c.isNull(trIdx)) null else GroveTerminalReason.valueOf(c.getString(trIdx)),
                    isActive = c.getValidatedInt(actIdx, "is_active") == 1,
                    qualifiedForGrove = c.getValidatedInt(qIdx, "qualified_for_grove") == 1,
                    qualifiedAtUtcMs = if (c.isNull(qaIdx)) null else c.getLong(qaIdx),
                    brokenOnLocalDate = if (c.isNull(brIdx)) null else LocalDateValidator.requireValid(c.getString(brIdx), "broken_on_local_date"),
                    treeState = GroveTreeState.valueOf(c.getString(tsIdx)),
                    createdAtUtcMs = c.getLong(crIdx),
                    updatedAtUtcMs = c.getLong(upIdx)
                )
                existingRunsByHabit.getOrPut(run.habitId) { mutableListOf() }.add(run)
            }
        }

        val allRuns = mutableListOf<HabitStreakRun>()
        val summaries = mutableMapOf<String, HabitSummary>()

        db.beginTransaction()
        try {
            for (habit in _habits.value) {
                val hId = habit.habitId
                val versions = versionsByHabit[hId].orEmpty()
                val entries = entriesByHabit[hId].orEmpty()
                val outcomes = outcomesByHabit[hId].orEmpty()
                val exceptions = exceptionsByHabit[hId].orEmpty()
                val existingRuns = existingRunsByHabit[hId].orEmpty()

                val runs = StreakAndGroveEngine.syncHabitStreakRunsInDb(
                    db = db,
                    habitId = hId,
                    versions = versions,
                    entries = entries,
                    outcomes = outcomes,
                    exceptions = exceptions,
                    weekStartHistory = _weekStartHistory.value,
                    evaluationDate = today,
                    nowUtcMs = nowMs,
                    existingRuns = existingRuns
                )
                allRuns.addAll(runs)

                val effToday = getEffectiveHabitVersion(hId, today) ?: versions.lastOrNull()
                if (effToday != null) {
                    val todayStr = today.toString()
                    val todayEntries = entries.filter { it.localDate == todayStr }
                    val todayOutcome = outcomes.find { it.localDate == todayStr }
                    val todayException = exceptions.find { it.localDate == todayStr }
                    val weekSnapshot = StreakAndGroveEngine.computeWeeklyProgressSnapshot(
                        versions = versions,
                        entries = entries,
                        outcomes = outcomes,
                        weekStartHistory = _weekStartHistory.value,
                        evaluationDate = today,
                        fallbackVersion = effToday
                    )
                    val evalTodayVersion = if (
                        weekSnapshot.frozenWeekVersion.targetPeriod == TargetPeriod.WEEK ||
                            effToday.targetPeriod == TargetPeriod.WEEK
                    ) {
                        weekSnapshot.frozenWeekVersion
                    } else {
                        effToday
                    }
                    val (todayState, todayBand) = StreakAndGroveEngine.evaluateDayRecordState(
                        version = effToday,
                        date = today,
                        dayEntries = todayEntries,
                        dayOutcome = todayOutcome,
                        dayException = todayException
                    )
                    val todayUnits = when (evalTodayVersion.trackingMode.canonical()) {
                        TrackingMode.QUANTITY, TrackingMode.DURATION ->
                            StreakAndGroveEngine.countDailyUnitsOrSessionsForWeek(evalTodayVersion, todayEntries)
                        TrackingMode.COMPLETION_ONLY ->
                            if (todayOutcome?.outcomeKind == "COMPLETED") BigInteger.ONE else BigInteger.ZERO
                        TrackingMode.EFFORT_AWARE, TrackingMode.EFFORT_LEVELS ->
                            if (todayOutcome != null) BigInteger.ONE else BigInteger.ZERO
                    }
                    val targetBi = BigInteger.valueOf((evalTodayVersion.targetValue ?: 1).coerceAtLeast(1).toLong())
                    val todayPct = when (evalTodayVersion.trackingMode.canonical()) {
                        TrackingMode.QUANTITY, TrackingMode.DURATION ->
                            ProgressBandClassifier.percentageFloor(todayUnits, targetBi)
                        TrackingMode.COMPLETION_ONLY ->
                            if (todayOutcome?.outcomeKind == "COMPLETED") 100 else 0
                        TrackingMode.EFFORT_AWARE, TrackingMode.EFFORT_LEVELS ->
                            when (todayOutcome?.effortLevel) {
                                "TINY" -> 20
                                "EXTRA" -> 125
                                "USUAL" -> 100
                                else -> if (todayOutcome != null) 100 else 0
                            }
                    }

                    val activeRun = runs.find { it.isActive }
                    val groveRuns = runs.filter { it.qualifiedForGrove }
                    val streakUnit = if (evalTodayVersion.targetPeriod == TargetPeriod.WEEK) StreakUnit.WEEKS else StreakUnit.DAYS
                    val lifetimeActive = StreakAndGroveEngine.computeLifetimeActiveDays(versions, entries, outcomes)
                    val isOpportunityToday = effToday.lifecycleState == HabitLifecycleState.ACTIVE &&
                        StreakAndGroveEngine.isScheduledOpportunityOnDate(effToday, today)
                    val isScheduledToday = if (effToday.lifecycleState != HabitLifecycleState.ACTIVE) {
                        false
                    } else {
                        when (effToday.scheduleKind) {
                            ScheduleKind.DAILY -> true
                            ScheduleKind.SELECTED_WEEKDAYS ->
                                WeekdayMask.contains(effToday.scheduledWeekdaysMask ?: WeekdayMask.ALL_DAYS_MASK, today.dayOfWeek)
                            ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> {
                                val prefMask = effToday.preferredWeekdaysMask
                                val matchesPreferred = prefMask == null ||
                                    prefMask == 0 ||
                                    WeekdayMask.contains(prefMask, today.dayOfWeek)

                                val weekSatisfied = weekSnapshot.weekTotalUnits >= weekSnapshot.weekTargetUnits

                                !weekSatisfied || matchesPreferred
                            }
                        }
                    }
                    val hasRestDayToday = todayException?.exceptionKind == HabitDateExceptionKind.REST_DAY
                    val hasActivityToday = todayEntries.isNotEmpty() || todayOutcome != null

                    summaries[hId] = HabitSummary(
                        habit = habit,
                        effectiveVersion = effToday,
                        todayState = todayState,
                        todayProgressBand = todayBand,
                        todayCompactBucket = ProgressBandClassifier.toCompactBucket(todayBand),
                        todayTotalUnits = todayUnits,
                        todayPercentage = todayPct,
                        weekTotalUnits = weekSnapshot.weekTotalUnits,
                        weekTargetUnits = weekSnapshot.weekTargetUnits,
                        weekProgressBand = weekSnapshot.weekProgressBand,
                        weekPercentage = weekSnapshot.weekPercentage,
                        weekStartLocalDate = weekSnapshot.weekStartLocalDate,
                        weekEndLocalDate = weekSnapshot.weekEndLocalDate,
                        currentCompletedStreak = activeRun?.completedStreakCount ?: 0,
                        currentElapsedActiveSpan = activeRun?.elapsedActiveSpanCount ?: 0,
                        streakUnit = streakUnit,
                        activeRun = activeRun,
                        groveRuns = groveRuns,
                        allStreakRuns = runs,
                        lifetimeActiveDays = lifetimeActive,
                        lifetimeGoalDays = habit.lifetimeGoalDays,
                        isScheduledToday = isScheduledToday,
                        hasRestDayToday = hasRestDayToday,
                        canTakeRestDayToday = isOpportunityToday && !hasRestDayToday && !hasActivityToday
                    )
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        _habitStreakRuns.value = allRuns
        _habitSummaries.value = summaries
    }

    /**
     * Selects the active habit with the longest active streak run for the Today header indicator (§5.1, §9.B).
     * Compares across daily and weekly habits by `elapsedActiveSpanCount` (tie-broken by `completedStreakCount`,
     * earliest `startLocalDate`, `habit.sortOrder`, and `habit.habitId`), while preserving explicit unit labels
     * (`"days"` vs `"weeks"`).
     */
    fun selectLongestActiveRun(): LongestActiveStreakSelection? {
        val candidates = _habitSummaries.value.values.mapNotNull { summary ->
            val run = summary.activeRun
            if (summary.effectiveVersion.lifecycleState == HabitLifecycleState.ACTIVE &&
                run != null &&
                run.isActive &&
                run.completedStreakCount > 0
            ) {
                LongestActiveStreakSelection(
                    habit = summary.habit,
                    run = run,
                    elapsedActiveSpanCount = run.elapsedActiveSpanCount,
                    completedStreakCount = run.completedStreakCount,
                    streakUnit = summary.streakUnit
                )
            } else {
                null
            }
        }
        return candidates.maxWithOrNull(
            Comparator<LongestActiveStreakSelection> { a, b ->
                val spanCmp = a.run.elapsedActiveSpanCount.compareTo(b.run.elapsedActiveSpanCount)
                if (spanCmp != 0) return@Comparator spanCmp
                val compCmp = a.run.completedStreakCount.compareTo(b.run.completedStreakCount)
                if (compCmp != 0) return@Comparator compCmp
                // Earlier startLocalDate wins tie
                val startCmp = b.run.startLocalDate.compareTo(a.run.startLocalDate)
                if (startCmp != 0) return@Comparator startCmp
                // Smaller sortOrder wins tie
                val sortCmp = b.habit.sortOrder.compareTo(a.habit.sortOrder)
                if (sortCmp != 0) return@Comparator sortCmp
                b.habit.habitId.compareTo(a.habit.habitId)
            }
        )
    }

    // =========================================================================
    // PROGRESS BANDS, DERIVED DAY STATUS & GROWTH MARK (G2 + G3)
    // =========================================================================

    fun getEntriesForHabitOnDate(habitId: String, date: LocalDate): List<HabitEntry> {
        val dStr = date.toString()
        return _habitEntries.value.filter { it.habitId == habitId && it.localDate == dStr }
    }

    fun getOutcomeForHabitOnDate(habitId: String, date: LocalDate): HabitDayOutcome? {
        val dStr = date.toString()
        return _habitDayOutcomes.value.find { it.habitId == habitId && it.localDate == dStr }
    }

    fun getDateNoteForHabitOnDate(habitId: String, date: LocalDate): HabitDateNote? {
        val dStr = date.toString()
        return _habitDateNotes.value.find { it.habitId == habitId && it.localDate == dStr }
    }

    /**
     * Evaluates the DerivedDayStatus for a habit on targetDate using the effective HabitVersion (D-188, D-193, D-218).
     */
    fun evaluateDerivedDayStatus(
        habitId: String,
        targetDate: LocalDate,
        referenceToday: LocalDate = clock.todayLocal()
    ): DerivedDayStatus {
        val version = getEffectiveHabitVersion(habitId, targetDate) ?: return DerivedDayStatus.UNSCHEDULED
        if (version.lifecycleState != HabitLifecycleState.ACTIVE) {
            return DerivedDayStatus.INACTIVE_LIFECYCLE
        }

        val habitVersionsForHabit = _habitVersions.value
            .filter { it.habitId == habitId }
            .sortedWith(compareBy<HabitVersion> { it.effectiveFromLocalDate }.thenBy { it.createdAtUtcMs })
        val firstDate = habitVersionsForHabit.firstOrNull()?.let {
            try {
                LocalDateValidator.parseStrict(it.effectiveFromLocalDate)
            } catch (_: Exception) {
                targetDate
            }
        } ?: targetDate
        val weekStart = getWeekStartDay(targetDate)
        val (weekStartDate, weekEnd) = WeekBoundaryHelper.weekBounds(targetDate, weekStart)
        val frozenWeekVersion = StreakAndGroveEngine.resolveFrozenWeekVersion(
            sortedVersions = habitVersionsForHabit,
            weekStart = weekStartDate,
            firstHabitDate = firstDate,
            fallbackDate = targetDate,
            fallbackVersion = version
        ) ?: version
        val isWeeklyPeriod = frozenWeekVersion.targetPeriod == TargetPeriod.WEEK ||
            frozenWeekVersion.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET ||
            version.targetPeriod == TargetPeriod.WEEK ||
            version.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET
        // Keep per-day lifecycle state on `version` separate from frozen weekly measurement/schedule semantics
        val evalVersion = if (isWeeklyPeriod) frozenWeekVersion else version

        val today = referenceToday
        val entriesOnDate = getEntriesForHabitOnDate(habitId, targetDate)
        val outcomeOnDate = getOutcomeForHabitOnDate(habitId, targetDate)
        val exceptionOnDate = getExceptionForHabitOnDate(habitId, targetDate)

        when (evalVersion.trackingMode.canonical()) {
            TrackingMode.COMPLETION_ONLY -> {
                if (outcomeOnDate != null) {
                    return DerivedDayStatus.COMPLETED
                }
            }
            TrackingMode.EFFORT_AWARE -> {
                if (outcomeOnDate != null) {
                    return if (outcomeOnDate.effortLevel == "TINY") {
                        DerivedDayStatus.COMPLETED
                    } else {
                        DerivedDayStatus.COMPLETED
                    }
                }
            }
            TrackingMode.QUANTITY, TrackingMode.DURATION -> {
                if (entriesOnDate.isNotEmpty()) {
                    val sum = StreakAndGroveEngine.countDailyUnitsOrSessionsForWeek(evalVersion, entriesOnDate)
                    if (sum == BigInteger.ZERO) {
                        return DerivedDayStatus.LOGGED_ZERO
                    }
                    val target = BigInteger.valueOf((evalVersion.targetValue ?: 1).coerceAtLeast(1).toLong())
                    val comparisonTotal = if (evalVersion.targetPeriod == TargetPeriod.WEEK ||
                        evalVersion.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET
                    ) {
                        val weekDates = (0..6).map { weekStartDate.plusDays(it.toLong()) }
                            .filter { !it.isBefore(firstDate) }
                        var weekTotal = BigInteger.ZERO
                        for (d in weekDates) {
                            val dSum = StreakAndGroveEngine.countDailyUnitsOrSessionsForWeek(
                                evalVersion,
                                getEntriesForHabitOnDate(habitId, d)
                            )
                            weekTotal = weekTotal.add(dSum)
                        }
                        weekTotal
                    } else {
                        sum
                    }
                    val band = ProgressBandClassifier.classify(comparisonTotal, target)
                    return when (band) {
                        ProgressBand.GOAL_MET, ProgressBand.EXTRA, ProgressBand.HUGE -> DerivedDayStatus.COMPLETED
                        ProgressBand.ZERO -> DerivedDayStatus.LOGGED_ZERO
                        else -> DerivedDayStatus.PARTIAL
                    }
                }
            }
            else -> {}
        }

        if (exceptionOnDate?.exceptionKind == HabitDateExceptionKind.REST_DAY) {
            return DerivedDayStatus.REST_DAY
        }

        // No recorded entry, outcome, or REST_DAY on targetDate
        return when (evalVersion.scheduleKind) {
            ScheduleKind.DAILY -> {
                if (targetDate.isBefore(today)) DerivedDayStatus.DERIVED_MISSED else DerivedDayStatus.OPEN_TODAY
            }
            ScheduleKind.SELECTED_WEEKDAYS -> {
                val mask = evalVersion.scheduledWeekdaysMask ?: WeekdayMask.ALL_DAYS_MASK
                if (!WeekdayMask.contains(mask, targetDate.dayOfWeek)) {
                    DerivedDayStatus.UNSCHEDULED
                } else if (targetDate.isBefore(today)) {
                    DerivedDayStatus.DERIVED_MISSED
                } else {
                    DerivedDayStatus.OPEN_TODAY
                }
            }
            ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> {
                if (WeekBoundaryHelper.isNeutralTransitionDate(targetDate, _weekStartHistory.value)) {
                    return DerivedDayStatus.UNSCHEDULED
                }
                val weekDates = (0..6).map { weekStartDate.plusDays(it.toLong()) }
                    .filter { !it.isBefore(firstDate) }
                val targetBi = BigInteger.valueOf((evalVersion.targetValue ?: 1).coerceAtLeast(1).toLong())
                val weekSatisfied = when (evalVersion.trackingMode.canonical()) {
                    TrackingMode.QUANTITY, TrackingMode.DURATION -> {
                        var weekTotal = BigInteger.ZERO
                        for (d in weekDates) {
                            val dSum = StreakAndGroveEngine.countDailyUnitsOrSessionsForWeek(
                                evalVersion,
                                getEntriesForHabitOnDate(habitId, d)
                            )
                            weekTotal = weekTotal.add(dSum)
                        }
                        weekTotal >= targetBi
                    }
                    else -> {
                        val weekCompletions = weekDates.count { d ->
                            getOutcomeForHabitOnDate(habitId, d) != null
                        }
                        BigInteger.valueOf(weekCompletions.toLong()) >= targetBi
                    }
                }
                if (weekSatisfied) {
                    DerivedDayStatus.UNSCHEDULED
                } else if (weekEnd.isBefore(today)) {
                    DerivedDayStatus.DERIVED_MISSED
                } else {
                    DerivedDayStatus.OPEN_TODAY
                }
            }
        }
    }

    /**
     * Computes the Growth Mark (G2 + G3) summary, Grove counts, and overall GrowthMetrics (D-032, D-201, D-217).
     */
    private fun computeGrowthMetricsSync() {
        val today = clock.todayLocal()
        val currentMonth = YearMonth.from(today)
        val activeDates = mutableSetOf<LocalDate>()

        var completedCount = 0
        var tinyCount = 0
        var usualCount = 0
        var extraCount = 0
        var currentMonthDurationMinutes = 0L
        val currentMonthDateSessionCounts = mutableMapOf<LocalDate, Int>()

        for (outcome in _habitDayOutcomes.value) {
            val d = try { LocalDateValidator.parseStrict(outcome.localDate) } catch (_: Exception) { continue }
            activeDates.add(d)
            completedCount++
            when (outcome.effortLevel) {
                "TINY" -> tinyCount++
                "USUAL" -> usualCount++
                "EXTRA" -> extraCount++
                else -> usualCount++
            }
            if (YearMonth.from(d) == currentMonth) {
                currentMonthDateSessionCounts[d] = (currentMonthDateSessionCounts[d] ?: 0) + 1
            }
        }

        for (entry in _habitEntries.value) {
            val d = try { LocalDateValidator.parseStrict(entry.localDate) } catch (_: Exception) { continue }
            if (entry.valueUnits > 0) {
                activeDates.add(d)
                completedCount++
                val version = getEffectiveHabitVersion(entry.habitId, d)
                val target = version?.targetValue ?: 1
                val band = ProgressBandClassifier.classify(entry.valueUnits, target)
                when (band) {
                    ProgressBand.TINY_STEP, ProgressBand.GETTING_STARTED -> tinyCount++
                    ProgressBand.HALFWAY, ProgressBand.ALMOST_THERE, ProgressBand.GOAL_MET -> usualCount++
                    ProgressBand.EXTRA, ProgressBand.HUGE -> extraCount++
                    ProgressBand.ZERO -> {}
                }

                if (YearMonth.from(d) == currentMonth) {
                    currentMonthDateSessionCounts[d] = (currentMonthDateSessionCounts[d] ?: 0) + 1
                    if (version?.trackingMode == TrackingMode.DURATION) {
                        val unitCode = version.unitCode ?: "min"
                        val mins = DurationNormalizer.toMinutes(entry.valueUnits, unitCode)
                        if (mins != null) {
                            currentMonthDurationMinutes += mins
                        }
                    }
                }
            }
        }

        for (j in _journalEntries.value) {
            val d = try { LocalDateValidator.parseStrict(j.localDate) } catch (_: Exception) { continue }
            activeDates.add(d)
        }

        var pastWeekOpportunities = 0
        var pastWeekCompletions = 0
        for (i in 0..6) {
            val d = today.minusDays(i.toLong())
            for (h in _habits.value) {
                val eff = getEffectiveHabitVersion(h.habitId, d)
                if (eff != null && eff.lifecycleState == HabitLifecycleState.ACTIVE) {
                    val isSched = when (eff.scheduleKind) {
                        ScheduleKind.DAILY -> true
                        ScheduleKind.SELECTED_WEEKDAYS -> WeekdayMask.contains(eff.scheduledWeekdaysMask ?: WeekdayMask.ALL_DAYS_MASK, d.dayOfWeek)
                        ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> WeekdayMask.contains(eff.preferredWeekdaysMask ?: WeekdayMask.ALL_DAYS_MASK, d.dayOfWeek)
                    }
                    if (isSched) {
                        pastWeekOpportunities++
                        if (hasQualifyingActivityOnDate(h.habitId, d)) {
                            pastWeekCompletions++
                        }
                    }
                }
            }
        }

        val runs = _habitStreakRuns.value
        val activeSaplings = runs.count { it.treeState == GroveTreeState.SAPLING_ACTIVE }
        val growingTrees = runs.count { it.treeState == GroveTreeState.GROWING_TREE }
        val dormantTrees = runs.count { it.treeState == GroveTreeState.DORMANT_TREE }

        _growthMetrics.value = GrowthMetrics(
            totalActiveDates = activeDates.size,
            totalHabitsCompleted = completedCount,
            tinyCount = tinyCount,
            usualCount = usualCount,
            extraCount = extraCount,
            plannedRestCount = _habitDateExceptions.value.size,
            journalEntriesCount = _journalEntries.value.size,
            currentPeriodCompletions = pastWeekCompletions,
            currentPeriodOpportunities = pastWeekOpportunities,
            currentMonthDurationMinutes = currentMonthDurationMinutes,
            currentMonthDateSessionCounts = currentMonthDateSessionCounts,
            activeSaplingsCount = activeSaplings,
            growingTreesCount = growingTrees,
            dormantTreesCount = dormantTrees
        )
    }

    // =========================================================================
    // JOURNAL ENTRIES, DRAFTS & RETROSPECTIVE PROMPTS (D-042, D-191)
    // =========================================================================

    private fun loadJournalEntriesSync(db: SQLiteDatabase) {
        val list = mutableListOf<JournalEntry>()
        db.query(
            DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES,
            null, null, null, null, null,
            "local_date DESC, created_at_utc_ms DESC"
        ).use { c ->
            val idIdx = c.getColumnIndexOrThrow("journal_entry_id")
            val dIdx = c.getColumnIndexOrThrow("local_date")
            val bIdx = c.getColumnIndexOrThrow("body")
            val tIdx = c.getColumnIndexOrThrow("title")
            val pmIdx = c.getColumnIndexOrThrow("primary_mood_code")
            val smIdx = c.getColumnIndexOrThrow("secondary_mood_code")
            val prIdx = c.getColumnIndexOrThrow("prompt_id")
            val lhIdx = c.getColumnIndexOrThrow("linked_habit_id")
            val crIdx = c.getColumnIndexOrThrow("created_at_utc_ms")
            val upIdx = c.getColumnIndexOrThrow("updated_at_utc_ms")

            while (c.moveToNext()) {
                val dateStr = LocalDateValidator.requireValid(c.getString(dIdx), "local_date")
                list.add(
                    JournalEntry(
                        journalEntryId = c.getString(idIdx),
                        localDate = dateStr,
                        body = c.getString(bIdx),
                        title = if (c.isNull(tIdx)) null else c.getString(tIdx),
                        primaryMoodCode = if (c.isNull(pmIdx)) null else c.getString(pmIdx),
                        secondaryMoodCode = if (c.isNull(smIdx)) null else c.getString(smIdx),
                        promptId = if (c.isNull(prIdx)) null else c.getString(prIdx),
                        linkedHabitId = if (c.isNull(lhIdx)) null else c.getString(lhIdx),
                        createdAtUtcMs = c.getLong(crIdx),
                        updatedAtUtcMs = c.getLong(upIdx)
                    )
                )
            }
        }
        _journalEntries.value = list
    }

    private fun loadJournalDraftsSync(db: SQLiteDatabase) {
        val list = mutableListOf<JournalDraft>()
        db.query(
            DayseedDatabaseHelper.TABLE_JOURNAL_DRAFTS,
            null, null, null, null, null,
            "updated_at_utc_ms DESC"
        ).use { c ->
            val idIdx = c.getColumnIndexOrThrow("draft_id")
            val dIdx = c.getColumnIndexOrThrow("local_date")
            val tIdx = c.getColumnIndexOrThrow("title")
            val bIdx = c.getColumnIndexOrThrow("body")
            val pmIdx = c.getColumnIndexOrThrow("primary_mood_code")
            val smIdx = c.getColumnIndexOrThrow("secondary_mood_code")
            val prIdx = c.getColumnIndexOrThrow("prompt_id")
            val lhIdx = c.getColumnIndexOrThrow("linked_habit_id")
            val intIdx = c.getColumnIndexOrThrow("intended_journal_entry_id")
            val srcIdx = c.getColumnIndexOrThrow("source_context")
            val crIdx = c.getColumnIndexOrThrow("created_at_utc_ms")
            val upIdx = c.getColumnIndexOrThrow("updated_at_utc_ms")

            while (c.moveToNext()) {
                val dateStr = LocalDateValidator.requireValid(c.getString(dIdx), "local_date")
                list.add(
                    JournalDraft(
                        draftId = c.getString(idIdx),
                        localDate = dateStr,
                        title = if (c.isNull(tIdx)) null else c.getString(tIdx),
                        body = c.getString(bIdx),
                        primaryMoodCode = if (c.isNull(pmIdx)) null else c.getString(pmIdx),
                        secondaryMoodCode = if (c.isNull(smIdx)) null else c.getString(smIdx),
                        promptId = if (c.isNull(prIdx)) null else c.getString(prIdx),
                        linkedHabitId = if (c.isNull(lhIdx)) null else c.getString(lhIdx),
                        intendedJournalEntryId = if (c.isNull(intIdx)) null else c.getString(intIdx),
                        sourceContext = c.getString(srcIdx),
                        createdAtUtcMs = c.getLong(crIdx),
                        updatedAtUtcMs = c.getLong(upIdx)
                    )
                )
            }
        }
        _journalDrafts.value = list
    }

    fun saveJournalDraftSync(
        draftId: String = "active_draft_${clock.todayLocal()}",
        localDate: LocalDate = clock.todayLocal(),
        title: String? = null,
        body: String = "",
        primaryMoodCode: String? = null,
        secondaryMoodCode: String? = null,
        promptId: String? = null,
        linkedHabitId: String? = null,
        sourceContext: String = "JOURNAL_COMPOSER"
    ): JournalDraft {
        val db = dbHelper.writableDatabase
        val nowMs = clock.nowUtcMs()
        val existing = _journalDrafts.value.find { it.draftId == draftId }
        val createdAt = existing?.createdAtUtcMs ?: nowMs
        val dateStr = LocalDateValidator.requireValid(localDate.toString(), "local_date")

        val validSecondary = if (primaryMoodCode != null && secondaryMoodCode != primaryMoodCode) secondaryMoodCode else null

        val cv = ContentValues().apply {
            put("draft_id", draftId)
            put("local_date", dateStr)
            if (!title.isNullOrBlank()) put("title", title.trim()) else putNull("title")
            put("body", body)
            if (primaryMoodCode != null) put("primary_mood_code", primaryMoodCode) else putNull("primary_mood_code")
            if (validSecondary != null) put("secondary_mood_code", validSecondary) else putNull("secondary_mood_code")
            if (!promptId.isNullOrBlank()) put("prompt_id", promptId) else putNull("prompt_id")
            if (!linkedHabitId.isNullOrBlank()) put("linked_habit_id", linkedHabitId) else putNull("linked_habit_id")
            if (existing?.intendedJournalEntryId != null) {
                put("intended_journal_entry_id", existing.intendedJournalEntryId)
            } else {
                putNull("intended_journal_entry_id")
            }
            put("source_context", sourceContext)
            put("created_at_utc_ms", createdAt)
            put("updated_at_utc_ms", maxOf(createdAt, nowMs))
        }
        db.insertWithOnConflict(
            DayseedDatabaseHelper.TABLE_JOURNAL_DRAFTS,
            null,
            cv,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        loadJournalDraftsSync(db)
        return _journalDrafts.value.first { it.draftId == draftId }
    }

    fun promoteDraftToJournalEntrySync(draftId: String): JournalEntry? {
        val db = dbHelper.writableDatabase
        val draft = _journalDrafts.value.find { it.draftId == draftId } ?: return null
        if (draft.body.isBlank()) return null

        val nowMs = clock.nowUtcMs()
        val intendedId = draft.intendedJournalEntryId ?: UUID.randomUUID().toString()

        if (draft.intendedJournalEntryId == null) {
            val prepCv = ContentValues().apply {
                put("intended_journal_entry_id", intendedId)
                put("updated_at_utc_ms", nowMs)
            }
            db.update(
                DayseedDatabaseHelper.TABLE_JOURNAL_DRAFTS,
                prepCv,
                "draft_id = ?",
                arrayOf(draftId)
            )
        }

        db.beginTransaction()
        try {
            val jCv = ContentValues().apply {
                put("journal_entry_id", intendedId)
                put("local_date", draft.localDate)
                put("body", draft.body.trim())
                if (!draft.title.isNullOrBlank()) put("title", draft.title.trim()) else putNull("title")
                if (draft.primaryMoodCode != null) put("primary_mood_code", draft.primaryMoodCode) else putNull("primary_mood_code")
                if (draft.secondaryMoodCode != null && draft.secondaryMoodCode != draft.primaryMoodCode) {
                    put("secondary_mood_code", draft.secondaryMoodCode)
                } else {
                    putNull("secondary_mood_code")
                }
                if (!draft.promptId.isNullOrBlank()) put("prompt_id", draft.promptId) else putNull("prompt_id")
                if (!draft.linkedHabitId.isNullOrBlank()) put("linked_habit_id", draft.linkedHabitId) else putNull("linked_habit_id")
                put("created_at_utc_ms", nowMs)
                put("updated_at_utc_ms", nowMs)
            }
            db.insertWithOnConflict(
                DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES,
                null,
                jCv,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            db.delete(DayseedDatabaseHelper.TABLE_JOURNAL_DRAFTS, "draft_id = ?", arrayOf(draftId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        recordObservedClockSync()
        refreshAllSync()
        return _journalEntries.value.find { it.journalEntryId == intendedId }
    }

    fun saveJournalEntrySync(entry: JournalEntry): JournalEntry {
        val db = dbHelper.writableDatabase
        val nowMs = clock.nowUtcMs()
        val dateStr = LocalDateValidator.requireValid(entry.localDate, "local_date")
        val existing = _journalEntries.value.find { it.journalEntryId == entry.journalEntryId }
        val createdAt = existing?.createdAtUtcMs ?: entry.createdAtUtcMs.takeIf { it > 0 } ?: nowMs
        val validSecondary = if (entry.primaryMoodCode != null && entry.secondaryMoodCode != entry.primaryMoodCode) {
            entry.secondaryMoodCode
        } else {
            null
        }

        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("journal_entry_id", entry.journalEntryId)
                put("local_date", dateStr)
                put("body", entry.body.trim())
                if (!entry.title.isNullOrBlank()) put("title", entry.title.trim()) else putNull("title")
                if (!entry.primaryMoodCode.isNullOrBlank()) put("primary_mood_code", entry.primaryMoodCode) else putNull("primary_mood_code")
                if (!validSecondary.isNullOrBlank()) put("secondary_mood_code", validSecondary) else putNull("secondary_mood_code")
                if (!entry.promptId.isNullOrBlank()) put("prompt_id", entry.promptId) else putNull("prompt_id")
                if (!entry.linkedHabitId.isNullOrBlank()) put("linked_habit_id", entry.linkedHabitId) else putNull("linked_habit_id")
                put("created_at_utc_ms", createdAt)
                put("updated_at_utc_ms", maxOf(createdAt, nowMs))
            }
            db.insertWithOnConflict(
                DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES,
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            db.delete(
                DayseedDatabaseHelper.TABLE_JOURNAL_DRAFTS,
                "local_date = ? AND source_context = 'JOURNAL_COMPOSER'",
                arrayOf(dateStr)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        recordObservedClockSync()
        refreshAllSync()
        return _journalEntries.value.first { it.journalEntryId == entry.journalEntryId }
    }

    suspend fun saveJournalEntry(entry: JournalEntry): JournalEntry = withContext(Dispatchers.IO) {
        saveJournalEntrySync(entry)
    }

    fun isHabitEligibleForRetrospectivePrompt(habitId: String, referenceDate: LocalDate = clock.todayLocal()): Boolean {
        if (!getSettingBoolean("retrospective_journal_prompts_enabled", true)) return false
        val dismissKey = "retrospective_prompt_dismissed_$habitId"
        val lastDismissedDateStr = _appSettings.value[dismissKey]?.valueText
        if (lastDismissedDateStr != null) {
            val lastDismissed = try { LocalDateValidator.parseStrict(lastDismissedDateStr) } catch (_: Exception) { null }
            if (lastDismissed != null && ChronoUnit.DAYS.between(lastDismissed, referenceDate) < 7) {
                return false
            }
        }
        var missedCount = 0
        for (i in 1..7) {
            val d = referenceDate.minusDays(i.toLong())
            if (evaluateDerivedDayStatus(habitId, d, referenceToday = referenceDate) == DerivedDayStatus.DERIVED_MISSED) {
                missedCount++
            }
        }
        return missedCount >= 2
    }

    fun dismissRetrospectivePromptSync(habitId: String, date: LocalDate = clock.todayLocal()) {
        setSettingSync("retrospective_prompt_dismissed_$habitId", "STRING", date.toString())
    }

    // =========================================================================
    // MOOD INSIGHTS & PERIOD COMPARISONS (D-096..D-102, AC-009)
    // =========================================================================

    fun computeMoodInsights(
        periodKind: String = "WEEK",
        referenceDate: LocalDate = clock.todayLocal(),
        selectedMoodFilter: String? = null
    ): MoodInsightsResult {
        val weekStart = getWeekStartDay(referenceDate)
        val (currentStart, currentEnd, prevStart, prevEnd) = if (periodKind.uppercase() == "MONTH") {
            val ym = YearMonth.from(referenceDate)
            val prevYm = ym.minusMonths(1)
            QuadrupleDate(ym.atDay(1), ym.atEndOfMonth(), prevYm.atDay(1), prevYm.atEndOfMonth())
        } else {
            val (ws, we) = WeekBoundaryHelper.weekBounds(referenceDate, weekStart)
            val (pws, pwe) = WeekBoundaryHelper.previousWeekBounds(referenceDate, weekStart)
            QuadrupleDate(ws, we, pws, pwe)
        }

        val currentEntries = _journalEntries.value.filter { e ->
            val d = try { LocalDateValidator.parseStrict(e.localDate) } catch (_: Exception) { null }
            d != null && !d.isBefore(currentStart) && !d.isAfter(currentEnd)
        }
        val prevEntries = _journalEntries.value.filter { e ->
            val d = try { LocalDateValidator.parseStrict(e.localDate) } catch (_: Exception) { null }
            d != null && !d.isBefore(prevStart) && !d.isAfter(prevEnd)
        }

        val currentWithMood = currentEntries.filter { !it.primaryMoodCode.isNullOrBlank() }
        val prevWithMood = prevEntries.filter { !it.primaryMoodCode.isNullOrBlank() }

        val currentCounts = currentWithMood.groupingBy { it.primaryMoodCode!! }.eachCount()
        val prevCounts = prevWithMood.groupingBy { it.primaryMoodCode!! }.eachCount()

        val allMoodCodes = (currentCounts.keys + prevCounts.keys).distinct()
        val distribution = allMoodCodes.map { code ->
            val count = currentCounts[code] ?: 0
            val pct = if (currentWithMood.isNotEmpty()) {
                (count * 100) / currentWithMood.size
            } else {
                0
            }
            val meta = MoodCatalog.findByCode(code)
            MoodDistributionItem(
                moodCode = code,
                label = meta?.label ?: code,
                symbol = meta?.symbol ?: "🌱",
                count = count,
                percentage = pct,
                previousPeriodCount = prevCounts[code] ?: 0
            )
        }.sortedWith(compareByDescending<MoodDistributionItem> { it.count }.thenBy { it.label })

        val matchingEntries = if (selectedMoodFilter.isNullOrBlank()) {
            currentEntries
        } else {
            currentEntries.filter {
                it.primaryMoodCode.equals(selectedMoodFilter, ignoreCase = true) ||
                    it.secondaryMoodCode.equals(selectedMoodFilter, ignoreCase = true)
            }
        }

        val label = if (periodKind.uppercase() == "MONTH") {
            "This Month (${currentStart.month.name.lowercase().replaceFirstChar { it.uppercase() }})"
        } else {
            "This Week ($currentStart – $currentEnd)"
        }

        return MoodInsightsResult(
            periodLabel = label,
            startDate = currentStart,
            endDate = currentEnd,
            totalEntriesWithPrimaryMood = currentWithMood.size,
            previousPeriodTotalEntries = prevWithMood.size,
            distribution = distribution,
            matchingEntries = matchingEntries
        )
    }

    private data class QuadrupleDate(
        val first: LocalDate,
        val second: LocalDate,
        val third: LocalDate,
        val fourth: LocalDate
    )

    // =========================================================================
    // COMEBACK 14-DAY & 30-DAY ENGINE (D-047..D-067, D-206, AC-011)
    // =========================================================================

    fun findLastRecordedActivityDate(): LocalDate? {
        val dates = mutableListOf<LocalDate>()
        _habitEntries.value.filter { it.valueUnits > 0 }.forEach {
            try { dates.add(LocalDateValidator.parseStrict(it.localDate)) } catch (_: Exception) {}
        }
        _habitDayOutcomes.value.forEach {
            try { dates.add(LocalDateValidator.parseStrict(it.localDate)) } catch (_: Exception) {}
        }
        _journalEntries.value.forEach {
            try { dates.add(LocalDateValidator.parseStrict(it.localDate)) } catch (_: Exception) {}
        }
        return dates.maxOrNull()
    }

    fun evaluateComebackSync(referenceDate: LocalDate = clock.todayLocal()): ComebackInfo? {
        if (isClockUncertainSync()) {
            _comebackInfo.value = null
            return null
        }

        val lastActivityDate = findLastRecordedActivityDate()
            ?: _appSettings.value["last_opened_date"]?.valueText?.let {
                try { LocalDateValidator.parseStrict(it) } catch (_: Exception) { null }
            }

        if (lastActivityDate == null) {
            setSettingSync("last_opened_date", "STRING", referenceDate.toString())
            _comebackInfo.value = null
            return null
        }

        val daysAway = ChronoUnit.DAYS.between(lastActivityDate, referenceDate).toInt()
        if (daysAway < 14) {
            setSettingSync("last_opened_date", "STRING", referenceDate.toString())
            _comebackInfo.value = null
            return null
        }

        val tier = if (daysAway >= 30) ComebackTier.DAYS_30 else ComebackTier.DAYS_14
        val episodeKey = "${lastActivityDate}_${tier.name}"
        val handledEpisode = _appSettings.value["comeback_handled_episode"]?.valueText

        if (handledEpisode == episodeKey) {
            _comebackInfo.value = null
            return null
        }

        val info = when (tier) {
            ComebackTier.DAYS_14 -> ComebackInfo(
                daysAway = daysAway,
                tier = ComebackTier.DAYS_14,
                episodeKey = episodeKey,
                title = "Welcome back — your past progress is still here.",
                message = "It has been $daysAway days since your last entry. Nothing has been reset. You can resume your habits as they are, lower a target, or pause anything that doesn't fit right now.",
                primaryActionLabel = "Review & Resume Habits",
                secondaryActionLabel = "Continue to Today"
            )
            ComebackTier.DAYS_30 -> ComebackInfo(
                daysAway = daysAway,
                tier = ComebackTier.DAYS_30,
                episodeKey = episodeKey,
                title = "A fresh season, with all your history intact.",
                message = "It has been $daysAway days since you last logged. Your earlier moments remain yours. Take a moment to reflect, restart with smaller targets, or pause habits you aren't tending right now.",
                primaryActionLabel = "Adaptive Restart & Reflection",
                secondaryActionLabel = "Keep Current Setup"
            )
        }
        _comebackInfo.value = info
        return info
    }

    fun dismissComeback() {
        val current = _comebackInfo.value
        if (current != null) {
            setSettingSync("comeback_handled_episode", "STRING", current.episodeKey)
        }
        setSettingSync("last_opened_date", "STRING", clock.todayLocal().toString())
        _comebackInfo.value = null
    }

    fun applyComebackAdjustmentsSync(
        resumeHabitIds: Set<String>,
        pauseHabitIds: Set<String>,
        loweredTargetByHabitId: Map<String, Int> = emptyMap(),
        comebackReflectionBody: String? = null,
        saveReflectionImmediately: Boolean = true
    ) {
        val today = clock.todayLocal()
        for (h in _habits.value) {
            when {
                pauseHabitIds.contains(h.habitId) -> {
                    saveHabitSync(
                        habit = h.copy(lifecycleState = HabitLifecycleState.PAUSED),
                        effectiveDateOverride = today,
                        applyTodayWhenLogged = true
                    )
                }
                resumeHabitIds.contains(h.habitId) -> {
                    val lowered = loweredTargetByHabitId[h.habitId]
                    saveHabitSync(
                        habit = h.copy(
                            lifecycleState = HabitLifecycleState.ACTIVE,
                            targetValue = lowered ?: h.targetValue
                        ),
                        effectiveDateOverride = today,
                        applyTodayWhenLogged = true
                    )
                }
            }
        }

        if (!comebackReflectionBody.isNullOrBlank()) {
            val draft = saveJournalDraftSync(
                draftId = "comeback_30d_${today}",
                localDate = today,
                title = "Returning Reflection",
                body = comebackReflectionBody.trim(),
                promptId = "prompt_comeback",
                sourceContext = "COMEBACK_30D"
            )
            if (saveReflectionImmediately) {
                promoteDraftToJournalEntrySync(draft.draftId)
            }
        }

        dismissComeback()
    }

    // =========================================================================
    // REMINDERS & TIMING SUGGESTIONS (D-068..D-095, D-192)
    // =========================================================================

    fun evaluateTodayRemindersSync(targetDate: LocalDate = clock.todayLocal()): List<ReminderEvaluationResult> {
        // One serialised write transaction for the entire day's evaluation: the ledger reads,
        // the shared 3/day cap count (DELIVERED + SCHEDULED) and the reservation writes become a
        // single atomic step even across separate database connections (two alarms firing in
        // parallel). Contention is retried a few times before failing honestly.
        val quietStart = getSettingString("quiet_hours_start", ReminderEngine.DEFAULT_QUIET_START)
        val quietEnd = getSettingString("quiet_hours_end", ReminderEngine.DEFAULT_QUIET_END)
        val dailyCap = getSettingInt("daily_reminder_cap", ReminderEngine.DEFAULT_DAILY_CAP)
        val nowMs = clock.nowUtcMs()

        return withImmediateTransaction {
            val results = mutableListOf<ReminderEvaluationResult>()
            for (h in _habits.value) {
                val eff = getEffectiveHabitVersion(h.habitId, targetDate) ?: continue
                val logged = hasQualifyingActivityOnDate(h.habitId, targetDate) ||
                    getExceptionForHabitOnDate(h.habitId, targetDate) != null
                val (scheduledOpportunity, weeklyTargetMet) =
                    computeScheduledOpportunityAndWeeklyMet(eff, targetDate)
                results.add(
                    ReminderEngine.evaluateHabitReminder(
                        db = dbHelper.writableDatabase,
                        habitVersion = eff,
                        targetDate = targetDate,
                        isScheduledOpportunity = scheduledOpportunity,
                        weeklyTargetMet = weeklyTargetMet,
                        alreadyLoggedToday = logged,
                        quietStartLocal = quietStart,
                        quietEndLocal = quietEnd,
                        dailyCap = dailyCap,
                        nowUtcMs = nowMs
                    )
                )
            }
            results
        }
    }

    /**
     * D-229 precomputation, shared by [evaluateTodayRemindersSync] and the
     * [reconcilePendingReminderPostsSync] crash-recovery sweep so both use identical semantics:
     * the canonical schedule domain the streak engine uses (R-WR-01/04) and the weekly-progress
     * semantics the Today snapshot uses (R-WR-02 modified). REST_DAY/effort-aware TINY are
     * logging affordances, not schedule modifiers (R-WR-05): they feed neither gate.
     */
    internal fun computeScheduledOpportunityAndWeeklyMet(
        eff: HabitVersion,
        date: LocalDate
    ): Pair<Boolean, Boolean> {
        val scheduledOpportunity = StreakAndGroveEngine.isScheduledOpportunityOnDate(eff, date)
        var weeklyTargetMet = false
        if (scheduledOpportunity && eff.targetPeriod == TargetPeriod.WEEK) {
            val weeklySnapshot = StreakAndGroveEngine.computeWeeklyProgressSnapshot(
                versions = _habitVersions.value.filter { it.habitId == eff.habitId },
                entries = _habitEntries.value.filter { it.habitId == eff.habitId },
                outcomes = _habitDayOutcomes.value.filter { it.habitId == eff.habitId },
                weekStartHistory = _weekStartHistory.value,
                evaluationDate = date,
                fallbackVersion = eff
            )
            weeklyTargetMet = weeklySnapshot.weekTotalUnits >= weeklySnapshot.weekTargetUnits
        }
        return scheduledOpportunity to weeklyTargetMet
    }

    /**
     * Runs [block] inside a serialised database write transaction, retrying on transient
     * `SQLiteDatabaseLockedException`/SQLITE_BUSY contention from other connections or
     * processes. Never relies on an in-memory mutex alone: the SQLite file-level write lock
     * is the cross-connection protection.
     *
     * Transaction mode: `SQLiteDatabase.beginTransactionNonExclusive()` issues SQLite's
     * `BEGIN IMMEDIATE` (Android framework contract; `beginTransaction()` issues
     * `BEGIN EXCLUSIVE`), so the write lock is acquired upfront and the ledger read,
     * daily-cap count and reservation below are one serialised step across connections.
     */
    private fun <T> withImmediateTransaction(block: () -> T): T {
        val db = dbHelper.writableDatabase
        var attempt = 0
        while (true) {
            attempt++
            try {
                db.beginTransactionNonExclusive()
                try {
                    val out = block()
                    db.setTransactionSuccessful()
                    return out
                } finally {
                    db.endTransaction()
                }
            } catch (e: Exception) {
                val locked = e is SQLiteDatabaseLockedException ||
                    e.cause is SQLiteDatabaseLockedException ||
                    e.message?.lowercase()?.contains("locked") == true ||
                    e.message?.contains("SQLITE_BUSY") == true
                if (!locked || attempt >= 8) throw e
                Thread.sleep(40L * attempt)
            }
        }
    }

    fun getSuggestedReminderTimeForHabit(habitId: String, lookbackDays: Int = 28): String? {
        val cutoffDate = clock.todayLocal().minusDays(lookbackDays.toLong()).toString()
        val timestamps = mutableListOf<Long>()
        _habitEntries.value
            .filter { it.habitId == habitId && it.localDate >= cutoffDate && it.valueUnits > 0 }
            .forEach { timestamps.add(it.createdAtUtcMs) }
        _habitDayOutcomes.value
            .filter { it.habitId == habitId && it.localDate >= cutoffDate }
            .forEach { timestamps.add(it.createdAtUtcMs) }

        val quietStart = getSettingString("quiet_hours_start", ReminderEngine.DEFAULT_QUIET_START)
        val quietEnd = getSettingString("quiet_hours_end", ReminderEngine.DEFAULT_QUIET_END)
        return ReminderEngine.suggestOptimalReminderTime(timestamps, clock.zoneId(), quietStart, quietEnd)
    }

    // =========================================================================
    // READABLE EXPORT, PORTABLE BACKUP & RESTORE (D-105..D-107, D-200, D-202, §9.2)
    // =========================================================================

    private fun loadRestoreConflictsSync(db: SQLiteDatabase) {
        val list = mutableListOf<RestoreConflict>()
        db.query(
            DayseedDatabaseHelper.TABLE_RESTORE_CONFLICTS,
            null, null, null, null, null,
            "created_at_utc_ms ASC"
        ).use { c ->
            val idIdx = c.getColumnIndexOrThrow("conflict_id")
            val sIdx = c.getColumnIndexOrThrow("restore_session_id")
            val tIdx = c.getColumnIndexOrThrow("record_type")
            val kIdx = c.getColumnIndexOrThrow("record_key")
            val curIdx = c.getColumnIndexOrThrow("current_payload_json")
            val bakIdx = c.getColumnIndexOrThrow("backup_payload_json")
            val crIdx = c.getColumnIndexOrThrow("created_at_utc_ms")
            while (c.moveToNext()) {
                list.add(
                    RestoreConflict(
                        conflictId = c.getString(idIdx),
                        restoreSessionId = c.getString(sIdx),
                        recordType = c.getString(tIdx),
                        recordKey = c.getString(kIdx),
                        currentPayloadJson = c.getString(curIdx),
                        backupPayloadJson = c.getString(bakIdx),
                        createdAtUtcMs = c.getLong(crIdx)
                    )
                )
            }
        }
        _restoreConflicts.value = list
    }

    fun exportReadableJournalsSync(format: JournalExportFormat): String {
        val habitsById = _habits.value.associate { it.habitId to it.name }
        return BackupAndExportManager.exportSavedJournals(_journalEntries.value, habitsById, format)
    }

    suspend fun exportReadableJournals(format: JournalExportFormat): String = withContext(Dispatchers.IO) {
        exportReadableJournalsSync(format)
    }

    /**
     * Generates a complete versioned portable backup (.dayseed.json) containing all v3 persistent tables.
     */
    fun exportPortableBackupSync(password: String? = null): String {
        val root = JSONObject()
        root.put("backup_kind", "dayseed_portable_backup")
        root.put("format_version", BackupAndExportManager.BACKUP_FORMAT_VERSION)
        root.put("source_schema_version", DayseedDatabaseHelper.DATABASE_VERSION)
        root.put("exported_at_utc_ms", clock.nowUtcMs())

        root.put("habits", JSONArray().apply {
            for (h in _habits.value) {
                put(habitToJson(h))
            }
        })

        root.put("habit_versions", JSONArray().apply {
            for (v in _habitVersions.value) {
                put(habitVersionToJson(v))
            }
        })

        root.put("habit_entries", JSONArray().apply {
            for (e in _habitEntries.value) {
                put(habitEntryToJson(e))
            }
        })

        root.put("habit_day_outcomes", JSONArray().apply {
            for (o in _habitDayOutcomes.value) {
                put(habitDayOutcomeToJson(o))
            }
        })

        root.put("habit_date_notes", JSONArray().apply {
            for (n in _habitDateNotes.value) {
                put(habitDateNoteToJson(n))
            }
        })

        root.put("habit_date_exceptions", JSONArray().apply {
            for (ex in _habitDateExceptions.value) {
                put(habitDateExceptionToJson(ex))
            }
        })

        root.put("habit_streak_runs", JSONArray().apply {
            for (run in _habitStreakRuns.value) {
                put(habitStreakRunToJson(run))
            }
        })

        root.put("week_start_history", JSONArray().apply {
            for (w in _weekStartHistory.value) {
                put(weekStartPolicyToJson(w))
            }
        })

        root.put("journal_entries", JSONArray().apply {
            for (j in _journalEntries.value) {
                put(journalEntryToJson(j))
            }
        })

        root.put("journal_drafts", JSONArray().apply {
            for (d in _journalDrafts.value) {
                put(journalDraftToJson(d))
            }
        })

        root.put("app_settings", JSONArray().apply {
            for (s in _appSettings.value.values) {
                put(appSettingToJson(s))
            }
        })

        val plainJson = root.toString(2)
        return if (!password.isNullOrEmpty()) {
            BackupAndExportManager.encryptBackupPayload(plainJson, password)
        } else {
            plainJson
        }
    }

    suspend fun exportDataAsJson(password: String? = null): String = withContext(Dispatchers.IO) {
        exportPortableBackupSync(password)
    }

    /**
     * Restores a portable backup (v2 or v3) using REPLACE or MERGE mode (D-106, D-202, §9.2):
     * Validates calendar dates and integer ranges before mutating live tables.
     */
    fun restorePortableBackupSync(
        rawBackupPayload: String,
        restoreMode: String = "REPLACE",
        password: String? = null
    ): RestoreSession {
        require(restoreMode in listOf("REPLACE", "MERGE")) { "Invalid restoreMode: $restoreMode" }
        val unwrappedJson = BackupAndExportManager.unwrapBackupPayload(rawBackupPayload, password)
        val root = JSONObject(unwrappedJson)
        val formatVersion = root.optInt("format_version", 0)
        val sourceSchemaVersion = root.optInt("source_schema_version", 0)
        require(formatVersion == BackupAndExportManager.BACKUP_FORMAT_VERSION && sourceSchemaVersion in listOf(2, 3)) {
            "Unsupported or corrupted backup format (format_version=$formatVersion, source_schema_version=$sourceSchemaVersion)"
        }

        // Pre-validate dates and numeric bounds in backup payload before touching live tables
        validateBackupJsonPayload(root)

        val nowMs = clock.nowUtcMs()
        val safetyBackupJson = exportPortableBackupSync(password = null)
        val safetyFile = File(appContext.filesDir, "pre_restore_safety_$nowMs.dayseed.json")
        safetyFile.writeText(safetyBackupJson)
        val verifiedSafety = safetyFile.exists() && JSONObject(safetyFile.readText()).optString("backup_kind") == "dayseed_portable_backup"
        check(verifiedSafety) { "Failed to verify pre-restore safety backup" }

        val sessionId = UUID.randomUUID().toString()
        val db = dbHelper.writableDatabase
        var finalState = "COMPLETED"

        db.beginTransaction()
        try {
            val sessionCv = ContentValues().apply {
                put("restore_session_id", sessionId)
                put("restore_mode", restoreMode)
                put("format_version", formatVersion)
                put("source_schema_version", sourceSchemaVersion)
                put("session_state", "VALIDATED")
                put("started_at_utc_ms", nowMs)
                put("updated_at_utc_ms", nowMs)
                put("manifest_json", JSONObject().apply {
                    put("habits_count", root.optJSONArray("habits")?.length() ?: 0)
                    put("journal_entries_count", root.optJSONArray("journal_entries")?.length() ?: 0)
                }.toString())
                put("safety_backup_ref", safetyFile.absolutePath)
                put("safety_backup_verified", 1)
            }
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_RESTORE_SESSIONS, null, sessionCv)

            if (restoreMode == "REPLACE") {
                db.delete(DayseedDatabaseHelper.TABLE_RESTORE_CONFLICTS, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_JOURNAL_DRAFTS, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_HABIT_STREAK_RUNS, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_HABIT_DATE_NOTES, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_HABIT_ENTRIES, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_HABIT_VERSIONS, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_HABITS, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_WEEK_START_HISTORY, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_APP_SETTINGS, null, null)

                importBackupArraysDirect(db, root, nowMs)
                finalState = "COMPLETED"
            } else {
                val conflictCount = mergeBackupWithConflictDetection(db, sessionId, root, nowMs)
                finalState = if (conflictCount > 0) "AWAITING_CONFLICT_RESOLUTION" else "COMPLETED"
            }

            val updateSessionCv = ContentValues().apply {
                put("session_state", finalState)
                put("updated_at_utc_ms", nowMs)
            }
            db.update(
                DayseedDatabaseHelper.TABLE_RESTORE_SESSIONS,
                updateSessionCv,
                "restore_session_id = ?",
                arrayOf(sessionId)
            )

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        refreshAllSync()
        rescheduleReminderAlarms()
        return RestoreSession(
            restoreSessionId = sessionId,
            restoreMode = restoreMode,
            formatVersion = formatVersion,
            sourceSchemaVersion = sourceSchemaVersion,
            sessionState = finalState,
            startedAtUtcMs = nowMs,
            updatedAtUtcMs = nowMs,
            manifestJson = "{}",
            safetyBackupRef = safetyFile.absolutePath,
            safetyBackupVerified = 1
        )
    }

    private fun validateBackupJsonPayload(root: JSONObject) {
        val dateArrays = listOf(
            "habit_versions" to "effective_from_local_date",
            "habit_entries" to "local_date",
            "habit_day_outcomes" to "local_date",
            "habit_date_notes" to "local_date",
            "habit_date_exceptions" to "local_date",
            "habit_streak_runs" to "start_local_date",
            "habit_streak_runs" to "end_local_date",
            "journal_entries" to "local_date",
            "journal_drafts" to "local_date",
            "week_start_history" to "effective_from_local_date"
        )
        for ((arrName, fieldName) in dateArrays) {
            val arr = root.optJSONArray(arrName) ?: continue
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val rawDate = o.optString(fieldName, "")
                LocalDateValidator.requireValid(rawDate, "$arrName.$fieldName")
            }
        }
        val runsArr = root.optJSONArray("habit_streak_runs")
        if (runsArr != null) {
            for (i in 0 until runsArr.length()) {
                val o = runsArr.getJSONObject(i)
                if (!o.isNull("broken_on_local_date") && o.has("broken_on_local_date")) {
                    val brokenDate = o.optString("broken_on_local_date", "")
                    if (brokenDate.isNotEmpty()) {
                        LocalDateValidator.requireValid(brokenDate, "habit_streak_runs.broken_on_local_date")
                    }
                }
            }
        }
        val entriesArr = root.optJSONArray("habit_entries")
        if (entriesArr != null) {
            for (i in 0 until entriesArr.length()) {
                val o = entriesArr.getJSONObject(i)
                val rawVal = o.getLong("value_units")
                if (rawVal < 0L || rawVal > Int.MAX_VALUE.toLong()) {
                    throw DayseedNumericRangeException("Invalid value_units $rawVal in backup habit_entries.")
                }
            }
        }
    }

    private fun computeEarliestBackupLocalDate(root: JSONObject): String {
        val candidates = mutableListOf<String>()
        val dateArrays = listOf(
            "habit_versions" to "effective_from_local_date",
            "habit_entries" to "local_date",
            "habit_day_outcomes" to "local_date",
            "habit_date_notes" to "local_date",
            "habit_date_exceptions" to "local_date",
            "journal_entries" to "local_date",
            "journal_drafts" to "local_date"
        )
        for ((arrName, fieldName) in dateArrays) {
            val arr = root.optJSONArray(arrName) ?: continue
            for (i in 0 until arr.length()) {
                val d = arr.getJSONObject(i).optString(fieldName, "")
                if (LocalDateValidator.isValidStrict(d)) {
                    candidates.add(d)
                }
            }
        }
        return candidates.minOrNull() ?: "1970-01-01"
    }

    private fun habitExistsInDb(db: SQLiteDatabase, habitId: String): Boolean {
        db.rawQuery(
            "SELECT 1 FROM ${DayseedDatabaseHelper.TABLE_HABITS} WHERE habit_id = ? LIMIT 1",
            arrayOf(habitId)
        ).use { c ->
            return c.moveToFirst()
        }
    }

    private fun hasRecordedActivityInDb(db: SQLiteDatabase, habitId: String, localDate: String): Boolean {
        db.rawQuery(
            """
            SELECT 1 FROM ${DayseedDatabaseHelper.TABLE_HABIT_ENTRIES} WHERE habit_id = ? AND local_date = ?
            UNION ALL
            SELECT 1 FROM ${DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES} WHERE habit_id = ? AND local_date = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(habitId, localDate, habitId, localDate)
        ).use { c ->
            return c.moveToFirst()
        }
    }

    private fun importBackupArraysDirect(db: SQLiteDatabase, root: JSONObject, nowMs: Long) {
        val habitsArr = root.optJSONArray("habits") ?: JSONArray()
        for (i in 0 until habitsArr.length()) {
            val o = habitsArr.getJSONObject(i)
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABITS, null, jsonToHabitCv(o))
        }

        val versionsArr = root.optJSONArray("habit_versions") ?: JSONArray()
        for (i in 0 until versionsArr.length()) {
            val o = versionsArr.getJSONObject(i)
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_VERSIONS, null, jsonToHabitVersionCv(o))
        }

        val entriesArr = root.optJSONArray("habit_entries") ?: JSONArray()
        for (i in 0 until entriesArr.length()) {
            val o = entriesArr.getJSONObject(i)
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_ENTRIES, null, jsonToHabitEntryCv(o))
        }

        val outcomesArr = root.optJSONArray("habit_day_outcomes") ?: JSONArray()
        for (i in 0 until outcomesArr.length()) {
            val o = outcomesArr.getJSONObject(i)
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES, null, jsonToHabitDayOutcomeCv(o))
        }

        val notesArr = root.optJSONArray("habit_date_notes") ?: JSONArray()
        for (i in 0 until notesArr.length()) {
            val o = notesArr.getJSONObject(i)
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_DATE_NOTES, null, jsonToHabitDateNoteCv(o))
        }

        val exceptionsArr = root.optJSONArray("habit_date_exceptions") ?: JSONArray()
        for (i in 0 until exceptionsArr.length()) {
            val o = exceptionsArr.getJSONObject(i)
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS, null, jsonToHabitDateExceptionCv(o))
        }

        val runsArr = root.optJSONArray("habit_streak_runs") ?: JSONArray()
        val seenHabitStartDates = mutableSetOf<Pair<String, String>>()
        val seenActiveHabits = mutableSetOf<String>()
        for (i in 0 until runsArr.length()) {
            val o = runsArr.getJSONObject(i)
            val hId = o.getString("habit_id")
            check(habitExistsInDb(db, hId)) {
                "Portable backup contains habit_streak_run for missing parent habit '$hId'; REPLACE restore was rejected."
            }
            val startDate = LocalDateValidator.requireValid(o.getString("start_local_date"), "habit_streak_runs.start_local_date")
            val isActive = o.optBoolean("is_active", o.optInt("is_active", 0) == 1)
            if (!seenHabitStartDates.add(hId to startDate)) {
                throw IllegalArgumentException("Duplicate habit_streak_run for ($hId, $startDate) in backup.")
            }
            if (isActive && !seenActiveHabits.add(hId)) {
                throw IllegalArgumentException("Multiple active habit_streak_runs for habit $hId in backup.")
            }
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_STREAK_RUNS, null, jsonToHabitStreakRunCv(o))
        }

        val journalsArr = root.optJSONArray("journal_entries") ?: JSONArray()
        for (i in 0 until journalsArr.length()) {
            val o = journalsArr.getJSONObject(i)
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES, null, jsonToJournalEntryCv(o))
        }

        val draftsArr = root.optJSONArray("journal_drafts") ?: JSONArray()
        for (i in 0 until draftsArr.length()) {
            val o = draftsArr.getJSONObject(i)
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_JOURNAL_DRAFTS, null, jsonToJournalDraftCv(o))
        }

        val settingsArr = root.optJSONArray("app_settings") ?: JSONArray()
        for (i in 0 until settingsArr.length()) {
            val o = settingsArr.getJSONObject(i)
            db.insertWithOnConflict(
                DayseedDatabaseHelper.TABLE_APP_SETTINGS,
                null,
                jsonToAppSettingCv(o),
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }

        val weekHistoryArr = root.optJSONArray("week_start_history")
        if (weekHistoryArr != null && weekHistoryArr.length() > 0) {
            for (i in 0 until weekHistoryArr.length()) {
                val o = weekHistoryArr.getJSONObject(i)
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_WEEK_START_HISTORY,
                    null,
                    jsonToWeekStartPolicyCv(o),
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
        } else {
            // Seed baseline week_start_history row for legacy v2 backups using earliest restored date
            var weekStartIso = 1
            db.rawQuery("SELECT value_text FROM ${DayseedDatabaseHelper.TABLE_APP_SETTINGS} WHERE setting_key = 'week_start' LIMIT 1", null).use { c ->
                if (c.moveToFirst()) {
                    val v = c.getString(0).trim()
                    weekStartIso = when {
                        v.equals("SUNDAY", ignoreCase = true) || v == "7" -> 7
                        v.toIntOrNull() in 1..7 -> v.toInt()
                        else -> 1
                    }
                }
            }
            val earliestDate = computeEarliestBackupLocalDate(root)
            db.insertWithOnConflict(
                DayseedDatabaseHelper.TABLE_WEEK_START_HISTORY,
                null,
                ContentValues().apply {
                    put("effective_from_local_date", earliestDate)
                    put("week_start_iso_day", weekStartIso)
                    put("is_legacy_baseline", 1)
                    put("created_at_utc_ms", nowMs)
                },
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }
    }

    private fun mergeBackupWithConflictDetection(
        db: SQLiteDatabase,
        sessionId: String,
        root: JSONObject,
        nowMs: Long
    ): Int {
        var conflicts = 0

        // 1. Habits
        val habitsArr = root.optJSONArray("habits") ?: JSONArray()
        for (i in 0 until habitsArr.length()) {
            val o = habitsArr.getJSONObject(i)
            val hId = o.getString("habit_id")
            val existing = _habits.value.find { it.habitId == hId }
            if (existing == null) {
                db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABITS, null, jsonToHabitCv(o))
            } else {
                val backupCategory = if (o.isNull("category") || !o.has("category")) null else o.getString("category")
                val backupGoalDays = if (o.isNull("lifetime_goal_days") || !o.has("lifetime_goal_days")) null else o.getInt("lifetime_goal_days")
                if (existing.category != backupCategory || existing.lifetimeGoalDays != backupGoalDays) {
                    insertConflictRow(
                        db = db,
                        sessionId = sessionId,
                        recordType = "habit",
                        recordKey = hId,
                        currentPayload = habitToJson(existing).toString(),
                        backupPayload = o.toString(),
                        nowMs = nowMs
                    )
                    conflicts++
                }
            }
        }

        // 2. Habit Versions
        val versionsArr = root.optJSONArray("habit_versions") ?: JSONArray()
        for (i in 0 until versionsArr.length()) {
            val o = versionsArr.getJSONObject(i)
            val hId = o.getString("habit_id")
            val effDate = o.getString("effective_from_local_date")
            val existing = _habitVersions.value.find { it.habitId == hId && it.effectiveFromLocalDate == effDate }
            val normalizedBackupCv = jsonToHabitVersionCv(o)
            val backupModeName = normalizedBackupCv.getAsString("tracking_mode")
            val backupSchedName = normalizedBackupCv.getAsString("schedule_kind")
            val backupTargetVal = normalizedBackupCv.getAsInteger("target_value")
            if (existing == null) {
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_HABIT_VERSIONS,
                    null,
                    normalizedBackupCv,
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            } else if (
                existing.name != o.getString("name") ||
                existing.trackingMode.canonical().name != backupModeName ||
                existing.scheduleKind.name != backupSchedName ||
                existing.targetValue != backupTargetVal
            ) {
                insertConflictRow(
                    db = db,
                    sessionId = sessionId,
                    recordType = "habit_version",
                    recordKey = "$hId:$effDate",
                    currentPayload = habitVersionToJson(existing).toString(),
                    backupPayload = o.toString(),
                    nowMs = nowMs
                )
                conflicts++
            }
        }

        // 3. Habit Day Outcomes
        val outcomesArr = root.optJSONArray("habit_day_outcomes") ?: JSONArray()
        for (i in 0 until outcomesArr.length()) {
            val o = outcomesArr.getJSONObject(i)
            val hId = o.getString("habit_id")
            val dStr = o.getString("local_date")
            val existing = _habitDayOutcomes.value.find { it.habitId == hId && it.localDate == dStr }
            val backupEffort = if (o.isNull("effort_level") || !o.has("effort_level")) null else o.getString("effort_level")
            if (existing == null) {
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES,
                    null,
                    jsonToHabitDayOutcomeCv(o),
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            } else if (existing.outcomeKind != o.getString("outcome_kind") || existing.effortLevel != backupEffort) {
                insertConflictRow(
                    db = db,
                    sessionId = sessionId,
                    recordType = "habit_day_outcome",
                    recordKey = "$hId:$dStr",
                    currentPayload = habitDayOutcomeToJson(existing).toString(),
                    backupPayload = o.toString(),
                    nowMs = nowMs
                )
                conflicts++
            }
        }

        // 4. Habit Entries
        val entriesArr = root.optJSONArray("habit_entries") ?: JSONArray()
        for (i in 0 until entriesArr.length()) {
            val o = entriesArr.getJSONObject(i)
            val eId = o.getString("entry_id")
            val existing = _habitEntries.value.find { it.entryId == eId }
            val backupNote = if (o.isNull("entry_note") || !o.has("entry_note")) "" else o.getString("entry_note")
            if (existing == null) {
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_HABIT_ENTRIES,
                    null,
                    jsonToHabitEntryCv(o),
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            } else if (existing.valueUnits != o.getInt("value_units") || (existing.entryNote ?: "") != backupNote) {
                insertConflictRow(
                    db = db,
                    sessionId = sessionId,
                    recordType = "habit_entry",
                    recordKey = eId,
                    currentPayload = habitEntryToJson(existing).toString(),
                    backupPayload = o.toString(),
                    nowMs = nowMs
                )
                conflicts++
            }
        }

        // 5. Habit Date Notes
        val notesArr = root.optJSONArray("habit_date_notes") ?: JSONArray()
        for (i in 0 until notesArr.length()) {
            val o = notesArr.getJSONObject(i)
            val hId = o.getString("habit_id")
            val dStr = o.getString("local_date")
            val existing = _habitDateNotes.value.find { it.habitId == hId && it.localDate == dStr }
            if (existing == null) {
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_HABIT_DATE_NOTES,
                    null,
                    jsonToHabitDateNoteCv(o),
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            } else if (existing.body != o.getString("body")) {
                insertConflictRow(
                    db = db,
                    sessionId = sessionId,
                    recordType = "habit_date_note",
                    recordKey = "$hId:$dStr",
                    currentPayload = habitDateNoteToJson(existing).toString(),
                    backupPayload = o.toString(),
                    nowMs = nowMs
                )
                conflicts++
            }
        }

        // 6. Habit Date Exceptions (v3)
        val exceptionsArr = root.optJSONArray("habit_date_exceptions") ?: JSONArray()
        for (i in 0 until exceptionsArr.length()) {
            val o = exceptionsArr.getJSONObject(i)
            val hId = o.getString("habit_id")
            val dStr = LocalDateValidator.requireValid(o.getString("local_date"), "habit_date_exceptions.local_date")
            val existing = _habitDateExceptions.value.find { it.habitId == hId && it.localDate == dStr }
            val hasActivity = hasRecordedActivityInDb(db, hId, dStr)
            val backupKind = o.optString("exception_kind", "REST_DAY")
            val backupNote = if (o.isNull("note") || !o.has("note")) null else o.getString("note")
            if (existing == null && !hasActivity) {
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS,
                    null,
                    jsonToHabitDateExceptionCv(o),
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            } else if (hasActivity) {
                val currentPayload = existing?.let { habitDateExceptionToJson(it).toString() }
                    ?: JSONObject().apply {
                        put("habit_id", hId)
                        put("local_date", dStr)
                        put("local_state", "RECORDED_ACTIVITY")
                    }.toString()
                insertConflictRow(
                    db = db,
                    sessionId = sessionId,
                    recordType = "habit_date_exception",
                    recordKey = "$hId:$dStr",
                    currentPayload = currentPayload,
                    backupPayload = o.toString(),
                    nowMs = nowMs
                )
                conflicts++
            } else if (existing != null && (existing.exceptionKind.name != backupKind || existing.note != backupNote)) {
                insertConflictRow(
                    db = db,
                    sessionId = sessionId,
                    recordType = "habit_date_exception",
                    recordKey = "$hId:$dStr",
                    currentPayload = habitDateExceptionToJson(existing).toString(),
                    backupPayload = o.toString(),
                    nowMs = nowMs
                )
                conflicts++
            }
        }

        // 7. Habit Streak Runs (v3)
        val runsArr = root.optJSONArray("habit_streak_runs") ?: JSONArray()
        for (i in 0 until runsArr.length()) {
            val o = runsArr.getJSONObject(i)
            val rId = o.getString("run_id")
            val hId = o.getString("habit_id")
            if (!habitExistsInDb(db, hId)) continue
            val startDate = LocalDateValidator.requireValid(o.getString("start_local_date"), "habit_streak_runs.start_local_date")
            val endDate = LocalDateValidator.requireValid(o.getString("end_local_date"), "habit_streak_runs.end_local_date")
            val isBackupActive = o.optBoolean("is_active", o.optInt("is_active", 0) == 1)
            val backupQualified = o.optBoolean("qualified_for_grove", o.optInt("qualified_for_grove", 0) == 1)
            val backupQualifiedAt = if (o.isNull("qualified_at_utc_ms") || !o.has("qualified_at_utc_ms")) null else o.getLong("qualified_at_utc_ms")
            val backupTerm = if (o.isNull("terminal_reason") || !o.has("terminal_reason")) null else o.getString("terminal_reason")

            val existingSameStart = _habitStreakRuns.value.find { it.habitId == hId && it.startLocalDate == startDate }
            val existingSameId = _habitStreakRuns.value.find { it.runId == rId }
            val existingActive = if (isBackupActive) {
                _habitStreakRuns.value.find { it.habitId == hId && it.isActive && it.startLocalDate != startDate }
            } else {
                null
            }
            val existingRun = existingSameStart ?: existingSameId ?: existingActive
            if (existingRun == null) {
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_HABIT_STREAK_RUNS,
                    null,
                    jsonToHabitStreakRunCv(o),
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            } else {
                val differs = existingRun.runId != rId ||
                    existingRun.startLocalDate != startDate ||
                    existingRun.endLocalDate != endDate ||
                    existingRun.completedStreakCount != o.getInt("completed_streak_count") ||
                    existingRun.elapsedActiveSpanCount != o.getInt("elapsed_active_span_count") ||
                    existingRun.isActive != isBackupActive ||
                    existingRun.qualifiedForGrove != backupQualified ||
                    existingRun.qualifiedAtUtcMs != backupQualifiedAt ||
                    existingRun.terminalReason?.name != backupTerm
                if (differs) {
                    insertConflictRow(
                        db = db,
                        sessionId = sessionId,
                        recordType = "habit_streak_run",
                        recordKey = "$hId:$startDate",
                        currentPayload = habitStreakRunToJson(existingRun).toString(),
                        backupPayload = o.toString(),
                        nowMs = nowMs
                    )
                    conflicts++
                }
            }
        }

        // 8. Week Start History (v3)
        val weekHistoryArr = root.optJSONArray("week_start_history")
        if (weekHistoryArr != null && weekHistoryArr.length() > 0) {
            for (i in 0 until weekHistoryArr.length()) {
                val o = weekHistoryArr.getJSONObject(i)
                val effDate = LocalDateValidator.requireValid(o.getString("effective_from_local_date"), "week_start_history.effective_from_local_date")
                val isoDay = o.getInt("week_start_iso_day")
                val existing = _weekStartHistory.value.find { it.effectiveFromLocalDate == effDate }
                if (existing == null) {
                    db.insertWithOnConflict(
                        DayseedDatabaseHelper.TABLE_WEEK_START_HISTORY,
                        null,
                        jsonToWeekStartPolicyCv(o),
                        SQLiteDatabase.CONFLICT_IGNORE
                    )
                } else if (existing.weekStartIsoDay != isoDay) {
                    insertConflictRow(
                        db = db,
                        sessionId = sessionId,
                        recordType = "week_start_history",
                        recordKey = effDate,
                        currentPayload = weekStartPolicyToJson(existing).toString(),
                        backupPayload = o.toString(),
                        nowMs = nowMs
                    )
                    conflicts++
                }
            }
        } else if (_weekStartHistory.value.isEmpty()) {
            val earliestDate = computeEarliestBackupLocalDate(root)
            db.insertWithOnConflict(
                DayseedDatabaseHelper.TABLE_WEEK_START_HISTORY,
                null,
                ContentValues().apply {
                    put("effective_from_local_date", earliestDate)
                    put("week_start_iso_day", 1)
                    put("is_legacy_baseline", 1)
                    put("created_at_utc_ms", nowMs)
                },
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }

        // 9. Journal Entries
        val journalsArr = root.optJSONArray("journal_entries") ?: JSONArray()
        for (i in 0 until journalsArr.length()) {
            val o = journalsArr.getJSONObject(i)
            val jId = o.getString("journal_entry_id")
            val existing = _journalEntries.value.find { it.journalEntryId == jId }
            val backupTitle = if (o.isNull("title") || !o.has("title")) "" else o.getString("title")
            if (existing == null) {
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES,
                    null,
                    jsonToJournalEntryCv(o),
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            } else if (existing.body != o.getString("body") || (existing.title ?: "") != backupTitle) {
                insertConflictRow(
                    db = db,
                    sessionId = sessionId,
                    recordType = "journal_entry",
                    recordKey = jId,
                    currentPayload = journalEntryToJson(existing).toString(),
                    backupPayload = o.toString(),
                    nowMs = nowMs
                )
                conflicts++
            }
        }

        // 10. Journal Drafts
        val draftsArr = root.optJSONArray("journal_drafts") ?: JSONArray()
        for (i in 0 until draftsArr.length()) {
            val o = draftsArr.getJSONObject(i)
            val dId = o.getString("draft_id")
            val existing = _journalDrafts.value.find { it.draftId == dId }
            val backupTitle = if (o.isNull("title") || !o.has("title")) "" else o.getString("title")
            if (existing == null) {
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_JOURNAL_DRAFTS,
                    null,
                    jsonToJournalDraftCv(o),
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            } else if (existing.body != o.getString("body") || (existing.title ?: "") != backupTitle) {
                insertConflictRow(
                    db = db,
                    sessionId = sessionId,
                    recordType = "journal_draft",
                    recordKey = dId,
                    currentPayload = journalDraftToJson(existing).toString(),
                    backupPayload = o.toString(),
                    nowMs = nowMs
                )
                conflicts++
            }
        }

        // 11. App Settings
        val settingsArr = root.optJSONArray("app_settings") ?: JSONArray()
        for (i in 0 until settingsArr.length()) {
            val o = settingsArr.getJSONObject(i)
            val sKey = o.getString("setting_key")
            val existing = _appSettings.value[sKey]
            if (existing == null) {
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_APP_SETTINGS,
                    null,
                    jsonToAppSettingCv(o),
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            } else if (existing.valueText != o.getString("value_text")) {
                insertConflictRow(
                    db = db,
                    sessionId = sessionId,
                    recordType = "app_setting",
                    recordKey = sKey,
                    currentPayload = appSettingToJson(existing).toString(),
                    backupPayload = o.toString(),
                    nowMs = nowMs
                )
                conflicts++
            }
        }

        return conflicts
    }

    private fun insertConflictRow(
        db: SQLiteDatabase,
        sessionId: String,
        recordType: String,
        recordKey: String,
        currentPayload: String,
        backupPayload: String,
        nowMs: Long
    ) {
        val cv = ContentValues().apply {
            put("conflict_id", UUID.randomUUID().toString())
            put("restore_session_id", sessionId)
            put("record_type", recordType)
            put("record_key", recordKey)
            put("current_payload_json", currentPayload)
            put("backup_payload_json", backupPayload)
            put("created_at_utc_ms", nowMs)
        }
        db.insertOrThrow(DayseedDatabaseHelper.TABLE_RESTORE_CONFLICTS, null, cv)
    }

    fun resolveRestoreConflictSync(
        conflictId: String,
        resolution: ConflictResolution,
        allowDestructiveActivityRemoval: Boolean = false,
        authorizedDestructiveConflictId: String? = null
    ) {
        val conflict = _restoreConflicts.value.find { it.conflictId == conflictId }
            ?: throw IllegalArgumentException("Conflict not found: $conflictId")
        require(resolution in conflict.allowedResolutions()) {
            "Resolution $resolution is not permitted for record_type=${conflict.recordType}"
        }

        val db = dbHelper.writableDatabase
        val backupObj = JSONObject(conflict.backupPayloadJson)
        val nowMs = clock.nowUtcMs()

        db.beginTransaction()
        try {
            when (resolution) {
                ConflictResolution.KEEP_CURRENT -> {}
                ConflictResolution.USE_BACKUP -> {
                    when (conflict.recordType) {
                        "habit" -> {
                            val hId = backupObj.getString("habit_id")
                            val cv = ContentValues().apply {
                                put("updated_at_utc_ms", maxOf(backupObj.getLong("updated_at_utc_ms"), nowMs))
                                if (backupObj.isNull("category") || !backupObj.has("category")) {
                                    putNull("category")
                                } else {
                                    put("category", backupObj.getString("category"))
                                }
                                if (backupObj.isNull("lifetime_goal_days") || !backupObj.has("lifetime_goal_days")) {
                                    putNull("lifetime_goal_days")
                                } else {
                                    put("lifetime_goal_days", backupObj.getInt("lifetime_goal_days"))
                                }
                            }
                            db.update(
                                DayseedDatabaseHelper.TABLE_HABITS,
                                cv,
                                "habit_id = ?",
                                arrayOf(hId)
                            )
                        }
                        "habit_version" -> {
                            db.insertWithOnConflict(
                                DayseedDatabaseHelper.TABLE_HABIT_VERSIONS,
                                null,
                                jsonToHabitVersionCv(backupObj),
                                SQLiteDatabase.CONFLICT_REPLACE
                            )
                        }
                        "habit_entry" -> {
                            db.insertWithOnConflict(
                                DayseedDatabaseHelper.TABLE_HABIT_ENTRIES,
                                null,
                                jsonToHabitEntryCv(backupObj),
                                SQLiteDatabase.CONFLICT_REPLACE
                            )
                        }
                        "habit_day_outcome" -> {
                            db.insertWithOnConflict(
                                DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES,
                                null,
                                jsonToHabitDayOutcomeCv(backupObj),
                                SQLiteDatabase.CONFLICT_REPLACE
                            )
                        }
                        "habit_date_note" -> {
                            db.insertWithOnConflict(
                                DayseedDatabaseHelper.TABLE_HABIT_DATE_NOTES,
                                null,
                                jsonToHabitDateNoteCv(backupObj),
                                SQLiteDatabase.CONFLICT_REPLACE
                            )
                        }
                        "habit_date_exception" -> {
                            val hId = backupObj.getString("habit_id")
                            val dStr = LocalDateValidator.requireValid(backupObj.getString("local_date"), "habit_date_exceptions.local_date")
                            val hasLocalActivity = hasRecordedActivityInDb(db, hId, dStr)
                            if (hasLocalActivity) {
                                val isAuthorizedForThisConflict = allowDestructiveActivityRemoval &&
                                    authorizedDestructiveConflictId == conflictId
                                if (!isAuthorizedForThisConflict) {
                                    throw IllegalStateException(
                                        "Cannot apply USE_BACKUP for habit_date_exception on ($hId, $dStr) while local recorded activity exists without explicit per-conflict destructive authorization."
                                    )
                                }
                                db.delete(
                                    DayseedDatabaseHelper.TABLE_HABIT_ENTRIES,
                                    "habit_id = ? AND local_date = ?",
                                    arrayOf(hId, dStr)
                                )
                                db.delete(
                                    DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES,
                                    "habit_id = ? AND local_date = ?",
                                    arrayOf(hId, dStr)
                                )
                            }
                            db.delete(
                                DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS,
                                "habit_id = ? AND local_date = ?",
                                arrayOf(hId, dStr)
                            )
                            db.insertOrThrow(
                                DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS,
                                null,
                                jsonToHabitDateExceptionCv(backupObj)
                            )
                        }
                        "habit_streak_run" -> {
                            val rId = backupObj.getString("run_id")
                            val hId = backupObj.getString("habit_id")
                            check(habitExistsInDb(db, hId)) { "Parent habit $hId does not exist for habit_streak_run $rId" }
                            val startDate = LocalDateValidator.requireValid(backupObj.getString("start_local_date"), "habit_streak_runs.start_local_date")
                            val isBackupActive = backupObj.optBoolean("is_active", backupObj.optInt("is_active", 0) == 1)

                            // A backup active run may replace only the same logical run (same start date)
                            // or the same run ID. Never erase a different active run to satisfy the
                            // one-active-run invariant: that run represents distinct streak history.
                            if (isBackupActive) {
                                db.rawQuery(
                                    """
                                    SELECT run_id, start_local_date
                                    FROM ${DayseedDatabaseHelper.TABLE_HABIT_STREAK_RUNS}
                                    WHERE habit_id = ? AND is_active = 1
                                      AND run_id != ? AND start_local_date != ?
                                    LIMIT 1
                                    """.trimIndent(),
                                    arrayOf(hId, rId, startDate)
                                ).use { c ->
                                    if (c.moveToFirst()) {
                                        throw IllegalStateException(
                                            "Cannot replace active streak run $rId for habit $hId: " +
                                                "a different active run (${c.getString(0)}) starts on ${c.getString(1)}. " +
                                                "Keep the current run or resolve the conflict after preserving its history."
                                        )
                                    }
                                }
                            }

                            db.delete(
                                DayseedDatabaseHelper.TABLE_HABIT_STREAK_RUNS,
                                "run_id = ? OR (habit_id = ? AND start_local_date = ?)",
                                arrayOf(rId, hId, startDate)
                            )
                            db.insertOrThrow(
                                DayseedDatabaseHelper.TABLE_HABIT_STREAK_RUNS,
                                null,
                                jsonToHabitStreakRunCv(backupObj)
                            )
                        }
                        "week_start_history" -> {
                            db.insertWithOnConflict(
                                DayseedDatabaseHelper.TABLE_WEEK_START_HISTORY,
                                null,
                                jsonToWeekStartPolicyCv(backupObj),
                                SQLiteDatabase.CONFLICT_REPLACE
                            )
                        }
                        "journal_entry" -> {
                            db.insertWithOnConflict(
                                DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES,
                                null,
                                jsonToJournalEntryCv(backupObj),
                                SQLiteDatabase.CONFLICT_REPLACE
                            )
                        }
                        "journal_draft" -> {
                            db.insertWithOnConflict(
                                DayseedDatabaseHelper.TABLE_JOURNAL_DRAFTS,
                                null,
                                jsonToJournalDraftCv(backupObj),
                                SQLiteDatabase.CONFLICT_REPLACE
                            )
                        }
                        "app_setting" -> {
                            db.insertWithOnConflict(
                                DayseedDatabaseHelper.TABLE_APP_SETTINGS,
                                null,
                                jsonToAppSettingCv(backupObj),
                                SQLiteDatabase.CONFLICT_REPLACE
                            )
                        }
                    }
                }
                ConflictResolution.KEEP_BOTH -> {
                    val copyObj = JSONObject(conflict.backupPayloadJson).apply {
                        put("journal_entry_id", UUID.randomUUID().toString())
                        put("updated_at_utc_ms", nowMs)
                    }
                    db.insertOrThrow(
                        DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES,
                        null,
                        jsonToJournalEntryCv(copyObj)
                    )
                }
            }

            db.delete(DayseedDatabaseHelper.TABLE_RESTORE_CONFLICTS, "conflict_id = ?", arrayOf(conflictId))

            var remaining = 0
            db.rawQuery(
                "SELECT COUNT(*) FROM ${DayseedDatabaseHelper.TABLE_RESTORE_CONFLICTS} WHERE restore_session_id = ?",
                arrayOf(conflict.restoreSessionId)
            ).use { c ->
                if (c.moveToFirst()) remaining = c.getInt(0)
            }
            if (remaining == 0) {
                val sCv = ContentValues().apply {
                    put("session_state", "COMPLETED")
                    put("updated_at_utc_ms", nowMs)
                }
                db.update(
                    DayseedDatabaseHelper.TABLE_RESTORE_SESSIONS,
                    sCv,
                    "restore_session_id = ?",
                    arrayOf(conflict.restoreSessionId)
                )
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        refreshAllSync()
    }

    // =========================================================================
    // THREE DELETION FLOWS & SINGLE-HABIT DELETION UNDO (D-192, D-203, §9.1)
    // =========================================================================

    fun deleteJournalEntrySync(journalEntryId: String) {
        val db = dbHelper.writableDatabase
        db.delete(
            DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES,
            "journal_entry_id = ?",
            arrayOf(journalEntryId)
        )
        refreshAllSync()
    }

    suspend fun deleteJournalEntry(id: String) = withContext(Dispatchers.IO) {
        deleteJournalEntrySync(id)
    }

    /**
     * Deletes a habit and returns an 8-second guarded [UndoToken] containing the full
     * [HabitDeletionPayload] snapshot so the deletion can be atomically undone (Revision 7 §9.1).
     */
    fun deleteHabitWithUndoSync(habitId: String): UndoToken? {
        val habit = _habits.value.find { it.habitId == habitId } ?: return null
        val versions = _habitVersions.value.filter { it.habitId == habitId }
        val entries = _habitEntries.value.filter { it.habitId == habitId }
        val outcomes = _habitDayOutcomes.value.filter { it.habitId == habitId }
        val notes = _habitDateNotes.value.filter { it.habitId == habitId }
        val exceptions = _habitDateExceptions.value.filter { it.habitId == habitId }
        val streakRuns = _habitStreakRuns.value.filter { it.habitId == habitId }
        val linkedJournals = _journalEntries.value.filter { it.linkedHabitId == habitId }
        val linkedDrafts = _journalDrafts.value.filter { it.linkedHabitId == habitId }

        val snapshotJson = JSONObject().apply {
            put("habit", habitToJson(habit))
            put("versions", JSONArray().apply { versions.forEach { put(habitVersionToJson(it)) } })
            put("entries", JSONArray().apply { entries.forEach { put(habitEntryToJson(it)) } })
            put("outcomes", JSONArray().apply { outcomes.forEach { put(habitDayOutcomeToJson(it)) } })
            put("notes", JSONArray().apply { notes.forEach { put(habitDateNoteToJson(it)) } })
            put("exceptions", JSONArray().apply { exceptions.forEach { put(habitDateExceptionToJson(it)) } })
            put("streak_runs", JSONArray().apply { streakRuns.forEach { put(habitStreakRunToJson(it)) } })
            put(
                "unlinked_journal_entries",
                JSONArray().apply {
                    linkedJournals.forEach { j ->
                        put(
                            JSONObject().apply {
                                put("journal_entry_id", j.journalEntryId)
                                put("expected_updated_at_utc_ms", j.updatedAtUtcMs)
                            }
                        )
                    }
                }
            )
            put(
                "unlinked_journal_drafts",
                JSONArray().apply {
                    linkedDrafts.forEach { d ->
                        put(
                            JSONObject().apply {
                                put("draft_id", d.draftId)
                                put("expected_updated_at_utc_ms", d.updatedAtUtcMs)
                            }
                        )
                    }
                }
            )
        }.toString()

        val db = dbHelper.writableDatabase
        val nowMs = clock.nowUtcMs()

        db.beginTransaction()
        try {
            val toCancel = mutableListOf<Pair<String, Int>>()
            db.rawQuery(
                """
                SELECT DISTINCT platform_notification_tag, platform_notification_id
                FROM ${DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES}
                WHERE habit_id = ? AND platform_notification_tag IS NOT NULL AND platform_notification_id IS NOT NULL
                """.trimIndent(),
                arrayOf(habitId)
            ).use { c ->
                while (c.moveToNext()) {
                    toCancel.add(c.getString(0) to c.getValidatedInt(1, "platform_notification_id"))
                }
            }

            if (toCancel.isEmpty()) {
                toCancel.add("dayseed_habit_$habitId" to habitId.hashCode())
            }

            for ((tag, notifId) in toCancel) {
                ReminderEngine.enqueueNotificationCancel(db, tag, notifId, nowMs)
            }

            db.delete(DayseedDatabaseHelper.TABLE_HABITS, "habit_id = ?", arrayOf(habitId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        processCancelOutboxSync()
        refreshAllSync()

        val token = UndoToken(
            tokenId = UUID.randomUUID().toString(),
            entityType = UndoEntityType.HABIT_DELETION,
            entityId = habitId,
            expectedUpdatedAtUtcMs = habit.updatedAtUtcMs,
            previousPayloadJson = snapshotJson,
            createdAtUtcMs = nowMs
        )
        _activeUndoToken.value = token
        return token
    }

    private fun doesUndoTokenBelongToHabit(token: UndoToken, habitId: String): Boolean {
        val payloadHabitId = token.previousPayloadJson?.let { json ->
            runCatching { JSONObject(json).optString("habit_id", "") }.getOrNull()
        }.orEmpty()
        if (payloadHabitId == habitId) {
            return true
        }
        return when (token.entityType) {
            UndoEntityType.HABIT_ENTRY ->
                _habitEntries.value.any { it.entryId == token.entityId && it.habitId == habitId }
            UndoEntityType.HABIT_DAY_OUTCOME ->
                _habitDayOutcomes.value.any { it.outcomeId == token.entityId && it.habitId == habitId }
            UndoEntityType.HABIT_DATE_NOTE ->
                _habitDateNotes.value.any { it.noteId == token.entityId && it.habitId == habitId }
            UndoEntityType.REST_DAY_EXCEPTION ->
                _habitDateExceptions.value.any { it.id == token.entityId && it.habitId == habitId }
            UndoEntityType.HABIT_DELETION ->
                token.entityId == habitId
        }
    }

    /**
     * Permanently deletes a habit in a single atomic SQLite transaction (Phase 2 F-02):
     * - Cancels active/scheduled habit notifications via the durable outbox.
     * - Unlinks any linked journal entries and drafts (`linked_habit_id = NULL`) while preserving their text.
     * - Deletes the habit and its cascaded versions, entries, outcomes, date notes, exceptions, and streak runs.
     * - Invalidates any active [UndoToken] that belongs to a session/record of [habitId], while preserving
     *   unrelated pending [UndoToken]s belonging to other habits.
     * - Does NOT call [deleteHabitWithUndoSync] or generate a `HABIT_DELETION` [UndoToken].
     */
    fun deleteHabitPermanentlySync(habitId: String): Boolean {
        _habits.value.find { it.habitId == habitId } ?: return false

        val currentUndo = _activeUndoToken.value
        if (currentUndo != null && doesUndoTokenBelongToHabit(currentUndo, habitId)) {
            consumedUndoTokenIds.add(currentUndo.tokenId)
            _activeUndoToken.value = null
        }

        val db = dbHelper.writableDatabase
        val nowMs = clock.nowUtcMs()

        db.beginTransaction()
        try {
            val toCancel = mutableListOf<Pair<String, Int>>()
            db.rawQuery(
                """
                SELECT DISTINCT platform_notification_tag, platform_notification_id
                FROM ${DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES}
                WHERE habit_id = ? AND platform_notification_tag IS NOT NULL AND platform_notification_id IS NOT NULL
                """.trimIndent(),
                arrayOf(habitId)
            ).use { c ->
                while (c.moveToNext()) {
                    toCancel.add(c.getString(0) to c.getValidatedInt(1, "platform_notification_id"))
                }
            }

            if (toCancel.isEmpty()) {
                toCancel.add("dayseed_habit_$habitId" to habitId.hashCode())
            }

            for ((tag, notifId) in toCancel) {
                ReminderEngine.enqueueNotificationCancel(db, tag, notifId, nowMs)
            }

            val unlinkedCv = ContentValues().apply {
                putNull("linked_habit_id")
            }
            db.update(
                DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES,
                unlinkedCv,
                "linked_habit_id = ?",
                arrayOf(habitId)
            )
            db.update(
                DayseedDatabaseHelper.TABLE_JOURNAL_DRAFTS,
                unlinkedCv,
                "linked_habit_id = ?",
                arrayOf(habitId)
            )

            db.delete(DayseedDatabaseHelper.TABLE_HABITS, "habit_id = ?", arrayOf(habitId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        processCancelOutboxSync()
        refreshAllSync()
        rescheduleReminderAlarms()
        return true
    }

    suspend fun deleteHabit(id: String): Boolean = withContext(Dispatchers.IO) {
        deleteHabitPermanentlySync(id)
    }

    private fun restoreDeletedHabitFromSnapshotInTx(db: SQLiteDatabase, snap: JSONObject): Boolean {
        val habitObj = snap.getJSONObject("habit")
        val habitId = habitObj.getString("habit_id")

        // Guard 1: habitId must not already exist
        db.rawQuery("SELECT 1 FROM ${DayseedDatabaseHelper.TABLE_HABITS} WHERE habit_id = ? LIMIT 1", arrayOf(habitId)).use { c ->
            if (c.moveToFirst()) return false
        }

        val versionsArr = snap.optJSONArray("versions") ?: JSONArray()
        val entriesArr = snap.optJSONArray("entries") ?: JSONArray()
        val outcomesArr = snap.optJSONArray("outcomes") ?: JSONArray()
        val notesArr = snap.optJSONArray("notes") ?: JSONArray()
        val exceptionsArr = snap.optJSONArray("exceptions") ?: JSONArray()
        val runsArr = snap.optJSONArray("streak_runs") ?: JSONArray()

        // Guard 2: Verify no primary-key or idempotency_key collisions exist before inserting
        for (i in 0 until versionsArr.length()) {
            val vId = versionsArr.getJSONObject(i).getString("version_id")
            db.rawQuery("SELECT 1 FROM ${DayseedDatabaseHelper.TABLE_HABIT_VERSIONS} WHERE version_id = ? LIMIT 1", arrayOf(vId)).use { c ->
                if (c.moveToFirst()) return false
            }
        }
        for (i in 0 until entriesArr.length()) {
            val o = entriesArr.getJSONObject(i)
            val eId = o.getString("entry_id")
            val iKey = o.getString("idempotency_key")
            db.rawQuery(
                """
                SELECT 1 FROM ${DayseedDatabaseHelper.TABLE_HABIT_ENTRIES} WHERE entry_id = ? OR idempotency_key = ?
                UNION ALL
                SELECT 1 FROM ${DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES} WHERE idempotency_key = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(eId, iKey, iKey)
            ).use { c ->
                if (c.moveToFirst()) return false
            }
        }
        for (i in 0 until outcomesArr.length()) {
            val o = outcomesArr.getJSONObject(i)
            val oId = o.getString("outcome_id")
            val iKey = o.getString("idempotency_key")
            db.rawQuery(
                """
                SELECT 1 FROM ${DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES} WHERE outcome_id = ? OR idempotency_key = ?
                UNION ALL
                SELECT 1 FROM ${DayseedDatabaseHelper.TABLE_HABIT_ENTRIES} WHERE idempotency_key = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(oId, iKey, iKey)
            ).use { c ->
                if (c.moveToFirst()) return false
            }
        }

        // Insert habit and all child records
        db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABITS, null, jsonToHabitCv(habitObj))
        for (i in 0 until versionsArr.length()) {
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_VERSIONS, null, jsonToHabitVersionCv(versionsArr.getJSONObject(i)))
        }
        for (i in 0 until entriesArr.length()) {
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_ENTRIES, null, jsonToHabitEntryCv(entriesArr.getJSONObject(i)))
        }
        for (i in 0 until outcomesArr.length()) {
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES, null, jsonToHabitDayOutcomeCv(outcomesArr.getJSONObject(i)))
        }
        for (i in 0 until notesArr.length()) {
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_DATE_NOTES, null, jsonToHabitDateNoteCv(notesArr.getJSONObject(i)))
        }
        for (i in 0 until exceptionsArr.length()) {
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS, null, jsonToHabitDateExceptionCv(exceptionsArr.getJSONObject(i)))
        }
        for (i in 0 until runsArr.length()) {
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_STREAK_RUNS, null, jsonToHabitStreakRunCv(runsArr.getJSONObject(i)))
        }

        // Re-link journals & drafts whose linked_habit_id is still NULL AND updated_at_utc_ms matches pre-deletion snapshot (§9.1)
        val journalRefs = snap.optJSONArray("unlinked_journal_entries") ?: JSONArray()
        for (i in 0 until journalRefs.length()) {
            val refObj = journalRefs.getJSONObject(i)
            val jId = refObj.getString("journal_entry_id")
            val expectedUpd = refObj.getLong("expected_updated_at_utc_ms")
            val cv = ContentValues().apply { put("linked_habit_id", habitId) }
            db.update(
                DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES,
                cv,
                "journal_entry_id = ? AND linked_habit_id IS NULL AND updated_at_utc_ms = ?",
                arrayOf(jId, expectedUpd.toString())
            )
        }
        val draftRefs = snap.optJSONArray("unlinked_journal_drafts") ?: JSONArray()
        for (i in 0 until draftRefs.length()) {
            val refObj = draftRefs.getJSONObject(i)
            val dId = refObj.getString("draft_id")
            val expectedUpd = refObj.getLong("expected_updated_at_utc_ms")
            val cv = ContentValues().apply { put("linked_habit_id", habitId) }
            db.update(
                DayseedDatabaseHelper.TABLE_JOURNAL_DRAFTS,
                cv,
                "draft_id = ? AND linked_habit_id IS NULL AND updated_at_utc_ms = ?",
                arrayOf(dId, expectedUpd.toString())
            )
        }
        return true
    }

    /**
     * Flow 3: Full local reset / Delete All Data (D-192, D-203, AC-015)
     */
    fun deleteAllDataSync() {
        val db = dbHelper.writableDatabase
        val nowMs = clock.nowUtcMs()

        db.beginTransaction()
        try {
            val toCancel = mutableListOf<Pair<String, Int>>()
            db.rawQuery(
                """
                SELECT DISTINCT platform_notification_tag, platform_notification_id
                FROM ${DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES}
                WHERE platform_notification_tag IS NOT NULL AND platform_notification_id IS NOT NULL
                """.trimIndent(),
                null
            ).use { c ->
                while (c.moveToNext()) {
                    toCancel.add(c.getString(0) to c.getValidatedInt(1, "platform_notification_id"))
                }
            }
            for ((tag, notifId) in toCancel) {
                ReminderEngine.enqueueNotificationCancel(db, tag, notifId, nowMs)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        processCancelOutboxSync()
        val notifManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notifManager?.cancelAll()

        db.beginTransaction()
        try {
            db.delete(DayseedDatabaseHelper.TABLE_RESTORE_CONFLICTS, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_RESTORE_SESSIONS, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_JOURNAL_DRAFTS, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_HABIT_STREAK_RUNS, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_HABIT_DATE_EXCEPTIONS, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_HABIT_DATE_NOTES, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_HABIT_ENTRIES, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_HABIT_VERSIONS, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_HABITS, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_WEEK_START_HISTORY, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_APP_SETTINGS, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_NOTIFICATION_CANCEL_OUTBOX, null, null)

            val cv = ContentValues().apply {
                put("setting_key", "onboarding_completed")
                put("value_type", "BOOL")
                put("value_text", "false")
                put("schema_version", 1)
                put("updated_at_utc_ms", nowMs)
            }
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_APP_SETTINGS, null, cv)

            val wCv = ContentValues().apply {
                put("effective_from_local_date", "1970-01-01")
                put("week_start_iso_day", 1)
                put("is_legacy_baseline", 1)
                put("created_at_utc_ms", nowMs)
            }
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_WEEK_START_HISTORY, null, wCv)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        appContext.filesDir?.listFiles()?.forEach { file ->
            if (file.name.endsWith(".dayseed.json") || file.name.startsWith("pre_restore_safety_")) {
                file.delete()
            }
        }

        _comebackInfo.value = null
        _activeUndoToken.value = null
        refreshAllSync()
    }

    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        deleteAllDataSync()
    }

    // =========================================================================
    // JSON <-> CONTENTVALUES SERIALIZATION HELPERS
    // =========================================================================

    private fun jsonToHabitCv(o: JSONObject) = ContentValues().apply {
        put("habit_id", o.getString("habit_id"))
        put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
        put("updated_at_utc_ms", o.getLong("updated_at_utc_ms"))
        if (o.isNull("category") || !o.has("category")) putNull("category") else put("category", o.getString("category"))
        if (o.isNull("lifetime_goal_days") || !o.has("lifetime_goal_days")) {
            putNull("lifetime_goal_days")
        } else {
            put("lifetime_goal_days", o.getInt("lifetime_goal_days"))
        }
    }

    private fun jsonToHabitVersionCv(o: JSONObject): ContentValues {
        val rawDesc = if (o.isNull("description")) null else o.getString("description")
        val parsed = parseHabitMetadataDescription(rawDesc)
        val rawScheduleKind = o.getString("schedule_kind")
        val rawTargetPeriod = o.optString(
            "target_period",
            if (rawScheduleKind == "FLEXIBLE_WEEKLY_TARGET") "WEEK" else "DAY"
        )
        val rawScheduledMask = if (o.isNull("scheduled_weekdays_mask") || !o.has("scheduled_weekdays_mask")) {
            null
        } else {
            o.getInt("scheduled_weekdays_mask")
        }
        val rawPreferredMask = if (o.isNull("preferred_weekdays_mask") || !o.has("preferred_weekdays_mask")) {
            null
        } else {
            o.getInt("preferred_weekdays_mask")
        }

        // Preserve legacy v2 weekly schedules (U-11):
        // - (DAILY, WEEK) -> (FLEXIBLE_WEEKLY_TARGET, WEEK, preferred_weekdays_mask = null)
        // - (SELECTED_WEEKDAYS, mask, WEEK) -> (FLEXIBLE_WEEKLY_TARGET, WEEK, scheduled_weekdays_mask = null, preferred_weekdays_mask = mask)
        val normalizedScheduleKind: String
        val normalizedPeriod: String
        val finalScheduledMask: Int?
        val finalPreferredMask: Int?
        when {
            rawTargetPeriod == "WEEK" && rawScheduleKind == "DAILY" -> {
                normalizedScheduleKind = "FLEXIBLE_WEEKLY_TARGET"
                normalizedPeriod = "WEEK"
                finalScheduledMask = null
                finalPreferredMask = null
            }
            rawTargetPeriod == "WEEK" && rawScheduleKind == "SELECTED_WEEKDAYS" -> {
                normalizedScheduleKind = "FLEXIBLE_WEEKLY_TARGET"
                normalizedPeriod = "WEEK"
                finalScheduledMask = null
                finalPreferredMask = rawScheduledMask
            }
            rawScheduleKind == "FLEXIBLE_WEEKLY_TARGET" -> {
                normalizedScheduleKind = "FLEXIBLE_WEEKLY_TARGET"
                normalizedPeriod = "WEEK"
                finalScheduledMask = null
                finalPreferredMask = rawPreferredMask
            }
            else -> {
                normalizedScheduleKind = rawScheduleKind
                normalizedPeriod = "DAY"
                finalScheduledMask = rawScheduledMask
                finalPreferredMask = null
            }
        }

        val canonicalMode = TrackingMode.fromDb(o.getString("tracking_mode")).canonical()

        return ContentValues().apply {
            put("version_id", o.getString("version_id"))
            put("habit_id", o.getString("habit_id"))
            put("effective_from_local_date", LocalDateValidator.requireValid(o.getString("effective_from_local_date")))
            put("name", o.getString("name"))
            val cleanDesc = parsed.cleanDescription.ifEmpty { null }
            if (cleanDesc == null) putNull("description") else put("description", cleanDesc)
            put("tracking_mode", canonicalMode.name)
            if (o.isNull("unit_code")) putNull("unit_code") else put("unit_code", o.getString("unit_code"))
            put("target_period", normalizedPeriod)
            if (o.isNull("target_value") || !o.has("target_value")) {
                if (canonicalMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION)) {
                    put("target_value", 1)
                } else {
                    putNull("target_value")
                }
            } else if (canonicalMode in listOf(TrackingMode.COMPLETION_ONLY, TrackingMode.EFFORT_AWARE) && normalizedPeriod == "DAY") {
                putNull("target_value")
            } else {
                put("target_value", o.getInt("target_value"))
            }
            put("schedule_kind", normalizedScheduleKind)
            if (finalScheduledMask == null) putNull("scheduled_weekdays_mask") else put("scheduled_weekdays_mask", finalScheduledMask)
            if (finalPreferredMask == null) putNull("preferred_weekdays_mask") else put("preferred_weekdays_mask", finalPreferredMask)
            put("lifecycle_state", o.getString("lifecycle_state"))
            put("reminder_mode", o.getString("reminder_mode"))
            if (o.isNull("reminder_time_local")) putNull("reminder_time_local") else put("reminder_time_local", o.getString("reminder_time_local"))
            put("tiny_description", if (o.has("tiny_description") && !o.isNull("tiny_description")) o.getString("tiny_description") else parsed.tiny)
            put("usual_description", if (o.has("usual_description") && !o.isNull("usual_description")) o.getString("usual_description") else parsed.usual)
            put("extra_description", if (o.has("extra_description") && !o.isNull("extra_description")) o.getString("extra_description") else parsed.extra)
            put("color_hex", if (o.has("color_hex") && !o.isNull("color_hex")) o.getString("color_hex") else parsed.colorHex)
            put("sort_order", o.optInt("sort_order", 0))
            put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
        }
    }

    private fun jsonToHabitEntryCv(o: JSONObject) = ContentValues().apply {
        put("entry_id", o.getString("entry_id"))
        put("habit_id", o.getString("habit_id"))
        put("local_date", LocalDateValidator.requireValid(o.getString("local_date")))
        put("value_units", o.getInt("value_units"))
        if (o.isNull("entry_note")) putNull("entry_note") else put("entry_note", o.getString("entry_note"))
        put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
        put("updated_at_utc_ms", o.getLong("updated_at_utc_ms"))
        put("idempotency_key", o.getString("idempotency_key"))
    }

    private fun jsonToHabitDayOutcomeCv(o: JSONObject) = ContentValues().apply {
        put("outcome_id", o.getString("outcome_id"))
        put("habit_id", o.getString("habit_id"))
        put("local_date", LocalDateValidator.requireValid(o.getString("local_date")))
        put("outcome_kind", o.getString("outcome_kind"))
        if (o.isNull("effort_level")) putNull("effort_level") else put("effort_level", o.getString("effort_level"))
        put("idempotency_key", o.getString("idempotency_key"))
        put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
        put("updated_at_utc_ms", o.getLong("updated_at_utc_ms"))
    }

    private fun jsonToHabitDateNoteCv(o: JSONObject) = ContentValues().apply {
        put("note_id", o.getString("note_id"))
        put("habit_id", o.getString("habit_id"))
        put("local_date", LocalDateValidator.requireValid(o.getString("local_date")))
        put("body", o.getString("body"))
        put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
        put("updated_at_utc_ms", o.getLong("updated_at_utc_ms"))
    }

    private fun jsonToHabitDateExceptionCv(o: JSONObject) = ContentValues().apply {
        put("id", o.getString("id"))
        put("habit_id", o.getString("habit_id"))
        put("local_date", LocalDateValidator.requireValid(o.getString("local_date")))
        put("exception_kind", o.optString("exception_kind", "REST_DAY"))
        if (o.isNull("note") || !o.has("note")) putNull("note") else put("note", o.getString("note"))
        put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
        put("updated_at_utc_ms", o.getLong("updated_at_utc_ms"))
    }

    private fun jsonToHabitStreakRunCv(o: JSONObject) = ContentValues().apply {
        put("run_id", o.getString("run_id"))
        put("habit_id", o.getString("habit_id"))
        put("start_local_date", LocalDateValidator.requireValid(o.getString("start_local_date")))
        put("end_local_date", LocalDateValidator.requireValid(o.getString("end_local_date")))
        put("completed_streak_count", o.getInt("completed_streak_count"))
        put("elapsed_active_span_count", o.getInt("elapsed_active_span_count"))
        put("streak_unit", o.getString("streak_unit"))
        if (o.isNull("terminal_reason")) putNull("terminal_reason") else put("terminal_reason", o.getString("terminal_reason"))
        put("is_active", if (o.optBoolean("is_active", o.optInt("is_active", 0) == 1)) 1 else 0)
        put("qualified_for_grove", if (o.optBoolean("qualified_for_grove", o.optInt("qualified_for_grove", 0) == 1)) 1 else 0)
        if (o.isNull("qualified_at_utc_ms")) putNull("qualified_at_utc_ms") else put("qualified_at_utc_ms", o.getLong("qualified_at_utc_ms"))
        if (o.isNull("broken_on_local_date")) putNull("broken_on_local_date") else put("broken_on_local_date", o.getString("broken_on_local_date"))
        put("tree_state", o.getString("tree_state"))
        put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
        put("updated_at_utc_ms", o.getLong("updated_at_utc_ms"))
    }

    private fun jsonToWeekStartPolicyCv(o: JSONObject) = ContentValues().apply {
        put("effective_from_local_date", LocalDateValidator.requireValid(o.getString("effective_from_local_date")))
        put("week_start_iso_day", o.getInt("week_start_iso_day"))
        put("is_legacy_baseline", if (o.optBoolean("is_legacy_baseline", o.optInt("is_legacy_baseline", 0) == 1)) 1 else 0)
        put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
    }

    private fun jsonToJournalEntryCv(o: JSONObject) = ContentValues().apply {
        put("journal_entry_id", o.getString("journal_entry_id"))
        put("local_date", LocalDateValidator.requireValid(o.getString("local_date")))
        put("body", o.getString("body"))
        if (o.isNull("title")) putNull("title") else put("title", o.getString("title"))
        if (o.isNull("primary_mood_code")) putNull("primary_mood_code") else put("primary_mood_code", o.getString("primary_mood_code"))
        if (o.isNull("secondary_mood_code")) putNull("secondary_mood_code") else put("secondary_mood_code", o.getString("secondary_mood_code"))
        if (o.isNull("prompt_id")) putNull("prompt_id") else put("prompt_id", o.getString("prompt_id"))
        if (o.isNull("linked_habit_id")) putNull("linked_habit_id") else put("linked_habit_id", o.getString("linked_habit_id"))
        put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
        put("updated_at_utc_ms", o.getLong("updated_at_utc_ms"))
    }

    private fun jsonToJournalDraftCv(o: JSONObject) = ContentValues().apply {
        put("draft_id", o.getString("draft_id"))
        put("local_date", LocalDateValidator.requireValid(o.getString("local_date")))
        if (o.isNull("title")) putNull("title") else put("title", o.getString("title"))
        put("body", o.getString("body"))
        if (o.isNull("primary_mood_code")) putNull("primary_mood_code") else put("primary_mood_code", o.getString("primary_mood_code"))
        if (o.isNull("secondary_mood_code")) putNull("secondary_mood_code") else put("secondary_mood_code", o.getString("secondary_mood_code"))
        if (o.isNull("prompt_id")) putNull("prompt_id") else put("prompt_id", o.getString("prompt_id"))
        if (o.isNull("linked_habit_id")) putNull("linked_habit_id") else put("linked_habit_id", o.getString("linked_habit_id"))
        if (o.isNull("intended_journal_entry_id")) putNull("intended_journal_entry_id") else put("intended_journal_entry_id", o.getString("intended_journal_entry_id"))
        put("source_context", o.getString("source_context"))
        put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
        put("updated_at_utc_ms", o.getLong("updated_at_utc_ms"))
    }

    private fun jsonToAppSettingCv(o: JSONObject) = ContentValues().apply {
        put("setting_key", o.getString("setting_key"))
        put("value_type", o.getString("value_type"))
        put("value_text", o.getString("value_text"))
        put("schema_version", o.optInt("schema_version", 1))
        put("updated_at_utc_ms", o.getLong("updated_at_utc_ms"))
    }

    private fun habitToJson(h: Habit) = JSONObject().apply {
        put("habit_id", h.habitId)
        put("created_at_utc_ms", h.createdAtUtcMs)
        put("updated_at_utc_ms", h.updatedAtUtcMs)
        put("category", h.category ?: JSONObject.NULL)
        put("lifetime_goal_days", h.lifetimeGoalDays ?: JSONObject.NULL)
    }

    private fun habitVersionToJson(v: HabitVersion) = JSONObject().apply {
        put("version_id", v.versionId)
        put("habit_id", v.habitId)
        put("effective_from_local_date", v.effectiveFromLocalDate)
        put("name", v.name)
        put("description", v.description ?: JSONObject.NULL)
        put("tracking_mode", v.trackingMode.name)
        put("unit_code", v.unitCode ?: JSONObject.NULL)
        put("target_period", v.targetPeriod.name)
        put("target_value", v.targetValue ?: JSONObject.NULL)
        put("schedule_kind", v.scheduleKind.name)
        put("scheduled_weekdays_mask", v.scheduledWeekdaysMask ?: JSONObject.NULL)
        put("preferred_weekdays_mask", v.preferredWeekdaysMask ?: JSONObject.NULL)
        put("lifecycle_state", v.lifecycleState.name)
        put("reminder_mode", v.reminderMode)
        put("reminder_time_local", v.reminderTimeLocal ?: JSONObject.NULL)
        put("tiny_description", v.tinyDescription)
        put("usual_description", v.usualDescription)
        put("extra_description", v.extraDescription)
        put("color_hex", v.colorHex)
        put("sort_order", v.sortOrder)
        put("created_at_utc_ms", v.createdAtUtcMs)
    }

    private fun habitEntryToJson(e: HabitEntry) = JSONObject().apply {
        put("entry_id", e.entryId)
        put("habit_id", e.habitId)
        put("local_date", e.localDate)
        put("value_units", e.valueUnits)
        put("entry_note", e.entryNote ?: JSONObject.NULL)
        put("created_at_utc_ms", e.createdAtUtcMs)
        put("updated_at_utc_ms", e.updatedAtUtcMs)
        put("idempotency_key", e.idempotencyKey)
    }

    private fun habitDayOutcomeToJson(o: HabitDayOutcome) = JSONObject().apply {
        put("outcome_id", o.outcomeId)
        put("habit_id", o.habitId)
        put("local_date", o.localDate)
        put("outcome_kind", o.outcomeKind)
        put("effort_level", o.effortLevel ?: JSONObject.NULL)
        put("idempotency_key", o.idempotencyKey)
        put("created_at_utc_ms", o.createdAtUtcMs)
        put("updated_at_utc_ms", o.updatedAtUtcMs)
    }

    private fun habitDateNoteToJson(n: HabitDateNote) = JSONObject().apply {
        put("note_id", n.noteId)
        put("habit_id", n.habitId)
        put("local_date", n.localDate)
        put("body", n.body)
        put("created_at_utc_ms", n.createdAtUtcMs)
        put("updated_at_utc_ms", n.updatedAtUtcMs)
    }

    private fun habitDateExceptionToJson(ex: HabitDateException) = JSONObject().apply {
        put("id", ex.id)
        put("habit_id", ex.habitId)
        put("local_date", ex.localDate)
        put("exception_kind", ex.exceptionKind.name)
        put("note", ex.note ?: JSONObject.NULL)
        put("created_at_utc_ms", ex.createdAtUtcMs)
        put("updated_at_utc_ms", ex.updatedAtUtcMs)
    }

    private fun habitStreakRunToJson(r: HabitStreakRun) = JSONObject().apply {
        put("run_id", r.runId)
        put("habit_id", r.habitId)
        put("start_local_date", r.startLocalDate)
        put("end_local_date", r.endLocalDate)
        put("completed_streak_count", r.completedStreakCount)
        put("elapsed_active_span_count", r.elapsedActiveSpanCount)
        put("streak_unit", r.streakUnit.name)
        put("terminal_reason", r.terminalReason?.name ?: JSONObject.NULL)
        put("is_active", r.isActive)
        put("qualified_for_grove", r.qualifiedForGrove)
        put("qualified_at_utc_ms", r.qualifiedAtUtcMs ?: JSONObject.NULL)
        put("broken_on_local_date", r.brokenOnLocalDate ?: JSONObject.NULL)
        put("tree_state", r.treeState.name)
        put("created_at_utc_ms", r.createdAtUtcMs)
        put("updated_at_utc_ms", r.updatedAtUtcMs)
    }

    private fun weekStartPolicyToJson(w: WeekStartPolicyEntry) = JSONObject().apply {
        put("effective_from_local_date", w.effectiveFromLocalDate)
        put("week_start_iso_day", w.weekStartIsoDay)
        put("is_legacy_baseline", w.isLegacyBaseline)
        put("created_at_utc_ms", w.createdAtUtcMs)
    }

    private fun journalEntryToJson(j: JournalEntry) = JSONObject().apply {
        put("journal_entry_id", j.journalEntryId)
        put("local_date", j.localDate)
        put("body", j.body)
        put("title", j.title ?: JSONObject.NULL)
        put("primary_mood_code", j.primaryMoodCode ?: JSONObject.NULL)
        put("secondary_mood_code", j.secondaryMoodCode ?: JSONObject.NULL)
        put("prompt_id", j.promptId ?: JSONObject.NULL)
        put("linked_habit_id", j.linkedHabitId ?: JSONObject.NULL)
        put("created_at_utc_ms", j.createdAtUtcMs)
        put("updated_at_utc_ms", j.updatedAtUtcMs)
    }

    private fun journalDraftToJson(d: JournalDraft) = JSONObject().apply {
        put("draft_id", d.draftId)
        put("local_date", d.localDate)
        put("title", d.title ?: JSONObject.NULL)
        put("body", d.body)
        put("primary_mood_code", d.primaryMoodCode ?: JSONObject.NULL)
        put("secondary_mood_code", d.secondaryMoodCode ?: JSONObject.NULL)
        put("prompt_id", d.promptId ?: JSONObject.NULL)
        put("linked_habit_id", d.linkedHabitId ?: JSONObject.NULL)
        put("intended_journal_entry_id", d.intendedJournalEntryId ?: JSONObject.NULL)
        put("source_context", d.sourceContext)
        put("created_at_utc_ms", d.createdAtUtcMs)
        put("updated_at_utc_ms", d.updatedAtUtcMs)
    }

    private fun appSettingToJson(s: AppSetting) = JSONObject().apply {
        put("setting_key", s.settingKey)
        put("value_type", s.valueType)
        put("value_text", s.valueText)
        put("schema_version", s.schemaVersion)
        put("updated_at_utc_ms", s.updatedAtUtcMs)
    }
}
