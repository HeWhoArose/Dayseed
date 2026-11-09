package com.example.dayseed.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Weekly reminder family (D-043, docs/23-WEEKLY-GOAL-REMINDERS.md, Phase 4A).
 *
 * Four logical occurrence types share the `reminder_deliveries` ledger with the per-habit
 * daily reminders, so the shared three-per-day cap, quiet hours, notification availability
 * and dedupe semantics stay in one place:
 *
 * - [Kind.WEEKLY_GOAL_REACHED] — one small congratulations the moment a weekly goal is met.
 * - [Kind.WEEK_START_REFLECTION] — outcome-aware fresh start at 09:00 device-local on the
 *   configured week-start day, for the most recently completed week only (never a backlog).
 * - [Kind.WEEKLY_INACTIVITY] — gentle check-in after two *complete* local days without progress
 *   on an unmet weekly goal, at most three per configured week.
 * - [Kind.PARTIAL_PROGRESS_TIP] — optional encouragement after a week of partial progress,
 *   at most three over the following seven days, never more than one per local day.
 *
 * Everything here is time-agnostic decision logic plus one atomic ledger reservation, so the
 * planner and the gates are directly testable without Android notifications.
 */
object WeeklyReminderEngine {

    /** O-9: week-start reflection is 09:00 device-local on the configured week-start day. */
    const val WEEKLY_TIME_LOCAL = "09:00"

    /** D-043: at most three inactivity check-ins per configured week. */
    const val MAX_INACTIVITY_PER_WEEK = 3

    /** docs/23: at most three partial-progress tips over the seven days after the result. */
    const val MAX_TIPS_PER_WEEK = 3

    /**
     * A check-in becomes eligible after two *complete* local days without recorded progress,
     * so the first permitted moment is the third day at 09:00.
     */
    const val INACTIVITY_FULL_DAYS = 2L

    enum class Kind(val ledgerType: String, val habitScoped: Boolean) {
        WEEKLY_GOAL_REACHED("WEEKLY_GOAL_REACHED", true),
        WEEK_START_REFLECTION("WEEK_START_REFLECTION", false),
        WEEKLY_INACTIVITY("WEEKLY_INACTIVITY", true),
        PARTIAL_PROGRESS_TIP("PARTIAL_PROGRESS_TIP", true)
    }

    enum class PreviousWeekResult { MET, PARTIAL, NONE }

    data class Plan(
        val deliveryKey: String,
        val kind: Kind,
        val habitId: String?,
        /** Local date the notification is due (also the ledger period key for the daily cap). */
        val occurrenceDate: LocalDate,
        val scheduledTimeLocal: String,
        val title: String,
        val body: String
    )

    enum class ReservationStatus {
        /** A fresh `SCHEDULED` row was reserved and claimed by [Reservation.claimToken]. */
        RESERVED,

        /** Terminal: the occurrence must not be posted; the row records why. */
        SUPPRESSED,

        /** Nothing to do for this occurrence (already finalised / actively claimed elsewhere). */
        ALREADY_FINALISED,

        /**
         * A transient environment gate (global opt-in, notification availability, quiet hours).
         * No ledger row is written, so a later pass can still deliver the occurrence.
         */
        DEFERRED
    }

    data class Reservation(
        val plan: Plan,
        val status: ReservationStatus,
        val reason: String? = null,
        val claimToken: String? = null
    )

    // ---------------------------------------------------------------------
    // Occurrence identity (stable across restarts, reschedules and time changes)
    // ---------------------------------------------------------------------

    fun goalReachedKey(habitId: String, weekStart: LocalDate): String =
        "WEEKLY_GOAL_REACHED:$habitId:$weekStart"

    fun weekStartReflectionKey(weekStart: LocalDate): String =
        "WEEK_START_REFLECTION:$weekStart"

    fun inactivityKey(habitId: String, checkInDate: LocalDate): String =
        "WEEKLY_INACTIVITY:$habitId:$checkInDate"

    fun tipKey(habitId: String, tipDate: LocalDate): String =
        "PARTIAL_PROGRESS_TIP:$habitId:$tipDate"

