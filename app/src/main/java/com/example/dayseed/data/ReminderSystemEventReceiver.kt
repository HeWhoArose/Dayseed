package com.example.dayseed.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-registers the reminder schedule after events that can invalidate previously scheduled
 * inexact alarms: device reboot, system time/timezone change, or an app update.
 *
 * This receiver never posts notifications — it only repairs the schedule. Actual delivery
 * always happens through [ReminderAlarmReceiver]'s re-checked pipeline.
 */
class ReminderSystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> Unit // handled below
            else -> return
        }

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            var repository: DayseedRepository? = null
            try {
                repository = DayseedRepository(context.applicationContext)
                ReminderScheduler.scheduleAll(context.applicationContext, repository)
                // Drain any pending notification-cancellation outbox while we are awake.
                repository.processCancelOutboxSync()
            } catch (_: Exception) {
                // Repairs are best-effort; the periodic reconcile worker runs next.
            } finally {
                try {
                    repository?.dbHelper?.close()
                } catch (_: Exception) {
                }
                result.finish()
            }
        }
    }
}
