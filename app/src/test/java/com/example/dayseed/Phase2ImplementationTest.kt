package com.example.dayseed

import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.DerivedDayStatus
import com.example.dayseed.data.GroveTerminalReason
import com.example.dayseed.data.GroveTreeState
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitConfigEffectivePolicy
import com.example.dayseed.data.HabitConfigurationLockedTodayException
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.data.ProgressBand
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.WeekdayMask
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Exhaustive Phase 2 test suite covering all mandatory test cases (T2-01 through T2-51)
 * specified in the Phase 2 Implementation Prompt §12.1.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase2ImplementationTest {

    private lateinit var app: Application
    private lateinit var testClock: TestDayseedClock
    private lateinit var repository: DayseedRepository
    private lateinit var viewModel: DayseedViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        testClock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 3, 18), // Wednesday
            currentLocalTime = LocalTime.of(10, 0),
            currentUtcMs = LocalDate.of(2026, 3, 18)
                .atTime(10, 0)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        )
        repository = DayseedRepository(
            context = app,
            clock = testClock,
            databaseName = null
        )
        viewModel = DayseedViewModel(app, customRepository = repository)
    }

    @After
    fun tearDown() {
        repository.dbHelper.close()
    }

    private fun advanceClockToDate(date: LocalDate, hour: Int = 10, minute: Int = 0) {
        testClock.currentLocalDate = date
        testClock.currentLocalTime = LocalTime.of(hour, minute)
        testClock.currentUtcMs = date.atTime(hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    // =========================================================================
    // 1. Navigation & Back-Stack Tests (T2-01 .. T2-05)
    // =========================================================================

    @Test
    fun t2_01_fiveDestinationBottomNavigationSwitchesTabsAndPreservesHistory() {
        assertEquals(NavTab.TODAY, viewModel.selectedTab.value)
        assertTrue(viewModel.tabHistoryStack.value.isEmpty())

        viewModel.selectTab(NavTab.HABITS)
        assertEquals(NavTab.HABITS, viewModel.selectedTab.value)
        assertEquals(listOf(NavTab.TODAY), viewModel.tabHistoryStack.value)

        viewModel.selectTab(NavTab.JOURNAL)
        assertEquals(NavTab.JOURNAL, viewModel.selectedTab.value)
        assertEquals(listOf(NavTab.TODAY, NavTab.HABITS), viewModel.tabHistoryStack.value)

        viewModel.selectTab(NavTab.INSIGHTS)
        assertEquals(NavTab.INSIGHTS, viewModel.selectedTab.value)
        assertEquals(listOf(NavTab.TODAY, NavTab.HABITS, NavTab.JOURNAL), viewModel.tabHistoryStack.value)

        viewModel.selectTab(NavTab.SETTINGS)
        assertEquals(NavTab.SETTINGS, viewModel.selectedTab.value)
        assertEquals(
            listOf(NavTab.TODAY, NavTab.HABITS, NavTab.JOURNAL, NavTab.INSIGHTS),
            viewModel.tabHistoryStack.value
        )

        // Also verify MainActivity launches cleanly under Robolectric
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertNotNull(controller.get())
        controller.pause().stop().destroy()
    }

    @Test
    fun t2_02_secondaryRouteStackRestoresExactParentScreenWithoutResettingToToday() {
        val habit = Habit(
            habitId = "h_nav_1",
            name = "Evening Stretch",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 15,
            createdAtUtcMs = testClock.nowUtcMs() - 86_400_000L * 5
        )
        repository.saveHabitSync(habit, effectiveDateOverride = LocalDate.of(2026, 3, 13))
        viewModel.refreshAllSync()

        // Navigate to HABITS tab and apply ACTIVE filter + search query + scroll position
        viewModel.selectTab(NavTab.HABITS)
        viewModel.setHabitsFilter(HabitsFilterState.ACTIVE)
        viewModel.setHabitsSearchQuery("Stretch")
        viewModel.saveScrollPosition("HABITS", 3, 120)

        // Open HabitDetail from HABITS tab on 2026-03-17
        viewModel.openHabitDetail(habit.habitId, "2026-03-17")
        assertTrue(viewModel.currentSecondaryRoute.value is SecondaryRoute.HabitDetail)
        assertEquals(LocalDate.of(2026, 3, 17), viewModel.selectedHistoryDate.value)

        // From HabitDetail, open RecordSession
        viewModel.openRecordSession(habit.habitId, "2026-03-17", null)
        assertTrue(viewModel.currentSecondaryRoute.value is SecondaryRoute.RecordSession)
        assertEquals(2, viewModel.secondaryRouteStack.value.size)

        // From RecordSession, navigateBack() -> returns to HabitDetail on 2026-03-17
        val handled1 = viewModel.navigateBack()
        assertTrue(handled1)
        val detailRoute = viewModel.currentSecondaryRoute.value as? SecondaryRoute.HabitDetail
        assertNotNull(detailRoute)
        assertEquals(LocalDate.of(2026, 3, 17), detailRoute?.selectedDate)
        assertEquals(NavTab.HABITS, viewModel.selectedTab.value)

        // From HabitDetail, navigateBack() -> returns to HABITS tab with filter, search & scroll preserved
        val handled2 = viewModel.navigateBack()
        assertTrue(handled2)
        assertNull(viewModel.currentSecondaryRoute.value)
        assertEquals(NavTab.HABITS, viewModel.selectedTab.value)
        assertEquals(HabitsFilterState.ACTIVE, viewModel.habitsFilter.value)
        assertEquals("Stretch", viewModel.habitsSearchQuery.value)
        assertEquals(3 to 120, viewModel.getScrollPosition("HABITS"))
    }

    @Test
    fun t2_03_secondaryRouteFromInsightsRestoresInsightsAndSelectedHistoryDate() {
        val habit = Habit(
            habitId = "h_ins_1",
            name = "Morning Walk",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20,
            createdAtUtcMs = testClock.nowUtcMs() - 86_400_000L * 10
        )
        repository.saveHabitSync(habit, effectiveDateOverride = LocalDate.of(2026, 3, 8))
        viewModel.refreshAllSync()

        viewModel.selectTab(NavTab.INSIGHTS)
        val targetDate = LocalDate.of(2026, 3, 15)
        viewModel.selectHistoryDate(targetDate)

        // Open LivingGrove from Insights
        viewModel.openLivingGrove(null)
        assertTrue(viewModel.currentSecondaryRoute.value is SecondaryRoute.LivingGrove)

        // Open HabitDetail from LivingGrove
        viewModel.openHabitDetail(habit.habitId)
        assertTrue(viewModel.currentSecondaryRoute.value is SecondaryRoute.HabitDetail)

        // Back once -> LivingGrove
        viewModel.navigateBack()
        assertTrue(viewModel.currentSecondaryRoute.value is SecondaryRoute.LivingGrove)
        assertEquals(NavTab.INSIGHTS, viewModel.selectedTab.value)

        // Back again -> Insights with selectedHistoryDate intact
        viewModel.navigateBack()
        assertNull(viewModel.currentSecondaryRoute.value)
        assertEquals(NavTab.INSIGHTS, viewModel.selectedTab.value)
        assertEquals(targetDate, viewModel.selectedHistoryDate.value)
    }

    @Test
    fun t2_04_t2_05_backStackAcrossTabsAndCollectedMomentsReturnsToOrigin() {
        viewModel.selectTab(NavTab.HABITS)
        viewModel.selectTab(NavTab.INSIGHTS)
        viewModel.openCollectedMomentsDetail(YearMonth.of(2026, 3), LocalDate.of(2026, 3, 14))
        assertTrue(viewModel.currentSecondaryRoute.value is SecondaryRoute.CollectedMomentsDetail)
        assertEquals(LocalDate.of(2026, 3, 14), viewModel.selectedHistoryDate.value)

        // Pop CollectedMomentsDetail -> back on INSIGHTS
        assertTrue(viewModel.navigateBack())
        assertNull(viewModel.currentSecondaryRoute.value)
        assertEquals(NavTab.INSIGHTS, viewModel.selectedTab.value)

        // Pop tab history -> HABITS -> TODAY
        assertTrue(viewModel.navigateBack())
        assertEquals(NavTab.HABITS, viewModel.selectedTab.value)
        assertTrue(viewModel.navigateBack())
        assertEquals(NavTab.TODAY, viewModel.selectedTab.value)
        assertFalse(viewModel.navigateBack())
    }

    // =========================================================================
    // 2. Today Screen & Longest Active Streak Selector (T2-06 .. T2-11)
    // =========================================================================

    @Test
    fun t2_06_longestActiveRunSelectorDistinguishesDailyAndWeeklyUnitsAndAppliesTieBreakers() {
        val dailyHabit = Habit(
            habitId = "h_daily",
            name = "Daily Water",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.DAY,
            scheduleKind = ScheduleKind.DAILY,
            sortOrder = 1,
            createdAtUtcMs = LocalDate.of(2026, 2, 15).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        val weeklyHabit = Habit(
            habitId = "h_weekly",
            name = "Weekly Run",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "sessions",
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 2,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
            sortOrder = 0,
            createdAtUtcMs = LocalDate.of(2026, 2, 15).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        repository.saveHabitSync(dailyHabit, effectiveDateOverride = LocalDate.of(2026, 2, 15))
        repository.saveHabitSync(weeklyHabit, effectiveDateOverride = LocalDate.of(2026, 2, 15))

        // Log 12 consecutive days for dailyHabit (2026-03-07 .. 2026-03-18) -> completedStreakCount = 12 days (elapsed span = 12)
        for (day in 7..18) {
            repository.logHabitDayOutcomeSync(
                habitId = dailyHabit.habitId,
                date = LocalDate.of(2026, 3, day),
                outcomeKind = "COMPLETED",
                effortLevel = null
            )
        }

        // Log 2 sessions in week 2026-03-09..15 and 2 sessions in current week 2026-03-16..22 -> 2 weeks (elapsed span = 10 days)
        repository.logHabitEntrySync(weeklyHabit.habitId, LocalDate.of(2026, 3, 10), 1, null)
        repository.logHabitEntrySync(weeklyHabit.habitId, LocalDate.of(2026, 3, 12), 1, null)
        repository.logHabitEntrySync(weeklyHabit.habitId, LocalDate.of(2026, 3, 16), 1, null)
        repository.logHabitEntrySync(weeklyHabit.habitId, LocalDate.of(2026, 3, 18), 1, null)

        val longest = repository.selectLongestActiveRun()
        assertNotNull(longest)
        assertEquals("h_daily", longest!!.habitId)
        assertEquals(12, longest.completedStreakCount)
        assertEquals("days", longest.explicitUnitLabel)
        assertTrue(longest.formatIndicatorText().contains("12 days"))

        // Now give weeklyHabit a 4-week active streak by logging earlier weeks (Feb 23..Mar 1 and Mar 2..Mar 8 -> elapsed span = 24 days)
        repository.logHabitEntrySync(weeklyHabit.habitId, LocalDate.of(2026, 2, 24), 2, null)
        repository.logHabitEntrySync(weeklyHabit.habitId, LocalDate.of(2026, 3, 3), 2, null)

        val longestAfter = repository.selectLongestActiveRun()
        assertNotNull(longestAfter)
        assertEquals("h_weekly", longestAfter!!.habitId)
        assertEquals(4, longestAfter.completedStreakCount)
        assertEquals("weeks", longestAfter.explicitUnitLabel)
        assertTrue(longestAfter.formatIndicatorText().contains("4 weeks"))
    }

    @Test
    fun t2_07_todayScheduledVsNotScheduledTodayPartitionAndExtraLogging() {
        // Test clock is Wednesday 2026-03-18
        val wedHabit = Habit(
            habitId = "h_wed",
            name = "Wednesday Yoga",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            scheduleKind = ScheduleKind.SELECTED_WEEKDAYS,
            scheduledWeekdaysMask = WeekdayMask.toMask(setOf(DayOfWeek.WEDNESDAY)),
            scheduleDays = setOf(DayOfWeek.WEDNESDAY),
            createdAtUtcMs = testClock.nowUtcMs() - 86_400_000L * 5
        )
        val monHabit = Habit(
            habitId = "h_mon",
            name = "Monday Planning",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "tasks",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 3,
            scheduleKind = ScheduleKind.SELECTED_WEEKDAYS,
            scheduledWeekdaysMask = WeekdayMask.toMask(setOf(DayOfWeek.MONDAY)),
            scheduleDays = setOf(DayOfWeek.MONDAY),
            createdAtUtcMs = testClock.nowUtcMs() - 86_400_000L * 5
        )
        repository.saveHabitSync(wedHabit, effectiveDateOverride = LocalDate.of(2026, 3, 13))
        repository.saveHabitSync(monHabit, effectiveDateOverride = LocalDate.of(2026, 3, 13))
        viewModel.refreshAllSync()

        val summaries = repository.loadHabitSummariesSync()
        assertTrue(summaries["h_wed"]!!.isScheduledToday)
        assertFalse(summaries["h_mon"]!!.isScheduledToday)

        // Toggle "Not scheduled today" expansion state in ViewModel
        assertFalse(viewModel.todayNotScheduledExpanded.value)
        viewModel.setTodayNotScheduledExpanded(true)
        assertTrue(viewModel.todayNotScheduledExpanded.value)

        // User can still log extra activity on an unscheduled habit today without penalty
        viewModel.logHabitQuantityOrDuration(monHabit, 3, LocalDate.of(2026, 3, 18), "Bonus Wednesday session")
        val updatedMonSummary = repository.loadHabitSummariesSync()["h_mon"]!!
        assertEquals(3, updatedMonSummary.todayTotalUnits.toInt())
        assertEquals(ProgressBand.GOAL_MET, updatedMonSummary.todayProgressBand)
    }

    @Test
    fun t2_08_t2_11_collectedMomentsSummaryMetricsAndDetailRoute() {
        val durHabit = Habit(
            habitId = "h_cm",
            name = "Meditation",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20,
            createdAtUtcMs = testClock.nowUtcMs() - 86_400_000L * 5
        )
        val checkHabit = Habit(
            habitId = "h_cm_check",
            name = "Gratitude",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.DAY,
            createdAtUtcMs = testClock.nowUtcMs() - 86_400_000L * 5
        )
        repository.saveHabitSync(durHabit)
        repository.saveHabitSync(checkHabit)
        repository.logHabitEntrySync(durHabit.habitId, LocalDate.of(2026, 3, 18), 25, "Morning calm")
        repository.logHabitDayOutcomeSync(checkHabit.habitId, LocalDate.of(2026, 3, 18), "COMPLETED", null)
        viewModel.refreshAllSync()

        val gm = viewModel.growthMetrics.value
        assertEquals(25L, gm.currentMonthDurationMinutes)
        assertEquals(1, gm.totalActiveDates)
        assertTrue(gm.totalHabitsCompleted >= 1)

        viewModel.openCollectedMomentsDetail()
        assertTrue(viewModel.currentSecondaryRoute.value is SecondaryRoute.CollectedMomentsDetail)
    }

    // =========================================================================
    // 3. Habits Directory: Filters, Search & Contiguous Sort Reordering (T2-12 .. T2-15)
    // =========================================================================

    @Test
    fun t2_12_t2_15_habitsDirectoryFiltersSearchAndReordersContiguously() {
        val h1 = Habit(
            habitId = "h_order_1",
            name = "Alpha Habit",
            category = "Morning",
            sortOrder = 0,
            createdAtUtcMs = 1000L
        )
        val h2 = Habit(
            habitId = "h_order_2",
            name = "Beta Habit",
            category = "Evening",
            sortOrder = 1,
            createdAtUtcMs = 2000L
        )
        val h3 = Habit(
            habitId = "h_order_3",
            name = "Gamma Paused",
            lifecycleState = HabitLifecycleState.PAUSED,
            sortOrder = 2,
            createdAtUtcMs = 3000L
        )
        repository.saveHabitSync(h1)
        repository.saveHabitSync(h2)
        repository.saveHabitSync(h3)

        val initialOrder = repository.loadHabitsSync().map { it.habitId }
        assertEquals(listOf("h_order_1", "h_order_2", "h_order_3"), initialOrder)

        // Move Beta Habit up -> becomes index 0
        repository.reorderHabitSync("h_order_2", moveUp = true)
        val reordered = repository.loadHabitsSync()
        assertEquals(listOf("h_order_2", "h_order_1", "h_order_3"), reordered.map { it.habitId })
        assertEquals(listOf(0, 1, 2), reordered.map { it.sortOrder })

        // Boundary check: moving top habit up or bottom habit down leaves order unchanged
        repository.reorderHabitSync("h_order_2", moveUp = true)
        repository.reorderHabitSync("h_order_3", moveUp = false)
        assertEquals(listOf("h_order_2", "h_order_1", "h_order_3"), repository.loadHabitsSync().map { it.habitId })

        // Verify filter and search state on ViewModel
        viewModel.setHabitsFilter(HabitsFilterState.PAUSED)
        assertEquals(HabitsFilterState.PAUSED, viewModel.habitsFilterState.value)
        viewModel.setHabitsSearchQuery("Gamma")
        assertEquals("Gamma", viewModel.habitsSearchQuery.value)
    }

    // =========================================================================
    // 4. Create & Edit Habit, Weekly Targets, D-194 & U-12 Freeze (T2-16 .. T2-27)
    // =========================================================================

    @Test
    fun t2_16_t2_21_createQuantityDurationCompletionOnlyHabitsAndFormatSummaries() {
        val qtyHabit = Habit(
            habitId = "h_create_qty",
            name = "Read Book",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 25,
            scheduleKind = ScheduleKind.DAILY,
            createdAtUtcMs = testClock.nowUtcMs()
        )
        val durHabit = Habit(
            habitId = "h_create_dur",
            name = "Piano Practice",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 150,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
            preferredWeekdaysMask = WeekdayMask.toMask(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)),
            createdAtUtcMs = testClock.nowUtcMs()
        )
        val checkHabit = Habit(
            habitId = "h_create_check",
            name = "Vitamins",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.DAY,
            scheduleKind = ScheduleKind.SELECTED_WEEKDAYS,
            scheduledWeekdaysMask = WeekdayMask.toMask(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
            scheduleDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            createdAtUtcMs = testClock.nowUtcMs()
        )

        repository.saveHabitSync(qtyHabit)
        repository.saveHabitSync(durHabit)
        repository.saveHabitSync(checkHabit)

        val saved = repository.loadHabitsSync().associateBy { it.habitId }
        assertEquals("25 pages / day", saved["h_create_qty"]!!.formatTargetAndPeriodSummary())
        assertEquals("150 min / week", saved["h_create_dur"]!!.formatTargetAndPeriodSummary())
        assertTrue(saved["h_create_check"]!!.formatTargetAndPeriodSummary().contains("check-off / day", ignoreCase = true))
        assertTrue(saved["h_create_dur"]!!.formatScheduleSummary().contains("Flexible weekly"))
        assertTrue(saved["h_create_check"]!!.formatScheduleSummary().contains("Mon"))
    }

    @Test
    fun t2_22_weeklyCompletionOnlyTargetEnforces1To7DaysAndComputesProgress() {
        val weeklyCheckOff = Habit(
            habitId = "h_wk_check",
            name = "Strength Training",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 3,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
            createdAtUtcMs = LocalDate.of(2026, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        repository.saveHabitSync(weeklyCheckOff, effectiveDateOverride = LocalDate.of(2026, 3, 1))

        // Log 2 days in current week (Mon 2026-03-16, Wed 2026-03-18)
        repository.logHabitDayOutcomeSync(weeklyCheckOff.habitId, LocalDate.of(2026, 3, 16), "COMPLETED", null)
        repository.logHabitDayOutcomeSync(weeklyCheckOff.habitId, LocalDate.of(2026, 3, 18), "COMPLETED", null)

        val summaryAfter2 = repository.loadHabitSummariesSync()[weeklyCheckOff.habitId]!!
        assertEquals(2, summaryAfter2.weekTotalUnits.toInt())
        assertEquals(3, summaryAfter2.weekTargetUnits.toInt())
        assertEquals(ProgressBand.HALFWAY, summaryAfter2.weekProgressBand) // 2/3 = 66.6% -> HALFWAY (50%-75%)
        assertTrue(summaryAfter2.isScheduledToday) // Target not yet met -> still scheduled

        // Log 3rd day on 2026-03-17 -> 3/3 = 100% -> GOAL_MET
        repository.logHabitDayOutcomeSync(weeklyCheckOff.habitId, LocalDate.of(2026, 3, 17), "COMPLETED", null)
        val summaryAfter3 = repository.loadHabitSummariesSync()[weeklyCheckOff.habitId]!!
        assertEquals(3, summaryAfter3.weekTotalUnits.toInt())
        assertEquals(3, summaryAfter3.weekTargetUnits.toInt())
        assertEquals(ProgressBand.GOAL_MET, summaryAfter3.weekProgressBand)
        assertEquals(1, summaryAfter3.activeRun?.completedStreakCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun t2_23_weeklyCompletionOnlyRejectsTargetAbove7Days() {
        val invalidHabit = Habit(
            habitId = "h_invalid_wk",
            name = "Invalid 8 Days",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 8,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
        )
        repository.saveHabitSync(invalidHabit)
    }

    @Test
    fun t2_24_u12OptionBWeeklyFreezePreventsMidWeekUnitMixing() {
        // Week starts Monday 2026-03-16; today is Wednesday 2026-03-18
        val habit = Habit(
            habitId = "h_u12",
            name = "Study Session",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 120,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
            createdAtUtcMs = LocalDate.of(2026, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        repository.saveHabitSync(habit, effectiveDateOverride = LocalDate.of(2026, 3, 1))

        // Log 60 min on Monday 2026-03-16 and 30 min on Wednesday 2026-03-18
        repository.logHabitEntrySync(habit.habitId, LocalDate.of(2026, 3, 16), 60, null)
        repository.logHabitEntrySync(habit.habitId, LocalDate.of(2026, 3, 18), 30, null)

        // Mid-week edit on Wednesday 2026-03-18 changing mode from DURATION (120 min) to QUANTITY (4 chapters)
        val editedHabit = habit.copy(
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "chapters",
            targetValue = 4
        )
        repository.saveHabitSync(editedHabit, applyTodayWhenLogged = true)

        // Under U-12 Option B, current week (2026-03-16..2026-03-22) freezes the start-of-week version (2026-03-16: 120 min)
        val summaryCurrentWeek = repository.loadHabitSummariesSync()[habit.habitId]!!
        assertEquals(90, summaryCurrentWeek.weekTotalUnits.toInt())
        assertEquals(120, summaryCurrentWeek.weekTargetUnits.toInt())
        assertEquals(ProgressBand.ALMOST_THERE, summaryCurrentWeek.weekProgressBand) // 90/120 = 75% -> ALMOST_THERE ("Nearly there", 75%..<100%)
        assertEquals("Nearly there", summaryCurrentWeek.weekProgressBand?.label)

        // Advance clock to next week (Monday 2026-03-23) -> new version (4 chapters/week) now governs
        advanceClockToDate(LocalDate.of(2026, 3, 23))
        repository.recomputeAllStreakRunsAndSummariesSync()
        val summaryNextWeek = repository.loadHabitSummariesSync()[habit.habitId]!!
        assertEquals(0, summaryNextWeek.weekTotalUnits.toInt())
        assertEquals(4, summaryNextWeek.weekTargetUnits.toInt())
    }

    @Test
    fun t2_25_t2_27_d194ApplyTodayVsApplyTomorrowAndLegacyEffortAwarePreservation() {
        val habit = Habit(
            habitId = "h_d194",
            name = "Reading",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20,
            createdAtUtcMs = LocalDate.of(2026, 3, 10).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        repository.saveHabitSync(habit, effectiveDateOverride = LocalDate.of(2026, 3, 10))
        repository.logHabitEntrySync(habit.habitId, LocalDate.of(2026, 3, 18), 20, null)

        // Attempting APPLY_TODAY when activity is already logged today throws HabitConfigurationLockedTodayException
        try {
            repository.saveHabitSync(
                habit.copy(targetValue = 40),
                policy = HabitConfigEffectivePolicy.APPLY_TODAY
            )
            fail("Expected HabitConfigurationLockedTodayException when APPLY_TODAY is requested after today's activity")
        } catch (_: HabitConfigurationLockedTodayException) {
            // Expected
        }

        // Edit target to 40 pages with applyTodayWhenLogged = false -> takes effect tomorrow (2026-03-19)
        repository.saveHabitSync(habit.copy(targetValue = 40), applyTodayWhenLogged = false)
        val effToday = repository.getEffectiveHabitVersion(habit.habitId, LocalDate.of(2026, 3, 18))!!
        val effTomorrow = repository.getEffectiveHabitVersion(habit.habitId, LocalDate.of(2026, 3, 19))!!
        assertEquals(20, effToday.targetValue)
        assertEquals(40, effTomorrow.targetValue)
    }

    // =========================================================================
    // 5. Record Session Sheet & D-188 Date Bounds (T2-28 .. T2-31)
    // =========================================================================

    @Test(expected = IllegalArgumentException::class)
    fun t2_28_logHabitEntryRejectsFutureDateUnderD188() {
        val habit = Habit(
            habitId = "h_d188_future",
            name = "Reading",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20,
            createdAtUtcMs = testClock.nowUtcMs() - 86_400_000L * 5
        )
        repository.saveHabitSync(habit, effectiveDateOverride = LocalDate.of(2026, 3, 13))
        // Today is 2026-03-18; logging 2026-03-19 must throw
        repository.logHabitEntrySync(habit.habitId, LocalDate.of(2026, 3, 19), 10, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun t2_29_logHabitEntryRejectsDateBeforeHabitCreationUnderD188() {
        // Created on 2026-03-15
        val createdMs = LocalDate.of(2026, 3, 15).atTime(9, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val habit = Habit(
            habitId = "h_d188_before",
            name = "Journaling",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 2,
            createdAtUtcMs = createdMs
        )
        repository.saveHabitSync(habit, effectiveDateOverride = LocalDate.of(2026, 3, 15))
        // Logging on 2026-03-10 (before 2026-03-15) must throw
        repository.logHabitEntrySync(habit.habitId, LocalDate.of(2026, 3, 10), 2, null)
    }

    @Test
    fun t2_30_t2_31_recordSessionZeroCheckInAndMultipleSessionEditing() {
        val habit = Habit(
            habitId = "h_zero_check",
            name = "Evening Piano",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20,
            createdAtUtcMs = testClock.nowUtcMs() - 86_400_000L * 5
        )
        repository.saveHabitSync(habit, effectiveDateOverride = LocalDate.of(2026, 3, 13))

        val (zeroEntry, _) = repository.logHabitEntrySync(habit.habitId, LocalDate.of(2026, 3, 18), 0, "Travelling today")
        val status = repository.evaluateDerivedDayStatus(habit.habitId, LocalDate.of(2026, 3, 18))
        assertEquals(DerivedDayStatus.LOGGED_ZERO, status)
        val summary = repository.loadHabitSummariesSync()[habit.habitId]!!
        assertEquals(ProgressBand.ZERO, summary.todayProgressBand)

        // Update the existing entry to 15 min and add a second 10 min entry -> total 25 min (EXTRA)
        repository.updateHabitEntrySync(zeroEntry.entryId, 15, "Practised after arriving")
        repository.logHabitEntrySync(habit.habitId, LocalDate.of(2026, 3, 18), 10, "Encore")
        val updatedSummary = repository.loadHabitSummariesSync()[habit.habitId]!!
        assertEquals(25, updatedSummary.todayTotalUnits.toInt())
        assertEquals(ProgressBand.EXTRA, updatedSummary.todayProgressBand)
    }

    // =========================================================================
    // 6. Guarded 8-Second Undo: Session Delete, Habit Delete, Single-Use & Expiry (T2-32 .. T2-37)
    // =========================================================================

    @Test
    fun t2_32_deleteIndividualSessionAndUndoRestoresSessionAndRecomputesSummary() {
        val habit = Habit(
            habitId = "h_undo_sess",
            name = "Hydration",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "glasses",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 8,
            createdAtUtcMs = testClock.nowUtcMs() - 86_400_000L * 5
        )
        repository.saveHabitSync(habit, effectiveDateOverride = LocalDate.of(2026, 3, 13))
        repository.logHabitEntrySync(habit.habitId, LocalDate.of(2026, 3, 18), 3, "Morning")
        repository.logHabitEntrySync(habit.habitId, LocalDate.of(2026, 3, 18), 5, "Afternoon")

        val entriesBefore = repository.loadHabitEntriesSync().filter { it.habitId == habit.habitId }
        assertEquals(2, entriesBefore.size)
        assertEquals(8, repository.loadHabitSummariesSync()[habit.habitId]!!.todayTotalUnits.toInt())

        val secondEntry = entriesBefore.single { it.valueUnits == 5 }
        val deleteToken = repository.deleteHabitEntryWithUndoSync(secondEntry.entryId)
        assertNotNull(deleteToken)

        // After delete, only 3 glasses remain
        assertEquals(3, repository.loadHabitSummariesSync()[habit.habitId]!!.todayTotalUnits.toInt())

        // Execute Undo within 8 seconds -> restores the deleted 5-glass session
        val undoSuccess = repository.executeUndoSync(deleteToken!!)
        assertTrue(undoSuccess)
        assertEquals(8, repository.loadHabitSummariesSync()[habit.habitId]!!.todayTotalUnits.toInt())
        assertEquals(2, repository.loadHabitEntriesSync().count { it.habitId == habit.habitId })

        // Single-use token enforcement: executing the same token a second time must return false
        val secondAttempt = repository.executeUndoSync(deleteToken)
        assertFalse(secondAttempt)
    }

    @Test
    fun t2_33_undoDeletedSessionRejectsWhenSameDateRestDayConflictExists() {
        val habit = Habit(
            habitId = "h_undo_conflict",
            name = "Morning Stretch",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 15,
            createdAtUtcMs = testClock.nowUtcMs() - 86_400_000L * 5
        )
        repository.saveHabitSync(habit, effectiveDateOverride = LocalDate.of(2026, 3, 13))
        repository.logHabitEntrySync(habit.habitId, LocalDate.of(2026, 3, 18), 15, null)
        val entry = repository.loadHabitEntriesSync().single { it.habitId == habit.habitId }

        val deleteToken = repository.deleteHabitEntryWithUndoSync(entry.entryId)!!
        // Now log a REST_DAY on 2026-03-18 before undoing
        repository.setRestDayExceptionSync(habit.habitId, LocalDate.of(2026, 3, 18))

        // Undoing the deleted session must fail safely because a REST_DAY now exists on that date
        assertFalse(repository.executeUndoSync(deleteToken))
    }

    @Test
    fun t2_34_t2_37_permanentHabitDeletionPreservesJournalAndSupports8SecondUndoAndExpiry() {
        val habit = Habit(
            habitId = "h_del_perm",
            name = "Deep Work",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 60,
            createdAtUtcMs = testClock.nowUtcMs() - 86_400_000L * 5
        )
        repository.saveHabitSync(habit, effectiveDateOverride = LocalDate.of(2026, 3, 13))
        repository.logHabitEntrySync(habit.habitId, LocalDate.of(2026, 3, 18), 60, "Focused block")

        val journal = JournalEntry(
            journalEntryId = "j_linked_1",
            localDate = "2026-03-18",
            title = "Great focus day",
            body = "Completed my deep work block calmly.",
            primaryMoodCode = "CALM",
            linkedHabitId = habit.habitId
        )
        repository.saveJournalEntrySync(journal)

        // Permanently delete habit
        val undoToken = repository.deleteHabitWithUndoSync(habit.habitId)
        assertNotNull(undoToken)
        assertTrue(repository.loadHabitsSync().none { it.habitId == habit.habitId })

        // Journal entry must still exist with linkedHabitId = null
        val journalAfterDelete = repository.loadJournalEntriesSync().single { it.journalEntryId == "j_linked_1" }
        assertNull(journalAfterDelete.linkedHabitId)

        // Advance clock by 4 seconds (within 8s window) and Undo
        testClock.currentUtcMs += 4_000L
        assertTrue(repository.executeUndoSync(undoToken!!))

        // Habit, entries, and journal link are all restored
        assertTrue(repository.loadHabitsSync().any { it.habitId == habit.habitId })
        assertEquals(60, repository.loadHabitSummariesSync()[habit.habitId]!!.todayTotalUnits.toInt())
        val journalAfterUndo = repository.loadJournalEntriesSync().single { it.journalEntryId == "j_linked_1" }
        assertEquals(habit.habitId, journalAfterUndo.linkedHabitId)

        // Delete again and advance past 8-second window -> Undo must fail
        val expiredToken = repository.deleteHabitWithUndoSync(habit.habitId)!!
        testClock.currentUtcMs += 8_001L
        assertFalse(repository.executeUndoSync(expiredToken))
    }

    // =========================================================================
    // 7. Individual Streaks, Living Grove Qualification & Dormant Acknowledgment (T2-38 .. T2-51)
    // =========================================================================

    @Test
    fun t2_38_groveSeparates7DayPermanentTreesFromShortRunsAndPreservesDormantTreeOnRestart() {
        val startMs = LocalDate.of(2026, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val habit = Habit(
            habitId = "h_grove_lifecycle",
            name = "Daily Sketch",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.DAY,
            scheduleKind = ScheduleKind.DAILY,
            createdAtUtcMs = startMs
        )
        repository.saveHabitSync(habit, effectiveDateOverride = LocalDate.of(2026, 3, 1))

        // Run 1: 3-day short run (2026-03-01 .. 2026-03-03), then missed 2026-03-04
        for (day in 1..3) {
            repository.logHabitDayOutcomeSync(
                habit.habitId,
                LocalDate.of(2026, 3, day),
                "COMPLETED",
                null
            )
        }

        // Run 2: 7-day qualified run (2026-03-06 .. 2026-03-12), then missed 2026-03-13
        for (day in 6..12) {
            repository.logHabitDayOutcomeSync(
                habit.habitId,
                LocalDate.of(2026, 3, day),
                "COMPLETED",
                null
            )
        }

        // Run 3: New active sapling (2026-03-17 .. 2026-03-18, 2 days)
        for (day in 17..18) {
            repository.logHabitDayOutcomeSync(
                habit.habitId,
                LocalDate.of(2026, 3, day),
                "COMPLETED",
                null
            )
        }

        val runs = repository.loadStreakRunsSync().filter { it.habitId == habit.habitId }
        assertEquals(3, runs.size)

        val shortRun = runs.single { it.startLocalDate == "2026-03-01" }
        assertFalse(shortRun.qualifiedForGrove)
        assertFalse(shortRun.isActive)
        assertEquals(GroveTreeState.UNQUALIFIED_HISTORY, shortRun.treeState)

        val dormantTree = runs.single { it.startLocalDate == "2026-03-06" }
        assertTrue(dormantTree.qualifiedForGrove)
        assertFalse(dormantTree.isActive)
        assertEquals(GroveTreeState.DORMANT_TREE, dormantTree.treeState)
        assertEquals(GroveTerminalReason.MISSED, dormantTree.terminalReason)

        val activeSapling = runs.single { it.startLocalDate == "2026-03-17" }
        assertFalse(activeSapling.qualifiedForGrove)
        assertTrue(activeSapling.isActive)
        assertEquals(GroveTreeState.SAPLING_ACTIVE, activeSapling.treeState)
        assertEquals(2, activeSapling.completedStreakCount)

        // Verify one-shot dormant transition acknowledgment
        viewModel.refreshAllSync()
        val unackedBefore = viewModel.getUnacknowledgedDormantRunForHabit(habit.habitId)
        assertNotNull(unackedBefore)
        assertEquals(dormantTree.runId, unackedBefore!!.runId)

        viewModel.acknowledgeDormantTransition(dormantTree.runId)
        val unackedAfter = viewModel.getUnacknowledgedDormantRunForHabit(habit.habitId)
        assertNull(unackedAfter)
    }

    @Test
    fun t2_39_habitDetailDateSelectionRestDayEligibilityAndLifecycleControls() {
        val startMs = LocalDate.of(2026, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val habit = Habit(
            habitId = "h_detail_lifecycle",
            name = "Morning Pages",
            description = "Three pages of free writing",
            category = "Writing",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 3,
            lifetimeGoalDays = 90,
            createdAtUtcMs = startMs
        )
        repository.saveHabitSync(habit, effectiveDateOverride = LocalDate.of(2026, 3, 1))

        // Today (2026-03-18) has no activity yet -> eligible for one-off rest day
        val initialSummary = repository.loadHabitSummariesSync()[habit.habitId]!!
        assertTrue(initialSummary.canTakeRestDayToday)

        // Take rest day today -> sets REST_DAY exception
        viewModel.takeRestDayToday(habit, "Resting eyes today")
        val afterRestSummary = repository.loadHabitSummariesSync()[habit.habitId]!!
        assertTrue(afterRestSummary.hasRestDayToday)
        assertEquals(DerivedDayStatus.REST_DAY, repository.evaluateDerivedDayStatus(habit.habitId, LocalDate.of(2026, 3, 18)))

        // Logging real activity on the same date clears REST_DAY automatically
        viewModel.logHabitQuantityOrDuration(habit, 3, LocalDate.of(2026, 3, 18), "Wrote three pages")
        val afterLogSummary = repository.loadHabitSummariesSync()[habit.habitId]!!
        assertFalse(afterLogSummary.hasRestDayToday)
        assertEquals(3, afterLogSummary.todayTotalUnits.toInt())

        // Pause -> Resume -> Complete -> Archive lifecycle transitions
        viewModel.updateHabitLifecycle(habit.habitId, HabitLifecycleState.PAUSED)
        assertEquals(HabitLifecycleState.PAUSED, repository.loadHabitsSync().single { it.habitId == habit.habitId }.lifecycleState)
        viewModel.updateHabitLifecycle(habit.habitId, HabitLifecycleState.ACTIVE)
        assertEquals(HabitLifecycleState.ACTIVE, repository.loadHabitsSync().single { it.habitId == habit.habitId }.lifecycleState)
        viewModel.updateHabitLifecycle(habit.habitId, HabitLifecycleState.COMPLETED)
        assertEquals(HabitLifecycleState.COMPLETED, repository.loadHabitsSync().single { it.habitId == habit.habitId }.lifecycleState)
        viewModel.updateHabitLifecycle(habit.habitId, HabitLifecycleState.ARCHIVED)
        assertEquals(HabitLifecycleState.ARCHIVED, repository.loadHabitsSync().single { it.habitId == habit.habitId }.lifecycleState)
    }

    @Test
    fun t2_40_t2_51_reducedMotionAndIdempotentQuickTapGuards() {
        // Verify reduced motion reads Settings.Global.ANIMATOR_DURATION_SCALE = 0f
        Settings.Global.putFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        assertTrue(viewModel.isReducedMotionEnabled())
        Settings.Global.putFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        assertFalse(viewModel.isReducedMotionEnabled())

        // Verify rapid duplicate quick-tap guard in ViewModel (within 350ms window)
        val habit = Habit(
            habitId = "h_idemp",
            name = "Pushups",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "reps",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 30,
            createdAtUtcMs = testClock.nowUtcMs() - 86_400_000L * 5
        )
        repository.saveHabitSync(habit, effectiveDateOverride = LocalDate.of(2026, 3, 13))

        // Two rapid taps within 100ms on the same quick action reuse deterministic idempotency key
        viewModel.logHabitQuantityOrDuration(habit, 10, LocalDate.of(2026, 3, 18))
        testClock.currentUtcMs += 100L
        viewModel.logHabitQuantityOrDuration(habit, 10, LocalDate.of(2026, 3, 18))
        assertEquals(10, repository.loadHabitSummariesSync()[habit.habitId]!!.todayTotalUnits.toInt())

        // Deliberate repeat tap after 500ms (> 350ms guard) logs a legitimate second session
        testClock.currentUtcMs += 500L
        viewModel.logHabitQuantityOrDuration(habit, 10, LocalDate.of(2026, 3, 18))
        assertEquals(20, repository.loadHabitSummariesSync()[habit.habitId]!!.todayTotalUnits.toInt())

        // Verify every ProgressBand has distinct non-empty encouragementQuote and motivationQuote
        for (band in ProgressBand.entries) {
            assertTrue(band.encouragementQuote.isNotBlank())
            assertTrue(band.motivationQuote.isNotBlank())
            assertNotEquals(band.encouragementQuote, band.motivationQuote)
        }
    }
}
