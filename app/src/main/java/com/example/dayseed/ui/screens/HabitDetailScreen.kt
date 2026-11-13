package com.example.dayseed.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.DerivedDayStatus
import com.example.dayseed.data.HabitDateExceptionKind
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.ProgressBand
import com.example.dayseed.data.StreakUnit
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.components.DormantTransitionBanner
import com.example.dayseed.ui.components.HabitBranchCanvas
import com.example.dayseed.ui.components.formatTerminalReasonLabel
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
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
    val activeUndoToken by viewModel.activeUndoToken.collectAsState()
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

    val habitColor = remember(habit.colorHex) {
        try {
            Color(habit.colorHex.toColorInt())
        } catch (_: Exception) {
            Color(0xFF286143)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("habit_detail_screen"),
        color = Color.Transparent
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Text("Edit")
                    }
                }
            }

            // Guarded 8-second Undo Banner
            if (activeUndoToken != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
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
                                text = "Action recorded (Undo available for 8s)",
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

            // SECTION A: Header & Metadata
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
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
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = habit.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = habit.lifecycleState.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("detail_lifecycle_badge")
                                )
                            }
                        }

                        if (habit.description.isNotBlank()) {
                            Text(
                                text = habit.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!habit.category.isNullOrBlank()) {
                                MetadataBadge("Category: ${habit.category}")
                            }
                            val modeLabel = when (habit.trackingMode.canonical()) {
                                TrackingMode.QUANTITY -> "Quantity"
                                TrackingMode.DURATION -> "Duration"
                                TrackingMode.COMPLETION_ONLY -> "Simple check-off"
                                TrackingMode.EFFORT_AWARE -> "Effort-Aware (Legacy)"
                                else -> habit.trackingMode.name
                            }
                            MetadataBadge("Mode: $modeLabel")
                            MetadataBadge("Target: ${summary?.formatTargetAndPeriodSummary() ?: habit.formatTargetAndPeriodSummary()}")
                            MetadataBadge("Schedule: ${summary?.formatScheduleSummary() ?: habit.formatScheduleSummary()}")
                            if (activeVersion != null) {
                                MetadataBadge("Rule effective from: ${activeVersion.effectiveFromLocalDate}")
                            }
                        }
                    }
                }
            }

            // SECTION B: Progress Summary (D-040 Progress Band & Dual Quotes)
            item {
                val activeBand = summary?.activeProgressBand ?: ProgressBand.ZERO
                val isWeekly = habit.targetPeriod == TargetPeriod.WEEK
                val currentUnits = summary?.activeTotalUnits ?: BigInteger.ZERO
                val targetUnits = summary?.activeTargetUnits
                    ?: BigInteger.valueOf((habit.targetValue ?: 1).coerceAtLeast(1).toLong())
                val pct = summary?.activePercentage ?: 0
                val unitLabel = habit.unitCode?.let { " $it" } ?: ""

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_detail_section_b")
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
                            Text(
                                text = if (isWeekly) "Weekly Progress" else "Today's Progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "${activeBand.label} (${activeBand.shortBadge})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("detail_progress_band_badge")
                                )
                            }
                        }

                        val progressLabel = if (isWeekly && !summary?.weekStartLocalDate.isNullOrBlank()) {
                            "This week (${summary?.weekStartLocalDate}..${summary?.weekEndLocalDate}): $currentUnits / $targetUnits$unitLabel ($pct%)"
                        } else {
                            "Today: $currentUnits / $targetUnits$unitLabel ($pct%)"
                        }
                        Text(
                            text = progressLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("detail_progress_summary_text")
                        )

                        LinearProgressIndicator(
                            progress = { (pct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
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

                        val lifetimeDays = summary?.lifetimeActiveDays ?: 0
                        val lifetimeLabel = if (habit.lifetimeGoalDays != null) {
                            "Lifetime active days: $lifetimeDays / ${habit.lifetimeGoalDays} days"
                        } else {
                            "Lifetime active days: $lifetimeDays days"
                        }
                        Text(
                            text = lifetimeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("detail_lifetime_active_days")
                        )
                    }
                }
            }

            // SECTION C: Selected Date & Individual Sessions
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_detail_section_c")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Selected Date & Individual Sessions",
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
                                        .clickable { onSelectDate(dStr) }
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
                                text = "$selectedLocalDate — $statusLabel",
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
                                text = "No individual sessions recorded on $selectedLocalDate.",
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
                                val timeFormatted = remember(entry.createdAtUtcMs) {
                                    runCatching {
                                        Instant.ofEpochMilli(entry.createdAtUtcMs)
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalTime()
                                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                                    }.getOrDefault("")
                                }
                                val unitSuffix = habit.unitCode?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
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
                                                text = "${entry.valueUnits}$unitSuffix" +
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

                        // Primary CTA: Record Session & Rest Day controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onOpenRecordSession(habitId, selectedLocalDate, null)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("detail_record_session_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Record Session")
                            }

                            if (selectedLocalDate == todayStr) {
                                val hasRestDayToday = exceptionOnSelectedDate?.exceptionKind == HabitDateExceptionKind.REST_DAY ||
                                    summary?.hasRestDayToday == true
                                if (hasRestDayToday) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearRestDayToday(habit) },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("detail_clear_rest_day_btn")
                                    ) {
                                        Text("Clear Rest Day")
                                    }
                                } else if (summary?.canTakeRestDayToday == true) {
                                    OutlinedButton(
                                        onClick = { viewModel.takeRestDayToday(habit) },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("detail_take_rest_day_btn")
                                    ) {
                                        Text("Take a rest day")
                                    }
                                }
                            }
                        }

                        if (selectedLocalDate != todayStr) {
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
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_detail_section_d")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Daily Habit Note ($selectedLocalDate)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = noteDraft,
                            onValueChange = { noteDraft = it },
                            placeholder = { Text("Add a note for ${habit.name} on $selectedLocalDate...") },
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
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

            // SECTION E: Individual Streak & Living Grove History
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_detail_section_e")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Individual Streak & Living Grove",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Streaks of 7+ active days remain permanent trees in your Grove forever.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { onOpenLivingGrove(habitId) },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("detail_open_grove_btn")
                            ) {
                                Icon(Icons.Default.Park, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
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
                            val unitWord = if (habit.targetPeriod == TargetPeriod.WEEK) "weeks" else "days"
                            val activeCount = activeRun?.completedStreakCount ?: 0
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
                                    text = "Started ${activeRun.startLocalDate} • $stateText",
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
                                    val unitWord = if (tree.streakUnit == StreakUnit.WEEKS) "weeks" else "days"
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
                                                    "🌳 Growing Tree • ${tree.completedStreakCount} $unitWord (${tree.elapsedActiveSpanCount} active days)"
                                                } else {
                                                    "🌲 Dormant Tree • ${tree.completedStreakCount} $unitWord (${tree.elapsedActiveSpanCount} active days)"
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${tree.startLocalDate} → ${tree.endLocalDate} • ${formatTerminalReasonLabel(tree.terminalReason)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Earlier Short Runs (< 7 active span days — strictly separate from Grove trees)
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
                                    val unitWord = if (shortRun.streakUnit == StreakUnit.WEEKS) "weeks" else "days"
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("detail_short_run_${shortRun.runId}")
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "🌱 Short Run • ${shortRun.completedStreakCount} $unitWord (${shortRun.elapsedActiveSpanCount} active days)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${shortRun.startLocalDate} → ${shortRun.endLocalDate} • ${formatTerminalReasonLabel(shortRun.terminalReason)}",
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
            }

            // SECTION F: Lifecycle Controls
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_detail_section_f")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Habit Lifecycle Controls",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pausing, completing, or archiving preserves your past Grove trees without penalty.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onOpenEditHabit(habit.habitId) },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("detail_lifecycle_edit_btn")
                            ) {
                                Text("Edit Habit")
                            }

                            if (habit.lifecycleState == HabitLifecycleState.PAUSED) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateHabitLifecycle(habit.habitId, HabitLifecycleState.ACTIVE)
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_pause_resume_btn")
                                ) {
                                    Text("Resume Habit")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateHabitLifecycle(habit.habitId, HabitLifecycleState.PAUSED)
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_pause_resume_btn")
                                ) {
                                    Text("Pause Habit")
                                }
                            }

                            if (habit.lifecycleState != HabitLifecycleState.COMPLETED) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateHabitLifecycle(habit.habitId, HabitLifecycleState.COMPLETED)
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_complete_habit_btn")
                                ) {
                                    Text("Mark Completed")
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
                                    Text("Unarchive Habit")
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
                                    Text("Archive Habit")
                                }
                            }

                            OutlinedButton(
                                onClick = { showDeleteConfirmDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("detail_delete_habit_btn")
                            ) {
                                Text("Delete Permanently")
                            }
                        }
                    }
                }
            }
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