    fun isWeeklyDeliveryKey(deliveryKey: String): Boolean {
        val prefix = deliveryKey.substringBefore(':')
        return Kind.entries.any { it.ledgerType == prefix }
    }

    fun platformTagFor(plan: Plan): String = when (plan.kind) {
        Kind.WEEK_START_REFLECTION -> "dayseed_weekly_reflection"
        else -> "dayseed_weekly_${plan.habitId ?: "reflection"}"
    }

    fun platformIdFor(plan: Plan): Int {
        val base = when (plan.kind) {
            Kind.WEEK_START_REFLECTION -> 0x5711
            Kind.WEEKLY_GOAL_REACHED -> 0x5712
            Kind.WEEKLY_INACTIVITY -> 0x5713
            Kind.PARTIAL_PROGRESS_TIP -> 0x5714
        }
        val habitPart = (plan.habitId?.hashCode() ?: 0) and 0x0000FFFF
        return ((base shl 8) or habitPart).coerceAtLeast(1)
    }

    // ---------------------------------------------------------------------
    // Copy (approved examples from docs/23; never includes journal text or mood)
    // ---------------------------------------------------------------------

    fun buildGoalReached(habitId: String, habitName: String, weekStart: LocalDate): Plan = Plan(
        deliveryKey = goalReachedKey(habitId, weekStart),
        kind = Kind.WEEKLY_GOAL_REACHED,
        habitId = habitId,
        occurrenceDate = weekStart,
        scheduledTimeLocal = WEEKLY_TIME_LOCAL,
        title = "Weekly goal complete",
        body = "“$habitName” reached its weekly goal. Well done — take a moment to appreciate your progress."
    )

    fun buildWeekStartReflection(weekStart: LocalDate, previous: PreviousWeekResult): Plan = Plan(
        deliveryKey = weekStartReflectionKey(weekStart),
        kind = Kind.WEEK_START_REFLECTION,
        habitId = null,
        occurrenceDate = weekStart,
        scheduledTimeLocal = WEEKLY_TIME_LOCAL,
        title = when (previous) {
            PreviousWeekResult.MET -> "A new week"
            PreviousWeekResult.PARTIAL -> "A fresh start"
            PreviousWeekResult.NONE -> "A gentle new week"
        },
        body = when (previous) {
            PreviousWeekResult.MET ->
                "You did it last week! A new week, a new milestone. Let's keep making progress, one step at a time."
            PreviousWeekResult.PARTIAL ->
                "You made some progress last week, and that counts. This is a fresh start — let's take one small step at a time."
            PreviousWeekResult.NONE ->
                "Last week didn't go quite as planned, and that's okay. This is a fresh start, not a reset of your progress."
        }
    )

    fun buildInactivity(habitId: String, habitName: String, checkInDate: LocalDate): Plan = Plan(
        deliveryKey = inactivityKey(habitId, checkInDate),
        kind = Kind.WEEKLY_INACTIVITY,
        habitId = habitId,
        occurrenceDate = checkInDate,
        scheduledTimeLocal = WEEKLY_TIME_LOCAL,
        title = "Still here",
        body = "“$habitName” has room for one small step this week. No pressure — anything counts."
    )

    fun buildTip(habitId: String, habitName: String, tipDate: LocalDate): Plan = Plan(
        deliveryKey = tipKey(habitId, tipDate),
        kind = Kind.PARTIAL_PROGRESS_TIP,
        habitId = habitId,
        occurrenceDate = tipDate,
        scheduledTimeLocal = WEEKLY_TIME_LOCAL,
        title = "A small step",
        body = "Progress on “$habitName” doesn't have to be perfect. One small step is enough."
    )

    // ---------------------------------------------------------------------
    // Cadence
    // ---------------------------------------------------------------------

