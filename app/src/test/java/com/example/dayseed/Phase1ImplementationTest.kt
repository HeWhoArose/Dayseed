package com.example.dayseed

import android.content.ContentValues
import android.content.Context
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.CompactHistoryBucket
import com.example.dayseed.data.ConflictResolution
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
import org.json.JSONArray
import org.json.JSONObject

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
        assertEquals(ProgressBand.EXTRA, ProgressBandClassifier.classify(150, target))
        assertEquals(ProgressBand.EXTRA, ProgressBandClassifier.classify(3, 2))
        assertEquals(ProgressBand.HUGE, ProgressBandClassifier.classify(151, target))
        assertEquals(ProgressBand.HUGE, ProgressBandClassifier.classify(301, 200))
        assertEquals(ProgressBand.HUGE, ProgressBandClassifier.classify(250, target))

        // Fractional boundary ratios (target = 200)
        assertEquals(ProgressBand.TINY_STEP, ProgressBandClassifier.classify(1, 200)) // 0.5%
        assertEquals(ProgressBand.TINY_STEP, ProgressBandClassifier.classify(49, 200)) // 24.5%
        assertEquals(ProgressBand.GETTING_STARTED, ProgressBandClassifier.classify(50, 200)) // 25.0%
        assertEquals(ProgressBand.GETTING_STARTED, ProgressBandClassifier.classify(99, 200)) // 49.5%
        assertEquals(ProgressBand.HALFWAY, ProgressBandClassifier.classify(100, 200)) // 50.0%
        assertEquals(ProgressBand.HALFWAY, ProgressBandClassifier.classify(149, 200)) // 74.5%
        assertEquals(ProgressBand.ALMOST_THERE, ProgressBandClassifier.classify(150, 200)) // 75.0%
        assertEquals(ProgressBand.ALMOST_THERE, ProgressBandClassifier.classify(199, 200)) // 99.5%
        assertEquals(ProgressBand.GOAL_MET, ProgressBandClassifier.classify(200, 200)) // 100.0%
        assertEquals(ProgressBand.EXTRA, ProgressBandClassifier.classify(201, 200)) // 100.5%
        assertEquals(ProgressBand.EXTRA, ProgressBandClassifier.classify(300, 200)) // 150.0%
        assertEquals(ProgressBand.HUGE, ProgressBandClassifier.classify(301, 200)) // 150.5%

        // D-040 visible labels and fractional-safe shortBadges
        assertEquals("Zero logged", ProgressBand.ZERO.label)
        assertEquals("0%", ProgressBand.ZERO.shortBadge)
        assertEquals("Tiny", ProgressBand.TINY_STEP.label)
        assertEquals(">0–<25%", ProgressBand.TINY_STEP.shortBadge)
        assertEquals("Small", ProgressBand.GETTING_STARTED.label)
        assertEquals("25–<50%", ProgressBand.GETTING_STARTED.shortBadge)
        assertEquals("Halfway", ProgressBand.HALFWAY.label)
        assertEquals("50–<75%", ProgressBand.HALFWAY.shortBadge)
        assertEquals("Nearly there", ProgressBand.ALMOST_THERE.label)
        assertEquals("75–<100%", ProgressBand.ALMOST_THERE.shortBadge)
        assertEquals("Goal met", ProgressBand.GOAL_MET.label)
        assertEquals("100%", ProgressBand.GOAL_MET.shortBadge)
        assertEquals("Extra", ProgressBand.EXTRA.label)
        assertEquals(">100–150%", ProgressBand.EXTRA.shortBadge)
        assertEquals("Huge", ProgressBand.HUGE.label)
        assertEquals(">150%", ProgressBand.HUGE.shortBadge)

        // Every detailed band has non-blank, distinct encouragement and motivation quotes (D-216 Option A)
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
        testClock.currentLocalDate = LocalDate.of(2026, 3, 13)
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

    // =========================================================================
    // T1-37: Chained v1->v2->v3 Migration & Existing-v3 In-Place Reconciliation (D-219)
    // =========================================================================
    @Test
    fun t1_37_chainedV1ToV2ToV3MigrationAndExistingV3Reconciliation() {
        // 1. Chained v1 -> v3 migration across all 4 v1 tracking modes
        val v1DbName = "test_chained_v1_v3_${UUID.randomUUID()}.db"
        try {
            val v1Helper = object : SQLiteOpenHelper(context, v1DbName, null, 1) {
                override fun onCreate(db: SQLiteDatabase) {
                    db.execSQL("CREATE TABLE habits (id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT, tracking_mode TEXT NOT NULL, tiny_desc TEXT, usual_desc TEXT, extra_desc TEXT, schedule_days TEXT NOT NULL, is_archived INTEGER NOT NULL DEFAULT 0, color_hex TEXT NOT NULL, created_at INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE daily_records (id TEXT PRIMARY KEY, habit_id TEXT NOT NULL, record_date TEXT NOT NULL, outcome TEXT NOT NULL, effort_level TEXT NOT NULL, note TEXT, updated_at INTEGER NOT NULL, UNIQUE(habit_id, record_date))")
                    db.execSQL("CREATE TABLE journal_entries (id TEXT PRIMARY KEY, entry_date TEXT NOT NULL, prompt TEXT, content TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE app_preferences (pref_key TEXT PRIMARY KEY, pref_value TEXT NOT NULL)")

                    db.execSQL("INSERT INTO habits VALUES ('h_bin', 'Make Bed', 'Morning', 'BINARY', '', '', '', '1,2,3,4,5,6,7', 0, '#286143', 1000)")
                    db.execSQL("INSERT INTO habits VALUES ('h_eff', 'Stretch', 'Back', 'EFFORT_LEVELS', '1m', '5m', '10m', 'MONDAY,WEDNESDAY', 0, '#3B6955', 1000)")
                    db.execSQL("INSERT INTO habits VALUES ('h_qty', 'Water', 'Glasses', 'QUANTITY', '', '', '', '1,2,3,4,5,6,7', 0, '#4A7C59', 1000)")
                    db.execSQL("INSERT INTO habits VALUES ('h_dur', 'Walk', 'Outdoor', 'DURATION', '', '', '', '1,2,3,4,5,6,7', 1, '#588157', 1000)")

                    db.execSQL("INSERT INTO daily_records VALUES ('r_bin', 'h_bin', '2026-03-10', 'COMPLETED', 'STANDARD', 'Neat', 2000)")
                    db.execSQL("INSERT INTO daily_records VALUES ('r_eff', 'h_eff', '2026-03-11', 'COMPLETED', 'TINY', 'Gentle', 2000)")
                    db.execSQL("INSERT INTO daily_records VALUES ('r_qty', 'h_qty', '2026-03-12', 'COMPLETED', 'STANDARD', 'Hydrated', 2000)")
                    db.execSQL("INSERT INTO daily_records VALUES ('r_dur', 'h_dur', '2026-03-12', 'COMPLETED', 'STANDARD', '', 2000)")
                    db.execSQL("INSERT INTO journal_entries VALUES ('j_1', '2026-03-12', 'prompt_energy', 'Felt steady today', 3000, 3500)")
                    db.execSQL("INSERT INTO app_preferences VALUES ('week_start', 'SUNDAY')")
                }
                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            }
            v1Helper.writableDatabase.close()

            val v3Helper = DayseedDatabaseHelper(
                context,
                v1DbName,
                migrationEvaluationDateProvider = { LocalDate.of(2026, 3, 12) }
            )
            val db = v3Helper.writableDatabase
            assertEquals(3, db.version)

            // Verify BINARY + EFFORT_LEVELS records migrated to habit_day_outcomes and QUANTITY + DURATION migrated to habit_entries
            db.rawQuery("SELECT COUNT(*) FROM habit_day_outcomes", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(2, c.getInt(0))
            }
            db.rawQuery("SELECT COUNT(*) FROM habit_entries", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(2, c.getInt(0))
            }
            // Verify streak runs were backfilled during chained v1 -> v2 -> v3 upgrade
            db.rawQuery("SELECT COUNT(*) FROM habit_streak_runs", null).use { c ->
                assertTrue(c.moveToFirst())
                assertTrue(c.getInt(0) >= 3)
            }
            v3Helper.close()
        } finally {
            context.deleteDatabase(v1DbName)
        }

        // 1b. Original v1 schema with 'date' column in daily_records and journal_entries (commit 4c1187aa) + PLANNED_REST
        val origV1DbName = "test_orig_v1_date_col_${UUID.randomUUID()}.db"
        try {
            val origV1Helper = object : SQLiteOpenHelper(context, origV1DbName, null, 1) {
                override fun onCreate(db: SQLiteDatabase) {
                    db.execSQL("CREATE TABLE habits (id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT, tracking_mode TEXT NOT NULL, tiny_desc TEXT, usual_desc TEXT, extra_desc TEXT, schedule_days TEXT NOT NULL, is_archived INTEGER DEFAULT 0, color_hex TEXT, created_at INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE daily_records (id TEXT PRIMARY KEY, habit_id TEXT NOT NULL, date TEXT NOT NULL, outcome TEXT NOT NULL, effort_level TEXT NOT NULL, note TEXT, updated_at INTEGER NOT NULL, UNIQUE(habit_id, date))")
                    db.execSQL("CREATE TABLE journal_entries (id TEXT PRIMARY KEY, date TEXT NOT NULL, prompt TEXT, content TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE app_preferences (pref_key TEXT PRIMARY KEY, pref_value TEXT NOT NULL)")

                    db.execSQL("INSERT INTO habits VALUES ('h_move', 'Gentle Movement', 'Move with kindness', 'EFFORT_LEVELS', '1 stretch', '10m walk', '25m walk', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY', 0, '#286143', 1000)")
                    db.execSQL("INSERT INTO daily_records VALUES ('r_move_1', 'h_move', '2026-03-10', 'COMPLETED', 'USUAL', 'Walked outside', 2000)")
                    db.execSQL("INSERT INTO daily_records VALUES ('r_move_rest', 'h_move', '2026-03-11', 'PLANNED_REST', 'STANDARD', 'Rested knee', 2500)")
                    db.execSQL("INSERT INTO journal_entries VALUES ('j_orig_1', '2026-03-10', 'Prompt', 'Started Dayseed today.', 3000, 3000)")
                }
                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            }
            origV1Helper.writableDatabase.close()

            val upgradedHelper = DayseedDatabaseHelper(context, origV1DbName) { LocalDate.of(2026, 3, 12) }
            val udb = upgradedHelper.writableDatabase
            udb.rawQuery("SELECT COUNT(*) FROM habit_day_outcomes WHERE habit_id = 'h_move'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }
            udb.rawQuery("SELECT COUNT(*) FROM habit_date_exceptions WHERE habit_id = 'h_move' AND exception_kind = 'REST_DAY'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }
            udb.rawQuery("SELECT COUNT(*) FROM journal_entries WHERE journal_entry_id = 'j_orig_1'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }
            upgradedHelper.close()
        } finally {
            context.deleteDatabase(origV1DbName)
        }

        // 2. Existing-v3 database reconciliation on open (D-219: DATABASE_VERSION stays 3)
        val oldV3DbName = "test_old_v3_reconcile_${UUID.randomUUID()}.db"
        try {
            val oldV3Helper = object : SQLiteOpenHelper(context, oldV3DbName, null, 3) {
                override fun onCreate(db: SQLiteDatabase) {
                    // Create canonical v3 schema first, then replace habit_versions and restore_conflicts with the older v3 CHECK definitions
                    val tempHelper = DayseedDatabaseHelper(context, null)
                    tempHelper.createV3Schema(db)
                    db.execSQL("DROP TRIGGER IF EXISTS trg_entries_mode_insert;")
                    db.execSQL("DROP TRIGGER IF EXISTS trg_entries_mode_update;")
                    db.execSQL("DROP TRIGGER IF EXISTS trg_outcomes_mode_insert;")
                    db.execSQL("DROP TRIGGER IF EXISTS trg_outcomes_mode_update;")
                    db.execSQL("DROP TABLE habit_versions;")
                    db.execSQL(
                        """
                        CREATE TABLE habit_versions (
                            version_id TEXT NOT NULL PRIMARY KEY CHECK (length(trim(version_id)) > 0),
                            habit_id TEXT NOT NULL REFERENCES habits(habit_id) ON DELETE CASCADE,
                            effective_from_local_date TEXT NOT NULL,
                            name TEXT NOT NULL,
                            description TEXT,
                            tracking_mode TEXT NOT NULL CHECK (tracking_mode IN ('QUANTITY', 'DURATION', 'COMPLETION_ONLY', 'EFFORT_AWARE')),
                            unit_code TEXT,
                            target_period TEXT NOT NULL CHECK (target_period IN ('DAY', 'WEEK')),
                            target_value INTEGER CHECK (
                                (tracking_mode IN ('QUANTITY', 'DURATION') AND target_value IS NOT NULL AND target_value >= 1 AND target_value <= 2147483647)
                                OR (tracking_mode IN ('COMPLETION_ONLY', 'EFFORT_AWARE') AND target_value IS NULL)
                            ),
                            schedule_kind TEXT NOT NULL,
                            scheduled_weekdays_mask INTEGER,
                            preferred_weekdays_mask INTEGER,
                            lifecycle_state TEXT NOT NULL,
                            reminder_mode TEXT NOT NULL,
                            reminder_time_local TEXT,
                            tiny_description TEXT NOT NULL DEFAULT 'Deliberately small action',
                            usual_description TEXT NOT NULL DEFAULT 'The ordinary intended action',
                            extra_description TEXT NOT NULL DEFAULT 'Optional stretch action',
                            color_hex TEXT NOT NULL DEFAULT '#286143',
                            sort_order INTEGER NOT NULL DEFAULT 0,
                            created_at_utc_ms INTEGER NOT NULL CHECK (created_at_utc_ms >= 0),
                            UNIQUE (habit_id, effective_from_local_date)
                        );
                        """.trimIndent()
                    )
                    db.execSQL("DROP TABLE restore_conflicts;")
                    db.execSQL(
                        """
                        CREATE TABLE restore_conflicts (
                            conflict_id TEXT NOT NULL PRIMARY KEY CHECK (length(trim(conflict_id)) > 0),
                            restore_session_id TEXT NOT NULL REFERENCES restore_sessions(restore_session_id) ON DELETE CASCADE,
                            record_type TEXT NOT NULL CHECK (
                                record_type IN (
                                    'habit',
                                    'habit_version',
                                    'habit_entry',
                                    'habit_day_outcome',
                                    'habit_date_note',
                                    'habit_date_exception',
                                    'journal_entry',
                                    'journal_draft',
                                    'app_setting',
                                    'week_start_history'
                                )
                            ),
                            record_key TEXT NOT NULL CHECK (length(trim(record_key)) > 0),
                            current_payload_json TEXT NOT NULL,
                            backup_payload_json TEXT NOT NULL,
                            created_at_utc_ms INTEGER NOT NULL CHECK (created_at_utc_ms >= 0)
                        );
                        """.trimIndent()
                    )
                    db.execSQL("INSERT INTO habits VALUES ('h_existing_v3', 1000, 1000, 'Health', 30);")
                    db.execSQL("INSERT INTO habit_versions VALUES ('v_existing_v3', 'h_existing_v3', '2026-03-01', 'Yoga', NULL, 'COMPLETION_ONLY', NULL, 'DAY', NULL, 'DAILY', NULL, NULL, 'ACTIVE', 'OFF', NULL, 'Tiny', 'Usual', 'Extra', '#286143', 0, 1000);")
                    db.execSQL("INSERT INTO restore_sessions VALUES ('rs_existing', 'MERGE', 1, 3, 'AWAITING_CONFLICT_RESOLUTION', 1000, 1000, '{}', NULL, 1);")
                    db.execSQL("INSERT INTO restore_conflicts VALUES ('rc_existing', 'rs_existing', 'habit', 'h_existing_v3', '{}', '{}', 1000);")
                }
                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            }
            oldV3Helper.writableDatabase.close()

            val reconciledHelper = DayseedDatabaseHelper(context, oldV3DbName)
            val db = reconciledHelper.writableDatabase
            assertEquals(3, db.version)

            // Verify existing rows were preserved and new v3 constraints now permit weekly COMPLETION_ONLY target_value > 1 and 'habit_streak_run' conflicts
            db.rawQuery("SELECT COUNT(*) FROM habit_versions WHERE version_id = 'v_existing_v3'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }
            db.execSQL("INSERT INTO habit_versions VALUES ('v_weekly_check', 'h_existing_v3', '2026-03-10', 'Yoga 3x', NULL, 'COMPLETION_ONLY', NULL, 'WEEK', 3, 'FLEXIBLE_WEEKLY_TARGET', NULL, NULL, 'ACTIVE', 'OFF', NULL, 'Tiny', 'Usual', 'Extra', '#286143', 0, 2000);")
            db.execSQL("INSERT INTO restore_conflicts VALUES ('rc_streak', 'rs_existing', 'habit_streak_run', 'h_existing_v3:2026-03-01', '{}', '{}', 2000);")

            // The reconciliation must preserve records in every related table touched by this fixture.
            db.rawQuery("SELECT COUNT(*) FROM habits WHERE habit_id = 'h_existing_v3'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }
            db.rawQuery("SELECT COUNT(*) FROM restore_sessions WHERE restore_session_id = 'rs_existing'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }
            db.rawQuery("SELECT COUNT(*) FROM restore_conflicts WHERE conflict_id IN ('rc_existing', 'rc_streak')", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(2, c.getInt(0))
            }

            // Primary keys and foreign keys must survive the in-place table rebuilds.
            db.rawQuery("PRAGMA table_info(habit_versions)", null).use { c ->
                var versionIdIsPrimaryKey = false
                while (c.moveToNext()) {
                    if (c.getString(c.getColumnIndexOrThrow("name")) == "version_id") {
                        versionIdIsPrimaryKey = c.getInt(c.getColumnIndexOrThrow("pk")) == 1
                    }
                }
                assertTrue(versionIdIsPrimaryKey)
            }
            db.rawQuery("PRAGMA foreign_key_list(habit_versions)", null).use { c ->
                var habitForeignKeyFound = false
                while (c.moveToNext()) {
                    if (c.getString(c.getColumnIndexOrThrow("table")) == "habits" &&
                        c.getString(c.getColumnIndexOrThrow("from")) == "habit_id"
                    ) habitForeignKeyFound = true
                }
                assertTrue(habitForeignKeyFound)
            }
            db.rawQuery("PRAGMA foreign_key_list(restore_conflicts)", null).use { c ->
                var sessionForeignKeyFound = false
                while (c.moveToNext()) {
                    if (c.getString(c.getColumnIndexOrThrow("table")) == "restore_sessions" &&
                        c.getString(c.getColumnIndexOrThrow("from")) == "restore_session_id"
                    ) sessionForeignKeyFound = true
                }
                assertTrue(sessionForeignKeyFound)
            }
            db.rawQuery("PRAGMA table_info(restore_conflicts)", null).use { c ->
                var conflictIdIsPrimaryKey = false
                while (c.moveToNext()) {
                    if (c.getString(c.getColumnIndexOrThrow("name")) == "conflict_id") {
                        conflictIdIsPrimaryKey = c.getInt(c.getColumnIndexOrThrow("pk")) == 1
                    }
                }
                assertTrue(conflictIdIsPrimaryKey)
            }

            // All canonical app-managed indexes and triggers must exist after reconciliation.
            val actualIndexes = mutableSetOf<String>()
            db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'index' AND name NOT LIKE 'sqlite_%'", null).use { c ->
                while (c.moveToNext()) actualIndexes.add(c.getString(0))
            }
            assertTrue(actualIndexes.containsAll(DayseedDatabaseHelper.ALL_V3_INDEXES))
            val actualTriggers = mutableSetOf<String>()
            db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'trigger'", null).use { c ->
                while (c.moveToNext()) actualTriggers.add(c.getString(0))
            }
            assertEquals(
                setOf(
                    "trg_habit_entries_validate_insert",
                    "trg_habit_entries_validate_update",
                    "trg_habit_outcomes_validate_insert",
                    "trg_habit_outcomes_validate_update",
                    "trg_habit_exceptions_validate_insert",
                    "trg_habit_exceptions_validate_update"
                ),
                actualTriggers
            )
            db.rawQuery("PRAGMA integrity_check", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("ok", c.getString(0))
            }

            // Verify idempotence, then close/reopen to exercise the actual onOpen path twice.
            reconciledHelper.reconcileExistingV3SchemaIfNeeded(db)
            reconciledHelper.close()
            val reopenedHelper = DayseedDatabaseHelper(context, oldV3DbName)
            val reopenedDb = reopenedHelper.writableDatabase
            reopenedDb.rawQuery("PRAGMA integrity_check", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("ok", c.getString(0))
            }
            reopenedDb.rawQuery("PRAGMA foreign_key_check", null).use { c ->
                assertFalse(c.moveToFirst())
            }
            reopenedDb.rawQuery("SELECT COUNT(*) FROM restore_conflicts WHERE conflict_id IN ('rc_existing', 'rc_streak')", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(2, c.getInt(0))
            }
            reopenedHelper.close()
        } finally {
            context.deleteDatabase(oldV3DbName)
        }

        // 3. Verify failed v3 reconciliation rolls back and leaves the original v3 database recoverable
        val badOldV3DbName = "test_bad_old_v3_${UUID.randomUUID()}.db"
        try {
            val badOldV3Helper = object : SQLiteOpenHelper(context, badOldV3DbName, null, 3) {
                override fun onCreate(db: SQLiteDatabase) {
                    val tempHelper = DayseedDatabaseHelper(context, null)
                    tempHelper.createV3Schema(db)
                    db.execSQL("DROP TABLE restore_conflicts;")
                    // Older table without 'habit_streak_run' and with a corrupt row (empty conflict_id)
                    db.execSQL("CREATE TABLE restore_conflicts (conflict_id TEXT NOT NULL PRIMARY KEY, restore_session_id TEXT NOT NULL, record_type TEXT NOT NULL, record_key TEXT NOT NULL, current_payload_json TEXT NOT NULL, backup_payload_json TEXT NOT NULL, created_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("INSERT INTO habits VALUES ('h_recover', 1000, 1000, NULL, NULL);")
                    db.execSQL("INSERT INTO restore_conflicts VALUES ('   ', 'rs_none', 'INVALID_TYPE', 'k', '{}', '{}', 1000);")
                }
                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            }
            badOldV3Helper.writableDatabase.close()

            // Capture the exact pre-reconciliation schema objects so rollback is checked, not inferred.
            val beforeRollbackDb = SQLiteDatabase.openDatabase(
                context.getDatabasePath(badOldV3DbName).absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            val originalConflictTableSql = beforeRollbackDb.rawQuery(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'restore_conflicts'",
                null
            ).use { c -> assertTrue(c.moveToFirst()); c.getString(0) }
            val originalTriggerNames = mutableSetOf<String>()
            beforeRollbackDb.rawQuery("SELECT name FROM sqlite_master WHERE type = 'trigger'", null).use { c ->
                while (c.moveToNext()) originalTriggerNames.add(c.getString(0))
            }
            beforeRollbackDb.close()

            val failingHelper = DayseedDatabaseHelper(context, badOldV3DbName)
            try {
                failingHelper.writableDatabase
                fail("Expected DayseedMigrationBlockedException on corrupt restore_conflicts row during v3 reconciliation")
            } catch (_: DayseedMigrationBlockedException) {
                // Expected rollback
            }
            failingHelper.close()

            // Verify original database was left intact and recoverable
            val rawDb = SQLiteDatabase.openDatabase(
                context.getDatabasePath(badOldV3DbName).absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            rawDb.rawQuery("SELECT COUNT(*) FROM habits WHERE habit_id = 'h_recover'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }
            rawDb.rawQuery(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'restore_conflicts'",
                null
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(originalConflictTableSql, c.getString(0))
            }
            val restoredTriggerNames = mutableSetOf<String>()
            rawDb.rawQuery("SELECT name FROM sqlite_master WHERE type = 'trigger'", null).use { c ->
                while (c.moveToNext()) restoredTriggerNames.add(c.getString(0))
            }
            assertEquals(originalTriggerNames, restoredTriggerNames)
            rawDb.rawQuery("PRAGMA integrity_check", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("ok", c.getString(0))
            }
            rawDb.close()
        } finally {
            context.deleteDatabase(badOldV3DbName)
        }
    }

    // =========================================================================
    // T1-38: Fail-Closed v2->v3 Migration Rollback Tests (Negative/Overflow Units, Unsupported Outcomes, Corrupt Schedules)
    // =========================================================================
    @Test
    fun t1_38_v2ToV3FailClosedRollbacksPreserveOriginalV2State() {
        fun assertV2MigrationRollsBack(setupCorruptV2: (SQLiteDatabase) -> Unit, expectedMessageFragment: String) {
            val dbName = "test_v2_rollback_${UUID.randomUUID()}.db"
            try {
                val v2Helper = object : SQLiteOpenHelper(context, dbName, null, 2) {
                    override fun onCreate(db: SQLiteDatabase) {
                        db.execSQL("CREATE TABLE habits (habit_id TEXT NOT NULL PRIMARY KEY, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL);")
                        db.execSQL("CREATE TABLE habit_versions (version_id TEXT NOT NULL PRIMARY KEY, habit_id TEXT NOT NULL, effective_from_local_date TEXT NOT NULL, name TEXT NOT NULL, description TEXT, tracking_mode TEXT NOT NULL, unit_code TEXT, target_period TEXT NOT NULL, target_value INTEGER, schedule_kind TEXT NOT NULL, scheduled_weekdays_mask INTEGER, preferred_weekdays_mask INTEGER, lifecycle_state TEXT NOT NULL, reminder_mode TEXT NOT NULL, reminder_time_local TEXT, created_at_utc_ms INTEGER NOT NULL);")
                        db.execSQL("CREATE TABLE habit_entries (entry_id TEXT NOT NULL PRIMARY KEY, habit_id TEXT NOT NULL, local_date TEXT NOT NULL, value_units INTEGER NOT NULL, entry_note TEXT, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL, idempotency_key TEXT NOT NULL UNIQUE);")
                        db.execSQL("CREATE TABLE habit_day_outcomes (outcome_id TEXT NOT NULL PRIMARY KEY, habit_id TEXT NOT NULL, local_date TEXT NOT NULL, outcome_kind TEXT NOT NULL, effort_level TEXT, idempotency_key TEXT NOT NULL UNIQUE, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL);")
                        db.execSQL("INSERT INTO habits VALUES ('h1', 1000, 1000);")
                        db.execSQL("INSERT INTO habit_versions VALUES ('v1', 'h1', '2026-03-01', 'Valid', NULL, 'QUANTITY', 'count', 'DAY', 5, 'DAILY', NULL, NULL, 'ACTIVE', 'OFF', NULL, 1000);")
                        setupCorruptV2(db)
                    }
                    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                }
                v2Helper.writableDatabase.close()

                val v3Helper = DayseedDatabaseHelper(context, dbName)
                try {
                    v3Helper.writableDatabase
                    fail("Expected DayseedMigrationBlockedException containing '$expectedMessageFragment'")
                } catch (e: DayseedMigrationBlockedException) {
                    assertTrue("Message was: ${e.message}", e.message!!.contains(expectedMessageFragment, ignoreCase = true))
                }
                v3Helper.close()

                // Verify SQLite rolled back the failed upgrade: version is still 2 and original rows remain intact
                val rawDb = SQLiteDatabase.openDatabase(
                    context.getDatabasePath(dbName).absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                assertEquals(2, rawDb.version)
                rawDb.rawQuery("SELECT COUNT(*) FROM habits WHERE habit_id = 'h1'", null).use { c ->
                    assertTrue(c.moveToFirst())
                    assertEquals(1, c.getInt(0))
                }
                rawDb.close()
            } finally {
                context.deleteDatabase(dbName)
            }
        }

        // 1. Negative value_units in habit_entries
        assertV2MigrationRollsBack(
            setupCorruptV2 = { db ->
                db.execSQL("INSERT INTO habit_entries VALUES ('e_neg', 'h1', '2026-03-02', -5, NULL, 1000, 1000, 'idem_neg');")
            },
            expectedMessageFragment = "negative value_units"
        )

        // 2. Out-of-range 64-bit integer (> Int.MAX_VALUE) in habit_entries
        assertV2MigrationRollsBack(
            setupCorruptV2 = { db ->
                val overflow = Int.MAX_VALUE.toLong() + 50L
                db.execSQL("INSERT INTO habit_entries VALUES ('e_ovf', 'h1', '2026-03-02', $overflow, NULL, 1000, 1000, 'idem_ovf');")
            },
            expectedMessageFragment = "value_units"
        )

        // 3. Unsupported outcome_kind ('SKIPPED') in habit_day_outcomes
        assertV2MigrationRollsBack(
            setupCorruptV2 = { db ->
                db.execSQL("INSERT INTO habit_day_outcomes VALUES ('o_skip', 'h1', '2026-03-02', 'SKIPPED', NULL, 'idem_skip', 1000, 1000);")
            },
            expectedMessageFragment = "SKIPPED"
        )

        // 4. Corrupt schedule_kind / target_period / mask combination in habit_versions
        assertV2MigrationRollsBack(
            setupCorruptV2 = { db ->
                db.execSQL("INSERT INTO habit_versions VALUES ('v_bad_sched', 'h1', '2026-03-05', 'BadSched', NULL, 'QUANTITY', 'count', 'DAY', 5, 'FLEXIBLE_WEEKLY_TARGET', NULL, NULL, 'ACTIVE', 'OFF', NULL, 2000);")
            },
            expectedMessageFragment = "FLEXIBLE_WEEKLY_TARGET"
        )
    }

    // =========================================================================
    // T1-39: U-11 Legacy v2 Weekly Schedule Conversion & Weekly Check-Off Target >= 1
    // =========================================================================
    @Test
    fun t1_39_u11LegacyV2WeeklySchedulesAndWeeklyCheckOffTargets() {
        val v2WeeklyDbName = "test_u11_weekly_${UUID.randomUUID()}.db"
        try {
            val v2Helper = object : SQLiteOpenHelper(context, v2WeeklyDbName, null, 2) {
                override fun onCreate(db: SQLiteDatabase) {
                    db.execSQL("CREATE TABLE habits (habit_id TEXT NOT NULL PRIMARY KEY, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("CREATE TABLE habit_versions (version_id TEXT NOT NULL PRIMARY KEY, habit_id TEXT NOT NULL, effective_from_local_date TEXT NOT NULL, name TEXT NOT NULL, description TEXT, tracking_mode TEXT NOT NULL, unit_code TEXT, target_period TEXT NOT NULL, target_value INTEGER, schedule_kind TEXT NOT NULL, scheduled_weekdays_mask INTEGER, preferred_weekdays_mask INTEGER, lifecycle_state TEXT NOT NULL, reminder_mode TEXT NOT NULL, reminder_time_local TEXT, created_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("CREATE TABLE habit_entries (entry_id TEXT NOT NULL PRIMARY KEY, habit_id TEXT NOT NULL, local_date TEXT NOT NULL, value_units INTEGER NOT NULL, entry_note TEXT, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL, idempotency_key TEXT NOT NULL UNIQUE);")
                    db.execSQL("CREATE TABLE habit_day_outcomes (outcome_id TEXT NOT NULL PRIMARY KEY, habit_id TEXT NOT NULL, local_date TEXT NOT NULL, outcome_kind TEXT NOT NULL, effort_level TEXT, idempotency_key TEXT NOT NULL UNIQUE, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("CREATE TABLE habit_date_notes (note_id TEXT NOT NULL PRIMARY KEY, habit_id TEXT NOT NULL, local_date TEXT NOT NULL, body TEXT NOT NULL, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("CREATE TABLE journal_entries (journal_entry_id TEXT NOT NULL PRIMARY KEY, local_date TEXT NOT NULL, body TEXT NOT NULL, title TEXT, primary_mood_code TEXT, secondary_mood_code TEXT, prompt_id TEXT, linked_habit_id TEXT, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("CREATE TABLE journal_drafts (draft_id TEXT NOT NULL PRIMARY KEY, local_date TEXT NOT NULL, title TEXT, body TEXT NOT NULL, primary_mood_code TEXT, secondary_mood_code TEXT, prompt_id TEXT, linked_habit_id TEXT, intended_journal_entry_id TEXT, source_context TEXT NOT NULL, created_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("CREATE TABLE app_settings (setting_key TEXT NOT NULL PRIMARY KEY, value_type TEXT NOT NULL, value_text TEXT NOT NULL, schema_version INTEGER NOT NULL DEFAULT 1, updated_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("CREATE TABLE reminder_deliveries (delivery_key TEXT NOT NULL PRIMARY KEY, reminder_type TEXT NOT NULL, habit_id TEXT, target_period_key TEXT NOT NULL, delivery_status TEXT NOT NULL, scheduled_at_utc_ms INTEGER NOT NULL, attempted_at_utc_ms INTEGER, delivered_at_utc_ms INTEGER, platform_notification_tag TEXT, platform_notification_id INTEGER, suppression_reason TEXT);")
                    db.execSQL("CREATE TABLE restore_sessions (restore_session_id TEXT NOT NULL PRIMARY KEY, restore_mode TEXT NOT NULL, format_version INTEGER NOT NULL, source_schema_version INTEGER NOT NULL, session_state TEXT NOT NULL, started_at_utc_ms INTEGER NOT NULL, updated_at_utc_ms INTEGER NOT NULL, manifest_json TEXT NOT NULL, safety_backup_ref TEXT, safety_backup_verified INTEGER NOT NULL DEFAULT 0);")
                    db.execSQL("CREATE TABLE restore_conflicts (conflict_id TEXT NOT NULL PRIMARY KEY, restore_session_id TEXT NOT NULL, record_type TEXT NOT NULL, record_key TEXT NOT NULL, current_payload_json TEXT NOT NULL, backup_payload_json TEXT NOT NULL, created_at_utc_ms INTEGER NOT NULL);")
                    db.execSQL("CREATE TABLE notification_cancel_outbox (cancel_id TEXT NOT NULL PRIMARY KEY, platform_notification_tag TEXT NOT NULL, platform_notification_id INTEGER NOT NULL, created_at_utc_ms INTEGER NOT NULL, last_attempt_at_utc_ms INTEGER, attempt_count INTEGER NOT NULL DEFAULT 0, status TEXT NOT NULL, error_category TEXT);")

                    // Habit 1: Legacy (DAILY, WEEK) with COMPLETION_ONLY and target_value = 3
                    db.execSQL("INSERT INTO habits VALUES ('h_w_daily', 1000, 1000);")
                    db.execSQL("INSERT INTO habit_versions VALUES ('v_w_daily', 'h_w_daily', '2026-03-02', 'Call Family', NULL, 'COMPLETION_ONLY', NULL, 'WEEK', 3, 'DAILY', NULL, NULL, 'ACTIVE', 'OFF', NULL, 1000);")

                    // Habit 2: Legacy (SELECTED_WEEKDAYS, mask=21 Mon/Wed/Fri, WEEK) with EFFORT_AWARE and target_value = 2
                    db.execSQL("INSERT INTO habits VALUES ('h_w_sel', 1000, 1000);")
                    db.execSQL("INSERT INTO habit_versions VALUES ('v_w_sel', 'h_w_sel', '2026-03-02', 'Strength', NULL, 'EFFORT_AWARE', NULL, 'WEEK', 2, 'SELECTED_WEEKDAYS', 21, NULL, 'ACTIVE', 'OFF', NULL, 1000);")
                }
                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            }
            v2Helper.writableDatabase.close()

            val migratedRepo = DayseedRepository(context, clock = testClock, databaseName = v2WeeklyDbName)
            try {
                val vDaily = migratedRepo.getEffectiveHabitVersion("h_w_daily", LocalDate.of(2026, 3, 5))!!
                assertEquals(ScheduleKind.FLEXIBLE_WEEKLY_TARGET, vDaily.scheduleKind)
                assertEquals(TargetPeriod.WEEK, vDaily.targetPeriod)
                assertNull(vDaily.scheduledWeekdaysMask)
                assertNull(vDaily.preferredWeekdaysMask)
                assertEquals(3, vDaily.targetValue)

                val vSel = migratedRepo.getEffectiveHabitVersion("h_w_sel", LocalDate.of(2026, 3, 5))!!
                assertEquals(ScheduleKind.FLEXIBLE_WEEKLY_TARGET, vSel.scheduleKind)
                assertEquals(TargetPeriod.WEEK, vSel.targetPeriod)
                assertNull(vSel.scheduledWeekdaysMask)
                assertEquals(21, vSel.preferredWeekdaysMask)
                assertEquals(2, vSel.targetValue)
            } finally {
                migratedRepo.dbHelper.close()
            }
        } finally {
            context.deleteDatabase(v2WeeklyDbName)
        }

        // Also verify weekly COMPLETION_ONLY habit saved via saveHabitSync preserves targetValue = 3 and requires 3 completions/week
        val monW1 = LocalDate.of(2026, 3, 9) // Mon Mar 9 .. Sun Mar 15
        val weeklyCheckOff = Habit(
            habitId = "h_weekly_checkoff_3",
            name = "Floss 3x/week",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 3,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
        )
        val savedVer = repository.saveHabitSync(weeklyCheckOff, effectiveDateOverride = monW1)
        assertEquals(3, savedVer.targetValue)

        // Log 2 completions in Week Mar 9..15 -> below target (3), so currentCompletedStreak is 0
        repository.logHabitDayOutcomeSync("h_weekly_checkoff_3", LocalDate.of(2026, 3, 9), "COMPLETED", null)
        repository.logHabitDayOutcomeSync("h_weekly_checkoff_3", LocalDate.of(2026, 3, 11), "COMPLETED", null)
        assertEquals(0, repository.habitSummaries.value["h_weekly_checkoff_3"]?.currentCompletedStreak)

        // Log 3rd completion on Mar 13 -> reaches weekly target of 3 -> currentCompletedStreak becomes 1 week!
        repository.logHabitDayOutcomeSync("h_weekly_checkoff_3", LocalDate.of(2026, 3, 13), "COMPLETED", null)
        assertEquals(1, repository.habitSummaries.value["h_weekly_checkoff_3"]?.currentCompletedStreak)
    }

    // =========================================================================
    // T1-40: ARCHIVED/COMPLETED Closures, D-188 Backdated Healing, and Shrinking Below 7 Days
    // =========================================================================
    @Test
    fun t1_40_lifecycleClosuresArchivedCompletedBackdatedHealingAndShrinkingBelow7Days() {
        val start = LocalDate.of(2026, 3, 1)
        // 1. ARCHIVED and COMPLETED lifecycle closures close active runs with matching terminalReason and brokenOnLocalDate = null
        val archiveHabit = Habit(habitId = "h_arch_close", name = "Archive Me", trackingMode = TrackingMode.COMPLETION_ONLY)
        val completeHabit = Habit(habitId = "h_comp_close", name = "Complete Me", trackingMode = TrackingMode.COMPLETION_ONLY)
        repository.saveHabitSync(archiveHabit, effectiveDateOverride = start)
        repository.saveHabitSync(completeHabit, effectiveDateOverride = start)

        for (i in 0..2) {
            val d = start.plusDays(i.toLong())
            testClock.currentLocalDate = d
            repository.logHabitDayOutcomeSync("h_arch_close", d, "COMPLETED", null)
            repository.logHabitDayOutcomeSync("h_comp_close", d, "COMPLETED", null)
        }
        testClock.currentLocalDate = LocalDate.of(2026, 3, 4)
        repository.updateHabitLifecycleSync("h_arch_close", HabitLifecycleState.ARCHIVED)
        repository.updateHabitLifecycleSync("h_comp_close", HabitLifecycleState.COMPLETED)

        val archRun = repository.habitStreakRuns.value.first { it.habitId == "h_arch_close" }
        assertFalse(archRun.isActive)
        assertEquals(GroveTerminalReason.ARCHIVED, archRun.terminalReason)
        assertNull(archRun.brokenOnLocalDate)

        val compRun = repository.habitStreakRuns.value.first { it.habitId == "h_comp_close" }
        assertFalse(compRun.isActive)
        assertEquals(GroveTerminalReason.COMPLETED, compRun.terminalReason)
        assertNull(compRun.brokenOnLocalDate)

        // 2. D-188 Backdated healing & shrinking a qualified run below 7 days
        testClock.currentLocalDate = LocalDate.of(2026, 3, 8)
        testClock.currentUtcMs = 1_773_000_000_000L
        val healHabit = Habit(
            habitId = "h_heal",
            name = "Meditate",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "min",
            targetValue = 10
        )
        repository.saveHabitSync(healHabit, effectiveDateOverride = start)

        // Log Mar 1..3 (3 days) and Mar 5..8 (4 days), leaving Mar 4 missed -> 2 separate runs
        for (d in listOf(1, 2, 3, 5, 6, 7, 8)) {
            repository.logHabitEntrySync("h_heal", LocalDate.of(2026, 3, d), 10)
        }
        val runsBeforeHeal = repository.habitStreakRuns.value.filter { it.habitId == "h_heal" }
        assertEquals(2, runsBeforeHeal.size)
        val firstRunId = runsBeforeHeal.first().runId
        val firstRunCreatedAt = runsBeforeHeal.first().createdAtUtcMs
        assertFalse(runsBeforeHeal.first().qualifiedForGrove)

        // Backdate an entry on Mar 4 -> heals the two runs into a single 8-day run preserving firstRunId and firstRunCreatedAt!
        testClock.advanceMillis(5_000L)
        val healTimeMs = testClock.nowUtcMs()
        val mar4Entry = repository.logHabitEntrySync("h_heal", LocalDate.of(2026, 3, 4), 10)
        val runsAfterHeal = repository.habitStreakRuns.value.filter { it.habitId == "h_heal" }
        assertEquals(1, runsAfterHeal.size)
        val healedRun = runsAfterHeal.first()
        assertEquals(firstRunId, healedRun.runId)
        assertEquals(firstRunCreatedAt, healedRun.createdAtUtcMs)
        assertEquals(8, healedRun.completedStreakCount)
        assertTrue(healedRun.qualifiedForGrove)
        assertEquals(healTimeMs, healedRun.qualifiedAtUtcMs)
        assertEquals(GroveTreeState.GROWING_TREE, healedRun.treeState)

        // Extend run on Mar 9 -> runId and qualifiedAtUtcMs remain stable!
        testClock.currentLocalDate = LocalDate.of(2026, 3, 9)
        testClock.advanceMillis(10_000L)
        val mar9Entry = repository.logHabitEntrySync("h_heal", LocalDate.of(2026, 3, 9), 10)
        val extendedRun = repository.habitStreakRuns.value.first { it.habitId == "h_heal" }
        assertEquals(firstRunId, extendedRun.runId)
        assertEquals(healTimeMs, extendedRun.qualifiedAtUtcMs)
        assertEquals(9, extendedRun.completedStreakCount)

        // Now delete Mar 4 and Mar 9 entries so the run starting on Mar 1 shrinks to 3 days (< 7 days):
        // Verify qualifiedForGrove becomes false and qualifiedAtUtcMs becomes null without violating SQLite CHECK constraint!
        repository.deleteHabitEntrySync(mar9Entry.first.entryId)
        repository.deleteHabitEntrySync(mar4Entry.first.entryId)
        val shrunkFirstRun = repository.habitStreakRuns.value.first { it.habitId == "h_heal" && it.startLocalDate == "2026-03-01" }
        assertEquals(firstRunId, shrunkFirstRun.runId)
        assertFalse(shrunkFirstRun.qualifiedForGrove)
        assertNull(shrunkFirstRun.qualifiedAtUtcMs)
        assertEquals(GroveTreeState.UNQUALIFIED_HISTORY, shrunkFirstRun.treeState)
    }

    // =========================================================================
    // T1-41: Complete v3 Portable Backup REPLACE Round-Trip & Legacy v2 Baseline Seeding
    // =========================================================================
    @Test
    fun t1_41_v3BackupReplaceAndMergeRoundTripWithLegacyV2BaselineSeeding() {
        val start = LocalDate.of(2026, 3, 1)
        testClock.currentLocalDate = LocalDate.of(2026, 3, 8)
        val habit = Habit(
            habitId = "h_backup_full",
            name = "Botanical Walk",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "km",
            targetValue = 5,
            category = "Outdoors",
            lifetimeGoalDays = 90
        )
        repository.saveHabitSync(habit, effectiveDateOverride = start)
        for (i in 0..6) {
            repository.logHabitEntrySync("h_backup_full", start.plusDays(i.toLong()), 5)
        }
        repository.setRestDayExceptionSync("h_backup_full", LocalDate.of(2026, 3, 8), "Rainy rest day")
        repository.setWeekStartSync(DayOfWeek.WEDNESDAY)

        val originalRun = repository.habitStreakRuns.value.first { it.habitId == "h_backup_full" }
        assertTrue(originalRun.qualifiedForGrove)
        assertNotNull(originalRun.qualifiedAtUtcMs)

        // Advance clock and export v3 backup
        val exportedJson = repository.exportPortableBackupSync(password = null)
        testClock.advanceMillis(60_000L)

        // Wipe local data and restore via REPLACE
        repository.deleteAllDataSync()
        assertTrue(repository.habits.value.isEmpty())

        val replaceSession = repository.restorePortableBackupSync(exportedJson, restoreMode = "REPLACE")
        assertEquals("COMPLETED", replaceSession.sessionState)

        val restoredHabit = repository.habits.value.first { it.habitId == "h_backup_full" }
        assertEquals("Outdoors", restoredHabit.category)
        assertEquals(90, restoredHabit.lifetimeGoalDays)

        val restoredRun = repository.habitStreakRuns.value.first { it.habitId == "h_backup_full" }
        assertEquals(originalRun.runId, restoredRun.runId)
        assertEquals(originalRun.createdAtUtcMs, restoredRun.createdAtUtcMs)
        assertEquals(originalRun.qualifiedAtUtcMs, restoredRun.qualifiedAtUtcMs)
        assertEquals(originalRun.treeState, restoredRun.treeState)

        val restoredEx = repository.habitDateExceptions.value.first { it.habitId == "h_backup_full" }
        assertEquals("2026-03-08", restoredEx.localDate)
        assertEquals("Rainy rest day", restoredEx.note)
        assertTrue(repository.weekStartHistory.value.any { it.effectiveFromLocalDate == "2026-03-11" && it.weekStartIsoDay == 3 })

        // Verify legacy v2 backup REPLACE seeds week_start_history at earliest restored local date using restored week_start = SUNDAY
        val v2Root = JSONObject(exportedJson).apply {
            put("source_schema_version", 2)
            remove("week_start_history")
            remove("habit_streak_runs")
            remove("habit_date_exceptions")
            put("app_settings", JSONArray().apply {
                put(JSONObject().apply {
                    put("setting_key", "week_start")
                    put("value_type", "ENUM")
                    put("value_text", "SUNDAY")
                    put("schema_version", 1)
                    put("updated_at_utc_ms", 1000L)
                })
            })
        }
        repository.restorePortableBackupSync(v2Root.toString(), restoreMode = "REPLACE")
        assertEquals("SUNDAY", repository.getSettingString("week_start", "MONDAY"))
        val seededBaseline = repository.weekStartHistory.value.first()
        assertEquals("2026-03-01", seededBaseline.effectiveFromLocalDate)
        assertEquals(7, seededBaseline.weekStartIsoDay)
        assertTrue(seededBaseline.isLegacyBaseline)
    }

    // =========================================================================
    // T1-42: MERGE Conflict Creation & Resolution + Per-Conflict Destructive Authorization
    // =========================================================================
    @Test
    fun t1_42_v3MergeConflictsAndPerConflictDestructiveAuthorization() {
        val start = LocalDate.of(2026, 3, 1)
        testClock.currentLocalDate = LocalDate.of(2026, 3, 10)
        val habit = Habit(
            habitId = "h_merge_test",
            name = "Sketching",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetValue = 2,
            category = "Art",
            lifetimeGoalDays = 50
        )
        repository.saveHabitSync(habit, effectiveDateOverride = start)
        for (i in 0..6) {
            repository.logHabitEntrySync("h_merge_test", start.plusDays(i.toLong()), 2)
        }
        // Log activity on Mar 9 and Mar 10 so backup REST_DAY exceptions on both dates will collide with local recorded activity!
        repository.logHabitEntrySync("h_merge_test", LocalDate.of(2026, 3, 9), 2)
        repository.logHabitEntrySync("h_merge_test", LocalDate.of(2026, 3, 10), 2)
        repository.setWeekStartSync(DayOfWeek.WEDNESDAY) // creates week_start_history row for 2026-03-18 = 3

        val localRun = repository.habitStreakRuns.value.first { it.habitId == "h_merge_test" }

        // Build a backup JSON that differs in:
        // 1. habit (category = "Creative", lifetime_goal_days = 120)
        // 2. habit_date_exceptions on 2026-03-09 and 2026-03-10 (where local has recorded activity!)
        // 3. habit_streak_run for (h_merge_test, 2026-03-01) with different run_id, created_at_utc_ms, and qualified_at_utc_ms
        // 4. week_start_history for 2026-03-18 with week_start_iso_day = 7 (Sunday)
        val backupRoot = JSONObject(repository.exportPortableBackupSync(password = null))
        backupRoot.getJSONArray("habits").getJSONObject(0).apply {
            put("category", "Creative")
            put("lifetime_goal_days", 120)
        }
        backupRoot.put("habit_date_exceptions", JSONArray().apply {
            put(JSONObject().apply {
                put("id", "ex_mar9")
                put("habit_id", "h_merge_test")
                put("local_date", "2026-03-09")
                put("exception_kind", "REST_DAY")
                put("note", "Backup rest Mar 9")
                put("created_at_utc_ms", 5000L)
                put("updated_at_utc_ms", 5000L)
            })
            put(JSONObject().apply {
                put("id", "ex_mar10")
                put("habit_id", "h_merge_test")
                put("local_date", "2026-03-10")
                put("exception_kind", "REST_DAY")
                put("note", "Backup rest Mar 10")
                put("created_at_utc_ms", 5000L)
                put("updated_at_utc_ms", 5000L)
            })
        })
        backupRoot.getJSONArray("habit_streak_runs").getJSONObject(0).apply {
            put("run_id", "run_from_backup_999")
            put("created_at_utc_ms", 123_456L)
            put("qualified_at_utc_ms", 234_567L)
        }
        val whArr = backupRoot.getJSONArray("week_start_history")
        for (i in 0 until whArr.length()) {
            val wObj = whArr.getJSONObject(i)
            if (wObj.getString("effective_from_local_date") == "2026-03-18") {
                wObj.put("week_start_iso_day", 7)
            }
        }

        val mergeSession = repository.restorePortableBackupSync(backupRoot.toString(), restoreMode = "MERGE")
        assertEquals("AWAITING_CONFLICT_RESOLUTION", mergeSession.sessionState)

        val conflicts = repository.restoreConflicts.value
        val habitConflict = conflicts.first { it.recordType == "habit" && it.recordKey == "h_merge_test" }
        val exMar9Conflict = conflicts.first { it.recordType == "habit_date_exception" && it.recordKey == "h_merge_test:2026-03-09" }
        val exMar10Conflict = conflicts.first { it.recordType == "habit_date_exception" && it.recordKey == "h_merge_test:2026-03-10" }
        val runConflict = conflicts.first { it.recordType == "habit_streak_run" && it.recordKey == "h_merge_test:2026-03-01" }
        val weekConflict = conflicts.first { it.recordType == "week_start_history" && it.recordKey == "2026-03-18" }

        // Verify KEEP_BOTH is disallowed for all four non-journal record types
        for (c in listOf(habitConflict, exMar9Conflict, runConflict, weekConflict)) {
            assertFalse(c.allowedResolutions().contains(ConflictResolution.KEEP_BOTH))
            try {
                repository.resolveRestoreConflictSync(c.conflictId, ConflictResolution.KEEP_BOTH)
                fail("Expected IllegalArgumentException for KEEP_BOTH on ${c.recordType}")
            } catch (_: IllegalArgumentException) {
                // Expected
            }
        }

        // 1. Resolve habitConflict with USE_BACKUP -> updates category and lifetimeGoalDays
        repository.resolveRestoreConflictSync(habitConflict.conflictId, ConflictResolution.USE_BACKUP)
        val updatedHabit = repository.habits.value.first { it.habitId == "h_merge_test" }
        assertEquals("Creative", updatedHabit.category)
        assertEquals(120, updatedHabit.lifetimeGoalDays)

        // 2. Attempting USE_BACKUP on exMar9Conflict WITHOUT destructive authorization must fail and roll back!
        try {
            repository.resolveRestoreConflictSync(exMar9Conflict.conflictId, ConflictResolution.USE_BACKUP)
            fail("Expected IllegalStateException when resolving habit_date_exception with USE_BACKUP without destructive authorization")
        } catch (_: IllegalStateException) {
            // Expected
        }
        // Local entry on Mar 9, unresolved conflict row, and AWAITING_CONFLICT_RESOLUTION state remain intact
        assertEquals(1, repository.getEntriesForHabitOnDate("h_merge_test", LocalDate.of(2026, 3, 9)).size)
        assertTrue(repository.restoreConflicts.value.any { it.conflictId == exMar9Conflict.conflictId })

        // 3. Attempting USE_BACKUP on exMar10Conflict using exMar9Conflict's authorization ID must ALSO fail!
        try {
            repository.resolveRestoreConflictSync(
                conflictId = exMar10Conflict.conflictId,
                resolution = ConflictResolution.USE_BACKUP,
                allowDestructiveActivityRemoval = true,
                authorizedDestructiveConflictId = exMar9Conflict.conflictId
            )
            fail("Expected per-conflict authorization check to reject mismatched conflictId")
        } catch (_: IllegalStateException) {
            // Expected
        }
        assertEquals(1, repository.getEntriesForHabitOnDate("h_merge_test", LocalDate.of(2026, 3, 10)).size)

        // 4. Resolve exMar9Conflict with explicit per-conflict authorization -> removes Mar 9 entry and inserts Mar 9 REST_DAY, while Mar 10 entry remains untouched!
        repository.resolveRestoreConflictSync(
            conflictId = exMar9Conflict.conflictId,
            resolution = ConflictResolution.USE_BACKUP,
            allowDestructiveActivityRemoval = true,
            authorizedDestructiveConflictId = exMar9Conflict.conflictId
        )
        assertTrue(repository.getEntriesForHabitOnDate("h_merge_test", LocalDate.of(2026, 3, 9)).isEmpty())
        assertEquals("Backup rest Mar 9", repository.getExceptionForHabitOnDate("h_merge_test", LocalDate.of(2026, 3, 9))?.note)
        assertEquals(1, repository.getEntriesForHabitOnDate("h_merge_test", LocalDate.of(2026, 3, 10)).size)

        // Resolve exMar10Conflict with KEEP_CURRENT -> preserves Mar 10 entry
        repository.resolveRestoreConflictSync(exMar10Conflict.conflictId, ConflictResolution.KEEP_CURRENT)
        assertEquals(1, repository.getEntriesForHabitOnDate("h_merge_test", LocalDate.of(2026, 3, 10)).size)
        assertNull(repository.getExceptionForHabitOnDate("h_merge_test", LocalDate.of(2026, 3, 10)))

        // 5. Resolve runConflict with USE_BACKUP -> preserves backup run_id, created_at_utc_ms, and qualified_at_utc_ms across recalculation!
        assertNotEquals("run_from_backup_999", localRun.runId)
        repository.resolveRestoreConflictSync(runConflict.conflictId, ConflictResolution.USE_BACKUP)
        val resolvedRun = repository.habitStreakRuns.value.first { it.habitId == "h_merge_test" && it.startLocalDate == "2026-03-01" }
        assertEquals("run_from_backup_999", resolvedRun.runId)
        assertEquals(123_456L, resolvedRun.createdAtUtcMs)
        assertEquals(234_567L, resolvedRun.qualifiedAtUtcMs)

        // 6. Resolve weekConflict with USE_BACKUP -> updates 2026-03-18 policy to Sunday (7) and completes session
        repository.resolveRestoreConflictSync(weekConflict.conflictId, ConflictResolution.USE_BACKUP)
        assertEquals(7, repository.weekStartHistory.value.first { it.effectiveFromLocalDate == "2026-03-18" }.weekStartIsoDay)
        assertTrue(repository.restoreConflicts.value.isEmpty())
        repository.dbHelper.readableDatabase.rawQuery(
            "SELECT session_state FROM restore_sessions WHERE restore_session_id = ?",
            arrayOf(mergeSession.restoreSessionId)
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("COMPLETED", c.getString(0))
        }
    }

    // =========================================================================
    // T1-43: MERGE rejects replacement of an unrelated active streak run
    // =========================================================================
    @Test
    fun t1_43_mergeDoesNotDeleteUnrelatedActiveStreakRun() {
        val start = LocalDate.of(2026, 3, 1)
        testClock.currentLocalDate = LocalDate.of(2026, 3, 10)
        repository.saveHabitSync(
            Habit(
                habitId = "h_merge_active_guard",
                name = "Daily walk",
                trackingMode = TrackingMode.QUANTITY,
                unitCode = "km",
                targetValue = 1
            ),
            effectiveDateOverride = start
        )
        for (i in 0..9) {
            repository.logHabitEntrySync("h_merge_active_guard", start.plusDays(i.toLong()), 1)
        }

        val activeBefore = repository.dbHelper.readableDatabase.rawQuery(
            "SELECT run_id, start_local_date FROM habit_streak_runs WHERE habit_id = ? AND is_active = 1",
            arrayOf("h_merge_active_guard")
        ).use { c ->
            assertTrue(c.moveToFirst())
            c.getString(0) to c.getString(1)
        }

        val backupRun = JSONObject(
            JSONObject(repository.exportPortableBackupSync(password = null))
                .getJSONArray("habit_streak_runs")
                .getJSONObject(0)
                .toString()
        ).apply {
            put("run_id", "run_backup_different_start")
            put("start_local_date", "2026-02-28")
            put("end_local_date", "2026-03-10")
            put("is_active", 1)
            put("terminal_reason", JSONObject.NULL)
            put("qualified_for_grove", 0)
            put("qualified_at_utc_ms", JSONObject.NULL)
            put("broken_on_local_date", JSONObject.NULL)
            put("tree_state", "SAPLING_ACTIVE")
        }

        val db = repository.dbHelper.writableDatabase
        val now = 1_777_777_000_000L
        val sessionId = "rs_active_guard"
        val conflictId = "rc_active_guard"
        db.insertOrThrow(
            DayseedDatabaseHelper.TABLE_RESTORE_SESSIONS,
            null,
            ContentValues().apply {
                put("restore_session_id", sessionId)
                put("restore_mode", "MERGE")
                put("format_version", 1)
                put("source_schema_version", 3)
                put("session_state", "AWAITING_CONFLICT_RESOLUTION")
                put("started_at_utc_ms", now)
                put("updated_at_utc_ms", now)
                put("manifest_json", "{}")
                putNull("safety_backup_ref")
                put("safety_backup_verified", 1)
            }
        )
        db.insertOrThrow(
            DayseedDatabaseHelper.TABLE_RESTORE_CONFLICTS,
            null,
            ContentValues().apply {
                put("conflict_id", conflictId)
                put("restore_session_id", sessionId)
                put("record_type", "habit_streak_run")
                put("record_key", "h_merge_active_guard:2026-02-28")
                put("current_payload_json", "{}")
                put("backup_payload_json", backupRun.toString())
                put("created_at_utc_ms", now)
            }
        )
        repository.refreshAllSync()

        try {
            repository.resolveRestoreConflictSync(conflictId, ConflictResolution.USE_BACKUP)
            fail("Expected USE_BACKUP to reject replacement of a different active streak run")
        } catch (_: IllegalStateException) {
            // Expected: the active run is unrelated and must remain untouched.
        }

        db.rawQuery(
            "SELECT run_id, start_local_date FROM habit_streak_runs WHERE habit_id = ? AND is_active = 1",
            arrayOf("h_merge_active_guard")
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(activeBefore.first, c.getString(0))
            assertEquals(activeBefore.second, c.getString(1))
        }
        db.rawQuery(
            "SELECT COUNT(*) FROM restore_conflicts WHERE conflict_id = ?",
            arrayOf(conflictId)
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }
    }

    // =========================================================================
    // T1-44: REPLACE fails closed and rolls back orphaned streak-run records
    // =========================================================================
    @Test
    fun t1_44_replaceRejectsOrphanedStreakRunAndPreservesLocalData() {
        val start = LocalDate.of(2026, 3, 1)
        repository.saveHabitSync(
            Habit(
                habitId = "h_replace_orphan_guard",
                name = "Read",
                trackingMode = TrackingMode.QUANTITY,
                unitCode = "pages",
                targetValue = 2
            ),
            effectiveDateOverride = start
        )
        for (i in 0..6) {
            repository.logHabitEntrySync("h_replace_orphan_guard", start.plusDays(i.toLong()), 2)
        }

        val beforeRunCount = repository.habitStreakRuns.value.count { it.habitId == "h_replace_orphan_guard" }
        assertTrue(beforeRunCount > 0)
        val backupRoot = JSONObject(repository.exportPortableBackupSync(password = null))
        val orphanRun = JSONObject(
            backupRoot.getJSONArray("habit_streak_runs").getJSONObject(0).toString()
        ).apply {
            put("run_id", "run_with_missing_parent")
            put("habit_id", "missing_parent_habit")
        }
        backupRoot.getJSONArray("habit_streak_runs").put(orphanRun)

        try {
            repository.restorePortableBackupSync(backupRoot.toString(), restoreMode = "REPLACE")
            fail("Expected REPLACE to reject a streak run whose parent habit is missing")
        } catch (_: IllegalStateException) {
            // Expected: transaction must roll back instead of silently skipping the run.
        }

        assertTrue(repository.habits.value.any { it.habitId == "h_replace_orphan_guard" })
        assertEquals(
            beforeRunCount,
            repository.habitStreakRuns.value.count { it.habitId == "h_replace_orphan_guard" }
        )
        assertFalse(repository.habits.value.any { it.habitId == "missing_parent_habit" })
    }

}
