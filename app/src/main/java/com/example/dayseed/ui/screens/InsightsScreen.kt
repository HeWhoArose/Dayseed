package com.example.dayseed.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.DurationNormalizer
import com.example.dayseed.data.MoodCatalog
import com.example.dayseed.data.ProgressBand
import com.example.dayseed.ui.components.BotanicalGrowthCanvas
import com.example.dayseed.ui.components.SharedMonthCalendar
import com.example.dayseed.ui.components.WeeklyHabitBoard
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@Composable
fun InsightsScreen(viewModel: DayseedViewModel) {
    val habits by viewModel.habits.collectAsState()
    val habitVersions by viewModel.habitVersions.collectAsState()
    val habitEntries by viewModel.habitEntries.collectAsState()
    val habitDayOutcomes by viewModel.habitDayOutcomes.collectAsState()
    val habitDateExceptions by viewModel.habitDateExceptions.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()
    val growthMetrics by viewModel.growthMetrics.collectAsState()
    val selectedDate by viewModel.selectedHistoryDate.collectAsState()

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("insights_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Insights",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Honest progress across habits, mood, and collected moments",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // =====================================================================
        // 1. OVERVIEW SECTION (Separated Habit vs. Journal & Opportunity Metrics)
        // =====================================================================
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insights_section_overview")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "1. Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverviewStatBox(
                            label = "Habit-Active Dates",
                            value = growthMetrics.habitActiveDatesCount.toString(),
                            subtitle = "Dates with habit logs",
                            testTag = "overview_habit_active_dates",
                            modifier = Modifier.weight(1f)
                        )
                        OverviewStatBox(
                            label = "Journal Reflections",
                            value = growthMetrics.journalEntriesCount.toString(),
                            subtitle = "${growthMetrics.journalActiveDatesCount} journal dates",
                            testTag = "overview_journal_entries_count",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val dailyRateText = if (growthMetrics.scheduledDailyOpportunitiesCount > 0) {
                            "${growthMetrics.habitDaysMetCount} / ${growthMetrics.scheduledDailyOpportunitiesCount}"
                        } else {
                            "0 / 0"
                        }
                        OverviewStatBox(
                            label = "Daily Targets Met",
                            value = dailyRateText,
                            subtitle = "Scheduled daily opportunities",
                            testTag = "overview_daily_opportunities_met",
                            modifier = Modifier.weight(1f)
                        )

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
                        OverviewStatBox(
                            label = "Weekly Targets Met",
                            value = weeklyRateText,
                            subtitle = weeklySub,
                            testTag = "overview_weekly_opportunities_met",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverviewStatBox(
                            label = "Unique Habits Met",
                            value = growthMetrics.uniqueHabitsCompletedCount.toString(),
                            subtitle = "Distinct habits achieving target",
                            testTag = "overview_unique_habits_completed",
                            modifier = Modifier.weight(1f)
                        )
                        OverviewStatBox(
                            label = "Planned Rest Days",
                            value = growthMetrics.plannedRestCount.toString(),
                            subtitle = "Honest rest exceptions",
                            testTag = "overview_planned_rest_count",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
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
        // 2. HABITS SECTION (Shared Month Calendar + 7-Column Weekly Board + D-040 Bands)
        // =====================================================================
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insights_section_habits"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "2. Habits",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap any date in the calendar or weekly board to open Day Detail",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = { viewModel.openDayDetail(selectedDate) },
                        modifier = Modifier.testTag("insights_open_selected_day_detail")
                    ) {
                        Text("Day Detail ($selectedDate)")
                    }
                }

                SharedMonthCalendar(
                    displayedMonth = displayedMonth,
                    selectedDate = selectedDate,
                    referenceToday = referenceToday,
                    weekStartDay = viewModel.repository.getWeekStartDay(monthStart),
                    habitsForMonth = habitsForMonth,
                    selectedHabitFilterId = selectedCalendarHabitId,
                    onSelectHabitFilter = { selectedCalendarHabitId = it },
                    evaluateDateStateForHabit = { hId, d ->
                        viewModel.repository.evaluateHonestCalendarDateState(hId, d, referenceToday)
                    },
                    journalEntries = journalEntries,
                    onPreviousMonth = { displayedMonthStr = displayedMonth.minusMonths(1).toString() },
                    onNextMonth = { displayedMonthStr = displayedMonth.plusMonths(1).toString() },
                    onJumpToToday = {
                        displayedMonthStr = YearMonth.from(referenceToday).toString()
                        weeklyBoardDateStr = referenceToday.toString()
                        viewModel.selectHistoryDate(referenceToday)
                    },
                    onSelectDate = { date ->
                        weeklyBoardDateStr = date.toString()
                        viewModel.openDayDetail(date)
                    },
                    testTagPrefix = "insights_calendar"
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

                // Aggregated D-040 Progress Bands Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("insights_progress_bands_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Aggregated Progress Bands (D-040)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aggregated per habit-date (daily targets) and per habit-week (weekly targets)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

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
                                    color = MaterialTheme.colorScheme.onSurface
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
        }

        // =====================================================================
        // 3. MOOD SECTION (D-096..D-102, Primary Mood Distribution & Comparisons)
        // =====================================================================
        item {
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
                                text = "3. Mood",
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
                            modifier = Modifier.testTag("mood_total_entries_text")
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
                                    .clickable {
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
                                    .clickable {
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
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insights_section_growth"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "4. Growth",
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
                        .clickable {
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
                                text = "${growthMetrics.habitActiveDatesCount} Habit-Active Dates",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$formattedDuration collected this month • $durationSessions duration ${if (durationSessions == 1) "session" else "sessions"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap to inspect Collected Moments by date →",
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
                        .clickable { viewModel.openLivingGrove() }
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
                            Column {
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
                            modifier = Modifier.testTag("insights_open_grove_btn")
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
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
