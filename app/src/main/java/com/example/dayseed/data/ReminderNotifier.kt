package com.example.dayseed.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
     * D-230 (2026-11-08 owner review): the single production delivery-availability gate,
     * mirroring the four checks the D-227 debug probe reports. Returns false whenever Android
     * would not visibly display a post on the reminder channel — no runtime permission
     * (Android 13+), app-level notifications disabled, or the reminder channel missing or
     * blocked at importance `NONE`. Callers must not reserve or post a delivery that could
     * then be recorded as delivered while the user never sees anything.
     */
    fun isNotificationDeliveryAvailable(context: Context): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        // POST_NOTIFICATIONS is a runtime permission only from API 33. On API 26-32 the
        // permission does not exist, so an unguarded check would wrongly report DENIED
        // and suppress every reminder on Android 8-12.
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        // App-level enablement (covers pre-13 app-blocking and summary states).
        if (!notificationManager.areNotificationsEnabled()) return false
        // Channel existence/importance: create-if-missing matches the post path's behaviour
        // (channels can only be renamed after creation — a user-blocked channel stays blocked).
        ensureReminderChannel(context)
        val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
        return channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /**
     * Posts the habit reminder notification. Returns true only when `notify` did not throw —
     * the caller must treat false as "not delivered" and correct the ledger accordingly.
     */
    fun postHabitReminder(context: Context, tag: String, id: Int, habitName: String): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        ensureReminderChannel(context)

        val openAppIntent = Intent(context, com.example.dayseed.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("Gentle nudge")
            .setContentText("Time for \"$habitName\" — your progress is more than your streak.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("A gentle note for \"$habitName\". Scheduled reminders may be slightly " +
                        "delayed by Android battery saving.")
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
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

    /**
     * Posts one weekly reminder occurrence (D-043 / Phase 4A: weekly goal acknowledgement,
     * week-start reflection, inactivity check-in or optional encouragement tip).
     *
     * Copy is supplied by [WeeklyReminderEngine] and never contains journal text, mood,
     * habit notes or any other private content. Tapping opens Dayseed so the user lands on the
     * app rather than a dead notification. Returns true only when `notify` did not throw.
     */
    fun postWeeklyReminder(
        context: Context,
        tag: String,
        id: Int,
        title: String,
        body: String
    ): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        ensureReminderChannel(context)

        val openAppIntent = Intent(context, com.example.dayseed.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        return try {
            notificationManager.notify(tag, id, notification)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun cancelNotification(context: Context, tag: String, id: Int) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?.cancel(tag, id)
    }

    /**
     * Posts the debug-only probe notification ([ReminderDebugProbe.PROBE_TAG]) on the same
     * production channel. Returns true only when `notify` did not throw; the probe records
     * failure instead of claiming success.
     */
    fun postDebugProbeReminder(context: Context): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        ensureReminderChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("Debug verification")
            .setContentText("Real notification test. It will be removed automatically.")
            .setAutoCancel(true)
            .build()

        return try {
            notificationManager.notify(
                com.example.dayseed.data.ReminderDebugProbe.PROBE_TAG,
                com.example.dayseed.data.ReminderDebugProbe.PROBE_NOTIFICATION_ID,
                notification
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}
