package com.example.dayseed.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

/**
 * The single posting surface for habit reminder notifications. Used by the alarm receiver,
 * the pending-post recovery path and the debug-only delivery verification, so the channel,
 * copy and posting semantics exist in exactly one place.
 *
 * Posting is idempotent per (tag, id): a repeat post with the same identity replaces the
 * previous notification instead of stacking a duplicate.
 */
internal object ReminderNotifier {

    const val CHANNEL_ID = "dayseed_reminders"

    fun ensureReminderChannel(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gentle reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Small nudges for habits and reflections"
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Posts the habit reminder notification. Returns true only when `notify` did not throw —
     * the caller must treat false as "not delivered" and correct the ledger accordingly.
     */
    fun postHabitReminder(context: Context, tag: String, id: Int, habitName: String): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        ensureReminderChannel(context)

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

        return try {
            notificationManager.notify(tag, id, notification)
            true
        } catch (_: Exception) {
            // Android refused the post (blocked channel, revoked permission at the last
            // moment, etc.) — the pipeline corrects the ledger row instead of leaving it
            // claiming a delivery that never happened.
            false
        }
    }

    fun cancelNotification(context: Context, tag: String, id: Int) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?.cancel(tag, id)
    }
}
