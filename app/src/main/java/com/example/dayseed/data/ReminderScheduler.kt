package com.example.dayseed.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONArray
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit


/**
 * Reminder scheduling ring (owner decision recorded for this phase).
 *
 * Division of responsibilities agreed with the owner:
 * - **AlarmManager** triggers user-facing reminders near their intended local clock time using
 *   *inexact* alarms ([AlarmManager.setWindow]). Android may shift the actual ring within the
 *   window (Doze, battery optimisation) — Dayseed copy must therefore describe reminders as
 *   "scheduled ... may be slightly delayed", never as exact-time guarantees.
 * - **WorkManager** performs durable background reconciliation (alarm re-registration, cancel
 *   outbox draining). WorkManager periodic jobs are deferrable and are never used as the
 *   user-facing clock-time trigger.
 * - Exact alarms (`setExact*`, `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`) are deliberately
 *   NOT requested: Android treats exact alarms as special access and Dayseed's gentle nudge
 *   does not need precise timing.
 *
 * Delivery remains gated end-to-end by the receiver's re-checks (global opt-in, current
 * persisted reminder configuration, Android notification permission, ledger idempotency via
 * [ReminderEngine]); the switches in Settings stay disabled until delivery passes
 * physical-device verification.
 */
object ReminderScheduler {

    const val ALARM_ACTION = "com.example.dayseed.action.REMINDER_ALARM"
    const val EXTRA_HABIT_ID = "habit_id"
    const val EXTRA_DELIVERY_DATE = "delivery_date"

    /**
     * Reminder time the alarm was scheduled for ("HH:mm"). The receiver compares it with the
     * current persisted configuration (Phase 3 stale-config protection): an alarm produced by
     * an obsolete reminder time or configuration is rejected, while an alarm recorded for
     * the *current* configuration may still be legitimately delayed by Android.
     */
    const val EXTRA_REMINDER_TIME_LOCAL = "reminder_time_local"

    /**
     * How long after its intended fire time an alarm is still trusted as a "delayed but valid"
     * delivery of the current configuration. This is intentionally far above the inexact
     * delivery window and plausible Doze/battery deferrals, yet far below a whole day: a ring
     * older than this (or for a past delivery date) is stale configuration, not a delayed
     * notification.
     */
    const val STALE_ALARM_GRACE_MS: Long = 6L * 60L * 60L * 1000L

    /** Inexact delivery window around the intended local time (10 minutes). */
    const val DELIVERY_WINDOW_MS: Long = 10L * 60L * 1000L

    /** Durable background reconciliation cadence (WorkManager periodic). */
    const val RECONCILE_WORK_NAME = "dayseed_reminder_reconcile"
    const val RECONCILE_INTERVAL_MINUTES: Long = 15L

    const val SETTING_REMINDERS_ENABLED = "reminders_enabled"

    /**
     * Persisted registry (app setting, JSON array) of the habit ids for which a pending alarm
     * was registered by the last successful [scheduleAll] run. It lets a later run cancel
     * alarms for habits that fell out of eligibility (deleted, paused, archived, reminder off,
     * global opt-in off, backup restore replacing the set, ...).
     */
    const val SETTING_SCHEDULED_ALARM_IDS = "reminder_scheduled_alarm_ids"

