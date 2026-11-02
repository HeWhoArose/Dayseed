package com.example.dayseed.data

import android.database.Cursor
import java.math.BigInteger
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

// =============================================================================
// 1. SAFE SQLITE INTEGER EXTRACTION & LOCAL DATE VALIDATION (Revision 7 §3.5, §5.4)
// =============================================================================

class DayseedNumericRangeException(message: String) : RuntimeException(message)

fun Cursor.getValidatedInt(colIndex: Int, columnName: String): Int {
    val raw = getLong(colIndex)
    if (raw < Int.MIN_VALUE.toLong() || raw > Int.MAX_VALUE.toLong()) {
        throw DayseedNumericRangeException(
            "Column '$columnName' value $raw exceeds 32-bit signed Int range."
        )
    }
    return raw.toInt()
}

fun Cursor.getNullableValidatedInt(colIndex: Int, columnName: String): Int? {
    if (isNull(colIndex)) return null
    return getValidatedInt(colIndex, columnName)
}

object LocalDateValidator {
    private val STRICT_ISO_DATE: DateTimeFormatter = DateTimeFormatter
        .ofPattern("uuuu-MM-dd", Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)

    fun parseStrict(raw: String): LocalDate {
        if (raw.length != 10) {
            throw IllegalArgumentException("Invalid ISO local date '$raw': expected 10-character YYYY-MM-DD.")
        }
        return try {
            LocalDate.parse(raw, STRICT_ISO_DATE)
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("Invalid calendar date '$raw': ${e.message}", e)
        }
    }

    fun isValidStrict(raw: String?): Boolean {
        if (raw == null || raw.length != 10) return false
        return try {
            LocalDate.parse(raw, STRICT_ISO_DATE)
            true
        } catch (_: DateTimeParseException) {
            false
        }
    }

    fun requireValid(raw: String, fieldName: String = "local_date"): String {
        parseStrict(raw)
        return raw
    }
}

// =============================================================================
// 2. TRACKING MODES, SCHEDULES, TARGET PERIODS & LIFECYCLE STATES (D-216, U-11)
// =============================================================================

enum class TrackingMode(val label: String) {
    QUANTITY("Quantity"),
    DURATION("Duration"),
    COMPLETION_ONLY("Simple check-off"),
    EFFORT_AWARE("Effort-aware (Legacy)"),
    @Deprecated("Legacy v1 alias for EFFORT_AWARE; mapped automatically during v1->v2 migration")
    EFFORT_LEVELS("Effort-aware");

    fun canonical(): TrackingMode = if (this == EFFORT_LEVELS) EFFORT_AWARE else this

    fun isCreatableForNewHabit(): Boolean = canonical() in creatableModes

    companion object {
        val creatableModes: Set<TrackingMode> = setOf(
            QUANTITY,
            DURATION,
            COMPLETION_ONLY
        )

        fun fromDb(value: String?): TrackingMode = when (value?.uppercase()) {
            "QUANTITY" -> QUANTITY
            "DURATION" -> DURATION
            "COMPLETION_ONLY", "BINARY" -> COMPLETION_ONLY
            "EFFORT_AWARE", "EFFORT_LEVELS" -> EFFORT_AWARE
            else -> EFFORT_AWARE
        }
    }
}

enum class ScheduleKind(val label: String) {
    DAILY("Every day"),
    SELECTED_WEEKDAYS("Selected weekdays"),
    FLEXIBLE_WEEKLY_TARGET("Flexible weekly target")
}

enum class TargetPeriod(val label: String) {
    DAY("Per day"),
    WEEK("Per week")
}

enum class HabitLifecycleState(val label: String) {
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    ARCHIVED("Archived")
}

enum class EffortLevel(val label: String, val badgeText: String) {
    TINY("Tiny", "Tiny Seed"),
    USUAL("Usual", "Steady Growth"),
    EXTRA("Extra", "Full Bloom"),
    STANDARD("Standard", "Completed");

    fun symbol(): String = when (this) {
        TINY -> "🌱"
        USUAL -> "🌿"
        EXTRA -> "🌳"
        STANDARD -> "✓"
    }

    fun displayName(): String = label
}

enum class DailyOutcome(val label: String) {
    COMPLETED("Completed"),
    PLANNED_REST("Rest Day"),
    MISSED("Missed")
}

