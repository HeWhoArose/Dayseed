package com.example.dayseed.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ReminderEvaluationResult(
    val deliveryKey: String,
    val habitId: String?,
    val reminderType: String,
    val targetPeriodKey: String,
    val deliveryStatus: String,
    val scheduledTimeLocal: String,
    val suppressionReason: String?
)

object ReminderEngine {

    const val DEFAULT_QUIET_START = "21:00"
    const val DEFAULT_QUIET_END = "08:00"
    const val DEFAULT_DAILY_CAP = 3
    const val DEFAULT_MAX_PENDING_AGE_MS = 30L * 60L * 1000L
    const val POSTING_CLAIM_PREFIX = "POSTING_CLAIM:"

    fun newPostingClaimToken(): String = "$POSTING_CLAIM_PREFIX${UUID.randomUUID()}"

    fun isPostingClaimToken(reason: String?): Boolean =
        reason != null && reason.startsWith(POSTING_CLAIM_PREFIX)

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Checks whether a given local time ("HH:mm") falls inside quiet hours.
     * Supports overnight quiet windows such as 21:00–08:00.
     */
    fun isWithinQuietHours(
        candidateTimeLocal: String,
        quietStartLocal: String = DEFAULT_QUIET_START,
        quietEndLocal: String = DEFAULT_QUIET_END
    ): Boolean {
        val candidate = parseLocalTimeOrNull(candidateTimeLocal) ?: return false
        val start = parseLocalTimeOrNull(quietStartLocal) ?: LocalTime.of(21, 0)
        val end = parseLocalTimeOrNull(quietEndLocal) ?: LocalTime.of(8, 0)

        return if (start <= end) {
            !candidate.isBefore(start) && candidate.isBefore(end)
        } else {
            // Overnight window (e.g., 21:00 to 08:00)
            !candidate.isBefore(start) || candidate.isBefore(end)
        }
    }

    /**
     * Computes a suggested local reminder time ("HH:mm") from the past 14–28 days of recorded habit activity
     * timestamps (created_at_utc_ms) using median minute-of-day, rounded to the nearest 15-minute mark.
     */
    fun suggestOptimalReminderTime(
        activityTimestampsUtcMs: List<Long>,
        zoneId: ZoneId,
        quietStartLocal: String = DEFAULT_QUIET_START,
        quietEndLocal: String = DEFAULT_QUIET_END
    ): String? {
        if (activityTimestampsUtcMs.size < 3) return null
        val minutesOfDay = activityTimestampsUtcMs.map { ts ->
            val localTime = Instant.ofEpochMilli(ts).atZone(zoneId).toLocalTime()
            localTime.hour * 60 + localTime.minute
        }.sorted()

        val medianMinutes = minutesOfDay[minutesOfDay.size / 2]
        val roundedMinutes = ((medianMinutes + 7) / 15) * 15 % (24 * 60)
        val candidate = LocalTime.of(roundedMinutes / 60, roundedMinutes % 60).format(timeFormatter)

        // Clamp outside quiet hours if needed
        return if (isWithinQuietHours(candidate, quietStartLocal, quietEndLocal)) {
            quietEndLocal
        } else {
            candidate
        }
    }

