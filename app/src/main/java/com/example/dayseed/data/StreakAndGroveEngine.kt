package com.example.dayseed.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.math.BigInteger
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Pure domain + SQLite persistence engine for Dayseed Phase 1 Streak & Grove evaluation.
 * Implements D-217, D-218, and D-219 (U-1 through U-13).
 */
object StreakAndGroveEngine {

    /**
     * Resolves the effective HabitVersion on [date]:
     * greatest `effective_from_local_date <= date`, tie-broken by latest `created_at_utc_ms`.
     */
    fun resolveEffectiveVersion(versions: List<HabitVersion>, date: LocalDate): HabitVersion? {
        if (versions.isEmpty()) return null
        val dateStr = date.toString()
        return versions
            .filter { it.effectiveFromLocalDate <= dateStr }
            .maxWithOrNull(
                compareBy<HabitVersion> { it.effectiveFromLocalDate }
                    .thenBy { it.createdAtUtcMs }
            )
    }

    /**
     * Returns true when [version] represents a weekly session-count habit (`TrackingMode.QUANTITY`,
     * `TargetPeriod.WEEK`, and `unitCode == "sessions"`, case-insensitive).
     */
    fun isWeeklySessionCountHabit(version: HabitVersion): Boolean {
        return isWeeklySessionCountHabit(
            trackingMode = version.trackingMode,
            targetPeriod = version.targetPeriod,
            unitCode = version.unitCode
        )
    }

    /**
     * Returns true when the given tracking parameters represent a weekly session-count habit
     * (`TrackingMode.QUANTITY`, `TargetPeriod.WEEK`, and `unitCode == "sessions"`, case-insensitive).
     */
    fun isWeeklySessionCountHabit(
        trackingMode: TrackingMode,
        targetPeriod: TargetPeriod,
        unitCode: String?
    ): Boolean {
        return trackingMode.canonical() == TrackingMode.QUANTITY &&
            targetPeriod == TargetPeriod.WEEK &&
            unitCode?.trim()?.equals("sessions", ignoreCase = true) == true
    }

    /**
     * Resolves the frozen start-of-week [HabitVersion] under U-12 Option B.
     * Uses `maxOf(firstHabitDate, weekStart)` as the freeze anchor date, falling back to [fallbackDate]
     * or [fallbackVersion] if needed.
     */
    fun resolveFrozenWeekVersion(
        sortedVersions: List<HabitVersion>,
        weekStart: LocalDate,
        firstHabitDate: LocalDate,
        fallbackDate: LocalDate,
        fallbackVersion: HabitVersion? = null
    ): HabitVersion? {
        val anchorDate = if (firstHabitDate.isAfter(weekStart)) firstHabitDate else weekStart
        return resolveEffectiveVersion(sortedVersions, anchorDate)
            ?: resolveEffectiveVersion(sortedVersions, fallbackDate)
            ?: fallbackVersion
    }

    /**
     * Counts qualifying weekly progress for a single day's entries under [frozenWeekVersion]:
     * - If `isWeeklySessionCountHabit(frozenWeekVersion)` is true, counts each entry with `valueUnits > 0`
     *   as `1` session (excluding explicit zero `valueUnits == 0` entries from the completed count).
     * - Otherwise sums positive `valueUnits` via [ProgressBandClassifier.sumUnitsExact].
     */
    fun countDailyUnitsOrSessionsForWeek(
        frozenWeekVersion: HabitVersion,
        dayEntries: List<HabitEntry>
    ): BigInteger {
        return if (isWeeklySessionCountHabit(frozenWeekVersion)) {
            BigInteger.valueOf(dayEntries.count { it.valueUnits > 0 }.toLong())
        } else {
            ProgressBandClassifier.sumUnitsExact(dayEntries.map { it.valueUnits })
        }
    }

    /**
     * Computes distinct local dates with real qualifying activity for U-6 Option A (`lifetime_goal_days`).
     * Counts positive numeric entries (`sum(valueUnits) > 0`), `COMPLETED` outcomes, and `EFFORT_LOGGED`
     * outcomes (`TINY`, `USUAL`, `EXTRA`).
     * Excludes explicit-zero-only dates, date notes, `REST_DAY` exceptions, and pause dates without activity.
     */
    fun computeLifetimeActiveDays(
        versions: List<HabitVersion>,
        entries: List<HabitEntry>,
        outcomes: List<HabitDayOutcome>
    ): Int {
        val activeDates = mutableSetOf<String>()

        val entriesByDate = entries.groupBy { it.localDate }
        for ((dateStr, dayEntries) in entriesByDate) {
            val total = ProgressBandClassifier.sumUnitsExact(dayEntries.map { it.valueUnits })
            if (total > BigInteger.ZERO) {
                activeDates.add(dateStr)
            }
        }

        for (outcome in outcomes) {
            val isValidOutcome = when (outcome.outcomeKind) {
                "COMPLETED" -> true
                "EFFORT_LOGGED" -> outcome.effortLevel in setOf("TINY", "USUAL", "EXTRA")
                else -> false
            }
            if (isValidOutcome) {
                activeDates.add(outcome.localDate)
            }
        }

        return activeDates.size
    }

