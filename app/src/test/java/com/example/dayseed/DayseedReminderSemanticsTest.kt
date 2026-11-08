package com.example.dayseed

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.Habit
import com.example.dayseed.data.ReminderScheduler
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.WeekdayMask
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * D-229 owner decision batch (accepted 2026-11-06, R-WR-01..05 with the R-WR-02 owner
 * modification): reminder eligibility follows the canonical schedule domain used by the
 * streak/grove engine, and a met weekly target suppresses the rest of that week's reminders
 * for that habit only — other habits with reminders on those days keep their own slots.
 * REST_DAY/effort-aware TINY are logging affordances, not schedule modifiers (R-WR-05),
 * so they add no schedule-domain suppression.
 *
 * Fixed clock facts (zone UTC, default week start MONDAY):
 * - 2026-03-15 is a Sunday; 2026-03-16 is a Monday (new week).
 * - The progress week containing 2026-03-15 runs Mon 2026-03-09 .. Sun 2026-03-15,
 *   so 2026-03-13 (Friday) and 2026-03-15 belong to the same progress week.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DayseedReminderSemanticsTest {

    private lateinit var context: Context
    private lateinit var testClock: TestDayseedClock
    private lateinit var repository: DayseedRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testClock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 3, 15),
            currentLocalTime = LocalTime.of(10, 0),
            currentUtcMs = 1_773_568_800_000L
        )
        repository = DayseedRepository(context, clock = testClock, databaseName = null)
        shadowOf(context.applicationContext as Application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun tearDown() {
        repository.dbHelper.close()
    }

    private fun dailyReminderHabit(habitId: String): Habit = Habit(
        habitId = habitId,
        name = "Daily $habitId",
        trackingMode = TrackingMode.COMPLETION_ONLY,
        targetPeriod = TargetPeriod.DAY,
        reminderMode = "AT_TIME",
        reminderTimeLocal = "09:00"
    )

    /** Weekday habit that reminds only on the given weekdays. */
    private fun weekdayReminderHabit(habitId: String, days: Set<DayOfWeek>): Habit = Habit(
        habitId = habitId,
        name = "Weekday $habitId",
        trackingMode = TrackingMode.COMPLETION_ONLY,
        targetPeriod = TargetPeriod.DAY,
        scheduleKind = ScheduleKind.SELECTED_WEEKDAYS,
        scheduleDays = days,
        reminderMode = "AT_TIME",
        reminderTimeLocal = "09:00"
    )

    /**
     * Flexible weekly-target habit ("N sessions per week"). [preferredDays] null/planting an
     * empty set means "no preferred weekdays" (reminder every day); passing days plants the
     * preferred-weekday mask directly.
     */
    private fun flexibleWeeklyHabit(
        habitId: String,
        targetValue: Int,
        preferredDays: Set<DayOfWeek> = emptySet()
    ): Habit = Habit(
        habitId = habitId,
        name = "Flexible $habitId",
        trackingMode = TrackingMode.QUANTITY,
        unitCode = "sessions",
        targetPeriod = TargetPeriod.WEEK,
        targetValue = targetValue,
        scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
        scheduleDays = preferredDays,
        preferredWeekdaysMask = if (preferredDays.isEmpty()) null else WeekdayMask.toMask(preferredDays),
        reminderMode = "AT_TIME",
        reminderTimeLocal = "09:00"
    )

    private fun firstEval(habitId: String, date: LocalDate) =
        repository.evaluateTodayRemindersSync(date).first { it.habitId == habitId }

    private fun alarmManagerShadows() = shadowOf(
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    )

    // ---- R-WR-01/03: scheduled-day gate ----

    @Test
    fun weekdayHabitsDoNotReserveOnUnscheduledDays() {
        repository.saveHabitSync(weekdayReminderHabit("h_monday", setOf(DayOfWeek.MONDAY)))
        val sundayEval = firstEval("h_monday", LocalDate.of(2026, 3, 15))
        assertEquals("SUPPRESSED", sundayEval.deliveryStatus)
        assertEquals("NOT_SCHEDULED", sundayEval.suppressionReason)

        // Advance to the scheduled day; the fresh date reserves normally.
        testClock.currentLocalDate = LocalDate.of(2026, 3, 16)
        val mondayEval = firstEval("h_monday", LocalDate.of(2026, 3, 16))
        assertEquals("SCHEDULED", mondayEval.deliveryStatus)
    }

    @Test
    fun dailyHabitsRemindEveryCalendarDay() {
        repository.saveHabitSync(dailyReminderHabit("h_daily"))
        assertEquals("SCHEDULED", firstEval("h_daily", LocalDate.of(2026, 3, 15)).deliveryStatus)
        testClock.currentLocalDate = LocalDate.of(2026, 3, 16)
        assertEquals("SCHEDULED", firstEval("h_daily", LocalDate.of(2026, 3, 16)).deliveryStatus)
    }

    // ---- R-WR-04: preferred weekdays drive flexible habits' reminder days ----

    @Test
    fun flexibleHabitWithoutPreferredDaysRemindsEveryDay() {
        repository.saveHabitSync(flexibleWeeklyHabit("h_flex_open", targetValue = 3))
        assertEquals("SCHEDULED", firstEval("h_flex_open", LocalDate.of(2026, 3, 15)).deliveryStatus)
    }

    @Test
    fun flexibleHabitWithPreferredDaysRespectsThem() {
        repository.saveHabitSync(
            flexibleWeeklyHabit("h_flex_pref", targetValue = 3, preferredDays = setOf(DayOfWeek.MONDAY))
        )
        val sundayEval = firstEval("h_flex_pref", LocalDate.of(2026, 3, 15))
        assertEquals("SUPPRESSED", sundayEval.deliveryStatus)
        assertEquals("NOT_SCHEDULED", sundayEval.suppressionReason)

        testClock.currentLocalDate = LocalDate.of(2026, 3, 16)
        assertEquals(
            "SCHEDULED",
            firstEval("h_flex_pref", LocalDate.of(2026, 3, 16)).deliveryStatus
        )
    }

    // ---- R-WR-02 (owner-modified): met weekly target suppresses that habit only ----
    // Friday 2026-03-13 and Sunday 2026-03-15 share the Mon-start progress week, so a target
    // met on Friday suppresses the Sunday reminder while today itself stays unlogged.

    @Test
    fun metWeeklyTargetSuppressesThisWeeksRemindersForThatHabitOnly() {
        testClock.currentLocalDate = LocalDate.of(2026, 3, 13) // Friday, habit creation date
        repository.saveHabitSync(flexibleWeeklyHabit("h_met", targetValue = 3))
        repository.saveHabitSync(flexibleWeeklyHabit("h_open", targetValue = 3))

        // The met habit satisfied its weekly target on Friday; the other logged once only.
        repository.logHabitEntrySync("h_met", LocalDate.of(2026, 3, 13), 1)
        repository.logHabitEntrySync("h_met", LocalDate.of(2026, 3, 13), 1)
        repository.logHabitEntrySync("h_met", LocalDate.of(2026, 3, 13), 1)
        repository.logHabitEntrySync("h_open", LocalDate.of(2026, 3, 13), 1)

        testClock.currentLocalDate = LocalDate.of(2026, 3, 15) // Sunday, same progress week

        val metEval = firstEval("h_met", LocalDate.of(2026, 3, 15))
        assertEquals("SUPPRESSED", metEval.deliveryStatus)
        assertEquals("WEEKLY_TARGET_MET", metEval.suppressionReason)

        // Another habit with reminders on the same day keeps its own slot (owner modification).
        assertEquals("SCHEDULED", firstEval("h_open", LocalDate.of(2026, 3, 15)).deliveryStatus)
    }

    @Test
    fun weeklyTargetMetOnEarlierDaysStillSchedulesNextWeek() {
        testClock.currentLocalDate = LocalDate.of(2026, 3, 13)
        repository.saveHabitSync(flexibleWeeklyHabit("h_renew", targetValue = 1))
        repository.logHabitEntrySync("h_renew", LocalDate.of(2026, 3, 13), 1)

        testClock.currentLocalDate = LocalDate.of(2026, 3, 15)
        assertEquals("WEEKLY_TARGET_MET", firstEval("h_renew", LocalDate.of(2026, 3, 15)).suppressionReason)

        // New progress week (Monday 2026-03-16): the met state resets and reminders resume.
        testClock.currentLocalDate = LocalDate.of(2026, 3, 16)
        assertEquals("SCHEDULED", firstEval("h_renew", LocalDate.of(2026, 3, 16)).deliveryStatus)
    }

    @Test
    fun weeklyTargetNotYetMetStillSchedules() {
        testClock.currentLocalDate = LocalDate.of(2026, 3, 14) // Saturday, habit creation date
        repository.saveHabitSync(flexibleWeeklyHabit("h_progress", targetValue = 3))
        repository.logHabitEntrySync("h_progress", LocalDate.of(2026, 3, 14), 1)

        testClock.currentLocalDate = LocalDate.of(2026, 3, 15)
        val eval = firstEval("h_progress", LocalDate.of(2026, 3, 15))
        assertEquals("SCHEDULED", eval.deliveryStatus)
    }

    // ---- R-WR-05: REST_DAY is a logging affordance, not a schedule modifier ----

    @Test
    fun restDayDoesNotCreateAScheduleDomainSuppression() {
        testClock.currentLocalDate = LocalDate.of(2026, 3, 16) // Monday, scheduled day
        val monday = LocalDate.of(2026, 3, 16)
        repository.saveHabitSync(weekdayReminderHabit("h_rest", setOf(DayOfWeek.MONDAY)))
        repository.setRestDayExceptionSync("h_rest", monday)
        val eval = firstEval("h_rest", monday)
        // The pre-existing REST_DAY interplay (today handled via the exception) applies as
        // ALREADY_LOGGED; D-229 adds no additional NOT_SCHEDULED suppression for it.
        assertEquals("SUPPRESSED", eval.deliveryStatus)
        assertEquals("ALREADY_LOGGED", eval.suppressionReason)
    }

    // ---- Scheduler arms alarms only for scheduled days (R-WR-01/03) ----

    @Test
    fun scheduleAllArmsTheNextScheduledDayInsteadOfAnUnscheduledToday() {
        repository.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "ENUM", "true")
        val habit = flexibleWeeklyHabit("h_sched", targetValue = 3, preferredDays = setOf(DayOfWeek.MONDAY))
        repository.saveHabitSync(habit)
        // Sunday 2026-03-15, 10:00 local, reminder 09:00: today is neither preferred nor ahead.
        ReminderScheduler.scheduleAll(context, repository)

        val alarms = alarmManagerShadows().scheduledAlarms
        assertEquals(1, alarms.size)
        // Monday 2026-03-16 09:00 UTC is armed, not Sunday and not tomorrow-by-default.
        assertEquals(
            LocalDateTime.of(2026, 3, 16, 9, 0).toEpochSecond(ZoneOffset.UTC) * 1000L,
            alarms.first().triggerAtTime
        )
        assertTrue(alarms.first().triggerAtTime > testClock.currentUtcMs)
    }

    @Test
    fun scheduleAllKeepsDailyHabitsOnTheLegacyTrigger() {
        repository.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "ENUM", "true")
        repository.saveHabitSync(dailyReminderHabit("h_keep_daily"))
        ReminderScheduler.scheduleAll(context, repository)
        val alarms = alarmManagerShadows().scheduledAlarms
        assertEquals(1, alarms.size)
        // 2026-03-15 10:00 with a 09:00 reminder → already passed today → tomorrow 09:00.
        assertEquals(
            LocalDateTime.of(2026, 3, 16, 9, 0).toEpochSecond(ZoneOffset.UTC) * 1000L,
            alarms.first().triggerAtTime
        )
    }

    @Test
    fun scheduleAllStillArmsUnmetFlexibleHabitOnItsPreferredDay() {
        repository.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "ENUM", "true")
        val habit = flexibleWeeklyHabit("h_alarm_progress", targetValue = 3, preferredDays = setOf(DayOfWeek.MONDAY))
        repository.saveHabitSync(habit)
        testClock.currentLocalDate = LocalDate.of(2026, 3, 16)
        repository.logHabitEntrySync("h_alarm_progress", LocalDate.of(2026, 3, 16), 1)
        // Monday 2026-03-16 10:00, reminder 09:00 already passed → next preferred day in a week
        // of Mondays is next Monday; the receiver re-checks met state at fire time either way.
        ReminderScheduler.scheduleAll(context, repository)
        assertEquals(1, alarmManagerShadows().scheduledAlarms.size)
        assertEquals(
            LocalDateTime.of(2026, 3, 23, 9, 0).toEpochSecond(ZoneOffset.UTC) * 1000L,
            alarmManagerShadows().scheduledAlarms.first().triggerAtTime
        )
    }

    // ---- Crash-recovery sweep honours the D-229 gates (same helper as fresh evaluation) ----

    private fun suppressionReasonFor(deliveryKey: String): String? {
        var reason: String? = null
        repository.dbHelper.writableDatabase.rawQuery(
            "SELECT suppression_reason FROM reminder_deliveries WHERE delivery_key = ?",
            arrayOf(deliveryKey)
        ).use { c ->
            if (c.moveToFirst() && !c.isNull(0)) reason = c.getString(0)
        }
        return reason
    }

    private fun makeActiveReservationStale(habitId: String) {
        // Plain SQL literal: SQLite does not accept Kotlin-style underscores in numbers.
        repository.dbHelper.writableDatabase.execSQL(
            "UPDATE reminder_deliveries SET attempted_at_utc_ms = attempted_at_utc_ms - 4000000, " +
                "scheduled_at_utc_ms = scheduled_at_utc_ms - 4000000 " +
                "WHERE delivery_key = 'HABIT_DAILY:$habitId:${repository.clock.todayLocal()}'"
        )
    }

    private fun notificationsCurrentlyVisible(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        return shadowOf(nm).activeNotifications.isNotEmpty()
    }

    @Test
    fun sweepDoesNotRepostOnADayThatIsNoLongerScheduled() {
        repository.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "ENUM", "true")
        // Monday 2026-03-16 10:00 local (UTC zone): the schedule was Monday-only.
        testClock.currentLocalDate = LocalDate.of(2026, 3, 16)
        testClock.currentUtcMs = 1_773_568_800_000L + 86_400_000L
        repository.saveHabitSync(weekdayReminderHabit("h_sweep_week", setOf(DayOfWeek.MONDAY)))
        val deliveryKey = "HABIT_DAILY:h_sweep_week:${repository.clock.todayLocal()}"
        assertEquals(
            "SCHEDULED",
            repository.evaluateTodayRemindersSync(repository.clock.todayLocal())
                .first { it.habitId == "h_sweep_week" }.deliveryStatus
        )

        // The receiver died between reserving and posting; the reservation goes stale…
        makeActiveReservationStale("h_sweep_week")
        // …and meanwhile the user re-schedules the habit away from today (no activity logged
        // today, so the edit becomes effective immediately under the default policy).
        repository.saveHabitSync(weekdayReminderHabit("h_sweep_week", setOf(DayOfWeek.FRIDAY)))

        // Advance past the stale window (30 min): 11:00 local.
        testClock.currentLocalTime = LocalTime.of(11, 0)
        testClock.currentUtcMs += 3_600_000L
        repository.reconcilePendingReminderPostsSync()

        val status = repository.getReminderDeliveryStatus(deliveryKey)
        assertEquals("SUPPRESSED", status)
        assertEquals("NOT_SCHEDULED", suppressionReasonFor(deliveryKey))
        assertFalse(notificationsCurrentlyVisible())
    }

    @Test
    fun sweepDoesNotRepostOnceTheWeeklyTargetBecameMet() {
        repository.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "ENUM", "true")
        // Sunday 2026-03-15 (week start SUNDAY): creating the habit and switching the week
        // start sets a week_start_history row effective 03-15, so Sun 03-15 and Mon 03-16
        // belong to the same progress week.
        testClock.currentLocalDate = LocalDate.of(2026, 3, 15)
        repository.setSettingSync("week_start", "STRING", "SUNDAY")
        repository.saveHabitSync(flexibleWeeklyHabit("h_sweep_met", targetValue = 1))

        // Monday 2026-03-16 10:00: nothing logged yet, so the daily reminder reserves.
        testClock.currentLocalDate = LocalDate.of(2026, 3, 16)
        testClock.currentUtcMs = 1_773_568_800_000L + 86_400_000L
        val deliveryKey = "HABIT_DAILY:h_sweep_met:${repository.clock.todayLocal()}"
        assertEquals(
            "SCHEDULED",
            repository.evaluateTodayRemindersSync(repository.clock.todayLocal())
                .first { it.habitId == "h_sweep_met" }.deliveryStatus
        )

        // The receiver dies mid-post; the user then logs the qualifying unit...
        makeActiveReservationStale("h_sweep_met")
        repository.logHabitEntrySync("h_sweep_met", LocalDate.of(2026, 3, 15), 1)

        // ...and the sweep runs an hour later, after the 30-minute window.
        testClock.currentLocalTime = LocalTime.of(11, 0)
        testClock.currentUtcMs += 3_600_000L
        repository.reconcilePendingReminderPostsSync()

        val status = repository.getReminderDeliveryStatus(deliveryKey)
        assertEquals("SUPPRESSED", status)
        assertEquals("WEEKLY_TARGET_MET", suppressionReasonFor(deliveryKey))
        assertFalse(notificationsCurrentlyVisible())

        // A fresh evaluation agrees with the sweep: met reminder for the rest of the week is
        // suppressed for this habit only, with zero cap consumption.
        val fresh = repository.evaluateTodayRemindersSync(repository.clock.todayLocal())
            .first { it.habitId == "h_sweep_met" }
        assertEquals("SUPPRESSED", fresh.deliveryStatus)
        assertEquals("WEEKLY_TARGET_MET", fresh.suppressionReason)
    }
}