    /**
     * Inactivity check-in dates for one unmet weekly goal.
     *
     * [anchorDate] is the last recorded progress (or rest) date, or the day before the week
     * started when the week has no progress yet. A local day only counts as "complete" once it
     * has ended, so the first check-in is [INACTIVITY_FULL_DAYS] complete days later, then at
     * the same two-day spacing, capped at [MAX_INACTIVITY_PER_WEEK] and clipped to the week.
     */
    fun inactivityCheckInDates(
        anchorDate: LocalDate,
        weekStart: LocalDate,
        weekEnd: LocalDate,
        today: LocalDate
    ): List<LocalDate> {
        val lastDeliverable = minOf(weekEnd, today)
        val dates = mutableListOf<LocalDate>()
        var candidate = anchorDate.plusDays(INACTIVITY_FULL_DAYS + 1)
        while (dates.size < MAX_INACTIVITY_PER_WEEK && !candidate.isAfter(lastDeliverable)) {
            if (!candidate.isBefore(weekStart)) {
                dates.add(candidate)
                candidate = candidate.plusDays(2)
            } else {
                candidate = candidate.plusDays(1)
            }
        }
        return dates
    }

    /**
     * Optional partial-progress tip dates: the first, third and fifth day after a completed
     * week with partial progress, so tips are at least 24 hours apart and no more than one per
     * local day, and never beyond seven days.
     */
    fun tipDates(previousWeekEnd: LocalDate, today: LocalDate): List<LocalDate> {
        return (0 until MAX_TIPS_PER_WEEK)
            .map { previousWeekEnd.plusDays(1L + it * 2L) }
            .filter { !it.isAfter(previousWeekEnd.plusDays(7)) && !it.isAfter(today) }
    }