enum class DerivedDayStatus {
    COMPLETED,
    PARTIAL,
    LOGGED_ZERO,
    REST_DAY,
    OPEN_TODAY,
    DERIVED_MISSED,
    UNSCHEDULED,
    INACTIVE_LIFECYCLE
}

enum class DayRecordState {
    NO_RECORD,
    EXPLICIT_ZERO,
    PARTIAL_PROGRESS,
    GOAL_MET_OR_EXCEEDED,
    REST_DAY_EXCEPTION,
    PAUSED_LIFECYCLE
}

// =============================================================================
// 3. PROGRESS BANDS & EXACT BIGINTEGER CLASSIFIER (D-040, D-216 Option A, §3.3)
// =============================================================================

enum class ProgressBand(
    val label: String,
    val shortBadge: String,
    val encouragementQuote: String,
    val motivationQuote: String
) {
    ZERO(
        label = "Zero logged",
        shortBadge = "0%",
        encouragementQuote = "Checking in honestly is still showing up for yourself.",
        motivationQuote = "Even a single small unit can shift today's momentum whenever you're ready."
    ),
    TINY_STEP(
        label = "Tiny step",
        shortBadge = "1–24%",
        encouragementQuote = "The hardest boundary is starting—you crossed it.",
        motivationQuote = "One small brick laid today keeps the path open for tomorrow."
    ),
    GETTING_STARTED(
        label = "Getting started",
        shortBadge = "25–49%",
        encouragementQuote = "You've built real traction today.",
        motivationQuote = "Steady steps compound faster than occasional bursts."
    ),
    HALFWAY(
        label = "Halfway there",
        shortBadge = "50–74%",
        encouragementQuote = "More than halfway—your consistency is taking root.",
        motivationQuote = "You're closer to the finish than the starting line."
    ),
    ALMOST_THERE(
        label = "Almost there",
        shortBadge = "75–99%",
        encouragementQuote = "Strong effort—you're right on the doorstep of your goal.",
        motivationQuote = "One last push brings today's target home."
    ),
    GOAL_MET(
        label = "Goal met",
        shortBadge = "100%",
        encouragementQuote = "Target reached—well tended today.",
        motivationQuote = "Reliability like this is how lasting habits grow."
    ),
    EXTRA(
        label = "Extra credit",
        shortBadge = "101–149%",
        encouragementQuote = "You went beyond what you asked of yourself today.",
        motivationQuote = "Surplus energy invested wisely strengthens the root."
    ),
    HUGE(
        label = "Huge day",
        shortBadge = "150%+",
        encouragementQuote = "An extraordinary session—celebrate the abundance.",
        motivationQuote = "Let today's high tide inspire calm confidence, not pressure, tomorrow."
    );

    companion object {
        @Deprecated("Use EXTRA or HUGE in v3; alias retained for binary/source compatibility")
        val EXCEEDED: ProgressBand = EXTRA
    }
}

enum class CompactHistoryBucket {
    NONE_RECORDED,
    PARTIAL,
    COMPLETED,
    EXCEEDED
}

object ProgressBandClassifier {
    private val BI_100 = BigInteger.valueOf(100L)
    private val BI_25 = BigInteger.valueOf(25L)
    private val BI_50 = BigInteger.valueOf(50L)
    private val BI_75 = BigInteger.valueOf(75L)
    private val BI_150 = BigInteger.valueOf(150L)

    fun sumUnitsExact(values: Iterable<Int>): BigInteger {
        var total = BigInteger.ZERO
        for (v in values) {
            if (v > 0) {
                total = total.add(BigInteger.valueOf(v.toLong()))
            }
        }
        return total
    }

    fun percentageFloor(recordedUnits: BigInteger, targetUnits: BigInteger): Int {
        if (targetUnits <= BigInteger.ZERO || recordedUnits <= BigInteger.ZERO) return 0
        val pct = recordedUnits.multiply(BI_100).divide(targetUnits)
        return pct.min(BigInteger.valueOf(Int.MAX_VALUE.toLong())).toInt()
    }

    fun displayPercentage(recordedUnits: Int, targetUnits: Int): Int =
        percentageFloor(BigInteger.valueOf(recordedUnits.toLong()), BigInteger.valueOf(targetUnits.toLong()))

    fun displayPercentage(recordedUnits: BigInteger, targetUnits: BigInteger): Int =
        percentageFloor(recordedUnits, targetUnits)

    fun classify(recordedUnits: Int, targetUnits: Int): ProgressBand =
        classify(BigInteger.valueOf(recordedUnits.toLong()), BigInteger.valueOf(targetUnits.toLong()))

