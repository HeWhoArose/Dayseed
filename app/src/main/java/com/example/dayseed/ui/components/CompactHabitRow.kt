package com.example.dayseed.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.graphics.toColorInt
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayseed.data.DailyOutcome
import com.example.dayseed.data.EffortLevel
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitDateException
import com.example.dayseed.data.HabitDateNote
import com.example.dayseed.data.HabitDayOutcome
import com.example.dayseed.data.HabitEntry
import com.example.dayseed.data.HabitSummary
import com.example.dayseed.data.StreakUnit
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TrackingMode

/**
 * One habit on Today, as a compact row with a single round action on the right (owner decision D-235).
 *
 * - Row body: name, one line of context, and for amount habits a progress bar that keeps going past the goal.
 *   Tapping the body opens the habit's details.
 * - Right-hand control, by habit type:
 *     amount habits (quantity or duration): a ring with "+" that opens the quick-log sheet (Complete, or log an
 *       amount); a filled tick once the goal is reached (tapping still opens the sheet, to add more); a gold edge
 *       when over the goal.
 *     check-off habits: an empty ring that completes the habit in one tap; a filled tick that undoes it on tap.
 *       A long press takes a rest day.
 * - Everything the old card carried is kept: rest day (and resuming it), weekly check-off progress, the legacy
 *   effort notice, the date note and the retrospective prompt.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactHabitRow(
    habit: Habit,
    outcome: HabitDayOutcome? = null,
    entries: List<HabitEntry> = emptyList(),
    dateNote: HabitDateNote? = null,
    restDayException: HabitDateException? = null,
    summary: HabitSummary? = null,
    retrospectivePromptEligible: Boolean = false,
    onOpenHabitDetail: (() -> Unit)? = null,
    onOpenQuickLog: (() -> Unit)? = null,
    onLogOutcome: (outcome: DailyOutcome, effort: EffortLevel) -> Unit,
    onTakeRestDayToday: (() -> Unit)? = null,
    onClearRestDayToday: (() -> Unit)? = null,
    onOpenRetrospectiveJournal: (() -> Unit)? = null,
    onDismissRetrospectivePrompt: (() -> Unit)? = null,
    onResetRecord: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val effVersion = summary?.effectiveVersion
    val mode = (effVersion?.trackingMode ?: habit.trackingMode).canonical()
    val progress = computeHabitProgress(habit, summary, entries)
    val isWeeklyCheckOff = progress == null && summary != null &&
        (effVersion?.targetPeriod ?: habit.targetPeriod) == TargetPeriod.WEEK
    val isRecorded = outcome != null || entries.isNotEmpty() || restDayException != null
    val restDay = restDayException != null
    val dot = runCatching { Color(habit.colorHex.toColorInt()) }.getOrDefault(scheme.primary)

    // One line of context under the name.
    val streak = if (summary != null && (summary.currentCompletedStreak > 0 || summary.currentElapsedActiveSpan > 0)) {
        // Compound adjective, always singular ("12-day streak", "2-week streak").
        "${summary.currentCompletedStreak}-${if (summary.streakUnit == StreakUnit.WEEKS) "week" else "day"} streak"
    } else null
    val context = buildList {
        when {
            restDay -> add("🍃 Rest day taken · streak held")
            progress != null -> {
                add(if (progress.isWeekly) "This week: ${progress.ratioText}" else progress.ratioText)
                add(if (progress.isGoalReached && progress.overUnits.signum() == 0) "Goal reached" else progress.band.label)
            }
            isWeeklyCheckOff -> add("This week: ${summary!!.weekTotalUnits} / ${summary.weekTargetUnits} days (${summary.weekPercentage}%)")
            mode == TrackingMode.EFFORT_AWARE -> add("Legacy effort habit — Edit habit to set a Quantity, Duration, or Check-Off goal.")
            summary != null -> add(summary.formatScheduleSummary())
            habit.description.isNotBlank() -> add(habit.description)
        }
        if (streak != null && !restDay) add(streak)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}")
            .semantics { contentDescription = "Habit ${habit.name}" },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(
            1.dp,
            if (isRecorded) scheme.primary.copy(alpha = 0.35f) else scheme.outlineVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Row body: opens details.
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (onOpenHabitDetail != null) {
                                Modifier.clickable(
                                    role = Role.Button,
                                    onClickLabel = "Open details for ${habit.name}"
                                ) { onOpenHabitDetail() }
                            } else Modifier
                        )
                        .padding(vertical = 4.dp)
                        .testTag("open_habit_detail_${habit.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dot))
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (context.isNotEmpty()) {
                            Text(
                                text = context.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (streak != null || progress?.isGoalReached == true) scheme.primary else scheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .testTag(
                                        when {
                                            restDay -> "habit_rest_day_banner_${habit.id}"
                                            mode == TrackingMode.EFFORT_AWARE && progress == null -> "legacy_effort_mode_notice_${habit.id}"
                                            progress != null -> "habit_progress_text_${habit.id}"
                                            else -> "habit_streak_badge_${habit.id}"
                                        }
                                    )
                            )
                        }
                        if (progress != null && !restDay) {
                            ProgressBarWithOverflow(
                                fraction = progress.fraction,
                                height = 5.dp,
                                modifier = Modifier.padding(end = 8.dp).testTag("habit_progress_bar_${habit.id}")
                            )
                        } else if (isWeeklyCheckOff && !restDay) {
                            ProgressBarWithOverflow(
                                fraction = summary!!.weekPercentage.toFloat() / 100f,
                                height = 5.dp,
                                modifier = Modifier.padding(end = 8.dp).testTag("habit_progress_bar_${habit.id}")
                            )
                        }
                    }
                }

                // The single round action.
                when {
                    restDay -> if (onClearRestDayToday != null) {
                        TextButton(
                            onClick = onClearRestDayToday,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("clear_rest_day_btn_${habit.id}")
                        ) { Text("Resume") }
                    }
                    progress != null -> RoundAction(
                        fraction = progress.fraction,
                        done = progress.isGoalReached,
                        over = progress.overUnits.signum() > 0,
                        showPlus = true,
                        description = if (progress.isGoalReached) "Add more for ${habit.name}" else "Log progress for ${habit.name}",
                        onClick = { onOpenQuickLog?.invoke() },
                        modifier = Modifier.testTag("quick_log_btn_${habit.id}")
                    )
                    outcome != null -> RoundAction(
                        fraction = 1f, done = true, over = false, showPlus = false,
                        description = "${habit.name} is complete for today. Tap to undo.",
                        onClick = onResetRecord,
                        modifier = Modifier.testTag("reset_outcome_btn_${habit.id}")
                    )
                    else -> RoundAction(
                        fraction = 0f, done = false, over = false, showPlus = false,
                        description = "Mark ${habit.name} complete",
                        onClick = { onLogOutcome(DailyOutcome.COMPLETED, EffortLevel.STANDARD) },
                        onLongClick = onTakeRestDayToday,
                        longClickLabel = "Take a rest day",
                        modifier = Modifier.testTag("habit_done_btn_${habit.id}")
                    )
                }
            }

            if (dateNote != null) {
                Text(
                    text = "Note: ${dateNote.body}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 22.dp, bottom = 6.dp, end = 8.dp).testTag("habit_date_note_${habit.id}")
                )
            }

            // D-042 low-frequency retrospective journal prompt (unchanged wording and tags).
            if (retrospectivePromptEligible && onOpenRetrospectiveJournal != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(scheme.primaryContainer.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("retrospective_prompt_${habit.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tough week for ${habit.name}? Reflect or adjust anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        TextButton(
                            onClick = onOpenRetrospectiveJournal,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("retrospective_write_btn_${habit.id}")
                        ) { Text("Reflect") }
                        if (onDismissRetrospectivePrompt != null) {
                            TextButton(onClick = onDismissRetrospectivePrompt, modifier = Modifier.heightIn(min = 48.dp)) { Text("Later") }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The 48 dp round control: an empty ring (to do), a ring that fills as progress grows (with "+" for amount
 * habits), or a filled tick (done). A gold edge marks progress beyond the goal; the bar and the text say so too.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoundAction(
    fraction: Float,
    done: Boolean,
    over: Boolean,
    showPlus: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    longClickLabel: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    val ring = scheme.outline
    val primary = scheme.primary
    val extra = progressExtraColor()
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = description,
                onLongClick = onLongClick,
                onLongClickLabel = longClickLabel,
                onClick = onClick
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(34.dp)) {
            val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2f
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            if (done) {
                drawCircle(primary)
                if (over) drawCircle(extra, radius = size.minDimension / 2f - inset, style = stroke)
            } else {
                drawCircle(ring.copy(alpha = if (fraction > 0f) 0.45f else 1f), style = stroke)
                if (fraction > 0f) {
                    drawArc(primary, -90f, 360f * fraction.coerceAtMost(1f), false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
                }
            }
        }
        if (done) Icon(Icons.Default.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(20.dp))
        else if (showPlus) Icon(Icons.Default.Add, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
    }
}
