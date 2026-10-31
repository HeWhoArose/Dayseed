package com.example.dayseed.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

// ============================================================================
// CORE ENUMS & CANONICAL VALUE OBJECTS
// ============================================================================

enum class TrackingMode(val label: String) {
    QUANTITY("Quantity (e.g. glasses, pages)"),
    DURATION("Duration (e.g. minutes, hours)"),
    COMPLETION_ONLY("Simple Check-Off"),
    EFFORT_AWARE("Effort-Aware (Tiny / Usual / Extra)"),
    EFFORT_LEVELS("Effort-Aware (Tiny / Usual / Extra)");

    fun canonical(): TrackingMode = if (this == EFFORT_LEVELS) EFFORT_AWARE else this

    val isMeasurable: Boolean
        get() = canonical() == QUANTITY || canonical() == DURATION

    val isOutcomeBased: Boolean
        get() = canonical() == COMPLETION_ONLY || canonical() == EFFORT_AWARE

    companion object {
        fun fromDb(raw: String?): TrackingMode = when (raw?.trim()?.uppercase()) {
            "QUANTITY" -> QUANTITY
            "DURATION" -> DURATION
            "COMPLETION_ONLY" -> COMPLETION_ONLY
            "EFFORT_AWARE", "EFFORT_LEVELS" -> EFFORT_AWARE
            else -> EFFORT_AWARE
        }
    }
}

enum class TargetPeriod(val label: String) {
    DAY("Daily"),
    WEEK("Weekly")
}

enum class ScheduleKind(val label: String) {
    DAILY("Every day"),
    SELECTED_WEEKDAYS("Selected weekdays"),
    FLEXIBLE_WEEKLY_TARGET("Flexible weekly target")
}

enum class HabitLifecycleState(val label: String) {
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    ARCHIVED("Archived")
}

enum class DailyOutcome(val label: String) {
    COMPLETED("Completed")
}

enum class EffortLevel(val label: String, private val sym: String) {
    TINY("Tiny", "🌱"),
    USUAL("Usual", "🌿"),
    EXTRA("Extra", "🌳"),
    STANDARD("Done", "✓");

    fun displayName(): String = label
    fun symbol(): String = sym
}

enum class DerivedDayStatus {
    COMPLETED,
    PARTIAL,
    LOGGED_ZERO,
    OPEN_TODAY,
    UNSCHEDULED,
    INACTIVE_LIFECYCLE,
    DERIVED_MISSED
}

enum class ProgressBand(val label: String, val encouragement: String) {
    ZERO("Not started", "Even a tiny step counts when you are ready."),
    TINY_STEP("Tiny step", "You started — that breaks the inertia."),
    GETTING_STARTED("Getting started", "Building steady momentum."),
    HALFWAY("Halfway", "Halfway through your target."),
    ALMOST_THERE("Almost there", "Nearly at your full target."),
    GOAL_MET("Goal met", "Target reached for this period."),
    EXCEEDED("Exceeded", "Above and beyond your target.")
}

/**
 * Deterministic progress-band classifier for measurable habits (D-040 / AC-002):
 * - 0 -> ZERO
 * - 1%–24% -> TINY_STEP
 * - 25%–49% -> GETTING_STARTED
 * - 50%–74% -> HALFWAY
 * - 75%–99% -> ALMOST_THERE
 * - 100% -> GOAL_MET
 * - >100% -> EXCEEDED
 * Tiny integer targets (e.g. target = 1, 2, 3) skip unreachable intermediate bands cleanly.
 */
object ProgressBandClassifier {
    fun classify(actualUnits: Int, targetUnits: Int): ProgressBand {
        val safeActual = actualUnits.coerceAtLeast(0)
        val safeTarget = targetUnits.coerceAtLeast(1)
        if (safeActual == 0) return ProgressBand.ZERO
        if (safeActual == safeTarget) return ProgressBand.GOAL_MET
        if (safeActual > safeTarget) return ProgressBand.EXCEEDED

        val pct = (safeActual * 100) / safeTarget
        return when {
            pct >= 75 -> ProgressBand.ALMOST_THERE
            pct >= 50 -> ProgressBand.HALFWAY
            pct >= 25 -> ProgressBand.GETTING_STARTED
            else -> ProgressBand.TINY_STEP
        }
    }

    fun displayPercentage(actualUnits: Int, targetUnits: Int): Int {
        val safeActual = actualUnits.coerceAtLeast(0)
        val safeTarget = targetUnits.coerceAtLeast(1)
        return (safeActual * 100) / safeTarget
    }

