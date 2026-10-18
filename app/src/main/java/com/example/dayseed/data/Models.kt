package com.example.dayseed.data

import java.time.DayOfWeek
import java.time.LocalDate

enum class TrackingMode {
    EFFORT_LEVELS, // Tiny, Usual, Extra
    COMPLETION_ONLY // Done
}

enum class EffortLevel {
    TINY,
    USUAL,
    EXTRA,
    STANDARD; // Used for completion-only habits

    fun displayName(): String = when (this) {
        TINY -> "Tiny"
        USUAL -> "Usual"
        EXTRA -> "Extra"
        STANDARD -> "Done"
    }

    fun symbol(): String = when (this) {
        TINY -> "🌱"
        USUAL -> "🌿"
        EXTRA -> "🌳"
        STANDARD -> "✓"
    }
}

enum class DailyOutcome {
    COMPLETED,
    SKIPPED,
    PLANNED_REST
}

data class Habit(
    val id: String,
    val name: String,
    val description: String = "",
    val trackingMode: TrackingMode = TrackingMode.EFFORT_LEVELS,
    val tinyDescription: String = "Deliberately small action",
    val usualDescription: String = "The ordinary intended action",
    val extraDescription: String = "Optional stretch action",
    val scheduleDays: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val isArchived: Boolean = false,
    val colorHex: String = "#286143",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isScheduledFor(date: LocalDate): Boolean {
        return scheduleDays.contains(date.dayOfWeek)
    }
}

data class DailyRecord(
    val id: String,
    val habitId: String,
    val date: LocalDate,
    val outcome: DailyOutcome,
    val effortLevel: EffortLevel,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

data class JournalEntry(
    val id: String,
    val date: LocalDate,
    val prompt: String = "",
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ComebackInfo(
    val daysAway: Int,
    val title: String,
    val message: String,
    val primaryActionLabel: String,
    val secondaryActionLabel: String
)

data class GrowthMetrics(
    val totalActiveDates: Int = 0,
    val totalHabitsCompleted: Int = 0,
    val tinyCount: Int = 0,
    val usualCount: Int = 0,
    val extraCount: Int = 0,
    val plannedRestCount: Int = 0,
    val journalEntriesCount: Int = 0,
    val currentPeriodCompletions: Int = 0,
    val currentPeriodOpportunities: Int = 0
) {
    val completionRatePercentage: Int
        get() = if (currentPeriodOpportunities > 0) {
            ((currentPeriodCompletions.toDouble() / currentPeriodOpportunities) * 100).toInt().coerceIn(0, 100)
        } else 0
}
