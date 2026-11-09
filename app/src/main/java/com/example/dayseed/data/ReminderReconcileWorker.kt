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
                // Crash-recovery for incomplete delivery reservations (Phase 1 remediation):
                // stale SCHEDULED reservations are re-posted while still eligible, or honestly corrected.
                repository.reconcilePendingReminderPostsSync()
                // Weekly reminder family (D-043 / Phase 4A): goal acknowledgement, week-start
                // reflection, inactivity check-in and optional tips are due-time evaluated here
                // because WorkManager may defer this pass by a few minutes (never an exact time).
                if (ReminderScheduler.isGlobalRemindersEnabled(repository)) {
                    repository.runWeeklyReminderPassSync()
                }
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
