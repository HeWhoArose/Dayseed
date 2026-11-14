package com.example.dayseed.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HonestCalendarDateState
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.data.MoodCatalog
import com.example.dayseed.data.WeeklyBoardModel
import com.example.dayseed.data.WeeklyPeriodStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharedMonthCalendar(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    referenceToday: LocalDate,
    weekStartDay: DayOfWeek,
    habitsForMonth: List<Habit>,
    selectedHabitFilterId: String?,
    onSelectHabitFilter: ((String?) -> Unit)? = null,
    evaluateDateStateForHabit: (habitId: String, date: LocalDate) -> HonestCalendarDateState,
    journalEntries: List<JournalEntry>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "shared_calendar"
) {
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    val orderedWeekdays = (0..6).map { offset ->
        DayOfWeek.of(((weekStartDay.value - 1 + offset) % 7) + 1)
    }

    val journalsByDate = journalEntries.groupBy { it.localDate }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month navigation header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("${testTagPrefix}_prev_month_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month"
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayedMonth.format(monthFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("${testTagPrefix}_month_label")
                    )
                    if (displayedMonth != YearMonth.from(referenceToday) || selectedDate != referenceToday) {
                        TextButton(
                            onClick = onJumpToToday,
                            modifier = Modifier.testTag("${testTagPrefix}_jump_today_btn")
                        ) {
                            Text(
                                text = "Jump to Today",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("${testTagPrefix}_next_month_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month"
                    )
                }
            }

            // Optional habit filter chips
            if (onSelectHabitFilter != null && habitsForMonth.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedHabitFilterId == null,
                        onClick = { onSelectHabitFilter(null) },
                        label = { Text("All Habits") },
                        modifier = Modifier.testTag("${testTagPrefix}_filter_all")
                    )
                    habitsForMonth.forEach { habit ->
                        FilterChip(
                            selected = selectedHabitFilterId == habit.habitId,
                            onClick = {
                                onSelectHabitFilter(
                                    if (selectedHabitFilterId == habit.habitId) null else habit.habitId
                                )
                            },
                            label = {
                                Text(
                                    text = habit.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("${testTagPrefix}_filter_${habit.habitId}")
                        )
                    }
                }
            }

            // 7-column weekday header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                orderedWeekdays.forEach { dow ->
                    Text(
                        text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Month days grid
            val firstOfMonth = displayedMonth.atDay(1)
            val daysInMonth = displayedMonth.lengthOfMonth()
            val leadingEmptyCells = Math.floorMod(firstOfMonth.dayOfWeek.value - weekStartDay.value, 7)
            val totalCells = leadingEmptyCells + daysInMonth
            val rowsCount = (totalCells + 6) / 7

            for (rowIdx in 0 until rowsCount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (colIdx in 0..6) {
                        val cellIndex = rowIdx * 7 + colIdx
                        val dayOfMonth = cellIndex - leadingEmptyCells + 1
                        if (dayOfMonth !in 1..daysInMonth) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                            )
                        } else {
                            val date = displayedMonth.atDay(dayOfMonth)
                            val dateStr = date.toString()
                            val dayJournals = journalsByDate[dateStr].orEmpty()
                            val latestMood = dayJournals
                                .maxByOrNull { it.createdAtUtcMs }
                                ?.primaryMoodCode
                                ?.let { MoodCatalog.findByCode(it) }

                            val isSingleHabitMode = selectedHabitFilterId != null
                            val perHabitStates = if (date.isAfter(referenceToday)) {
                                emptyList()
                            } else if (isSingleHabitMode) {
                                listOf(evaluateDateStateForHabit(selectedHabitFilterId!!, date))
                            } else {
                                habitsForMonth.map { evaluateDateStateForHabit(it.habitId, date) }
                            }

                            val singleHabitState = if (date.isAfter(referenceToday)) {
                                HonestCalendarDateState.FUTURE_DATE
                            } else if (isSingleHabitMode) {
                                perHabitStates.firstOrNull() ?: HonestCalendarDateState.UNSCHEDULED_DATE
                            } else {
                                resolveCalendarCellHonestState(
                                    date = date,
                                    referenceToday = referenceToday,
                                    habitsForMonth = habitsForMonth,
                                    selectedHabitFilterId = null,
                                    evaluateDateStateForHabit = evaluateDateStateForHabit
                                )
                            }

                            val isSelected = date == selectedDate
                            val isToday = date == referenceToday
                            // In All-Habits Mode, keep the cell container background neutral instead of
                            // painting the whole cell green when only one habit is completed (Plan 2.B1).
                            val (bgColor, textColor, borderColor) = if (isSingleHabitMode) {
                                calendarColorsForState(
                                    state = singleHabitState,
                                    isSelected = isSelected,
                                    isToday = isToday
                                )
                            } else {
                                val scheme = MaterialTheme.colorScheme
                                val neutralBg = if (isSelected) {
                                    scheme.primaryContainer.copy(alpha = 0.25f)
                                } else {
                                    scheme.surface
                                }
                                val neutralFg = if (date.isAfter(referenceToday)) {
                                    scheme.onSurfaceVariant.copy(alpha = 0.6f)
                                } else {
                                    scheme.onSurface
                                }
                                val neutralBorder = when {
                                    isSelected -> scheme.primary
                                    isToday -> scheme.secondary
                                    else -> scheme.outlineVariant.copy(alpha = 0.5f)
                                }
                                Triple(neutralBg, neutralFg, neutralBorder)
                            }

                            val a11yDescription = buildString {
                                append(date.toString())
                                if (isToday) append(", Today")
                                append(", ${singleHabitState.displayLabel}")
                                if (latestMood != null) append(", Mood ${latestMood.label}")
                                if (dayJournals.isNotEmpty()) {
                                    append(", ${dayJournals.size} journal ${if (dayJournals.size == 1) "entry" else "entries"}")
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minWidth = 44.dp, minHeight = 54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bgColor)
                                    .border(
                                        width = if (isSelected) 2.dp else if (isToday) 1.5.dp else 0.5.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectDate(date) }
                                    .semantics { contentDescription = a11yDescription }
                                    .testTag("${testTagPrefix}_date_${date}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 3.dp, horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor
                                    )
                                    // Habit non-colour status indicators (All-Habits compact per-habit marks vs Single-Habit mark)
                                    if (isSingleHabitMode) {
                                        Text(
                                            text = singleHabitState.nonColorIndicator,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    } else {
                                        val compactMarks = perHabitStates
                                            .filter {
                                                it != HonestCalendarDateState.UNSCHEDULED_DATE &&
                                                    it != HonestCalendarDateState.FUTURE_DATE
                                            }
                                            .take(4)
                                            .joinToString("") { it.nonColorIndicator }
                                            .ifEmpty { "·" }
                                        Text(
                                            text = compactMarks,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            maxLines = 1
                                        )
                                    }
                                    // Independent Mood + Journal indicators (Workstream D)
                                    if (latestMood != null || dayJournals.isNotEmpty()) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (latestMood != null) {
                                                Text(
                                                    text = latestMood.symbol,
                                                    fontSize = 8.sp
                                                )
                                            }
                                            if (dayJournals.isNotEmpty()) {
                                                val jBadgeText = if (dayJournals.size > 1) "✍️${dayJournals.size}" else "✍️"
                                                Text(
                                                    text = jBadgeText,
                                                    fontSize = 8.sp,
                                                    modifier = Modifier.testTag("${testTagPrefix}_journal_badge_$date")
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Honest Date Legend (Compact non-colour + colour cues)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("${testTagPrefix}_legend"),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LegendChip("✓", "Met")
                LegendChip("◐", "Partial/Tiny")
                LegendChip("⏸", "Rest/Paused")
                LegendChip("⟳", "Week Open")
                LegendChip("0", "Explicit 0")
                LegendChip("○", "Missed")
                LegendChip("·", "Unscheduled/Future")
            }
        }
    }
}

@Composable
private fun LegendChip(symbol: String, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun resolveCalendarCellHonestState(
    date: LocalDate,
    referenceToday: LocalDate,
    habitsForMonth: List<Habit>,
    selectedHabitFilterId: String?,
    evaluateDateStateForHabit: (habitId: String, date: LocalDate) -> HonestCalendarDateState
): HonestCalendarDateState {
    if (date.isAfter(referenceToday)) {
        return HonestCalendarDateState.FUTURE_DATE
    }
    if (selectedHabitFilterId != null) {
        return evaluateDateStateForHabit(selectedHabitFilterId, date)
    }
    if (habitsForMonth.isEmpty()) {
        return HonestCalendarDateState.UNSCHEDULED_DATE
    }
    val states = habitsForMonth.map { evaluateDateStateForHabit(it.habitId, date) }
    return when {
        states.any { it == HonestCalendarDateState.COMPLETED_TARGET_MET } &&
            states.none { it == HonestCalendarDateState.PARTIAL_OR_TINY_PROGRESS || it == HonestCalendarDateState.MISSED_SCHEDULED_DATE } ->
            HonestCalendarDateState.COMPLETED_TARGET_MET
        states.any { it == HonestCalendarDateState.COMPLETED_TARGET_MET || it == HonestCalendarDateState.PARTIAL_OR_TINY_PROGRESS } ->
            HonestCalendarDateState.PARTIAL_OR_TINY_PROGRESS
        states.any { it == HonestCalendarDateState.EXPLICIT_ZERO_NOT_DONE } ->
            HonestCalendarDateState.EXPLICIT_ZERO_NOT_DONE
        states.any { it == HonestCalendarDateState.REST_DAY_PLANNED_PAUSE } ->
            HonestCalendarDateState.REST_DAY_PLANNED_PAUSE
        states.any { it == HonestCalendarDateState.PAUSED_DATE_RANGE } ->
            HonestCalendarDateState.PAUSED_DATE_RANGE
        states.any { it == HonestCalendarDateState.FLEXIBLE_WEEKLY_IN_PROGRESS } ->
            HonestCalendarDateState.FLEXIBLE_WEEKLY_IN_PROGRESS
        states.any { it == HonestCalendarDateState.MISSED_SCHEDULED_DATE } ->
            HonestCalendarDateState.MISSED_SCHEDULED_DATE
        states.any { it == HonestCalendarDateState.NEUTRAL_TRANSITION_GAP } ->
            HonestCalendarDateState.NEUTRAL_TRANSITION_GAP
        else -> HonestCalendarDateState.UNSCHEDULED_DATE
    }
}

@Composable
private fun calendarColorsForState(
    state: HonestCalendarDateState,
    isSelected: Boolean,
    isToday: Boolean
): Triple<Color, Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val bg = when (state) {
        HonestCalendarDateState.COMPLETED_TARGET_MET -> scheme.primaryContainer
        HonestCalendarDateState.PARTIAL_OR_TINY_PROGRESS -> scheme.secondaryContainer
        HonestCalendarDateState.REST_DAY_PLANNED_PAUSE,
        HonestCalendarDateState.PAUSED_DATE_RANGE -> scheme.surfaceVariant.copy(alpha = 0.75f)
        HonestCalendarDateState.FLEXIBLE_WEEKLY_IN_PROGRESS -> scheme.tertiaryContainer.copy(alpha = 0.65f)
        HonestCalendarDateState.EXPLICIT_ZERO_NOT_DONE -> scheme.surfaceVariant
        HonestCalendarDateState.MISSED_SCHEDULED_DATE -> scheme.errorContainer.copy(alpha = 0.45f)
        HonestCalendarDateState.FUTURE_DATE,
        HonestCalendarDateState.UNSCHEDULED_DATE,
        HonestCalendarDateState.NEUTRAL_TRANSITION_GAP -> scheme.surface
    }
    val fg = when (state) {
        HonestCalendarDateState.COMPLETED_TARGET_MET -> scheme.onPrimaryContainer
        HonestCalendarDateState.PARTIAL_OR_TINY_PROGRESS -> scheme.onSecondaryContainer
        HonestCalendarDateState.MISSED_SCHEDULED_DATE -> scheme.onErrorContainer
        HonestCalendarDateState.FUTURE_DATE -> scheme.onSurfaceVariant.copy(alpha = 0.6f)
        else -> scheme.onSurface
    }
    val border = when {
        isSelected -> scheme.primary
        isToday -> scheme.secondary
        else -> scheme.outlineVariant.copy(alpha = 0.5f)
    }
    return Triple(bg, fg, border)
}

@Composable
fun WeeklyHabitBoard(
    boardModel: WeeklyBoardModel,
    referenceToday: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onJumpToCurrentWeek: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onOpenHabitDetail: (String, LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val shortDateFmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_habit_board_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousWeek,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("weekly_board_prev_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous week"
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${boardModel.weekStartDate.format(shortDateFmt)} – ${boardModel.weekEndDate.format(shortDateFmt)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("weekly_board_range_label")
                    )
                    Text(
                        text = "Week starts ${boardModel.weekStartDayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (referenceToday !in boardModel.weekStartDate..boardModel.weekEndDate) {
                        TextButton(
                            onClick = onJumpToCurrentWeek,
                            modifier = Modifier.testTag("weekly_board_jump_current_btn")
                        ) {
                            Text("Current Week")
                        }
                    }
                }

                IconButton(
                    onClick = onNextWeek,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("weekly_board_next_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next week"
                    )
                }
            }

            // 7-Column Date Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(56.dp))
                boardModel.days.forEach { day ->
                    val isToday = day == referenceToday
                    val mood = boardModel.primaryMoodByDate[day]
                    val jCount = boardModel.journalCountsByDate[day] ?: 0
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectDate(day) }
                            .padding(vertical = 4.dp)
                            .testTag("weekly_board_day_header_$day"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = day.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (mood != null || jCount > 0) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (mood != null) {
                                    Text(text = mood.symbol, fontSize = 9.sp)
                                }
                                if (jCount > 0) {
                                    Text(
                                        text = if (jCount > 1) "✍️$jCount" else "✍️",
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (boardModel.rows.isEmpty()) {
                Text(
                    text = "No active or historical habits recorded during this week.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .testTag("weekly_board_empty_state")
                )
            } else {
                boardModel.rows.forEach { row ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weekly_board_row_${row.habit.habitId}"),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.effectiveVersion.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onOpenHabitDetail(row.habit.habitId, boardModel.weekEndDate.coerceAtMost(referenceToday)) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val badgeColor = when (row.periodStatus) {
                                    WeeklyPeriodStatus.ABOVE_TARGET,
                                    WeeklyPeriodStatus.MET -> MaterialTheme.colorScheme.primaryContainer
                                    WeeklyPeriodStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondaryContainer
                                    WeeklyPeriodStatus.MISSED -> MaterialTheme.colorScheme.errorContainer
                                    WeeklyPeriodStatus.PAUSED, WeeklyPeriodStatus.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = badgeColor
                                ) {
                                    Text(
                                        text = row.summaryText,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                            .testTag("weekly_board_summary_${row.habit.habitId}")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.effectiveVersion.targetPeriod.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(56.dp)
                                )
                                row.cells.forEach { cell ->
                                    val (cellBg, cellFg, cellBorder) = calendarColorsForState(
                                        state = cell.honestState,
                                        isSelected = false,
                                        isToday = cell.date == referenceToday
                                    )
                                    val cellDesc = "${row.effectiveVersion.name}, ${cell.date}, ${cell.honestState.displayLabel}" +
                                        if (cell.isPreferredWeekday) ", Scheduled/Preferred day" else ""
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(cellBg)
                                            .border(1.dp, cellBorder, RoundedCornerShape(8.dp))
                                            .clickable { onSelectDate(cell.date) }
                                            .semantics { contentDescription = cellDesc }
                                            .testTag("weekly_board_cell_${row.habit.habitId}_${cell.date}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = cell.cellLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = cellFg
                                            )
                                            if (cell.isPreferredWeekday) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(cellFg.copy(alpha = 0.7f))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
