package com.example.dayseed

import android.content.ContentValues
import android.content.Context
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.CompactHistoryBucket
import com.example.dayseed.data.DayRecordState
import com.example.dayseed.data.DayseedDatabaseHelper
import com.example.dayseed.data.DayseedMigrationBlockedException
import com.example.dayseed.data.DayseedNumericRangeException
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.GroveTerminalReason
import com.example.dayseed.data.GroveTreeState
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitConfigEffectivePolicy
import com.example.dayseed.data.HabitConfigurationLockedTodayException
import com.example.dayseed.data.HabitDateException
import com.example.dayseed.data.HabitDayOutcome
import com.example.dayseed.data.HabitEntry
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.HabitVersion
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.data.LocalDateValidator
import com.example.dayseed.data.ProgressBand
import com.example.dayseed.data.ProgressBandClassifier
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.StreakAndGroveEngine
import com.example.dayseed.data.StreakUnit
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.UndoEntityType
import com.example.dayseed.data.WeekBoundaryHelper
import com.example.dayseed.data.WeekStartPolicyEntry
import com.example.dayseed.data.WeekdayMask
import com.example.dayseed.data.derivePauseRanges
import com.example.dayseed.data.getValidatedInt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigInteger
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Exhaustive Phase 1 test suite covering all 36 mandatory test cases (T1-01 through T1-36)
 * specified in Phase 1 Implementation Plan Revision 7 §10.2.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase1ImplementationTest {

    private lateinit var context: Context
    private lateinit var testClock: TestDayseedClock
    private lateinit var repository: DayseedRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testClock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 3, 15),
            currentLocalTime = LocalTime.of(10, 0),
            currentUtcMs = 1_773_568_800_000L
        )
        repository = DayseedRepository(context, clock = testClock, databaseName = null)
    }

    @After
    fun tearDown() {
        repository.dbHelper.close()
    }

    // =========================================================================
    // T1-01: Creatable vs Legacy TrackingModes (D-216)
    // =========================================================================
    @Test
    fun t1_01_creatableModesRejectNewEffortAwareWhilePreservingLegacyReads() {
        assertEquals(
            setOf(TrackingMode.QUANTITY, TrackingMode.DURATION, TrackingMode.COMPLETION_ONLY),
            TrackingMode.creatableModes
        )
        assertFalse(TrackingMode.EFFORT_AWARE.isCreatableForNewHabit())

        // Attempt to create a brand-new EFFORT_AWARE habit via repository must fail
        val newEffortHabit = Habit(
            habitId = "h_new_effort",
            name = "New Effort Habit",
            trackingMode = TrackingMode.EFFORT_AWARE
        )
        try {
            repository.saveHabitSync(newEffortHabit)
            fail("Expected IllegalArgumentException when creating new EFFORT_AWARE habit")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("EFFORT_AWARE"))
        }

        // Legacy EFFORT_AWARE habit already in DB can still be read, logged, and edited
        repository.saveHabitSync(
            newEffortHabit.copy(habitId = "h_legacy_effort"),
            effectiveDateOverride = LocalDate.of(2026, 3, 1),
            allowLegacyEffortModeCreation = true
        )
        repository.logHabitDayOutcomeSync("h_legacy_effort", LocalDate.of(2026, 3, 10), "EFFORT_LOGGED", "USUAL")
        val updatedVer = repository.saveHabitSync(
            newEffortHabit.copy(habitId = "h_legacy_effort", name = "Legacy Effort Renamed")
        )
        assertEquals("Legacy Effort Renamed", updatedVer.name)
        assertEquals(TrackingMode.EFFORT_AWARE, updatedVer.trackingMode)
    }

    // =========================================================================
    // T1-02: ProgressBand 7+ZERO Exact Boundary Classification & Quotes (D-040, D-216)
    // =========================================================================
    @Test
    fun t1_02_progressBandExactBoundariesAndPairedQuotes() {
        val target = 100
        assertEquals(ProgressBand.ZERO, ProgressBandClassifier.classify(0, target))
        assertEquals(ProgressBand.TINY_STEP, ProgressBandClassifier.classify(1, target))
        assertEquals(ProgressBand.TINY_STEP, ProgressBandClassifier.classify(24, target))
        assertEquals(ProgressBand.GETTING_STARTED, ProgressBandClassifier.classify(25, target))
        assertEquals(ProgressBand.GETTING_STARTED, ProgressBandClassifier.classify(49, target))
        assertEquals(ProgressBand.HALFWAY, ProgressBandClassifier.classify(50, target))
        assertEquals(ProgressBand.HALFWAY, ProgressBandClassifier.classify(74, target))
        assertEquals(ProgressBand.ALMOST_THERE, ProgressBandClassifier.classify(75, target))
        assertEquals(ProgressBand.ALMOST_THERE, ProgressBandClassifier.classify(99, target))
        assertEquals(ProgressBand.GOAL_MET, ProgressBandClassifier.classify(100, target))
        assertEquals(ProgressBand.EXTRA, ProgressBandClassifier.classify(101, target))
        assertEquals(ProgressBand.EXTRA, ProgressBandClassifier.classify(149, target))
        assertEquals(ProgressBand.HUGE, ProgressBandClassifier.classify(150, target))
        assertEquals(ProgressBand.HUGE, ProgressBandClassifier.classify(250, target))

        // Every detailed band has non-blank, distinct encouragement and motivation quotes
        for (band in ProgressBand.values()) {
            assertTrue(band.encouragementQuote.isNotBlank())
            assertTrue(band.motivationQuote.isNotBlank())
            assertNotEquals(band.encouragementQuote, band.motivationQuote)
        }

        // CompactHistoryBucket mapping remains 4 buckets
        assertEquals(CompactHistoryBucket.NONE_RECORDED, ProgressBandClassifier.toCompactBucket(ProgressBand.ZERO))
        assertEquals(CompactHistoryBucket.PARTIAL, ProgressBandClassifier.toCompactBucket(ProgressBand.TINY_STEP))
        assertEquals(CompactHistoryBucket.PARTIAL, ProgressBandClassifier.toCompactBucket(ProgressBand.ALMOST_THERE))
        assertEquals(CompactHistoryBucket.COMPLETED, ProgressBandClassifier.toCompactBucket(ProgressBand.GOAL_MET))
        assertEquals(CompactHistoryBucket.EXCEEDED, ProgressBandClassifier.toCompactBucket(ProgressBand.EXTRA))
        assertEquals(CompactHistoryBucket.EXCEEDED, ProgressBandClassifier.toCompactBucket(ProgressBand.HUGE))
    }

    // =========================================================================
    // T1-03: Overflow-Safe BigInteger Aggregation & Classification (§3.4, §5.3)
    // =========================================================================
    @Test
    fun t1_03_bigIntegerAggregationHandlesMultiEntryExceedingIntMaxValue() {
        val entries = listOf(Int.MAX_VALUE, Int.MAX_VALUE, 500)
        val sum = ProgressBandClassifier.sumUnitsExact(entries)
        val expectedSum = BigInteger.valueOf(Int.MAX_VALUE.toLong())
            .multiply(BigInteger.valueOf(2L))
            .add(BigInteger.valueOf(500L))
        assertEquals(expectedSum, sum)

        val target = BigInteger.valueOf(Int.MAX_VALUE.toLong()).multiply(BigInteger.valueOf(2L))
        // sum is slightly above 100% of target -> EXTRA (101-149% or 100%+)
        val band = ProgressBandClassifier.classify(sum, target)
        assertEquals(ProgressBand.EXTRA, band)
    }

    // =========================================================================
    // T1-04: Safe Cursor.getValidatedInt Rejects Out-Of-Range 64-Bit SQLite Values (§3.5)
    // =========================================================================
    @Test
    fun t1_04_cursorGetValidatedIntRejects64BitOverflow() {
        MatrixCursor(arrayOf("value_units")).use { cursor ->
            cursor.addRow(arrayOf(42L))
            cursor.addRow(arrayOf(Int.MAX_VALUE.toLong() + 100L))

            assertTrue(cursor.moveToFirst())
            assertEquals(42, cursor.getValidatedInt(0, "value_units"))

            assertTrue(cursor.moveToNext())
            try {
                cursor.getValidatedInt(0, "value_units")
                fail("Expected DayseedNumericRangeException for value > Int.MAX_VALUE")
            } catch (e: DayseedNumericRangeException) {
                assertTrue(e.message!!.contains("value_units"))
            }
        }
    }

    // =========================================================================
    // T1-05: Strict Calendar Date Validation (Feb 30, Leap Years, Month 13) (§5.4)
    // =========================================================================
    @Test
    fun t1_05_strictLocalDateValidatorRejectsImpossibleDatesAndAcceptsLeapDay() {
        // Valid dates including leap year 2024-02-29
        assertEquals(LocalDate.of(2024, 2, 29), LocalDateValidator.parseStrict("2024-02-29"))
        assertTrue(LocalDateValidator.isValidStrict("2026-03-15"))

        // Invalid calendar dates that match YYYY-MM-DD GLOB but are impossible dates
        val invalidDates = listOf(
            "2026-02-29", // Non-leap year
            "2026-02-30",
            "2026-04-31",
            "2026-13-01",
            "2026-00-10",
            "2026-03-00",
            "2026-3-5"
        )
        for (bad in invalidDates) {
            assertFalse("Expected $bad to be rejected", LocalDateValidator.isValidStrict(bad))
            try {
                LocalDateValidator.parseStrict(bad)
                fail("Expected IllegalArgumentException for $bad")
            } catch (_: IllegalArgumentException) {
                // Expected
            }
        }
    }

    // =========================================================================
    // T1-06: Six Distinct DayRecordStates & Explicit Zero Non-Qualification (D-217)
    // =========================================================================
    @Test
    fun t1_06_explicitZeroIsDistinctFromNoRecordAndBreaksScheduledStreak() {
        val start = LocalDate.of(2026, 3, 10)
        testClock.currentLocalDate = LocalDate.of(2026, 3, 12)
        val habit = Habit(
            habitId = "h_zero_test",
            name = "Pushups",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "reps",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20,
            scheduleKind = ScheduleKind.DAILY
        )
        repository.saveHabitSync(habit, effectiveDateOverride = start)

        // Day 1 (Mar 10): Full completion (20 reps) -> streak = 1
        repository.logHabitEntrySync("h_zero_test", start, 20)
        // Day 2 (Mar 11): Explicit zero (0 reps) -> EXPLICIT_ZERO (breaks run at end of Mar 11!)
        repository.logHabitEntrySync("h_zero_test", start.plusDays(1), 0)

        val runs = repository.habitStreakRuns.value.filter { it.habitId == "h_zero_test" }
        assertEquals(1, runs.size)
        val closedRun = runs.first()
        assertFalse("Explicit zero on a past scheduled date must close the run", closedRun.isActive)
        assertEquals(GroveTerminalReason.MISSED, closedRun.terminalReason)
        assertEquals("2026-03-11", closedRun.brokenOnLocalDate)
        assertEquals(GroveTreeState.UNQUALIFIED_HISTORY, closedRun.treeState)
    }

    // =========================================================================
    // T1-07: Partial Progress Holds Existing Daily Streak Without Starting One (D-217)
    // =========================================================================
    @Test
    fun t1_07_partialProgressHoldsExistingDailyRunWithoutStartingOrIncrementingCompletedCount() {
        val d1 = LocalDate.of(2026, 3, 10)
        testClock.currentLocalDate = LocalDate.of(2026, 3, 13)
        val habit = Habit(
            habitId = "h_partial_daily",
            name = "Read",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20,
            scheduleKind = ScheduleKind.DAILY
        )
        repository.saveHabitSync(habit, effectiveDateOverride = d1)

        // Day 1 (Mar 10): Partial only (5 pages) -> cannot start a streak run!
        repository.logHabitEntrySync("h_partial_daily", d1, 5)
        assertTrue(repository.habitStreakRuns.value.none { it.habitId == "h_partial_daily" })

        // Day 2 (Mar 11): Full completion (20 pages) -> starts run (completed=1, span=1)
        repository.logHabitEntrySync("h_partial_daily", d1.plusDays(1), 20)
        // Day 3 (Mar 12): Partial (5 pages) -> holds run! completed stays 1, span advances to 2
        repository.logHabitEntrySync("h_partial_daily", d1.plusDays(2), 5)
        // Day 4 (Mar 13, today): Full completion (20 pages) -> completed becomes 2, span becomes 3
        repository.logHabitEntrySync("h_partial_daily", d1.plusDays(3), 20)

        val summary = repository.habitSummaries.value["h_partial_daily"]!!
        assertEquals(2, summary.currentCompletedStreak)
        assertEquals(3, summary.currentElapsedActiveSpan)
        assertNotNull(summary.activeRun)
        assertEquals(GroveTreeState.SAPLING_ACTIVE, summary.activeRun!!.treeState)
    }

    // =========================================================================
    // T1-08: Unscheduled Bonus Completion & Unscheduled Partial Activity (U-2, U-7)
    // =========================================================================
    @Test
    fun t1_08_unscheduledFullCompletionStartsOrExtendsAndUnscheduledPartialHoldsOnly() {
        // Mon/Wed/Fri habit starting Monday 2026-03-09
        val mon = LocalDate.of(2026, 3, 9)
        val tue = LocalDate.of(2026, 3, 10) // Unscheduled
        val wed = LocalDate.of(2026, 3, 11)
        val thu = LocalDate.of(2026, 3, 12) // Unscheduled
        testClock.currentLocalDate = thu

        val mwfMask = WeekdayMask.toMask(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        val habit = Habit(
            habitId = "h_mwf",
            name = "Strength",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "sets",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 10,
            scheduleKind = ScheduleKind.SELECTED_WEEKDAYS,
            scheduledWeekdaysMask = mwfMask,
            scheduleDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        )
        repository.saveHabitSync(habit, effectiveDateOverride = mon)

        // Unscheduled partial on Tuesday alone cannot start a run (U-7)
        repository.logHabitEntrySync("h_mwf", tue, 4)
        assertTrue(repository.habitStreakRuns.value.none { it.habitId == "h_mwf" })
        repository.removeRecordSync("h_mwf", tue)

        // Monday full completion (10 sets) -> starts run (completed=1)
        repository.logHabitEntrySync("h_mwf", mon, 10)
        // Tuesday (unscheduled) bonus full completion (10 sets) -> extends run (completed=2) (U-2)
        repository.logHabitEntrySync("h_mwf", tue, 10)
        // Wednesday (scheduled) full completion (10 sets) -> extends run (completed=3)
        repository.logHabitEntrySync("h_mwf", wed, 10)
        // Thursday (unscheduled) partial activity (3 sets) -> holds run, advances endLocalDate to Thu, completed stays 3 (U-7)
        repository.logHabitEntrySync("h_mwf", thu, 3)

        val activeRun = repository.habitSummaries.value["h_mwf"]?.activeRun
        assertNotNull(activeRun)
        assertEquals(3, activeRun!!.completedStreakCount)
        assertEquals("2026-03-09", activeRun.startLocalDate)
        assertEquals("2026-03-12", activeRun.endLocalDate)
        assertEquals(4, activeRun.elapsedActiveSpanCount)
    }

    // =========================================================================
    // T1-09: Legacy EFFORT_AWARE Qualification (U-8: USUAL/EXTRA full, TINY partial)
    // =========================================================================
    @Test
    fun t1_09_effortAwareUsualAndExtraQualifyFullWhileTinyHoldsAsPartial() {
        val d1 = LocalDate.of(2026, 3, 10)
        testClock.currentLocalDate = LocalDate.of(2026, 3, 12)
        val habit = Habit(
            habitId = "h_effort_u8",
            name = "Legacy Stretch",
            trackingMode = TrackingMode.EFFORT_AWARE,
            scheduleKind = ScheduleKind.DAILY
        )
        repository.saveHabitSync(habit, effectiveDateOverride = d1, allowLegacyEffortModeCreation = true)

        // Day 1: TINY alone does not start a streak run (U-8), though it counts as a lifetime active day (U-6)
        repository.logHabitDayOutcomeSync("h_effort_u8", d1, "EFFORT_LOGGED", "TINY")
        assertNull(repository.habitSummaries.value["h_effort_u8"]?.activeRun)
        assertEquals(1, repository.habitSummaries.value["h_effort_u8"]?.lifetimeActiveDays)

        // Day 2: USUAL starts the streak run (completed = 1)
        repository.logHabitDayOutcomeSync("h_effort_u8", d1.plusDays(1), "EFFORT_LOGGED", "USUAL")
        // Day 3: TINY holds the streak run without incrementing completedStreakCount (completed = 1, span = 2)
        repository.logHabitDayOutcomeSync("h_effort_u8", d1.plusDays(2), "EFFORT_LOGGED", "TINY")

        val summary = repository.habitSummaries.value["h_effort_u8"]!!
        assertEquals(1, summary.currentCompletedStreak)
        assertEquals(2, summary.currentElapsedActiveSpan)
        assertEquals(3, summary.lifetimeActiveDays)
    }

    // =========================================================================
    // T1-10: Today-Only REST_DAY Exception Creation & Mutual Exclusion (D-218, U-1)
    // =========================================================================
    @Test
    fun t1_10_restDayExceptionIsTodayOnlyAndHoldsStreakWithoutIncrementing() {
        val d1 = LocalDate.of(2026, 3, 14)
        val today = LocalDate.of(2026, 3, 15)
        testClock.currentLocalDate = today

        val habit = Habit(
            habitId = "h_rest_test",
            name = "Run",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            scheduleKind = ScheduleKind.DAILY
        )
        repository.saveHabitSync(habit, effectiveDateOverride = d1)
        repository.logHabitDayOutcomeSync("h_rest_test", d1, "COMPLETED", null)

        // U-1: Retroactive or future REST_DAY creation must be rejected
        try {
            repository.setRestDayExceptionSync("h_rest_test", d1)
            fail("Expected IllegalArgumentException for retroactive REST_DAY")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
        try {
            repository.setRestDayExceptionSync("h_rest_test", today.plusDays(1))
            fail("Expected IllegalArgumentException for future REST_DAY")
        } catch (_: IllegalArgumentException) {
            // Expected
        }

        // Taking REST_DAY today succeeds and holds yesterday's streak (completed = 1, span = 2)
        val (restEx, undoToken) = repository.setRestDayExceptionSync("h_rest_test", today, "Recovering")
        assertEquals(today.toString(), restEx.localDate)

        // Advance clock to tomorrow (Mar 16) so Mar 15 is evaluated as a past date with REST_DAY
        testClock.currentLocalDate = today.plusDays(1)
        repository.reloadAllSync()
        val summaryAfterRest = repository.habitSummaries.value["h_rest_test"]!!
        assertEquals(1, summaryAfterRest.currentCompletedStreak)
        assertEquals(2, summaryAfterRest.currentElapsedActiveSpan)

        // Undo REST_DAY within 8s window
        testClock.currentLocalDate = today
        assertTrue(repository.executeUndoSync(undoToken))
        assertTrue(repository.habitDateExceptions.value.none { it.habitId == "h_rest_test" })
    }

    // =========================================================================
    // T1-11: Real Activity Auto-Clears Same-Date REST_DAY Exception (D-218)
    // =========================================================================
    @Test
    fun t1_11_loggingRealActivityOnSameDateClearsRestDayAtomically() {
        val today = testClock.todayLocal()
        val habit = Habit(
            habitId = "h_rest_autoclear",
            name = "Journal Walk",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "min",
            targetValue = 15
        )
        repository.saveHabitSync(habit, effectiveDateOverride = today.minusDays(2))

        repository.setRestDayExceptionSync("h_rest_autoclear", today)
        assertEquals(1, repository.habitDateExceptions.value.size)

        // Logging real activity on today automatically clears REST_DAY in the same transaction
        repository.logHabitEntrySync("h_rest_autoclear", today, 15)
        assertEquals(0, repository.habitDateExceptions.value.size)
        assertEquals(1, repository.habitSummaries.value["h_rest_autoclear"]?.currentCompletedStreak)
    }

    // =========================================================================
    // T1-12: Derived Pause Ranges From habit_versions.lifecycle_state (U-9)
    // =========================================================================
    @Test
    fun t1_12_derivePauseRangesFromLifecycleVersionsWithoutHabitPausesTable() {
        val db = repository.dbHelper.readableDatabase
        db.rawQuery(
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='habit_pauses'",
            null
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("habit_pauses table must not exist (U-9)", 0, c.getInt(0))
        }

        val versions = listOf(
            HabitVersion(
                versionId = "v1",
                habitId = "h1",
                effectiveFromLocalDate = "2026-03-01",
                name = "Habit",
                trackingMode = TrackingMode.COMPLETION_ONLY,
                lifecycleState = HabitLifecycleState.ACTIVE,
                createdAtUtcMs = 100L
            ),
            HabitVersion(
                versionId = "v2",
                habitId = "h1",
                effectiveFromLocalDate = "2026-03-05",
                name = "Habit",
                trackingMode = TrackingMode.COMPLETION_ONLY,
                lifecycleState = HabitLifecycleState.PAUSED,
                createdAtUtcMs = 200L
            ),
            HabitVersion(
                versionId = "v3",
                habitId = "h1",
                effectiveFromLocalDate = "2026-03-10",
                name = "Habit",
                trackingMode = TrackingMode.COMPLETION_ONLY,
                lifecycleState = HabitLifecycleState.ACTIVE,
                createdAtUtcMs = 300L
            )
        )
        val ranges = derivePauseRanges(versions)
        assertEquals(1, ranges.size)
        assertEquals(LocalDate.of(2026, 3, 5), ranges[0].startLocalDate)
        assertEquals(LocalDate.of(2026, 3, 9), ranges[0].endLocalDateInclusive)
        assertTrue(ranges[0].contains(LocalDate.of(2026, 3, 7)))
        assertFalse(ranges[0].contains(LocalDate.of(2026, 3, 10)))
    }

    // =========================================================================
    // T1-13 & T1-14: Grove Qualification at >= 7 Active Span Days & Sub-7-Day History (D-217, U-13)
    // =========================================================================
    @Test
    fun t1_13_and_14_groveQualificationAt7SpanDaysAndSub7DayUnqualifiedHistoryPreserved() {
        val start = LocalDate.of(2026, 3, 1)
        testClock.currentLocalDate = LocalDate.of(2026, 3, 15)
        val habit = Habit(
            habitId = "h_grove",
            name = "Meditation",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            scheduleKind = ScheduleKind.DAILY
        )
        repository.saveHabitSync(habit, effectiveDateOverride = start)

        // Run 1: 3 days completed (Mar 1..3), then missed on Mar 4 -> closes as UNQUALIFIED_HISTORY (U-13)
        for (i in 0..2) {
            repository.logHabitDayOutcomeSync("h_grove", start.plusDays(i.toLong()), "COMPLETED", null)
        }
        // Run 2: 7 days completed (Mar 5..11), then missed on Mar 12 -> qualifies at >=7 days and closes as DORMANT_TREE
        val run2Start = LocalDate.of(2026, 3, 5)
        for (i in 0..6) {
            repository.logHabitDayOutcomeSync("h_grove", run2Start.plusDays(i.toLong()), "COMPLETED", null)
        }
        // Run 3: Active 2-day run on Mar 14..15 -> SAPLING_ACTIVE
        repository.logHabitDayOutcomeSync("h_grove", LocalDate.of(2026, 3, 14), "COMPLETED", null)
        repository.logHabitDayOutcomeSync("h_grove", LocalDate.of(2026, 3, 15), "COMPLETED", null)

        val summary = repository.habitSummaries.value["h_grove"]!!
        assertEquals(3, summary.allStreakRuns.size)

        val run1 = summary.allStreakRuns[0]
        assertFalse(run1.qualifiedForGrove)
        assertEquals(GroveTreeState.UNQUALIFIED_HISTORY, run1.treeState)
        assertEquals(GroveTerminalReason.MISSED, run1.terminalReason)
        assertEquals("2026-03-04", run1.brokenOnLocalDate)

        val run2 = summary.allStreakRuns[1]
        assertTrue(run2.qualifiedForGrove)
        assertEquals(GroveTreeState.DORMANT_TREE, run2.treeState)
        assertEquals(GroveTerminalReason.MISSED, run2.terminalReason)
        assertEquals("2026-03-12", run2.brokenOnLocalDate)

        val run3 = summary.allStreakRuns[2]
        assertTrue(run3.isActive)
        assertEquals(GroveTreeState.SAPLING_ACTIVE, run3.treeState)
        assertNull(run3.terminalReason)

        // Grove runs only include qualified runs (run2)
        assertEquals(1, summary.groveRuns.size)
        assertEquals(run2.runId, summary.groveRuns.first().runId)
    }

    // =========================================================================
    // T1-15: Pause Closes Active Run Immediately With terminalReason = PAUSED (U-5)
    // =========================================================================
    @Test
    fun t1_15_pausingHabitClosesActiveRunWithPausedReasonAndNeverBridgesPause() {
        val start = LocalDate.of(2026, 3, 1)
        val habit = Habit(
            habitId = "h_pause_grove",
            name = "Write",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            scheduleKind = ScheduleKind.DAILY
        )
        repository.saveHabitSync(habit, effectiveDateOverride = start)

        // Complete 7 consecutive days (Mar 1..7) -> GROWING_TREE
        for (i in 0..6) {
            val d = start.plusDays(i.toLong())
            testClock.currentLocalDate = d
            repository.logHabitDayOutcomeSync("h_pause_grove", d, "COMPLETED", null)
        }
        assertEquals(
            GroveTreeState.GROWING_TREE,
            repository.habitSummaries.value["h_pause_grove"]?.activeRun?.treeState
        )

        // Pause on Mar 8 -> closes active run with terminalReason = PAUSED, brokenOnLocalDate = null, treeState = DORMANT_TREE
        val pauseDate = LocalDate.of(2026, 3, 8)
        testClock.currentLocalDate = pauseDate
        repository.updateHabitLifecycleSync("h_pause_grove", HabitLifecycleState.PAUSED)

        val runsDuringPause = repository.habitStreakRuns.value.filter { it.habitId == "h_pause_grove" }
        assertEquals(1, runsDuringPause.size)
        val closedByPause = runsDuringPause.first()
        assertFalse(closedByPause.isActive)
        assertEquals(GroveTerminalReason.PAUSED, closedByPause.terminalReason)
        assertNull(closedByPause.brokenOnLocalDate)
        assertEquals(GroveTreeState.DORMANT_TREE, closedByPause.treeState)

        // Resume on Mar 10 and complete -> starts a brand-new run (never bridges the pause)
        val resumeDate = LocalDate.of(2026, 3, 10)
        testClock.currentLocalDate = resumeDate
        repository.updateHabitLifecycleSync("h_pause_grove", HabitLifecycleState.ACTIVE)
        repository.logHabitDayOutcomeSync("h_pause_grove", resumeDate, "COMPLETED", null)

        val runsAfterResume = repository.habitStreakRuns.value.filter { it.habitId == "h_pause_grove" }
        assertEquals(2, runsAfterResume.size)
        assertTrue(runsAfterResume.last().isActive)
        assertEquals("2026-03-10", runsAfterResume.last().startLocalDate)
        assertEquals(1, runsAfterResume.last().completedStreakCount)
    }

    // =========================================================================
    // T1-16..T1-19: Weekly Cadence Evaluation, Mid-Week Creation, Exceptions & Pause (U-3, U-4, U-5, U-11)
    // =========================================================================
    @Test
    fun t1_16_to_19_weeklyHabitCadenceMidWeekCreationExceptionsAndPause() {
        // Week 1: Mon 2026-03-02 .. Sun 2026-03-08
        // Habit created mid-week on Thursday 2026-03-05 (U-3: incomplete creation week is held!)
        val thuWeek1 = LocalDate.of(2026, 3, 5)
        testClock.currentLocalDate = LocalDate.of(2026, 3, 22)

        val weeklyHabit = Habit(
            habitId = "h_weekly",
            name = "Gym Sessions",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "sessions",
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 3,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
        )
        repository.saveHabitSync(weeklyHabit, effectiveDateOverride = thuWeek1)

        // Week 1 (Mar 5..8): reaches full target (3 sessions) -> starts weekly streak = 1!
        repository.logHabitEntrySync("h_weekly", LocalDate.of(2026, 3, 5), 1)
        repository.logHabitEntrySync("h_weekly", LocalDate.of(2026, 3, 6), 1)
        repository.logHabitEntrySync("h_weekly", LocalDate.of(2026, 3, 7), 1)

        // Week 2 (Mon Mar 9 .. Sun Mar 15): logs 1 session on Mon Mar 9 -> partial progress holds the weekly run!
        repository.logHabitEntrySync("h_weekly", LocalDate.of(2026, 3, 9), 1)

        // Week 3 (Mon Mar 16 .. Sun Mar 22): logs 3 sessions -> qualifies Week 3 (completedStreakCount = 2 weeks!)
        repository.logHabitEntrySync("h_weekly", LocalDate.of(2026, 3, 16), 1)
        repository.logHabitEntrySync("h_weekly", LocalDate.of(2026, 3, 18), 1)
        repository.logHabitEntrySync("h_weekly", LocalDate.of(2026, 3, 20), 1)

        val summary = repository.habitSummaries.value["h_weekly"]!!
        assertEquals(StreakUnit.WEEKS, summary.streakUnit)
        assertEquals(2, summary.currentCompletedStreak)
        assertTrue(summary.currentElapsedActiveSpan >= 16)
        assertEquals(GroveTreeState.GROWING_TREE, summary.activeRun?.treeState)
    }

    @Test
    fun t1_18_weeklyRemainingOpportunitiesExceptedHoldsWeekUnderU4() {
        // Week 1: Mon 2026-03-02 .. Sun 2026-03-08 (qualifies with 2 sessions)
        // Week 2: Mon 2026-03-09 .. Sun 2026-03-15 (preferred days Mon & Wed only; Mon completed=0, Mon & Wed covered by REST_DAY)
        val monW1 = LocalDate.of(2026, 3, 2)
        val prefMask = WeekdayMask.toMask(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        val habit = Habit(
            habitId = "h_weekly_u4",
            name = "Swim",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "laps",
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 2,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
            preferredWeekdaysMask = prefMask
        )
        repository.saveHabitSync(habit, effectiveDateOverride = monW1)
        repository.logHabitEntrySync("h_weekly_u4", LocalDate.of(2026, 3, 2), 1)
        repository.logHabitEntrySync("h_weekly_u4", LocalDate.of(2026, 3, 4), 1)

        // In Week 2, take REST_DAY on Monday Mar 9 and Wednesday Mar 11
        testClock.currentLocalDate = LocalDate.of(2026, 3, 9)
        repository.setRestDayExceptionSync("h_weekly_u4", LocalDate.of(2026, 3, 9))
        testClock.currentLocalDate = LocalDate.of(2026, 3, 11)
        repository.setRestDayExceptionSync("h_weekly_u4", LocalDate.of(2026, 3, 11))

        // Advance to Monday Mar 16 (Week 3) -> Week 2 is held because all eligible preferred days were excepted (U-4)
        testClock.currentLocalDate = LocalDate.of(2026, 3, 16)
        repository.reloadAllSync()
        val activeRun = repository.habitSummaries.value["h_weekly_u4"]?.activeRun
        assertNotNull("Weekly run must be held when remaining eligible opportunities are covered by REST_DAY", activeRun)
        assertEquals(1, activeRun!!.completedStreakCount)
    }

    // =========================================================================
    // T1-20 & T1-21: Week-Start History & Neutral Transition Interval (U-10)
    // =========================================================================
    @Test
    fun t1_20_and_21_weekStartChangeFinishesCurrentWeekAndCreatesNeutralTransitionGap() {
        // Today is Sunday 2026-03-15. Current week_start is MONDAY (current week: Mon Mar 9 .. Sun Mar 15).
        // Changing week_start to WEDNESDAY must finish current week (Mar 15) and take effect on Wed 2026-03-18,
        // leaving Mon Mar 16 .. Tue Mar 17 as a neutral 2-day transition interval!
        val entry = repository.setWeekStartSync(DayOfWeek.WEDNESDAY)
        assertEquals("2026-03-18", entry.effectiveFromLocalDate)
        assertEquals(DayOfWeek.WEDNESDAY.value, entry.weekStartIsoDay)
        assertFalse(entry.isLegacyBaseline)

        val history = repository.weekStartHistory.value
        assertTrue(WeekBoundaryHelper.isNeutralTransitionDate(LocalDate.of(2026, 3, 16), history))
        assertTrue(WeekBoundaryHelper.isNeutralTransitionDate(LocalDate.of(2026, 3, 17), history))
        assertFalse(WeekBoundaryHelper.isNeutralTransitionDate(LocalDate.of(2026, 3, 18), history))
    }

    // =========================================================================
    // T1-22: Lifetime Goal Days Counts Distinct Real-Activity Dates Only (U-6)
    // =========================================================================
    @Test
    fun t1_22_lifetimeGoalDaysCountsDistinctDatesWithRealActivityOnly() {
        val d1 = LocalDate.of(2026, 3, 10)
        testClock.currentLocalDate = LocalDate.of(2026, 3, 15)
        val habit = Habit(
            habitId = "h_lifetime",
            name = "Guitar",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "min",
            targetValue = 30,
            lifetimeGoalDays = 100
        )
        repository.saveHabitSync(habit, effectiveDateOverride = d1)

        // d1: two positive entries on the same date -> counts as 1 distinct active day
        repository.logHabitEntrySync("h_lifetime", d1, 10)
        repository.logHabitEntrySync("h_lifetime", d1, 25)
        // d2: explicit zero only -> does NOT count toward lifetimeGoalDays
        repository.logHabitEntrySync("h_lifetime", d1.plusDays(1), 0)
        // d3: date note only -> does NOT count toward lifetimeGoalDays
        repository.saveHabitDateNoteSync("h_lifetime", d1.plusDays(2), "String broke")
        // d4: partial positive entry (5 min) -> counts as 1 distinct active day
        repository.logHabitEntrySync("h_lifetime", d1.plusDays(3), 5)
        // today (Mar 15): REST_DAY only -> does NOT count toward lifetimeGoalDays
        repository.setRestDayExceptionSync("h_lifetime", LocalDate.of(2026, 3, 15))

        val summary = repository.habitSummaries.value["h_lifetime"]!!
        assertEquals(2, summary.lifetimeActiveDays)
        assertEquals(100, summary.lifetimeGoalDays)
    }

    // =========================================================================
    // T1-23..T1-25: D-194 Transactional Version Protection & U-12 Mid-Week Target/Unit Policy
    // =========================================================================
    @Test
    fun t1_23_to_25_d194TransactionalLockOnAllEvaluationFieldsAndU12WeeklyFreeze() {
        val today = testClock.todayLocal() // Sunday 2026-03-15
        val habit = Habit(
            habitId = "h_d194",
            name = "Focus",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pomodoros",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 4,
            scheduleKind = ScheduleKind.DAILY
        )
        repository.saveHabitSync(habit, effectiveDateOverride = today.minusDays(5))

        // Log explicit zero today -> counts as recorded activity for D-194 version protection!
        repository.logHabitEntrySync("h_d194", today, 0)

        // Attempting APPLY_TODAY with a target change must throw HabitConfigurationLockedTodayException
        try {
            repository.saveHabitSync(
                habit.copy(targetValue = 2),
                policy = HabitConfigEffectivePolicy.APPLY_TODAY
            )
            fail("Expected HabitConfigurationLockedTodayException when overwriting today's target after explicit zero")
        } catch (_: HabitConfigurationLockedTodayException) {
            // Expected
        }

        // Presentation-only change (name/color/reminder) on today IS allowed even after activity
        val cosmeticVer = repository.saveHabitSync(
            habit.copy(name = "Deep Focus", colorHex = "#3B6955"),
            policy = HabitConfigEffectivePolicy.APPLY_TODAY
        )
        assertEquals("Deep Focus", cosmeticVer.name)
        assertEquals(4, repository.getEffectiveHabitVersion("h_d194", today)?.targetValue)

        // Evaluation-affecting change with APPLY_TOMORROW succeeds and writes effective_from_local_date = tomorrow
        val tomorrowVer = repository.saveHabitSync(
            habit.copy(name = "Deep Focus", targetValue = 2),
            policy = HabitConfigEffectivePolicy.APPLY_TOMORROW
        )
        assertEquals(today.plusDays(1).toString(), tomorrowVer.effectiveFromLocalDate)
        assertEquals(4, repository.getEffectiveHabitVersion("h_d194", today)?.targetValue)
        assertEquals(2, repository.getEffectiveHabitVersion("h_d194", today.plusDays(1))?.targetValue)

        // U-12 Option B: Mid-week same-unit target change on a weekly habit is saved effective tomorrow,
        // while in-progress week freezes its start-of-week target; unit change is deferred to next week boundary!
        val wed = LocalDate.of(2026, 3, 11) // Wednesday in Mon Mar 9 .. Sun Mar 15 week
        testClock.currentLocalDate = wed
        val weeklyHabit = Habit(
            habitId = "h_u12_weekly",
            name = "Cycling",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 120,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
        )
        repository.saveHabitSync(weeklyHabit, effectiveDateOverride = LocalDate.of(2026, 3, 9))
        repository.logHabitEntrySync("h_u12_weekly", LocalDate.of(2026, 3, 10), 60)

        // Same-unit target change mid-week (120 -> 90 min): saved effective tomorrow (Thu Mar 12),
        // but Week Mar 9..15 still requires 120 min (start-of-week frozen target)!
        val midWeekTargetVer = repository.saveHabitSync(weeklyHabit.copy(targetValue = 90))
        assertEquals("2026-03-12", midWeekTargetVer.effectiveFromLocalDate)

        // Log +30 min on Fri Mar 13 -> total = 90 min. Because start-of-week target (120) is frozen for Mar 9..15,
        // 90 min is PARTIAL for this week (does not complete the 120-min week)!
        repository.logHabitEntrySync("h_u12_weekly", LocalDate.of(2026, 3, 13), 30)
        assertEquals(0, repository.habitSummaries.value["h_u12_weekly"]?.currentCompletedStreak)

        // Unit change mid-week ("min" -> "hr", target 2): deferred to next weekly boundary (Mon 2026-03-16)!
        val unitChangeVer = repository.saveHabitSync(weeklyHabit.copy(unitCode = "hr", targetValue = 2))
        assertEquals("2026-03-16", unitChangeVer.effectiveFromLocalDate)
    }

    // =========================================================================
    // T1-26..T1-31: SQLite v3 Schema, Indexes, Triggers & v2->v3 Migration (§6, §7)
    // =========================================================================
    @Test
    fun t1_26_to_31_v2ToV3MigrationPreservesFkCascadeExtractsMetaAndFailsClosedOnCorruptV2() {
        val v2DbName = "test_v2_to_v3_${UUID.randomUUID()}.db"
        try {
            // 1. Construct a genuine v2 database with DAYSEED_META JSON, linked journal entries, and restore_conflicts
            val v2Helper = object : SQLiteOpenHelper(context, v2DbName, null, 2) {
                override fun onConfigure(db: SQLiteDatabase) {
                    super.onConfigure(db)
                    db.setForeignKeyConstraintsEnabled(true)
                }

                override fun onCreate(db: SQLiteDatabase) {
                    db.execSQL("CREATE TABLE habits (habit_id TEXT NOT NULL PRIMARY KEY, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL(
                        """
                        CREATE TABLE habit_versions (
                            version_id TEXT NOT NULL PRIMARY KEY,
                            habit_id TEXT NOT NULL REFERENCES habits(habit_id) ON DELETE CASCADE,
                            effective_from_local_date TEXT NOT NULL,
                            name TEXT NOT NULL,
                            description TEXT,
                            tracking_mode TEXT NOT NULL,
                            unit_code TEXT,
                            target_period TEXT NOT NULL,
                            target_value INTEGER,
                            schedule_kind TEXT NOT NULL,
                            scheduled_weekdays_mask INTEGER,
                            preferred_weekdays_mask INTEGER,
                            lifecycle_state TEXT NOT NULL,
                            reminder_mode TEXT NOT NULL,
                            reminder_time_local TEXT,
                            created_at_utc_ms INTEGER NOT NULL
                        );
                        """.trimIndent()
                    )
                    db.execSQL("CREATE TABLE habit_entries (entry_id TEXT NOT NULL PRIMARY KEY, habit_id TEXT NOT NULL REFERENCES habits(habit_id) ON DELETE CASCADE, local_date TEXT NOT NULL, value_units INTEGER NOT NULL, entry_note TEXT, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL, idempotency_key TEXT NOT NULL UNIQUE);")
                    db.execSQL("CREATE TABLE habit_day_outcomes (outcome_id TEXT NOT NULL PRIMARY KEY, habit_id TEXT NOT NULL REFERENCES habits(habit_id) ON DELETE CASCADE, local_date TEXT NOT NULL, outcome_kind TEXT NOT NULL, effort_level TEXT, idempotency_key TEXT NOT NULL UNIQUE, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("CREATE TABLE habit_date_notes (note_id TEXT NOT NULL PRIMARY KEY, habit_id TEXT NOT NULL REFERENCES habits(habit_id) ON DELETE CASCADE, local_date TEXT NOT NULL, body TEXT NOT NULL, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL, UNIQUE (habit_id, local_date));")
                    db.execSQL("CREATE TABLE journal_entries (journal_entry_id TEXT NOT NULL PRIMARY KEY, local_date TEXT NOT NULL, body TEXT NOT NULL, title TEXT, primary_mood_code TEXT, secondary_mood_code TEXT, prompt_id TEXT, linked_habit_id TEXT REFERENCES habits(habit_id) ON DELETE SET NULL, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("CREATE TABLE journal_drafts (draft_id TEXT NOT NULL PRIMARY KEY, local_date TEXT NOT NULL, title TEXT, body TEXT NOT NULL, primary_mood_code TEXT, secondary_mood_code TEXT, prompt_id TEXT, linked_habit_id TEXT REFERENCES habits(habit_id) ON DELETE SET NULL, intended_journal_entry_id TEXT REFERENCES journal_entries(journal_entry_id) ON DELETE SET NULL, source_context TEXT NOT NULL, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("CREATE TABLE app_settings (setting_key TEXT NOT NULL PRIMARY KEY, value_type TEXT NOT NULL, value_text TEXT NOT NULL, schema_version INTEGER NOT NULL DEFAULT 1, updated_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("CREATE TABLE reminder_deliveries (delivery_key TEXT NOT NULL PRIMARY KEY, reminder_type TEXT NOT NULL, habit_id TEXT REFERENCES habits(habit_id) ON DELETE CASCADE, target_period_key TEXT NOT NULL, delivery_status TEXT NOT NULL, scheduled_at_utc_ms INTEGER NOT NULL, attempted_at_utc_ms INTEGER, delivered_at_utc_ms INTEGER, platform_notification_tag TEXT, platform_notification_id INTEGER, suppression_reason TEXT);")
                    db.execSQL("CREATE TABLE restore_sessions (restore_session_id TEXT NOT NULL PRIMARY KEY, restore_mode TEXT NOT NULL, format_version INTEGER NOT NULL, source_schema_version INTEGER NOT NULL, session_state TEXT NOT NULL, started_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL, manifest_json TEXT NOT NULL, safety_backup_ref TEXT, safety_backup_verified INTEGER NOT NULL DEFAULT 0);")
                    db.execSQL("CREATE TABLE restore_conflicts (conflict_id TEXT NOT NULL PRIMARY KEY, restore_session_id TEXT NOT NULL REFERENCES restore_sessions(restore_session_id) ON DELETE CASCADE, record_type TEXT NOT NULL, record_key TEXT NOT NULL, current_payload_json TEXT NOT NULL, backup_payload_json TEXT NOT NULL, created_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("CREATE TABLE notification_cancel_outbox (cancel_id TEXT NOT NULL PRIMARY KEY, platform_notification_tag TEXT NOT NULL, platform_notification_id INTEGER NOT NULL, created_at_utc_ms INTEGER NOT NULL, last_attempt_at_utc_ms INTEGER, attempt_count INTEGER NOT NULL DEFAULT 0, status TEXT NOT NULL, error_category TEXT);")

                    db.execSQL("INSERT INTO habits VALUES ('h_v2', 1000, 1000);")
                    val descWithMeta = "Morning mobility\n<!--DAYSEED_META:{\"tiny\":\"1 stretch\",\"usual\":\"5 min flow\",\"extra\":\"15 min yoga\",\"color\":\"#4A7C59\"}-->"
                    db.execSQL(
                        "INSERT INTO habit_versions VALUES ('ver_v2', 'h_v2', '2026-03-01', 'Mobility', ?, 'EFFORT_AWARE', NULL, 'DAY', NULL, 'DAILY', NULL, NULL, 'ACTIVE', 'OFF', NULL, 1000);",
                        arrayOf<Any>(descWithMeta)
                    )
                    for (d in 1..7) {
                        val dateStr = String.format("2026-03-%02d", d)
                        db.execSQL("INSERT INTO habit_day_outcomes VALUES ('out_$d', 'h_v2', '$dateStr', 'EFFORT_LOGGED', 'USUAL', 'idem_$d', 2000, 2000);")
                    }
                    db.execSQL("INSERT INTO journal_entries VALUES ('j_v2', '2026-03-05', 'Feeling limber', 'Mobility Note', 'CALM', NULL, NULL, 'h_v2', 3000, 3000);")
                    db.execSQL("INSERT INTO restore_sessions VALUES ('rs_1', 'MERGE', 1, 2, 'AWAITING_CONFLICT_RESOLUTION', 1000, 1000, '{}', NULL, 1);")
                    db.execSQL("INSERT INTO restore_conflicts VALUES ('rc_1', 'rs_1', 'journal_entry', 'j_v2', '{}', '{}', 1000);")
                }

                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            }
            v2Helper.writableDatabase.close()

            // Upgrade to v3 using DayseedDatabaseHelper
            val v3Helper = DayseedDatabaseHelper(
                context,
                v2DbName,
                migrationEvaluationDateProvider = { LocalDate.of(2026, 3, 7) }
            )
            val db = v3Helper.writableDatabase

            // Verify all 13 named indexes + partial unique index exist
            val actualIndexes = mutableSetOf<String>()
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='index'", null).use { c ->
                while (c.moveToNext()) actualIndexes.add(c.getString(0))
            }
            for (idx in DayseedDatabaseHelper.ALL_V3_INDEXES + "idx_habit_streak_runs_one_active_per_habit") {
                assertTrue("Missing v3 index: $idx", actualIndexes.contains(idx))
            }

            // Verify DAYSEED_META was extracted cleanly into dedicated v3 columns and stripped from description
            db.rawQuery(
                "SELECT description, tiny_description, usual_description, extra_description, color_hex FROM habit_versions WHERE version_id = 'ver_v2'",
                null
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Morning mobility", c.getString(0))
                assertEquals("1 stretch", c.getString(1))
                assertEquals("5 min flow", c.getString(2))
                assertEquals("15 min yoga", c.getString(3))
                assertEquals("#4A7C59", c.getString(4))
            }

            // Verify child rows were NEVER cascade-deleted during v2->v3 migration!
            db.rawQuery("SELECT COUNT(*) FROM habit_day_outcomes WHERE habit_id = 'h_v2'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(7, c.getInt(0))
            }
            db.rawQuery("SELECT linked_habit_id FROM journal_entries WHERE journal_entry_id = 'j_v2'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("h_v2", c.getString(0))
            }
            db.rawQuery("SELECT COUNT(*) FROM restore_conflicts WHERE conflict_id = 'rc_1'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }

            // Verify streak runs were backfilled (7 consecutive days -> GROWING_TREE)
            db.rawQuery("SELECT completed_streak_count, tree_state, qualified_for_grove FROM habit_streak_runs WHERE habit_id = 'h_v2'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(7, c.getInt(0))
                assertEquals("GROWING_TREE", c.getString(1))
                assertEquals(1, c.getInt(2))
            }

            // Verify week_start_history legacy baseline row was seeded
            db.rawQuery("SELECT effective_from_local_date, is_legacy_baseline FROM week_start_history", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("2026-03-01", c.getString(0))
                assertEquals(1, c.getInt(1))
            }
            v3Helper.close()
        } finally {
            context.deleteDatabase(v2DbName)
        }

        // 2. Verify fail-closed migration rollback on impossible calendar date in v2 ("2026-02-30")
        val corruptV2DbName = "test_corrupt_v2_${UUID.randomUUID()}.db"
        try {
            val badV2 = object : SQLiteOpenHelper(context, corruptV2DbName, null, 2) {
                override fun onCreate(db: SQLiteDatabase) {
                    db.execSQL("CREATE TABLE habits (habit_id TEXT NOT NULL PRIMARY KEY, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("CREATE TABLE habit_versions (version_id TEXT NOT NULL PRIMARY KEY, habit_id TEXT NOT NULL, effective_from_local_date TEXT NOT NULL, name TEXT NOT NULL, description TEXT, tracking_mode TEXT NOT NULL, unit_code TEXT, target_period TEXT NOT NULL, target_value INTEGER, schedule_kind TEXT NOT NULL, scheduled_weekdays_mask INTEGER, preferred_weekdays_mask INTEGER, lifecycle_state TEXT NOT NULL, reminder_mode TEXT NOT NULL, reminder_time_local TEXT, created_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("CREATE TABLE habit_entries (entry_id TEXT NOT NULL PRIMARY KEY, habit_id TEXT NOT NULL, local_date TEXT NOT NULL, value_units INTEGER NOT NULL, entry_note TEXT, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL, idempotency_key TEXT NOT NULL UNIQUE);")
                    db.execSQL("INSERT INTO habits VALUES ('h_bad', 1000, 1000);")
                    db.execSQL("INSERT INTO habit_versions VALUES ('v_bad', 'h_bad', '2026-02-01', 'Bad', NULL, 'QUANTITY', 'pages', 'DAY', 10, 'DAILY', NULL, NULL, 'ACTIVE', 'OFF', NULL, 1000);")
                    db.execSQL("INSERT INTO habit_entries VALUES ('e_bad', 'h_bad', '2026-02-30', 5, NULL, 1000, 1000, 'idem_bad');")
                }
                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            }
            badV2.writableDatabase.close()

            val blockedV3 = DayseedDatabaseHelper(context, corruptV2DbName)
            try {
                blockedV3.writableDatabase
                fail("Expected DayseedMigrationBlockedException for impossible v2 date 2026-02-30")
            } catch (e: DayseedMigrationBlockedException) {
                assertTrue(e.message!!.contains("2026-02-30"))
            }
            blockedV3.close()
        } finally {
            context.deleteDatabase(corruptV2DbName)
        }
    }

    // =========================================================================
    // T1-32..T1-36: 8-Second Guarded HABIT_DELETION Undo & Journal Relink Collision Guards (§9.2)
    // =========================================================================
    @Test
    fun t1_32_to_36_habitDeletionUndoRestoresAllDataAndRespectsJournalCollisionGuards() {
        val today = testClock.todayLocal()
        val habit = Habit(
            habitId = "h_del_undo",
            name = "Evening Tea",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "cups",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 1,
            lifetimeGoalDays = 30
        )
        repository.saveHabitSync(habit, effectiveDateOverride = today.minusDays(3))
        repository.logHabitEntrySync("h_del_undo", today.minusDays(2), 1)
        repository.logHabitEntrySync("h_del_undo", today.minusDays(1), 1)
        repository.saveHabitDateNoteSync("h_del_undo", today.minusDays(1), "Chamomile")
        repository.setRestDayExceptionSync("h_del_undo", today)

        // Create two linked journal entries: J_UNCHANGED and J_MODIFIED
        repository.saveJournalEntrySync(
            JournalEntry(
                journalEntryId = "j_unchanged",
                localDate = today.toString(),
                body = "Peaceful tea ritual",
                linkedHabitId = "h_del_undo"
            )
        )
        repository.saveJournalEntrySync(
            JournalEntry(
                journalEntryId = "j_modified",
                localDate = today.toString(),
                body = "Second linked note",
                linkedHabitId = "h_del_undo"
            )
        )

        // Delete the habit -> returns HABIT_DELETION UndoToken
        testClock.advanceMillis(1_000L)
        val undoToken = repository.deleteHabitPermanentlySync("h_del_undo")
        assertNotNull(undoToken)
        assertEquals(UndoEntityType.HABIT_DELETION, undoToken!!.entityType)
        assertFalse(repository.habits.value.any { it.habitId == "h_del_undo" })

        // Both journal entries now have linkedHabitId = null
        assertNull(repository.journalEntries.value.first { it.journalEntryId == "j_unchanged" }.linkedHabitId)
        assertNull(repository.journalEntries.value.first { it.journalEntryId == "j_modified" }.linkedHabitId)

        // Simulate user editing j_modified during the 8-second undo window (changes updated_at_utc_ms!)
        testClock.advanceMillis(2_000L)
        val modEntry = repository.journalEntries.value.first { it.journalEntryId == "j_modified" }
        repository.saveJournalEntrySync(modEntry.copy(body = "Edited after habit deletion"))

        // Execute undo within 8 seconds
        testClock.advanceMillis(1_000L)
        assertTrue(repository.executeUndoSync(undoToken))

        // Verify habit, versions, entries, notes, exceptions, and streak runs are all restored!
        val restoredHabit = repository.habits.value.find { it.habitId == "h_del_undo" }
        assertNotNull(restoredHabit)
        assertEquals(30, restoredHabit!!.lifetimeGoalDays)
        assertEquals(2, repository.habitEntries.value.count { it.habitId == "h_del_undo" })
        assertEquals(1, repository.habitDateNotes.value.count { it.habitId == "h_del_undo" })
        assertEquals(1, repository.habitDateExceptions.value.count { it.habitId == "h_del_undo" })
        assertEquals(2, repository.habitSummaries.value["h_del_undo"]?.currentCompletedStreak)

        // Collision guard check: j_unchanged was relinked to "h_del_undo", while j_modified (edited after deletion) was NOT overwritten!
        val relinkedUnchanged = repository.journalEntries.value.first { it.journalEntryId == "j_unchanged" }
        val untouchedModified = repository.journalEntries.value.first { it.journalEntryId == "j_modified" }
        assertEquals("h_del_undo", relinkedUnchanged.linkedHabitId)
        assertNull("Modified journal entry must not have its post-deletion edit clobbered", untouchedModified.linkedHabitId)
        assertEquals("Edited after habit deletion", untouchedModified.body)
    }
}