    fun classify(recordedUnits: Long, targetUnits: Long): ProgressBand =
        classify(BigInteger.valueOf(recordedUnits), BigInteger.valueOf(targetUnits))

    fun classify(recordedUnits: BigInteger, targetUnits: BigInteger): ProgressBand {
        require(targetUnits > BigInteger.ZERO) { "targetUnits must be > 0 (was $targetUnits)" }
        if (recordedUnits <= BigInteger.ZERO) return ProgressBand.ZERO

        val scaled = recordedUnits.multiply(BI_100)
        val t25 = targetUnits.multiply(BI_25)
        val t50 = targetUnits.multiply(BI_50)
        val t75 = targetUnits.multiply(BI_75)
        val t100 = targetUnits.multiply(BI_100)
        val t150 = targetUnits.multiply(BI_150)

        return when {
            scaled < t25 -> ProgressBand.TINY_STEP
            scaled < t50 -> ProgressBand.GETTING_STARTED
            scaled < t75 -> ProgressBand.HALFWAY
            scaled < t100 -> ProgressBand.ALMOST_THERE
            scaled == t100 -> ProgressBand.GOAL_MET
            scaled < t150 -> ProgressBand.EXTRA
            else -> ProgressBand.HUGE
        }
    }

    fun toCompactBucket(band: ProgressBand): CompactHistoryBucket = when (band) {
        ProgressBand.ZERO -> CompactHistoryBucket.NONE_RECORDED
        ProgressBand.TINY_STEP,
        ProgressBand.GETTING_STARTED,
        ProgressBand.HALFWAY,
        ProgressBand.ALMOST_THERE -> CompactHistoryBucket.PARTIAL
        ProgressBand.GOAL_MET -> CompactHistoryBucket.COMPLETED
        ProgressBand.EXTRA,
        ProgressBand.HUGE -> CompactHistoryBucket.EXCEEDED
    }

    fun reachableBandsForTarget(targetUnits: Int): List<ProgressBand> {
        require(targetUnits >= 1) { "targetUnits must be >= 1" }
        val maxSample = (targetUnits.toLong() * 2L).coerceAtMost(200L).toInt()
        val bands = linkedSetOf<ProgressBand>()
        for (u in 0..maxSample) {
            bands.add(classify(u, targetUnits))
        }
        return bands.toList()
    }
}

// =============================================================================
// 4. WEEKDAY BITMASK & DURATION NORMALIZER
// =============================================================================

object WeekdayMask {
    const val ALL_DAYS_MASK = 127 // Bits 0..6 for MONDAY..SUNDAY

    fun bitFor(dayOfWeek: DayOfWeek): Int = 1 shl (dayOfWeek.value - 1)

    fun toMask(days: Set<DayOfWeek>): Int {
        var mask = 0
        for (d in days) {
            mask = mask or bitFor(d)
        }
        return mask
    }

    fun fromMask(mask: Int): Set<DayOfWeek> {
        val result = mutableSetOf<DayOfWeek>()
        for (d in DayOfWeek.values()) {
            if ((mask and bitFor(d)) != 0) {
                result.add(d)
            }
        }
        return result
    }

    fun contains(mask: Int, dayOfWeek: DayOfWeek): Boolean = (mask and bitFor(dayOfWeek)) != 0
}

object DurationNormalizer {
    private val MINUTE_UNITS = setOf("min", "mins", "minute", "minutes", "m")
    private val HOUR_UNITS = setOf("hr", "hrs", "hour", "hours", "h")
    val supportedDurationUnits: List<String> = listOf("min", "hr")

    fun isSupportedDurationUnit(unitCode: String?): Boolean {
        if (unitCode == null) return false
        val norm = unitCode.trim().lowercase()
        return norm in MINUTE_UNITS || norm in HOUR_UNITS
    }

    fun toMinutes(valueUnits: Int, unitCode: String?): Long? {
        if (valueUnits < 0) return null
        val norm = unitCode?.trim()?.lowercase() ?: "min"
        return when (norm) {
            in MINUTE_UNITS -> valueUnits.toLong()
            in HOUR_UNITS -> valueUnits.toLong() * 60L
            else -> null
        }
    }

    fun formatTotalMinutes(totalMinutes: Long): String {
        if (totalMinutes <= 0L) return "0m"
        val hours = totalMinutes / 60L
        val mins = totalMinutes % 60L
        return when {
            hours > 0L && mins > 0L -> "${hours}h ${mins}m"
            hours > 0L -> "${hours}h"
            else -> "${mins}m"
        }
    }
}