    /**
     * Evaluates the DayRecordState and ProgressBand for a single date.
     */
    fun evaluateDayRecordState(
        version: HabitVersion?,
        date: LocalDate,
        dayEntries: List<HabitEntry>,
        dayOutcome: HabitDayOutcome?,
        dayException: HabitDateException?
    ): Pair<DayRecordState, ProgressBand> {
        if (version == null) {
            return DayRecordState.NO_RECORD to ProgressBand.ZERO
        }
        if (version.lifecycleState == HabitLifecycleState.PAUSED) {
            return DayRecordState.PAUSED_LIFECYCLE to ProgressBand.ZERO
        }

        return when (version.trackingMode.canonical()) {
            TrackingMode.QUANTITY, TrackingMode.DURATION -> {
                if (dayEntries.isNotEmpty()) {
                    val total = countDailyUnitsOrSessionsForWeek(version, dayEntries)
                    if (total == BigInteger.ZERO) {
                        DayRecordState.EXPLICIT_ZERO to ProgressBand.ZERO
                    } else {
                        val target = BigInteger.valueOf((version.targetValue ?: 1).coerceAtLeast(1).toLong())
                        val band = ProgressBandClassifier.classify(total, target)
                        val state = when (band) {
                            ProgressBand.GOAL_MET, ProgressBand.EXTRA, ProgressBand.HUGE ->
                                DayRecordState.GOAL_MET_OR_EXCEEDED
                            ProgressBand.ZERO ->
                                DayRecordState.EXPLICIT_ZERO
                            else ->
                                DayRecordState.PARTIAL_PROGRESS
                        }
                        state to band
                    }
                } else if (dayException?.exceptionKind == HabitDateExceptionKind.REST_DAY) {
                    DayRecordState.REST_DAY_EXCEPTION to ProgressBand.ZERO
                } else {
                    DayRecordState.NO_RECORD to ProgressBand.ZERO
                }
            }

            TrackingMode.COMPLETION_ONLY -> {
                if (dayOutcome != null && dayOutcome.outcomeKind == "COMPLETED") {
                    DayRecordState.GOAL_MET_OR_EXCEEDED to ProgressBand.GOAL_MET
                } else if (dayException?.exceptionKind == HabitDateExceptionKind.REST_DAY) {
                    DayRecordState.REST_DAY_EXCEPTION to ProgressBand.ZERO
                } else {
                    DayRecordState.NO_RECORD to ProgressBand.ZERO
                }
            }

            TrackingMode.EFFORT_AWARE, TrackingMode.EFFORT_LEVELS -> {
                if (dayOutcome != null && (dayOutcome.outcomeKind == "EFFORT_LOGGED" || dayOutcome.outcomeKind == "COMPLETED")) {
                    when (dayOutcome.effortLevel) {
                        "TINY" -> DayRecordState.PARTIAL_PROGRESS to ProgressBand.TINY_STEP
                        "EXTRA" -> DayRecordState.GOAL_MET_OR_EXCEEDED to ProgressBand.EXTRA
                        else -> DayRecordState.GOAL_MET_OR_EXCEEDED to ProgressBand.GOAL_MET
                    }
                } else if (dayException?.exceptionKind == HabitDateExceptionKind.REST_DAY) {
                    DayRecordState.REST_DAY_EXCEPTION to ProgressBand.ZERO
                } else {
                    DayRecordState.NO_RECORD to ProgressBand.ZERO
                }
            }
        }
    }

    /**
     * Returns true if [date] is an eligible scheduled opportunity under [version].
     */
    fun isScheduledOpportunityOnDate(version: HabitVersion, date: LocalDate): Boolean {
        return when (version.scheduleKind) {
            ScheduleKind.DAILY -> true
            ScheduleKind.SELECTED_WEEKDAYS -> {
                val mask = version.scheduledWeekdaysMask ?: WeekdayMask.ALL_DAYS_MASK
                WeekdayMask.contains(mask, date.dayOfWeek)
            }
            ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> {
                val mask = version.preferredWeekdaysMask
                if (mask != null && mask > 0) WeekdayMask.contains(mask, date.dayOfWeek) else true
            }
        }
    }