    /**
     * Atomic daily-cap reservation and idempotent delivery decision for one habit occurrence.
     *
     * Callers must run this inside a serialised write transaction (the repository wraps the
     * whole day's evaluation in `beginTransactionNonExclusive`): the ledger read, the shared
     * daily-cap count (DELIVERED + SCHEDULED) and the row write must be one atomic step even
     * when two receivers hold separate SQLite connections.
     *
     * Ledger contract for this row:
     * - `SUPPRESSED` (with a reason) — no notification should be posted; replays stay suppressed.
     * - `SCHEDULED` (the ledger's existing reservation status) — a fresh deliverable slot was
     *   reserved; the pipeline must promote it to `DELIVERED` only after `NotificationManager.notify`
     *   succeeded, or correct it to `SUPPRESSED`/`POST_FAILED` when the post fails. SCHEDULED
     *   rows older than the recovery window are finalised by the reconcile worker.
     * - `DELIVERED` — Android accepted an actual post for this occurrence.
     *
     * Schedule gates (D-229, owner batch 2026-11-06): evaluation first consults the canonical
     * schedule domain (`isScheduledOpportunityOnDate`) so weekday-masked/flexible habits never
     * reserve on unscheduled days (`NOT_SCHEDULED`), and suppresses the rest of a week's
     * reminders once the habit's weekly target is met (`WEEKLY_TARGET_MET`, per-habit; other
     * habits and the shared cap are unaffected). No `REPLACE` rewrite occurs while a
     * `DELIVERED`/`SUPPRESSED`/`SCHEDULED` row already exists (idempotent replay).
     *
     * D-230 (2026-11-08 owner review): a required [notificationsAvailable] gate covers the
     * production delivery environment — runtime permission, app-level notification enablement
     * and the reminder channel's importance (the same checks the D-227 debug probe reports).
     * A blocked environment can never reserve a slot that a later `DELIVERED` status would
     * claim: the row records `SUPPRESSED`/`NOTIFICATIONS_BLOCKED` instead (zero cap use).
     */
    fun evaluateHabitReminder(
        db: SQLiteDatabase,
        habitVersion: HabitVersion,
        targetDate: LocalDate,
        isScheduledOpportunity: Boolean,
        weeklyTargetMet: Boolean,
        notificationsAvailable: Boolean,
        alreadyLoggedToday: Boolean,
        quietStartLocal: String,
        quietEndLocal: String,
        dailyCap: Int,
        nowUtcMs: Long
    ): ReminderEvaluationResult {
        val periodKey = targetDate.toString()
        val deliveryKey = "HABIT_DAILY:${habitVersion.habitId}:$periodKey"
        val scheduledLocal = habitVersion.reminderTimeLocal ?: "09:00"

        // Check existing ledger entry for idempotence
        db.rawQuery(
            "SELECT delivery_status, suppression_reason FROM ${DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES} WHERE delivery_key = ?",
            arrayOf(deliveryKey)
        ).use { c ->
            if (c.moveToFirst()) {
                val existingStatus = c.getString(0)
                val existingReason = if (c.isNull(1)) null else c.getString(1)
                if (existingStatus == "DELIVERED" || existingStatus == "SUPPRESSED" ||
                    existingStatus == "SCHEDULED"
                ) {
                    // SCHEDULED replays return the reservation untouched: a reserved slot
                    // must not count against itself, and only the posting pipeline (receiver,
                    // reconciler) may promote it to DELIVERED or correct it to SUPPRESSED.
                    // D-231: while a SCHEDULED row is actively claimed for posting, its
                    // internal POSTING_CLAIM:<uuid> token in suppression_reason is masked from
                    // evaluation callers so SCHEDULED rows continue to report null reason.
                    val exposedReason = if (existingStatus == "SCHEDULED" && isPostingClaimToken(existingReason)) {
                        null
                    } else {
                        existingReason
                    }
                    return ReminderEvaluationResult(
                        deliveryKey = deliveryKey,
                        habitId = habitVersion.habitId,
                        reminderType = "HABIT_DAILY",
                        targetPeriodKey = periodKey,
                        deliveryStatus = existingStatus,
                        scheduledTimeLocal = scheduledLocal,
                        suppressionReason = exposedReason
                    )
                }
            }
        }

        // Cap must be consumed by *both* confirmed deliverables and outstanding reservations
        // (SCHEDULED, an existing status of the closed-world ledger enum): a reserved slot
        // that is about to post is already spent. Rows are unique per (habit, date) via the
        // delivery_key primary key.
        var consumedCountToday = 0
        db.rawQuery(
            """
            SELECT COUNT(*) FROM ${DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES}
            WHERE target_period_key = ? AND delivery_status IN ('DELIVERED', 'SCHEDULED')
            """.trimIndent(),
            arrayOf(periodKey)
        ).use { c ->
            if (c.moveToFirst()) {
                consumedCountToday = c.getInt(0)
            }
        }

        val (status, reason) = when {
            habitVersion.lifecycleState != HabitLifecycleState.ACTIVE || habitVersion.reminderMode == "OFF" ->
                "SUPPRESSED" to "REMINDER_OFF_OR_INACTIVE"
            // D-229 R-WR-01/03: reminder eligibility follows the canonical schedule domain used
            // by the streak/grove engine — weekday-masked and flexible habits never reserve or
            // post on days that are not scheduled opportunities for the habit.
            !isScheduledOpportunity ->
                "SUPPRESSED" to "NOT_SCHEDULED"
            alreadyLoggedToday ->
                "SUPPRESSED" to "ALREADY_LOGGED"
            // D-229 R-WR-02 (owner-modified): once the habit's weekly target is met for the
            // week containing the delivery date, this habit's remaining reminders are
            // suppressed for the rest of that week. Other habits with reminders on those
            // days are unaffected: the gate is per-habit and the shared daily cap remains
            // shared among the remaining reminder-enabled habits.
            weeklyTargetMet ->
                "SUPPRESSED" to "WEEKLY_TARGET_MET"
            isWithinQuietHours(scheduledLocal, quietStartLocal, quietEndLocal) ->
                "SUPPRESSED" to "QUIET_HOURS"
            // D-230: Android would not display anything (permission/app-level/channel) — the
            // environment must be verified before anything can be reserved for a delivery.
            !notificationsAvailable ->
                "SUPPRESSED" to "NOTIFICATIONS_BLOCKED"
            consumedCountToday >= dailyCap ->
                "SUPPRESSED" to "DAILY_CAP_REACHED"
            else ->
                // Fresh deliverable: reserve the slot as SCHEDULED (the ledger's existing
                // reservation status). Only the immediate poster promotes this row to
                // DELIVERED after NotificationManager.notify succeeded (or corrects it to
                // SUPPRESSED/POST_FAILED if the post fails). A row must never claim
                // DELIVERED merely because it was inserted.
                "SCHEDULED" to null
        }

        val notifTag = platformTagFor(habitVersion.habitId)
        val notifId = platformIdFor(habitVersion.habitId)

        val cv = ContentValues().apply {
            put("delivery_key", deliveryKey)
            put("reminder_type", "HABIT_DAILY")
            put("habit_id", habitVersion.habitId)
            put("target_period_key", periodKey)
            put("delivery_status", status)
            put("scheduled_at_utc_ms", nowUtcMs)
            put("attempted_at_utc_ms", nowUtcMs)
            if (status == "DELIVERED") {
                put("delivered_at_utc_ms", nowUtcMs)
            } else {
                putNull("delivered_at_utc_ms")
            }
            put("platform_notification_tag", notifTag)
            put("platform_notification_id", notifId)
            if (reason != null) put("suppression_reason", reason) else putNull("suppression_reason")
        }

        db.insertWithOnConflict(
            DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES,
            null,
            cv,
            SQLiteDatabase.CONFLICT_REPLACE
        )

        return ReminderEvaluationResult(
            deliveryKey = deliveryKey,
            habitId = habitVersion.habitId,
            reminderType = "HABIT_DAILY",
            targetPeriodKey = periodKey,
            deliveryStatus = status,
            scheduledTimeLocal = scheduledLocal,
            suppressionReason = reason
        )
    }

