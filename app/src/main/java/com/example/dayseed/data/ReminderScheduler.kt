package com.example.dayseed.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
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
 * Delivery remains gated end-to-end by the receiver's re-checks (global opt-in, Android
 * notification permission, ledger/idempotency via [ReminderEngine]); the switches in
 * Settings stay disabled until delivery passes physical-device verification.
 */
object ReminderScheduler {

    const val ALARM_ACTION = "com.example.dayseed.action.REMINDER_ALARM"
    const val EXTRA_HABIT_ID = "habit_id"
    const val EXTRA_DELIVERY_DATE = "delivery_date"

    /** Inexact delivery window around the intended local time (10 minutes). */
    const val DELIVERY_WINDOW_MS: Long = 10L * 60L * 1000L

    /** Durable background reconciliation cadence (WorkManager periodic). */
    const val RECONCILE_WORK_NAME = "dayseed_reminder_reconcile"
    const val RECONCILE_INTERVAL_MINUTES: Long = 15L

    const val SETTING_REMINDERS_ENABLED = "reminders_enabled"

    /**
     * (Re)registers every due inexact alarm from current repository state, cancels alarms for
     * habits that no longer have an active reminder, and ensures the periodic reconcile worker
     * exists. Safe to call any time; only registered when schedule inputs are present.
     */
    fun scheduleAll(context: Context, repository: DayseedRepository) {
        val appContext = context.applicationContext

        if (!isGlobalRemindersEnabled(repository)) {
            // Delivery not opted in yet: nothing to trigger, but keep the durable reconcile
            // worker alive so a later opt-in is picked up without waiting for an app restart.
            ensurePeriodicReconcile(appContext)
            return
        }

        val zone = repository.clock.zoneId()
        val today = repository.clock.todayLocal()
        val now = LocalDateTime.of(today, repository.clock.nowLocalTime())

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            ensurePeriodicReconcile(appContext)
            return
        }

        var scheduledCount = 0
        for (habit in repository.habits.value) {
            val effective = repository.getEffectiveHabitVersion(habit.habitId, today) ?: continue
            if (effective.lifecycleState != HabitLifecycleState.ACTIVE) continue
            if (effective.reminderMode == "OFF") continue

            val timeLocal = effective.reminderTimeLocal ?: "09:00"
            val trigger = nextTriggerFor(timeLocal, zone, now) ?: continue
            val pendingIntent = buildAlarmPendingIntent(
                appContext,
                habitId = habit.habitId,
                deliveryDate = trigger.toLocalDate().toString()
            )
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                trigger.atZone(zone).toInstant().toEpochMilli(),
                DELIVERY_WINDOW_MS,
                pendingIntent
            )
            scheduledCount++
        }

        ensurePeriodicReconcile(appContext)
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

    fun buildAlarmPendingIntent(
        context: Context,
        habitId: String,
        deliveryDate: String
    ): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ALARM_ACTION
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_DELIVERY_DATE, deliveryDate)
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

    fun cancelAlarm(context: Context, habitId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(
            buildAlarmPendingIntent(context, habitId, deliveryDate = "")
        )
    }

    fun isGlobalRemindersEnabled(repository: DayseedRepository): Boolean =
        repository.getSettingString(SETTING_REMINDERS_ENABLED, "false").equals("true", ignoreCase = true)

    /**
     * Registers the durable periodic reconciliation work. WorkManager is used only for
     * deferrable maintenance — never for a user-facing clock-time trigger (owner decision).
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