    /**
     * Evaluates the full history of a habit up to [evaluationDate] and returns all
     * resulting [HabitStreakRun] rows (both closed historical runs and the current active run, if any).
     */
    fun evaluateHabitStreakRuns(
        habitId: String,
        versions: List<HabitVersion>,
        entries: List<HabitEntry>,
        outcomes: List<HabitDayOutcome>,
        exceptions: List<HabitDateException>,
        weekStartHistory: List<WeekStartPolicyEntry>,
        evaluationDate: LocalDate,
        nowUtcMs: Long,
        existingRuns: List<HabitStreakRun> = emptyList()
    ): List<HabitStreakRun> {
        if (versions.isEmpty()) return emptyList()
        val sortedVersions = versions.sortedWith(
            compareBy<HabitVersion> { it.effectiveFromLocalDate }.thenBy { it.createdAtUtcMs }
        )
        val firstDate = try {
            LocalDateValidator.parseStrict(sortedVersions.first().effectiveFromLocalDate)
        } catch (_: Exception) {
            return emptyList()
        }
        if (firstDate.isAfter(evaluationDate)) return emptyList()

        // U-11 Option A: target_period determines daily vs weekly cadence
        val latestEffective = resolveEffectiveVersion(sortedVersions, evaluationDate) ?: sortedVersions.last()
        return if (latestEffective.targetPeriod == TargetPeriod.WEEK) {
            evaluateWeeklyCadenceRuns(
                habitId = habitId,
                versions = sortedVersions,
                entries = entries,
                outcomes = outcomes,
                exceptions = exceptions,
                weekStartHistory = weekStartHistory,
                firstDate = firstDate,
                evaluationDate = evaluationDate,
                nowUtcMs = nowUtcMs,
                existingRuns = existingRuns
            )
        } else {
            evaluateDailyCadenceRuns(
                habitId = habitId,
                versions = sortedVersions,
                entries = entries,
                outcomes = outcomes,
                exceptions = exceptions,
                firstDate = firstDate,
                evaluationDate = evaluationDate,
                nowUtcMs = nowUtcMs,
                existingRuns = existingRuns
            )
        }
    }

    private data class MutableRun(
        val runId: String,
        val habitId: String,
        var startLocalDate: LocalDate,
        var endLocalDate: LocalDate,
        var completedStreakCount: Int,
        var activeDatesCount: Int,
        val streakUnit: StreakUnit,
        var terminalReason: GroveTerminalReason? = null,
        var isActive: Boolean = true,
        var qualifiedForGrove: Boolean = false,
        var qualifiedAtUtcMs: Long? = null,
        val priorQualifiedAtUtcMs: Long? = null,
        var brokenOnLocalDate: LocalDate? = null,
        val createdAtUtcMs: Long,
        var updatedAtUtcMs: Long
    ) {
        fun updateQualification(nowUtcMs: Long) {
            val activeSpanDays = activeDatesCount
            if (activeSpanDays >= 7) {
                if (!qualifiedForGrove) {
                    qualifiedForGrove = true
                }
                if (qualifiedAtUtcMs == null) {
                    qualifiedAtUtcMs = priorQualifiedAtUtcMs ?: nowUtcMs
                }
            } else {
                qualifiedForGrove = false
                qualifiedAtUtcMs = null
            }
        }

        fun toImmutable(streakUnit: StreakUnit): HabitStreakRun {
            val elapsedSpan = (ChronoUnit.DAYS.between(startLocalDate, endLocalDate).toInt() + 1)
                .coerceAtLeast(completedStreakCount)
                .coerceAtLeast(1)
            val treeState = when {
                isActive && qualifiedForGrove -> GroveTreeState.GROWING_TREE
                isActive && !qualifiedForGrove -> GroveTreeState.SAPLING_ACTIVE
                !isActive && qualifiedForGrove -> GroveTreeState.DORMANT_TREE
                else -> GroveTreeState.UNQUALIFIED_HISTORY
            }
            val effectiveQualifiedAt = if (qualifiedForGrove) {
                qualifiedAtUtcMs ?: priorQualifiedAtUtcMs ?: updatedAtUtcMs
            } else {
                null
            }
            return HabitStreakRun(
                runId = runId,
                habitId = habitId,
                startLocalDate = startLocalDate.toString(),
                endLocalDate = endLocalDate.toString(),
                completedStreakCount = completedStreakCount,
                elapsedActiveSpanCount = elapsedSpan,
                streakUnit = streakUnit,
                terminalReason = terminalReason,
                isActive = isActive,
                qualifiedForGrove = qualifiedForGrove,
                qualifiedAtUtcMs = effectiveQualifiedAt,
                brokenOnLocalDate = brokenOnLocalDate?.toString(),
                treeState = treeState,
                createdAtUtcMs = createdAtUtcMs,
                updatedAtUtcMs = maxOf(createdAtUtcMs, updatedAtUtcMs)
            )
        }
    }