    fun reachableBandsForTarget(targetUnits: Int): List<ProgressBand> {
        val safeTarget = targetUnits.coerceAtLeast(1)
        val bands = linkedSetOf<ProgressBand>()
        for (actual in 0..(safeTarget + 1)) {
            bands.add(classify(actual, safeTarget))
        }
        return bands.toList()
    }
}

/**
 * Duration normalization helper (D-198 / AC-025).
 * Only DURATION habits with supported duration unit_code contribute to Growth Mark G2.
 */
object DurationNormalizer {
    val supportedDurationUnits = setOf("min", "mins", "minute", "minutes", "hr", "hrs", "hour", "hours")

    fun isSupportedDurationUnit(unitCode: String?): Boolean {
        if (unitCode.isNullOrBlank()) return false
        return unitCode.trim().lowercase() in supportedDurationUnits
    }

    fun toMinutes(valueUnits: Int, unitCode: String?): Long {
        if (valueUnits <= 0 || unitCode.isNullOrBlank()) return 0L
        return when (unitCode.trim().lowercase()) {
            "min", "mins", "minute", "minutes" -> valueUnits.toLong()
            "hr", "hrs", "hour", "hours" -> valueUnits.toLong() * 60L
            else -> 0L
        }
    }

    fun formatTotalMinutes(totalMinutes: Long): String {
        val safe = totalMinutes.coerceAtLeast(0L)
        if (safe < 60L) return "$safe min"
        val hours = safe / 60L
        val mins = safe % 60L
        return if (mins == 0L) "${hours}h" else "${hours}h ${mins}m"
    }
}

/**
 * ISO weekday bitmask helper (D-188):
 * Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64.
 * Valid non-empty masks are integers 1..127.
 */
object WeekdayMask {
    const val MONDAY_BIT = 1
    const val TUESDAY_BIT = 2
    const val WEDNESDAY_BIT = 4
    const val THURSDAY_BIT = 8
    const val FRIDAY_BIT = 16
    const val SATURDAY_BIT = 32
    const val SUNDAY_BIT = 64
    const val ALL_DAYS_MASK = 127
    const val ALL_WEEKDAYS = 127

    fun bitFor(day: DayOfWeek): Int = when (day) {
        DayOfWeek.MONDAY -> MONDAY_BIT
        DayOfWeek.TUESDAY -> TUESDAY_BIT
        DayOfWeek.WEDNESDAY -> WEDNESDAY_BIT
        DayOfWeek.THURSDAY -> THURSDAY_BIT
        DayOfWeek.FRIDAY -> FRIDAY_BIT
        DayOfWeek.SATURDAY -> SATURDAY_BIT
        DayOfWeek.SUNDAY -> SUNDAY_BIT
    }

    fun toMask(days: Set<DayOfWeek>): Int {
        var mask = 0
        for (day in days) {
            mask = mask or bitFor(day)
        }
        return mask
    }

    fun fromMask(mask: Int?): Set<DayOfWeek> {
        if (mask == null || mask <= 0) return emptySet()
        val result = linkedSetOf<DayOfWeek>()
        for (day in DayOfWeek.values()) {
            if ((mask and bitFor(day)) != 0) {
                result.add(day)
            }
        }
        return result
    }

    fun contains(mask: Int?, day: DayOfWeek): Boolean {
        if (mask == null || mask <= 0) return false
        return (mask and bitFor(day)) != 0
    }

    fun isValidNonEmptyMask(mask: Int?): Boolean {
        return mask != null && mask in 1..ALL_DAYS_MASK
    }
}

/**
 * Curated mood catalog (D-057, D-058, D-180):
 * 6 starter moods + 4 additional moods = 10 total.
 */
object MoodCatalog {
    data class MoodItem(
        val code: String,
        val label: String,
        val symbol: String,
        val isStarter: Boolean = true
    )

    val starterMoods: List<MoodItem> = listOf(
        MoodItem("CALM", "Calm", "🌿", isStarter = true),
        MoodItem("FOCUSED", "Focused", "🎯", isStarter = true),
        MoodItem("ENERGISED", "Energised", "☀️", isStarter = true),
        MoodItem("TIRED", "Tired", "🌙", isStarter = true),
        MoodItem("STRESSED", "Stressed", "🌧️", isStarter = true),
        MoodItem("GRATEFUL", "Grateful", "✨", isStarter = true)
    )

