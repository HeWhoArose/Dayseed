package com.example.dayseed.data

import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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
    val dbHelper = DayseedDatabaseHelper(appContext, databaseName)
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

    init {
        refreshAllSync()
        processCancelOutboxSync()
        evaluateComebackSync()
    }

    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        refreshAllSync()
    }

    fun refreshAllSync() {
        val db = dbHelper.writableDatabase
        loadAppSettingsSync(db)
        loadHabitVersionsSync(db)
        loadHabitsSync(db)
        loadHabitEntriesSync(db)
        loadHabitDayOutcomesSync(db)
        loadHabitDateNotesSync(db)
        loadJournalEntriesSync(db)
        loadJournalDraftsSync(db)
        loadRestoreConflictsSync(db)
        computeGrowthMetricsSync()
    }

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

    // =========================================================================
    // APP SETTINGS & CLOCK UNCERTAINTY
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
                    schemaVersion = c.getInt(sIdx),
                    updatedAtUtcMs = c.getLong(uIdx)
                )
                map[setting.settingKey] = setting
            }
        }
        _appSettings.value = map
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

    fun getWeekStartDay(): DayOfWeek {
        return when (getSettingString("week_start", "MONDAY").uppercase()) {
            "SUNDAY" -> DayOfWeek.SUNDAY
            else -> DayOfWeek.MONDAY
        }
    }

    fun setSettingSync(key: String, valueType: String, valueText: String) {
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
    // HABITS & EFFECTIVE-DATED HABIT VERSIONS (D-194 / D-195)
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
            val crIdx = c.getColumnIndexOrThrow("created_at_utc_ms")

            while (c.moveToNext()) {
                list.add(
                    HabitVersion(
                        versionId = c.getString(vIdIdx),
                        habitId = c.getString(hIdIdx),
                        effectiveFromLocalDate = c.getString(effIdx),
                        name = c.getString(nameIdx),
                        description = if (c.isNull(descIdx)) null else c.getString(descIdx),
                        trackingMode = TrackingMode.fromDb(c.getString(modeIdx)),
                        unitCode = if (c.isNull(unitIdx)) null else c.getString(unitIdx),
                        targetPeriod = TargetPeriod.valueOf(c.getString(perIdx)),
                        targetValue = if (c.isNull(valIdx)) null else c.getInt(valIdx),
                        scheduleKind = ScheduleKind.valueOf(c.getString(schIdx)),
                        scheduledWeekdaysMask = if (c.isNull(sMaskIdx)) null else c.getInt(sMaskIdx),
                        preferredWeekdaysMask = if (c.isNull(pMaskIdx)) null else c.getInt(pMaskIdx),
                        lifecycleState = HabitLifecycleState.valueOf(c.getString(lifeIdx)),
                        reminderMode = c.getString(remMIdx),
                        reminderTimeLocal = if (c.isNull(remTIdx)) null else c.getString(remTIdx),
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

            while (c.moveToNext()) {
                val habitId = c.getString(idIdx)
                val createdAt = c.getLong(crIdx)
                val updatedAt = c.getLong(upIdx)
                val versions = versionsByHabit[habitId].orEmpty()
                val effective = versions
                    .filter { it.effectiveFromLocalDate <= todayStr }
                    .maxWithOrNull(compareBy<HabitVersion> { it.effectiveFromLocalDate }.thenBy { it.createdAtUtcMs })
                    ?: versions.lastOrNull()

                if (effective != null) {
                    val parsedDesc = parseHabitMetadataDescription(effective.description)
                    val schedDays = when (effective.scheduleKind) {
                        ScheduleKind.DAILY -> DayOfWeek.values().toSet()
                        ScheduleKind.SELECTED_WEEKDAYS -> WeekdayMask.fromMask(effective.scheduledWeekdaysMask ?: WeekdayMask.ALL_DAYS_MASK)
                        ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> WeekdayMask.fromMask(effective.preferredWeekdaysMask ?: WeekdayMask.ALL_DAYS_MASK)
                    }
                    list.add(
                        Habit(
                            habitId = habitId,
                            createdAtUtcMs = createdAt,
                            updatedAtUtcMs = updatedAt,
                            currentVersion = effective,
                            name = effective.name,
                            description = parsedDesc.cleanDescription,
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
                            tinyDescription = parsedDesc.tiny,
                            usualDescription = parsedDesc.usual,
                            extraDescription = parsedDesc.extra,
                            scheduleDays = schedDays,
                            colorHex = parsedDesc.colorHex
                        )
                    )
                }
            }
        }
        _habits.value = list
    }

    private data class ParsedHabitMeta(
        val cleanDescription: String,
        val tiny: String,
        val usual: String,
        val extra: String,
        val colorHex: String
    )

    private fun encodeHabitMetadataDescription(
        description: String,
        tiny: String,
        usual: String,
        extra: String,
        colorHex: String
    ): String? {
        val trimmed = description.trim()
        val hasCustomMeta = tiny != "Deliberately small action" ||
            usual != "The ordinary intended action" ||
            extra != "Optional stretch action" ||
            colorHex != "#286143"
        if (!hasCustomMeta) {
            return trimmed.ifEmpty { null }
        }
        val metaObj = JSONObject().apply {
            put("desc", trimmed)
            put("tiny", tiny)
            put("usual", usual)
            put("extra", extra)
            put("color", colorHex)
        }
        return "DAYSEED_META:" + metaObj.toString()
    }

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
     * Returns true if the habit already has qualifying activity recorded on targetDate (D-194).
     */
    fun hasQualifyingActivityOnDate(habitId: String, targetDate: LocalDate): Boolean {
        val dStr = targetDate.toString()
        val hasEntry = _habitEntries.value.any { it.habitId == habitId && it.localDate == dStr && it.valueUnits > 0 }
        val hasOutcome = _habitDayOutcomes.value.any { it.habitId == habitId && it.localDate == dStr }
        return hasEntry || hasOutcome
    }

    /**
     * Saves a new habit or creates/updates an effective-dated HabitVersion (D-194).
     * When editing an existing habit that already has activity today:
     * - applyToday = true updates or replaces the version effective today.
     * - applyToday = false creates a version effective tomorrow (today.plusDays(1)).
     */
    fun saveHabitSync(
        habit: Habit,
        effectiveDateOverride: LocalDate? = null,
        applyTodayWhenLogged: Boolean = true
    ): HabitVersion {
        val db = dbHelper.writableDatabase
        val nowMs = clock.nowUtcMs()
        val today = clock.todayLocal()
        val existingHabit = _habits.value.find { it.habitId == habit.habitId }

        val effectiveDate = when {
            effectiveDateOverride != null -> effectiveDateOverride
            existingHabit == null -> today
            hasQualifyingActivityOnDate(habit.habitId, today) && !applyTodayWhenLogged -> today.plusDays(1)
            else -> today
        }
        val effectiveDateStr = effectiveDate.toString()

        val normalizedMode = habit.trackingMode.canonical()
        val normalizedTargetPeriod = when (habit.scheduleKind) {
            ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> TargetPeriod.WEEK
            else -> habit.targetPeriod
        }
        val normalizedTargetValue: Int? = when (normalizedMode) {
            TrackingMode.QUANTITY, TrackingMode.DURATION -> (habit.targetValue ?: 1).coerceAtLeast(1)
            TrackingMode.COMPLETION_ONLY, TrackingMode.EFFORT_AWARE -> null
            else -> null
        }
        val normalizedUnitCode: String? = when (normalizedMode) {
            TrackingMode.DURATION -> habit.unitCode?.takeIf { DurationNormalizer.isSupportedDurationUnit(it) } ?: "min"
            TrackingMode.QUANTITY -> habit.unitCode?.ifBlank { "count" } ?: "count"
            else -> null
        }
        val scheduledMask: Int? = when (habit.scheduleKind) {
            ScheduleKind.SELECTED_WEEKDAYS -> {
                val m = habit.scheduledWeekdaysMask ?: WeekdayMask.toMask(habit.scheduleDays)
                m.coerceIn(1, WeekdayMask.ALL_DAYS_MASK)
            }
            else -> null
        }
        val preferredMask: Int? = when (habit.scheduleKind) {
            ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> {
                val m = habit.preferredWeekdaysMask ?: WeekdayMask.toMask(habit.scheduleDays)
                if (m in 1..WeekdayMask.ALL_DAYS_MASK) m else null
            }
            else -> null
        }
        val remMode = if (habit.reminderMode in listOf("OFF", "AT_TIME")) habit.reminderMode else "OFF"
        val remTime = if (remMode == "AT_TIME") (habit.reminderTimeLocal ?: "09:00") else null

        val encodedDesc = encodeHabitMetadataDescription(
            description = habit.description,
            tiny = habit.tinyDescription,
            usual = habit.usualDescription,
            extra = habit.extraDescription,
            colorHex = habit.colorHex
        )

        var savedVersion: HabitVersion? = null
        db.beginTransaction()
        try {
            if (existingHabit == null) {
                val hCv = ContentValues().apply {
                    put("habit_id", habit.habitId)
                    put("created_at_utc_ms", habit.createdAtUtcMs.takeIf { it > 0 } ?: nowMs)
                    put("updated_at_utc_ms", nowMs)
                }
                db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABITS, null, hCv)
            } else {
                val hCv = ContentValues().apply {
                    put("updated_at_utc_ms", nowMs)
                }
                db.update(DayseedDatabaseHelper.TABLE_HABITS, hCv, "habit_id = ?", arrayOf(habit.habitId))
            }

            // Check if a version already exists for (habit_id, effective_from_local_date)
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
                if (encodedDesc != null) put("description", encodedDesc) else putNull("description")
                put("tracking_mode", normalizedMode.name)
                if (normalizedUnitCode != null) put("unit_code", normalizedUnitCode) else putNull("unit_code")
                put("target_period", normalizedTargetPeriod.name)
                if (normalizedTargetValue != null) put("target_value", normalizedTargetValue) else putNull("target_value")
                put("schedule_kind", habit.scheduleKind.name)
                if (scheduledMask != null) put("scheduled_weekdays_mask", scheduledMask) else putNull("scheduled_weekdays_mask")
                if (preferredMask != null) put("preferred_weekdays_mask", preferredMask) else putNull("preferred_weekdays_mask")
                put("lifecycle_state", habit.lifecycleState.name)
                put("reminder_mode", remMode)
                if (remTime != null) put("reminder_time_local", remTime) else putNull("reminder_time_local")
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
                description = encodedDesc,
                trackingMode = normalizedMode,
                unitCode = normalizedUnitCode,
                targetPeriod = normalizedTargetPeriod,
                targetValue = normalizedTargetValue,
                scheduleKind = habit.scheduleKind,
                scheduledWeekdaysMask = scheduledMask,
                preferredWeekdaysMask = preferredMask,
                lifecycleState = habit.lifecycleState,
                reminderMode = remMode,
                reminderTimeLocal = remTime,
                createdAtUtcMs = nowMs
            )

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        refreshAllSync()
        return savedVersion!!
    }

    suspend fun saveHabit(
        habit: Habit,
        effectiveDateOverride: LocalDate? = null,
        applyTodayWhenLogged: Boolean = true
    ): HabitVersion = withContext(Dispatchers.IO) {
        saveHabitSync(habit, effectiveDateOverride, applyTodayWhenLogged)
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

    // =========================================================================
    // HABIT ENTRIES, OUTCOMES, DATE NOTES & GUARDED UNDO (D-189, D-190, D-209)
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
                list.add(
                    HabitEntry(
                        entryId = c.getString(idIdx),
                        habitId = c.getString(hIdx),
                        localDate = c.getString(dIdx),
                        valueUnits = c.getInt(vIdx),
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
                list.add(
                    HabitDayOutcome(
                        outcomeId = c.getString(idIdx),
                        habitId = c.getString(hIdx),
                        localDate = c.getString(dIdx),
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
                list.add(
                    HabitDateNote(
                        noteId = c.getString(idIdx),
                        habitId = c.getString(hIdx),
                        localDate = c.getString(dIdx),
                        body = c.getString(bIdx),
                        createdAtUtcMs = c.getLong(crIdx),
                        updatedAtUtcMs = c.getLong(upIdx)
                    )
                )
            }
        }
        _habitDateNotes.value = list
    }

    /**
     * Logs a multi-entry QUANTITY or DURATION habit entry (D-189, D-190).
     * Idempotent retry: if idempotencyKey already exists in habit_entries with identical habitId & localDate,
     * returns the existing entry without duplicating.
     */
    fun logHabitEntrySync(
        habitId: String,
        date: LocalDate,
        valueUnits: Int,
        entryNote: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Pair<HabitEntry, UndoToken> {
        require(valueUnits >= 0) { "value_units must be >= 0" }
        val db = dbHelper.writableDatabase
        val dateStr = date.toString()

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

        val nowMs = clock.nowUtcMs()
        val entryId = UUID.randomUUID().toString()
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
            previousPayloadJson = null,
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
     * Logs a COMPLETION_ONLY or EFFORT_AWARE outcome in habit_day_outcomes (D-188, D-189, D-190).
     * Never stores SKIPPED or MISSED in habit_day_outcomes.
     */
    fun logHabitDayOutcomeSync(
        habitId: String,
        date: LocalDate,
        outcomeKind: String,
        effortLevel: String? = null,
        note: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Pair<HabitDayOutcome, UndoToken> {
        val dateStr = date.toString()
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

        val existingForDate = _habitDayOutcomes.value.find { it.habitId == habitId && it.localDate == dateStr }
        val prevJson = existingForDate?.let {
            JSONObject().apply {
                put("outcome_id", it.outcomeId)
                put("outcome_kind", it.outcomeKind)
                put("effort_level", it.effortLevel ?: JSONObject.NULL)
                put("idempotency_key", it.idempotencyKey)
                put("created_at_utc_ms", it.createdAtUtcMs)
                put("updated_at_utc_ms", it.updatedAtUtcMs)
            }.toString()
        }

        val nowMs = clock.nowUtcMs()
        val outcomeId = existingForDate?.outcomeId ?: UUID.randomUUID().toString()
        val createdAt = existingForDate?.createdAtUtcMs ?: nowMs

        db.beginTransaction()
        try {
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

    suspend fun removeRecord(habitId: String, date: LocalDate) = withContext(Dispatchers.IO) {
        val dStr = date.toString()
        val db = dbHelper.writableDatabase
        db.delete(DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES, "habit_id = ? AND local_date = ?", arrayOf(habitId, dStr))
        db.delete(DayseedDatabaseHelper.TABLE_HABIT_ENTRIES, "habit_id = ? AND local_date = ?", arrayOf(habitId, dStr))
        refreshAllSync()
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
        val result = saveHabitDateNoteInternal(db, habitId, date.toString(), body.trim(), nowMs)
        refreshAllSync()
        _activeUndoToken.value = result.second
        return result
    }

    suspend fun saveHabitDateNote(habitId: String, date: LocalDate, body: String): Pair<HabitDateNote, UndoToken> =
        withContext(Dispatchers.IO) {
            saveHabitDateNoteSync(habitId, date, body)
        }

    /**
     * Guarded 8-second Undo execution (D-209 / AC-023):
     * Verifies token has not expired (nowUtcMs <= expiresAtUtcMs) AND the target row still exists
     * with the exact expectedUpdatedAtUtcMs before reversing the mutation.
     */
    fun executeUndoSync(token: UndoToken): Boolean {
        val nowMs = clock.nowUtcMs()
        if (nowMs > token.expiresAtUtcMs) {
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
                    val current = _habitEntries.value.find { it.entryId == token.entityId }
                    if (current != null && current.updatedAtUtcMs == token.expectedUpdatedAtUtcMs) {
                        if (token.previousPayloadJson == null) {
                            val deleted = db.delete(
                                DayseedDatabaseHelper.TABLE_HABIT_ENTRIES,
                                "entry_id = ? AND updated_at_utc_ms = ?",
                                arrayOf(token.entityId, token.expectedUpdatedAtUtcMs.toString())
                            )
                            undone = deleted == 1
                        } else {
                            val prev = JSONObject(token.previousPayloadJson)
                            val cv = ContentValues().apply {
                                put("value_units", prev.getInt("value_units"))
                                if (prev.isNull("entry_note")) putNull("entry_note") else put("entry_note", prev.getString("entry_note"))
                                put("updated_at_utc_ms", prev.getLong("updated_at_utc_ms"))
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

                UndoEntityType.HABIT_DAY_OUTCOME -> {
                    val current = _habitDayOutcomes.value.find { it.outcomeId == token.entityId }
                    if (current != null && current.updatedAtUtcMs == token.expectedUpdatedAtUtcMs) {
                        if (token.previousPayloadJson == null) {
                            val deleted = db.delete(
                                DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES,
                                "outcome_id = ? AND updated_at_utc_ms = ?",
                                arrayOf(token.entityId, token.expectedUpdatedAtUtcMs.toString())
                            )
                            undone = deleted == 1
                        } else {
                            val prev = JSONObject(token.previousPayloadJson)
                            val cv = ContentValues().apply {
                                put("outcome_kind", prev.getString("outcome_kind"))
                                if (prev.isNull("effort_level")) putNull("effort_level") else put("effort_level", prev.getString("effort_level"))
                                put("idempotency_key", prev.getString("idempotency_key"))
                                put("created_at_utc_ms", prev.getLong("created_at_utc_ms"))
                                put("updated_at_utc_ms", prev.getLong("updated_at_utc_ms"))
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
            }
            if (undone) {
                db.setTransactionSuccessful()
            }
        } finally {
            db.endTransaction()
        }

        if (_activeUndoToken.value?.tokenId == token.tokenId) {
            _activeUndoToken.value = null
        }
        if (undone) {
            refreshAllSync()
        }
        return undone
    }

    suspend fun executeUndo(token: UndoToken): Boolean = withContext(Dispatchers.IO) {
        executeUndoSync(token)
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
     * Evaluates the DerivedDayStatus for a habit on targetDate using the effective HabitVersion (D-188, D-193).
     * Missed status is derived ONLY after a scheduled day (or week) has passed without qualifying activity.
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

        val today = referenceToday
        val entriesOnDate = getEntriesForHabitOnDate(habitId, targetDate)
        val outcomeOnDate = getOutcomeForHabitOnDate(habitId, targetDate)

        when (version.trackingMode) {
            TrackingMode.COMPLETION_ONLY, TrackingMode.EFFORT_AWARE -> {
                if (outcomeOnDate != null) {
                    return DerivedDayStatus.COMPLETED
                }
            }
            TrackingMode.QUANTITY, TrackingMode.DURATION -> {
                if (entriesOnDate.isNotEmpty()) {
                    val sum = entriesOnDate.sumOf { it.valueUnits }
                    val target = version.targetValue ?: 1
                    val band = ProgressBandClassifier.classify(sum, target)
                    return when (band) {
                        ProgressBand.GOAL_MET, ProgressBand.EXCEEDED -> DerivedDayStatus.COMPLETED
                        ProgressBand.ZERO -> DerivedDayStatus.LOGGED_ZERO
                        else -> DerivedDayStatus.PARTIAL
                    }
                }
            }
            else -> {}
        }

        // No recorded entry or outcome on targetDate
        return when (version.scheduleKind) {
            ScheduleKind.DAILY -> {
                if (targetDate.isBefore(today)) DerivedDayStatus.DERIVED_MISSED else DerivedDayStatus.OPEN_TODAY
            }
            ScheduleKind.SELECTED_WEEKDAYS -> {
                val mask = version.scheduledWeekdaysMask ?: WeekdayMask.ALL_DAYS_MASK
                if (!WeekdayMask.contains(mask, targetDate.dayOfWeek)) {
                    DerivedDayStatus.UNSCHEDULED
                } else if (targetDate.isBefore(today)) {
                    DerivedDayStatus.DERIVED_MISSED
                } else {
                    DerivedDayStatus.OPEN_TODAY
                }
            }
            ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> {
                val weekStart = getWeekStartDay()
                val (_, weekEnd) = WeekBoundaryHelper.weekBounds(targetDate, weekStart)
                // Check total qualifying activity across the week
                val weekDates = (0..6).map { weekEnd.minusDays((6 - it).toLong()) }
                val weekSatisfied = when (version.trackingMode) {
                    TrackingMode.QUANTITY, TrackingMode.DURATION -> {
                        val weekTotal = weekDates.sumOf { d ->
                            getEntriesForHabitOnDate(habitId, d).sumOf { it.valueUnits }
                        }
                        weekTotal >= (version.targetValue ?: 1)
                    }
                    else -> {
                        val weekCompletions = weekDates.count { d ->
                            getOutcomeForHabitOnDate(habitId, d) != null
                        }
                        weekCompletions >= (version.targetValue ?: 1)
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
     * Computes the Growth Mark (G2 + G3) summary and overall GrowthMetrics (D-032, D-201).
     * - G2: Current-month total duration (in normalized minutes) from DURATION-tracked habits only.
     * - G3: Per-date session counts across the current month (habit_entries with value_units > 0 + habit_day_outcomes).
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

        // Aggregate habit_day_outcomes
        for (outcome in _habitDayOutcomes.value) {
            val d = try { LocalDate.parse(outcome.localDate) } catch (_: Exception) { continue }
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

        // Aggregate habit_entries (QUANTITY & DURATION)
        for (entry in _habitEntries.value) {
            val d = try { LocalDate.parse(entry.localDate) } catch (_: Exception) { continue }
            if (entry.valueUnits > 0) {
                activeDates.add(d)
                completedCount++
                val version = getEffectiveHabitVersion(entry.habitId, d)
                val target = version?.targetValue ?: 1
                val band = ProgressBandClassifier.classify(entry.valueUnits, target)
                when (band) {
                    ProgressBand.TINY_STEP, ProgressBand.GETTING_STARTED -> tinyCount++
                    ProgressBand.HALFWAY, ProgressBand.ALMOST_THERE, ProgressBand.GOAL_MET -> usualCount++
                    ProgressBand.EXCEEDED -> extraCount++
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
            val d = try { LocalDate.parse(j.localDate) } catch (_: Exception) { continue }
            activeDates.add(d)
        }

        // Past 7 days opportunities
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

        _growthMetrics.value = GrowthMetrics(
            totalActiveDates = activeDates.size,
            totalHabitsCompleted = completedCount,
            tinyCount = tinyCount,
            usualCount = usualCount,
            extraCount = extraCount,
            plannedRestCount = 0,
            journalEntriesCount = _journalEntries.value.size,
            currentPeriodCompletions = pastWeekCompletions,
            currentPeriodOpportunities = pastWeekOpportunities,
            currentMonthDurationMinutes = currentMonthDurationMinutes,
            currentMonthDateSessionCounts = currentMonthDateSessionCounts
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
                list.add(
                    JournalEntry(
                        journalEntryId = c.getString(idIdx),
                        localDate = c.getString(dIdx),
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
                list.add(
                    JournalDraft(
                        draftId = c.getString(idIdx),
                        localDate = c.getString(dIdx),
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

    /**
     * Saves or updates an autosaved draft in journal_drafts (D-191).
     * While typing, intended_journal_entry_id remains null until prepareAndCommitDraftSync is called.
     */
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

        val validSecondary = if (primaryMoodCode != null && secondaryMoodCode != primaryMoodCode) secondaryMoodCode else null

        val cv = ContentValues().apply {
            put("draft_id", draftId)
            put("local_date", localDate.toString())
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
            put("updated_at_utc_ms", nowMs)
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

    /**
     * Atomically promotes a JournalDraft to a saved JournalEntry (D-191 / AC-008):
     * 1. Prepares the draft by assigning intended_journal_entry_id if null.
     * 2. In a single transaction, inserts/updates journal_entries with that exact journal_entry_id and deletes the draft.
     * Calling this repeatedly on the same draft or retrying after interruption never creates duplicate entries.
     */
    fun promoteDraftToJournalEntrySync(draftId: String): JournalEntry? {
        val db = dbHelper.writableDatabase
        val draft = _journalDrafts.value.find { it.draftId == draftId } ?: return null
        if (draft.body.isBlank()) return null

        val nowMs = clock.nowUtcMs()
        val intendedId = draft.intendedJournalEntryId ?: UUID.randomUUID().toString()

        // Step 1: Persist intended_journal_entry_id on draft before promotion
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

        // Step 2: Atomic insert into journal_entries + delete from journal_drafts
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

    /**
     * Saves or edits a JournalEntry directly (e.g. from editor dialog or date edit).
     * Also clears any active composer draft for the same date.
     */
    fun saveJournalEntrySync(entry: JournalEntry): JournalEntry {
        val db = dbHelper.writableDatabase
        val nowMs = clock.nowUtcMs()
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
                put("local_date", entry.localDate)
                put("body", entry.body.trim())
                if (!entry.title.isNullOrBlank()) put("title", entry.title.trim()) else putNull("title")
                if (!entry.primaryMoodCode.isNullOrBlank()) put("primary_mood_code", entry.primaryMoodCode) else putNull("primary_mood_code")
                if (!validSecondary.isNullOrBlank()) put("secondary_mood_code", validSecondary) else putNull("secondary_mood_code")
                if (!entry.promptId.isNullOrBlank()) put("prompt_id", entry.promptId) else putNull("prompt_id")
                if (!entry.linkedHabitId.isNullOrBlank()) put("linked_habit_id", entry.linkedHabitId) else putNull("linked_habit_id")
                put("created_at_utc_ms", createdAt)
                put("updated_at_utc_ms", nowMs)
            }
            db.insertWithOnConflict(
                DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES,
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            // Clear matching composer draft if present
            db.delete(
                DayseedDatabaseHelper.TABLE_JOURNAL_DRAFTS,
                "local_date = ? AND source_context = 'JOURNAL_COMPOSER'",
                arrayOf(entry.localDate)
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

    /**
     * Evaluates whether a habit qualifies for a low-frequency retrospective journal prompt (D-042 / AC-024):
     * Eligible when the habit has >= 2 derived missed scheduled days in the past 7 days,
     * retrospective prompts are enabled in settings, and the prompt has not been dismissed/shown in the last 7 days.
     */
    fun isHabitEligibleForRetrospectivePrompt(habitId: String, referenceDate: LocalDate = clock.todayLocal()): Boolean {
        if (!getSettingBoolean("retrospective_journal_prompts_enabled", true)) return false
        val dismissKey = "retrospective_prompt_dismissed_$habitId"
        val lastDismissedDateStr = _appSettings.value[dismissKey]?.valueText
        if (lastDismissedDateStr != null) {
            val lastDismissed = try { LocalDate.parse(lastDismissedDateStr) } catch (_: Exception) { null }
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
        periodKind: String = "WEEK", // "WEEK" or "MONTH"
        referenceDate: LocalDate = clock.todayLocal(),
        selectedMoodFilter: String? = null
    ): MoodInsightsResult {
        val weekStart = getWeekStartDay()
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
            val d = try { LocalDate.parse(e.localDate) } catch (_: Exception) { null }
            d != null && !d.isBefore(currentStart) && !d.isAfter(currentEnd)
        }
        val prevEntries = _journalEntries.value.filter { e ->
            val d = try { LocalDate.parse(e.localDate) } catch (_: Exception) { null }
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
            try { dates.add(LocalDate.parse(it.localDate)) } catch (_: Exception) {}
        }
        _habitDayOutcomes.value.forEach {
            try { dates.add(LocalDate.parse(it.localDate)) } catch (_: Exception) {}
        }
        _journalEntries.value.forEach {
            try { dates.add(LocalDate.parse(it.localDate)) } catch (_: Exception) {}
        }
        return dates.maxOrNull()
    }

    /**
     * Evaluates Comeback eligibility (D-047..D-067, D-206):
     * - Suppressed when clock is uncertain (D-187).
     * - 14-day flow eligible at 14..29 days since last recorded activity.
     * - 30-day flow eligible at >= 30 days since last recorded activity, superseding 14-day flow.
     * - Once-per-episode suppression: an inactivity episode is keyed by the last recorded activity date.
     */
    fun evaluateComebackSync(referenceDate: LocalDate = clock.todayLocal()): ComebackInfo? {
        if (isClockUncertainSync()) {
            _comebackInfo.value = null
            return null
        }

        val lastActivityDate = findLastRecordedActivityDate()
            ?: _appSettings.value["last_opened_date"]?.valueText?.let {
                try { LocalDate.parse(it) } catch (_: Exception) { null }
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

    /**
     * Applies adaptive habit adjustments from the 14-day or 30-day Comeback flow (D-055, D-206):
     * - Habits in resumeHabitIds stay ACTIVE (with optional loweredTargetByHabitId).
     * - Habits in pauseHabitIds transition to PAUSED effective today.
     * - Optional comebackReflectionBody is saved as a COMEBACK_30D draft or promoted to journal_entries.
     */
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
        val db = dbHelper.writableDatabase
        val quietStart = getSettingString("quiet_hours_start", ReminderEngine.DEFAULT_QUIET_START)
        val quietEnd = getSettingString("quiet_hours_end", ReminderEngine.DEFAULT_QUIET_END)
        val dailyCap = getSettingInt("daily_reminder_cap", ReminderEngine.DEFAULT_DAILY_CAP)
        val nowMs = clock.nowUtcMs()

        val results = mutableListOf<ReminderEvaluationResult>()
        for (h in _habits.value) {
            val eff = getEffectiveHabitVersion(h.habitId, targetDate) ?: continue
            val logged = hasQualifyingActivityOnDate(h.habitId, targetDate)
            results.add(
                ReminderEngine.evaluateHabitReminder(
                    db = db,
                    habitVersion = eff,
                    targetDate = targetDate,
                    alreadyLoggedToday = logged,
                    quietStartLocal = quietStart,
                    quietEndLocal = quietEnd,
                    dailyCap = dailyCap,
                    nowUtcMs = nowMs
                )
            )
        }
        return results
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
    // READABLE EXPORT, PORTABLE BACKUP & RESTORE (D-105..D-107, D-200, D-202)
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
     * Generates a complete versioned portable backup (.dayseed.json) containing all persistent tables.
     * Supports optional password-based AES-256-GCM encryption.
     */
    fun exportPortableBackupSync(password: String? = null): String {
        val root = JSONObject()
        root.put("backup_kind", "dayseed_portable_backup")
        root.put("format_version", BackupAndExportManager.BACKUP_FORMAT_VERSION)
        root.put("source_schema_version", DayseedDatabaseHelper.DATABASE_VERSION)
        root.put("exported_at_utc_ms", clock.nowUtcMs())

        root.put("habits", JSONArray().apply {
            for (h in _habits.value) {
                put(JSONObject().apply {
                    put("habit_id", h.habitId)
                    put("created_at_utc_ms", h.createdAtUtcMs)
                    put("updated_at_utc_ms", h.updatedAtUtcMs)
                })
            }
        })

        root.put("habit_versions", JSONArray().apply {
            for (v in _habitVersions.value) {
                put(JSONObject().apply {
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
                    put("created_at_utc_ms", v.createdAtUtcMs)
                })
            }
        })

        root.put("habit_entries", JSONArray().apply {
            for (e in _habitEntries.value) {
                put(JSONObject().apply {
                    put("entry_id", e.entryId)
                    put("habit_id", e.habitId)
                    put("local_date", e.localDate)
                    put("value_units", e.valueUnits)
                    put("entry_note", e.entryNote ?: JSONObject.NULL)
                    put("created_at_utc_ms", e.createdAtUtcMs)
                    put("updated_at_utc_ms", e.updatedAtUtcMs)
                    put("idempotency_key", e.idempotencyKey)
                })
            }
        })

        root.put("habit_day_outcomes", JSONArray().apply {
            for (o in _habitDayOutcomes.value) {
                put(JSONObject().apply {
                    put("outcome_id", o.outcomeId)
                    put("habit_id", o.habitId)
                    put("local_date", o.localDate)
                    put("outcome_kind", o.outcomeKind)
                    put("effort_level", o.effortLevel ?: JSONObject.NULL)
                    put("idempotency_key", o.idempotencyKey)
                    put("created_at_utc_ms", o.createdAtUtcMs)
                    put("updated_at_utc_ms", o.updatedAtUtcMs)
                })
            }
        })

        root.put("habit_date_notes", JSONArray().apply {
            for (n in _habitDateNotes.value) {
                put(JSONObject().apply {
                    put("note_id", n.noteId)
                    put("habit_id", n.habitId)
                    put("local_date", n.localDate)
                    put("body", n.body)
                    put("created_at_utc_ms", n.createdAtUtcMs)
                    put("updated_at_utc_ms", n.updatedAtUtcMs)
                })
            }
        })

        root.put("journal_entries", JSONArray().apply {
            for (j in _journalEntries.value) {
                put(JSONObject().apply {
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
                })
            }
        })

        root.put("journal_drafts", JSONArray().apply {
            for (d in _journalDrafts.value) {
                put(JSONObject().apply {
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
                })
            }
        })

        root.put("app_settings", JSONArray().apply {
            for (s in _appSettings.value.values) {
                put(JSONObject().apply {
                    put("setting_key", s.settingKey)
                    put("value_type", s.valueType)
                    put("value_text", s.valueText)
                    put("schema_version", s.schemaVersion)
                    put("updated_at_utc_ms", s.updatedAtUtcMs)
                })
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
     * Restores a portable backup using REPLACE or MERGE mode (D-106, D-202, AC-014, AC-022):
     * 1. Unwraps/decrypts and validates the backup JSON before mutating live tables.
     * 2. Writes and verifies an automatic pre-restore safety backup file (`safety_backup_verified = 1`).
     * 3. Creates a `restore_sessions` row.
     * 4. In REPLACE mode, replaces all user data atomically.
     * 5. In MERGE mode, imports non-conflicting records and inserts conflicting records into `restore_conflicts`
     *    with session_state = 'AWAITING_CONFLICT_RESOLUTION'.
     */
    fun restorePortableBackupSync(
        rawBackupPayload: String,
        restoreMode: String = "REPLACE", // "REPLACE" or "MERGE"
        password: String? = null
    ): RestoreSession {
        require(restoreMode in listOf("REPLACE", "MERGE")) { "Invalid restoreMode: $restoreMode" }
        val unwrappedJson = BackupAndExportManager.unwrapBackupPayload(rawBackupPayload, password)
        val root = JSONObject(unwrappedJson)
        val formatVersion = root.optInt("format_version", 0)
        val sourceSchemaVersion = root.optInt("source_schema_version", 0)
        require(formatVersion == BackupAndExportManager.BACKUP_FORMAT_VERSION && sourceSchemaVersion == 2) {
            "Unsupported or corrupted backup format"
        }

        // Write and verify pre-restore safety backup
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
                db.delete(DayseedDatabaseHelper.TABLE_HABIT_DATE_NOTES, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_HABIT_ENTRIES, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_HABIT_VERSIONS, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_HABITS, null, null)
                db.delete(DayseedDatabaseHelper.TABLE_APP_SETTINGS, null, null)

                importBackupArraysDirect(db, root)
                finalState = "COMPLETED"
            } else {
                // MERGE mode with conflict detection (D-202)
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

    private fun importBackupArraysDirect(db: SQLiteDatabase, root: JSONObject) {
        val habitsArr = root.optJSONArray("habits") ?: JSONArray()
        for (i in 0 until habitsArr.length()) {
            val o = habitsArr.getJSONObject(i)
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABITS, null, ContentValues().apply {
                put("habit_id", o.getString("habit_id"))
                put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
                put("updated_at_utc_ms", o.getLong("updated_at_utc_ms"))
            })
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
                db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABITS, null, ContentValues().apply {
                    put("habit_id", hId)
                    put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
                    put("updated_at_utc_ms", o.getLong("updated_at_utc_ms"))
                })
            }
        }

        // 2. Habit Versions
        val versionsArr = root.optJSONArray("habit_versions") ?: JSONArray()
        for (i in 0 until versionsArr.length()) {
            val o = versionsArr.getJSONObject(i)
            val hId = o.getString("habit_id")
            val effDate = o.getString("effective_from_local_date")
            val existing = _habitVersions.value.find { it.habitId == hId && it.effectiveFromLocalDate == effDate }
            if (existing == null) {
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_HABIT_VERSIONS,
                    null,
                    jsonToHabitVersionCv(o),
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            } else if (existing.name != o.getString("name") || existing.trackingMode.name != o.getString("tracking_mode")) {
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
            if (existing == null) {
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES,
                    null,
                    jsonToHabitDayOutcomeCv(o),
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            } else if (existing.outcomeKind != o.getString("outcome_kind") || existing.effortLevel != o.optString("effort_level", null)) {
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
            if (existing == null) {
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_HABIT_ENTRIES,
                    null,
                    jsonToHabitEntryCv(o),
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            } else if (existing.valueUnits != o.getInt("value_units") || (existing.entryNote ?: "") != o.optString("entry_note", "")) {
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

        // 6. Journal Entries
        val journalsArr = root.optJSONArray("journal_entries") ?: JSONArray()
        for (i in 0 until journalsArr.length()) {
            val o = journalsArr.getJSONObject(i)
            val jId = o.getString("journal_entry_id")
            val existing = _journalEntries.value.find { it.journalEntryId == jId }
            if (existing == null) {
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES,
                    null,
                    jsonToJournalEntryCv(o),
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            } else if (existing.body != o.getString("body") || (existing.title ?: "") != o.optString("title", "")) {
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

        // 7. App Settings
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

    /**
     * Resolves a single RestoreConflict (D-202 / AC-022):
     * Enforces RestoreConflict.allowedResolutions() so KEEP_BOTH is permitted ONLY for journal_entry.
     */
    fun resolveRestoreConflictSync(conflictId: String, resolution: ConflictResolution) {
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
                ConflictResolution.KEEP_CURRENT -> {
                    // Keep current live row unchanged
                }
                ConflictResolution.USE_BACKUP -> {
                    when (conflict.recordType) {
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
                    // Only allowed for journal_entry: insert backup copy under a new journal_entry_id
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

            // If no remaining conflicts for this session, transition session_state to COMPLETED
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
    // THREE DELETION FLOWS (D-192, D-203, AC-015, AC-026)
    // =========================================================================

    /**
     * Flow 1: Delete a single saved journal entry.
     */
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
     * Flow 2: Permanent single-habit delete (D-192, D-203, AC-026):
     * - Enqueues notification_cancel_outbox rows for any delivered/scheduled reminders for this habit.
     * - Deletes the habit row in `habits`:
     *   - Cascades to habit_versions, habit_entries, habit_day_outcomes, habit_date_notes, reminder_deliveries.
     *   - Preserves journal_entries and journal_drafts with linked_habit_id set to NULL (ON DELETE SET NULL).
     * - Processes the notification cancel outbox.
     */
    fun deleteHabitPermanentlySync(habitId: String) {
        val db = dbHelper.writableDatabase
        val nowMs = clock.nowUtcMs()

        db.beginTransaction()
        try {
            // Collect platform notification tags/ids from reminder_deliveries for this habit
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
                    toCancel.add(c.getString(0) to c.getInt(1))
                }
            }
            if (toCancel.isEmpty()) {
                val defaultTag = "dayseed_habit_$habitId"
                val defaultId = (habitId.hashCode() and 0x7FFFFFFF).coerceAtLeast(1)
                toCancel.add(defaultTag to defaultId)
            }

            for ((tag, id) in toCancel) {
                val outboxCv = ContentValues().apply {
                    put("cancel_id", UUID.randomUUID().toString())
                    put("platform_notification_tag", tag)
                    put("platform_notification_id", id)
                    put("created_at_utc_ms", nowMs)
                    putNull("last_attempt_at_utc_ms")
                    put("attempt_count", 0)
                    put("status", "PENDING")
                    putNull("error_category")
                }
                db.insertOrThrow(DayseedDatabaseHelper.TABLE_NOTIFICATION_CANCEL_OUTBOX, null, outboxCv)
            }

            // Delete from habits (triggers ON DELETE CASCADE on habit children and ON DELETE SET NULL on journal tables)
            db.delete(DayseedDatabaseHelper.TABLE_HABITS, "habit_id = ?", arrayOf(habitId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        processCancelOutboxSync()
        refreshAllSync()
    }

    suspend fun deleteHabit(habitId: String) = withContext(Dispatchers.IO) {
        deleteHabitPermanentlySync(habitId)
    }

    /**
     * Flow 3: Delete all personal data (D-203, AC-015):
     * Clears all 12 tables, deletes local backup/export files in filesDir, and resets onboarding.
     */
    fun deleteAllDataSync() {
        val db = dbHelper.writableDatabase
        val nowMs = clock.nowUtcMs()
        db.beginTransaction()
        try {
            db.delete(DayseedDatabaseHelper.TABLE_RESTORE_CONFLICTS, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_RESTORE_SESSIONS, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_JOURNAL_DRAFTS, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_HABIT_DATE_NOTES, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_HABIT_ENTRIES, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_HABIT_VERSIONS, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_HABITS, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_APP_SETTINGS, null, null)
            db.delete(DayseedDatabaseHelper.TABLE_NOTIFICATION_CANCEL_OUTBOX, null, null)

            // Reset onboarding_completed to false
            val cv = ContentValues().apply {
                put("setting_key", "onboarding_completed")
                put("value_type", "BOOL")
                put("value_text", "false")
                put("schema_version", 1)
                put("updated_at_utc_ms", nowMs)
            }
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_APP_SETTINGS, null, cv)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        // Remove any local backup/safety files
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

    private fun jsonToHabitVersionCv(o: JSONObject) = ContentValues().apply {
        put("version_id", o.getString("version_id"))
        put("habit_id", o.getString("habit_id"))
        put("effective_from_local_date", o.getString("effective_from_local_date"))
        put("name", o.getString("name"))
        if (o.isNull("description")) putNull("description") else put("description", o.getString("description"))
        put("tracking_mode", o.getString("tracking_mode"))
        if (o.isNull("unit_code")) putNull("unit_code") else put("unit_code", o.getString("unit_code"))
        put("target_period", o.getString("target_period"))
        if (o.isNull("target_value")) putNull("target_value") else put("target_value", o.getInt("target_value"))
        put("schedule_kind", o.getString("schedule_kind"))
        if (o.isNull("scheduled_weekdays_mask")) putNull("scheduled_weekdays_mask") else put("scheduled_weekdays_mask", o.getInt("scheduled_weekdays_mask"))
        if (o.isNull("preferred_weekdays_mask")) putNull("preferred_weekdays_mask") else put("preferred_weekdays_mask", o.getInt("preferred_weekdays_mask"))
        put("lifecycle_state", o.getString("lifecycle_state"))
        put("reminder_mode", o.getString("reminder_mode"))
        if (o.isNull("reminder_time_local")) putNull("reminder_time_local") else put("reminder_time_local", o.getString("reminder_time_local"))
        put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
    }

    private fun jsonToHabitEntryCv(o: JSONObject) = ContentValues().apply {
        put("entry_id", o.getString("entry_id"))
        put("habit_id", o.getString("habit_id"))
        put("local_date", o.getString("local_date"))
        put("value_units", o.getInt("value_units"))
        if (o.isNull("entry_note")) putNull("entry_note") else put("entry_note", o.getString("entry_note"))
        put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
        put("updated_at_utc_ms", o.getLong("updated_at_utc_ms"))
        put("idempotency_key", o.getString("idempotency_key"))
    }

    private fun jsonToHabitDayOutcomeCv(o: JSONObject) = ContentValues().apply {
        put("outcome_id", o.getString("outcome_id"))
        put("habit_id", o.getString("habit_id"))
        put("local_date", o.getString("local_date"))
        put("outcome_kind", o.getString("outcome_kind"))
        if (o.isNull("effort_level")) putNull("effort_level") else put("effort_level", o.getString("effort_level"))
        put("idempotency_key", o.getString("idempotency_key"))
        put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
        put("updated_at_utc_ms", o.getLong("updated_at_utc_ms"))
    }

    private fun jsonToHabitDateNoteCv(o: JSONObject) = ContentValues().apply {
        put("note_id", o.getString("note_id"))
        put("habit_id", o.getString("habit_id"))
        put("local_date", o.getString("local_date"))
        put("body", o.getString("body"))
        put("created_at_utc_ms", o.getLong("created_at_utc_ms"))
        put("updated_at_utc_ms", o.getLong("updated_at_utc_ms"))
    }

    private fun jsonToJournalEntryCv(o: JSONObject) = ContentValues().apply {
        put("journal_entry_id", o.getString("journal_entry_id"))
        put("local_date", o.getString("local_date"))
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
        put("local_date", o.getString("local_date"))
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

    private fun appSettingToJson(s: AppSetting) = JSONObject().apply {
        put("setting_key", s.settingKey)
        put("value_type", s.valueType)
        put("value_text", s.valueText)
        put("schema_version", s.schemaVersion)
        put("updated_at_utc_ms", s.updatedAtUtcMs)
    }
}