    private fun evaluateDailyCadenceRuns(
        habitId: String,
        versions: List<HabitVersion>,
        entries: List<HabitEntry>,
        outcomes: List<HabitDayOutcome>,
        exceptions: List<HabitDateException>,
        firstDate: LocalDate,
        evaluationDate: LocalDate,
        nowUtcMs: Long,
        existingRuns: List<HabitStreakRun>
    ): List<HabitStreakRun> {
        val entriesByDate = entries.groupBy { it.localDate }
        val outcomesByDate = outcomes.associateBy { it.localDate }
        val exceptionsByDate = exceptions.associateBy { it.localDate }
        val existingByStart = existingRuns.associateBy { it.startLocalDate }

        val closedRuns = mutableListOf<MutableRun>()
        var openRun: MutableRun? = null

        var cursor = firstDate
        while (!cursor.isAfter(evaluationDate)) {
            val dateStr = cursor.toString()
            val eff = resolveEffectiveVersion(versions, cursor)
            if (eff == null) {
                cursor = cursor.plusDays(1)
                continue
            }

            // Lifecycle closures (PAUSED, ARCHIVED, COMPLETED) close any open run immediately without brokenOnLocalDate (U-5, U-13)
            if (eff.lifecycleState != HabitLifecycleState.ACTIVE) {
                val active = openRun
                if (active != null) {
                    active.isActive = false
                    active.terminalReason = when (eff.lifecycleState) {
                        HabitLifecycleState.PAUSED -> GroveTerminalReason.PAUSED
                        HabitLifecycleState.ARCHIVED -> GroveTerminalReason.ARCHIVED
                        HabitLifecycleState.COMPLETED -> GroveTerminalReason.COMPLETED
                        else -> GroveTerminalReason.PAUSED
                    }
                    active.brokenOnLocalDate = null
                    active.updatedAtUtcMs = nowUtcMs
                    closedRuns.add(active)
                    openRun = null
                }
                cursor = cursor.plusDays(1)
                continue
            }

            val isScheduled = isScheduledOpportunityOnDate(eff, cursor)
            val dayEntries = entriesByDate[dateStr].orEmpty()
            val dayOutcome = outcomesByDate[dateStr]
            val dayException = exceptionsByDate[dateStr]
            val (dayState, _) = evaluateDayRecordState(eff, cursor, dayEntries, dayOutcome, dayException)

            when (dayState) {
                DayRecordState.GOAL_MET_OR_EXCEEDED -> {
                    // Scheduled OR unscheduled bonus full completion (U-2 Option A): starts or extends run
                    val current = openRun
                    if (current == null) {
                        val existing = existingByStart[dateStr]
                        val newRun = MutableRun(
                            runId = existing?.runId ?: UUID.randomUUID().toString(),
                            habitId = habitId,
                            startLocalDate = cursor,
                            endLocalDate = cursor,
                            completedStreakCount = 1,
                            activeDatesCount = 1,
                            streakUnit = StreakUnit.DAYS,
                            createdAtUtcMs = existing?.createdAtUtcMs ?: nowUtcMs,
                            updatedAtUtcMs = maxOf(existing?.createdAtUtcMs ?: nowUtcMs, nowUtcMs),
                            priorQualifiedAtUtcMs = existing?.qualifiedAtUtcMs
                        )
                        newRun.updateQualification(nowUtcMs)
                        openRun = newRun
                    } else {
                        val gapDays = ChronoUnit.DAYS.between(current.endLocalDate, cursor).toInt()
                        current.endLocalDate = cursor
                        current.completedStreakCount += 1
                        current.activeDatesCount += gapDays.coerceAtLeast(1)
                        current.updatedAtUtcMs = nowUtcMs
                        current.updateQualification(nowUtcMs)
                    }
                }

                DayRecordState.PARTIAL_PROGRESS -> {
                    // Scheduled partial OR unscheduled partial (U-7 Option A, U-8 Option A for TINY):
                    // Holds an existing run and advances endLocalDate, without incrementing completedStreakCount or starting a new run.
                    val current = openRun
                    if (current != null) {
                        val gapDays = ChronoUnit.DAYS.between(current.endLocalDate, cursor).toInt()
                        current.endLocalDate = cursor
                        current.activeDatesCount += gapDays.coerceAtLeast(1)
                        current.updatedAtUtcMs = nowUtcMs
                        current.updateQualification(nowUtcMs)
                    }
                }

                DayRecordState.REST_DAY_EXCEPTION -> {
                    // D-218: Holds an existing run across a scheduled date
                    val current = openRun
                    if (current != null) {
                        val gapDays = ChronoUnit.DAYS.between(current.endLocalDate, cursor).toInt()
                        current.endLocalDate = cursor
                        current.activeDatesCount += gapDays.coerceAtLeast(1)
                        current.updatedAtUtcMs = nowUtcMs
                        current.updateQualification(nowUtcMs)
                    }
                }

                DayRecordState.EXPLICIT_ZERO, DayRecordState.NO_RECORD -> {
                    if (isScheduled) {
                        if (cursor.isBefore(evaluationDate)) {
                            // Past scheduled day with no qualifying or holding activity breaks the run
                            val current = openRun
                            if (current != null) {
                                current.isActive = false
                                current.terminalReason = GroveTerminalReason.MISSED
                                current.brokenOnLocalDate = cursor
                                current.updatedAtUtcMs = nowUtcMs
                                closedRuns.add(current)
                                openRun = null
                            }
                        } else {
                            // cursor == evaluationDate (today): today is still open until day ends!
                            // However, if explicit zero was logged today, do not break today while today is still open,
                            // or wait: does explicit zero on a past day break, while on today it stays open or breaks?
                            // Let's check D-188 & Revision 7 §8.2: a scheduled day is only missed once the day has passed
                            // without qualifying/holding activity, EXCEPT if openRun exists, today's unlogged state leaves openRun active!
                        }
                    } else {
                        // Unscheduled day with no activity: neutral, preserves openRun; if openRun exists,
                        // does an unscheduled day advance elapsedActiveSpanCount when subsequent activity occurs?
                        // Yes, gapDays in the next activity or active span through evaluationDate handles it.
                    }
                }

                DayRecordState.PAUSED_LIFECYCLE -> {}
            }

            cursor = cursor.plusDays(1)
        }

        val allRuns = mutableListOf<HabitStreakRun>()
        closedRuns.forEach { allRuns.add(it.toImmutable(StreakUnit.DAYS)) }
        openRun?.let { allRuns.add(it.toImmutable(StreakUnit.DAYS)) }
        return allRuns
    }