// =============================================================================
// 5. CORE PERSISTENT ENTITIES (v3 Schema)
// =============================================================================

data class HabitVersion(
    val versionId: String,
    val habitId: String,
    val effectiveFromLocalDate: String, // YYYY-MM-DD
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
    val reminderMode: String = "OFF", // "OFF" or "AT_TIME"
    val reminderTimeLocal: String? = null, // "HH:mm"
    val tinyDescription: String = "Deliberately small action",
    val usualDescription: String = "The ordinary intended action",
    val extraDescription: String = "Optional stretch action",
    val colorHex: String = "#286143",
    val sortOrder: Int = 0,
    val createdAtUtcMs: Long = 0L
)

data class Habit(
    val habitId: String,
    val createdAtUtcMs: Long = 0L,
    val updatedAtUtcMs: Long = 0L,
    val category: String? = null,
    val lifetimeGoalDays: Int? = null,
    val currentVersion: HabitVersion? = null,
    // Convenience properties resolved from effective HabitVersion
    val name: String = currentVersion?.name ?: "",
    val description: String = currentVersion?.description ?: "",
    val trackingMode: TrackingMode = currentVersion?.trackingMode ?: TrackingMode.COMPLETION_ONLY,
    val unitCode: String? = currentVersion?.unitCode,
    val targetPeriod: TargetPeriod = currentVersion?.targetPeriod ?: TargetPeriod.DAY,
    val targetValue: Int? = currentVersion?.targetValue,
    val scheduleKind: ScheduleKind = currentVersion?.scheduleKind ?: ScheduleKind.DAILY,
    val scheduledWeekdaysMask: Int? = currentVersion?.scheduledWeekdaysMask,
    val preferredWeekdaysMask: Int? = currentVersion?.preferredWeekdaysMask,
    val lifecycleState: HabitLifecycleState = currentVersion?.lifecycleState ?: HabitLifecycleState.ACTIVE,
    val reminderMode: String = currentVersion?.reminderMode ?: "OFF",
    val reminderTimeLocal: String? = currentVersion?.reminderTimeLocal,
    val tinyDescription: String = currentVersion?.tinyDescription ?: "Deliberately small action",
    val usualDescription: String = currentVersion?.usualDescription ?: "The ordinary intended action",
    val extraDescription: String = currentVersion?.extraDescription ?: "Optional stretch action",
    val scheduleDays: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val colorHex: String = currentVersion?.colorHex ?: "#286143",
    val sortOrder: Int = currentVersion?.sortOrder ?: 0
) {
    val id: String get() = habitId
    val isArchived: Boolean get() = lifecycleState == HabitLifecycleState.ARCHIVED

    fun isScheduledFor(dayOfWeek: DayOfWeek): Boolean = when (scheduleKind) {
        ScheduleKind.DAILY -> true
        ScheduleKind.SELECTED_WEEKDAYS -> {
            val mask = scheduledWeekdaysMask ?: WeekdayMask.toMask(scheduleDays)
            WeekdayMask.contains(mask, dayOfWeek)
        }
        ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> {
            val mask = preferredWeekdaysMask
            if (mask != null && mask > 0) WeekdayMask.contains(mask, dayOfWeek) else true
        }
    }

    fun isScheduledFor(date: LocalDate): Boolean = isScheduledFor(date.dayOfWeek)
}

data class HabitEntry(
    val entryId: String,
    val habitId: String,
    val localDate: String, // YYYY-MM-DD
    val valueUnits: Int,
    val entryNote: String? = null,
    val createdAtUtcMs: Long,
    val updatedAtUtcMs: Long,
    val idempotencyKey: String
)

data class HabitDayOutcome(
    val outcomeId: String,
    val habitId: String,
    val localDate: String, // YYYY-MM-DD
    val outcomeKind: String, // "COMPLETED" or "EFFORT_LOGGED"
    val effortLevel: String? = null, // "TINY", "USUAL", "EXTRA" when outcomeKind == "EFFORT_LOGGED"
    val idempotencyKey: String,
    val createdAtUtcMs: Long,
    val updatedAtUtcMs: Long
)

data class HabitDateNote(
    val noteId: String,
    val habitId: String,
    val localDate: String, // YYYY-MM-DD
    val body: String,
    val createdAtUtcMs: Long,
    val updatedAtUtcMs: Long
)

