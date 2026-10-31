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

    fun evaluateHabitReminder(
        db: SQLiteDatabase,
        habitVersion: HabitVersion,
        targetDate: LocalDate,
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
                if (existingStatus == "DELIVERED" || existingStatus == "SUPPRESSED") {
                    return ReminderEvaluationResult(
                        deliveryKey = deliveryKey,
                        habitId = habitVersion.habitId,
                        reminderType = "HABIT_DAILY",
                        targetPeriodKey = periodKey,
                        deliveryStatus = existingStatus,
                        scheduledTimeLocal = scheduledLocal,
                        suppressionReason = existingReason
                    )
                }
            }
        }

        // Count already delivered notifications on targetDate
        var deliveredCountToday = 0
        db.rawQuery(
            """
            SELECT COUNT(*) FROM ${DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES}
            WHERE target_period_key = ? AND delivery_status = 'DELIVERED'
            """.trimIndent(),
            arrayOf(periodKey)
        ).use { c ->
            if (c.moveToFirst()) {
                deliveredCountToday = c.getInt(0)
            }
        }

        val (status, reason) = when {
            habitVersion.lifecycleState != HabitLifecycleState.ACTIVE || habitVersion.reminderMode == "OFF" ->
                "SUPPRESSED" to "REMINDER_OFF_OR_INACTIVE"
            alreadyLoggedToday ->
                "SUPPRESSED" to "ALREADY_LOGGED"
            isWithinQuietHours(scheduledLocal, quietStartLocal, quietEndLocal) ->
                "SUPPRESSED" to "QUIET_HOURS"
            deliveredCountToday >= dailyCap ->
                "SUPPRESSED" to "DAILY_CAP_REACHED"
            else ->
                "DELIVERED" to null
        }

        val notifTag = "dayseed_habit_${habitVersion.habitId}"
        val notifId = (habitVersion.habitId.hashCode() and 0x7FFFFFFF).coerceAtLeast(1)

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
                        platformNotificationId = c.getInt(2),
                        createdAtUtcMs = c.getLong(3),
                        lastAttemptAtUtcMs = if (c.isNull(4)) null else c.getLong(4),
                        attemptCount = c.getInt(5),
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

    private fun parseLocalTimeOrNull(text: String): LocalTime? {
        return try {
            LocalTime.parse(text, timeFormatter)
        } catch (_: Exception) {
            null
        }
    }
}