    private fun evaluateWeeklyCadenceRuns(
        habitId: String,
        versions: List<HabitVersion>,
        entries: List<HabitEntry>,
        outcomes: List<HabitDayOutcome>,
        exceptions: List<HabitDateException>,
        weekStartHistory: List<WeekStartPolicyEntry>,
        firstDate: LocalDate,
        evaluationDate: LocalDate,
        nowUtcMs: Long,
        existingRuns: List<HabitStreakRun>
    ): List<HabitStreakRun> {
        val entriesByDate = entries.groupBy { it.localDate }
        val outcomesByDate = outcomes.associateBy { it.localDate }
        val exceptionsByDate = exceptions.associateBy { it.localDate }
        val existingByStart = existingRuns.associateBy { it.startLocalDate }

        val closedRuns = mutableListOf<MutableRun>()
        var openRun: MutableRun? = null

        // Build chronological weekly periods (and neutral transition intervals under U-10) covering firstDate..evaluationDate
        val firstWeekStartDay = WeekBoundaryHelper.resolveWeekStartPolicyForDate(firstDate, weekStartHistory)
        val (initialWeekStart, _) = WeekBoundaryHelper.weekBounds(firstDate, firstWeekStartDay)

        var periodCursor = initialWeekStart
        while (!periodCursor.isAfter(evaluationDate)) {
            if (WeekBoundaryHelper.isNeutralTransitionDate(periodCursor, weekStartHistory)) {
                // Advance through neutral transition days (U-10 Option A)
                var transEnd = periodCursor
                while (!transEnd.plusDays(1).isAfter(evaluationDate) &&
                    WeekBoundaryHelper.isNeutralTransitionDate(transEnd.plusDays(1), weekStartHistory)
                ) {
                    transEnd = transEnd.plusDays(1)
                }
                val current = openRun
                if (current != null) {
                    val gapDays = ChronoUnit.DAYS.between(current.endLocalDate, transEnd).toInt()
                    if (gapDays > 0) {
                        current.endLocalDate = transEnd
                        current.activeDatesCount += gapDays
                        current.updateQualification(nowUtcMs)
                    }
                }
                periodCursor = transEnd.plusDays(1)
                continue
            }

            val weekStartDay = WeekBoundaryHelper.resolveWeekStartPolicyForDate(periodCursor, weekStartHistory)
            val (weekStart, rawWeekEnd) = WeekBoundaryHelper.weekBounds(periodCursor, weekStartDay)
            // Clip weekEnd if a new week_start_history entry starts inside rawWeekEnd
            val weekDates = (0..6).map { weekStart.plusDays(it.toLong()) }
            val weekEnd = weekDates.last()

            // U-12 Option B: freeze the HabitVersion at the start of the week
            val frozenVersion = resolveFrozenWeekVersion(
                sortedVersions = versions,
                weekStart = weekStart,
                firstHabitDate = firstDate,
                fallbackDate = weekEnd
            )

            if (frozenVersion == null) {
                periodCursor = weekEnd.plusDays(1)
                continue
            }

            // Walk day-by-day within the week to detect lifecycle transitions (PAUSED, ARCHIVED, COMPLETED) per U-5 Option A
            // A week may be split into active sub-segments separated by PAUSED/ARCHIVED/COMPLETED dates.
            var activeSegmentDates = mutableListOf<LocalDate>()
            var hadLifecycleClosureBeforeSegment = false

            fun evaluateWeeklySegment(segmentDates: List<LocalDate>, wasInterruptedByClosureAfter: Boolean, wasPrecededByClosure: Boolean) {
                if (segmentDates.isEmpty()) return
                val segStart = segmentDates.first()
                val segEnd = segmentDates.last()
                // Use frozen start-of-week HabitVersion for target, mode, unit, and schedule semantics under U-12 Option B
                val segVersion = frozenVersion
                val targetVal = (segVersion.targetValue ?: 1).coerceAtLeast(1)
                val targetBi = BigInteger.valueOf(targetVal.toLong())

                var totalUnits = BigInteger.ZERO
                val completedCheckOffDates = mutableSetOf<String>()
                var lastActivityDate: LocalDate? = null
                for (d in segmentDates) {
                    val dStr = d.toString()
                    when (segVersion.trackingMode.canonical()) {
                        TrackingMode.QUANTITY, TrackingMode.DURATION -> {
                            val dEntries = entriesByDate[dStr].orEmpty()
                            val daySum = countDailyUnitsOrSessionsForWeek(segVersion, dEntries)
                            if (daySum > BigInteger.ZERO) {
                                totalUnits = totalUnits.add(daySum)
                                lastActivityDate = d
                            }
                        }
                        TrackingMode.COMPLETION_ONLY -> {
                            val hasCompletedOnDay = outcomes.any { it.localDate == dStr && it.outcomeKind == "COMPLETED" }
                            if (hasCompletedOnDay && completedCheckOffDates.add(dStr)) {
                                totalUnits = BigInteger.valueOf(completedCheckOffDates.size.toLong())
                                lastActivityDate = d
                            }
                        }
                        TrackingMode.EFFORT_AWARE, TrackingMode.EFFORT_LEVELS -> {
                            val out = outcomesByDate[dStr]
                            if (out != null && (out.outcomeKind == "COMPLETED" ||
                                    (out.outcomeKind == "EFFORT_LOGGED" && out.effortLevel in setOf("USUAL", "EXTRA")))
                            ) {
                                if (completedCheckOffDates.add(dStr)) {
                                    totalUnits = BigInteger.valueOf(completedCheckOffDates.size.toLong())
                                }
                                lastActivityDate = d
                            } else if (out != null && out.outcomeKind == "EFFORT_LOGGED" && out.effortLevel == "TINY") {
                                lastActivityDate = d
                            }
                        }
                    }
                }

                val reachedTarget = totalUnits >= targetBi
                val isPartialCreationWeek = firstDate.isAfter(weekStart) && segStart == firstDate
                val isPartialPauseWeek = wasInterruptedByClosureAfter || wasPrecededByClosure
                val isCurrentInProgressWeek = !weekEnd.isBefore(evaluationDate) && !wasInterruptedByClosureAfter

                // U-4 Option A: Check if all remaining eligible scheduled opportunities after lastActivityDate are covered by REST_DAY
                val eligibleDatesAfterActivity = segmentDates.filter { d ->
                    isScheduledOpportunityOnDate(segVersion, d) &&
                        (lastActivityDate == null || d.isAfter(lastActivityDate))
                }
                val allRemainingExcepted = eligibleDatesAfterActivity.isNotEmpty() &&
                    eligibleDatesAfterActivity.all { d ->
                        exceptionsByDate[d.toString()]?.exceptionKind == HabitDateExceptionKind.REST_DAY
                    }

                when {
                    reachedTarget -> {
                        val current = openRun
                        if (current == null) {
                            val startKey = segStart.toString()
                            val existing = existingByStart[startKey]
                            val newRun = MutableRun(
                                runId = existing?.runId ?: UUID.randomUUID().toString(),
                                habitId = habitId,
                                startLocalDate = segStart,
                                endLocalDate = segEnd.coerceAtMost(evaluationDate),
                                completedStreakCount = 1,
                                activeDatesCount = 7, // 1 completed weekly period = 7 active calendar days for Grove
                                streakUnit = StreakUnit.WEEKS,
                                createdAtUtcMs = existing?.createdAtUtcMs ?: nowUtcMs,
                                updatedAtUtcMs = maxOf(existing?.createdAtUtcMs ?: nowUtcMs, nowUtcMs),
                                priorQualifiedAtUtcMs = existing?.qualifiedAtUtcMs
                            )
                            newRun.updateQualification(nowUtcMs)
                            openRun = newRun
                        } else {
                            current.endLocalDate = segEnd.coerceAtMost(evaluationDate)
                            current.completedStreakCount += 1
                            current.activeDatesCount += 7
                            current.updatedAtUtcMs = nowUtcMs
                            current.updateQualification(nowUtcMs)
                        }
                    }

                    isPartialCreationWeek || isPartialPauseWeek || allRemainingExcepted || (totalUnits > BigInteger.ZERO && !isCurrentInProgressWeek) -> {
                        // Held week (U-3 mid-week creation, U-4 remaining opportunities excepted, U-5 partial pause week, or partial weekly progress):
                        // Note: for weekly habits, does ordinary partial progress without U-3/U-4/U-5 hold or miss?
                        // Wait: let's check Revision 7 §8.2 & U-3/U-4/U-5!
                        // Under U-3, U-4, U-5:
                        // - U-3 holds an incomplete creation week.
                        // - U-4 holds a week when remaining eligible opportunities are covered by REST_DAY / PAUSED.
                        // - U-5 holds incomplete pre-pause and post-resume partial weeks.
                        // What if a full normal week (not creation week, not pause week, not U-4 excepted, and already past evaluationDate) is below target?
                        // Then a full normal week below target is MISSED!
                        val isHeldByRule = isPartialCreationWeek || isPartialPauseWeek || allRemainingExcepted || totalUnits > BigInteger.ZERO
                        if (isHeldByRule) {
                            val current = openRun
                            if (current != null) {
                                current.endLocalDate = segEnd.coerceAtMost(evaluationDate)
                                current.activeDatesCount += 7
                                current.updatedAtUtcMs = nowUtcMs
                                current.updateQualification(nowUtcMs)
                            }
                        } else if (!isCurrentInProgressWeek) {
                            // Past full week below target without U-3/U-4/U-5 exception -> MISSED
                            val current = openRun
                            if (current != null) {
                                current.isActive = false
                                current.terminalReason = GroveTerminalReason.MISSED
                                current.brokenOnLocalDate = segEnd
                                current.updatedAtUtcMs = nowUtcMs
                                closedRuns.add(current)
                                openRun = null
                            }
                        }
                    }

                    !isCurrentInProgressWeek -> {
                        // Past full week with 0 progress and not held -> MISSED
                        val current = openRun
                        if (current != null) {
                            current.isActive = false
                            current.terminalReason = GroveTerminalReason.MISSED
                            current.brokenOnLocalDate = segEnd
                            current.updatedAtUtcMs = nowUtcMs
                            closedRuns.add(current)
                            openRun = null
                        }
                    }
                }
            }

            for (d in weekDates) {
                if (d.isBefore(firstDate) || d.isAfter(evaluationDate)) continue
                val eff = resolveEffectiveVersion(versions, d) ?: continue
                if (eff.lifecycleState != HabitLifecycleState.ACTIVE) {
                    // First evaluate any pre-pause segment in this week (U-5 Option A: hold incomplete pre-pause portion)
                    if (activeSegmentDates.isNotEmpty()) {
                        evaluateWeeklySegment(
                            segmentDates = activeSegmentDates.toList(),
                            wasInterruptedByClosureAfter = true,
                            wasPrecededByClosure = hadLifecycleClosureBeforeSegment
                        )
                        activeSegmentDates.clear()
                    }
                    // Close any open run immediately on the pause/archive/complete date (U-5 Option A)
                    val current = openRun
                    if (current != null) {
                        current.isActive = false
                        current.terminalReason = when (eff.lifecycleState) {
                            HabitLifecycleState.PAUSED -> GroveTerminalReason.PAUSED
                            HabitLifecycleState.ARCHIVED -> GroveTerminalReason.ARCHIVED
                            HabitLifecycleState.COMPLETED -> GroveTerminalReason.COMPLETED
                            else -> GroveTerminalReason.PAUSED
                        }
                        current.brokenOnLocalDate = null
                        current.updatedAtUtcMs = nowUtcMs
                        closedRuns.add(current)
                        openRun = null
                    }
                    hadLifecycleClosureBeforeSegment = true
                } else {
                    activeSegmentDates.add(d)
                }
            }

            if (activeSegmentDates.isNotEmpty()) {
                evaluateWeeklySegment(
                    segmentDates = activeSegmentDates.toList(),
                    wasInterruptedByClosureAfter = false,
                    wasPrecededByClosure = hadLifecycleClosureBeforeSegment
                )
            }

            periodCursor = weekEnd.plusDays(1)
        }

        val allRuns = mutableListOf<HabitStreakRun>()
        closedRuns.forEach { allRuns.add(it.toImmutable(StreakUnit.WEEKS)) }
        openRun?.let { allRuns.add(it.toImmutable(StreakUnit.WEEKS)) }
        return allRuns
    }