// =============================================================================
// 6. PHASE 1 SCHEDULING EXCEPTIONS, PAUSE RANGES, STREAKS & GROVE (D-217, D-218, U-1..U-13)
// =============================================================================

enum class HabitDateExceptionKind {
    REST_DAY
}

data class HabitDateException(
    val id: String,
    val habitId: String,
    val localDate: String, // YYYY-MM-DD
    val exceptionKind: HabitDateExceptionKind = HabitDateExceptionKind.REST_DAY,
    val note: String? = null,
    val createdAtUtcMs: Long,
    val updatedAtUtcMs: Long
)

/**
 * Derived in-memory pause range from habit_versions.lifecycle_state transitions (U-9 Option A).
 * No separate habit_pauses table exists.
 */
data class DerivedPauseRange(
    val habitId: String,
    val startLocalDate: LocalDate,
    val endLocalDateInclusive: LocalDate? // null means open-ended pause
) {
    fun contains(date: LocalDate): Boolean {
        if (date.isBefore(startLocalDate)) return false
        val end = endLocalDateInclusive ?: return true
        return !date.isAfter(end)
    }
}

fun derivePauseRanges(versions: List<HabitVersion>): List<DerivedPauseRange> {
    if (versions.isEmpty()) return emptyList()
    val sorted = versions.sortedWith(
        compareBy<HabitVersion> { it.effectiveFromLocalDate }.thenBy { it.createdAtUtcMs }
    )
    // Collapse same-date versions to the latest version for that effectiveFromLocalDate
    val deduplicated = sorted
        .groupBy { it.effectiveFromLocalDate }
        .values
        .map { it.last() }
        .sortedBy { it.effectiveFromLocalDate }

    val ranges = mutableListOf<DerivedPauseRange>()
    var currentPauseStart: LocalDate? = null
    val habitId = deduplicated.first().habitId

    for (v in deduplicated) {
        val effDate = try {
            LocalDateValidator.parseStrict(v.effectiveFromLocalDate)
        } catch (_: Exception) {
            continue
        }
        if (v.lifecycleState == HabitLifecycleState.PAUSED) {
            if (currentPauseStart == null) {
                currentPauseStart = effDate
            }
        } else {
            if (currentPauseStart != null) {
                val endInclusive = effDate.minusDays(1)
                if (!endInclusive.isBefore(currentPauseStart)) {
                    ranges.add(
                        DerivedPauseRange(
                            habitId = habitId,
                            startLocalDate = currentPauseStart,
                            endLocalDateInclusive = endInclusive
                        )
                    )
                }
                currentPauseStart = null
            }
        }
    }

    if (currentPauseStart != null) {
        ranges.add(
            DerivedPauseRange(
                habitId = habitId,
                startLocalDate = currentPauseStart,
                endLocalDateInclusive = null
            )
        )
    }
    return ranges
}

enum class StreakUnit {
    DAYS,
    WEEKS
}

enum class GroveTerminalReason {
    MISSED,
    PAUSED,
    ARCHIVED,
    COMPLETED
}

enum class GroveTreeState {
    SAPLING_ACTIVE,
    GROWING_TREE,
    DORMANT_TREE,
    UNQUALIFIED_HISTORY
}

data class HabitStreakRun(
    val runId: String,
    val habitId: String,
    val startLocalDate: String, // YYYY-MM-DD
    val endLocalDate: String, // YYYY-MM-DD
    val completedStreakCount: Int,
    val elapsedActiveSpanCount: Int,
    val streakUnit: StreakUnit,
    val terminalReason: GroveTerminalReason? = null,
    val isActive: Boolean,
    val qualifiedForGrove: Boolean,
    val qualifiedAtUtcMs: Long? = null,
    val brokenOnLocalDate: String? = null,
    val treeState: GroveTreeState,
    val createdAtUtcMs: Long,
    val updatedAtUtcMs: Long
)

data class WeekStartPolicyEntry(
    val effectiveFromLocalDate: String, // YYYY-MM-DD
    val weekStartIsoDay: Int, // 1 (Monday) .. 7 (Sunday)
    val isLegacyBaseline: Boolean = false,
    val createdAtUtcMs: Long
) {
    val dayOfWeek: DayOfWeek get() = DayOfWeek.of(weekStartIsoDay)
}

enum class HabitConfigEffectivePolicy {
    APPLY_TODAY,
    APPLY_TOMORROW,
    DEFER_TO_NEXT_WEEK_BOUNDARY
}

