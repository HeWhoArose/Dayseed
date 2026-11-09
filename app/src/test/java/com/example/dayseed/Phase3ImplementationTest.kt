package com.example.dayseed

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.CompactHistoryBucket
import com.example.dayseed.data.DayRecordState
import com.example.dayseed.data.DayseedClock
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.DurationNormalizer
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.HonestCalendarDateState
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.data.ProgressBand
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.WeekdayMask
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigInteger
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase3ImplementationTest {

    private class MutableTestClock(
        var currentLocalDate: LocalDate = LocalDate.of(2026, 10, 14),
        var currentLocalTime: LocalTime = LocalTime.of(10, 30),
        var currentUtcMs: Long = 1_791_973_800_000L,
        private val zone: ZoneId = ZoneId.of("UTC")
    ) : DayseedClock {
        override fun nowUtcMs(): Long = currentUtcMs
        override fun todayLocal(): LocalDate = currentLocalDate
        override fun nowLocalTime(): LocalTime = currentLocalTime
        override fun zoneId(): ZoneId = zone
    }

    private lateinit var app: Application
    private lateinit var testClock: MutableTestClock
    private lateinit var dbName: String
    private lateinit var repository: DayseedRepository
    private lateinit var viewModel: DayseedViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        testClock = MutableTestClock()
        dbName = "dayseed_phase3_test_${UUID.randomUUID()}.db"
        repository = DayseedRepository(
            context = app,
            clock = testClock,
            databaseName = dbName
        )
        viewModel = DayseedViewModel(app, repository)
    }

    @After
    fun tearDown() {
        repository.onAfterDraftSavePreparedHook = null
        repository.injectedJournalSaveFailure = null
        repository.dbHelper.close()
        app.deleteDatabase(dbName)
    }

    // =========================================================================
    // 4.1 Journal & Draft Lifecycle, Race-Free Two-Phase Promotion & Cursor Exclusion
    // =========================================================================

    @Test
    fun p3_01_newlyAutosavedDraftHasNullIntendedIdUntilSavePreparedAndTwoPhasePromotionIsIdempotent() {
        val today = testClock.todayLocal()
        val draft = repository.saveJournalDraftSync(
            draftId = "draft_oct_14",
            localDate = today,
            title = "Morning Dew",
            body = "First version saved by Caller 1.",
            primaryMoodCode = "CALM",
            secondaryMoodCode = "HOPEFUL"
        )
        // Newly created/autosaved drafts must NOT require a non-null intended_journal_entry_id
        assertNull(draft.intendedJournalEntryId)

        // Save cursor selection for this draft
        repository.saveJournalDraftCursorSync("draft_oct_14", 5, 12)
        assertEquals(5 to 12, repository.getJournalDraftCursorSync("draft_oct_14"))

        // Simulate a concurrent/interleaved Caller 2 that runs right after Caller 1 completes Phase 1
        var interleavedResult: JournalEntry? = null
        var hookFired = false
        repository.onAfterDraftSavePreparedHook = { draftId, preparedEntryId ->
            if (!hookFired) {
                hookFired = true
                assertNotNull(preparedEntryId)
                // Caller 1 completes promotion first inside the hook, then a stale/concurrent draft mutation
                // is attempted before Caller 2 finishes:
                interleavedResult = repository.promoteDraftToJournalEntrySync(draftId)
                testClock.currentUtcMs += 5_000L
            }
        }

        val caller1Entry = repository.promoteDraftToJournalEntrySync("draft_oct_14")
        assertNotNull(caller1Entry)
        assertNotNull(interleavedResult)

        // Both callers must converge on the exact same journalEntryId and preserve the first saved content/timestamps
        assertEquals(interleavedResult!!.journalEntryId, caller1Entry!!.journalEntryId)
        assertEquals("First version saved by Caller 1.", caller1Entry.body)
        assertEquals(interleavedResult!!.createdAtUtcMs, caller1Entry.createdAtUtcMs)
        assertEquals(interleavedResult!!.updatedAtUtcMs, caller1Entry.updatedAtUtcMs)

        // Exactly 1 saved journal entry, 0 remaining drafts, and cursor setting cleaned up
        assertEquals(1, repository.journalEntries.value.size)
        assertEquals(0, repository.journalDrafts.value.size)
        assertNull(repository.getJournalDraftCursorSync("draft_oct_14"))

        // Calling promoteDraftToJournalEntrySync a third time after draft removal still returns the existing entry idempotently
        val repeatCall = repository.promoteDraftToJournalEntrySync("draft_oct_14")
        assertNotNull(repeatCall)
        assertEquals(caller1Entry.journalEntryId, repeatCall!!.journalEntryId)
        assertEquals(1, repository.journalEntries.value.size)
    }

    @Test
    fun p3_02_failedJournalSavePreservesDraftAndIntendedEntryIdAndRetryReusesSameId() {
        val today = testClock.todayLocal()
        viewModel.openJournalEditor(entry = null, forDate = today, draftId = "draft_fail_retry")
        val initialDraft = repository.saveJournalDraftSync(
            draftId = "draft_fail_retry",
            localDate = today,
            title = "Resilient Draft",
            body = "Must survive a failed save without losing text or intended ID.",
            primaryMoodCode = "FOCUSED"
        )
        assertNull(initialDraft.intendedJournalEntryId)

        // Inject failure after Phase 1 save preparation
        repository.injectedJournalSaveFailure = {
            IllegalStateException("Simulated disk/SQLite write failure")
        }

        val candidateEntry = JournalEntry(
            journalEntryId = UUID.randomUUID().toString(),
            localDate = today.toString(),
            title = "Resilient Draft",
            body = "Must survive a failed save without losing text or intended ID.",
            primaryMoodCode = "FOCUSED"
        )
        val saveSucceeded = viewModel.saveJournalEntry(candidateEntry, sourceDraftId = "draft_fail_retry")
        assertFalse(saveSucceeded)
        // Editor must stay open, error banner state must be populated, no false success message
        assertTrue(viewModel.isJournalEditorOpen.value)
        assertNotNull(viewModel.lastJournalSaveError.value)
        assertNull(viewModel.userMessage.value)

        // Draft must still exist in SQLite and must now have a non-null durable intendedJournalEntryId from Phase 1
        assertEquals(0, repository.journalEntries.value.size)
        assertEquals(1, repository.journalDrafts.value.size)
        val preservedDraft = repository.journalDrafts.value.single()
        val preparedIdFromFailedAttempt = preservedDraft.intendedJournalEntryId
        assertNotNull(preparedIdFromFailedAttempt)
        assertEquals("Must survive a failed save without losing text or intended ID.", preservedDraft.body)

        // Clear failure and retry Save: must reuse the exact same intendedJournalEntryId
        repository.injectedJournalSaveFailure = null
        val retrySucceeded = viewModel.saveJournalEntry(candidateEntry, sourceDraftId = "draft_fail_retry")
        assertTrue(retrySucceeded)
        assertFalse(viewModel.isJournalEditorOpen.value)
        assertNull(viewModel.lastJournalSaveError.value)
        assertEquals(0, repository.journalDrafts.value.size)
        assertEquals(1, repository.journalEntries.value.size)
        val savedEntry = repository.journalEntries.value.single()
        assertEquals(preparedIdFromFailedAttempt, savedEntry.journalEntryId)
    }

    @Test
    fun p3_03_multipleJournalEntriesOnSameDateAndEditingEntryDoesNotMutateSiblingEntriesOrUnrelatedDrafts() {
        val today = testClock.todayLocal()
        val entry1 = repository.saveJournalEntrySync(
            JournalEntry(
                journalEntryId = "j_same_1",
                localDate = today.toString(),
                title = "Morning Note",
                body = "Walked in the garden.",
                primaryMoodCode = "CALM",
                createdAtUtcMs = 1_000L,
                updatedAtUtcMs = 1_000L
            )
        )
        val entry2 = repository.saveJournalEntrySync(
            JournalEntry(
                journalEntryId = "j_same_2",
                localDate = today.toString(),
                title = "Evening Note",
                body = "Read two chapters.",
                primaryMoodCode = "CONTENT",
                createdAtUtcMs = 2_000L,
                updatedAtUtcMs = 2_000L
            )
        )
        val unrelatedDraft = repository.saveJournalDraftSync(
            draftId = "comeback_draft_$today",
            localDate = today,
            title = "Comeback Draft",
            body = "Still drafting comeback thoughts.",
            sourceContext = "COMEBACK_30D"
        )

        // Edit entry1's body and move its date back 1 day
        testClock.currentUtcMs = 5_000L
        repository.saveJournalEntrySync(
            entry1.copy(
                localDate = today.minusDays(1).toString(),
                body = "Walked in the garden at sunrise."
            )
        )

        val reloaded1 = repository.journalEntries.value.first { it.journalEntryId == "j_same_1" }
        val reloaded2 = repository.journalEntries.value.first { it.journalEntryId == "j_same_2" }
        assertEquals(today.minusDays(1).toString(), reloaded1.localDate)
        assertEquals(1_000L, reloaded1.createdAtUtcMs)
        assertEquals(5_000L, reloaded1.updatedAtUtcMs)

        // Entry 2 and unrelated draft must be completely untouched
        assertEquals(today.toString(), reloaded2.localDate)
        assertEquals("Read two chapters.", reloaded2.body)
        assertEquals(1, repository.journalDrafts.value.size)
        assertEquals(unrelatedDraft.draftId, repository.journalDrafts.value.single().draftId)
    }

    @Test
    fun p3_04_journalDraftCursorPersistedRestoredAndExcludedFromPortableBackupExportAndRestore() {
        val today = testClock.todayLocal()
        repository.saveJournalDraftSync(
            draftId = "draft_cursor_test",
            localDate = today,
            body = "Testing cursor persistence and backup exclusion."
        )
        repository.saveJournalDraftCursorSync("draft_cursor_test", 8, 15)
        assertEquals(8 to 15, repository.getJournalDraftCursorSync("draft_cursor_test"))

        val backupJsonStr = repository.exportPortableBackupSync()
        val root = JSONObject(backupJsonStr)
        val settingsArr = root.getJSONArray("app_settings")
        for (i in 0 until settingsArr.length()) {
            val key = settingsArr.getJSONObject(i).getString("setting_key")
            assertFalse("Cursor setting must not be exported in portable backup", key.startsWith("journal_draft_cursor_"))
        }

        // Inject a synthetic journal_draft_cursor_ row into the backup JSON and test REPLACE and MERGE restore
        settingsArr.put(
            JSONObject().apply {
                put("setting_key", "journal_draft_cursor_injected")
                put("value_type", "STRING")
                put("value_text", "3:9")
                put("schema_version", 1)
                put("updated_at_utc_ms", testClock.nowUtcMs())
            }
        )
        repository.restorePortableBackupSync(root.toString(), restoreMode = "REPLACE")
        assertNull(repository.getJournalDraftCursorSync("injected"))

        repository.restorePortableBackupSync(root.toString(), restoreMode = "MERGE")
        assertNull(repository.getJournalDraftCursorSync("injected"))
    }

    @Test
    fun p3_05_journalPermanentDeleteAndDraftDiscardDoNotCreateUndoTokens() {
        val today = testClock.todayLocal()
        repository.saveJournalDraftSync(
            draftId = "draft_to_discard",
            localDate = today,
            body = "Temporary draft"
        )
        viewModel.discardJournalDraft("draft_to_discard")
        assertEquals(0, repository.journalDrafts.value.size)
        assertNull(repository.activeUndoToken.value)

        val saved = repository.saveJournalEntrySync(
            JournalEntry(
                journalEntryId = "j_to_delete",
                localDate = today.toString(),
                body = "Permanent delete check"
            )
        )
        viewModel.deleteJournalEntry(saved.journalEntryId)
        assertEquals(0, repository.journalEntries.value.size)
        assertNull(repository.activeUndoToken.value)
    }

    // =========================================================================
    // 4.2 Shared Calendar, Day Detail, Weekly Board & Historical Visibility
    // =========================================================================

    @Test
    fun p3_06_honestCalendarDateStatesCoverAllTenStatesAndHistoricalVisibilityPreservedAfterPauseOrArchive() {
        val today = LocalDate.of(2026, 10, 14) // Wednesday
        testClock.currentLocalDate = today

        // Daily quantity habit active from 2026-10-01
        val dailyHabit = Habit(
            habitId = "h_daily_cal",
            name = "Reading",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20,
            scheduleKind = ScheduleKind.SELECTED_WEEKDAYS,
            scheduledWeekdaysMask = WeekdayMask.toMask(
                setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
            )
        )
        repository.saveHabitSync(dailyHabit, effectiveDateOverride = LocalDate.of(2026, 10, 1))

        // 1. COMPLETED_TARGET_MET on 2026-10-05 (Monday)
        repository.logHabitEntrySync("h_daily_cal", LocalDate.of(2026, 10, 5), 20)
        // 2. PARTIAL_OR_TINY_PROGRESS on 2026-10-06 (Tuesday)
        repository.logHabitEntrySync("h_daily_cal", LocalDate.of(2026, 10, 6), 5)
        // 3. REST_DAY_PLANNED_PAUSE on 2026-10-07 (Wednesday, logged on that date per U-1)
        testClock.currentLocalDate = LocalDate.of(2026, 10, 7)
        repository.setRestDayExceptionSync("h_daily_cal", LocalDate.of(2026, 10, 7), "Travelling")
        testClock.currentLocalDate = today
        // 4. EXPLICIT_ZERO_NOT_DONE on 2026-10-08 (Thursday)
        repository.logHabitEntrySync("h_daily_cal", LocalDate.of(2026, 10, 8), 0)
        // 5. MISSED_SCHEDULED_DATE on 2026-10-09 (Friday - unlogged past scheduled weekday)
        // 6. UNSCHEDULED_DATE on 2026-10-10 (Saturday - weekend not in scheduledWeekdaysMask)
        // 7. FUTURE_DATE on 2026-10-16 (Friday > today)

        assertEquals(
            HonestCalendarDateState.COMPLETED_TARGET_MET,
            repository.evaluateHonestCalendarDateState("h_daily_cal", LocalDate.of(2026, 10, 5), today)
        )
        assertEquals(
            HonestCalendarDateState.PARTIAL_OR_TINY_PROGRESS,
            repository.evaluateHonestCalendarDateState("h_daily_cal", LocalDate.of(2026, 10, 6), today)
        )
        assertEquals(
            HonestCalendarDateState.REST_DAY_PLANNED_PAUSE,
            repository.evaluateHonestCalendarDateState("h_daily_cal", LocalDate.of(2026, 10, 7), today)
        )
        assertEquals(
            HonestCalendarDateState.EXPLICIT_ZERO_NOT_DONE,
            repository.evaluateHonestCalendarDateState("h_daily_cal", LocalDate.of(2026, 10, 8), today)
        )
        assertEquals(
            HonestCalendarDateState.MISSED_SCHEDULED_DATE,
            repository.evaluateHonestCalendarDateState("h_daily_cal", LocalDate.of(2026, 10, 9), today)
        )
        assertEquals(
            HonestCalendarDateState.UNSCHEDULED_DATE,
            repository.evaluateHonestCalendarDateState("h_daily_cal", LocalDate.of(2026, 10, 10), today)
        )
        assertEquals(
            HonestCalendarDateState.FUTURE_DATE,
            repository.evaluateHonestCalendarDateState("h_daily_cal", LocalDate.of(2026, 10, 16), today)
        )

        // 8. FLEXIBLE_WEEKLY_IN_PROGRESS for a TargetPeriod.WEEK habit in the current open week
        val weeklyHabit = Habit(
            habitId = "h_weekly_cal",
            name = "Swim",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 3,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
        )
        repository.saveHabitSync(weeklyHabit, effectiveDateOverride = LocalDate.of(2026, 10, 1))
        assertEquals(
            HonestCalendarDateState.FLEXIBLE_WEEKLY_IN_PROGRESS,
            repository.evaluateHonestCalendarDateState("h_weekly_cal", LocalDate.of(2026, 10, 13), today)
        )

        // 9. PAUSED_DATE_RANGE & Historical Visibility when habit is later paused/archived
        testClock.currentLocalDate = LocalDate.of(2026, 10, 12)
        repository.saveHabitSync(
            dailyHabit.copy(lifecycleState = HabitLifecycleState.PAUSED)
        )
        testClock.currentLocalDate = today
        assertEquals(
            HonestCalendarDateState.PAUSED_DATE_RANGE,
            repository.evaluateHonestCalendarDateState("h_daily_cal", LocalDate.of(2026, 10, 13), today)
        )

        // Advance clock to November 2026 and archive the habit
        testClock.currentLocalDate = LocalDate.of(2026, 11, 1)
        repository.saveHabitSync(
            dailyHabit.copy(lifecycleState = HabitLifecycleState.ARCHIVED)
        )
        testClock.currentLocalDate = LocalDate.of(2026, 11, 5)

        // Inspecting October 2026 must still include h_daily_cal in calendar range, DayDetail, and WeeklyBoard!
        val octHabits = repository.getHabitsRelevantForDateRange(
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 31)
        )
        assertTrue(octHabits.any { it.habitId == "h_daily_cal" })

        val oct5Detail = repository.computeDayDetailSummary(LocalDate.of(2026, 10, 5), testClock.todayLocal())
        val oct5HabitItem = oct5Detail.habitItems.find { it.habit.habitId == "h_daily_cal" }
        assertNotNull(oct5HabitItem)
        assertEquals(HonestCalendarDateState.COMPLETED_TARGET_MET, oct5HabitItem!!.honestState)
    }

    @Test
    fun p3_07_weeklyHabitBoardAndDayDetailNavigationRestoreParentContext() {
        val today = LocalDate.of(2026, 10, 14) // Wednesday
        testClock.currentLocalDate = today

        val weeklyHabit = Habit(
            habitId = "h_wb_week",
            name = "Strength Training",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "sessions",
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 3,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
            preferredWeekdaysMask = WeekdayMask.toMask(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        )
        repository.saveHabitSync(weeklyHabit, effectiveDateOverride = LocalDate.of(2026, 10, 1))
        repository.logHabitEntrySync("h_wb_week", LocalDate.of(2026, 10, 12), 1, "Monday lift")
        repository.logHabitEntrySync("h_wb_week", LocalDate.of(2026, 10, 14), 1, "Wednesday lift")

        val board = repository.computeWeeklyBoard(today, referenceToday = today)
        assertEquals(LocalDate.of(2026, 10, 12), board.weekStartDate)
        assertEquals(LocalDate.of(2026, 10, 18), board.weekEndDate)
        assertEquals(7, board.days.size)
        val row = board.rows.single { it.habit.habitId == "h_wb_week" }
        assertEquals(7, row.cells.size)
        assertEquals(2, row.weekTotalUnits.toInt())
        assertEquals(3, row.weekTargetUnits.toInt())

        // Navigate Insights -> DayDetail -> Back
        viewModel.selectTab(NavTab.INSIGHTS)
        viewModel.openDayDetail(LocalDate.of(2026, 10, 12))
        assertTrue(viewModel.currentSecondaryRoute.value is SecondaryRoute.DayDetail)
        assertEquals(LocalDate.of(2026, 10, 12), (viewModel.currentSecondaryRoute.value as SecondaryRoute.DayDetail).date)

        assertTrue(viewModel.navigateBack())
        assertNull(viewModel.currentSecondaryRoute.value)
        assertEquals(NavTab.INSIGHTS, viewModel.selectedTab.value)
    }

    // =========================================================================
    // 4.3 Collected Moments (G2 + G3) Duration Normalization & Explicit Zero
    // =========================================================================

    @Test
    fun p3_08_collectedMomentsG2AndG3DurationNormalizationZeroValueDistinctionAndUnsupportedUnitExclusion() {
        assertEquals("0 min", DurationNormalizer.formatCollectedMomentsDuration(0L))
        assertEquals("45 min", DurationNormalizer.formatCollectedMomentsDuration(45L))
        assertEquals("1 hr", DurationNormalizer.formatCollectedMomentsDuration(60L))
        assertEquals("1 hr 25 min", DurationNormalizer.formatCollectedMomentsDuration(85L))
        assertEquals("2 hr", DurationNormalizer.formatCollectedMomentsDuration(120L))
        assertEquals("2 hr 30 min", DurationNormalizer.formatCollectedMomentsDuration(150L))
        assertEquals("1 day 2 hr", DurationNormalizer.formatCollectedMomentsDuration(1560L))
        assertEquals("1 wk 2 days", DurationNormalizer.formatCollectedMomentsDuration(12960L))

        val today = LocalDate.of(2026, 10, 14)
        testClock.currentLocalDate = today

        val minHabit = Habit(
            habitId = "h_dur_min",
            name = "Meditation",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20
        )
        val hrHabit = Habit(
            habitId = "h_dur_hr",
            name = "Deep Work",
            trackingMode = TrackingMode.DURATION,
            unitCode = "hr",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 1
        )
        val unsupportedDurHabit = Habit(
            habitId = "h_dur_pom",
            name = "Pomodoros",
            trackingMode = TrackingMode.DURATION,
            unitCode = "pomodoros",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 4
        )
        val qtyHabit = Habit(
            habitId = "h_qty_pages",
            name = "Reading Pages",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 30
        )

        repository.saveHabitSync(minHabit, effectiveDateOverride = LocalDate.of(2026, 10, 1))
        repository.saveHabitSync(hrHabit, effectiveDateOverride = LocalDate.of(2026, 10, 1))
        repository.saveHabitSync(unsupportedDurHabit, effectiveDateOverride = LocalDate.of(2026, 10, 1))
        repository.saveHabitSync(qtyHabit, effectiveDateOverride = LocalDate.of(2026, 10, 1))

        // Simulate an imported/legacy DURATION habit version whose persisted unit_code is unsupported ("pomodoros")
        repository.dbHelper.writableDatabase.execSQL(
            "UPDATE habit_versions SET unit_code = 'pomodoros' WHERE habit_id = 'h_dur_pom'"
        )
        repository.refreshAllSync()

        // Positive duration sessions: 30m + 1hr (60m) = 90m ("1 hr 30 min")
        repository.logHabitEntrySync("h_dur_min", today, 30, "Morning sit")
        repository.logHabitEntrySync("h_dur_hr", today, 1, "Architecture review")
        // Explicit 0 duration check-in on previous day: must NOT add to duration total or positive session count
        repository.logHabitEntrySync("h_dur_min", today.minusDays(1), 0, "Honest 0 check-in")
        // Unsupported duration unit & non-duration habit: must be excluded from Collected Moments total
        repository.logHabitEntrySync("h_dur_pom", today, 3, "3 pomodoros")
        repository.logHabitEntrySync("h_qty_pages", today, 25, "25 pages")

        val gm = repository.growthMetrics.value
        assertEquals(90L, gm.currentMonthDurationMinutes)
        assertEquals("1 hr 30 min", DurationNormalizer.formatCollectedMomentsDuration(gm.currentMonthDurationMinutes))
        assertEquals(2, gm.currentMonthDurationPositiveSessionsCount)
        assertEquals(1, gm.currentMonthDurationZeroSessionsCount)
        assertTrue(gm.currentMonthUnsupportedDurationUnits.contains("pomodoros"))

        val monthDetail = repository.computeCollectedMomentsMonthDetail(YearMonth.of(2026, 10))
        assertEquals(90L, monthDetail.totalMinutes)
        assertEquals("1 hr 30 min", monthDetail.formattedMonthTotal)
        assertEquals(2, monthDetail.positiveSessionsCount)
        assertEquals(1, monthDetail.explicitZeroSessionsCount)
        assertTrue(monthDetail.unsupportedUnitCodes.contains("pomodoros"))

        val zeroDaySummary = monthDetail.dateSummaries[today.minusDays(1)]
        assertNotNull(zeroDaySummary)
        assertEquals(0L, zeroDaySummary!!.totalMinutes)
        assertEquals(0, zeroDaySummary.positiveSessionsCount)
        assertEquals(1, zeroDaySummary.explicitZeroSessionsCount)
        assertTrue(zeroDaySummary.sessions.single().isExplicitZero)
    }

    // =========================================================================
    // 4.4 Mood Insights Canonical October 2026 J1–J11 Fixture (docs/07 lines 143–169)
    // =========================================================================

    @Test
    fun p3_09_moodInsightsCanonicalOctober2026J1ToJ11FixtureAndPrimaryMoodOnlyFiltering() {
        testClock.currentLocalDate = LocalDate.of(2026, 10, 20)

        val entries = listOf(
            // Current month (October 2026): J1..J8
            JournalEntry("J1", "2026-10-02", "Quiet walk", "J1", primaryMoodCode = "CALM", secondaryMoodCode = "HOPEFUL"),
            JournalEntry("J2", "2026-10-03", "Deep study", "J2", primaryMoodCode = "FOCUSED", secondaryMoodCode = "CALM"),
            JournalEntry("J3", "2026-10-05", "Slow evening", "J3", primaryMoodCode = "CALM", secondaryMoodCode = "TIRED"),
            JournalEntry("J4", "2026-10-06", "Long day", "J4", primaryMoodCode = "TIRED", secondaryMoodCode = "SAD"),
            JournalEntry("J5", "2026-10-08", "New idea", "J5", primaryMoodCode = "HOPEFUL", secondaryMoodCode = null),
            JournalEntry("J6", "2026-10-10", "Project milestone", "J6", primaryMoodCode = "FOCUSED", secondaryMoodCode = "JOYFUL"),
            JournalEntry("J7", "2026-10-12", "Plain note without mood", "J7", primaryMoodCode = null, secondaryMoodCode = null),
            JournalEntry("J8", "2026-10-14", "Rainy afternoon", "J8", primaryMoodCode = "SAD", secondaryMoodCode = "TIRED"),
            // Previous month (September 2026): J9..J11
            JournalEntry("J9", "2026-09-10", "Sep calm", "J9", primaryMoodCode = "CALM", secondaryMoodCode = null),
            JournalEntry("J10", "2026-09-14", "Sep tired 1", "J10", primaryMoodCode = "TIRED", secondaryMoodCode = null),
            JournalEntry("J11", "2026-09-22", "Sep tired 2", "J11", primaryMoodCode = "TIRED", secondaryMoodCode = null)
        )
        entries.forEach { repository.saveJournalEntrySync(it) }

        // D1 unsaved draft on 2026-10-15 with JOYFUL mood (must be excluded from Mood Insights!)
        repository.saveJournalDraftSync(
            draftId = "D1",
            localDate = LocalDate.of(2026, 10, 15),
            title = "Draft D1",
            body = "Unsaved joyful draft",
            primaryMoodCode = "JOYFUL"
        )

        val octInsights = repository.computeMoodInsights(
            periodKind = "MONTH",
            referenceDate = LocalDate.of(2026, 10, 20),
            selectedMoodFilter = "CALM"
        )

        assertEquals(7, octInsights.totalEntriesWithPrimaryMood)
        assertEquals(3, octInsights.previousPeriodTotalEntries)

        val distByCode = octInsights.distribution.associateBy { it.moodCode }
        assertEquals(2, distByCode["CALM"]?.count)
        assertEquals(28, distByCode["CALM"]?.percentage)
        assertEquals(1, distByCode["CALM"]?.previousPeriodCount)

        assertEquals(2, distByCode["FOCUSED"]?.count)
        assertEquals(28, distByCode["FOCUSED"]?.percentage)
        assertEquals(0, distByCode["FOCUSED"]?.previousPeriodCount)

        assertEquals(1, distByCode["TIRED"]?.count)
        assertEquals(14, distByCode["TIRED"]?.percentage)
        assertEquals(2, distByCode["TIRED"]?.previousPeriodCount)

        assertEquals(1, distByCode["HOPEFUL"]?.count)
        assertEquals(14, distByCode["HOPEFUL"]?.percentage)

        assertEquals(1, distByCode["SAD"]?.count)
        assertEquals(14, distByCode["SAD"]?.percentage)

        // D1 draft and J6 secondary JOYFUL mood must NOT appear in primary mood distribution
        assertNull(distByCode["JOYFUL"])

        // Filtering by primary mood "CALM" must return ONLY J1 and J3 (NOT J2 where CALM is secondary!)
        val matchingIds = octInsights.matchingEntries.map { it.journalEntryId }.toSet()
        assertEquals(setOf("J1", "J3"), matchingIds)
    }

    // =========================================================================
    // 4.5 Insights Metric Separation, Aggregated D-040 Bands & Past/Current/Future Opportunities
    // =========================================================================

    @Test
    fun p3_10_insightsSeparatesHabitAndJournalDatesAndAggregatesD040BandsPerHabitDateAndHabitWeek() {
        val today = LocalDate.of(2026, 10, 14) // Wednesday
        testClock.currentLocalDate = today

        // 1. Log a Journal entry on a date with NO habit activity (2026-10-10)
        repository.saveJournalEntrySync(
            JournalEntry(
                journalEntryId = "j_only_date",
                localDate = "2026-10-10",
                body = "Journal-only day"
            )
        )
        val metricsAfterJournalOnly = repository.growthMetrics.value
        assertEquals(0, metricsAfterJournalOnly.habitActiveDatesCount)
        assertEquals(0, metricsAfterJournalOnly.totalActiveDates)
        assertEquals(1, metricsAfterJournalOnly.journalEntriesCount)
        assertEquals(1, metricsAfterJournalOnly.journalActiveDatesCount)

        // 2. Daily quantity habit (target 100 ml/day): log TWO 50-ml sessions on today (2026-10-14)
        val waterHabit = Habit(
            habitId = "h_water_100",
            name = "Hydration",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "ml",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 100
        )
        repository.saveHabitSync(waterHabit, effectiveDateOverride = today)
        repository.logHabitEntrySync("h_water_100", today, 50, "Morning glass")
        repository.logHabitEntrySync("h_water_100", today, 50, "Afternoon glass")

        // 3. Weekly duration habit (target 60 min/week): log THREE 20-min sessions across the week
        val mobilityHabit = Habit(
            habitId = "h_mob_60w",
            name = "Mobility",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 60,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
        )
        repository.saveHabitSync(mobilityHabit, effectiveDateOverride = LocalDate.of(2026, 10, 12))
        repository.logHabitEntrySync("h_mob_60w", LocalDate.of(2026, 10, 12), 20)
        repository.logHabitEntrySync("h_mob_60w", LocalDate.of(2026, 10, 13), 20)
        repository.logHabitEntrySync("h_mob_60w", LocalDate.of(2026, 10, 14), 20)

        val gm = repository.growthMetrics.value
        // 3 habit-active dates (Oct 12, 13, 14); Oct 10 Journal-only date is NOT in habitActiveDatesCount
        assertEquals(3, gm.habitActiveDatesCount)
        assertEquals(1, gm.journalActiveDatesCount)
        // 5 positive habit_entries rows total (2 water + 3 mobility)
        assertEquals(5, gm.numericDurationSessionsCount)
        assertEquals(0, gm.completionOutcomesCount)

        // Aggregated D-040 bands:
        // - Water on Oct 14: 50 + 50 = 100 / 100 -> 1 GOAL_MET in dailyProgressBandCounts (NOT 2 HALFWAY!)
        assertEquals(1, gm.dailyProgressBandCounts[ProgressBand.GOAL_MET])
        assertEquals(0, gm.dailyProgressBandCounts[ProgressBand.HALFWAY])
        // - Mobility for week of Oct 12: 20 + 20 + 20 = 60 / 60 -> 1 GOAL_MET in weeklyProgressBandCounts
        assertEquals(1, gm.weeklyProgressBandCounts[ProgressBand.GOAL_MET])
        assertEquals(2, gm.uniqueHabitsCompletedCount)
    }

    @Test
    fun p3_11_scheduledDailyOpportunitiesNumeratorDenominatorAndPastCurrentFutureDateResolutionRules() {
        // Reference today is Wednesday 2026-10-14
        val referenceToday = LocalDate.of(2026, 10, 14)
        testClock.currentLocalDate = referenceToday

        val habit = Habit(
            habitId = "h_opp_daily",
            name = "Daily Practice",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "min",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 10,
            scheduleKind = ScheduleKind.DAILY
        )
        // Habit effective from 2026-10-10 (4 past days: Oct 10, 11, 12, 13; current day: Oct 14; future days: Oct 15..18)
        repository.saveHabitSync(habit, effectiveDateOverride = LocalDate.of(2026, 10, 10))

        // Past dates (< referenceToday):
        // Oct 10: Met (10/10) -> numerator +1, denominator +1
        repository.logHabitEntrySync("h_opp_daily", LocalDate.of(2026, 10, 10), 10)
        // Oct 11: Met (15/10) -> numerator +1, denominator +1
        repository.logHabitEntrySync("h_opp_daily", LocalDate.of(2026, 10, 11), 15)
        // Oct 12: Missed (nothing logged) -> numerator +0, denominator +1
        // Oct 13: Missed/Partial (5/10 < 10) -> numerator +0, denominator +1
        repository.logHabitEntrySync("h_opp_daily", LocalDate.of(2026, 10, 13), 5)

        // Evaluate reporting period 2026-10-10 .. 2026-10-18 (includes past, current Oct 14 still open, and 4 future dates Oct 15..18)
        val openTodayMetrics = repository.computeGrowthMetricsForPeriodSync(
            startDate = LocalDate.of(2026, 10, 10),
            endDate = LocalDate.of(2026, 10, 18),
            referenceToday = referenceToday
        )
        // While today (Oct 14) is not yet met, only the 4 completed past days (Oct 10..13) are in denominator (4), with 2 met.
        // Future dates (Oct 15..18) are excluded from both numerator and denominator!
        assertEquals(2, openTodayMetrics.habitDaysMetCount)
        assertEquals(4, openTodayMetrics.scheduledDailyOpportunitiesCount)

        // Now complete today's target on referenceToday (Oct 14): 10/10 met
        repository.logHabitEntrySync("h_opp_daily", referenceToday, 10)

        val metTodayMetrics = repository.computeGrowthMetricsForPeriodSync(
            startDate = LocalDate.of(2026, 10, 10),
            endDate = LocalDate.of(2026, 10, 18),
            referenceToday = referenceToday
        )
        // Today (Oct 14) is now met -> included in both numerator (3) and denominator (5) -> 3/5 = 60%.
        // Future scheduled dates (Oct 15..18) still do NOT increase scheduledDailyOpportunitiesCount or create missed opportunities!
        assertEquals(3, metTodayMetrics.habitDaysMetCount)
        assertEquals(5, metTodayMetrics.scheduledDailyOpportunitiesCount)
    }

    @Test
    fun p3_12_postPreparationViewModelSaveConcurrencyAndWeeklyBoardCellContributionAndExplicitZeroBands() {
        val today = LocalDate.of(2026, 10, 14) // Wednesday
        testClock.currentLocalDate = today

        // 1. Two-caller post-preparation concurrency via ViewModel.saveJournalEntry:
        // Caller 1 and Caller 2 both hold draftId "draft_vm_race". Caller 1 promotes first,
        // then Caller 2 invokes viewModel.saveJournalEntry with a stale/different payload.
        repository.saveJournalDraftSync(
            draftId = "draft_vm_race",
            localDate = today,
            title = "Canonical Title",
            body = "Canonical body saved by Caller 1.",
            primaryMoodCode = "CALM"
        )
        val preparedId = repository.prepareDraftForSaveSync("draft_vm_race")
        assertNotNull(preparedId)

        // Caller 1 promotes the draft
        val promotedByCaller1 = repository.promoteDraftToJournalEntrySync("draft_vm_race")
        assertNotNull(promotedByCaller1)
        assertEquals(preparedId, promotedByCaller1!!.journalEntryId)

        // Caller 2 (late/concurrent) invokes viewModel.saveJournalEntry with stale/mutated text for the same sourceDraftId
        testClock.currentUtcMs += 10_000L
        val staleCaller2Entry = JournalEntry(
            journalEntryId = UUID.randomUUID().toString(),
            localDate = today.minusDays(2).toString(),
            title = "Stale Overwrite Title",
            body = "Stale body that must NOT overwrite Caller 1!",
            primaryMoodCode = "STRESSED"
        )
        val caller2Result = viewModel.saveJournalEntry(staleCaller2Entry, sourceDraftId = "draft_vm_race")
        assertTrue(caller2Result)

        // Verify only 1 entry exists, with Caller 1's exact ID, body, title, mood, and timestamps preserved!
        assertEquals(1, repository.journalEntries.value.size)
        val persistedEntry = repository.journalEntries.value.single()
        assertEquals(preparedId, persistedEntry.journalEntryId)
        assertEquals("Canonical Title", persistedEntry.title)
        assertEquals("Canonical body saved by Caller 1.", persistedEntry.body)
        assertEquals("CALM", persistedEntry.primaryMoodCode)
        assertEquals(promotedByCaller1.createdAtUtcMs, persistedEntry.createdAtUtcMs)
        assertEquals(promotedByCaller1.updatedAtUtcMs, persistedEntry.updatedAtUtcMs)

        // 2. Weekly Board TargetPeriod.WEEK cell contributions & non-missed unlogged days:
        val weeklyPagesHabit = Habit(
            habitId = "h_weekly_pages",
            name = "Weekly Reading Goal",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 50,
            scheduleKind = ScheduleKind.SELECTED_WEEKDAYS,
            scheduledWeekdaysMask = WeekdayMask.toMask(
                setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
            )
        )
        repository.saveHabitSync(weeklyPagesHabit, effectiveDateOverride = LocalDate.of(2026, 10, 1))
        // Log 15 pages on Monday (2026-10-12), nothing on Tuesday (2026-10-13), explicit 0 on Wednesday (2026-10-14)
        repository.logHabitEntrySync("h_weekly_pages", LocalDate.of(2026, 10, 12), 15)
        repository.logHabitEntrySync("h_weekly_pages", LocalDate.of(2026, 10, 14), 0)

        val board = repository.computeWeeklyBoard(today, filterHabitId = "h_weekly_pages", referenceToday = today)
        val row = board.rows.single()
        val monCell = row.cells.first { it.date == LocalDate.of(2026, 10, 12) }
        val tueCell = row.cells.first { it.date == LocalDate.of(2026, 10, 13) }
        val wedCell = row.cells.first { it.date == LocalDate.of(2026, 10, 14) }

        // Individual date cells show daily contribution (+15p, ·, 0) and do NOT classify individual days against 50p weekly target
        assertEquals("+15p", monCell.cellLabel)
        assertEquals(ProgressBand.ZERO, monCell.progressBand)
        // Unlogged Tuesday in a weekly-target habit must NEVER be marked MISSED_SCHEDULED_DATE
        assertEquals(HonestCalendarDateState.FLEXIBLE_WEEKLY_IN_PROGRESS, tueCell.honestState)
        assertEquals("·", tueCell.cellLabel)
        // Explicit 0 on Wednesday shows "0" and EXPLICIT_ZERO_NOT_DONE
        assertEquals("0", wedCell.cellLabel)
        assertEquals(HonestCalendarDateState.EXPLICIT_ZERO_NOT_DONE, wedCell.honestState)
        // The weekly D-040 band is evaluated once on the week row (15 / 50 = 30% -> GETTING_STARTED)
        assertEquals(ProgressBand.GETTING_STARTED, row.weekProgressBand)

        // 3. Explicit-zero ProgressBand.ZERO counting in dailyProgressBandCounts and weeklyProgressBandCounts:
        val dailyZeroHabit = Habit(
            habitId = "h_daily_zero",
            name = "Evening Stretch",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 15
        )
        repository.saveHabitSync(dailyZeroHabit, effectiveDateOverride = LocalDate.of(2026, 10, 12))
        repository.logHabitEntrySync("h_daily_zero", LocalDate.of(2026, 10, 12), 0)

        val weeklyZeroHabit = Habit(
            habitId = "h_weekly_zero",
            name = "Weekly Run",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "km",
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 10,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
        )
        repository.saveHabitSync(weeklyZeroHabit, effectiveDateOverride = LocalDate.of(2026, 10, 12))
        repository.logHabitEntrySync("h_weekly_zero", LocalDate.of(2026, 10, 12), 0)

        val gm = repository.growthMetrics.value
        assertEquals(1, gm.dailyProgressBandCounts[ProgressBand.ZERO])
        assertEquals(1, gm.weeklyProgressBandCounts[ProgressBand.ZERO])
    }

    @Test
    fun p3_13_dayDetailWeeklyHabitShowsDailyContributionWithoutWeeklyTargetMisclassificationAndPreservesDailyHabitBehavior() {
        val today = LocalDate.of(2026, 10, 14) // Wednesday (week: Mon Oct 12 .. Sun Oct 18)
        testClock.currentLocalDate = today

        // 1. Weekly habit: 50 pages/week effective from start of week (2026-10-12)
        val weeklyReading = Habit(
            habitId = "h_daydetail_week_pages",
            name = "Weekly Literature",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 50,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
        )
        repository.saveHabitSync(weeklyReading, effectiveDateOverride = LocalDate.of(2026, 10, 12))

        // Mid-week version change on Wednesday (2026-10-14) raising target to 100 pages/week:
        // Under U-12 frozen start-of-week rule, the week of Oct 12..18 must still be evaluated once against the frozen 50-page version!
        repository.saveHabitSync(
            weeklyReading.copy(targetValue = 100),
            effectiveDateOverride = LocalDate.of(2026, 10, 14)
        )

        // 2. Daily habit: 20 min/day for verifying daily-target Day Detail behaviour remains unchanged
        val dailyMeditation = Habit(
            habitId = "h_daydetail_day_med",
            name = "Daily Meditation",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20,
            scheduleKind = ScheduleKind.DAILY
        )
        repository.saveHabitSync(dailyMeditation, effectiveDateOverride = LocalDate.of(2026, 10, 12))

        // Log partially contributing Monday (2026-10-12): 15 pages on weekly habit, 10 min on daily habit (HALFWAY)
        repository.logHabitEntrySync("h_daydetail_week_pages", LocalDate.of(2026, 10, 12), 15)
        repository.logHabitEntrySync("h_daydetail_day_med", LocalDate.of(2026, 10, 12), 10)

        val monDetailBeforeWeekMet = repository.computeDayDetailSummary(LocalDate.of(2026, 10, 12), today)
        val monWeeklyBefore = monDetailBeforeWeekMet.habitItems.first { it.habit.habitId == "h_daydetail_week_pages" }
        // Selected date's contribution is 15 pages, NOT classified as GETTING_STARTED on the individual date
        assertEquals(BigInteger.valueOf(15), monWeeklyBefore.totalUnits)
        assertEquals(BigInteger.ZERO, monWeeklyBefore.targetUnits)
        assertEquals(0, monWeeklyBefore.percentage)
        assertEquals(ProgressBand.ZERO, monWeeklyBefore.progressBand)
        assertEquals(DayRecordState.PARTIAL_PROGRESS, monWeeklyBefore.dayRecordState)
        assertEquals(HonestCalendarDateState.COMPLETED_TARGET_MET, monWeeklyBefore.honestState)
        // Weekly result is calculated once using the frozen start-of-week version (15 / 50 = 30% -> GETTING_STARTED)
        assertEquals(50, monWeeklyBefore.effectiveVersion.targetValue)
        assertEquals(BigInteger.valueOf(15), monWeeklyBefore.weekTotalUnits)
        assertEquals(BigInteger.valueOf(50), monWeeklyBefore.weekTargetUnits)
        assertEquals(30, monWeeklyBefore.weekPercentage)
        assertEquals(ProgressBand.GETTING_STARTED, monWeeklyBefore.weekProgressBand)
        assertNotNull(monWeeklyBefore.weekProgressSummary)
        assertTrue(monWeeklyBefore.weekProgressSummary!!.contains("15 / 50 pages"))
        assertTrue(monWeeklyBefore.weekProgressSummary!!.contains(ProgressBand.GETTING_STARTED.displayName))

        // Verify daily-target habit on Monday is unchanged (10 / 20 min = 50% -> HALFWAY)
        val monDaily = monDetailBeforeWeekMet.habitItems.first { it.habit.habitId == "h_daydetail_day_med" }
        assertEquals(BigInteger.valueOf(10), monDaily.totalUnits)
        assertEquals(BigInteger.valueOf(20), monDaily.targetUnits)
        assertEquals(50, monDaily.percentage)
        assertEquals(ProgressBand.HALFWAY, monDaily.progressBand)
        assertNull(monDaily.weekProgressSummary)
        assertNull(monDaily.weekProgressBand)

        // Now log 35 pages on Wednesday (2026-10-14) so the week meets its frozen 50-page target (15 + 35 = 50 / 50 -> GOAL_MET),
        // and log 20 min on the daily habit on Wednesday (20 / 20 -> GOAL_MET).
        repository.logHabitEntrySync("h_daydetail_week_pages", LocalDate.of(2026, 10, 14), 35)
        repository.logHabitEntrySync("h_daydetail_day_med", LocalDate.of(2026, 10, 14), 20)

        val monDetailAfterWeekMet = repository.computeDayDetailSummary(LocalDate.of(2026, 10, 12), today)
        val tueDetailAfterWeekMet = repository.computeDayDetailSummary(LocalDate.of(2026, 10, 13), today)
        val wedDetailAfterWeekMet = repository.computeDayDetailSummary(LocalDate.of(2026, 10, 14), today)

        val monWeeklyAfter = monDetailAfterWeekMet.habitItems.first { it.habit.habitId == "h_daydetail_week_pages" }
        val tueWeeklyAfter = tueDetailAfterWeekMet.habitItems.first { it.habit.habitId == "h_daydetail_week_pages" }
        val wedWeeklyAfter = wedDetailAfterWeekMet.habitItems.first { it.habit.habitId == "h_daydetail_week_pages" }

        // Each date shows its own actual daily contribution (15, 0, 35) without daily weekly-target classification
        assertEquals(BigInteger.valueOf(15), monWeeklyAfter.totalUnits)
        assertEquals(ProgressBand.ZERO, monWeeklyAfter.progressBand)
        assertEquals(BigInteger.ZERO, tueWeeklyAfter.totalUnits)
        assertEquals(ProgressBand.ZERO, tueWeeklyAfter.progressBand)
        assertEquals(BigInteger.valueOf(35), wedWeeklyAfter.totalUnits)
        assertEquals(ProgressBand.ZERO, wedWeeklyAfter.progressBand)

        // And all dates in the week share the single weekly result computed from the frozen start-of-week version (50 / 50 -> GOAL_MET)
        for (item in listOf(monWeeklyAfter, tueWeeklyAfter, wedWeeklyAfter)) {
            assertEquals(50, item.effectiveVersion.targetValue)
            assertEquals(BigInteger.valueOf(50), item.weekTotalUnits)
            assertEquals(BigInteger.valueOf(50), item.weekTargetUnits)
            assertEquals(100, item.weekPercentage)
            assertEquals(ProgressBand.GOAL_MET, item.weekProgressBand)
            assertNotNull(item.weekProgressSummary)
            assertTrue(item.weekProgressSummary!!.contains("50 / 50 pages"))
            assertTrue(item.weekProgressSummary!!.contains(ProgressBand.GOAL_MET.displayName))
        }

        // Daily habit on Wednesday shows 20 / 20 min -> GOAL_MET, unchanged
        val wedDaily = wedDetailAfterWeekMet.habitItems.first { it.habit.habitId == "h_daydetail_day_med" }
        assertEquals(BigInteger.valueOf(20), wedDaily.totalUnits)
        assertEquals(BigInteger.valueOf(20), wedDaily.targetUnits)
        assertEquals(100, wedDaily.percentage)
        assertEquals(ProgressBand.GOAL_MET, wedDaily.progressBand)
        assertNull(wedDaily.weekProgressSummary)
    }

    @Test
    fun p3_14_weeklyEffortAwareDayDetailPreservesRecordedTinyEffortVersusQualifyingContribution() {
        val today = LocalDate.of(2026, 10, 14) // Wednesday (week: Mon Oct 12 .. Sun Oct 18)
        testClock.currentLocalDate = today

        // Legacy weekly EFFORT_AWARE habit: target 2 days/week
        val weeklyEffortHabit = Habit(
            habitId = "h_weekly_effort_legacy",
            name = "Legacy Weekly Practice",
            trackingMode = TrackingMode.EFFORT_AWARE,
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 2,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
        )
        repository.saveHabitSync(
            weeklyEffortHabit,
            effectiveDateOverride = LocalDate.of(2026, 10, 12),
            allowLegacyEffortModeCreation = true
        )

        // 1. Log TINY effort on Monday (2026-10-12):
        // Recorded effort is preserved (outcomes non-empty, PARTIAL_PROGRESS, PARTIAL_OR_TINY_PROGRESS, CompactHistoryBucket.PARTIAL),
        // but qualifying weekly contribution is 0 days (matching computeWeeklyProgressSnapshot).
        repository.logHabitDayOutcomeSync(
            habitId = "h_weekly_effort_legacy",
            date = LocalDate.of(2026, 10, 12),
            outcomeKind = "EFFORT_LOGGED",
            effortLevel = "TINY"
        )

        val monDetailTinyOnly = repository.computeDayDetailSummary(LocalDate.of(2026, 10, 12), today)
        val monItemTiny = monDetailTinyOnly.habitItems.first { it.habit.habitId == "h_weekly_effort_legacy" }
        assertEquals(1, monItemTiny.outcomes.size)
        assertEquals("TINY", monItemTiny.outcomes.single().effortLevel)
        assertEquals(DayRecordState.PARTIAL_PROGRESS, monItemTiny.dayRecordState)
        assertEquals(HonestCalendarDateState.PARTIAL_OR_TINY_PROGRESS, monItemTiny.honestState)
        assertEquals(CompactHistoryBucket.PARTIAL, monItemTiny.compactBucket)
        // Qualifying weekly contribution on Monday is 0 days, and weekly total is 0 / 2 days (ProgressBand.ZERO)
        assertEquals(BigInteger.ZERO, monItemTiny.totalUnits)
        assertEquals(ProgressBand.ZERO, monItemTiny.progressBand)
        assertEquals(BigInteger.ZERO, monItemTiny.weekTotalUnits)
        assertEquals(BigInteger.valueOf(2), monItemTiny.weekTargetUnits)
        assertEquals(0, monItemTiny.weekPercentage)
        assertEquals(ProgressBand.ZERO, monItemTiny.weekProgressBand)

        // 2. Log qualifying USUAL effort on Tuesday (2026-10-13) and EXTRA effort on Wednesday (2026-10-14):
        // Now the week has 2 qualifying days (Tue + Wed = 2 / 2 -> GOAL_MET), while Monday remains 0 qualifying days with TINY effort preserved.
        repository.logHabitDayOutcomeSync(
            habitId = "h_weekly_effort_legacy",
            date = LocalDate.of(2026, 10, 13),
            outcomeKind = "EFFORT_LOGGED",
            effortLevel = "USUAL"
        )
        repository.logHabitDayOutcomeSync(
            habitId = "h_weekly_effort_legacy",
            date = LocalDate.of(2026, 10, 14),
            outcomeKind = "EFFORT_LOGGED",
            effortLevel = "EXTRA"
        )

        val monDetailAfter = repository.computeDayDetailSummary(LocalDate.of(2026, 10, 12), today)
        val tueDetailAfter = repository.computeDayDetailSummary(LocalDate.of(2026, 10, 13), today)
        val wedDetailAfter = repository.computeDayDetailSummary(LocalDate.of(2026, 10, 14), today)

        val monItemAfter = monDetailAfter.habitItems.first { it.habit.habitId == "h_weekly_effort_legacy" }
        val tueItemAfter = tueDetailAfter.habitItems.first { it.habit.habitId == "h_weekly_effort_legacy" }
        val wedItemAfter = wedDetailAfter.habitItems.first { it.habit.habitId == "h_weekly_effort_legacy" }

        // Monday still preserves recorded TINY effort while reporting 0 qualifying days for that date
        assertEquals("TINY", monItemAfter.outcomes.single().effortLevel)
        assertEquals(DayRecordState.PARTIAL_PROGRESS, monItemAfter.dayRecordState)
        assertEquals(HonestCalendarDateState.PARTIAL_OR_TINY_PROGRESS, monItemAfter.honestState)
        assertEquals(CompactHistoryBucket.PARTIAL, monItemAfter.compactBucket)
        assertEquals(BigInteger.ZERO, monItemAfter.totalUnits)
        assertEquals(ProgressBand.ZERO, monItemAfter.progressBand)

        // Tuesday (USUAL) and Wednesday (EXTRA) each report 1 qualifying day for their date
        assertEquals("USUAL", tueItemAfter.outcomes.single().effortLevel)
        assertEquals(HonestCalendarDateState.COMPLETED_TARGET_MET, tueItemAfter.honestState)
        assertEquals(CompactHistoryBucket.COMPLETED, tueItemAfter.compactBucket)
        assertEquals(BigInteger.ONE, tueItemAfter.totalUnits)
        assertEquals(ProgressBand.ZERO, tueItemAfter.progressBand)

        assertEquals("EXTRA", wedItemAfter.outcomes.single().effortLevel)
        assertEquals(HonestCalendarDateState.COMPLETED_TARGET_MET, wedItemAfter.honestState)
        assertEquals(CompactHistoryBucket.COMPLETED, wedItemAfter.compactBucket)
        assertEquals(BigInteger.ONE, wedItemAfter.totalUnits)
        assertEquals(ProgressBand.ZERO, wedItemAfter.progressBand)

        // All three dates show the consistent weekly snapshot: 2 / 2 days (100% -> GOAL_MET)
        for (item in listOf(monItemAfter, tueItemAfter, wedItemAfter)) {
            assertEquals(BigInteger.valueOf(2), item.weekTotalUnits)
            assertEquals(BigInteger.valueOf(2), item.weekTargetUnits)
            assertEquals(100, item.weekPercentage)
            assertEquals(ProgressBand.GOAL_MET, item.weekProgressBand)
            assertNotNull(item.weekProgressSummary)
            assertTrue(item.weekProgressSummary!!.contains("2 / 2 days"))
            assertTrue(item.weekProgressSummary!!.contains(ProgressBand.GOAL_MET.displayName))
        }
    }
}