    val additionalMoods: List<MoodItem> = listOf(
        MoodItem("CONTENT", "Content", "🍵", isStarter = false),
        MoodItem("HOPEFUL", "Hopeful", "🌱", isStarter = false),
        MoodItem("LOW", "Low", "🍂", isStarter = false),
        MoodItem("ANXIOUS", "Anxious", "💨", isStarter = false)
    )

    val allMoods: List<MoodItem> = starterMoods + additionalMoods

    fun findByCode(code: String?): MoodItem? {
        if (code.isNullOrBlank()) return null
        return allMoods.find { it.code.equals(code.trim(), ignoreCase = true) }
    }
}

/**
 * Offline daily reflection prompt rotation (D-052).
 */
object DailyPrompts {
    val prompts: List<Pair<String, String>> = listOf(
        "prompt_small_win" to "What small win from today deserves a quiet nod?",
        "prompt_energy_shift" to "What gave you energy today, and what gently drained it?",
        "prompt_kindest_choice" to "What was the kindest thing you did for your future self today?",
        "prompt_tiny_step" to "Where did a tiny step feel better than doing nothing?",
        "prompt_friction_notice" to "What made a habit feel easier or harder than usual today?",
        "prompt_gratitude_moment" to "Which ordinary moment today felt worth remembering?",
        "prompt_let_go" to "What expectation can you set down before tomorrow begins?"
    )

    fun promptForDate(date: LocalDate): Pair<String, String> {
        val index = Math.floorMod(date.toEpochDay().toInt(), prompts.size)
        return prompts[index]
    }

    fun promptTextForId(promptId: String?): String? {
        if (promptId.isNullOrBlank()) return null
        return prompts.find { it.first == promptId }?.second ?: promptId
    }
}

/**
 * Shared week-boundary helper honoring app_settings.week_start (MONDAY or SUNDAY) (D-187).
 */
object WeekBoundaryHelper {
    fun weekBounds(referenceDate: LocalDate, weekStart: DayOfWeek): Pair<LocalDate, LocalDate> {
        val normalizedStart = if (weekStart == DayOfWeek.SUNDAY) DayOfWeek.SUNDAY else DayOfWeek.MONDAY
        val start = referenceDate.with(TemporalAdjusters.previousOrSame(normalizedStart))
        val end = start.plusDays(6)
        return start to end
    }

    fun previousWeekBounds(referenceDate: LocalDate, weekStart: DayOfWeek): Pair<LocalDate, LocalDate> {
        val (currentStart, _) = weekBounds(referenceDate, weekStart)
        val prevStart = currentStart.minusDays(7)
        val prevEnd = currentStart.minusDays(1)
        return prevStart to prevEnd
    }
}

// ============================================================================
// DOMAIN & PERSISTENCE MODELS (12 CANONICAL TABLES + VIEW PROJECTIONS)
// ============================================================================

/**
 * Convenience projection combining `habits` identity + effective `habit_versions` row.
 */
data class Habit(
    val habitId: String = UUID.randomUUID().toString(),
    val createdAtUtcMs: Long = System.currentTimeMillis(),
    val updatedAtUtcMs: Long = System.currentTimeMillis(),
    val currentVersion: HabitVersion? = null,
    val name: String,
    val description: String = "",
    val trackingMode: TrackingMode = TrackingMode.EFFORT_AWARE,
    val unitCode: String? = null,
    val targetPeriod: TargetPeriod = TargetPeriod.DAY,
    val targetValue: Int? = null,
    val scheduleKind: ScheduleKind = ScheduleKind.DAILY,
    val scheduledWeekdaysMask: Int? = null,
    val preferredWeekdaysMask: Int? = null,
    val lifecycleState: HabitLifecycleState = HabitLifecycleState.ACTIVE,
    val reminderMode: String = "OFF",
    val reminderTimeLocal: String? = "09:00",
    val tinyDescription: String = "Deliberately small action",
    val usualDescription: String = "The ordinary intended action",
    val extraDescription: String = "Optional stretch action",
    val scheduleDays: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val colorHex: String = "#286143"
) {
    val id: String get() = habitId
    val isArchived: Boolean get() = lifecycleState != HabitLifecycleState.ACTIVE

    fun isScheduledFor(date: LocalDate): Boolean {
        if (lifecycleState != HabitLifecycleState.ACTIVE) return false
        return when (scheduleKind) {
            ScheduleKind.DAILY -> true
            ScheduleKind.SELECTED_WEEKDAYS -> {
                if (scheduledWeekdaysMask != null) {
                    WeekdayMask.contains(scheduledWeekdaysMask, date.dayOfWeek)
                } else {
                    date.dayOfWeek in scheduleDays
                }
            }
            ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> true
        }
    }
}