class HabitConfigurationLockedTodayException(
    val habitId: String,
    val localDate: String,
    message: String = "Today's habit configuration is locked because activity has already been recorded on $localDate (D-194). Use APPLY_TOMORROW instead."
) : IllegalStateException(message)

data class HabitSummary(
    val habit: Habit,
    val effectiveVersion: HabitVersion,
    val todayState: DayRecordState,
    val todayProgressBand: ProgressBand,
    val todayCompactBucket: CompactHistoryBucket,
    val todayTotalUnits: BigInteger,
    val todayPercentage: Int,
    val currentCompletedStreak: Int,
    val currentElapsedActiveSpan: Int,
    val streakUnit: StreakUnit,
    val activeRun: HabitStreakRun?,
    val groveRuns: List<HabitStreakRun>,
    val allStreakRuns: List<HabitStreakRun>,
    val lifetimeActiveDays: Int,
    val lifetimeGoalDays: Int?,
    val isScheduledToday: Boolean,
    val hasRestDayToday: Boolean,
    val canTakeRestDayToday: Boolean
)

data class HabitDeletionPayload(
    val habit: Habit,
    val versions: List<HabitVersion>,
    val entries: List<HabitEntry>,
    val outcomes: List<HabitDayOutcome>,
    val notes: List<HabitDateNote>,
    val exceptions: List<HabitDateException>,
    val streakRuns: List<HabitStreakRun>,
    val unlinkedJournalEntryIds: List<String>,
    val unlinkedJournalDraftIds: List<String>
)

// =============================================================================
// 7. JOURNAL, SETTINGS, RESTORE & UNDO MODELS
// =============================================================================

data class JournalEntry(
    val journalEntryId: String,
    val localDate: String, // YYYY-MM-DD
    val body: String,
    val title: String? = null,
    val primaryMoodCode: String? = null,
    val secondaryMoodCode: String? = null,
    val promptId: String? = null,
    val linkedHabitId: String? = null,
    val createdAtUtcMs: Long = 0L,
    val updatedAtUtcMs: Long = 0L
) {
    val id: String get() = journalEntryId
    val content: String get() = body
    val date: LocalDate
        get() = try {
            LocalDateValidator.parseStrict(localDate)
        } catch (_: Exception) {
            LocalDate.now()
        }
    val prompt: String get() = DailyPrompts.promptTextForId(promptId)
}

data class JournalDraft(
    val draftId: String,
    val localDate: String, // YYYY-MM-DD
    val title: String? = null,
    val body: String,
    val primaryMoodCode: String? = null,
    val secondaryMoodCode: String? = null,
    val promptId: String? = null,
    val linkedHabitId: String? = null,
    val intendedJournalEntryId: String? = null,
    val sourceContext: String = "JOURNAL_COMPOSER", // JOURNAL_COMPOSER, HABIT_REFLECTION, COMEBACK_30D
    val createdAtUtcMs: Long = 0L,
    val updatedAtUtcMs: Long = 0L
)

data class AppSetting(
    val settingKey: String,
    val valueType: String, // STRING, INT, BOOL, JSON
    val valueText: String,
    val schemaVersion: Int = 1,
    val updatedAtUtcMs: Long = 0L
)

data class NotificationCancelOutboxItem(
    val cancelId: String,
    val platformNotificationTag: String,
    val platformNotificationId: Int,
    val createdAtUtcMs: Long,
    val lastAttemptAtUtcMs: Long? = null,
    val attemptCount: Int = 0,
    val status: String = "PENDING", // PENDING, COMPLETED, FAILED
    val errorCategory: String? = null
)

enum class ConflictResolution {
    KEEP_CURRENT,
    USE_BACKUP,
    KEEP_BOTH
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
    fun allowedResolutions(): Set<ConflictResolution> = when (recordType) {
        "journal_entry" -> setOf(
            ConflictResolution.KEEP_CURRENT,
            ConflictResolution.USE_BACKUP,
            ConflictResolution.KEEP_BOTH
        )
        else -> setOf(
            ConflictResolution.KEEP_CURRENT,
            ConflictResolution.USE_BACKUP
        )
    }
}

data class RestoreSession(
    val restoreSessionId: String,
    val restoreMode: String, // REPLACE, MERGE
    val formatVersion: Int,
    val sourceSchemaVersion: Int,
    val sessionState: String,
    val startedAtUtcMs: Long,
    val updatedAtUtcMs: Long,
    val manifestJson: String,
    val safetyBackupRef: String? = null,
    val safetyBackupVerified: Int = 0
)

