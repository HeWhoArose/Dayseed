package com.example.dayseed.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires when an inexact AlarmManager ring reaches one habit's intended reminder time.
 *
 * Delivery pipeline (all steps re-checked at fire time — a scheduled alarm is never treated
 * as a delivered reminder):
 * 1. Global opt-in (`reminders_enabled` setting, disabled in Settings pending device
 *    verification) and the real Android `POST_NOTIFICATIONS` permission are checked first;
 *    a failure here performs **no ledger writes at all**.
 * 2. [ReminderEngine.evaluateHabitReminder] runs through [DayseedRepository.evaluateTodayRemindersSync]
 *    (one serialised transaction) applying quiet hours, habit state, already-logged state,
 *    idempotency and the shared 3-per-day cap (counting DELIVERED + SCHEDULED), then writes the
 *    outcome to `reminder_deliveries`. A fresh deliverable is stored as **SCHEDULED** — a
 *    reservation, not a delivery claim.
 * 3. Only a *pending or fresh reservation owned by this invocation* is actually posted —
 *    never a replay of an already-DELIVERED record. On posting success the reservation row
 *    is promoted to `DELIVERED`; if posting fails it is corrected back to `SUPPRESSED`/
 *    `POST_FAILED`, so the ledger always matches reality. Stale reservations (process died
 *    between reservation and post) are recovered by the reconcile worker.
 * 4. The next day's alarm is re-registered either way so the schedule self-heals.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ALARM_ACTION) return

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            var repository: DayseedRepository? = null
            try {
                val appContext = context.applicationContext
                repository = DayseedRepository(appContext)
                handleAlarm(
                    appContext,
                    repository,
                    intent.getStringExtra(ReminderScheduler.EXTRA_HABIT_ID)
                )
            } catch (_: Exception) {
                // Never crash the system from a reminder; the reconcile worker repairs state.
            } finally {
                try {
                    repository?.let { ReminderScheduler.scheduleAll(context.applicationContext, it) }
                    repository?.dbHelper?.close()
                } catch (_: Exception) {
                    // keep goAsync() finish contractual
                }
                try {
                    result.finish()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun handleAlarm(
        context: Context,
        repository: DayseedRepository,
        habitIdRaw: String?
    ) {
        val habitId = habitIdRaw ?: return

        if (!ReminderScheduler.isGlobalRemindersEnabled(repository)) return
        val permissionGranted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) return

        val targetDate = repository.clock.todayLocal()
        val alreadyRecorded = repository.getReminderDeliveryStatus("HABIT_DAILY:$habitId:$targetDate")

        val evaluations = repository.evaluateTodayRemindersSync(targetDate)
        val target = evaluations.firstOrNull { it.habitId == habitId } ?: return

        val freshReservation = alreadyRecorded == null && target.deliveryStatus == "SCHEDULED"
        val retryPending = alreadyRecorded == "SCHEDULED" && target.deliveryStatus == "SCHEDULED"
        // Already-DELIVERED replays (two alarms at the same minute) must not post twice.
        if (!freshReservation && !retryPending) return

        val posted = ReminderNotifier.postHabitReminder(
            context,
            ReminderEngine.platformTagFor(habitId),
            ReminderEngine.platformIdFor(habitId),
            repository.habits.value.firstOrNull { it.habitId == habitId }?.name ?: "your habit"
        )
        if (posted) {
            repository.markReminderDeliveryDelivered(
                deliveryKey = "HABIT_DAILY:$habitId:$targetDate",
                nowUtcMs = repository.clock.nowUtcMs()
            )
        } else {
            repository.markReminderDeliveryNotDelivered(
                deliveryKey = "HABIT_DAILY:$habitId:$targetDate",
                suppressionReason = "POST_FAILED",
                nowUtcMs = repository.clock.nowUtcMs()
            )
        }
    }

    companion object {
        const val CHANNEL_ID = ReminderNotifier.CHANNEL_ID
    }
}