    /**
     * (Re)registers every due inexact alarm from current repository state and cancels pending
     * alarms for habits that no longer have an eligible reminder, then ensures the periodic
     * reconcile worker exists.
     *
     * - Cancel-and-replace for reminder time or configuration edits: re-registering with the
     *   same deterministic PendingIntent identity (request code + component + action; extra
     *   values do NOT participate in identity) and [PendingIntent.FLAG_UPDATE_CURRENT] simply
     *   moves the existing pending alarm to the new trigger.
     * - Cancellation: habit ids found in the persisted registry that are not eligible any more
     *   are cancelled via [cancelAlarm] before new alarms are registered.
     * - Idempotent/repeat-safe: every run recomputes from current repository state and writes
     *   the registry as a whole; running it repeatedly produces the same pending-alarm set.
     *   Concurrent runs could transiently cancel an alarm another connection just registered,
     *   but every caller registers from current state and each trigger re-registers, so the
     *   schedule self-heals.
     */
    fun scheduleAll(context: Context, repository: DayseedRepository) {
        val appContext = context.applicationContext

        ensurePeriodicReconcile(appContext)

        val zone = repository.clock.zoneId()
        val today = repository.clock.todayLocal()
        val now = LocalDateTime.of(today, repository.clock.nowLocalTime())

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        val eligibleIds = LinkedHashSet<String>()
        if (isGlobalRemindersEnabled(repository) && alarmManager != null) {
            for (habit in repository.habits.value) {
                val effective = repository.getEffectiveHabitVersion(habit.habitId, today) ?: continue
                if (effective.lifecycleState != HabitLifecycleState.ACTIVE) continue
                if (effective.reminderMode == "OFF") continue

                // D-229 R-WR-01/03: alarms are registered for the next *scheduled* day of the
                // habit (today only when today is a scheduled opportunity and the time is
                // still ahead of the caller clock), so weekday-masked/flexible habits no
                // longer arm alarms that can only be skipped at fire time.
                // D-230 R-FA-03: a WEEK-target habit whose weekly target is already met skips
                // arming entirely until the next progress-week boundary, where met state
                // naturally resets (U-12 frozen start-of-week version) and reminders resume.
                val timeLocal = effective.reminderTimeLocal ?: "09:00"
                var notBefore: LocalDate? = null
                if (effective.targetPeriod == TargetPeriod.WEEK) {
                    val weeklyState = repository.computeWeeklyReminderGateState(effective, today)
                    if (weeklyState.weeklyTargetMet) {
                        notBefore = weeklyState.currentWeekEndLocalDate.plusDays(1)
                    }
                }
                val trigger = nextScheduledTriggerFor(effective, timeLocal, zone, now, notBefore) ?: continue
                val pendingIntent = buildAlarmPendingIntent(
                    appContext,
                    habitId = habit.habitId,
                    deliveryDate = trigger.toLocalDate().toString(),
                    reminderTimeLocal = timeLocal
                )
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    trigger.atZone(zone).toInstant().toEpochMilli(),
                    DELIVERY_WINDOW_MS,
                    pendingIntent
                )
                eligibleIds.add(habit.habitId)
            }
        }

