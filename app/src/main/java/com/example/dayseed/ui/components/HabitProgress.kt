package com.example.dayseed.ui.components

import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitEntry
import com.example.dayseed.data.HabitSummary
import com.example.dayseed.data.ProgressBand
import com.example.dayseed.data.ProgressBandClassifier
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.UnitFormatter
import java.math.BigInteger

/**
 * Today's progress for a quantity or duration habit, computed once and shared by the compact habit row and the
 * quick-log sheet so both always show the same numbers (D-040 classification on the exact ratio).
 */
data class HabitProgress(
    val targetUnits: Int,
    val targetBig: BigInteger,
    val currentBig: BigInteger,
    val unitLabel: String,
    val isWeekly: Boolean,
    /** Weekly "N sessions" habits count entries rather than summing amounts. */
    val isSessionCount: Boolean,
    val band: ProgressBand,
    val percent: Int,
    val isGoalReached: Boolean,
    /** Progress as a fraction of the target; above 1.0 when the target is exceeded. */
    val fraction: Float,
    /** For example "6 / 10 pages". */
    val ratioText: String
) {
    /** Units still needed to reach the target (0 once reached; 1 for a session-count habit). */
    val remainingUnits: Int
        get() = when {
            isGoalReached -> 0
            isSessionCount -> 1
            else -> targetBig.subtract(currentBig).min(BigInteger.valueOf(Int.MAX_VALUE.toLong())).toInt().coerceAtLeast(1)
        }

    /** How far past the target the person is, in stored units (zero unless over). */
    val overUnits: BigInteger
        get() = if (currentBig > targetBig) currentBig.subtract(targetBig) else BigInteger.ZERO

    /** The same progress after adding [addUnits], for the live preview in the quick-log sheet. */
    fun after(addUnits: Int): HabitProgress {
        val newCurrent = currentBig.add(BigInteger.valueOf(addUnits.coerceAtLeast(0).toLong()))
        return build(targetUnits, newCurrent, unitLabel, isWeekly, isSessionCount)
    }

    /** "2 pages over" style text, or null when not over. */
    fun overText(): String? =
        if (overUnits.signum() > 0) "${UnitFormatter.formatValueWithUnitBig(overUnits, unitLabel)} over" else null

    companion object {
        internal fun build(
            targetUnits: Int,
            current: BigInteger,
            unitLabel: String,
            isWeekly: Boolean,
            isSessionCount: Boolean
        ): HabitProgress {
            val targetBig = BigInteger.valueOf(targetUnits.toLong())
            val ratio = if (UnitFormatter.isCurrencyUnit(unitLabel) || UnitFormatter.extractScale(unitLabel) > 0) {
                UnitFormatter.formatProgressRatioBig(current, targetBig, unitLabel)
            } else {
                "$current / $targetUnits ${UnitFormatter.formatUnitForCount(targetUnits, unitLabel)}"
            }
            return HabitProgress(
                targetUnits = targetUnits,
                targetBig = targetBig,
                currentBig = current,
                unitLabel = unitLabel,
                isWeekly = isWeekly,
                isSessionCount = isSessionCount,
                band = ProgressBandClassifier.classify(current, targetBig),
                percent = ProgressBandClassifier.displayPercentage(current, targetBig),
                isGoalReached = current >= targetBig,
                fraction = progressFraction(current, targetBig),
                ratioText = ratio
            )
        }
    }
}

/**
 * Progress for a quantity or duration habit, or null for check-off habits (which have no amount).
 * Mirrors the calculation the old habit card used, so numbers are unchanged.
 */
fun computeHabitProgress(habit: Habit, summary: HabitSummary?, entries: List<HabitEntry>): HabitProgress? {
    val mode = (summary?.effectiveVersion?.trackingMode ?: habit.trackingMode).canonical()
    if (mode != TrackingMode.QUANTITY && mode != TrackingMode.DURATION) return null

    val isWeekly = (summary?.effectiveVersion?.targetPeriod ?: habit.targetPeriod) == TargetPeriod.WEEK
    val target = if (isWeekly && summary != null) {
        summary.weekTargetUnits.min(BigInteger.valueOf(Int.MAX_VALUE.toLong())).toInt().coerceAtLeast(1)
    } else {
        (summary?.effectiveVersion?.targetValue ?: habit.targetValue ?: 1).coerceAtLeast(1)
    }
    val unitLabel = summary?.effectiveVersion?.unitCode
        ?: habit.unitCode
        ?: if (habit.trackingMode == TrackingMode.DURATION) "min" else "units"
    val isSessionCount = isWeekly && mode == TrackingMode.QUANTITY && unitLabel.trim().equals("sessions", ignoreCase = true)
    val today = if (isSessionCount) {
        BigInteger.valueOf(entries.count { it.valueUnits > 0 }.toLong())
    } else {
        ProgressBandClassifier.sumUnitsExact(entries.map { it.valueUnits })
    }
    val current = if (isWeekly && summary != null) summary.weekTotalUnits else today
    return HabitProgress.build(target, current, unitLabel, isWeekly, isSessionCount)
}