    /**
     * Recomputes and persists `habit_streak_runs` for a single habit inside an open SQLite database,
     * or across all habits during v2 -> v3 migration backfill.
     */
    fun syncHabitStreakRunsInDb(
        db: SQLiteDatabase,
        habitId: String,
        versions: List<HabitVersion>,
        entries: List<HabitEntry>,
        outcomes: List<HabitDayOutcome>,
        exceptions: List<HabitDateException>,
        weekStartHistory: List<WeekStartPolicyEntry>,
        evaluationDate: LocalDate,
        nowUtcMs: Long,
        existingRuns: List<HabitStreakRun> = emptyList()
    ): List<HabitStreakRun> {
        val computed = evaluateHabitStreakRuns(
            habitId = habitId,
            versions = versions,
            entries = entries,
            outcomes = outcomes,
            exceptions = exceptions,
            weekStartHistory = weekStartHistory,
            evaluationDate = evaluationDate,
            nowUtcMs = nowUtcMs,
            existingRuns = existingRuns
        )
        db.delete(DayseedDatabaseHelper.TABLE_HABIT_STREAK_RUNS, "habit_id = ?", arrayOf(habitId))
        for (run in computed) {
            val cv = ContentValues().apply {
                put("run_id", run.runId)
                put("habit_id", run.habitId)
                put("start_local_date", run.startLocalDate)
                put("end_local_date", run.endLocalDate)
                put("completed_streak_count", run.completedStreakCount)
                put("elapsed_active_span_count", run.elapsedActiveSpanCount)
                put("streak_unit", run.streakUnit.name)
                if (run.terminalReason != null) put("terminal_reason", run.terminalReason.name) else putNull("terminal_reason")
                put("is_active", if (run.isActive) 1 else 0)
                put("qualified_for_grove", if (run.qualifiedForGrove) 1 else 0)
                if (run.qualifiedAtUtcMs != null) put("qualified_at_utc_ms", run.qualifiedAtUtcMs) else putNull("qualified_at_utc_ms")
                if (run.brokenOnLocalDate != null) put("broken_on_local_date", run.brokenOnLocalDate) else putNull("broken_on_local_date")
                put("tree_state", run.treeState.name)
                put("created_at_utc_ms", run.createdAtUtcMs)
                put("updated_at_utc_ms", run.updatedAtUtcMs)
            }
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_STREAK_RUNS, null, cv)
        }
        return computed
    }

    data class WeeklyProgressSnapshot(
        val weekTotalUnits: BigInteger,
        val weekTargetUnits: BigInteger,
        val weekProgressBand: ProgressBand,
        val weekPercentage: Int,
        val weekStartLocalDate: String,
        val weekEndLocalDate: String,
        val frozenWeekVersion: HabitVersion
    )

    /**
     * Computes week-to-date progress on [evaluationDate] under U-12 Option B (frozen start-of-week HabitVersion).
     */
    fun computeWeeklyProgressSnapshot(
        versions: List<HabitVersion>,
        entries: List<HabitEntry>,
        outcomes: List<HabitDayOutcome>,
        weekStartHistory: List<WeekStartPolicyEntry>,
        evaluationDate: LocalDate,
        fallbackVersion: HabitVersion
    ): WeeklyProgressSnapshot {
        val sortedVersions = versions.sortedWith(
            compareBy<HabitVersion> { it.effectiveFromLocalDate }.thenBy { it.createdAtUtcMs }
        )
        val firstDate = sortedVersions.firstOrNull()?.let {
            try {
                LocalDateValidator.parseStrict(it.effectiveFromLocalDate)
            } catch (_: Exception) {
                evaluationDate
            }
        } ?: evaluationDate

        val weekStartDay = WeekBoundaryHelper.resolveWeekStartPolicyForDate(evaluationDate, weekStartHistory)
        val (weekStart, weekEnd) = WeekBoundaryHelper.weekBounds(evaluationDate, weekStartDay)
        val frozenVersion = resolveFrozenWeekVersion(
            sortedVersions = sortedVersions,
            weekStart = weekStart,
            firstHabitDate = firstDate,
            fallbackDate = evaluationDate,
            fallbackVersion = fallbackVersion
        ) ?: fallbackVersion

        val targetInt = (frozenVersion.targetValue ?: 1).coerceAtLeast(1)
        val targetBi = BigInteger.valueOf(targetInt.toLong())
        val weekDates = (0..6).map { weekStart.plusDays(it.toLong()) }
            .filter { !it.isBefore(firstDate) }

        val entriesByDate = entries.groupBy { it.localDate }
        var totalUnits = BigInteger.ZERO
        val completedDates = mutableSetOf<String>()

        for (d in weekDates) {
            val dStr = d.toString()
            when (frozenVersion.trackingMode.canonical()) {
                TrackingMode.QUANTITY, TrackingMode.DURATION -> {
                    val dayEntries = entriesByDate[dStr].orEmpty()
                    val daySum = countDailyUnitsOrSessionsForWeek(frozenVersion, dayEntries)
                    if (daySum > BigInteger.ZERO) {
                        totalUnits = totalUnits.add(daySum)
                    }
                }
                TrackingMode.COMPLETION_ONLY -> {
                    val hasCompleted = outcomes.any { it.localDate == dStr && it.outcomeKind == "COMPLETED" }
                    if (hasCompleted && completedDates.add(dStr)) {
                        totalUnits = BigInteger.valueOf(completedDates.size.toLong())
                    }
                }
                TrackingMode.EFFORT_AWARE, TrackingMode.EFFORT_LEVELS -> {
                    val hasQualifyingOutcome = outcomes.any { out ->
                        out.localDate == dStr && (
                            out.outcomeKind == "COMPLETED" ||
                                (out.outcomeKind == "EFFORT_LOGGED" && out.effortLevel in setOf("USUAL", "EXTRA"))
                            )
                    }
                    if (hasQualifyingOutcome && completedDates.add(dStr)) {
                        totalUnits = BigInteger.valueOf(completedDates.size.toLong())
                    }
                }
            }
        }

        val band = ProgressBandClassifier.classify(totalUnits, targetBi)
        val pct = ProgressBandClassifier.percentageFloor(totalUnits, targetBi)
        return WeeklyProgressSnapshot(
            weekTotalUnits = totalUnits,
            weekTargetUnits = targetBi,
            weekProgressBand = band,
            weekPercentage = pct,
            weekStartLocalDate = weekStart.toString(),
            weekEndLocalDate = weekEnd.toString(),
            frozenWeekVersion = frozenVersion
        )
    }
}
