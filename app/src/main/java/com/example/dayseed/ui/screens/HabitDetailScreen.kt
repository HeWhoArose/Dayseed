package com.example.dayseed.ui.screens

import androidx.compose.ui.text.style.TextOverflow
import com.example.dayseed.ui.components.DayseedSecondaryButton
import com.example.dayseed.ui.components.DayseedPrimaryButton
import com.example.dayseed.ui.components.AdaptiveActionRow
import com.example.dayseed.ui.components.friendlyDateRange
import com.example.dayseed.ui.components.friendlyDate
import com.example.dayseed.ui.components.DayseedStatusPill
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import com.example.dayseed.ui.components.dayseedHeading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.dayseed.ui.components.spokenDescription
import android.text.format.DateFormat
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import com.example.dayseed.ui.components.ProgressBarWithOverflow
import com.example.dayseed.ui.components.rememberCompletionUndoToken
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.DerivedDayStatus
import com.example.dayseed.data.HabitDateExceptionKind
import com.example.dayseed.data.HabitDurationKind
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.HabitLifetimeCalculator
import com.example.dayseed.data.HabitLifetimeStatus
import com.example.dayseed.data.ProgressBand
import com.example.dayseed.data.StreakUnit
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.UnitFormatter
import com.example.dayseed.ui.components.DayseedExpandableCard
import com.example.dayseed.ui.components.DayseedShapes
import com.example.dayseed.ui.components.DormantTransitionBanner
import com.example.dayseed.ui.components.HabitBranchCanvas
import com.example.dayseed.ui.components.formatTerminalReasonLabel
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    viewModel: DayseedViewModel,
    habitId: String,
    selectedLocalDate: String,
    onSelectDate: (String) -> Unit,
    onOpenRecordSession: (String, String, String?) -> Unit,
    onOpenEditHabit: (String) -> Unit,
    onOpenLivingGrove: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val habitVersions by viewModel.habitVersions.collectAsState()
    val summaries by viewModel.habitSummaries.collectAsState()
    val streakRuns by viewModel.habitStreakRuns.collectAsState()
    val entries by viewModel.habitEntries.collectAsState()
    val outcomes by viewModel.habitDayOutcomes.collectAsState()
    val dateNotes by viewModel.habitDateNotes.collectAsState()
    val dateExceptions by viewModel.habitDateExceptions.collectAsState()
    val activeUndoToken = rememberCompletionUndoToken(viewModel)
    val appSettings by viewModel.appSettings.collectAsState()

    val habit = habits.find { it.habitId == habitId }
    if (habit == null) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .testTag("habit_detail_screen"),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Habit not found.", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("secondary_back_btn")
                    ) {
                        Text("Back")
                    }
                }
            }
        }
        return
    }

    val summary = summaries[habitId]
    val today = viewModel.repository.clock.todayLocal()
    val todayStr = today.toString()
    val selectedDateObj = remember(selectedLocalDate) {
        runCatching { LocalDate.parse(selectedLocalDate) }.getOrDefault(today)
    }

    val recentDates = remember(today) {
        (0..29).map { today.minusDays(it.toLong()) }
    }

    val versionsForHabit = remember(habitVersions, habit.habitId) {
        habitVersions.filter { it.habitId == habit.habitId }.sortedBy { it.effectiveFromLocalDate }
    }
    val activeVersion = summary?.effectiveVersion ?: versionsForHabit.lastOrNull()

    val entriesOnSelectedDate = remember(entries, habitId, selectedLocalDate) {
        entries.filter { it.habitId == habitId && it.localDate == selectedLocalDate }
            .sortedBy { it.createdAtUtcMs }
    }
    val outcomeOnSelectedDate = remember(outcomes, habitId, selectedLocalDate) {
        outcomes.find { it.habitId == habitId && it.localDate == selectedLocalDate }
    }
    val exceptionOnSelectedDate = remember(dateExceptions, habitId, selectedLocalDate) {
        dateExceptions.find { it.habitId == habitId && it.localDate == selectedLocalDate }
    }
    val noteOnSelectedDate = remember(dateNotes, habitId, selectedLocalDate) {
        dateNotes.find { it.habitId == habitId && it.localDate == selectedLocalDate }
    }
    val derivedStatusOnSelectedDate = remember(habitId, selectedDateObj, outcomes, entries, dateExceptions, habits) {
        viewModel.repository.evaluateDerivedDayStatus(habitId, selectedDateObj)
    }

    var noteDraft by remember(noteOnSelectedDate, selectedLocalDate) {
        mutableStateOf(noteOnSelectedDate?.body ?: "")
    }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEndHabitConfirmDialog by remember { mutableStateOf(false) }

    val habitStartDate = remember(habit.startLocalDate, activeVersion, today) {
        val raw = habit.startLocalDate ?: activeVersion?.effectiveFromLocalDate
        raw?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
    }
    var showEndDateEditor by remember(habit.habitId) { mutableStateOf(false) }
    var editDurationKind by remember(habit.durationKind, habit.scheduledEndLocalDate) {
        val initialKind = when {
            habit.durationKind != HabitDurationKind.NONE -> habit.durationKind
            habit.scheduledEndLocalDate != null -> HabitDurationKind.SPECIFIC_DATE
            else -> HabitDurationKind.DAYS
        }
        mutableStateOf(initialKind)
    }
    var editDurationValueText by remember(habit.durationValue) {
        mutableStateOf((habit.durationValue ?: 30).toString())
    }
    var editSpecificEndDateText by remember(habit.scheduledEndLocalDate) {
        mutableStateOf(habit.scheduledEndLocalDate ?: habitStartDate.plusDays(30).toString())
    }
    var editEndDateError by remember { mutableStateOf<String?>(null) }
    var showDetailDatePickerDialog by remember { mutableStateOf(false) }

    val editEndDateCalculation = remember(
        habitStartDate,
        editDurationKind,
        editDurationValueText,
        editSpecificEndDateText
    ) {
        HabitLifetimeCalculator.calculateEndDateResult(
            startDate = habitStartDate,
            durationKind = editDurationKind,
            durationValue = editDurationValueText.trim().toIntOrNull(),
            specificEndDateIso = editSpecificEndDateText.trim().ifBlank { null }
        )
    }

    val habitRuns = remember(streakRuns, habitId) {
        streakRuns.filter { it.habitId == habitId }
            .sortedByDescending { it.startLocalDate }
    }
    val activeRun = habitRuns.find { it.isActive }
    val qualifiedGroveTrees = habitRuns.filter { it.qualifiedForGrove }
    val shortRuns = habitRuns.filter { !it.qualifiedForGrove && !it.isActive }

    val unacknowledgedDormantRun = remember(streakRuns, appSettings, habitId) {
        viewModel.getUnacknowledgedDormantRunForHabit(habitId)
    }
    val reducedMotion = remember(appSettings) { viewModel.isReducedMotionEnabled() }
    var streakGroveExpanded by remember(habitId, unacknowledgedDormantRun) {
        mutableStateOf(unacknowledgedDormantRun != null)
    }
    var lifetimeExpanded by remember(habitId) { mutableStateOf(false) }

    val habitColor = remember(habit.colorHex) {
        try {
            Color(habit.colorHex.toColorInt())
        } catch (_: Exception) {
            Color(0xFF286143)
        }
    }

    val context = LocalContext.current
    val is24HourFormat = remember(context) { DateFormat.is24HourFormat(context) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("habit_detail_screen"),
        color = Color.Transparent
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("secondary_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Habit Detail",
                            modifier = Modifier.weight(1f, fill = false).dayseedHeading(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    OutlinedButton(
                        onClick = { onOpenEditHabit(habit.habitId) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("detail_edit_habit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", maxLines = 1, softWrap = false)
                    }
                }
            }

            // Short undo banner, only for a just-registered completion or deleted session (D-237)
            if (activeUndoToken != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag("undo_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Change recorded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { viewModel.triggerUndo() },
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .testTag("undo_action_btn")
                            ) {
                                Text("Undo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // SECTION A: Habit Identity (Concise & Scannable)
            item {
                val activeReminderMode = activeVersion?.reminderMode ?: habit.reminderMode
                val activeReminderTime = activeVersion?.reminderTimeLocal ?: habit.reminderTimeLocal
                val reminderSuffix = if (!habit.lifecycleState.isTerminal && activeReminderMode == "AT_TIME" && !activeReminderTime.isNullOrBlank()) {
                    " · Reminder ${SettingsSupport.formatUnambiguousTimeDisplay(activeReminderTime, is24HourFormat)}"
                } else {
                    ""
                }
                val targetAndScheduleLine = "${summary?.formatTargetAndPeriodSummary() ?: habit.formatTargetAndPeriodSummary()} · ${summary?.formatScheduleSummary() ?: habit.formatScheduleSummary()}$reminderSuffix"

                Card(
                    shape = DayseedShapes.Card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_detail_section_a")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(habitColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = habit.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            DayseedStatusPill(
                                text = habit.lifetimeStatusLabel,
                                containerColor = when (habit.lifetimeStatus) {
                                    HabitLifetimeStatus.ONGOING,
                                    HabitLifetimeStatus.SCHEDULED_TO_END ->
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                    HabitLifetimeStatus.COMPLETED ->
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    HabitLifetimeStatus.ENDED ->
                                        MaterialTheme.colorScheme.secondaryContainer
                                    HabitLifetimeStatus.PAUSED ->
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                    HabitLifetimeStatus.ARCHIVED ->
                                        MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("detail_lifecycle_badge")
                            )
                        }

                        Text(
                            text = targetAndScheduleLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("detail_target_schedule_line")
                        )

                        if (habit.description.isNotBlank()) {
                            Text(
                                text = habit.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (habit.scheduledEndLocalDate != null || habit.lifecycleState.isEndedOrCompleted) {
                            Text(
                                text = habit.formatLifetimeSummary(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("detail_lifetime_summary_text")
                            )
                        }

                    }
                }
            }

            // SECTION B: Today's Progress & Immediate Primary Actions
            item {
                val activeBand = summary?.activeProgressBand ?: ProgressBand.ZERO
                val isWeekly = habit.targetPeriod == TargetPeriod.WEEK
                val currentUnits = summary?.activeTotalUnits ?: BigInteger.ZERO
                val targetUnits = summary?.activeTargetUnits
                    ?: BigInteger.valueOf((habit.targetValue ?: 1).coerceAtLeast(1).toLong())
                val pct = summary?.activePercentage ?: 0
                val rawUnit = activeVersion?.unitCode ?: habit.unitCode
                val ratioDisplay = if (rawUnit != null && (UnitFormatter.extractScale(rawUnit) > 0 || UnitFormatter.isCurrencyUnit(rawUnit))) {
                    UnitFormatter.formatProgressRatioBig(currentUnits, targetUnits, rawUnit)
                } else {
                    val unitLabel = rawUnit?.let { " ${UnitFormatter.cleanUnitCode(it)}" } ?: ""
                    "$currentUnits / $targetUnits$unitLabel"
                }

                Card(
                    shape = DayseedShapes.Card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_detail_section_b")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isWeekly) "Weekly Progress" else "Today's Progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            DayseedStatusPill(
                                text = activeBand.label,
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .semantics { contentDescription = activeBand.spokenDescription() }
                                    .testTag("detail_progress_band_badge")
                            )
                        }

                        val progressLabel = if (isWeekly && !summary?.weekStartLocalDate.isNullOrBlank()) {
                            "This week (${friendlyDateRange(summary?.weekStartLocalDate.orEmpty(), summary?.weekEndLocalDate.orEmpty())}): $ratioDisplay ($pct%)"
                        } else {
                            "Today: $ratioDisplay ($pct%)"
                        }
                        Text(
                            text = progressLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("detail_progress_summary_text")
                        )

                        ProgressBarWithOverflow(
                            fraction = pct / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = activeBand.encouragementQuote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("detail_encouragement_quote")
                        )
                        Text(
                            text = activeBand.motivationQuote,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("detail_motivation_quote")
                        )

                        // Immediately discoverable Primary Actions
                        if (!habit.lifecycleState.isTerminal) {
                            DayseedPrimaryButton(
                                text = if (selectedLocalDate == todayStr) "Add progress" else "Add progress for ${friendlyDate(selectedLocalDate, today)}",
                                onClick = { onOpenRecordSession(habitId, selectedLocalDate, null) },
                                icon = Icons.Default.Add,
                                modifier = Modifier.fillMaxWidth().testTag("detail_record_session_btn")
                            )

                            if (selectedLocalDate == todayStr) {
                                val hasRestDayToday = exceptionOnSelectedDate?.exceptionKind == HabitDateExceptionKind.REST_DAY ||
                                    summary?.hasRestDayToday == true
                                if (hasRestDayToday) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearRestDayToday(habit) },
                                        shape = DayseedShapes.Button,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("detail_clear_rest_day_btn")
                                    ) {
                                        Text("Clear Rest Day")
                                    }
                                } else if (summary?.canTakeRestDayToday == true) {
                                    TextButton(
                                        onClick = { viewModel.takeRestDayToday(habit) },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("detail_take_rest_day_btn")
                                    ) {
                                        Text("Take a planned rest day today")
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "This habit is ${habit.lifetimeStatusLabel.lowercase()}. Historical records are preserved below; active daily logging is paused.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("detail_terminal_logging_disabled_notice")
                            )
                        }
                    }
                }
            }

            // SECTION C: Selected Date & Individual Sessions
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_detail_section_c")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Recent days",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Date Strip
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recentDates, key = { it.toString() }) { d ->
                                val dStr = d.toString()
                                val isSelected = dStr == selectedLocalDate
                                val hasActivity = entries.any { it.habitId == habitId && it.localDate == dStr } ||
                                    outcomes.any { it.habitId == habitId && it.localDate == dStr } ||
                                    dateExceptions.any { it.habitId == habitId && it.localDate == dStr }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .clickable(role = Role.Button) { onSelectDate(dStr) }
                                        .testTag("detail_date_pill_$dStr")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = d.format(DateTimeFormatter.ofPattern("EEE")),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = d.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (hasActivity) "🌱" else "·",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }

                        // DerivedDayStatus summary for selectedDate
                        val statusLabel = when (derivedStatusOnSelectedDate) {
                            DerivedDayStatus.COMPLETED -> "Goal met / Completed"
                            DerivedDayStatus.PARTIAL -> "Partial progress recorded"
                            DerivedDayStatus.LOGGED_ZERO -> "Checked in (0 recorded)"
                            DerivedDayStatus.REST_DAY -> "Rest day taken (streak held)"
                            DerivedDayStatus.OPEN_TODAY -> "Open for today"
                            DerivedDayStatus.DERIVED_MISSED -> "Quiet day (unlogged past scheduled day)"
                            DerivedDayStatus.UNSCHEDULED -> "Unscheduled neutral day"
                            DerivedDayStatus.INACTIVE_LIFECYCLE -> "Paused or inactive on this date"
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "${friendlyDate(selectedLocalDate, today)} — $statusLabel",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("detail_derived_status_badge")
                            )
                        }

                        // Individual Sessions list on selectedDate
                        if (entriesOnSelectedDate.isEmpty() && outcomeOnSelectedDate == null && exceptionOnSelectedDate == null) {
                            Text(
                                text = "Nothing recorded on ${friendlyDate(selectedLocalDate, today)}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            if (outcomeOnSelectedDate != null && entriesOnSelectedDate.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val effSuffix = outcomeOnSelectedDate.effortLevel?.let { " ($it)" } ?: ""
                                        Text(
                                            text = "Outcome: ${outcomeOnSelectedDate.outcomeKind}$effSuffix",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        TextButton(
                                            onClick = { viewModel.removeRecord(habitId, selectedDateObj) },
                                            modifier = Modifier.testTag("detail_clear_outcome_btn")
                                        ) {
                                            Text("Clear")
                                        }
                                    }
                                }
                            }

                            for (entry in entriesOnSelectedDate) {
                                val timeFormatted = remember(entry.createdAtUtcMs, is24HourFormat) {
                                    runCatching {
                                        val rawHhMm = Instant.ofEpochMilli(entry.createdAtUtcMs)
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalTime()
                                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                                        SettingsSupport.formatUnambiguousTimeDisplay(rawHhMm, is24HourFormat)
                                    }.getOrDefault("")
                                }
                                val rawUnit = activeVersion?.unitCode ?: habit.unitCode
                                val entryValDisplay = if (rawUnit != null && (UnitFormatter.extractScale(rawUnit) > 0 || UnitFormatter.isCurrencyUnit(rawUnit))) {
                                    UnitFormatter.formatValueWithUnit(entry.valueUnits, rawUnit)
                                } else {
                                    val unitSuffix = rawUnit?.takeIf { it.isNotBlank() }?.let { " ${UnitFormatter.cleanUnitCode(it)}" } ?: ""
                                    "${entry.valueUnits}$unitSuffix"
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("detail_session_row_${entry.entryId}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entryValDisplay +
                                                    if (timeFormatted.isNotBlank()) " • $timeFormatted" else "",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (!entry.entryNote.isNullOrBlank()) {
                                                Text(
                                                    text = entry.entryNote,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    onOpenRecordSession(habitId, selectedLocalDate, entry.entryId)
                                                },
                                                modifier = Modifier
                                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                                    .testTag("edit_session_${entry.entryId}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit session",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteHabitEntry(entry.entryId)
                                                },
                                                modifier = Modifier
                                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                                    .testTag("delete_session_${entry.entryId}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete session",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!habit.lifecycleState.isTerminal && selectedLocalDate != todayStr) {
                            Text(
                                text = "Rest days can only be taken on today's date (D-057).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("detail_rest_day_past_info")
                            )
                        }
                    }
                }
            }

            // SECTION D: Habit Date Note
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_detail_section_d")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Note · ${friendlyDate(selectedLocalDate, today)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = noteDraft,
                            onValueChange = { noteDraft = it },
                            placeholder = { Text("Add a note for ${friendlyDate(selectedLocalDate, today)}…") },
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnFocus()
                                .testTag("habit_detail_note_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    viewModel.saveHabitDateNote(habitId, selectedDateObj, noteDraft)
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("habit_detail_save_note_btn")
                            ) {
                                Text("Save Note")
                            }
                        }
                    }
                }
            }

            // SECTION E: Expandable Streak & Living Grove History
            item {
                val unitWord = if (habit.targetPeriod == TargetPeriod.WEEK) "weeks" else "days"
                val activeCount = activeRun?.completedStreakCount ?: 0
                val streakGroveSummary = buildString {
                    if (activeRun != null) {
                        append("$activeCount $unitWord active streak")
                    } else {
                        append("No active streak")
                    }
                    append(" · ${qualifiedGroveTrees.size} Grove ${if (qualifiedGroveTrees.size == 1) "tree" else "trees"}")
                }

                DayseedExpandableCard(
                    title = "Streak & Living Grove",
                    summary = streakGroveSummary,
                    expanded = streakGroveExpanded,
                    onToggle = { streakGroveExpanded = !streakGroveExpanded },
                    leadingIcon = Icons.Default.Park,
                    toggleTestTag = "detail_toggle_streak_grove",
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                    modifier = Modifier.testTag("habit_detail_section_e")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Streaks of 7+ active days remain permanent trees in your Grove forever.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { onOpenLivingGrove(habitId) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("detail_open_grove_btn")
                        ) {
                            Text("Open Grove")
                        }
                    }

                    if (unacknowledgedDormantRun != null) {
                        DormantTransitionBanner(
                            run = unacknowledgedDormantRun,
                            habitName = habit.name,
                            onAcknowledge = { runId ->
                                viewModel.acknowledgeDormantTransition(runId)
                            }
                        )
                    }

                    HabitBranchCanvas(
                        habitName = habit.name,
                        activeRun = activeRun,
                        groveRuns = qualifiedGroveTrees,
                        reducedMotion = reducedMotion
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        val activeSpan = activeRun?.elapsedActiveSpanCount ?: 0
                        Text(
                            text = if (activeRun != null) {
                                "Active streak: $activeCount $unitWord ($activeSpan active days)"
                            } else {
                                "No active streak run right now — log today to sprout a new sapling."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("detail_active_streak_text")
                        )
                        if (activeRun != null) {
                            val stateText = if (activeRun.qualifiedForGrove) {
                                "Growing Grove Tree (7+ active days reached)"
                            } else {
                                "Active Sapling (${7 - activeRun.elapsedActiveSpanCount.coerceAtMost(7)} more active days to become a permanent Grove tree)"
                            }
                            Text(
                                text = "Started ${friendlyDate(activeRun.startLocalDate, today)} • $stateText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Permanent Grove Trees (>= 7 active span days)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_grove_trees_section"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Permanent Grove Trees (${qualifiedGroveTrees.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (qualifiedGroveTrees.isEmpty()) {
                            Text(
                                text = "No permanent Grove trees for this habit yet (requires 7+ active span days).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            for (tree in qualifiedGroveTrees) {
                                val treeUnit = if (tree.streakUnit == StreakUnit.WEEKS) "weeks" else "days"
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("detail_grove_tree_${tree.runId}")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = if (tree.isActive) {
                                                "🌳 Growing Tree • ${tree.completedStreakCount} $treeUnit (${tree.elapsedActiveSpanCount} active days)"
                                            } else {
                                                "🌲 Dormant Tree • ${tree.completedStreakCount} $treeUnit (${tree.elapsedActiveSpanCount} active days)"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${friendlyDate(tree.startLocalDate, today)} → ${friendlyDate(tree.endLocalDate, today)} • ${formatTerminalReasonLabel(tree.terminalReason)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Earlier Short Runs (< 7 active span days)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_short_runs_section"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Earlier Short Runs (< 7 days — not Grove trees) (${shortRuns.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (shortRuns.isEmpty()) {
                            Text(
                                text = "No earlier short runs recorded.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            for (shortRun in shortRuns) {
                                val srUnit = if (shortRun.streakUnit == StreakUnit.WEEKS) "weeks" else "days"
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("detail_short_run_${shortRun.runId}")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "🌱 Short Run • ${shortRun.completedStreakCount} $srUnit (${shortRun.elapsedActiveSpanCount} active days)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${friendlyDate(shortRun.startLocalDate, today)} → ${friendlyDate(shortRun.endLocalDate, today)} • ${formatTerminalReasonLabel(shortRun.terminalReason)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Expandable Lifetime Progress & Rule Details (eliminates top-card clutter & duplicate stats)
            item {
                val lifetimeDays = summary?.lifetimeActiveDays ?: 0
                val lifetimeSummaryText = if (habit.lifetimeGoalDays != null) {
                    "$lifetimeDays / ${habit.lifetimeGoalDays} active days"
                } else {
                    "$lifetimeDays active ${if (lifetimeDays == 1) "day" else "days"} recorded"
                }
                val modeLabel = when (habit.trackingMode.canonical()) {
                    TrackingMode.QUANTITY -> "Quantity"
                    TrackingMode.DURATION -> "Duration"
                    TrackingMode.COMPLETION_ONLY -> "Simple check-off"
                    TrackingMode.EFFORT_AWARE -> "Effort-Aware (Legacy)"
                    else -> habit.trackingMode.name
                }

                DayseedExpandableCard(
                    title = "Lifetime Progress & Rules",
                    summary = lifetimeSummaryText,
                    expanded = lifetimeExpanded,
                    onToggle = { lifetimeExpanded = !lifetimeExpanded },
                    toggleTestTag = "detail_toggle_lifetime",
                    modifier = Modifier.testTag("habit_detail_lifetime_card")
                ) {
                    Text(
                        text = if (habit.lifetimeGoalDays != null) {
                            "Lifetime active days: $lifetimeDays / ${habit.lifetimeGoalDays} days"
                        } else {
                            "Lifetime active days: $lifetimeDays days"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("detail_lifetime_active_days")
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!habit.category.isNullOrBlank()) {
                            MetadataBadge("Category: ${habit.category}")
                        }
                        MetadataBadge("Mode: $modeLabel")
                        if (activeVersion != null) {
                            MetadataBadge("Rule effective from: ${friendlyDate(activeVersion.effectiveFromLocalDate, today)}")
                        }
                    }
                }
            }

            // SECTION F: Lifecycle & End-Date Management
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_detail_section_f")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Lifecycle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        // Lifecycle status & dates summary box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Started ${friendlyDate(habitStartDate, today)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("detail_start_date_text")
                                )
                                val scheduledEndDisplay = habit.scheduledEndLocalDate?.let { raw ->
                                    val parsed = runCatching { LocalDate.parse(raw) }.getOrNull()
                                    if (parsed != null) {
                                        friendlyDate(parsed, today)
                                    } else {
                                        raw
                                    }
                                } ?: "none (ongoing)"
                                Text(
                                    text = "Ends: $scheduledEndDisplay",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("detail_scheduled_end_date_text")
                                )
                                if (habit.actualEndLocalDate != null || habit.endReason != null || habit.lifecycleState.isEndedOrCompleted) {
                                    val reasonText = habit.endReason?.label ?: habit.lifecycleState.displayLabel
                                    Text(
                                        text = "Ended ${habit.actualEndLocalDate?.let { friendlyDate(it, today) } ?: "—"} ($reasonText)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.testTag("detail_actual_end_date_text")
                                    )
                                }
                            }
                        }

                        // End-date editor toggle & remove end date button
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    editEndDateError = null
                                    showEndDateEditor = !showEndDateEditor
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("detail_edit_end_date_btn")
                            ) {
                                Text(
                                    when {
                                        showEndDateEditor -> "Close end-date editor"
                                        habit.scheduledEndLocalDate != null -> "Change end date"
                                        else -> "Set end date"
                                    }
                                )
                            }

                            if (habit.scheduledEndLocalDate != null || habit.lifecycleState.isEndedOrCompleted) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.clearHabitEndDate(habit.habitId) {
                                            showEndDateEditor = false
                                        }
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_remove_end_date_btn")
                                ) {
                                    Text("Return to ongoing")
                                }
                            }

                            if (!habit.lifecycleState.isEndedOrCompleted) {
                                Button(
                                    onClick = { showEndHabitConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_end_habit_btn")
                                ) {
                                    Text("Finish habit…")
                                }
                            }
                        }

                        if (showEndDateEditor) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_end_date_editor")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "How long do you want to maintain this habit?",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HabitDurationKind.entries.forEach { kind ->
                                            FilterChip(
                                                selected = editDurationKind == kind,
                                                onClick = {
                                                    editDurationKind = kind
                                                    editEndDateError = null
                                                    when (kind) {
                                                        HabitDurationKind.DAYS -> {
                                                            if (editDurationValueText.toIntOrNull() == null || editDurationValueText.toInt() <= 0) {
                                                                editDurationValueText = "30"
                                                            }
                                                        }
                                                        HabitDurationKind.WEEKS -> {
                                                            if (editDurationValueText.toIntOrNull() == null || editDurationValueText.toInt() <= 0 || editDurationValueText == "30") {
                                                                editDurationValueText = "2"
                                                            }
                                                        }
                                                        HabitDurationKind.MONTHS -> {
                                                            if (editDurationValueText.toIntOrNull() == null || editDurationValueText.toInt() <= 0 || editDurationValueText == "30") {
                                                                editDurationValueText = "3"
                                                            }
                                                        }
                                                        HabitDurationKind.YEARS -> {
                                                            if (editDurationValueText.toIntOrNull() == null || editDurationValueText.toInt() <= 0 || editDurationValueText == "30") {
                                                                editDurationValueText = "1"
                                                            }
                                                        }
                                                        HabitDurationKind.SPECIFIC_DATE -> {
                                                            if (editSpecificEndDateText.isBlank()) {
                                                                editSpecificEndDateText = today.plusDays(30).toString()
                                                            }
                                                        }
                                                        HabitDurationKind.NONE -> Unit
                                                    }
                                                },
                                                label = { Text(kind.label) },
                                                modifier = Modifier.testTag("detail_duration_kind_${kind.name}")
                                            )
                                        }
                                    }

                                    when (editDurationKind) {
                                        HabitDurationKind.NONE -> {
                                            Text(
                                                text = "Removes the scheduled end date and returns this habit to Ongoing.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.testTag("detail_calculated_end_date_text")
                                            )
                                        }
                                        HabitDurationKind.DAYS,
                                        HabitDurationKind.WEEKS,
                                        HabitDurationKind.MONTHS,
                                        HabitDurationKind.YEARS -> {
                                            val unitLabel = when (editDurationKind) {
                                                HabitDurationKind.DAYS -> "Number of days"
                                                HabitDurationKind.WEEKS -> "Number of weeks"
                                                HabitDurationKind.MONTHS -> "Number of months"
                                                HabitDurationKind.YEARS -> "Number of years"
                                                else -> "Duration"
                                            }
                                            OutlinedTextField(
                                                value = editDurationValueText,
                                                onValueChange = { raw ->
                                                    editDurationValueText = raw.filter { it.isDigit() || it == '-' }
                                                    editEndDateError = null
                                                },
                                                label = { Text(unitLabel) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .bringIntoViewOnFocus()
                                                    .testTag("detail_duration_value_input")
                                            )
                                            val calcDate = editEndDateCalculation.getOrNull()
                                            val calcErr = editEndDateCalculation.exceptionOrNull()?.message
                                            if (calcDate != null) {
                                                Text(
                                                    text = "Calculated end date: ${HabitLifetimeCalculator.formatHumanReadableDate(calcDate)} ($calcDate)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.testTag("detail_calculated_end_date_text")
                                                )
                                            } else if (calcErr != null) {
                                                Text(
                                                    text = calcErr,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.testTag("detail_end_date_error_text")
                                                )
                                            }
                                        }
                                        HabitDurationKind.SPECIFIC_DATE -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = editSpecificEndDateText,
                                                    onValueChange = {
                                                        editSpecificEndDateText = it
                                                        editEndDateError = null
                                                    },
                                                    label = { Text("End date (YYYY-MM-DD)") },
                                                    singleLine = true,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .bringIntoViewOnFocus()
                                                        .testTag("detail_specific_end_date_input")
                                                )
                                                OutlinedButton(
                                                    onClick = { showDetailDatePickerDialog = true },
                                                    modifier = Modifier
                                                        .heightIn(min = 48.dp)
                                                        .testTag("detail_pick_end_date_btn")
                                                ) {
                                                    Text("Calendar")
                                                }
                                            }
                                            val calcDate = editEndDateCalculation.getOrNull()
                                            val calcErr = editEndDateCalculation.exceptionOrNull()?.message
                                            if (calcDate != null) {
                                                Text(
                                                    text = "Selected end date: ${HabitLifetimeCalculator.formatHumanReadableDate(calcDate)} ($calcDate)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.testTag("detail_calculated_end_date_text")
                                                )
                                            } else if (calcErr != null) {
                                                Text(
                                                    text = calcErr,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.testTag("detail_end_date_error_text")
                                                )
                                            }
                                        }
                                    }

                                    if (editEndDateError != null) {
                                        Text(
                                            text = editEndDateError!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.testTag("detail_end_date_save_error")
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { showEndDateEditor = false },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("detail_cancel_end_date_btn")
                                        ) {
                                            Text("Cancel")
                                        }
                                        Button(
                                            onClick = {
                                                if (editEndDateCalculation.isFailure) {
                                                    editEndDateError = editEndDateCalculation.exceptionOrNull()?.message
                                                        ?: "Please enter a valid end date."
                                                    return@Button
                                                }
                                                if (editDurationKind == HabitDurationKind.NONE) {
                                                    viewModel.clearHabitEndDate(habit.habitId) {
                                                        showEndDateEditor = false
                                                    }
                                                } else {
                                                    viewModel.updateHabitEndDate(
                                                        habitId = habit.habitId,
                                                        durationKind = editDurationKind,
                                                        durationValue = editDurationValueText.trim().toIntOrNull(),
                                                        specificEndDate = editSpecificEndDateText.trim().ifBlank { null }
                                                    ) {
                                                        showEndDateEditor = false
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("detail_save_end_date_btn")
                                        ) {
                                            Text("Save end date")
                                        }
                                    }
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (habit.lifecycleState == HabitLifecycleState.PAUSED) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateHabitLifecycle(habit.habitId, HabitLifecycleState.ACTIVE)
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_pause_resume_btn")
                                ) {
                                    Text("Resume habit")
                                }
                            } else if (habit.lifecycleState == HabitLifecycleState.ACTIVE) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateHabitLifecycle(habit.habitId, HabitLifecycleState.PAUSED)
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_pause_resume_btn")
                                ) {
                                    Text("Pause habit")
                                }
                            }

                            if (habit.lifecycleState == HabitLifecycleState.ARCHIVED) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateHabitLifecycle(habit.habitId, HabitLifecycleState.ACTIVE)
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_archive_habit_btn")
                                ) {
                                    Text("Unarchive habit")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateHabitLifecycle(habit.habitId, HabitLifecycleState.ARCHIVED)
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_archive_habit_btn")
                                ) {
                                    Text("Archive habit")
                                }
                            }

                        }
                    }
                }
            }

            // Visually Separated Destructive Danger Zone
            item {
                Card(
                    shape = DayseedShapes.Card,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.14f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.42f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_detail_danger_zone")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Delete habit",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Permanently deletes this habit and all its recorded sessions and notes. To keep your history, end or archive the habit above instead.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("detail_delete_habit_btn")
                        ) {
                            Text("Delete permanently", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showEndHabitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEndHabitConfirmDialog = false },
            title = { Text("End \"${habit.name}\"?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ending this habit means:\n" +
                            "• It will no longer appear as an active habit on Today.\n" +
                            "• Its scheduled reminders will stop immediately.\n" +
                            "• All existing progress, streaks, and history will be preserved."
                    )
                    Text(
                        "How would you like to record the ending of this habit?",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showEndHabitConfirmDialog = false
                            viewModel.endHabitImmediately(habit.habitId, markAsCompleted = true)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("confirm_end_habit_completed_btn")
                    ) {
                        Text("Mark as Completed")
                    }
                    OutlinedButton(
                        onClick = {
                            showEndHabitConfirmDialog = false
                            viewModel.endHabitImmediately(habit.habitId, markAsCompleted = false)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("confirm_end_habit_ended_btn")
                    ) {
                        Text("End Without Completion")
                    }
                    TextButton(
                        onClick = { showEndHabitConfirmDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("cancel_end_habit_btn")
                    ) {
                        Text("Cancel")
                    }
                }
            },
            modifier = Modifier.testTag("end_habit_confirm_dialog")
        )
    }

    if (showDetailDatePickerDialog) {
        val initialSelectedDate = runCatching { LocalDate.parse(editSpecificEndDateText.trim()) }.getOrNull()
            ?: today.plusDays(30)
        val initialMillis = initialSelectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDetailDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val pickedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            editSpecificEndDateText = pickedDate.toString()
                            editEndDateError = null
                        }
                        showDetailDatePickerDialog = false
                    },
                    modifier = Modifier.testTag("confirm_detail_end_date_picker_btn")
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDetailDatePickerDialog = false },
                    modifier = Modifier.testTag("cancel_detail_end_date_picker_btn")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("detail_end_date_picker_dialog")
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete \"${habit.name}\" permanently?") },
            text = {
                Text(
                    "This removes the habit, its logged sessions, and its streak runs. " +
                        "Any journal reflections linked to this habit will be preserved safely. " +
                        "This action is permanent and cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteHabit(habit.habitId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("confirm_delete_habit_btn")
                ) {
                    Text("Delete Habit")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("cancel_delete_habit_btn")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("delete_habit_confirm_dialog")
        )
    }
}

@Composable
private fun MetadataBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
