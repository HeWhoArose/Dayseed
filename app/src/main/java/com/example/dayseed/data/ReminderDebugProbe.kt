package com.example.dayseed.data

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.dayseed.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * **Debug-only** end-to-end notification verification (Phase 4 remediation).
 *
 * Runs the [ReminderAlarmReceiver]'s real AlarmManager path: the Settings panel (debug builds
 * only) schedules a genuine inexact alarm on the reminder channel through the actual receiver,
 * which posts a real notification via [ReminderNotifier] and cancels it again automatically.
 *
 * Guarantees:
 * - compiled in and reachable only when [BuildConfig.DEBUG]; the release build does not
 *   minify (`isMinifyEnabled = false`), so the class symbols remain in the release dex but
 *   every probe entry point is a runtime no-op without DEBUG (verified 2026-11-08);
 * - never enables, writes or toggles the production `reminders_enabled` preference;
 * - never inserts or edits `reminder_deliveries` rows (no fake "delivered" records);
 * - never modifies habit or journal data.
 *
 * The state report distinguishes the four independent reasons a reminder can be blocked:
 * Dayseed's reminder preference, the Android 13+ runtime permission, app-level notification
 * enablement, and the `dayseed_reminders` channel importance — so a failed step can be
 * fixed at the right place.
 */
internal object ReminderDebugProbe {

    const val PROBE_ACTION = "com.example.dayseed.action.DEBUG_PROBE_ALARM"

    /** Dedicated notification identity so the probe can never overwrite a real reminder. */
    const val PROBE_TAG = "dayseed_debug_probe"
    const val PROBE_NOTIFICATION_ID = 999_999

    private const val SCHEDULE_DELAY_MS = 400L
    private const val SCHEDULE_WINDOW_MS = 3_000L
    private const val AUTO_CANCEL_DELAY_MS = 5_000L

    data class Step(val name: String, val ok: Boolean, val detail: String)
    data class State(val steps: List<Step>, val alarmFired: Boolean, val finished: Boolean)

    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state.asStateFlow()

    /** Visible only in debug builds; the released app must not expose any of this. */
    fun isAvailable(): Boolean = BuildConfig.DEBUG

    /** Static gate audit + real AlarmManager scheduling of the probe alarm. */
    fun startProbe(context: Context) {
        if (!BuildConfig.DEBUG) return
        val appContext = context.applicationContext
        val steps = mutableListOf<Step>()

        var repository: DayseedRepository? = null
        var preferenceOn = false
        var preferenceRead = false
        try {
            repository = DayseedRepository(appContext)
            preferenceOn = ReminderScheduler.isGlobalRemindersEnabled(repository)
            preferenceRead = true
        } catch (e: Exception) {
            steps.add(Step("Dayseed reminder preference (report only)", false, "unreadable: ${e.message ?: "exception"}"))
        } finally {
            try { repository?.dbHelper?.close() } catch (_: Exception) { }
        }
        steps.add(
            Step(
                "Dayseed reminder preference (report only)",
                preferenceRead,
                if (preferenceOn) "reminders_enabled = true" else
                    "reminders_enabled = false — reported without changing it; later gates decide"
            )
        )

        // API 26-32 has no POST_NOTIFICATIONS permission; an unguarded query would report a
        // false denial, so the probe treats "permission not required" as passing.
        val permissionRequired = android.os.Build.VERSION.SDK_INT >= 33
        val permissionGranted = !permissionRequired || ContextCompat.checkSelfPermission(
            appContext, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        steps.add(
            Step(
                "Android 13+ POST_NOTIFICATIONS permission",
                permissionGranted,
                when {
                    !permissionRequired -> "not required before API 33"
                    permissionGranted -> "granted"
                    else -> "not granted"
                }
            )
        )

        val appLevelEnabled = NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        steps.add(
            Step(
                "App notifications enabled (areNotificationsEnabled)",
                appLevelEnabled,
                if (appLevelEnabled) "app level on" else "app level off"
            )
        )

        ReminderNotifier.ensureReminderChannel(appContext)
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val channel = nm?.getNotificationChannel(ReminderNotifier.CHANNEL_ID)
        val channelBlocked = channel == null || channel.importance == NotificationManager.IMPORTANCE_NONE
        steps.add(
            Step(
                "dayseed_reminders channel",
                !channelBlocked,
                when {
                    channel == null -> "channel missing (Android defaulting — check system settings)"
                    channel.importance == NotificationManager.IMPORTANCE_NONE ->
                        "channel blocked (importance NONE)"
                    else -> "channel importance ${channel.importance} (${channel.name})"
                }
            )
        )

        val gatesPass = permissionGranted && appLevelEnabled && !channelBlocked
        if (gatesPass) {
            val scheduled = try {
                val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + SCHEDULE_DELAY_MS,
                    SCHEDULE_WINDOW_MS,
                    probePendingIntent(appContext)
                )
                true
            } catch (e: Exception) {
                steps.add(Step("AlarmManager.setWindow (real alarm)", false, e.message ?: "exception"))
                false
            }
            if (scheduled) {
                steps.add(
                    Step(
                        "AlarmManager.setWindow (real alarm)",
                        true,
                        "probe alarm pending (inexact, ~$SCHEDULE_DELAY_MS ms ahead). " +
                            "Android 12+ may hold a short inexact window for up to ~10 minutes — " +
                            "a delayed fire is a permitted platform delay, not a failed probe."
                    )
                )
            }
        } else {
            steps.add(
                Step(
                    "Hard gates before the alarm",
                    false,
                    "fix the reported gate(s); the probe stops so the failure stays obvious"
                )
            )
        }

        _state.value = State(steps, alarmFired = false, finished = true)
    }

    /** Called by [ReminderAlarmReceiver] when the probe alarm actually fires. */
    fun onProbeAlarm(context: Context) {
        if (!BuildConfig.DEBUG) return
        val appContext = context.applicationContext

        val steps = mutableListOf<Step>()
        steps.add(Step("Alarm received via the real broadcast path", true, "ReminderAlarmReceiver fired the probe alarm"))

        val posted = ReminderNotifier.postDebugProbeReminder(appContext)
        steps.add(
            Step(
                "Real NotificationManager.notify on dayseed_reminders",
                posted,
                if (posted) "test notification posted (tag $PROBE_TAG)" else
                    "notify did not accept the post — check the gates above"
            )
        )

        // Merge into the existing probe state so the panel shows the full run.
        _state.update { current ->
            State(
                steps = (current?.steps ?: emptyList()) + steps,
                alarmFired = true,
                finished = false
            )
        }

        // Auto-cancel so the probe is unobtrusive; the stock notification keeps itself visible
        // for the user to observe in between, which is the point of the verification.
        Handler(Looper.getMainLooper()).postDelayed({
            ReminderNotifier.cancelNotification(appContext, PROBE_TAG, PROBE_NOTIFICATION_ID)
            _state.update { current ->
                current?.copy(
                    steps = current.steps + Step(
                        "Auto-cancel after $AUTO_CANCEL_DELAY_MS ms",
                        true,
                        "test notification removed (real NotificationManager.cancel)"
                    ),
                    finished = true
                ) ?: current
            }
        }, AUTO_CANCEL_DELAY_MS)
    }

    private fun probePendingIntent(appContext: Context): PendingIntent {
        val intent = Intent(appContext, ReminderAlarmReceiver::class.java).apply {
            action = PROBE_ACTION
        }
        return PendingIntent.getBroadcast(
            appContext,
            PROBE_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
