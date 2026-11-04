package com.example.dayseed.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Durable background reconciliation (WorkManager). Runs periodically:
 * - drains pending notification-cancellation work through the existing outbox processor,
 * - (re)registers inexact alarm triggers from the latest repository state so habit changes,
 *   restores, missed scheduling gaps or device restarts self-heal within a short interval.
 *
 * Per the owner's scheduling decision this worker never triggers user-facing reminders
 * directly; it only repairs the schedule. Timing-accurate Journal captions and copy keep
 * describing reminders as "scheduled ... may be slightly delayed".
 */
class ReminderReconcileWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        return try {
            val repository = DayseedRepository(applicationContext)
            try {
                // Cancellation retries (D-192/D-203 pipeline) first.
                repository.processCancelOutboxSync()
                // Then bring the alarm schedule back in line with current settings/habits.
                ReminderScheduler.scheduleAll(applicationContext, repository)
            } finally {
                repository.dbHelper.close()
            }
            Result.success()
        } catch (_: Exception) {
            // Transient SQLite/provider issues: let WorkManager retry with backoff.
            Result.retry()
        }
    }
}
