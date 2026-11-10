package com.example.dayseed

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedClock
import com.example.dayseed.data.BackupAndExportManager
import com.example.dayseed.data.DayseedDatabaseHelper
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.Habit
import com.example.dayseed.data.ReminderScheduler
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.WeeklyReminderEngine
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Phase 4 implementation self-checks (Stage B). These are implementation tests, not
 * independent Stage C validation, and they do not replace the human/device checks in
 * docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase4ImplementationTest {

    private class MutableTestClock(
        var currentLocalDate: LocalDate = LocalDate.of(2026, 10, 12),
        var currentLocalTime: LocalTime = LocalTime.of(10, 0),
        var currentUtcMs: Long = 1_792_000_000_000L,
        private val zone: ZoneId = ZoneId.of("UTC")
    ) : DayseedClock {
        override fun nowUtcMs(): Long = currentUtcMs
        override fun todayLocal(): LocalDate = currentLocalDate
        override fun nowLocalTime(): LocalTime = currentLocalTime
        override fun zoneId(): ZoneId = zone
    }

    private lateinit var app: Application
    private lateinit var testClock: MutableTestClock
    private lateinit var dbName: String
    private lateinit var repository: DayseedRepository

    private val weekStart = LocalDate.of(2026, 10, 12) // Monday
    private val habitId = "h_weekly_p4"

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        testClock = MutableTestClock()
        // Robolectric isolates each test method in its own sandbox; keep this filename short
        // so long method names remain within Windows' legacy path limit.
        dbName = "p4.db"
        repository = DayseedRepository(context = app, clock = testClock, databaseName = dbName)
        // The weekly family is part of the same opt-in reminder feature, so the pass tests run
        // with reminders enabled and the Android notification permission granted.
        org.robolectric.Shadows.shadowOf(app)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        repository.setSettingSync("reminders_enabled", "BOOL", "true")
        repository.setSettingSync("weekly_reflections_enabled", "BOOL", "false")
    }

    @After
    fun tearDown() {
        repository.dbHelper.close()
        app.deleteDatabase(dbName)
    }

    private fun seedWeeklyHabit(id: String = habitId, name: String = "Swim", target: Int = 3) {
        repository.saveHabitSync(
            Habit(
                habitId = id,
                name = name,
                trackingMode = TrackingMode.COMPLETION_ONLY,
                targetPeriod = TargetPeriod.WEEK,
                targetValue = target,
                scheduleKind = ScheduleKind.DAILY
            ),
            effectiveDateOverride = LocalDate.of(2026, 10, 5)
        )
    }

    private fun weeklyLedgerCount(): Int {
        var count = 0
        repository.dbHelper.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM ${DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES} " +
                "WHERE reminder_type IN ('WEEKLY_GOAL_REACHED', 'WEEK_START_REFLECTION', " +
                "'WEEKLY_INACTIVITY', 'PARTIAL_PROGRESS_TIP')",
            null
        ).use { c -> if (c.moveToFirst()) count = c.getInt(0) }
        return count
    }

    private fun ledgerRow(deliveryKey: String): Pair<String, String?>? {
        var result: Pair<String, String?>? = null
        repository.dbHelper.readableDatabase.rawQuery(
            "SELECT delivery_status, suppression_reason FROM ${DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES} " +
                "WHERE delivery_key = ?",
            arrayOf(deliveryKey)
        ).use { c ->
            if (c.moveToFirst()) {
                result = c.getString(0) to (if (c.isNull(1)) null else c.getString(1))
            }
        }
        return result
    }

    // ---------------------------------------------------------------------
    // A. Ledger/schema support for the weekly reminder family
    // ---------------------------------------------------------------------

    @Test
    fun p4_01_reminderLedgerAcceptsEveryWeeklyOccurrenceTypeAndRejectsUnknownTypes() {
        val db = repository.dbHelper.writableDatabase
        val nowMs = testClock.nowUtcMs()

        fun insert(reminderType: String, deliveryKey: String, habit: String?) {
            db.execSQL(
                "INSERT INTO ${DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES} " +
                    "(delivery_key, reminder_type, habit_id, target_period_key, delivery_status, " +
                    "scheduled_at_utc_ms, attempted_at_utc_ms) VALUES (?, ?, ?, ?, 'SUPPRESSED', ?, ?)",
                arrayOf(deliveryKey, reminderType, habit, weekStart.toString(), nowMs, nowMs)
            )
        }

        seedWeeklyHabit()
        insert("WEEKLY_GOAL_REACHED", "WEEKLY_GOAL_REACHED:$habitId:2026-10-12", habitId)
        insert("WEEKLY_INACTIVITY", "WEEKLY_INACTIVITY:$habitId:2026-10-14", habitId)
        insert("PARTIAL_PROGRESS_TIP", "PARTIAL_PROGRESS_TIP:$habitId:2026-10-12", habitId)
        insert("WEEK_START_REFLECTION", "WEEK_START_REFLECTION:2026-10-12", null)
        assertEquals(4, weeklyLedgerCount())

        var rejected = false
        try {
            insert("NOT_A_REAL_TYPE", "NOT_A_REAL_TYPE:x", null)
        } catch (_: SQLiteConstraintException) {
            rejected = true
        }
        assertTrue("Unknown reminder types must stay rejected by the ledger constraint", rejected)
    }

    @Test
    fun p4_02_createdSchemaDeclaresTheWeeklyReminderTypes() {
        val sql = StringBuilder()
        repository.dbHelper.readableDatabase.rawQuery(
            "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES)
        ).use { c -> if (c.moveToFirst()) sql.append(c.getString(0)) }
        val schema = sql.toString()
        assertTrue(schema.contains("WEEKLY_GOAL_REACHED"))
        assertTrue(schema.contains("WEEK_START_REFLECTION"))
        assertTrue(schema.contains("WEEKLY_INACTIVITY"))
        assertTrue(schema.contains("PARTIAL_PROGRESS_TIP"))
    }

    // ---------------------------------------------------------------------
    // A. Week-start reflection
    // ---------------------------------------------------------------------

    @Test
    fun p4_03_weekStartReflectionIsOutcomeAwareAndDeliveredOncePerWeek() {
        seedWeeklyHabit()
        repository.setSettingSync("weekly_reflections_enabled", "BOOL", "true")
        // Previous week (5–11 Oct) had no recorded progress -> "none" copy.
        val plans = repository.planWeeklyRemindersSync(
            today = weekStart,
            nowLocalTime = LocalTime.of(10, 0)
        )
        val reflection = plans.firstOrNull { it.kind == WeeklyReminderEngine.Kind.WEEK_START_REFLECTION }
        assertNotNull("A week-start reflection is due on the configured week-start day", reflection)
        assertTrue(reflection!!.body.contains("fresh start, not a reset of your progress"))

        val posted = repository.runWeeklyReminderPassSync(
            today = weekStart,
            nowLocalTime = LocalTime.of(10, 0)
        ) { _, _, _, _ -> true }
        assertEquals(1, posted.posted)

        val again = repository.planWeeklyRemindersSync(
            today = weekStart,
            nowLocalTime = LocalTime.of(11, 0)
        )
        assertTrue(
            "The same week's reflection must never be planned twice",
            again.none { it.kind == WeeklyReminderEngine.Kind.WEEK_START_REFLECTION }
        )
    }

    @Test
    fun p4_04_weekStartReflectionCelebratesMetAndAcknowledgesPartialProgress() {
        val metPlan = WeeklyReminderEngine.buildWeekStartReflection(
            weekStart,
            WeeklyReminderEngine.PreviousWeekResult.MET
        )
        val partialPlan = WeeklyReminderEngine.buildWeekStartReflection(
            weekStart,
            WeeklyReminderEngine.PreviousWeekResult.PARTIAL
        )
        val nonePlan = WeeklyReminderEngine.buildWeekStartReflection(
            weekStart,
            WeeklyReminderEngine.PreviousWeekResult.NONE
        )
        assertTrue(metPlan.body.contains("You did it last week"))
        assertTrue(partialPlan.body.contains("You made some progress last week, and that counts"))
        assertTrue(nonePlan.body.contains("that's okay"))
        assertEquals(3, setOf(metPlan.body, partialPlan.body, nonePlan.body).size)
    }

    // ---------------------------------------------------------------------
    // A. Weekly goal acknowledgement, inactivity cadence and optional tips
    // ---------------------------------------------------------------------

    @Test
    fun p4_05_weeklyGoalAcknowledgementIsSentOncePerConfiguredWeek() {
        seedWeeklyHabit()
        testClock.currentLocalDate = LocalDate.of(2026, 10, 12)
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 12), "COMPLETED")
        testClock.currentLocalDate = LocalDate.of(2026, 10, 13)
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 13), "COMPLETED")
        testClock.currentLocalDate = LocalDate.of(2026, 10, 14)
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 14), "COMPLETED")

        val first = repository.runWeeklyReminderPassSync(
            today = LocalDate.of(2026, 10, 14),
            nowLocalTime = LocalTime.of(10, 0)
        ) { _, _, _, _ -> true }
        assertEquals(1, first.posted)
        assertEquals(
            "DELIVERED",
            ledgerRow(WeeklyReminderEngine.goalReachedKey(habitId, weekStart))?.first
        )

        val second = repository.runWeeklyReminderPassSync(
            today = LocalDate.of(2026, 10, 15),
            nowLocalTime = LocalTime.of(10, 0)
        ) { _, _, _, _ -> true }
        assertEquals("No duplicate congratulations in the same week", 0, second.posted)
    }

    @Test
    fun p4_06_inactivityCheckInsWaitTwoCompleteDaysAndStopAtThreePerWeek() {
        seedWeeklyHabit()

        val beforeTwoFullDays = repository.planWeeklyRemindersSync(
            today = LocalDate.of(2026, 10, 13),
            nowLocalTime = LocalTime.of(10, 0)
        )
        assertTrue(beforeTwoFullDays.none { it.kind == WeeklyReminderEngine.Kind.WEEKLY_INACTIVITY })

        val dueDates = WeeklyReminderEngine.inactivityCheckInDates(
            anchorDate = weekStart.minusDays(1),
            weekStart = weekStart,
            weekEnd = LocalDate.of(2026, 10, 18),
            today = LocalDate.of(2026, 10, 18)
        )
        assertEquals(listOf("2026-10-14", "2026-10-16", "2026-10-18"), dueDates.map { it.toString() })

        for (day in dueDates) {
            testClock.currentLocalDate = day
            val result = repository.runWeeklyReminderPassSync(today = day, nowLocalTime = LocalTime.of(10, 0)) { _, _, _, _ -> true }
            assertEquals("One check-in per due day", 1, result.posted)
        }
        testClock.currentLocalDate = LocalDate.of(2026, 10, 18)
        val extraSameDay = repository.runWeeklyReminderPassSync(
            today = LocalDate.of(2026, 10, 18),
            nowLocalTime = LocalTime.of(11, 0)
        ) { _, _, _, _ -> true }
        assertEquals("Never more than one weekly prompt per habit per local day", 0, extraSameDay.posted)

        var inactivityRows = 0
        repository.dbHelper.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM ${DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES} " +
                "WHERE reminder_type = 'WEEKLY_INACTIVITY'",
            null
        ).use { c -> if (c.moveToFirst()) inactivityRows = c.getInt(0) }
        assertEquals(3, inactivityRows)
    }

    @Test
    fun p4_07_inactivityStopsOnceTheWeeklyTargetIsMet() {
        seedWeeklyHabit()
        testClock.currentLocalDate = LocalDate.of(2026, 10, 14)
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 12), "COMPLETED")
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 13), "COMPLETED")
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 14), "COMPLETED")

        val plans = repository.planWeeklyRemindersSync(
            today = LocalDate.of(2026, 10, 20),
            nowLocalTime = LocalTime.of(10, 0)
        )
        assertTrue(
            "A met weekly target must not produce inactivity check-ins",
            plans.none {
                it.kind == WeeklyReminderEngine.Kind.WEEKLY_INACTIVITY &&
                    it.habitId == habitId &&
                    it.deliveryKey.contains("2026-10-")
            }
        )
    }

    @Test
    fun p4_08_encouragementTipsAreOptInAndAtMostOnePerDay() {
        seedWeeklyHabit()
        // Previous week: partial progress (1 of 3).
        testClock.currentLocalDate = LocalDate.of(2026, 10, 6)
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 6), "COMPLETED")
        testClock.currentLocalDate = weekStart

        val offByDefault = repository.planWeeklyRemindersSync(today = weekStart, nowLocalTime = LocalTime.of(10, 0))
        assertTrue(offByDefault.none { it.kind == WeeklyReminderEngine.Kind.PARTIAL_PROGRESS_TIP })

        repository.setSettingSync("encouragement_tips_enabled", "BOOL", "true")
        val tipPlans = repository.planWeeklyRemindersSync(
            today = weekStart.plusDays(1),
            nowLocalTime = LocalTime.of(10, 0)
        )
        assertEquals(1, tipPlans.count { it.kind == WeeklyReminderEngine.Kind.PARTIAL_PROGRESS_TIP })

        val sameDayAgain = repository.planWeeklyRemindersSync(
            today = weekStart.plusDays(1),
            nowLocalTime = LocalTime.of(14, 0)
        )
        // Not delivered yet, so planning can still return it; delivery/ledger dedupe is what
        // prevents a second prompt the same day. Run the pass to record the first one.
        val delivered = repository.runWeeklyReminderPassSync(
            today = weekStart.plusDays(1),
            nowLocalTime = LocalTime.of(14, 0)
        ) { _, _, _, _ -> true }
        assertTrue(delivered.posted >= 1)
        val afterDelivery = repository.planWeeklyRemindersSync(
            today = weekStart.plusDays(1),
            nowLocalTime = LocalTime.of(15, 0)
        )
        assertTrue(
            "The delivered tip must not be planned again",
            afterDelivery.none {
                it.kind == WeeklyReminderEngine.Kind.PARTIAL_PROGRESS_TIP &&
                    it.deliveryKey.endsWith("2026-10-13")
            }
        )
        assertFalse(sameDayAgain.isEmpty())
    }

    @Test
    fun p4_09_tipCadenceStaysWithinThreeSpacedDays() {
        val dates = WeeklyReminderEngine.tipDates(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 20))
        assertEquals(listOf("2026-10-12", "2026-10-14", "2026-10-16"), dates.map { it.toString() })
        assertEquals(3, WeeklyReminderEngine.tipDates(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 30)).size)
    }

    // ---------------------------------------------------------------------
    // A. Shared gates: daily cap, quiet hours, notification availability
    // ---------------------------------------------------------------------

    @Test
    fun p4_10_sharedDailyCapAppliesAcrossWeeklyReminderTypes() {
        seedWeeklyHabit(id = "h_met", name = "Met")
        seedWeeklyHabit(id = "h_unmet", name = "Unmet")
        repository.setSettingSync("daily_reminder_cap", "INT", "1")
        testClock.currentLocalDate = LocalDate.of(2026, 10, 14)
        repository.logHabitDayOutcomeSync("h_met", LocalDate.of(2026, 10, 12), "COMPLETED")
        repository.logHabitDayOutcomeSync("h_met", LocalDate.of(2026, 10, 13), "COMPLETED")
        repository.logHabitDayOutcomeSync("h_met", LocalDate.of(2026, 10, 14), "COMPLETED")

        val result = repository.runWeeklyReminderPassSync(
            today = LocalDate.of(2026, 10, 14),
            nowLocalTime = LocalTime.of(10, 0)
        ) { _, _, _, _ -> true }
        assertEquals("Only the shared cap's worth may be posted", 1, result.posted)
        assertEquals(1, result.suppressed)
    }

    @Test
    fun p4_11_quietHoursDeferInsteadOfClaimingOrBurningTheOccurrence() {
        seedWeeklyHabit()
        repository.setSettingSync("weekly_reflections_enabled", "BOOL", "true")
        repository.setSettingSync("quiet_hours_start", "TIME_LOCAL", "09:00")
        repository.setSettingSync("quiet_hours_end", "TIME_LOCAL", "10:30")

        val result = repository.runWeeklyReminderPassSync(
            today = weekStart,
            nowLocalTime = LocalTime.of(11, 0)
        ) { _, _, _, _ -> true }
        assertEquals(0, result.posted)
        assertTrue(result.deferred >= 1)
        assertEquals("Quiet hours must not write a terminal row", 0, weeklyLedgerCount())
    }

    @Test
    fun p4_12_blockedNotificationsDeferWithoutWritingALedgerRow() {
        val plan = WeeklyReminderEngine.buildInactivity(
            habitId = habitId,
            habitName = "Swim",
            checkInDate = weekStart
        )
        val reservation = WeeklyReminderEngine.reserve(
            db = repository.dbHelper.writableDatabase,
            plan = plan,
            habitEligible = true,
            globallyEnabled = true,
            notificationsAvailable = false,
            quietStartLocal = "21:00",
            quietEndLocal = "08:00",
            dailyCap = 3,
            nowUtcMs = testClock.nowUtcMs(),
            today = weekStart
        )
        assertEquals(WeeklyReminderEngine.ReservationStatus.DEFERRED, reservation.status)
        assertEquals(0, weeklyLedgerCount())
    }

    @Test
    fun p4_13_postingFailureNeverClaimsDelivery() {
        seedWeeklyHabit()
        testClock.currentLocalDate = LocalDate.of(2026, 10, 14)
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 12), "COMPLETED")
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 13), "COMPLETED")
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 14), "COMPLETED")

        val result = repository.runWeeklyReminderPassSync(
            today = LocalDate.of(2026, 10, 14),
            nowLocalTime = LocalTime.of(10, 0)
        ) { _, _, _, _ -> false }
        assertEquals(0, result.posted)

        val row = ledgerRow(WeeklyReminderEngine.goalReachedKey(habitId, weekStart))
        assertNotNull(row)
        assertEquals("SUPPRESSED", row!!.first)
        assertEquals("POST_FAILED", row.second)
    }

    @Test
    fun p4_14_pausedHabitWeeklyPromptIsSuppressedNotPosted() {
        seedWeeklyHabit()
        testClock.currentLocalDate = LocalDate.of(2026, 10, 14)
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 12), "COMPLETED")
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 13), "COMPLETED")
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 14), "COMPLETED")

        val paused = repository.habits.value.first { it.habitId == habitId }
            .copy(lifecycleState = com.example.dayseed.data.HabitLifecycleState.PAUSED)
        repository.saveHabitSync(paused)

        val result = repository.runWeeklyReminderPassSync(
            today = LocalDate.of(2026, 10, 14),
            nowLocalTime = LocalTime.of(10, 0)
        ) { _, _, _, _ -> true }
        assertEquals(0, result.posted)
    }

    // ---------------------------------------------------------------------
    // B/C. Backup, deletion and privacy invariants
    // ---------------------------------------------------------------------

    @Test
    fun p4_15_portableBackupCoversPrivateEntitiesAndExcludesLiveNotificationState() {
        seedWeeklyHabit()
        testClock.currentLocalDate = LocalDate.of(2026, 10, 13)
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 12), "COMPLETED")
        repository.runWeeklyReminderPassSync(today = LocalDate.of(2026, 10, 13), nowLocalTime = LocalTime.of(10, 0)) { _, _, _, _ -> true }

        val json = JSONObject(repository.exportPortableBackupSync())
        assertTrue(json.has("habit_streak_runs"))
        assertTrue(json.has("habit_date_exceptions"))
        assertTrue(json.has("habit_date_notes"))
        assertFalse("Notification ledger must not be exported as live work", json.has("reminder_deliveries"))
        assertFalse(json.toString().contains("platform_notification_id"))
    }

    @Test
    fun p4_16_deleteAllRemovesWeeklyLedgerAndSettingsButKeepsAUsableDatabase() {
        seedWeeklyHabit()
        testClock.currentLocalDate = LocalDate.of(2026, 10, 14)
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 12), "COMPLETED")
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 13), "COMPLETED")
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 14), "COMPLETED")
        repository.runWeeklyReminderPassSync(today = LocalDate.of(2026, 10, 14), nowLocalTime = LocalTime.of(10, 0)) { _, _, _, _ -> true }
        assertTrue(weeklyLedgerCount() > 0)
        repository.saveJournalEntrySync(
            com.example.dayseed.data.JournalEntry(
                journalEntryId = "j_delete_all",
                localDate = "2026-10-14",
                body = "private journal body",
                linkedHabitId = habitId
            )
        )
        repository.saveJournalDraftSync(draftId = "draft_delete_all", body = "private draft")
        repository.dbHelper.writableDatabase.execSQL(
            """
            INSERT INTO ${DayseedDatabaseHelper.TABLE_RESTORE_SESSIONS}
                (restore_session_id, restore_mode, format_version, source_schema_version,
                 session_state, started_at_utc_ms, updated_at_utc_ms, manifest_json,
                 safety_backup_ref, safety_backup_verified)
            VALUES ('delete_all_session', 'MERGE', 3, 3, 'AWAITING_CONFLICT_RESOLUTION', 100, 100, '{}', NULL, 0)
            """.trimIndent()
        )
        repository.dbHelper.writableDatabase.execSQL(
            """
            INSERT INTO ${DayseedDatabaseHelper.TABLE_RESTORE_CONFLICTS}
                (conflict_id, restore_session_id, record_type, record_key,
                 current_payload_json, backup_payload_json, created_at_utc_ms)
            VALUES ('delete_all_conflict', 'delete_all_session', 'journal_entry', 'j_delete_all', '{}', '{}', 100)
            """.trimIndent()
        )
        repository.dbHelper.writableDatabase.execSQL(
            """
            INSERT INTO ${DayseedDatabaseHelper.TABLE_NOTIFICATION_CANCEL_OUTBOX}
                (cancel_id, platform_notification_tag, platform_notification_id, created_at_utc_ms,
                 last_attempt_at_utc_ms, attempt_count, status, error_category)
            VALUES ('delete_all_cancel', 'test-tag', 4321, 100, NULL, 0, 'PENDING', NULL)
            """.trimIndent()
        )

        repository.deleteAllDataSync()
        val db = repository.dbHelper.readableDatabase
        for (table in DayseedDatabaseHelper.ALL_V3_TABLES) {
            val expectedRows = when (table) {
                DayseedDatabaseHelper.TABLE_APP_SETTINGS,
                DayseedDatabaseHelper.TABLE_WEEK_START_HISTORY -> 1
                else -> 0
            }
            db.rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
                cursor.moveToFirst()
                assertEquals("Unexpected rows remain in $table after delete-all", expectedRows, cursor.getInt(0))
            }
        }
        assertEquals("false", repository.getSettingString("onboarding_completed", ""))
        assertTrue(repository.habits.value.isEmpty())
        assertTrue(repository.journalEntries.value.isEmpty())
        assertTrue(repository.journalDrafts.value.isEmpty())
    }

    @Test
    fun p4_17_weeklyNotificationCopyNeverContainsJournalOrMoodContent() {
        seedWeeklyHabit()
        val secretText = "SECRET-JOURNAL-BODY-P4"
        repository.saveJournalEntrySync(
            com.example.dayseed.data.JournalEntry(
                journalEntryId = "j_secret",
                localDate = weekStart.toString(),
                body = secretText,
                title = "Private title",
                primaryMoodCode = "CALM"
            )
        )
        repository.setSettingSync("encouragement_tips_enabled", "BOOL", "true")
        repository.setSettingSync("weekly_reflections_enabled", "BOOL", "true")

        val plans = repository.planWeeklyRemindersSync(today = weekStart, nowLocalTime = LocalTime.of(10, 0))
        assertTrue(plans.isNotEmpty())
        for (plan in plans) {
            assertFalse(plan.title.contains(secretText))
            assertFalse(plan.body.contains(secretText))
            assertFalse(plan.body.contains("CALM"))
        }
    }

    @Test
    fun p4_18_manifestKeepsBackupDisabledAndRequestsOnlyReminderPermissions() {
        val manifest = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        ).firstOrNull { it.exists() }
        assertNotNull("AndroidManifest.xml must be readable from the test working directory", manifest)
        val text = manifest!!.readText()

        assertTrue(text.contains("android:allowBackup=\"false\""))
        assertTrue(text.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
        assertTrue(text.contains("android:fullBackupContent=\"@xml/backup_rules\""))
        assertTrue(text.contains("android.permission.POST_NOTIFICATIONS"))
        assertTrue(text.contains("android.permission.RECEIVE_BOOT_COMPLETED"))
        assertFalse("Core features must stay offline: no network permission", text.contains("android.permission.INTERNET"))
        assertFalse(text.contains("android.permission.SCHEDULE_EXACT_ALARM"))
        assertFalse(text.contains("android.permission.USE_EXACT_ALARM"))

        val extractionRules = File("src/main/res/xml/data_extraction_rules.xml")
        assertTrue("Data extraction rules must be available to the test", extractionRules.exists())
        val extractionText = extractionRules.readText()
        assertTrue(extractionText.contains("<cloud-backup>"))
        assertTrue(extractionText.contains("<device-transfer>"))
        for (domain in listOf("database", "sharedpref", "file")) {
            assertTrue(extractionText.contains("exclude domain=\"$domain\" path=\".\""))
        }
        val dependencyCatalog = File("../gradle/libs.versions.toml")
            .takeIf { it.exists() } ?: File("gradle/libs.versions.toml")
        assertTrue(dependencyCatalog.exists())
        val dependencyText = dependencyCatalog.readText().lowercase()
        for (blocked in listOf("firebase", "analytics", "admob", "billing", "okhttp", "retrofit", "ktor-client", "gemini", "openai")) {
            assertFalse("Unapproved network/analytics dependency marker '$blocked'", dependencyText.contains(blocked))
        }
        val mainSources = File("src/main/java").takeIf { it.exists() } ?: File("app/src/main/java")
        val networkMarkers = listOf("HttpURLConnection", "OkHttpClient", "Retrofit.Builder", "FirebaseAnalytics", "MobileAds")
        mainSources.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { source ->
            val sourceText = source.readText()
            for (marker in networkMarkers) {
                assertFalse("Unapproved network-capable code marker '$marker' in ${source.name}", sourceText.contains(marker))
            }
        }
    }

    @Test
    fun p4_19_weekStartReflectionPreferenceDefaultsOff() {
        val defaultDbName = "p4_default.db"
        val defaultRepository = DayseedRepository(
            context = app,
            clock = testClock,
            databaseName = defaultDbName
        )
        try {
            defaultRepository.setSettingSync("reminders_enabled", "BOOL", "true")
            defaultRepository.saveHabitSync(
                Habit(
                    habitId = "h_default_reflection",
                    name = "Swim",
                    trackingMode = TrackingMode.COMPLETION_ONLY,
                    targetPeriod = TargetPeriod.WEEK,
                    targetValue = 3,
                    scheduleKind = ScheduleKind.DAILY
                ),
                effectiveDateOverride = LocalDate.of(2026, 10, 5)
            )

            val plans = defaultRepository.planWeeklyRemindersSync(
                today = weekStart,
                nowLocalTime = LocalTime.of(10, 0)
            )

            assertFalse(
                "Weekly reflection must require explicit opt-in on a fresh install",
                plans.any { it.kind == WeeklyReminderEngine.Kind.WEEK_START_REFLECTION }
            )
        } finally {
            defaultRepository.dbHelper.close()
            app.deleteDatabase(defaultDbName)
        }
    }

    @Test
    fun p4_20_portableBackupDeclaresTheV3ModelVersion() {
        val root = JSONObject(repository.exportPortableBackupSync())

        assertEquals(3, root.getInt("format_version"))
        assertEquals(3, root.getInt("schema_version"))
        assertEquals(3, root.getInt("source_schema_version"))
    }

    @Test
    fun p4_21_supportedV2BackupConvertsBeforeReplaceAndPreservesV2Records() {
        seedWeeklyHabit()
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 12), "COMPLETED")
        val legacyV2 = JSONObject(repository.exportPortableBackupSync()).apply {
            put("format_version", 2)
            put("schema_version", 2)
            put("source_schema_version", 2)
            remove("habit_date_exceptions")
            remove("habit_streak_runs")
            remove("week_start_history")
        }
        val expectedOutcomeCount = legacyV2.getJSONArray("habit_day_outcomes").length()

        val session = repository.restorePortableBackupSync(
            rawBackupPayload = legacyV2.toString(),
            restoreMode = "REPLACE"
        )

        assertEquals(3, session.formatVersion)
        assertEquals(2, session.sourceSchemaVersion)
        assertTrue(repository.habits.value.any { it.habitId == habitId })
        assertEquals(expectedOutcomeCount, repository.habitDayOutcomes.value.size)
        assertTrue(repository.weekStartHistory.value.isNotEmpty())
    }

    @Test
    fun p4_22_portableBackupPreservesPendingRestoreConflictsWithoutDeviceFilePaths() {
        val sourceSessionId = "source-pending-session"
        val sourceConflictId = "source-pending-conflict"
        val db = repository.dbHelper.writableDatabase
        db.execSQL(
            """
            INSERT INTO ${DayseedDatabaseHelper.TABLE_RESTORE_SESSIONS}
                (restore_session_id, restore_mode, format_version, source_schema_version,
                 session_state, started_at_utc_ms, updated_at_utc_ms, manifest_json,
                 safety_backup_ref, safety_backup_verified)
            VALUES (?, 'MERGE', 3, 3, 'AWAITING_CONFLICT_RESOLUTION', 100, 100, '{}', ?, 1)
            """.trimIndent(),
            arrayOf(sourceSessionId, "C:\\private\\local-safety-backup.dayseed.json")
        )
        db.execSQL(
            """
            INSERT INTO ${DayseedDatabaseHelper.TABLE_RESTORE_CONFLICTS}
                (conflict_id, restore_session_id, record_type, record_key,
                 current_payload_json, backup_payload_json, created_at_utc_ms)
            VALUES (?, ?, 'journal_entry', 'j_pending', '{"body":"local"}', '{"body":"backup"}', 100)
            """.trimIndent(),
            arrayOf(sourceConflictId, sourceSessionId)
        )

        val backup = JSONObject(repository.exportPortableBackupSync())
        val sessions = backup.getJSONArray("restore_sessions")
        val conflicts = backup.getJSONArray("restore_conflicts")
        assertEquals(1, sessions.length())
        assertEquals(1, conflicts.length())
        assertTrue(sessions.getJSONObject(0).isNull("safety_backup_ref"))
        assertFalse(backup.toString().contains("C:\\\\private"))

        val targetDbName = "p4_target.db"
        val targetRepository = DayseedRepository(context = app, clock = testClock, databaseName = targetDbName)
        try {
            targetRepository.restorePortableBackupSync(backup.toString(), restoreMode = "REPLACE")

            assertEquals(1, targetRepository.restoreConflicts.value.size)
            val restoredConflict = targetRepository.restoreConflicts.value.single()
            assertEquals("journal_entry", restoredConflict.recordType)
            assertTrue(restoredConflict.restoreSessionId != sourceSessionId)
            targetRepository.dbHelper.readableDatabase.rawQuery(
                "SELECT session_state FROM ${DayseedDatabaseHelper.TABLE_RESTORE_SESSIONS} WHERE restore_session_id = ?",
                arrayOf(restoredConflict.restoreSessionId)
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("AWAITING_CONFLICT_RESOLUTION", cursor.getString(0))
            }
        } finally {
            targetRepository.dbHelper.close()
            app.deleteDatabase(targetDbName)
        }
    }

    @Test
    fun p4_23_unsupportedBackupVersionLeavesExistingDataUnchanged() {
        seedWeeklyHabit()
        val before = repository.habits.value.map { it.habitId }
        val unsupported = JSONObject(repository.exportPortableBackupSync()).put("format_version", 99)

        var rejected = false
        try {
            repository.restorePortableBackupSync(unsupported.toString(), restoreMode = "REPLACE")
        } catch (_: IllegalArgumentException) {
            rejected = true
        }

        assertTrue("Unknown portable backup versions must be rejected", rejected)
        assertEquals(before, repository.habits.value.map { it.habitId })
    }

    @Test
    fun p4_24_wrongBackupPasswordLeavesExistingDataUnchanged() {
        seedWeeklyHabit()
        val before = repository.habits.value.map { it.habitId }
        val encrypted = BackupAndExportManager.encryptBackupPayload(
            repository.exportPortableBackupSync(),
            "correct horse battery staple"
        )

        var rejected = false
        try {
            repository.restorePortableBackupSync(
                encrypted,
                restoreMode = "REPLACE",
                password = "wrong password"
            )
        } catch (_: Exception) {
            rejected = true
        }

        assertTrue("Wrong-password backups must be rejected", rejected)
        assertEquals(before, repository.habits.value.map { it.habitId })
    }

    @Test
    fun p4_25_tamperedBackupLeavesExistingDataUnchanged() {
        seedWeeklyHabit()
        val before = repository.habits.value.map { it.habitId }
        val envelope = JSONObject(
            BackupAndExportManager.encryptBackupPayload(repository.exportPortableBackupSync(), "backup password")
        )
        val ciphertext = envelope.getString("ciphertext_b64")
        envelope.put("ciphertext_b64", (if (ciphertext.first() == 'A') "B" else "A") + ciphertext.drop(1))

        var rejected = false
        try {
            repository.restorePortableBackupSync(envelope.toString(), restoreMode = "REPLACE", password = "backup password")
        } catch (_: Exception) {
            rejected = true
        }

        assertTrue("Tampered encrypted backups must be rejected", rejected)
        assertEquals(before, repository.habits.value.map { it.habitId })
    }

    @Test
    fun p4_26_safetyBackupWriteFailureLeavesExistingDataUnchanged() {
        seedWeeklyHabit()
        val before = repository.habits.value.map { it.habitId }
        val safetyPath = File(app.filesDir, "pre_restore_safety_${testClock.nowUtcMs()}.dayseed.json")
        safetyPath.deleteRecursively()
        assertTrue(safetyPath.mkdir())
        try {
            var rejected = false
            try {
                repository.restorePortableBackupSync(repository.exportPortableBackupSync(), restoreMode = "REPLACE")
            } catch (_: Exception) {
                rejected = true
            }
            assertTrue("Restore must stop when the pre-restore safety backup cannot be written", rejected)
            assertEquals(before, repository.habits.value.map { it.habitId })
        } finally {
            safetyPath.deleteRecursively()
        }
    }

    @Test
    fun p4_27_midRestoreFailureRollsBackExistingRecords() {
        seedWeeklyHabit()
        repository.logHabitDayOutcomeSync(habitId, LocalDate.of(2026, 10, 12), "COMPLETED")
        val db = repository.dbHelper.readableDatabase
        fun rowCount(table: String): Int = db.rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
        val beforeHabits = rowCount(DayseedDatabaseHelper.TABLE_HABITS)
        val beforeOutcomes = rowCount(DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES)
        val root = JSONObject(repository.exportPortableBackupSync())
        root.getJSONArray("habit_streak_runs").put(
            JSONObject()
                .put("habit_id", "missing_parent")
                .put("start_local_date", "2026-10-12")
                .put("end_local_date", "2026-10-12")
        )
        val safetyPath = File(app.filesDir, "pre_restore_safety_${testClock.nowUtcMs()}.dayseed.json")
        safetyPath.delete()
        try {
            var rejected = false
            try {
                repository.restorePortableBackupSync(root.toString(), restoreMode = "REPLACE")
            } catch (_: IllegalStateException) {
                rejected = true
            }
            assertTrue("Orphan streak data must abort the restore", rejected)
            assertEquals(beforeHabits, rowCount(DayseedDatabaseHelper.TABLE_HABITS))
            assertEquals(beforeOutcomes, rowCount(DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES))
        } finally {
            safetyPath.delete()
        }
    }

    @Test
    fun p4_28_mergeRestorePreservesPortableUnresolvedConflicts() {
        val sourceSessionId = "merge-source-session"
        repository.dbHelper.writableDatabase.execSQL(
            """
            INSERT INTO ${DayseedDatabaseHelper.TABLE_RESTORE_SESSIONS}
                (restore_session_id, restore_mode, format_version, source_schema_version,
                 session_state, started_at_utc_ms, updated_at_utc_ms, manifest_json,
                 safety_backup_ref, safety_backup_verified)
            VALUES (?, 'MERGE', 3, 3, 'AWAITING_CONFLICT_RESOLUTION', 100, 100, '{}', NULL, 0)
            """.trimIndent(),
            arrayOf(sourceSessionId)
        )
        repository.dbHelper.writableDatabase.execSQL(
            """
            INSERT INTO ${DayseedDatabaseHelper.TABLE_RESTORE_CONFLICTS}
                (conflict_id, restore_session_id, record_type, record_key,
                 current_payload_json, backup_payload_json, created_at_utc_ms)
            VALUES ('merge-source-conflict', ?, 'journal_entry', 'j_pending', '{"body":"local"}', '{"body":"backup"}', 100)
            """.trimIndent(),
            arrayOf(sourceSessionId)
        )
        val portable = repository.exportPortableBackupSync()
        val targetDbName = "p4_merge.db"
        val target = DayseedRepository(context = app, clock = testClock, databaseName = targetDbName)
        try {
            target.restorePortableBackupSync(portable, restoreMode = "MERGE")
            assertEquals(1, target.restoreConflicts.value.size)
            val conflict = target.restoreConflicts.value.single()
            assertEquals("j_pending", conflict.recordKey)
            assertEquals("{\"body\":\"backup\"}", conflict.backupPayloadJson)
        } finally {
            target.dbHelper.close()
            app.deleteDatabase(targetDbName)
        }
    }

    @Test
    fun p4_29_truncatedBackupLeavesExistingDataUnchanged() {
        seedWeeklyHabit()
        val before = repository.habits.value.map { it.habitId }
        val truncated = "{\"backup_kind\":\"dayseed_portable_backup\",\"format_version\":3,\"habits\":["

        var rejected = false
        try {
            repository.restorePortableBackupSync(truncated, restoreMode = "REPLACE")
        } catch (_: Exception) {
            rejected = true
        }

        assertTrue("Truncated backup content must be rejected before mutation", rejected)
        assertEquals(before, repository.habits.value.map { it.habitId })
    }

    @Test
    fun p4_30_missingRequiredCollectionLeavesExistingDataUnchanged() {
        seedWeeklyHabit()
        val before = repository.habits.value.map { it.habitId }
        val incomplete = JSONObject(repository.exportPortableBackupSync()).apply { remove("habits") }.toString()
        val safetyFile = File(app.filesDir, "pre_restore_safety_${testClock.nowUtcMs()}.dayseed.json")
        safetyFile.delete()

        try {
            var rejected = false
            try {
                repository.restorePortableBackupSync(incomplete, restoreMode = "REPLACE")
            } catch (_: Exception) {
                rejected = true
            }

            assertTrue("Portable backup must include its required record collections", rejected)
            assertEquals(before, repository.habits.value.map { it.habitId })
            assertFalse("Malformed input must be rejected before creating a safety backup", safetyFile.exists())
        } finally {
            safetyFile.delete()
        }
    }

    @Test
    fun p4_31_restoreCannotReEnableReminderDeliveryBeforeDeviceVerification() {
        repository.setSettingSync("reminders_enabled", "BOOL", "true")
        val targetDbName = "p4_lock.db"
        val target = DayseedRepository(context = app, clock = testClock, databaseName = targetDbName)
        try {
            target.restorePortableBackupSync(repository.exportPortableBackupSync(), restoreMode = "REPLACE")
            assertEquals(
                "false",
                target.getSettingString("reminders_enabled", "true")
            )
        } finally {
            target.dbHelper.close()
            app.deleteDatabase(targetDbName)
        }
    }

    // ---------------------------------------------------------------------
    // D. Phase 4 audit corrections: reset, week-start realignment, portability
    // ---------------------------------------------------------------------

    private fun seedReminderHabit(id: String = "h_reminder_p4", time: String = "09:00") {
        repository.saveHabitSync(
            Habit(
                habitId = id,
                name = "Stretch",
                trackingMode = TrackingMode.COMPLETION_ONLY,
                targetPeriod = TargetPeriod.DAY,
                targetValue = 1,
                scheduleKind = ScheduleKind.DAILY,
                reminderMode = "AT_TIME",
                reminderTimeLocal = time
            ),
            effectiveDateOverride = LocalDate.of(2026, 10, 5)
        )
    }

    private fun alarmRepository(): ShadowAlarmManager =
        org.robolectric.Shadows.shadowOf(
            app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        )

    private fun registryIds(): Set<String> {
        val text = repository.getSettingString(ReminderScheduler.SETTING_SCHEDULED_ALARM_IDS, "")
        if (text.isBlank()) return emptySet()
        val arr = org.json.JSONArray(text)
        return (0 until arr.length()).map { arr.getString(it) }.toSet()
    }

    @Test
    fun p4_32_deleteAllCancelsArmedReminderAlarmsBeforeClearingTheirRegistry() {
        seedReminderHabit()
        ReminderScheduler.scheduleAll(app, repository)
        assertEquals(1, alarmRepository().scheduledAlarms.size)
        assertTrue(registryIds().contains("h_reminder_p4"))

        repository.deleteAllDataSync()

        assertEquals(
            "delete-all must not leave an armed reminder behind",
            0,
            alarmRepository().scheduledAlarms.size
        )
        assertTrue(registryIds().isEmpty())
    }

    @Test
    fun p4_33_weekStartChangeRealignsArmedReminderAlarms() {
        // A registration left over from an earlier run, as if its habit had since disappeared.
        val staleIntent = ReminderScheduler.buildAlarmPendingIntent(
            app,
            habitId = "h_stale_p4",
            deliveryDate = "",
            reminderTimeLocal = ""
        )
        (app.getSystemService(Context.ALARM_SERVICE) as AlarmManager).set(
            AlarmManager.RTC_WAKEUP,
            testClock.currentUtcMs + 60_000L,
            staleIntent
        )
        repository.setSettingSync(
            ReminderScheduler.SETTING_SCHEDULED_ALARM_IDS,
            "JSON",
            "[\"h_stale_p4\"]"
        )
        assertEquals(1, alarmRepository().scheduledAlarms.size)

        repository.setWeekStartSync(java.time.DayOfWeek.FRIDAY)

        assertEquals(
            "a week-start change must realign the alarm registry, not wait for reconcile",
            0,
            alarmRepository().scheduledAlarms.size
        )
        assertFalse(registryIds().contains("h_stale_p4"))
    }

    @Test
    fun p4_34_portableBackupNeverCarriesDeviceSchedulerState() {
        seedReminderHabit()
        ReminderScheduler.scheduleAll(app, repository)
        assertTrue(registryIds().contains("h_reminder_p4"))

        val exported = JSONObject(repository.exportPortableBackupSync())
        assertFalse(
            "device-local alarm registry must not be exported",
            exported.toString().contains(ReminderScheduler.SETTING_SCHEDULED_ALARM_IDS)
        )

        // A backup that still carries the key (older or hand-edited) must not import it either.
        exported.getJSONArray("app_settings").put(
            JSONObject().apply {
                put("setting_key", ReminderScheduler.SETTING_SCHEDULED_ALARM_IDS)
                put("value_type", "JSON")
                put("value_text", "[\"h_imported_p4\"]")
                put("schema_version", 1)
                put("updated_at_utc_ms", 100)
            }
        )
        repository.restorePortableBackupSync(
            rawBackupPayload = exported.toString(),
            restoreMode = "REPLACE"
        )

        assertFalse(
            "restore must not adopt another device's alarm registry",
            registryIds().contains("h_imported_p4")
        )
    }

    @Test
    fun p4_35_weekStartChangeKeepsAnEligibleHabitArmedWhileRemovingStaleRegistrations() {
        seedReminderHabit()
        ReminderScheduler.scheduleAll(app, repository)
        assertEquals(1, alarmRepository().scheduledAlarms.size)

        // A registration whose habit no longer exists, as left behind by an earlier policy change.
        val staleIntent = ReminderScheduler.buildAlarmPendingIntent(
            app,
            habitId = "h_stale_p4",
            deliveryDate = "",
            reminderTimeLocal = ""
        )
        (app.getSystemService(Context.ALARM_SERVICE) as AlarmManager).set(
            AlarmManager.RTC_WAKEUP,
            testClock.currentUtcMs + 120_000L,
            staleIntent
        )
        repository.setSettingSync(
            ReminderScheduler.SETTING_SCHEDULED_ALARM_IDS,
            "JSON",
            "[\"h_reminder_p4\",\"h_stale_p4\"]"
        )
        assertEquals(2, alarmRepository().scheduledAlarms.size)

        repository.setWeekStartSync(java.time.DayOfWeek.FRIDAY)

        assertEquals(
            "the eligible habit must still have exactly one armed alarm after the change",
            1,
            alarmRepository().scheduledAlarms.size
        )
        assertEquals(
            "only the eligible habit may remain registered",
            setOf("h_reminder_p4"),
            registryIds()
        )
    }

    @Test
    fun p4_36_mergeRestoreNeverAdoptsADeviceSchedulerRegistry() {
        seedReminderHabit()
        ReminderScheduler.scheduleAll(app, repository)
        val before = registryIds()
        assertTrue(before.contains("h_reminder_p4"))

        val exported = JSONObject(repository.exportPortableBackupSync())
        exported.getJSONArray("app_settings").put(
            JSONObject().apply {
                put("setting_key", ReminderScheduler.SETTING_SCHEDULED_ALARM_IDS)
                put("value_type", "JSON")
                put("value_text", "[\"h_imported_p4\"]")
                put("schema_version", 1)
                put("updated_at_utc_ms", 100)
            }
        )
        val restored = repository.restorePortableBackupSync(
            rawBackupPayload = exported.toString(),
            restoreMode = "MERGE"
        )

        assertTrue(
            "the merge must complete; any remaining conflicts are the pre-existing reminders_enabled rule, not this key",
            restored.sessionState == "AWAITING_CONFLICT_RESOLUTION" || restored.sessionState == "COMPLETED"
        )
        assertEquals(
            "device-local scheduler state must not become a mergeable portable record",
            emptyList<String>(),
            repository.restoreConflicts.value
                .filter { it.recordKey == ReminderScheduler.SETTING_SCHEDULED_ALARM_IDS }
                .map { it.recordKey }
        )
        assertEquals("local scheduler state must survive the merge untouched", before, registryIds())
        assertEquals(
            "no foreign alarm may be registered by a merge restore",
            1,
            alarmRepository().scheduledAlarms.size
        )
    }
}