    fun daysBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)

    // ---------------------------------------------------------------------
    // Atomic reservation
    // ---------------------------------------------------------------------

    /**
     * Reserves one weekly occurrence in the shared `reminder_deliveries` ledger.
     *
     * Contract:
     * - Terminal suppressions (habit no longer active, cap reached) write `SUPPRESSED` with a
     *   reason, so the occurrence is never retried and never claims a delivery that did not
     *   happen.
     * - Transient gates (global opt-in off, Android notification availability, quiet hours)
     *   write nothing and return [ReservationStatus.DEFERRED]; a later pass may deliver it.
     * - `WEEK_START_REFLECTION` and the other weekly types are never posted for a habit that
     *   was deleted or paused after planning: `habitEligible` is re-checked here.
     * - Posting ownership uses the same `POSTING_CLAIM:<uuid>` lease as the daily pipeline, so
     *   two passes on separate connections cannot both post the same occurrence.
     */
    fun reserve(
        db: SQLiteDatabase,
        plan: Plan,
        habitEligible: Boolean,
        globallyEnabled: Boolean,
        notificationsAvailable: Boolean,
        quietStartLocal: String,
        quietEndLocal: String,
        dailyCap: Int,
        nowUtcMs: Long,
        today: LocalDate
    ): Reservation {
        var existingStatus: String? = null
        var existingReason: String? = null
        var existingAttemptedMs = 0L
        var existingScheduledMs = 0L
        db.rawQuery(
            """
            SELECT delivery_status, suppression_reason, attempted_at_utc_ms, scheduled_at_utc_ms
            FROM ${DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES}
            WHERE delivery_key = ?
            """.trimIndent(),
            arrayOf(plan.deliveryKey)
        ).use { c ->
            if (c.moveToFirst()) {
                existingStatus = c.getString(0)
                existingReason = if (c.isNull(1)) null else c.getString(1)
                existingAttemptedMs = if (c.isNull(2)) 0L else c.getLong(2)
                existingScheduledMs = if (c.isNull(3)) 0L else c.getLong(3)
            }
        }

        when (existingStatus) {
            "DELIVERED", "SUPPRESSED", "CANCELLED", "DISMISSED", "ACTED" ->
                return Reservation(plan, ReservationStatus.ALREADY_FINALISED, existingReason)
            "SCHEDULED" -> {
                val lastActivityMs = maxOf(existingAttemptedMs, existingScheduledMs)
                val activelyClaimed = ReminderEngine.isPostingClaimToken(existingReason) &&
                    nowUtcMs - lastActivityMs < ReminderEngine.DEFAULT_MAX_PENDING_AGE_MS
                if (activelyClaimed) {
                    return Reservation(plan, ReservationStatus.ALREADY_FINALISED, null)
                }
                // A stale reservation (process died between reserve and post) is corrected
                // honestly before the occurrence is planned again.
                val suppressCv = ContentValues().apply {
                    put("delivery_status", "SUPPRESSED")
                    putNull("delivered_at_utc_ms")
                    put("suppression_reason", "PENDING_EXPIRED")
                    put("attempted_at_utc_ms", nowUtcMs)
                }
                db.update(
                    DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES,
                    suppressCv,
                    "delivery_key = ? AND delivery_status = 'SCHEDULED'",
                    arrayOf(plan.deliveryKey)
                )
                return Reservation(plan, ReservationStatus.SUPPRESSED, "PENDING_EXPIRED")
            }
        }

        if (!globallyEnabled) {
            return Reservation(plan, ReservationStatus.DEFERRED, "REMINDERS_OFF")
        }
        if (plan.kind.habitScoped && !habitEligible) {
            return Reservation(plan, ReservationStatus.SUPPRESSED, "HABIT_INACTIVE")
        }
        // A weekly occurrence that only became due after the device clock left quiet hours is
        // still deliverable; the check uses the configured weekly time, like the daily path.
        if (ReminderEngine.isWithinQuietHours(plan.scheduledTimeLocal, quietStartLocal, quietEndLocal)) {
            return Reservation(plan, ReservationStatus.DEFERRED, "QUIET_HOURS")
        }
        if (!notificationsAvailable) {
            return Reservation(plan, ReservationStatus.DEFERRED, "NOTIFICATIONS_BLOCKED")
        }

        var consumedToday = 0
        db.rawQuery(
            """
            SELECT COUNT(*) FROM ${DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES}
            WHERE target_period_key = ? AND delivery_status IN ('DELIVERED', 'SCHEDULED')
            """.trimIndent(),
            arrayOf(today.toString())
        ).use { c ->
            if (c.moveToFirst()) consumedToday = c.getInt(0)
        }
        if (consumedToday >= dailyCap) {
            val cv = ContentValues().apply {
                put("delivery_key", plan.deliveryKey)
                put("reminder_type", plan.kind.ledgerType)
                if (plan.habitId != null) put("habit_id", plan.habitId) else putNull("habit_id")
                put("target_period_key", today.toString())
                put("delivery_status", "SUPPRESSED")
                put("scheduled_at_utc_ms", nowUtcMs)
                put("attempted_at_utc_ms", nowUtcMs)
                putNull("delivered_at_utc_ms")
                putNull("platform_notification_tag")
                putNull("platform_notification_id")
                put("suppression_reason", "DAILY_CAP_REACHED")
            }
            db.insertWithOnConflict(
                DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES,
                null,
                cv,
                SQLiteDatabase.CONFLICT_IGNORE
            )
            return Reservation(plan, ReservationStatus.SUPPRESSED, "DAILY_CAP_REACHED")
        }

        val claimToken = ReminderEngine.newPostingClaimToken()
        val cv = ContentValues().apply {
            put("delivery_key", plan.deliveryKey)
            put("reminder_type", plan.kind.ledgerType)
            if (plan.habitId != null) put("habit_id", plan.habitId) else putNull("habit_id")
            put("target_period_key", today.toString())
            put("delivery_status", "SCHEDULED")
            put("scheduled_at_utc_ms", nowUtcMs)
            put("attempted_at_utc_ms", nowUtcMs)
            putNull("delivered_at_utc_ms")
            put("platform_notification_tag", platformTagFor(plan))
            put("platform_notification_id", platformIdFor(plan))
            put("suppression_reason", claimToken)
        }
        val rowId = db.insertWithOnConflict(
            DayseedDatabaseHelper.TABLE_REMINDER_DELIVERIES,
            null,
            cv,
            SQLiteDatabase.CONFLICT_IGNORE
        )
        return if (rowId == -1L) {
            // Another connection inserted the same occurrence first: it owns the post.
            Reservation(plan, ReservationStatus.ALREADY_FINALISED, null)
        } else {
            Reservation(plan, ReservationStatus.RESERVED, null, claimToken)
        }
    }

    fun newTraceId(): String = UUID.randomUUID().toString()
}
