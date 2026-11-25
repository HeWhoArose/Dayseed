package com.example.dayseed.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dayseed.data.UnitFormatter

/**
 * Content of the bottom sheet opened by the round control on a quantity or duration habit row.
 *
 * Two ways to log, in the person's own words:
 *  - **Complete**: records exactly what is left to reach the goal.
 *  - **Log an amount**: any amount greater than zero, less or more than what is left; going past the goal is fine
 *    and shown as a second lap on the progress bar.
 *
 * The live preview uses [HabitProgress.after], so the numbers match what the row will show after saving.
 */
@Composable
fun QuickLogSheetContent(
    habitName: String,
    progress: HabitProgress,
    onComplete: (units: Int) -> Unit,
    onLogAmount: (units: Int) -> Unit,
    modifier: Modifier = Modifier,
    onTakeRestDay: (() -> Unit)? = null,
    onOpenRecord: (() -> Unit)? = null,
    onAddNote: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf("") }
    val parsed = remember(text, progress.unitLabel) { UnitFormatter.parseUserValueToStoredUnits(text, progress.unitLabel) }
    val valid = parsed != null && parsed > 0
    val showError = text.isNotBlank() && !valid
    val preview = if (valid) progress.after(parsed!!) else null
    val shown = preview ?: progress
    val decimal = UnitFormatter.extractScale(progress.unitLabel) > 0 || UnitFormatter.isCurrencyUnit(progress.unitLabel)
    val unitName = UnitFormatter.cleanUnitCode(progress.unitLabel).ifBlank { "units" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("quick_log_sheet"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = habitName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.dayseedHeading()
            )
            Text(
                text = if (progress.isWeekly) "Log progress for this week" else "Log progress for today",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Where things stand (and where they will stand after saving), with the bar that can run past the goal.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = shown.ratioText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("quick_log_ratio")
                )
                DayseedStatusPill(
                    text = if (shown.isGoalReached && shown.overUnits.signum() == 0) "Goal reached" else shown.band.label,
                    modifier = Modifier.semantics { contentDescription = shown.band.spokenDescription() }
                )
            }
            ProgressBarWithOverflow(fraction = shown.fraction, height = 10.dp, modifier = Modifier.testTag("quick_log_bar"))
            val over = shown.overText()
            Text(
                text = when {
                    preview != null && over != null -> "After saving: ${shown.percent}% - $over. Lovely."
                    preview != null -> "After saving: ${shown.percent}% of your goal."
                    over != null -> "${shown.percent}% - $over."
                    shown.isGoalReached -> "Goal reached. Add more if you did more."
                    else -> "${shown.percent}% so far."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("quick_log_summary")
            )
        }

        if (!progress.isGoalReached) {
            val remainingText = UnitFormatter.formatValueWithUnit(progress.remainingUnits, progress.unitLabel)
            DayseedPrimaryButton(
                text = if (progress.isSessionCount) "Complete (+1 session)" else "Complete (+$remainingText)",
                onClick = { onComplete(progress.remainingUnits) },
                icon = Icons.Default.Check,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Complete $habitName: add $remainingText" }
                    .testTag("quick_log_complete_btn")
            )
        }

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text("or log what you did", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Amount ($unitName)") },
            supportingText = { Text(if (showError) "Enter an amount above 0." else "More or less than the goal is fine.") },
            isError = showError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("quick_log_amount_input")
        )

        val steps = UnitFormatter.quickIncrementStoredUnits(progress.targetUnits, progress.unitLabel)
        if (steps != null && !progress.isSessionCount) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(steps.first, steps.second).distinct().forEachIndexed { i, step ->
                    DayseedSecondaryButton(
                        text = "+" + UnitFormatter.formatStoredUnitsForInput(step, progress.unitLabel),
                        onClick = {
                            val current = (parsed ?: 0).coerceAtLeast(0)
                            text = UnitFormatter.formatStoredUnitsForInput(current + step, progress.unitLabel)
                        },
                        modifier = Modifier.testTag("quick_log_step_$i")
                    )
                }
            }
        }

        DayseedSecondaryButton(
            text = "Save amount",
            onClick = { if (valid) onLogAmount(parsed!!) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().testTag("quick_log_save_btn")
        )

        if (onTakeRestDay != null || onOpenRecord != null || onAddNote != null) {
            Column {
                if (onTakeRestDay != null) {
                    TextButton(onClick = onTakeRestDay, modifier = Modifier.testTag("quick_log_rest_day_btn")) {
                        Text("Take a rest day (keeps your streak)")
                    }
                }
                if (onAddNote != null) {
                    TextButton(onClick = onAddNote, modifier = Modifier.testTag("quick_log_add_note_btn")) {
                        Text("Add a note")
                    }
                }
                if (onOpenRecord != null) {
                    TextButton(onClick = onOpenRecord, modifier = Modifier.testTag("quick_log_open_record_btn")) {
                        Text("Today's entries")
                    }
                }
            }
        }
    }
}
