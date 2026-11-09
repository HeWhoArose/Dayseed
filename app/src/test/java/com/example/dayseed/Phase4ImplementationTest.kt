package com.example.dayseed

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedClock
import com.example.dayseed.data.DayseedDatabaseHelper
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.Habit
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
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

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
        dbName = "dayseed_phase4_test_${UUID.randomUUID()}.db"
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

        repository.deleteAllDataSync()
        assertEquals(0, weeklyLedgerCount())
        assertEquals(0, repository.habits.value.size)
        assertEquals(0, repository.habitEntries.value.size)
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
    }
}
