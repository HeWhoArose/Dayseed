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
import com.example.dayseed.ui.screens.newlyEndedRunIds
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
import java.math.BigInteger
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

        // Now give weeklyHabit a 4-week active streak by logging 2 sessions in each earlier week (Feb 23..Mar 1 and Mar 2..Mar 8 -> elapsed span = 24 days)
        repository.logHabitEntrySync(weeklyHabit.habitId, LocalDate.of(2026, 2, 24), 1, null)
        repository.logHabitEntrySync(weeklyHabit.habitId, LocalDate.of(2026, 2, 26), 1, null)
        repository.logHabitEntrySync(weeklyHabit.habitId, LocalDate.of(2026, 3, 3), 1, null)
        repository.logHabitEntrySync(weeklyHabit.habitId, LocalDate.of(2026, 3, 5), 1, null)

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

    // =========================================================================
    // 8. Phase 2 Targeted Remediation Regression Tests (Corrections A, B, C, D)
    // =========================================================================

    @Test
    fun t2_c01_correctionA_createEditHabitSaveCancelDeleteAndFailedSaveErrorState() {
        // 1. Create from Today -> Save pops once and returns to Today
        viewModel.selectTab(NavTab.TODAY)
        viewModel.openCreateEditHabit(null)
        assertTrue(viewModel.currentSecondaryRoute.value is SecondaryRoute.CreateEditHabit)

        val habitFromToday = Habit(
            habitId = "h_corr_a_today",
            name = "Morning Walk",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20
        )
        val saved1 = viewModel.saveHabit(habitFromToday)
        assertNotNull(saved1)
        assertEquals(null, viewModel.currentSecondaryRoute.value)
        assertEquals(NavTab.TODAY, viewModel.selectedTab.value)
        assertNull(viewModel.lastSaveError.value)

        // 2. Create from Habits -> Save pops once and returns to Habits
        viewModel.selectTab(NavTab.HABITS)
        viewModel.openCreateEditHabit(null)
        assertTrue(viewModel.currentSecondaryRoute.value is SecondaryRoute.CreateEditHabit)

        val habitFromHabits = Habit(
            habitId = "h_corr_a_habits",
            name = "Evening Reading",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 15
        )
        val saved2 = viewModel.saveHabit(habitFromHabits)
        assertNotNull(saved2)
        assertEquals(null, viewModel.currentSecondaryRoute.value)
        assertEquals(NavTab.HABITS, viewModel.selectedTab.value)

        // 3. HabitDetail -> Edit -> Save pops CreateEditHabit once and returns to HabitDetail for same habitId
        viewModel.openHabitDetail(habitFromHabits.habitId)
        assertTrue(viewModel.currentSecondaryRoute.value is SecondaryRoute.HabitDetail)
        viewModel.openEditHabit(habitFromHabits)
        assertTrue(viewModel.currentSecondaryRoute.value is SecondaryRoute.CreateEditHabit)

        val savedEdit = viewModel.saveHabit(habitFromHabits.copy(description = "Read before sleep"))
        assertNotNull(savedEdit)
        val routeAfterSave = viewModel.currentSecondaryRoute.value
        assertTrue(
            "Expected SecondaryRoute.HabitDetail after saving from HabitDetail -> Edit, but was $routeAfterSave",
            routeAfterSave is SecondaryRoute.HabitDetail && routeAfterSave.habitId == habitFromHabits.habitId
        )

        // 4. HabitDetail -> Edit -> Cancel pops CreateEditHabit once and returns to HabitDetail
        viewModel.openEditHabit(habitFromHabits)
        assertTrue(viewModel.currentSecondaryRoute.value is SecondaryRoute.CreateEditHabit)
        viewModel.closeAddEditHabit()
        val routeAfterCancel = viewModel.currentSecondaryRoute.value
        assertTrue(routeAfterCancel is SecondaryRoute.HabitDetail && routeAfterCancel.habitId == habitFromHabits.habitId)

        // 5. Failed save keeps CreateEditHabit open and populates lastSaveError / lastErrorMessage
        repository.logHabitEntrySync(habitFromHabits.habitId, testClock.todayLocal(), 15, null)
        viewModel.openEditHabit(habitFromHabits)
        assertTrue(viewModel.currentSecondaryRoute.value is SecondaryRoute.CreateEditHabit)

        val failedSave = viewModel.saveHabit(
            habitFromHabits.copy(targetValue = 30),
            applyTodayWhenLogged = true
        )
        assertNull(failedSave)
        assertTrue(viewModel.currentSecondaryRoute.value is SecondaryRoute.CreateEditHabit)
        assertNotNull(viewModel.lastSaveError.value)
        assertNotNull(viewModel.lastErrorMessage.value)

        // Clearing error and retrying with Apply Tomorrow succeeds and returns to HabitDetail
        viewModel.clearSaveError()
        assertNull(viewModel.lastSaveError.value)
        val retrySave = viewModel.saveHabit(
            habitFromHabits.copy(targetValue = 30),
            applyTodayWhenLogged = false
        )
        assertNotNull(retrySave)
        val routeAfterRetry = viewModel.currentSecondaryRoute.value
        assertTrue(routeAfterRetry is SecondaryRoute.HabitDetail && routeAfterRetry.habitId == habitFromHabits.habitId)

        // 6. Delete from HabitDetail -> Edit removes both routes for the deleted habit and synchronizes currentSecondaryRoute
        viewModel.openEditHabit(habitFromHabits)
        assertEquals(2, viewModel.secondaryRouteStack.value.size)
        viewModel.deleteHabit(habitFromHabits.habitId)
        assertTrue(viewModel.secondaryRouteStack.value.isEmpty())
        assertEquals(null, viewModel.currentSecondaryRoute.value)
        assertEquals(NavTab.HABITS, viewModel.selectedTab.value)
        assertNotNull(viewModel.activeUndoToken.value)

        // The deletion Undo remains available after navigation returns to Habits.
        viewModel.triggerUndo()
        assertTrue(repository.loadHabitsSync().any { it.habitId == habitFromHabits.habitId })
        assertNull(viewModel.activeUndoToken.value)
    }

    @Test
    fun t2_c02_correctionB_d194PositiveZeroAndOutcomeLocksRejectApplyTodayWithoutSilentRetryAndSupportThreeEffectiveDateCases() {
        val today = testClock.todayLocal() // Wednesday 2026-03-18
        val tomorrow = today.plusDays(1)   // Thursday 2026-03-19

        // 1. Positive activity recorded today -> locks today's version; Apply Today rejected without silent retry
        val positiveHabit = Habit(
            habitId = "h_corr_b_pos",
            name = "Water Intake",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "glasses",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 8,
            createdAtUtcMs = LocalDate.of(2026, 3, 10).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        repository.saveHabitSync(positiveHabit, effectiveDateOverride = LocalDate.of(2026, 3, 10))
        repository.logHabitEntrySync(positiveHabit.habitId, today, 5, null)
        assertTrue(repository.hasRecordedActivityForVersionLockOnDate(positiveHabit.habitId, today))

        val rejectedPos = viewModel.saveHabit(positiveHabit.copy(targetValue = 12), applyTodayWhenLogged = true)
        assertNull(rejectedPos)
        assertNotNull(viewModel.lastSaveError.value)
        // Verify no silent retry created a tomorrow version
        assertEquals(8, repository.getEffectiveHabitVersion(positiveHabit.habitId, today)?.targetValue)
        assertEquals(8, repository.getEffectiveHabitVersion(positiveHabit.habitId, tomorrow)?.targetValue)
        assertEquals(5, repository.getEntriesForHabitOnDate(positiveHabit.habitId, today).single().valueUnits)

        // 2. Explicit zero activity recorded today -> also locks today's version; Apply Today rejected without silent retry
        val zeroHabit = Habit(
            habitId = "h_corr_b_zero",
            name = "Vocabulary Cards",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "cards",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20,
            createdAtUtcMs = LocalDate.of(2026, 3, 10).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        repository.saveHabitSync(zeroHabit, effectiveDateOverride = LocalDate.of(2026, 3, 10))
        repository.logHabitEntrySync(zeroHabit.habitId, today, 0, "Honest zero check-in")
        assertFalse(repository.hasQualifyingActivityOnDate(zeroHabit.habitId, today))
        assertTrue(repository.hasRecordedActivityForVersionLockOnDate(zeroHabit.habitId, today))

        val rejectedZero = viewModel.saveHabit(zeroHabit.copy(targetValue = 35), applyTodayWhenLogged = true)
        assertNull(rejectedZero)
        assertNotNull(viewModel.lastSaveError.value)
        assertEquals(20, repository.getEffectiveHabitVersion(zeroHabit.habitId, today)?.targetValue)
        assertEquals(20, repository.getEffectiveHabitVersion(zeroHabit.habitId, tomorrow)?.targetValue)
        assertEquals(0, repository.getEntriesForHabitOnDate(zeroHabit.habitId, today).single().valueUnits)

        // 3. Recorded day outcome today -> locks today's version; Apply Today rejected without silent retry
        val outcomeHabit = Habit(
            habitId = "h_corr_b_out",
            name = "Stretch Routine",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 3,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
            createdAtUtcMs = LocalDate.of(2026, 3, 10).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        repository.saveHabitSync(outcomeHabit, effectiveDateOverride = LocalDate.of(2026, 3, 10))
        repository.logHabitDayOutcomeSync(outcomeHabit.habitId, today, "COMPLETED", null)
        assertTrue(repository.hasRecordedActivityForVersionLockOnDate(outcomeHabit.habitId, today))

        val rejectedOut = viewModel.saveHabit(outcomeHabit.copy(targetValue = 5), applyTodayWhenLogged = true)
        assertNull(rejectedOut)
        assertNotNull(viewModel.lastSaveError.value)
        assertEquals(3, repository.getEffectiveHabitVersion(outcomeHabit.habitId, today)?.targetValue)

        // 4. Ordinary Daily Apply Tomorrow succeeds with effectiveFromLocalDate == tomorrow and preserves today's version/activity
        val appliedTomorrowDaily = viewModel.saveHabit(zeroHabit.copy(targetValue = 35), applyTodayWhenLogged = false)
        assertNotNull(appliedTomorrowDaily)
        assertEquals(tomorrow.toString(), appliedTomorrowDaily!!.effectiveFromLocalDate)
        assertEquals(20, repository.getEffectiveHabitVersion(zeroHabit.habitId, today)?.targetValue)
        assertEquals(35, repository.getEffectiveHabitVersion(zeroHabit.habitId, tomorrow)?.targetValue)
        assertEquals(0, repository.getEntriesForHabitOnDate(zeroHabit.habitId, today).single().valueUnits)

        // 5. Weekly same-unit target change takes effect tomorrow while current week retains frozen start-of-week version
        val appliedTomorrowWeeklySameUnit = viewModel.saveHabit(
            outcomeHabit.copy(targetValue = 5),
            applyTodayWhenLogged = false
        )
        assertNotNull(appliedTomorrowWeeklySameUnit)
        assertEquals(tomorrow.toString(), appliedTomorrowWeeklySameUnit!!.effectiveFromLocalDate)
        val currentWeekSummary = repository.loadHabitSummariesSync()[outcomeHabit.habitId]!!
        assertEquals(BigInteger.valueOf(3), currentWeekSummary.weekTargetUnits)

        // 6. Weekly unit or tracking-mode change is deferred to the next weekly boundary (Monday 2026-03-23)
        val weeklyModeChange = viewModel.saveHabit(
            outcomeHabit.copy(
                trackingMode = TrackingMode.DURATION,
                unitCode = "min",
                targetValue = 90
            ),
            applyTodayWhenLogged = false
        )
        assertNotNull(weeklyModeChange)
        assertEquals("2026-03-23", weeklyModeChange!!.effectiveFromLocalDate)
    }

    @Test
    fun t2_c03_correctionC_weeklySessionCountHabitCountsPositiveSessionsEditsDeletesUndosAndFreezesWeekConfig() {
        // Week starts Monday 2026-03-16; today is Wednesday 2026-03-18
        val sessionHabit = Habit(
            habitId = "h_corr_c_sessions",
            name = "Gym Workout",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "sessions",
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 3,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
            createdAtUtcMs = LocalDate.of(2026, 3, 16).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        repository.saveHabitSync(sessionHabit, effectiveDateOverride = LocalDate.of(2026, 3, 16))

        // 1. Log positive session with valueUnits = 20 on Monday and valueUnits = 20 on Tuesday -> counts as 2 sessions (not 40!)
        repository.logHabitEntrySync(sessionHabit.habitId, LocalDate.of(2026, 3, 16), 20, "Upper body")
        val (tueEntry1, _) = repository.logHabitEntrySync(sessionHabit.habitId, LocalDate.of(2026, 3, 17), 20, "Lower body")

        val summaryAfter2 = repository.loadHabitSummariesSync()[sessionHabit.habitId]!!
        assertEquals(BigInteger.valueOf(2), summaryAfter2.weekTotalUnits)
        assertEquals(BigInteger.valueOf(3), summaryAfter2.weekTargetUnits)
        assertEquals(66, summaryAfter2.weekPercentage)
        assertEquals(ProgressBand.HALFWAY, summaryAfter2.weekProgressBand)
        assertEquals(
            DerivedDayStatus.PARTIAL,
            repository.evaluateDerivedDayStatus(sessionHabit.habitId, LocalDate.of(2026, 3, 17))
        )

        // 2. Multiple positive entries on the same date count separately: add 2nd entry on Tuesday (valueUnits = 15) -> 3 / 3 sessions
        val (tueEntry2, _) = repository.logHabitEntrySync(sessionHabit.habitId, LocalDate.of(2026, 3, 17), 15, "Cardio")
        val summaryAfter3 = repository.loadHabitSummariesSync()[sessionHabit.habitId]!!
        assertEquals(BigInteger.valueOf(3), summaryAfter3.weekTotalUnits)
        assertEquals(100, summaryAfter3.weekPercentage)
        assertEquals(ProgressBand.GOAL_MET, summaryAfter3.weekProgressBand)
        assertEquals(
            DerivedDayStatus.COMPLETED,
            repository.evaluateDerivedDayStatus(sessionHabit.habitId, LocalDate.of(2026, 3, 17))
        )
        assertEquals(1, summaryAfter3.activeRun?.completedStreakCount)

        // 3. Explicit zero entry on Wednesday remains recorded (LOGGED_ZERO) but adds 0 to completed sessions
        repository.logHabitEntrySync(sessionHabit.habitId, LocalDate.of(2026, 3, 18), 0, "Rested today")
        val summaryAfterZero = repository.loadHabitSummariesSync()[sessionHabit.habitId]!!
        assertEquals(BigInteger.valueOf(3), summaryAfterZero.weekTotalUnits)
        assertEquals(
            DerivedDayStatus.LOGGED_ZERO,
            repository.evaluateDerivedDayStatus(sessionHabit.habitId, LocalDate.of(2026, 3, 18))
        )

        // 4. Editing, deleting, and undoing sessions recalculate weekly session counts accurately
        // Edit tueEntry1 from 20 to 30 -> still 3 positive sessions
        repository.updateHabitEntrySync(tueEntry1.entryId, 30, "Extended lower body")
        assertEquals(BigInteger.valueOf(3), repository.loadHabitSummariesSync()[sessionHabit.habitId]!!.weekTotalUnits)

        // Edit tueEntry2 from 15 to 0 -> drops to 2 positive sessions
        repository.updateHabitEntrySync(tueEntry2.entryId, 0, "Zeroed")
        assertEquals(BigInteger.valueOf(2), repository.loadHabitSummariesSync()[sessionHabit.habitId]!!.weekTotalUnits)

        // Restore tueEntry2 to 10 -> back to 3 positive sessions
        repository.updateHabitEntrySync(tueEntry2.entryId, 10, "Restored")
        assertEquals(BigInteger.valueOf(3), repository.loadHabitSummariesSync()[sessionHabit.habitId]!!.weekTotalUnits)

        // Delete tueEntry2 with Undo -> drops to 2 / 3 sessions
        val delToken = repository.deleteHabitEntryWithUndoSync(tueEntry2.entryId)
        assertNotNull(delToken)
        assertEquals(BigInteger.valueOf(2), repository.loadHabitSummariesSync()[sessionHabit.habitId]!!.weekTotalUnits)

        // Execute Undo -> returns to 3 / 3 sessions
        assertTrue(repository.executeUndoSync(delToken!!))
        assertEquals(BigInteger.valueOf(3), repository.loadHabitSummariesSync()[sessionHabit.habitId]!!.weekTotalUnits)

        // 5. Midweek configuration change (U-12 Option B) & per-day lifecycle independence
        repository.saveHabitSync(
            sessionHabit.copy(targetValue = 5),
            applyTodayWhenLogged = false
        )
        advanceClockToDate(LocalDate.of(2026, 3, 19)) // Thursday 2026-03-19 (where new version target=5 is effective for daily lookups)
        repository.recomputeAllStreakRunsAndSummariesSync()
        val summaryThu = repository.loadHabitSummariesSync()[sessionHabit.habitId]!!
        // Frozen Monday version (3 sessions/week) still governs the current week
        assertEquals(BigInteger.valueOf(3), summaryThu.weekTotalUnits)
        assertEquals(BigInteger.valueOf(3), summaryThu.weekTargetUnits)
        assertEquals(ProgressBand.GOAL_MET, summaryThu.weekProgressBand)
        assertEquals(
            DerivedDayStatus.OPEN_TODAY,
            repository.evaluateDerivedDayStatus(sessionHabit.habitId, LocalDate.of(2026, 3, 19))
        )

        // Pausing on Friday 2026-03-20 sets Friday to INACTIVE_LIFECYCLE without altering Tuesday's historical COMPLETED status
        advanceClockToDate(LocalDate.of(2026, 3, 20))
        repository.updateHabitLifecycleSync(sessionHabit.habitId, HabitLifecycleState.PAUSED, LocalDate.of(2026, 3, 20))
        assertEquals(
            DerivedDayStatus.INACTIVE_LIFECYCLE,
            repository.evaluateDerivedDayStatus(sessionHabit.habitId, LocalDate.of(2026, 3, 20))
        )
        assertEquals(
            DerivedDayStatus.COMPLETED,
            repository.evaluateDerivedDayStatus(sessionHabit.habitId, LocalDate.of(2026, 3, 17))
        )

        // 6. Non-session QUANTITY ("pages") and DURATION ("min") continue to sum valueUnits
        val pagesHabit = Habit(
            habitId = "h_corr_c_pages",
            name = "Weekly Reading",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 100,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
            createdAtUtcMs = LocalDate.of(2026, 3, 16).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        repository.saveHabitSync(pagesHabit, effectiveDateOverride = LocalDate.of(2026, 3, 16))
        repository.logHabitEntrySync(pagesHabit.habitId, LocalDate.of(2026, 3, 16), 20, null)
        repository.logHabitEntrySync(pagesHabit.habitId, LocalDate.of(2026, 3, 17), 30, null)
        val pagesSummary = repository.loadHabitSummariesSync()[pagesHabit.habitId]!!
        assertEquals(BigInteger.valueOf(50), pagesSummary.weekTotalUnits)
        assertEquals(ProgressBand.HALFWAY, pagesSummary.weekProgressBand)

        val durationHabit = Habit(
            habitId = "h_corr_c_min",
            name = "Weekly Meditation",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 60,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
            createdAtUtcMs = LocalDate.of(2026, 3, 16).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        repository.saveHabitSync(durationHabit, effectiveDateOverride = LocalDate.of(2026, 3, 16))
        repository.logHabitEntrySync(durationHabit.habitId, LocalDate.of(2026, 3, 16), 20, null)
        repository.logHabitEntrySync(durationHabit.habitId, LocalDate.of(2026, 3, 17), 25, null)
        val durationSummary = repository.loadHabitSummariesSync()[durationHabit.habitId]!!
        assertEquals(BigInteger.valueOf(45), durationSummary.weekTotalUnits)
        assertEquals(ProgressBand.ALMOST_THERE, durationSummary.weekProgressBand)
    }

    @Test
    fun t2_c04_correctionD_flexibleWeeklySchedulingBeforeAndAfterTargetMetBonusLoggingAndUnchangedDailySelectedWeekdays() {
        val mwfMask = WeekdayMask.toMask(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))

        val flexHabit = Habit(
            habitId = "h_corr_d_flex",
            name = "Pilates",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "sessions",
            targetPeriod = TargetPeriod.WEEK,
            targetValue = 3,
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
            preferredWeekdaysMask = mwfMask,
            scheduleDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            createdAtUtcMs = LocalDate.of(2026, 3, 16).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        val dailyHabit = Habit(
            habitId = "h_corr_d_daily",
            name = "Daily Gratitude",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.DAY,
            scheduleKind = ScheduleKind.DAILY,
            createdAtUtcMs = LocalDate.of(2026, 3, 16).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        val selectedWeekdaysHabit = Habit(
            habitId = "h_corr_d_mwf",
            name = "MWF Run",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.DAY,
            scheduleKind = ScheduleKind.SELECTED_WEEKDAYS,
            scheduledWeekdaysMask = mwfMask,
            scheduleDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            createdAtUtcMs = LocalDate.of(2026, 3, 16).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        repository.saveHabitSync(flexHabit, effectiveDateOverride = LocalDate.of(2026, 3, 16))
        repository.saveHabitSync(dailyHabit, effectiveDateOverride = LocalDate.of(2026, 3, 16))
        repository.saveHabitSync(selectedWeekdaysHabit, effectiveDateOverride = LocalDate.of(2026, 3, 16))

        // 1. Before reaching weekly target (0 / 3 on Monday, 2 / 3 on Tuesday):
        // Scheduled on Monday (preferred) AND Tuesday (non-preferred)
        advanceClockToDate(LocalDate.of(2026, 3, 16)) // Monday (preferred)
        repository.recomputeAllStreakRunsAndSummariesSync()
        assertTrue(repository.loadHabitSummariesSync()[flexHabit.habitId]!!.isScheduledToday)
        assertTrue(repository.loadHabitSummariesSync()[dailyHabit.habitId]!!.isScheduledToday)
        assertTrue(repository.loadHabitSummariesSync()[selectedWeekdaysHabit.habitId]!!.isScheduledToday)

        repository.logHabitEntrySync(flexHabit.habitId, LocalDate.of(2026, 3, 16), 1, null)
        advanceClockToDate(LocalDate.of(2026, 3, 17)) // Tuesday (non-preferred)
        repository.logHabitEntrySync(flexHabit.habitId, LocalDate.of(2026, 3, 17), 1, null) // 2 / 3 sessions
        repository.recomputeAllStreakRunsAndSummariesSync()
        assertTrue(
            "Incomplete flexible weekly habit (2/3) must remain scheduled on non-preferred Tuesday",
            repository.loadHabitSummariesSync()[flexHabit.habitId]!!.isScheduledToday
        )
        assertTrue(repository.loadHabitSummariesSync()[dailyHabit.habitId]!!.isScheduledToday)
        assertFalse(repository.loadHabitSummariesSync()[selectedWeekdaysHabit.habitId]!!.isScheduledToday)

        // 2. Reach weekly target (3 / 3) on Wednesday 2026-03-18
        advanceClockToDate(LocalDate.of(2026, 3, 18)) // Wednesday (preferred)
        repository.logHabitEntrySync(flexHabit.habitId, LocalDate.of(2026, 3, 18), 1, null) // 3 / 3 sessions
        repository.recomputeAllStreakRunsAndSummariesSync()
        assertEquals(ProgressBand.GOAL_MET, repository.loadHabitSummariesSync()[flexHabit.habitId]!!.weekProgressBand)
        assertTrue(repository.loadHabitSummariesSync()[flexHabit.habitId]!!.isScheduledToday)

        // 3. On Thursday 2026-03-19 (non-preferred day after target met): moves to Not scheduled today (isScheduledToday == false)
        advanceClockToDate(LocalDate.of(2026, 3, 19)) // Thursday (non-preferred)
        repository.recomputeAllStreakRunsAndSummariesSync()
        assertFalse(
            "Satisfied flexible weekly habit (3/3) must move to Not scheduled today on non-preferred Thursday",
            repository.loadHabitSummariesSync()[flexHabit.habitId]!!.isScheduledToday
        )
        assertEquals(
            "A satisfied flexible weekly habit must be UNSCHEDULED on a non-preferred weekday",
            DerivedDayStatus.UNSCHEDULED,
            repository.evaluateDerivedDayStatus(flexHabit.habitId, LocalDate.of(2026, 3, 19))
        )
        assertTrue(repository.loadHabitSummariesSync()[dailyHabit.habitId]!!.isScheduledToday)
        assertFalse(repository.loadHabitSummariesSync()[selectedWeekdaysHabit.habitId]!!.isScheduledToday)

        // 4. Bonus logging remains possible on Thursday when habit is Not scheduled today -> 4 / 3 sessions (133%, EXTRA)
        repository.logHabitEntrySync(flexHabit.habitId, LocalDate.of(2026, 3, 19), 1, "Bonus Thursday session")
        val summaryAfterBonus = repository.loadHabitSummariesSync()[flexHabit.habitId]!!
        assertEquals(BigInteger.valueOf(4), summaryAfterBonus.weekTotalUnits)
        assertEquals(133, summaryAfterBonus.weekPercentage)
        assertEquals(ProgressBand.EXTRA, summaryAfterBonus.weekProgressBand)

        // 5. On Friday 2026-03-20 (preferred day after target met): remains scheduled on preferred day (isScheduledToday == true)
        advanceClockToDate(LocalDate.of(2026, 3, 20)) // Friday (preferred)
        repository.recomputeAllStreakRunsAndSummariesSync()
        assertTrue(
            "Satisfied flexible weekly habit must remain scheduled on preferred Friday",
            repository.loadHabitSummariesSync()[flexHabit.habitId]!!.isScheduledToday
        )
        assertEquals(
            "A satisfied flexible weekly habit must remain scheduled on a preferred weekday",
            DerivedDayStatus.OPEN_TODAY,
            repository.evaluateDerivedDayStatus(flexHabit.habitId, LocalDate.of(2026, 3, 20))
        )
        assertTrue(repository.loadHabitSummariesSync()[dailyHabit.habitId]!!.isScheduledToday)
        assertTrue(repository.loadHabitSummariesSync()[selectedWeekdaysHabit.habitId]!!.isScheduledToday)
    }

    @Test
    fun t2_c05_recordSessionOperationsReturnSuccessOnlyWhenRepositoryMutationSucceeds() {
        // Missing records must be reported as failures so RecordSessionSheet stays open.
        assertFalse(viewModel.updateHabitEntry("missing_entry", 5, "missing"))
        assertFalse(viewModel.deleteHabitEntry("missing_entry"))

        val quantityHabit = Habit(
            habitId = "h_corr_e_record",
            name = "Read Pages",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20,
            createdAtUtcMs = testClock.nowUtcMs()
        )
        repository.saveHabitSync(quantityHabit, effectiveDateOverride = testClock.todayLocal())
        assertFalse(viewModel.logHabitQuantityOrDuration(quantityHabit, -1, testClock.todayLocal(), "Invalid session"))
        assertTrue(viewModel.logHabitQuantityOrDuration(quantityHabit, 5, testClock.todayLocal(), "First session"))

        val recordedEntry = repository.loadHabitEntriesSync().single { it.habitId == quantityHabit.habitId }
        assertTrue(viewModel.updateHabitEntry(recordedEntry.entryId, 7, "Updated session"))
        assertEquals(7, repository.loadHabitEntriesSync().single { it.entryId == recordedEntry.entryId }.valueUnits)

        assertTrue(viewModel.deleteHabitEntry(recordedEntry.entryId))
        assertTrue(repository.loadHabitEntriesSync().none { it.entryId == recordedEntry.entryId })

        val completionHabit = Habit(
            habitId = "h_corr_e_completion",
            name = "Morning Check-In",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.DAY,
            createdAtUtcMs = testClock.nowUtcMs()
        )
        repository.saveHabitSync(completionHabit, effectiveDateOverride = testClock.todayLocal())
        assertTrue(viewModel.logHabit(completionHabit, date = testClock.todayLocal()))
        assertNotNull(repository.getOutcomeForHabitOnDate(completionHabit.habitId, testClock.todayLocal()))
    }

    @Test
    fun t2_c06_groveRunEndingAnimationTriggersOnlyForNewActiveToEndedTransitions() {
        val previous = mapOf(
            "active_run" to true,
            "already_dormant" to false
        )
        val current = mapOf(
            "active_run" to false,
            "already_dormant" to false,
            "new_active_run" to true
        )

        assertEquals(setOf("active_run"), newlyEndedRunIds(previous, current))

        // Recomposition with the same snapshot and initial load of an already-ended run do not replay.
        assertTrue(newlyEndedRunIds(current, current).isEmpty())
        assertTrue(newlyEndedRunIds(emptyMap(), current).isEmpty())
    }
}