    /**
     * Startup/foreground outbox processor (D-192 / D-203): retries pending platform notification cancellations.
     */
    fun processNotificationCancelOutbox(
        db: SQLiteDatabase,
        nowUtcMs: Long,
        platformCancelCallback: (tag: String, id: Int) -> Boolean = { _, _ -> true }
    ): Int {
        val pending = mutableListOf<NotificationCancelOutboxItem>()
        db.rawQuery(
            """
            SELECT cancel_id, platform_notification_tag, platform_notification_id, created_at_utc_ms,
                   last_attempt_at_utc_ms, attempt_count, status, error_category
            FROM ${DayseedDatabaseHelper.TABLE_NOTIFICATION_CANCEL_OUTBOX}
            WHERE status IN ('PENDING', 'FAILED')
            """.trimIndent(),
            null
        ).use { c ->
            while (c.moveToNext()) {
                pending.add(
                    NotificationCancelOutboxItem(
                        cancelId = c.getString(0),
                        platformNotificationTag = c.getString(1),
                        platformNotificationId = c.getValidatedInt(2, "platform_notification_id"),
                        createdAtUtcMs = c.getLong(3),
                        lastAttemptAtUtcMs = if (c.isNull(4)) null else c.getLong(4),
                        attemptCount = c.getValidatedInt(5, "attempt_count"),
                        status = c.getString(6),
                        errorCategory = if (c.isNull(7)) null else c.getString(7)
                    )
                )
            }
        }

        var completedCount = 0
        for (item in pending) {
            val succeeded = try {
                platformCancelCallback(item.platformNotificationTag, item.platformNotificationId)
            } catch (_: Exception) {
                false
            }
            val cv = ContentValues().apply {
                put("last_attempt_at_utc_ms", nowUtcMs)
                put("attempt_count", item.attemptCount + 1)
                if (succeeded) {
                    put("status", "COMPLETED")
                    putNull("error_category")
                } else {
                    put("status", "FAILED")
                    put("error_category", "PLATFORM_CANCEL_ERROR")
                }
            }
            db.update(
                DayseedDatabaseHelper.TABLE_NOTIFICATION_CANCEL_OUTBOX,
                cv,
                "cancel_id = ?",
                arrayOf(item.cancelId)
            )
            if (succeeded) completedCount++
        }
        return completedCount
    }

    fun enqueueNotificationCancel(
        db: SQLiteDatabase,
        platformNotificationTag: String,
        platformNotificationId: Int,
        nowUtcMs: Long
    ) {
        val cv = ContentValues().apply {
            put("cancel_id", UUID.randomUUID().toString())
            put("platform_notification_tag", platformNotificationTag)
            put("platform_notification_id", platformNotificationId)
            put("created_at_utc_ms", nowUtcMs)
            putNull("last_attempt_at_utc_ms")
            put("attempt_count", 0)
            put("status", "PENDING")
            putNull("error_category")
        }
        db.insertOrThrow(DayseedDatabaseHelper.TABLE_NOTIFICATION_CANCEL_OUTBOX, null, cv)
    }

    private fun parseLocalTimeOrNull(text: String): LocalTime? {
        return try {
            LocalTime.parse(text, timeFormatter)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Shared platform-notification identity so the evaluation ledger and the Android
     * notification poster use one consistent (tag, id) pair per habit.
     */
    fun platformTagFor(habitId: String): String = "dayseed_habit_$habitId"

    fun platformIdFor(habitId: String): Int =
        (habitId.hashCode() and 0x7FFFFFFF).coerceAtLeast(1)
}