enum class UndoEntityType {
    HABIT_ENTRY,
    HABIT_DAY_OUTCOME,
    HABIT_DATE_NOTE,
    REST_DAY_EXCEPTION,
    HABIT_DELETION
}

data class UndoToken(
    val tokenId: String,
    val entityType: UndoEntityType,
    val entityId: String,
    val expectedUpdatedAtUtcMs: Long,
    val previousPayloadJson: String? = null,
    val createdAtUtcMs: Long,
    val expiresAtUtcMs: Long = createdAtUtcMs + 8_000L // 8-second undo window (D-209)
)

// =============================================================================
// 8. COMEBACK, GROWTH METRICS, MOODS & PROMPTS
// =============================================================================

enum class ComebackTier {
    DAYS_14,
    DAYS_30
}

data class ComebackInfo(
    val daysAway: Int,
    val tier: ComebackTier = if (daysAway >= 30) ComebackTier.DAYS_30 else ComebackTier.DAYS_14,
    val episodeKey: String = "",
    val title: String,
    val message: String,
    val primaryActionLabel: String = "Resume & Adjust Habits",
    val secondaryActionLabel: String = "Keep Current Setup"
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
    val currentPeriodOpportunities: Int = 0,
    val currentMonthDurationMinutes: Long = 0L,
    val currentMonthDateSessionCounts: Map<LocalDate, Int> = emptyMap(),
    val activeSaplingsCount: Int = 0,
    val growingTreesCount: Int = 0,
    val dormantTreesCount: Int = 0
)

data class MoodOption(
    val code: String,
    val label: String,
    val symbol: String
)

object MoodCatalog {
    val starterMoods: List<MoodOption> = listOf(
        MoodOption("CALM", "Calm", "🌿"),
        MoodOption("GRATEFUL", "Grateful", "🌼"),
        MoodOption("HOPEFUL", "Hopeful", "🌱"),
        MoodOption("FOCUSED", "Focused", "🎯"),
        MoodOption("ENERGISED", "Energised", "☀️"),
        MoodOption("CONTENT", "Content", "🍵"),
        MoodOption("TIRED", "Tired", "🌙"),
        MoodOption("STRESSED", "Stressed", "🌧️"),
        MoodOption("ANXIOUS", "Anxious", "🍃"),
        MoodOption("LOW", "Low", "🪨")
    )

    val allMoods: List<MoodOption> = starterMoods + listOf(
        MoodOption("GROUNDED", "Grounded", "🌳"),
        MoodOption("ENERGIZED", "Energized", "☀️"),
        MoodOption("OVERWHELMED", "Overwhelmed", "🌊"),
        MoodOption("RESTLESS", "Restless", "🌬️"),
        MoodOption("PEACEFUL", "Peaceful", "🕊️"),
        MoodOption("PROUD", "Proud", "✨")
    )

    val options: List<MoodOption> = allMoods

    fun findByCode(code: String?): MoodOption? {
        if (code.isNullOrBlank()) return null
        return allMoods.find { it.code.equals(code, ignoreCase = true) }
    }
}

data class MoodDistributionItem(
    val moodCode: String,
    val label: String,
    val symbol: String,
    val count: Int,
    val percentage: Int,
    val previousPeriodCount: Int
)

data class MoodInsightsResult(
    val periodLabel: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalEntriesWithPrimaryMood: Int,
    val previousPeriodTotalEntries: Int,
    val distribution: List<MoodDistributionItem>,
    val matchingEntries: List<JournalEntry>
)

object DailyPrompts {
    val prompts: List<Pair<String, String>> = listOf(
        "prompt_small_action" to "What small action felt surprisingly manageable today?",
        "prompt_resistance" to "Where did you notice resistance today, and how can you make tomorrow's step smaller?",
        "prompt_energy" to "What helped your energy today, even a little?",
        "prompt_tiny_version" to "If tomorrow gets busy, what is the 'Tiny' version of your most important habit?",
        "prompt_future_self" to "What is one thing you did today that your future self will thank you for?",
        "prompt_natural_habit" to "Which habit feels most natural right now, and why?",
        "prompt_enough_today" to "Take a breath: what does 'enough for today' look like right now?",
        "prompt_retrospective" to "What got in the way this week, and what would make this habit gentler next time?",
        "prompt_comeback" to "Welcome back — what feels like a kind, sustainable starting point today?"
    )

