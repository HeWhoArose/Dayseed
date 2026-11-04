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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    onTakeRestDayToday: (() -> Unit)? = null,
    onClearRestDayToday: (() -> Unit)? = null,
    onOpenRetrospectiveJournal: (() -> Unit)? = null,
    onDismissRetrospectivePrompt: (() -> Unit)? = null,
    onResetRecord: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
            // Header Row: Habit Name (tappable to open HabitDetail), Target/Schedule summary, Rest day action
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

                if (summary?.canTakeRestDayToday == true && onTakeRestDayToday != null && restDayException == null) {
                    TextButton(
                        onClick = onTakeRestDayToday,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("take_rest_day_btn_${habit.id}")
                    ) {
                        Text(
                            text = "Rest day",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            onOpenRecordSession = onOpenRecordSession
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
                                onReset = onResetRecord,
                                onOpenRecordSession = onOpenRecordSession
                            )
                        } else {
                            UnrecordedOutcomeActionRow(
                                habit = habit,
                                summary = summary,
                                onLog = onLogOutcome,
                                onOpenRecordSession = onOpenRecordSession
                            )
                        }
                    }

                    else -> {}
                }
            }

            // Read-only date note preview (editing lives in Habit Detail / Record Session)
            if (dateNote != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: ${dateNote.body}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("habit_date_note_${habit.id}")
                )
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
    onOpenRecordSession: ((String?) -> Unit)? = null
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

    val isSessionCountHabit = isWeekly &&
        (summary?.effectiveVersion?.trackingMode ?: habit.trackingMode).canonical() == TrackingMode.QUANTITY &&
        unitLabel.trim().equals("sessions", ignoreCase = true)
    val todayUnitsBig = if (isSessionCountHabit) {
        BigInteger.valueOf(entries.count { it.valueUnits > 0 }.toLong())
    } else {
        ProgressBandClassifier.sumUnitsExact(entries.map { it.valueUnits })
    }
    val activeUnitsBig = if (isWeekly && summary != null) summary.weekTotalUnits else todayUnitsBig
    val band = ProgressBandClassifier.classify(activeUnitsBig, targetBig)
    val pct = ProgressBandClassifier.displayPercentage(activeUnitsBig, targetBig)

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

        // Separate Quick Complete / Quick Add actions and detailed Record action
        val stepSmall = if (isSessionCountHabit) 1 else (target / 4).coerceAtLeast(1)
        val stepHalf = if (isSessionCountHabit) 1 else (target / 2).coerceAtLeast(1)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!isSessionCountHabit) {
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
            }
            Button(
                onClick = {
                    if (isSessionCountHabit) {
                        onAddEntry(1, null)
                    } else {
                        val remainingBig = targetBig.subtract(activeUnitsBig)
                        val remaining = if (remainingBig <= BigInteger.ONE) 1 else remainingBig.min(BigInteger.valueOf(Int.MAX_VALUE.toLong())).toInt()
                        onAddEntry(remaining, null)
                    }
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
                Text(if (isSessionCountHabit) "+1 Session" else "Complete Goal", fontSize = 12.sp)
            }
            if (onOpenRecordSession != null) {
                OutlinedButton(
                    onClick = { onOpenRecordSession(null) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("open_record_session_${habit.id}"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Record session for ${habit.name}",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Record", fontSize = 12.sp)
                }
            }
        }

        // Compact, read-only session count summary (editing/deletion lives in HabitDetailScreen & RecordSessionSheet)
        if (entries.isNotEmpty()) {
            val sessionCountText = if (entries.size == 1) "1 session logged today" else "${entries.size} sessions logged today"
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_today_sessions_summary_${habit.id}")
            ) {
                Text(
                    text = sessionCountText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun RecordedOutcomeRow(
    outcome: HabitDayOutcome,
    onReset: () -> Unit,
    onOpenRecordSession: ((String?) -> Unit)? = null
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = symbol, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = effColor
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (onOpenRecordSession != null) {
                OutlinedButton(
                    onClick = { onOpenRecordSession(null) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("open_record_session_${outcome.habitId}")
                ) {
                    Text(text = "Record", style = MaterialTheme.typography.labelMedium)
                }
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
}

@Composable
private fun UnrecordedOutcomeActionRow(
    habit: Habit,
    summary: HabitSummary? = null,
    onLog: (outcome: DailyOutcome, effort: EffortLevel) -> Unit,
    onOpenRecordSession: ((String?) -> Unit)? = null
) {
    val effVersion = summary?.effectiveVersion
    val mode = (effVersion?.trackingMode ?: habit.trackingMode).canonical()
    if (mode == TrackingMode.EFFORT_AWARE) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EffortButton(
                symbol = "🌱",
                title = "Tiny",
                subtitle = effVersion?.tinyDescription ?: habit.tinyDescription,
                color = TinySproutColor,
                modifier = Modifier.weight(1f),
                onClick = { onLog(DailyOutcome.COMPLETED, EffortLevel.TINY) }
            )
            EffortButton(
                symbol = "🌿",
                title = "Usual",
                subtitle = effVersion?.usualDescription ?: habit.usualDescription,
                color = UsualLeafColor,
                modifier = Modifier.weight(1f),
                onClick = { onLog(DailyOutcome.COMPLETED, EffortLevel.USUAL) }
            )
            EffortButton(
                symbol = "🌳",
                title = "Extra",
                subtitle = effVersion?.extraDescription ?: habit.extraDescription,
                color = ExtraBranchColor,
                modifier = Modifier.weight(1f),
                onClick = { onLog(DailyOutcome.COMPLETED, EffortLevel.EXTRA) }
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onLog(DailyOutcome.COMPLETED, EffortLevel.STANDARD) },
                modifier = Modifier
                    .weight(1f)
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
                Text("Complete", style = MaterialTheme.typography.labelLarge)
            }
            if (onOpenRecordSession != null) {
                OutlinedButton(
                    onClick = { onOpenRecordSession(null) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("open_record_session_${habit.id}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Record", style = MaterialTheme.typography.labelLarge)
                }
            }
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
