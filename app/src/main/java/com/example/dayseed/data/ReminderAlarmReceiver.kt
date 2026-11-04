package com.example.dayseed.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires when an inexact AlarmManager ring reaches one habit's intended reminder time.
 *
 * Delivery pipeline (all steps re-checked at fire time — a scheduled alarm is never treated
 * as a delivered reminder):
 * 1. Global opt-in (`reminders_enabled` setting, currently disabled in Settings pending device
 *    verification) and the real Android `POST_NOTIFICATIONS` permission are checked first;
 *    a failure here performs **no ledger writes at all**.
 * 2. [ReminderEngine.evaluateHabitReminder] runs through [DayseedRepository.evaluateTodayRemindersSync]
 *    applying quiet hours, habit state, already-logged state, idempotency and the shared
 *    3-per-day cap, then records the outcome in `reminder_deliveries`.
 * 3. Only a *fresh* DELIVERED result (never a replay of an already-delivered record) is
 *    posted as an Android notification; if posting fails, the ledger row is corrected back to
 *    SUPPRESSED/POST_FAILED so the delivery record always reflects reality.
 * 4. The next day's alarm is re-registered either way so the schedule self-heals.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ALARM_ACTION) return
        val habitId = intent.getStringExtra(ReminderScheduler.EXTRA_HABIT_ID) ?: return

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            var repository: DayseedRepository? = null
            try {
                val appContext = context.applicationContext
                repository = DayseedRepository(appContext)
                handleAlarm(appContext, repository, habitId)
            } catch (_: Exception) {
                // Never crash the system from a reminder; the reconcile worker repairs state.
            } finally {
                try {
                    repository?.let { ReminderScheduler.scheduleAll(context.applicationContext, it) }
                    repository?.dbHelper?.close()
                } catch (_: Exception) {
                    // keep goAsync() finish contractual
                }
                result.finish()
            }
        }
    }

    private fun handleAlarm(
        context: Context,
        repository: DayseedRepository,
        habitId: String
    ) {
        if (!ReminderScheduler.isGlobalRemindersEnabled(repository)) return

        val permissionGranted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) return

        val targetDate = repository.clock.todayLocal()
        val deliveryKey = "HABIT_DAILY:$habitId:$targetDate"
        val alreadyRecorded = repository.getReminderDeliveryStatus(deliveryKey)

        val evaluations = repository.evaluateTodayRemindersSync(targetDate)
        val target = evaluations.firstOrNull { it.habitId == habitId }
        if (target == null || target.deliveryStatus != "DELIVERED") return
        // Replay of an already-delivered record (e.g. two alarms at the same minute):
        // the notification has already been posted once — do not duplicate it.
        if (alreadyRecorded == "DELIVERED") return

        val posted = try {
            postHabitNotification(context, repository, habitId)
        } catch (_: Exception) {
            false
        }
        if (!posted) {
            repository.markReminderDeliveryNotDelivered(
                deliveryKey = deliveryKey,
                suppressionReason = "POST_FAILED",
                nowUtcMs = repository.clock.nowUtcMs()
            )
        }
    }

    private fun postHabitNotification(
        context: Context,
        repository: DayseedRepository,
        habitId: String
    ): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gentle reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Small nudges for habits and reflections"
        }
        notificationManager.createNotificationChannel(channel)

        val habitName = repository.habits.value
            .firstOrNull { it.habitId == habitId }?.name ?: "Your habit"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("Gentle nudge")
            .setContentText("Time for \"$habitName\" — your progress is more than your streak.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("A gentle note for \"$habitName\". Scheduled reminders may be slightly " +
                        "delayed by Android battery saving.")
            )
            .setAutoCancel(true)
            .build()

        notificationManager.notify(
            ReminderEngine.platformTagFor(habitId),
            ReminderEngine.platformIdFor(habitId),
            notification
        )
        return true
    }

    companion object {
        const val CHANNEL_ID = "dayseed_reminders"
    }
}