    fun promptForDate(date: LocalDate): Pair<String, String> {
        val dailyPool = prompts.take(7)
        val index = Math.floorMod(date.toEpochDay().toInt(), dailyPool.size)
        return dailyPool[index]
    }

    fun promptTextForId(promptId: String?): String {
        if (promptId.isNullOrBlank()) return ""
        return prompts.find { it.first == promptId }?.second ?: promptId
    }
}

object WeekBoundaryHelper {
    fun weekBounds(date: LocalDate, weekStart: DayOfWeek): Pair<LocalDate, LocalDate> {
        val diff = Math.floorMod(date.dayOfWeek.value - weekStart.value, 7)
        val start = date.minusDays(diff.toLong())
        val end = start.plusDays(6)
        return start to end
    }

    fun previousWeekBounds(date: LocalDate, weekStart: DayOfWeek): Pair<LocalDate, LocalDate> {
        val (currentStart, _) = weekBounds(date, weekStart)
        val prevStart = currentStart.minusDays(7)
        val prevEnd = currentStart.minusDays(1)
        return prevStart to prevEnd
    }

    /**
     * Resolves the effective week-start DayOfWeek for targetDate using week_start_history (U-10 Option A).
     * Also returns whether targetDate falls inside a transition gap between an ending 7-day period
     * under the previous week-start policy and the next effective_from_local_date.
     */
    fun resolveWeekStartPolicyForDate(
        date: LocalDate,
        history: List<WeekStartPolicyEntry>,
        fallbackDay: DayOfWeek = DayOfWeek.MONDAY
    ): DayOfWeek {
        if (history.isEmpty()) return fallbackDay
        val dateStr = date.toString()
        val sorted = history.sortedBy { it.effectiveFromLocalDate }
        val active = sorted.lastOrNull { it.effectiveFromLocalDate <= dateStr } ?: sorted.first()
        return active.dayOfWeek
    }

    /**
     * Computes the effective date for a new week-start preference change (U-10 Option A):
     * 1. Finishes the current 7-day period under the currently effective week-start day.
     * 2. Finds the first date strictly after currentWeekEnd whose ISO day matches newWeekStart.
     */
    fun computeNextWeekStartEffectiveDate(
        today: LocalDate,
        currentWeekStart: DayOfWeek,
        newWeekStart: DayOfWeek
    ): Pair<LocalDate, LocalDateRange> {
        val (_, currentWeekEnd) = weekBounds(today, currentWeekStart)
        var candidate = currentWeekEnd.plusDays(1)
        val transitionStart = candidate
        while (candidate.dayOfWeek != newWeekStart) {
            candidate = candidate.plusDays(1)
        }
        val transitionEnd = candidate.minusDays(1)
        return candidate to LocalDateRange(transitionStart, transitionEnd)
    }

    /**
     * Returns true if `date` lies inside a neutral transition gap immediately preceding a
     * non-baseline `WeekStartPolicyEntry` (U-10 Option A).
     */
    fun isNeutralTransitionDate(
        date: LocalDate,
        history: List<WeekStartPolicyEntry>
    ): Boolean {
        if (history.size <= 1) return false
        val sorted = history.sortedBy { it.effectiveFromLocalDate }
        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val curr = sorted[i]
            if (curr.isLegacyBaseline) continue
            val currEff = try { LocalDateValidator.parseStrict(curr.effectiveFromLocalDate) } catch (_: Exception) { continue }
            // Under U-10, any transition gap before currEff is at most 6 days and starts after the last full 7-day period of prev.
            val prevEff = try { LocalDateValidator.parseStrict(prev.effectiveFromLocalDate) } catch (_: Exception) { continue }
            if (!date.isBefore(currEff) || date.isBefore(prevEff)) continue
            val daysFromPrevStart = java.time.temporal.ChronoUnit.DAYS.between(prevEff, currEff)
            val remainder = Math.floorMod(daysFromPrevStart, 7)
            if (remainder > 0) {
                val gapStart = currEff.minusDays(remainder.toLong())
                if (!date.isBefore(gapStart) && date.isBefore(currEff)) {
                    return true
                }
            }
        }
        return false
    }
}

data class LocalDateRange(
    val startInclusive: LocalDate,
    val endInclusive: LocalDate
) {
    val isEmpty: Boolean get() = endInclusive.isBefore(startInclusive)
}
