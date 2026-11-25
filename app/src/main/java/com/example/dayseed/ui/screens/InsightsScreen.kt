package com.example.dayseed.ui.screens

import com.example.dayseed.ui.components.SegmentedChoice
import com.example.dayseed.ui.components.dayseedHeading
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.CompactHistoryBucket
import com.example.dayseed.data.DayDetailHabitItem
import com.example.dayseed.data.DayDetailSummary
import com.example.dayseed.data.DurationNormalizer
import com.example.dayseed.data.MoodCatalog
import com.example.dayseed.data.ProgressBand
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.UnitFormatter
import com.example.dayseed.ui.components.BotanicalGrowthCanvas
import com.example.dayseed.ui.components.DayseedExpandableCard
import com.example.dayseed.ui.components.DayseedShapes
import com.example.dayseed.ui.components.SharedMonthCalendar
import com.example.dayseed.ui.components.WeeklyHabitBoard
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(viewModel: DayseedViewModel, initialMode: Int = 0) {
    val habits by viewModel.habits.collectAsState()
    val habitVersions by viewModel.habitVersions.collectAsState()
    val habitEntries by viewModel.habitEntries.collectAsState()
    val habitDayOutcomes by viewModel.habitDayOutcomes.collectAsState()
    val habitDateExceptions by viewModel.habitDateExceptions.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()
    val growthMetrics by viewModel.growthMetrics.collectAsState()
    val selectedDate by viewModel.selectedHistoryDate.collectAsState()
    val habitSummaries by viewModel.habitSummaries.collectAsState()
    val longestActiveStreak = remember(habitSummaries, habits) { viewModel.selectLongestActiveRun() }

    val referenceToday = viewModel.repository.clock.todayLocal()
    val reducedMotion = viewModel.isReducedMotionEnabled()

    // Section 2 (Habits) state
    var displayedMonthStr by rememberSaveable { mutableStateOf(YearMonth.from(selectedDate).toString()) }
    val displayedMonth = remember(displayedMonthStr) {
        runCatching { YearMonth.parse(displayedMonthStr) }.getOrElse { YearMonth.from(referenceToday) }
    }
    var selectedCalendarHabitId by rememberSaveable { mutableStateOf<String?>(null) }
    var weeklyBoardDateStr by rememberSaveable { mutableStateOf(selectedDate.toString()) }
    val weeklyBoardReferenceDate = remember(weeklyBoardDateStr) {
        runCatching { LocalDate.parse(weeklyBoardDateStr) }.getOrElse { referenceToday }
    }

    // Section 3 (Mood) state
    var moodPeriodKind by rememberSaveable { mutableStateOf("WEEK") }
    var moodRefDateStr by rememberSaveable { mutableStateOf(referenceToday.toString()) }
    val moodRefDate = remember(moodRefDateStr) {
        runCatching { LocalDate.parse(moodRefDateStr) }.getOrElse { referenceToday }
    }
    var selectedMoodFilter by rememberSaveable { mutableStateOf<String?>(null) }

    val monthStart = displayedMonth.atDay(1)
    val monthEnd = displayedMonth.atEndOfMonth()
    val habitsForMonth = remember(habits, habitVersions, habitEntries, habitDayOutcomes, displayedMonth) {
        viewModel.repository.getHabitsRelevantForDateRange(monthStart, monthEnd)
    }

    val weeklyBoardModel = remember(
        weeklyBoardReferenceDate,
        selectedCalendarHabitId,
        referenceToday,
        habits,
        habitVersions,
        habitEntries,
        habitDayOutcomes,
        habitDateExceptions,
        journalEntries
    ) {
        viewModel.repository.computeWeeklyBoard(
            referenceDateInWeek = weeklyBoardReferenceDate,
            filterHabitId = selectedCalendarHabitId,
            referenceToday = referenceToday
        )
    }

    val moodInsights = remember(journalEntries, moodPeriodKind, moodRefDate, selectedMoodFilter) {
        viewModel.repository.computeMoodInsights(
            periodKind = moodPeriodKind,
            referenceDate = moodRefDate,
            selectedMoodFilter = selectedMoodFilter
        )
    }

    val selectedDaySummary = remember(
        selectedDate,
        referenceToday,
        habits,
        habitVersions,
        habitEntries,
        habitDayOutcomes,
        habitDateExceptions,
        journalEntries
    ) {
        viewModel.repository.computeDayDetailSummary(selectedDate, referenceToday)
    }
    var progressBandsExpanded by rememberSaveable { mutableStateOf(false) }
    // Two modes: 0 = Trends (graph and weekly board), 1 = Stats. The month calendar moved to the Journal (D-250).
    var insightsMode by rememberSaveable { mutableStateOf(initialMode) }
    val calendarMode = insightsMode == 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("insights_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Insights",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.dayseedHeading()
            )
            Text(
                text = "Honest progress across habits, mood, and collected moments",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            SegmentedChoice(
                labels = listOf("Trends", "Stats"),
                tags = listOf("insights_mode_trends", "insights_mode_stats"),
                selected = insightsMode,
                onSelect = { insightsMode = it }
            )
        }

        if (!calendarMode && longestActiveStreak != null) {
            item {
                com.example.dayseed.ui.components.TodayLongestStreakIndicator(
                    longestSelection = longestActiveStreak,
                    onOpenHabitDetail = { habitId -> viewModel.openHabitDetail(habitId, referenceToday) },
                    onOpenLivingGrove = { habitId -> viewModel.openLivingGrove(habitId) }
                )
            }
        }

        // =====================================================================
        // 1. TRENDS GRAPH (the month calendar lives in the Journal now; D-250)
        // =====================================================================
        if (calendarMode) item {
            val activeForChart = remember(habits) { habits.filter { it.lifecycleState == com.example.dayseed.data.HabitLifecycleState.ACTIVE } }
            val trendDays = remember(activeForChart, habitEntries, habitDayOutcomes, habitDateExceptions, referenceToday) {
                (13 downTo 0).map { back ->
                    val d = referenceToday.minusDays(back.toLong())
                    var met = 0; var partial = 0; var scheduled = 0
                    for (h in activeForChart) {
                        when (viewModel.repository.evaluateHonestCalendarDateState(h.habitId, d, referenceToday)) {
                            com.example.dayseed.data.HonestCalendarDateState.COMPLETED_TARGET_MET -> { met++; scheduled++ }
                            com.example.dayseed.data.HonestCalendarDateState.PARTIAL_OR_TINY_PROGRESS -> { partial++; scheduled++ }
                            com.example.dayseed.data.HonestCalendarDateState.EXPLICIT_ZERO_NOT_DONE,
                            com.example.dayseed.data.HonestCalendarDateState.MISSED_SCHEDULED_DATE -> scheduled++
                            else -> Unit
                        }
                    }
                    com.example.dayseed.ui.components.TrendDay(d, met, partial, scheduled)
                }
            }
            Column(modifier = Modifier.fillMaxWidth().testTag("insights_section_trends")) {
                com.example.dayseed.ui.components.TrendsChartCard(
                    days = trendDays,
                    today = referenceToday,
                    onSelectDay = { date -> viewModel.openDayDetail(date) }
                )
            }
        }

        // =====================================================================
        // 2. OVERVIEW METRICS (Equal-height paired stat cards)
        // =====================================================================
        if (!calendarMode) item {
            Card(
                shape = DayseedShapes.CardLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insights_section_overview")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val dailyRateText = if (growthMetrics.scheduledDailyOpportunitiesCount > 0) {
                        "${growthMetrics.habitDaysMetCount} / ${growthMetrics.scheduledDailyOpportunitiesCount}"
                    } else {
                        "0 / 0"
                    }
                    val weeklyRateText = if (growthMetrics.flexibleWeeklyOpportunitiesTotalCount > 0) {
                        "${growthMetrics.flexibleWeeklyOpportunitiesMetCount} / ${growthMetrics.flexibleWeeklyOpportunitiesTotalCount}"
                    } else {
                        "0 / 0"
                    }
                    val weeklySub = if (growthMetrics.flexibleWeeklyInProgressCount > 0) {
                        "${growthMetrics.flexibleWeeklyInProgressCount} week in progress"
                    } else {
                        "Completed weekly targets"
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val useSingleColumn = maxWidth < 220.dp
                        if (useSingleColumn) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OverviewStatBox(
                                    label = "Habit-Active Dates",
                                    value = growthMetrics.habitActiveDatesCount.toString(),
                                    subtitle = "Dates with habit logs",
                                    testTag = "overview_habit_active_dates",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OverviewStatBox(
                                    label = "Journal Reflections",
                                    value = growthMetrics.journalEntriesCount.toString(),
                                    subtitle = "${growthMetrics.journalActiveDatesCount} journal dates",
                                    testTag = "overview_journal_entries_count",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OverviewStatBox(
                                    label = "Daily Targets Met",
                                    value = dailyRateText,
                                    subtitle = "Scheduled daily opportunities",
                                    testTag = "overview_daily_opportunities_met",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OverviewStatBox(
                                    label = "Weekly Targets Met",
                                    value = weeklyRateText,
                                    subtitle = weeklySub,
                                    testTag = "overview_weekly_opportunities_met",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OverviewStatBox(
                                    label = "Unique Habits Met",
                                    value = growthMetrics.uniqueHabitsCompletedCount.toString(),
                                    subtitle = "Distinct habits achieving target",
                                    testTag = "overview_unique_habits_completed",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OverviewStatBox(
                                    label = "Planned Rest Days",
                                    value = growthMetrics.plannedRestCount.toString(),
                                    subtitle = "Honest rest exceptions",
                                    testTag = "overview_planned_rest_count",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OverviewStatBox(
                                        label = "Habit-Active Dates",
                                        value = growthMetrics.habitActiveDatesCount.toString(),
                                        subtitle = "Dates with habit logs",
                                        testTag = "overview_habit_active_dates",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                    OverviewStatBox(
                                        label = "Journal Reflections",
                                        value = growthMetrics.journalEntriesCount.toString(),
                                        subtitle = "${growthMetrics.journalActiveDatesCount} journal dates",
                                        testTag = "overview_journal_entries_count",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OverviewStatBox(
                                        label = "Daily Targets Met",
                                        value = dailyRateText,
                                        subtitle = "Scheduled daily opportunities",
                                        testTag = "overview_daily_opportunities_met",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                    OverviewStatBox(
                                        label = "Weekly Targets Met",
                                        value = weeklyRateText,
                                        subtitle = weeklySub,
                                        testTag = "overview_weekly_opportunities_met",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OverviewStatBox(
                                        label = "Unique Habits Met",
                                        value = growthMetrics.uniqueHabitsCompletedCount.toString(),
                                        subtitle = "Distinct habits achieving target",
                                        testTag = "overview_unique_habits_completed",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                    OverviewStatBox(
                                        label = "Planned Rest Days",
                                        value = growthMetrics.plannedRestCount.toString(),
                                        subtitle = "Honest rest exceptions",
                                        testTag = "overview_planned_rest_count",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Numeric/Duration Sessions: ${growthMetrics.numericDurationSessionsCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("overview_numeric_sessions_count")
                        )
                        Text(
                            text = "Check-in Outcomes: ${growthMetrics.completionOutcomesCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("overview_completion_outcomes_count")
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 3. WEEKLY HABIT CONSISTENCY BOARD (Calendar mode) & EXPANDABLE PROGRESS BANDS (Stats mode)
        // =====================================================================
        if (calendarMode) item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insights_section_habits"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Weekly Consistency",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                WeeklyHabitBoard(
                    boardModel = weeklyBoardModel,
                    referenceToday = referenceToday,
                    onPreviousWeek = { weeklyBoardDateStr = weeklyBoardReferenceDate.minusDays(7).toString() },
                    onNextWeek = { weeklyBoardDateStr = weeklyBoardReferenceDate.plusDays(7).toString() },
                    onJumpToCurrentWeek = { weeklyBoardDateStr = referenceToday.toString() },
                    onSelectDate = { date -> viewModel.openDayDetail(date) },
                    onOpenHabitDetail = { habitId, date -> viewModel.openHabitDetail(habitId, date) }
                )

            }
        }

        if (!calendarMode) item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insights_section_bands"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Expandable Detailed Progress Bands (D-040 — preserved in full without crowding Insights)
                val detailedBands = listOf(
                    ProgressBand.ZERO,
                    ProgressBand.TINY_STEP,
                    ProgressBand.GETTING_STARTED,
                    ProgressBand.HALFWAY,
                    ProgressBand.ALMOST_THERE,
                    ProgressBand.GOAL_MET,
                    ProgressBand.EXTRA,
                    ProgressBand.HUGE
                )
                val goalMetOrAbove = (growthMetrics.dailyProgressBandCounts[ProgressBand.GOAL_MET] ?: 0) +
                    (growthMetrics.dailyProgressBandCounts[ProgressBand.EXTRA] ?: 0) +
                    (growthMetrics.dailyProgressBandCounts[ProgressBand.HUGE] ?: 0) +
                    (growthMetrics.weeklyProgressBandCounts[ProgressBand.GOAL_MET] ?: 0) +
                    (growthMetrics.weeklyProgressBandCounts[ProgressBand.EXTRA] ?: 0) +
                    (growthMetrics.weeklyProgressBandCounts[ProgressBand.HUGE] ?: 0)

                DayseedExpandableCard(
                    title = "Detailed Progress Bands",
                    summary = "$goalMetOrAbove goal-met periods across all 7 bands",
                    expanded = progressBandsExpanded,
                    onToggle = { progressBandsExpanded = !progressBandsExpanded },
                    toggleTestTag = "insights_toggle_progress_bands",
                    modifier = Modifier.testTag("insights_progress_bands_card")
                ) {
                    Text(
                        text = "Aggregated per habit-date (daily targets) and per habit-week (weekly targets)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    detailedBands.forEach { band ->
                        val dailyCount = growthMetrics.dailyProgressBandCounts[band] ?: 0
                        val weeklyCount = growthMetrics.weeklyProgressBandCounts[band] ?: 0
                        val combined = dailyCount + weeklyCount
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("insights_band_row_${band.name}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${band.displayName} (${band.rangeLabel})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            Text(
                                text = "$combined ($dailyCount daily • $weeklyCount weekly)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "Legacy Effort Outcomes: Tiny ${growthMetrics.tinyCount} • Usual ${growthMetrics.usualCount} • Extra ${growthMetrics.extraCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("insights_effort_summary_text")
                    )
                }
            }
        }

        // =====================================================================
        // 3. MOOD SECTION (D-096..D-102, Primary Mood Distribution & Comparisons)
        // =====================================================================
        if (!calendarMode) item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mood_insights_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mood",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = moodInsights.periodLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("mood_insights_period_label")
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = moodPeriodKind == "WEEK",
                                onClick = { moodPeriodKind = "WEEK" },
                                label = { Text("Week") },
                                modifier = Modifier.testTag("mood_period_week_chip")
                            )
                            FilterChip(
                                selected = moodPeriodKind == "MONTH",
                                onClick = { moodPeriodKind = "MONTH" },
                                label = { Text("Month") },
                                modifier = Modifier.testTag("mood_period_month_chip")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                moodRefDateStr = if (moodPeriodKind == "MONTH") {
                                    moodRefDate.minusMonths(1).toString()
                                } else {
                                    moodRefDate.minusDays(7).toString()
                                }
                            },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("mood_prev_period_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous period"
                            )
                        }
                        Text(
                            text = "${moodInsights.totalEntriesWithPrimaryMood} entries with primary mood (prev: ${moodInsights.previousPeriodTotalEntries})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                                .testTag("mood_total_entries_text")
                        )
                        IconButton(
                            onClick = {
                                moodRefDateStr = if (moodPeriodKind == "MONTH") {
                                    moodRefDate.plusMonths(1).toString()
                                } else {
                                    moodRefDate.plusDays(7).toString()
                                }
                            },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("mood_next_period_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next period"
                            )
                        }
                    }

                    if (moodInsights.distribution.isEmpty()) {
                        Text(
                            text = "No primary moods logged in this period yet. Add a mood to any Journal reflection to see distribution and period comparisons.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("mood_empty_state_text")
                        )
                    } else {
                        moodInsights.distribution.forEach { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) {
                                        selectedMoodFilter = if (selectedMoodFilter.equals(item.moodCode, ignoreCase = true)) {
                                            null
                                        } else {
                                            item.moodCode
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                                    .testTag("mood_distribution_row_${item.moodCode}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.symbol} ${item.label}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val delta = item.count - item.previousPeriodCount
                                    val deltaStr = when {
                                        delta > 0 -> "+$delta vs prev"
                                        delta < 0 -> "$delta vs prev"
                                        else -> "same as prev"
                                    }
                                    Text(
                                        text = "${item.count} (${item.percentage}%) • $deltaStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((item.percentage / 100f).coerceIn(0.04f, 1f))
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }

                        // Primary Mood Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedMoodFilter == null,
                                onClick = { selectedMoodFilter = null },
                                label = { Text("All") },
                                modifier = Modifier.testTag("mood_filter_chip_ALL")
                            )
                            moodInsights.distribution.forEach { item ->
                                FilterChip(
                                    selected = selectedMoodFilter.equals(item.moodCode, ignoreCase = true),
                                    onClick = {
                                        selectedMoodFilter = if (selectedMoodFilter.equals(item.moodCode, ignoreCase = true)) {
                                            null
                                        } else {
                                            item.moodCode
                                        }
                                    },
                                    label = { Text("${item.symbol} ${item.label}") },
                                    modifier = Modifier.testTag("mood_filter_chip_${item.moodCode}")
                                )
                            }
                        }

                        // Matching entries preview
                        moodInsights.matchingEntries.take(5).forEach { entry ->
                            val pMood = MoodCatalog.findByCode(entry.primaryMoodCode)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) {
                                        viewModel.openJournalEditor(entry = entry, forDate = entry.date)
                                    }
                                    .testTag("mood_matching_entry_${entry.journalEntryId}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "${entry.localDate} • ${pMood?.symbol.orEmpty()} ${entry.title ?: "Reflection"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = entry.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 4. GROWTH SECTION (Collected Moments G2/G3 + Living Grove)
        // =====================================================================
        if (!calendarMode) item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insights_section_growth"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Growth & Living Grove",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                val formattedDuration = DurationNormalizer.formatCollectedMomentsDuration(growthMetrics.currentMonthDurationMinutes)
                val durationSessions = growthMetrics.currentMonthDurationPositiveSessionsCount

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) {
                            viewModel.openCollectedMomentsDetail(
                                yearMonth = YearMonth.from(referenceToday)
                            )
                        }
                        .testTag("insights_collected_moments_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BotanicalGrowthCanvas(
                            activeDates = growthMetrics.habitActiveDatesCount,
                            reducedMotion = reducedMotion,
                            modifier = Modifier.size(68.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (growthMetrics.currentMonthDurationMinutes > 0L) formattedDuration else "Collected moments",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (growthMetrics.currentMonthDurationMinutes > 0L) {
                                    "collected this month • $durationSessions timed ${if (durationSessions == 1) "session" else "sessions"}"
                                } else {
                                    "No timed sessions yet this month"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "See time by date →",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { viewModel.openLivingGrove() }
                        .testTag("insights_open_grove_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Park,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp)
                            ) {
                                Text(
                                    text = "Living Grove",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${growthMetrics.activeSaplingsCount} Saplings • ${growthMetrics.growingTreesCount} Growing • ${growthMetrics.dormantTreesCount} Dormant",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.openLivingGrove() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("insights_open_grove_btn")
                        ) {
                            Text("Explore")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OverviewStatBox(
    label: String,
    value: String,
    subtitle: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 82.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectedDayActivitySummaryCard(
    summary: DayDetailSummary,
    onOpenDayDetail: () -> Unit,
    onOpenHabitDetail: (String) -> Unit
) {
    val dateLabel = remember(summary.date, summary.isToday) {
        val formatted = summary.date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
        if (summary.isToday) "Today · $formatted" else formatted
    }
    val completedCount = summary.habitItems.count {
        it.compactBucket == CompactHistoryBucket.COMPLETED || it.compactBucket == CompactHistoryBucket.EXCEEDED
    }
    val partialCount = summary.habitItems.count { it.compactBucket == CompactHistoryBucket.PARTIAL }

    Card(
        shape = DayseedShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("insights_selected_day_summary")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val countSub = when {
                            summary.isFutureDate -> "Upcoming date"
                            summary.habitItems.isEmpty() -> "No scheduled habits on this day"
                            partialCount > 0 -> "$completedCount of ${summary.habitItems.size} met · $partialCount in progress"
                            else -> "$completedCount of ${summary.habitItems.size} habits completed"
                        }
                        Text(
                            text = countSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = onOpenDayDetail,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("insights_open_selected_day_detail")
                ) {
                    Text("Full Day")
                }
            }

            if (summary.habitItems.isNotEmpty()) {
                summary.habitItems.take(4).forEach { item ->
                    SelectedDayHabitRow(
                        item = item,
                        onClick = { onOpenHabitDetail(item.habit.habitId) }
                    )
                }
                if (summary.habitItems.size > 4) {
                    Text(
                        text = "+${summary.habitItems.size - 4} more in Full Day Detail",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (summary.journalEntries.isNotEmpty()) {
                val firstEntry = summary.journalEntries.first()
                Text(
                    text = "Reflection: ${firstEntry.title?.takeIf { it.isNotBlank() } ?: firstEntry.body}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SelectedDayHabitRow(
    item: DayDetailHabitItem,
    onClick: () -> Unit
) {
    val mode = item.effectiveVersion.trackingMode.canonical()
    val rawUnit = item.effectiveVersion.unitCode ?: item.habit.unitCode
    val statusText = when {
        item.compactBucket == CompactHistoryBucket.COMPLETED || item.compactBucket == CompactHistoryBucket.EXCEEDED -> {
            if (mode == TrackingMode.QUANTITY || mode == TrackingMode.DURATION) {
                "✓ ${UnitFormatter.formatProgressRatioBig(item.totalUnits, item.targetUnits, rawUnit)}"
            } else {
                "✓ Completed"
            }
        }
        item.compactBucket == CompactHistoryBucket.PARTIAL -> {
            if (mode == TrackingMode.QUANTITY || mode == TrackingMode.DURATION) {
                "◐ ${UnitFormatter.formatProgressRatioBig(item.totalUnits, item.targetUnits, rawUnit)}"
            } else {
                "◐ Partial"
            }
        }
        item.restDayException != null -> "◇ Rest day"
        else -> "○ Not yet logged"
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("insights_day_summary_habit_${item.habit.habitId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.habit.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.compactBucket == CompactHistoryBucket.COMPLETED || item.compactBucket == CompactHistoryBucket.EXCEEDED) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open ${item.habit.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