        // Cancel pending alarms for habits that fell out of eligibility since the previous
        // successful run (including the globally-disabled case: everything must be cancelled).
        val previousIds = readScheduledAlarmIds(repository)
        if (alarmManager != null) {
            (previousIds - eligibleIds).forEach { cancelAlarm(appContext, it) }
            writeScheduledAlarmIds(repository, eligibleIds)
        }
    }

    /** Intended next trigger for a daily local reminder time, tomorrow's if today's already passed.
     *  The "today" anchor comes from the caller-provided [now] (test/production clock), never the
     *  wall clock, so scheduling stays consistent with Dayseed's injected clock contract. */
    fun nextTriggerFor(
        timeLocal: String,
        zone: ZoneId,
        now: LocalDateTime
    ): LocalDateTime? {
        val time = try {
            LocalTime.parse(timeLocal)
        } catch (_: Exception) {
            return null
        }
        var candidate = LocalDateTime.of(now.atZone(zone).toLocalDate(), time)
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    /**
     * Schedule-aware next trigger (D-229 R-WR-01/03): the next day that is a scheduled
     * opportunity for [version] — today when today qualifies and the reminder time is still
     * ahead of [now], otherwise the first matching day within the next 7 calendar days.
     * Returns `null` only for an unparseable time. As a defensive fallback for a corrupt
     * weekday mask (no scheduled day found within a week), the legacy daily trigger is kept;
     * the receiver's evaluation still rejects any alarm on an unscheduled day with zero
     * ledger writes, so the fallback can never cause an unscheduled delivery.
     */
    fun nextScheduledTriggerFor(
        version: HabitVersion,
        timeLocal: String,
        zone: ZoneId,
        now: LocalDateTime,
        notBefore: LocalDate? = null
    ): LocalDateTime? {
        val time = try {
            LocalTime.parse(timeLocal)
        } catch (_: Exception) {
            return null
        }
        val today = now.atZone(zone).toLocalDate()
        val isScheduledOn = { day: LocalDate -> StreakAndGroveEngine.isScheduledOpportunityOnDate(version, day) }
        val scanStart = notBefore ?: today
        if (scanStart.isAfter(today)) {
            // D-230: met-weekly suppression restarts scanning at the next progress-week
            // boundary; the missing mask's defensive fallback also anchors there.
            for (offset in 0..7L) {
                val day = scanStart.plusDays(offset)
                if (isScheduledOn(day)) return LocalDateTime.of(day, time)
            }
            return LocalDateTime.of(scanStart, time)
        }
        val todayTrigger = LocalDateTime.of(today, time)
        if (isScheduledOn(today) && todayTrigger.isAfter(now)) return todayTrigger
        for (offset in 1..7L) {
            val day = today.plusDays(offset)
            if (isScheduledOn(day)) return LocalDateTime.of(day, time)
        }
        return if (todayTrigger.isAfter(now)) todayTrigger else LocalDateTime.of(today.plusDays(1), time)
    }

    fun buildAlarmPendingIntent(
        context: Context,
        habitId: String,
        deliveryDate: String,
        reminderTimeLocal: String
    ): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ALARM_ACTION
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_DELIVERY_DATE, deliveryDate)
            putExtra(EXTRA_REMINDER_TIME_LOCAL, reminderTimeLocal)
        }
        return PendingIntent.getBroadcast(
            context,
            requestIdFor(habitId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun requestIdFor(habitId: String): Int =
        ("reminder_" + habitId).hashCode() and 0x7FFFFFFF

    /**
     * Cancels a pending alarm by habit id. PendingIntent matching ignores extra values
     * (`filterEquals` compares only the action, data, type, component and categories), so the
     * extras recorded here never change the alarm's identity and never defeat cancellation.
     */
    fun cancelAlarm(context: Context, habitId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(
            buildAlarmPendingIntent(context, habitId, deliveryDate = "", reminderTimeLocal = "")
        )
    }

    private fun readScheduledAlarmIds(repository: DayseedRepository): Set<String> {
        val text = repository.getSettingString(SETTING_SCHEDULED_ALARM_IDS, "")
        if (text.isBlank()) return emptySet()
        return try {
            val arr = JSONArray(text)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (_: Exception) {
            // Corrupt registry values are treated as empty state; each trigger re-registers.
            emptySet()
        }
    }

    private fun writeScheduledAlarmIds(repository: DayseedRepository, ids: Set<String>) {
        val json = JSONArray().apply { ids.sorted().forEach { put(it) } }.toString()
        try {
            repository.setSettingSync(SETTING_SCHEDULED_ALARM_IDS, "JSON", json)
        } catch (_: Exception) {
            // Best-effort persistence: the reconcile worker re-derives alarms from habits, so
            // a failed registry write only delays cancellation, never duplicates reminders.
        }
    }

    fun isGlobalRemindersEnabled(repository: DayseedRepository): Boolean =
        repository.getSettingString(SETTING_REMINDERS_ENABLED, "false").equals("true", ignoreCase = true)

    /**
     * Registers the durable periodic reconciliation work. WorkManager is used only for
     * deferrable maintenance (alarm re-registration, cancellation outbox draining, pending
     * reservation recovery) — never for a user-facing clock-time trigger (owner decision).
     * Registration failures in non-Android test environments are swallowed: alarms alone
     * already re-register after each trigger, and receivers keep the schedule repaired.
     */
    fun ensurePeriodicReconcile(appContext: Context) {
        try {
            WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                RECONCILE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderReconcileWorker>(
                    RECONCILE_INTERVAL_MINUTES, TimeUnit.MINUTES
                ).build()
            )
        } catch (_: Exception) {
            // WorkManager unavailable (e.g. unit tests without an initialised factory): skip.
        }
    }
}