data class HabitVersion(
    val versionId: String = UUID.randomUUID().toString(),
    val habitId: String,
    val effectiveFromLocalDate: String,
    val name: String,
    val description: String? = null,
    val trackingMode: TrackingMode,
    val unitCode: String? = null,
    val targetPeriod: TargetPeriod = TargetPeriod.DAY,
    val targetValue: Int? = null,
    val scheduleKind: ScheduleKind = ScheduleKind.DAILY,
    val scheduledWeekdaysMask: Int? = null,
    val preferredWeekdaysMask: Int? = null,
    val lifecycleState: HabitLifecycleState = HabitLifecycleState.ACTIVE,
    val reminderMode: String = "OFF",
    val reminderTimeLocal: String? = null,
    val createdAtUtcMs: Long = System.currentTimeMillis()
) {
    fun isScheduledOn(date: LocalDate): Boolean {
        if (lifecycleState != HabitLifecycleState.ACTIVE) return false
        return when (scheduleKind) {
            ScheduleKind.DAILY -> true
            ScheduleKind.SELECTED_WEEKDAYS -> WeekdayMask.contains(scheduledWeekdaysMask, date.dayOfWeek)
            ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> true
        }
    }
}

data class HabitEntry(
    val entryId: String = UUID.randomUUID().toString(),
    val habitId: String,
    val localDate: String,
    val valueUnits: Int,
    val entryNote: String? = null,
    val createdAtUtcMs: Long = System.currentTimeMillis(),
    val updatedAtUtcMs: Long = System.currentTimeMillis(),
    val idempotencyKey: String? = UUID.randomUUID().toString()
)

data class HabitDayOutcome(
    val outcomeId: String = UUID.randomUUID().toString(),
    val habitId: String,
    val localDate: String,
    val outcomeKind: String, // COMPLETED | EFFORT_LOGGED
    val effortLevel: String? = null, // TINY | USUAL | EXTRA | null
    val idempotencyKey: String? = UUID.randomUUID().toString(),
    val createdAtUtcMs: Long = System.currentTimeMillis(),
    val updatedAtUtcMs: Long = System.currentTimeMillis()
)

data class HabitDateNote(
    val noteId: String = UUID.randomUUID().toString(),
    val habitId: String,
    val localDate: String,
    val body: String,
    val createdAtUtcMs: Long = System.currentTimeMillis(),
    val updatedAtUtcMs: Long = System.currentTimeMillis()
)

data class JournalEntry(
    val journalEntryId: String = UUID.randomUUID().toString(),
    val localDate: String,
    val body: String,
    val title: String? = null,
    val primaryMoodCode: String? = null,
    val secondaryMoodCode: String? = null,
    val promptId: String? = null,
    val linkedHabitId: String? = null,
    val createdAtUtcMs: Long = System.currentTimeMillis(),
    val updatedAtUtcMs: Long = System.currentTimeMillis()
) {
    val id: String get() = journalEntryId
    val date: LocalDate get() = LocalDate.parse(localDate)
    val content: String get() = body
    val prompt: String get() = DailyPrompts.promptTextForId(promptId) ?: ""
}

data class JournalDraft(
    val draftId: String = UUID.randomUUID().toString(),
    val localDate: String? = null,
    val title: String? = null,
    val body: String = "",
    val primaryMoodCode: String? = null,
    val secondaryMoodCode: String? = null,
    val promptId: String? = null,
    val linkedHabitId: String? = null,
    val intendedJournalEntryId: String? = null,
    val sourceContext: String? = "MANUAL",
    val createdAtUtcMs: Long = System.currentTimeMillis(),
    val updatedAtUtcMs: Long = System.currentTimeMillis()
)

data class AppSetting(
    val settingKey: String,
    val valueType: String,
    val valueText: String,
    val schemaVersion: Int = 1,
    val updatedAtUtcMs: Long = System.currentTimeMillis()
)

data class ReminderDelivery(
    val deliveryKey: String,
    val reminderType: String,
    val habitId: String? = null,
    val targetPeriodKey: String,
    val deliveryStatus: String,
    val scheduledAtUtcMs: Long,
    val attemptedAtUtcMs: Long? = null,
    val deliveredAtUtcMs: Long? = null,
    val platformNotificationTag: String? = null,
    val platformNotificationId: Int? = null,
    val suppressionReason: String? = null
)

