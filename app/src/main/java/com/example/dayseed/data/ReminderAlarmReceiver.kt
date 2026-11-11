package com.example.dayseed.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.dayseed.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Fires when an inexact AlarmManager ring reaches one habit's intended reminder time.
 *
 * Delivery pipeline (all steps re-checked at fire time — a scheduled alarm is never treated
 * as a delivered reminder):
 * 1. Global opt-in (`reminders_enabled` setting, disabled in Settings pending device
 *    verification) and the real Android `POST_NOTIFICATIONS` permission are checked first;
 *    a failure here performs **no ledger writes at all**.
 * 2. Phase 3 staleness validation (no ledger writes): the alarm's recorded reminder time must
 *    still match the current persisted configuration, and the recorded delivery date must
 *    still describe a plausible delivery of the *current* configuration. Obsolete alarms from
 *    an edited reminder time, a past delivery date, or a deferral longer than
 *    [ReminderScheduler.STALE_ALARM_GRACE_MS] are rejected; rings more than the inexact window
 *    before the intended time are not delivered either. Permitted platform delivery delays
 *    ([ReminderScheduler.DELIVERY_WINDOW_MS], Doze/battery) stay inside the grace and are not
 *    rejected.
 * 3. [ReminderEngine.evaluateHabitReminder] runs through
 *    [DayseedRepository.evaluateHabitReminderForDateSync] (one serialised transaction, and
 *    since D-230 scoped to THIS alarm's habit only) applying the canonical schedule gates
 *    (D-229), the notification-availability gate (D-230: runtime permission, app-level
 *    enablement, channel importance), already-logged state, idempotency and the shared
 *    3-per-day cap (counting DELIVERED + SCHEDULED), then writes the outcome to
 *    `reminder_deliveries`. A fresh deliverable is stored as **SCHEDULED** — a reservation,
 *    not a delivery claim.
 * 4. Only a *pending or fresh reservation owned by this invocation* is actually posted —
 *    never a replay of an already-DELIVERED record. On posting success the reservation row
 *    is promoted to `DELIVERED`; if posting fails it is corrected back to `SUPPRESSED`/
 *    `POST_FAILED`, so the ledger always matches reality. Stale reservations (process died
 *    between reservation and post) are recovered by the reconcile worker.
 * 5. The next day's alarm is re-registered either way so the schedule self-heals.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isProbe = intent.action == ReminderDebugProbe.PROBE_ACTION
        if (intent.action != ReminderScheduler.ALARM_ACTION &&
            !(isProbe && BuildConfig.DEBUG)
        ) return

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            var repository: DayseedRepository? = null
            try {
                val appContext = context.applicationContext
                if (isProbe) {
                    // Debug-only delivery verification: real alarm path, real post, auto-cancel.
                    ReminderDebugProbe.onProbeAlarm(appContext)
                    return@launch
                }
                repository = DayseedRepository(appContext)
                processAlarmSync(
                    appContext,
                    repository,
                    habitIdRaw = intent.getStringExtra(ReminderScheduler.EXTRA_HABIT_ID),
                    deliveryDateRaw = intent.getStringExtra(ReminderScheduler.EXTRA_DELIVERY_DATE),
                    reminderTimeRaw = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TIME_LOCAL)
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

    companion object {

        const val CHANNEL_ID = ReminderNotifier.CHANNEL_ID

        // Process-level decisions, exposed for test visibility (physical-device diagnosis and
        // Robolectric tests assert exactly which gate fired).
        const val DECISION_POSTED = "POSTED"
        const val DECISION_POST_FAILED = "POST_FAILED"
        const val DECISION_ALREADY_DELIVERED = "SKIPPED_ALREADY_DELIVERED"
        const val DECISION_SUPPRESSED = "SUPPRESSED"
        const val DECISION_NO_HABIT_ID = "SKIPPED_NO_HABIT_ID"
        const val DECISION_UNVERIFIABLE_INTENT = "SKIPPED_UNVERIFIABLE_INTENT"
        const val DECISION_INELIGIBLE_HABIT = "SKIPPED_INELIGIBLE_HABIT"
        const val DECISION_STALE_CONFIG = "SKIPPED_STALE_CONFIG"
        const val DECISION_STALE_TOO_LATE = "SKIPPED_STALE_TOO_LATE"
        const val DECISION_TOO_EARLY = "SKIPPED_TOO_EARLY"
        const val DECISION_GLOBALLY_DISABLED = "SKIPPED_GLOBALLY_DISABLED"
        const val DECISION_NO_PERMISSION = "SKIPPED_NO_PERMISSION"
        const val DECISION_NO_EVALUATION = "SKIPPED_NO_EVALUATION"

        /**
         * Fire-time validation + atomic reservation + posting ownership + posting for one alarm,
         * fully synchronised so callers (receiver and Robolectric tests) can drive it
         * deterministically.
         *
         * Returns one of the [DECISION_*] constants describing exactly which step succeeded or
         * failed. Stale/unverifiable alarms return before any ledger interaction: they never
         * write a `reminder_deliveries` row and never post a notification.
         *
         * D-231: Ledger evaluation, shared daily-cap reservation, and exclusive posting-claim
         * acquisition (`POSTING_CLAIM:<uuid>`) execute inside one atomic `BEGIN IMMEDIATE`
         * transaction via [DayseedRepository.evaluateAndClaimHabitReminderForPostingSync]. At
         * most one active poster across all SQLite connections can obtain `CLAIMED` and invoke
         * [poster]; any concurrent duplicate receiver observes the active claim or finalized row
         * inside the serialized transaction and returns [DECISION_ALREADY_DELIVERED] without
         * calling [poster].
         */
        fun processAlarmSync(
            context: Context,
            repository: DayseedRepository,
            habitIdRaw: String?,
            deliveryDateRaw: String?,
            reminderTimeRaw: String?,
            poster: (Context, String, Int, String) -> Boolean = ReminderNotifier::postHabitReminder
        ): String {
            val habitId = habitIdRaw ?: return DECISION_NO_HABIT_ID

            val zone: ZoneId = repository.clock.zoneId()
            val targetDate = repository.clock.todayLocal()
            val now: LocalDateTime = LocalDateTime.of(targetDate, repository.clock.nowLocalTime())

            // ---- Phase 3: verify this alarm still belongs to the current configuration ----
            val deliveryDate = deliveryDateRaw?.let {
                try { LocalDate.parse(it) } catch (_: Exception) { null }
            } ?: return DECISION_UNVERIFIABLE_INTENT
            val intendedTime = reminderTimeRaw?.let {
                try { LocalTime.parse(it) } catch (_: Exception) { null }
            } ?: return DECISION_UNVERIFIABLE_INTENT

            val effective = repository.getEffectiveHabitVersion(habitId, targetDate)
            if (effective == null ||
                effective.lifecycleState != HabitLifecycleState.ACTIVE ||
                effective.reminderMode == "OFF"
            ) {
                return DECISION_INELIGIBLE_HABIT
            }

            val configuredTime = effective.reminderTimeLocal ?: "09:00"
            // A stale alarm (scheduled before the user changed the reminder time) must never
            // reach the user, even though Android would happily ring it on schedule.
            if (LocalTime.parse(configuredTime) != intendedTime) {
                return DECISION_STALE_CONFIG
            }

            // Separate obsolete-alarm protection from permitted inexact delivery delays:
            // the intended fire moment is (delivery date + recorded reminder time) in the
            // device zone. Delays within [STALE_ALARM_GRACE_MS] remain a valid ring.
            val intendedFireInstant = intendedTime.atDate(deliveryDate).atZone(zone).toInstant()
            val delayMs = Duration.between(
                intendedFireInstant,
                now.atZone(zone).toInstant()
            ).toMillis()
            if (deliveryDate.isBefore(targetDate) || delayMs > ReminderScheduler.STALE_ALARM_GRACE_MS) {
                return DECISION_STALE_TOO_LATE
            }
            if (deliveryDate.isAfter(targetDate) ||
                delayMs < -ReminderScheduler.DELIVERY_WINDOW_MS
            ) {
                return DECISION_TOO_EARLY
            }

            // ---- Hard gates before any ledger interaction ----
            if (!ReminderScheduler.isGlobalRemindersEnabled(repository)) {
                return DECISION_GLOBALLY_DISABLED
            }
            // POST_NOTIFICATIONS is a runtime permission only from API 33. On API 26-32 the
            // constant is unknown to the platform, so querying it reports DENIED and would
            // silently suppress every reminder there; the notification channel is the real gate.
            val permissionGranted = android.os.Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) return DECISION_NO_PERMISSION

            // ---- D-230 + D-231: Atomic per-habit evaluation + exclusive posting-ownership claim ----
            val deliveryKey = "HABIT_DAILY:$habitId:$targetDate"
            val claimOutcome = repository.evaluateAndClaimHabitReminderForPostingSync(habitId, targetDate)
            when (claimOutcome.status) {
                DayseedRepository.ReminderPostingClaimStatus.NO_EVALUATION ->
                    return DECISION_NO_EVALUATION
                DayseedRepository.ReminderPostingClaimStatus.SUPPRESSED ->
                    return DECISION_SUPPRESSED
                DayseedRepository.ReminderPostingClaimStatus.ALREADY_CLAIMED_OR_DELIVERED ->
                    return DECISION_ALREADY_DELIVERED
                DayseedRepository.ReminderPostingClaimStatus.CLAIMED -> Unit
            }

            val claimToken = claimOutcome.claimToken ?: return DECISION_ALREADY_DELIVERED

            // ---- Real NotificationManager post; promote/correct the reservation honestly ----
            val habitName = repository.habits.value.firstOrNull { it.habitId == habitId }?.name ?: "your habit"
            val posted = try {
                poster(
                    context,
                    ReminderEngine.platformTagFor(habitId),
                    ReminderEngine.platformIdFor(habitId),
                    habitName
                )
            } catch (_: Exception) {
                false
            }
            return if (posted) {
                repository.markReminderDeliveryDelivered(
                    deliveryKey = deliveryKey,
                    nowUtcMs = repository.clock.nowUtcMs(),
                    claimToken = claimToken
                )
                DECISION_POSTED
            } else {
                repository.markReminderDeliveryNotDelivered(
                    deliveryKey = deliveryKey,
                    suppressionReason = "POST_FAILED",
                    nowUtcMs = repository.clock.nowUtcMs(),
                    claimToken = claimToken
                )
                DECISION_POST_FAILED
            }
        }
    }
}
