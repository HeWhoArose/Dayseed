package com.example.dayseed

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.ReminderAlarmReceiver
import com.example.dayseed.data.ReminderEngine
import com.example.dayseed.data.ReminderNotifier
import com.example.dayseed.data.ReminderScheduler
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Phase 2 + 3 remediation coverage: pending AlarmManager alarms are really cancelled /
 * replaced when eligibility changes, and stale or obsolete alarms are rejected at fire time
 * without ledger writes, while legitimately delayed inexact deliveries still deliver.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DayseedReminderAlarmLifecycleTest {

    private lateinit var context: Context
    private lateinit var testClock: TestDayseedClock
    private lateinit var repository: DayseedRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testClock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 3, 15),
            currentLocalTime = LocalTime.of(10, 0),
            currentUtcMs = LocalDate.of(2026, 3, 15)
                .atTime(10, 0)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        )
        repository = DayseedRepository(context, clock = testClock, databaseName = null)
        shadowOf(context.applicationContext as Application)
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun tearDown() {
        repository.dbHelper.close()
    }

    private fun reminderHabit(habitId: String, time: String = "09:00"): Habit = Habit(
        habitId = habitId,
        name = "Habit $habitId",
        trackingMode = TrackingMode.COMPLETION_ONLY,
        targetPeriod = TargetPeriod.DAY,
        reminderMode = "AT_TIME",
        reminderTimeLocal = time
    )

    private fun enableReminders() {
        repository.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "ENUM", "true")
    }

    private fun alarmRepository(): ShadowAlarmManager =
        shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)

    private fun scheduledAlarms(): List<ShadowAlarmManager.ScheduledAlarm> =
        alarmRepository().scheduledAlarms

    private fun scheduledRequestCodes(): Set<Int> =
        scheduledAlarms().map { shadowOf(it.operation).requestCode }.toSet()

    private fun registryIds(): Set<String> {
        val text = repository.getSettingString(ReminderScheduler.SETTING_SCHEDULED_ALARM_IDS, "")
        if (text.isBlank()) return emptySet()
        val arr = org.json.JSONArray(text)
        return (0 until arr.length()).map { arr.getString(it) }.toSet()
    }

    private fun expectedTriggerMs(timeLocal: String): Long {
        val now = LocalDateTime.of(
            repository.clock.todayLocal(),
            repository.clock.nowLocalTime()
        )
        return ReminderScheduler.nextTriggerFor(timeLocal, repository.clock.zoneId(), now)!!
            .atZone(repository.clock.zoneId())
            .toInstant()
            .toEpochMilli()
    }

    // ---- Phase 2: cancel & replace, registry-backed cancellation ----

    @Test
    fun reminderTimeChangeReplacesTheExistingPendingAlarmInPlace() {
        enableReminders()
        repository.saveHabitSync(reminderHabit("h_clock", time = "09:00"))
        ReminderScheduler.scheduleAll(context, repository)

        val before = scheduledAlarms()
        assertEquals(1, before.size)
        assertEquals(setOf("h_clock"), registryIds())

        // Change the reminder time through the normal mutation path; the reschedule hook
        // and/or the next scheduleAll must move the same alarm identity to the new trigger.
        repository.saveHabitSync(reminderHabit("h_clock", time = "14:00"))
        ReminderScheduler.scheduleAll(context, repository)

        val after = scheduledAlarms()
        assertEquals(1, after.size)
        assertEquals(setOf("h_clock"), registryIds())
        assertEquals(expectedTriggerMs("14:00"), after.first().triggerAtTime)
    }

    @Test
    fun deletingAHabitCancelsItsPendingOsAlarmAndRegistryEntry() {
        enableReminders()
        repository.saveHabitSync(reminderHabit("h_keep"))
        repository.saveHabitSync(reminderHabit("h_gone", time = "18:00"))
        ReminderScheduler.scheduleAll(context, repository)
        assertEquals(2, scheduledAlarms().size)

        // Permanent deletion routes through the cancellation + reschedule hooks.
        repository.deleteHabitPermanentlySync("h_gone")
        ReminderScheduler.scheduleAll(context, repository)

        assertEquals(1, scheduledAlarms().size)
        assertEquals(
            setOf(ReminderScheduler.requestIdFor("h_keep")),
            scheduledRequestCodes()
        )
        assertEquals(setOf("h_keep"), registryIds())
    }

    @Test
    fun pausingAHabitCancelsItsPendingOsAlarm() {
        enableReminders()
        repository.saveHabitSync(reminderHabit("h_active"))
        repository.saveHabitSync(reminderHabit("h_paused", time = "17:00"))
        ReminderScheduler.scheduleAll(context, repository)
        assertEquals(2, scheduledAlarms().size)

        val paused = _pausedHabit("h_paused")
        repository.saveHabitSync(paused, effectiveDateOverride = repository.clock.todayLocal())
        assertEquals(1, scheduledAlarms().size)
        assertEquals(
            setOf(ReminderScheduler.requestIdFor("h_active")),
            scheduledRequestCodes()
        )

        // Resumption re-registers the alarm.
        repository.saveHabitSync(paused.copy(lifecycleState = HabitLifecycleState.ACTIVE),
            effectiveDateOverride = repository.clock.todayLocal())
        assertTrue(scheduledRequestCodes().contains(ReminderScheduler.requestIdFor("h_paused")))
    }

    @Test
    fun globalOptInOffCancelsAllPendingAlarmsAndEmptiesTheRegistry() {
        enableReminders()
        repository.saveHabitSync(reminderHabit("h_on"))
        ReminderScheduler.scheduleAll(context, repository)
        assertEquals(1, scheduledAlarms().size)

        repository.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "ENUM", "false")
        ReminderScheduler.scheduleAll(context, repository)

        assertEquals(0, scheduledAlarms().size)
        assertEquals(emptySet<String>(), registryIds())
    }

    @Test
    fun scheduleAllIsRepeatSafeAndDoesNotAmassAlarms() {
        enableReminders()
        repository.saveHabitSync(reminderHabit("h_repeat"))
        repeat(3) { ReminderScheduler.scheduleAll(context, repository) }
        assertEquals(1, scheduledAlarms().size)
        assertEquals(setOf("h_repeat"), registryIds())
    }

    // ---- Phase 3: stale / obsolete alarm protection at fire time ----

    private fun processAlarm(habitId: String, deliveryDate: String, reminderTime: String): String =
        ReminderAlarmReceiver.processAlarmSync(
            context,
            repository,
            habitIdRaw = habitId,
            deliveryDateRaw = deliveryDate,
            reminderTimeRaw = reminderTime
        )

    private fun ledgerStatus(habitId: String): String? =
        repository.getReminderDeliveryStatus("HABIT_DAILY:$habitId:${repository.clock.todayLocal()}")

    @Test
    fun alarmScheduledWithObsoleteReminderTimeIsRejectedWithNoLedgerWrite() {
        enableReminders()
        repository.saveHabitSync(reminderHabit("h_stale", time = "08:00"))
        val today = repository.clock.todayLocal().toString()

        // The user changed the reminder time after the alarm was scheduled but before any
        // reschedule ran: the old alarm fires and must not deliver the old 08:00 reminder.
        repository.saveHabitSync(reminderHabit("h_stale", time = "10:30"))

        assertEquals(ReminderAlarmReceiver.DECISION_STALE_CONFIG, processAlarm("h_stale", today, "08:00"))
        assertNull(ledgerStatus("h_stale"))

        // Alarm recorded for the *current* configuration is not stale: the changed alarm fires
        // at its new intended time 10:30.
        repository.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "ENUM", "true")
        testClock.currentLocalTime = LocalTime.of(10, 35)
        testClock.currentUtcMs = repository.clock.todayLocal().atTime(10, 35).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(ReminderAlarmReceiver.DECISION_POSTED, processAlarm("h_stale", today, "10:30"))
        assertEquals("DELIVERED", ledgerStatus("h_stale"))
    }

    @Test
    fun intentWithoutVerifiableExtrasIsRejectedWithoutLedgerWrite() {
        enableReminders()
        repository.saveHabitSync(reminderHabit("h_unver"))
        assertEquals(
            ReminderAlarmReceiver.DECISION_UNVERIFIABLE_INTENT,
            processAlarm("h_unver", "not-a-date", "09:00")
        )
        assertEquals(
            ReminderAlarmReceiver.DECISION_UNVERIFIABLE_INTENT,
            processAlarm("h_unver", repository.clock.todayLocal().toString(), "")
        )
        assertNull(ledgerStatus("h_unver"))
    }

    @Test
    fun alarmForDeletedHabitIsRejectedWithoutLedgerWrite() {
        enableReminders()
        repository.saveHabitSync(reminderHabit("h_dead"))
        repository.deleteHabitPermanentlySync("h_dead")
        assertEquals(
            ReminderAlarmReceiver.DECISION_INELIGIBLE_HABIT,
            processAlarm("h_dead", repository.clock.todayLocal().toString(), "09:00")
        )
        assertNull(ledgerStatus("h_dead"))
    }

    @Test
    fun legitimatelyDelayedRingInsideGraceDeliversOnceForReal() {
        enableReminders()
        repository.saveHabitSync(reminderHabit("h_delayed"))
        val today = repository.clock.todayLocal()

        // Intended 09:00; the inexact alarm actually rings at 09:07 (inside the 10-minute
        // window) — a permitted platform delay, not staleness.
        testClock.currentLocalTime = LocalTime.of(9, 7)
        testClock.currentUtcMs = today.atTime(9, 7).toInstant(ZoneOffset.UTC).toEpochMilli()

        assertEquals(ReminderAlarmReceiver.DECISION_POSTED, processAlarm("h_delayed", today.toString(), "09:00"))
        assertEquals("DELIVERED", ledgerStatus("h_delayed"))

        // The notification really exists in NotificationManager and stops existing after cancel.
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        assertTrue(shadowOf(nm).activeNotifications.isNotEmpty())
        ReminderNotifier.cancelNotification(
            context,
            ReminderEngine.platformTagFor("h_delayed"),
            ReminderEngine.platformIdFor("h_delayed")
        )
        assertTrue(shadowOf(nm).activeNotifications.isEmpty())

        // A duplicate ring of the same occurrence posts nothing further.
        assertEquals(
            ReminderAlarmReceiver.DECISION_ALREADY_DELIVERED,
            processAlarm("h_delayed", today.toString(), "09:00")
        )
        assertTrue(shadowOf(nm).activeNotifications.isEmpty())
    }

    @Test
    fun ringForAPastDeliveryDateIsStaleAndNotDelivered() {
        enableReminders()
        repository.saveHabitSync(reminderHabit("h_past"))
        val today = repository.clock.todayLocal()

        assertEquals(
            ReminderAlarmReceiver.DECISION_STALE_TOO_LATE,
            processAlarm("h_past", today.minusDays(1).toString(), "09:00")
        )
        assertNull(ledgerStatus("h_past"))
    }

    @Test
    fun ringFarBeyondGraceAfterIntendedTimeIsStaleAndNotDelivered() {
        repository.saveHabitSync(reminderHabit("h_late"))
        enableReminders()
        val today = repository.clock.todayLocal()

        // Intended 09:00, ring at 16:30 (> 6 h grace later): obsolete rather than delayed.
        testClock.currentLocalTime = LocalTime.of(16, 30)
        testClock.currentUtcMs = today.atTime(16, 30).toInstant(ZoneOffset.UTC).toEpochMilli()

        assertEquals(
            ReminderAlarmReceiver.DECISION_STALE_TOO_LATE,
            processAlarm("h_late", today.toString(), "09:00")
        )
        assertNull(ledgerStatus("h_late"))
    }

    @Test
    fun ringBeforeTheIntendedWindowIsNotDeliveredEarly() {
        enableReminders()
        repository.saveHabitSync(reminderHabit("h_early"))
        val today = repository.clock.todayLocal()

        // Intended 09:00; the phone clock/deferral would ring at 08:30, before the configured
        // reminder is due: the user asked to be nudged at 09:00, not at 08:30.
        testClock.currentLocalTime = LocalTime.of(8, 30)
        testClock.currentUtcMs = today.atTime(8, 30).toInstant(ZoneOffset.UTC).toEpochMilli()

        assertEquals(
            ReminderAlarmReceiver.DECISION_TOO_EARLY,
            processAlarm("h_early", today.toString(), "09:00")
        )
        assertNull(ledgerStatus("h_early"))
    }

    @Test
    fun globallyDisabledReminderIsRejectedBeforeAnyLedgerInteraction() {
        repository.saveHabitSync(reminderHabit("h_off"))
        repository.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "ENUM", "false")

        assertEquals(
            ReminderAlarmReceiver.DECISION_GLOBALLY_DISABLED,
            processAlarm("h_off", repository.clock.todayLocal().toString(), "09:00")
        )
        assertNull(ledgerStatus("h_off"))
    }

    // ---- helpers ----

    private fun _pausedHabit(habitId: String): Habit {
        val current = repository.habits.value.first { it.habitId == habitId }
        return current.copy(lifecycleState = HabitLifecycleState.PAUSED)
    }
}
