package com.example.dayseed.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayseed.data.DailyOutcome
import com.example.dayseed.data.EffortLevel
import com.example.dayseed.data.GroveTreeState
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitDateException
import com.example.dayseed.data.HabitDateNote
import com.example.dayseed.data.HabitDayOutcome
import com.example.dayseed.data.HabitEntry
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.HabitSummary
import com.example.dayseed.data.ProgressBandClassifier
import com.example.dayseed.data.StreakUnit
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.theme.ExtraBranchColor
import com.example.dayseed.ui.theme.TinySproutColor
import com.example.dayseed.ui.theme.UsualLeafColor
import java.math.BigInteger

@Composable
fun HabitCard(
    habit: Habit,
    outcome: HabitDayOutcome? = null,
    entries: List<HabitEntry> = emptyList(),
    dateNote: HabitDateNote? = null,
    restDayException: HabitDateException? = null,
    summary: HabitSummary? = null,
    retrospectivePromptEligible: Boolean = false,
    onOpenHabitDetail: (() -> Unit)? = null,
    onOpenRecordSession: ((entryId: String?) -> Unit)? = null,
    onLogOutcome: (outcome: DailyOutcome, effort: EffortLevel) -> Unit,
    onLogQuantityOrDuration: ((valueUnits: Int, entryNote: String?) -> Unit)? = null,
    onDeleteEntry: ((entryId: String) -> Unit)? = null,
    onSaveDateNote: ((body: String) -> Unit)? = null,
    onTakeRestDayToday: (() -> Unit)? = null,
    onClearRestDayToday: (() -> Unit)? = null,
    onOpenRetrospectiveJournal: (() -> Unit)? = null,
    onDismissRetrospectivePrompt: (() -> Unit)? = null,
    onResetRecord: () -> Unit,
    onEditHabit: () -> Unit,
    onArchiveHabit: () -> Unit,
    onPauseResumeHabit: ((HabitLifecycleState) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var noteDraft by remember(dateNote) { mutableStateOf(dateNote?.body ?: "") }
    val effVersion = summary?.effectiveVersion
    val mode = (effVersion?.trackingMode ?: habit.trackingMode).canonical()
    val isRecorded = outcome != null || entries.isNotEmpty() || restDayException != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}")
            .semantics {
                contentDescription = "Habit card for ${habit.name}"
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isRecorded) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Habit Name (tappable to open HabitDetail), Target/Schedule summary, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (onOpenHabitDetail != null) {
                                Modifier.clickable { onOpenHabitDetail() }
                            } else {
                                Modifier
                            }
                        )
                        .padding(vertical = 4.dp)
                        .testTag("open_habit_detail_${habit.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(habit.colorHex))
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!habit.category.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = habit.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        val metaLine = if (summary != null) {
                            "${summary.formatTargetAndPeriodSummary()} • ${summary.formatScheduleSummary()}"
                        } else if (habit.description.isNotBlank()) {
                            habit.description
                        } else {
                            ""
                        }
                        if (metaLine.isNotBlank()) {
                            Text(
                                text = metaLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("habit_menu_${habit.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Habit options for ${habit.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (onOpenHabitDetail != null) {
                            DropdownMenuItem(
                                text = { Text("Habit Details & History") },
                                onClick = {
                                    showMenu = false
                                    onOpenHabitDetail()
                                },
                                modifier = Modifier.testTag("menu_open_detail_${habit.id}")
                            )
                        }
                        if (onOpenRecordSession != null) {
                            DropdownMenuItem(
                                text = { Text("Record Session") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onOpenRecordSession(null)
                                },
                                modifier = Modifier.testTag("menu_record_session_${habit.id}")
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (dateNote != null) "Edit Date Note" else "Add Date Note") },
                            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showNoteEditor = !showNoteEditor
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Habit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEditHabit()
                            }
                        )
                        if (summary?.canTakeRestDayToday == true && onTakeRestDayToday != null) {
                            DropdownMenuItem(
                                text = { Text("Take a Rest Day (Hold Streak)") },
                                onClick = {
                                    showMenu = false
                                    onTakeRestDayToday()
                                },
                                modifier = Modifier.testTag("take_rest_day_menu_${habit.id}")
                            )
                        }
                        if (onPauseResumeHabit != null) {
                            val isPaused = habit.lifecycleState == HabitLifecycleState.PAUSED
                            DropdownMenuItem(
                                text = { Text(if (isPaused) "Resume Habit" else "Pause Habit") },
                                leadingIcon = {
                                    Icon(
                                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onPauseResumeHabit(
                                        if (isPaused) HabitLifecycleState.ACTIVE else HabitLifecycleState.PAUSED
                                    )
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (habit.isArchived) "Restore Habit" else "Archive Habit") },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onArchiveHabit()
                            }
                        )
                    }
                }
            }

            // Optional Streak & Grove / Lifetime Goal Row (D-217, U-6)
            if (summary != null && (summary.currentCompletedStreak > 0 || summary.currentElapsedActiveSpan > 0 || summary.lifetimeGoalDays != null)) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (summary.currentCompletedStreak > 0 || summary.currentElapsedActiveSpan > 0) {
                        val unitWord = if (summary.streakUnit == StreakUnit.WEEKS) {
                            if (summary.currentCompletedStreak == 1) "week" else "weeks"
                        } else {
                            if (summary.currentCompletedStreak == 1) "day" else "days"
                        }
                        val treeIcon = when (summary.activeRun?.treeState) {
                            GroveTreeState.GROWING_TREE -> "🌳"
                            GroveTreeState.SAPLING_ACTIVE -> "🌱"
                            else -> "🌿"
                        }
                        val streakText = if (summary.currentElapsedActiveSpan > summary.currentCompletedStreak) {
                            "$treeIcon ${summary.currentCompletedStreak} $unitWord (${summary.currentElapsedActiveSpan}d span)"
                        } else {
                            "$treeIcon ${summary.currentCompletedStreak} $unitWord streak"
                        }
                        Text(
                            text = streakText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("habit_streak_badge_${habit.id}")
                        )
                    }
                    if (summary.lifetimeGoalDays != null) {
                        Text(
                            text = "• ${summary.lifetimeActiveDays} / ${summary.lifetimeGoalDays} active days",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("habit_lifetime_goal_${habit.id}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mode-specific Logging / Progress Section or Rest Day Exception Banner (D-218)
            if (restDayException != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("habit_rest_day_banner_${habit.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🍃 Rest day taken for today",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Your streak is held safely today.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    if (onClearRestDayToday != null) {
                        TextButton(
                            onClick = onClearRestDayToday,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("clear_rest_day_btn_${habit.id}")
                        ) {
                            Text("Resume Today")
                        }
                    }
                }
            } else {
                when (mode) {
                    TrackingMode.QUANTITY, TrackingMode.DURATION -> {
                        QuantityOrDurationSection(
                            habit = habit,
                            summary = summary,
                            entries = entries,
                            onAddEntry = { units, note ->
                                onLogQuantityOrDuration?.invoke(units, note)
                            },
                            onOpenRecordSession = onOpenRecordSession,
                            onDeleteEntry = { entryId ->
                                onDeleteEntry?.invoke(entryId)
                            }
                        )
                    }

                    TrackingMode.COMPLETION_ONLY, TrackingMode.EFFORT_AWARE -> {
                        val isWeeklyTarget = (summary?.effectiveVersion?.targetPeriod ?: habit.targetPeriod) == TargetPeriod.WEEK
                        if (isWeeklyTarget && summary != null) {
                            WeeklyCheckOffProgressBanner(habit = habit, summary = summary)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (outcome != null) {
                            RecordedOutcomeRow(
                                outcome = outcome,
                                onReset = onResetRecord
                            )
                        } else {
                            UnrecordedOutcomeActionRow(
                                habit = habit,
                                onLog = onLogOutcome
                            )
                        }
                    }

                    else -> {}
                }
            }

            // Optional per-habit-per-date note display or editor
            if (dateNote != null && !showNoteEditor) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: ${dateNote.body}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("habit_date_note_${habit.id}")
                )
            }

            if (showNoteEditor && onSaveDateNote != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    label = { Text("Note for this date") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_note_input_${habit.id}")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showNoteEditor = false },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            if (noteDraft.isNotBlank()) {
                                onSaveDateNote(noteDraft.trim())
                            }
                            showNoteEditor = false
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("save_habit_note_btn_${habit.id}")
                    ) {
                        Text("Save Note")
                    }
                }
            }

            // D-042 Low-frequency retrospective journal prompt
            if (retrospectivePromptEligible && onOpenRetrospectiveJournal != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("retrospective_prompt_${habit.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tough week for ${habit.name}? Reflect or adjust anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        TextButton(
                            onClick = onOpenRetrospectiveJournal,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("retrospective_write_btn_${habit.id}")
                        ) {
                            Text("Reflect")
                        }
                        if (onDismissRetrospectivePrompt != null) {
                            TextButton(
                                onClick = onDismissRetrospectivePrompt,
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Later")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyCheckOffProgressBanner(
    habit: Habit,
    summary: HabitSummary
) {
    val band = summary.weekProgressBand
    val pct = summary.weekPercentage
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "This week: ${summary.weekTotalUnits} / ${summary.weekTargetUnits} days ($pct%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("habit_weekly_progress_text_${habit.id}")
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.testTag("habit_band_badge_${habit.id}")
            ) {
                Text(
                    text = band.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        LinearProgressIndicator(
            progress = { (pct.toFloat() / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun QuantityOrDurationSection(
    habit: Habit,
    summary: HabitSummary?,
    entries: List<HabitEntry>,
    onAddEntry: (Int, String?) -> Unit,
    onOpenRecordSession: ((String?) -> Unit)? = null,
    onDeleteEntry: (String) -> Unit
) {
    val isWeekly = (summary?.effectiveVersion?.targetPeriod ?: habit.targetPeriod) == TargetPeriod.WEEK
    val target = if (isWeekly && summary != null) {
        summary.weekTargetUnits.min(BigInteger.valueOf(Int.MAX_VALUE.toLong())).toInt().coerceAtLeast(1)
    } else {
        (summary?.effectiveVersion?.targetValue ?: habit.targetValue ?: 1).coerceAtLeast(1)
    }
    val targetBig = BigInteger.valueOf(target.toLong())
    val unitLabel = summary?.effectiveVersion?.unitCode
        ?: habit.unitCode
        ?: if (habit.trackingMode == TrackingMode.DURATION) "min" else "units"

    val todayUnitsBig = ProgressBandClassifier.sumUnitsExact(entries.map { it.valueUnits })
    val activeUnitsBig = if (isWeekly && summary != null) summary.weekTotalUnits else todayUnitsBig
    val band = ProgressBandClassifier.classify(activeUnitsBig, targetBig)
    val pct = ProgressBandClassifier.displayPercentage(activeUnitsBig, targetBig)

    var customAmountText by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Progress bar + Band badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val progressPrefix = if (isWeekly) "This week: " else ""
            Text(
                text = "$progressPrefix$activeUnitsBig / $target $unitLabel ($pct%)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("habit_progress_text_${habit.id}")
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.testTag("habit_band_badge_${habit.id}")
            ) {
                Text(
                    text = band.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        if (isWeekly && todayUnitsBig > BigInteger.ZERO) {
            Text(
                text = "Today: +$todayUnitsBig $unitLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LinearProgressIndicator(
            progress = { (pct.toFloat() / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )

        if (entries.isNotEmpty() || activeUnitsBig > BigInteger.ZERO) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = band.encouragementQuote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = band.motivationQuote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick-add buttons
        val stepSmall = (target / 4).coerceAtLeast(1)
        val stepHalf = (target / 2).coerceAtLeast(1)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = { onAddEntry(stepSmall, null) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("quick_add_small_${habit.id}"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("+$stepSmall $unitLabel", fontSize = 12.sp)
            }
            if (stepHalf != stepSmall) {
                OutlinedButton(
                    onClick = { onAddEntry(stepHalf, null) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("quick_add_half_${habit.id}"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+$stepHalf $unitLabel", fontSize = 12.sp)
                }
            }
            Button(
                onClick = {
                    val remainingBig = targetBig.subtract(activeUnitsBig)
                    val remaining = if (remainingBig <= BigInteger.ONE) 1 else remainingBig.min(BigInteger.valueOf(Int.MAX_VALUE.toLong())).toInt()
                    onAddEntry(remaining, null)
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("quick_add_goal_${habit.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Goal", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = {
                    if (onOpenRecordSession != null) {
                        onOpenRecordSession(null)
                    } else {
                        showCustomInput = !showCustomInput
                    }
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("open_record_session_${habit.id}"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Record session for ${habit.name}", modifier = Modifier.size(16.dp))
            }
            OutlinedButton(
                onClick = { showCustomInput = !showCustomInput },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("toggle_custom_entry_${habit.id}"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Inline custom entry amount", modifier = Modifier.size(16.dp))
            }
        }

        if (showCustomInput) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = { customAmountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Amount ($unitLabel, 0 allowed)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_entry_input_${habit.id}")
                )
                Button(
                    onClick = {
                        val amt = customAmountText.toIntOrNull()
                        if (amt != null && amt >= 0) {
                            onAddEntry(amt, null)
                            customAmountText = ""
                            showCustomInput = false
                        }
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("submit_custom_entry_${habit.id}")
                ) {
                    Text("Log")
                }
            }
        }

        // Individual entries list for today/selected date
        if (entries.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (entry in entries) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (entry.valueUnits == 0) "0 $unitLabel (Checked in)"
                            else "+${entry.valueUnits} $unitLabel" + (entry.entryNote?.let { " • $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onOpenRecordSession != null) {
                                IconButton(
                                    onClick = { onOpenRecordSession(entry.entryId) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("edit_entry_${entry.entryId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit session",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onDeleteEntry(entry.entryId) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("delete_entry_${entry.entryId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete entry",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordedOutcomeRow(
    outcome: HabitDayOutcome,
    onReset: () -> Unit
) {
    val effort = when (outcome.effortLevel) {
        "TINY" -> EffortLevel.TINY
        "EXTRA" -> EffortLevel.EXTRA
        else -> EffortLevel.USUAL
    }
    val effColor = when (effort) {
        EffortLevel.TINY -> TinySproutColor
        EffortLevel.USUAL -> UsualLeafColor
        EffortLevel.EXTRA -> ExtraBranchColor
        EffortLevel.STANDARD -> UsualLeafColor
    }
    val label = if (outcome.outcomeKind == "COMPLETED") {
        "Completed for today"
    } else {
        "${effort.displayName()} recorded"
    }
    val symbol = if (outcome.outcomeKind == "COMPLETED") "✓" else effort.symbol()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(effColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = symbol, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = effColor
            )
        }

        OutlinedButton(
            onClick = onReset,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, effColor.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = effColor),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("reset_outcome_btn_${outcome.habitId}")
        ) {
            Text(text = "Change", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun UnrecordedOutcomeActionRow(
    habit: Habit,
    onLog: (outcome: DailyOutcome, effort: EffortLevel) -> Unit
) {
    if (habit.trackingMode.canonical() == TrackingMode.EFFORT_AWARE) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EffortButton(
                symbol = "🌱",
                title = "Tiny",
                subtitle = habit.tinyDescription,
                color = TinySproutColor,
                modifier = Modifier.weight(1f),
                onClick = { onLog(DailyOutcome.COMPLETED, EffortLevel.TINY) }
            )
            EffortButton(
                symbol = "🌿",
                title = "Usual",
                subtitle = habit.usualDescription,
                color = UsualLeafColor,
                modifier = Modifier.weight(1f),
                onClick = { onLog(DailyOutcome.COMPLETED, EffortLevel.USUAL) }
            )
            EffortButton(
                symbol = "🌳",
                title = "Extra",
                subtitle = habit.extraDescription,
                color = ExtraBranchColor,
                modifier = Modifier.weight(1f),
                onClick = { onLog(DailyOutcome.COMPLETED, EffortLevel.EXTRA) }
            )
        }
    } else {
        Button(
            onClick = { onLog(DailyOutcome.COMPLETED, EffortLevel.STANDARD) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("habit_done_btn_${habit.id}"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Complete for Today", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EffortButton(
    symbol: String,
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag("effort_btn_${title.lowercase()}"),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = symbol, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