data class RestoreSession(
    val restoreSessionId: String,
    val restoreMode: String,
    val formatVersion: Int,
    val sourceSchemaVersion: Int,
    val sessionState: String,
    val startedAtUtcMs: Long,
    val updatedAtUtcMs: Long,
    val manifestJson: String,
    val safetyBackupRef: String,
    val safetyBackupVerified: Int
)

enum class ConflictResolution(val label: String) {
    KEEP_CURRENT("Keep current device record"),
    USE_BACKUP("Use backup record"),
    KEEP_BOTH("Keep both records")
}

data class RestoreConflict(
    val conflictId: String,
    val restoreSessionId: String,
    val recordType: String,
    val recordKey: String,
    val currentPayloadJson: String,
    val backupPayloadJson: String,
    val createdAtUtcMs: Long
) {
    /**
     * D-191 / AC-022:
     * Single-cardinality records (habit_version, habit_day_outcome, habit_date_note, app_setting)
     * ONLY allow KEEP_CURRENT or USE_BACKUP.
     * Multi-record entities (journal_entry, habit_entry) allow KEEP_CURRENT, USE_BACKUP, or KEEP_BOTH.
     */
    fun allowedResolutions(): List<ConflictResolution> {
        return when (recordType.lowercase()) {
            "journal_entry", "habit_entry" -> listOf(
                ConflictResolution.KEEP_CURRENT,
                ConflictResolution.USE_BACKUP,
                ConflictResolution.KEEP_BOTH
            )
            else -> listOf(
                ConflictResolution.KEEP_CURRENT,
                ConflictResolution.USE_BACKUP
            )
        }
    }
}

data class NotificationCancelOutboxItem(
    val cancelId: String,
    val platformNotificationTag: String,
    val platformNotificationId: Int,
    val createdAtUtcMs: Long,
    val lastAttemptAtUtcMs: Long? = null,
    val attemptCount: Int = 0,
    val status: String = "PENDING",
    val errorCategory: String? = null
)

enum class UndoEntityType {
    HABIT_ENTRY,
    HABIT_DAY_OUTCOME,
    HABIT_DATE_NOTE
}

/**
 * 8-second guarded undo token (D-209 / AC-023).
 */
data class UndoToken(
    val tokenId: String = UUID.randomUUID().toString(),
    val entityType: UndoEntityType,
    val entityId: String,
    val expectedUpdatedAtUtcMs: Long,
    val previousPayloadJson: String? = null,
    val createdAtUtcMs: Long,
    val expiresAtUtcMs: Long = createdAtUtcMs + 8_000L
)

enum class ComebackTier {
    DAYS_14,
    DAYS_30
}

data class ComebackInfo(
    val daysAway: Int,
    val tier: ComebackTier,
    val episodeKey: String,
    val title: String,
    val message: String,
    val primaryActionLabel: String,
    val secondaryActionLabel: String
)

/**
 * Growth Mark 3-metric state (D-198 / D-034 / AC-025):
 * - G1: All-time active days across habit_entries + habit_day_outcomes
 * - G2: Current calendar-month normalized duration minutes across DURATION habits
 * - G3: Current calendar-month 12-dot rhythmic ring + session intensity map
 */
data class GrowthMetrics(
    val totalActiveDates: Int = 0,
    val totalHabitsCompleted: Int = 0,
    val tinyCount: Int = 0,
    val usualCount: Int = 0,
    val extraCount: Int = 0,
    val plannedRestCount: Int = 0,
    val journalEntriesCount: Int = 0,
    val currentPeriodCompletions: Int = 0,
    val currentPeriodOpportunities: Int = 0,
    val currentMonthDurationMinutes: Long = 0L,
    val currentMonthDateSessionCounts: Map<LocalDate, Int> = emptyMap()
) {
    val allTimeActiveDays: Int get() = totalActiveDates
}

data class MoodDistributionItem(
    val moodCode: String,
    val label: String,
    val symbol: String,
    val count: Int,
    val percentage: Int,
    val previousPeriodCount: Int = 0
)

data class MoodInsightsResult(
    val periodLabel: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalEntriesWithPrimaryMood: Int,
    val previousPeriodTotalEntries: Int,
    val distribution: List<MoodDistributionItem>,
    val matchingEntries: List<JournalEntry> = emptyList()
)
