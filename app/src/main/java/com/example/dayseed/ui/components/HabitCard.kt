package com.example.dayseed.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayseed.data.DailyOutcome
import com.example.dayseed.data.DailyRecord
import com.example.dayseed.data.EffortLevel
import com.example.dayseed.data.Habit
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.theme.ExtraBranchColor
import com.example.dayseed.ui.theme.RestColor
import com.example.dayseed.ui.theme.SkippedColor
import com.example.dayseed.ui.theme.TinySproutColor
import com.example.dayseed.ui.theme.UsualLeafColor

@Composable
fun HabitCard(
    habit: Habit,
    record: DailyRecord?,
    onLog: (outcome: DailyOutcome, effort: EffortLevel) -> Unit,
    onResetRecord: () -> Unit,
    onEditHabit: () -> Unit,
    onArchiveHabit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val isRecorded = record != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isRecorded) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Habit Name, Category indicator, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                try { Color(android.graphics.Color.parseColor(habit.colorHex)) }
                                catch (_: Exception) { MaterialTheme.colorScheme.primary }
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (habit.description.isNotBlank()) {
                            Text(
                                text = habit.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("habit_menu_${habit.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Habit options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Planned Rest Day") },
                            leadingIcon = { Icon(Icons.Default.Hotel, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onLog(DailyOutcome.PLANNED_REST, EffortLevel.STANDARD)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Record Skip") },
                            leadingIcon = { Icon(Icons.Default.SkipNext, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onLog(DailyOutcome.SKIPPED, EffortLevel.STANDARD)
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

            Spacer(modifier = Modifier.height(12.dp))

            // Action / Status Area
            if (record != null) {
                RecordedStateRow(
                    record = record,
                    onReset = onResetRecord
                )
            } else {

                UnrecordedActionRow(
                    habit = habit,
                    onLog = onLog
                )
            }
        }
    }
}

@Composable
private fun RecordedStateRow(
    record: DailyRecord,
    onReset: () -> Unit
) {
    val (bgColor, textColor, label, symbol) = when (record.outcome) {
        DailyOutcome.COMPLETED -> {
            val effColor = when (record.effortLevel) {
                EffortLevel.TINY -> TinySproutColor
                EffortLevel.USUAL -> UsualLeafColor
                EffortLevel.EXTRA -> ExtraBranchColor
                EffortLevel.STANDARD -> UsualLeafColor
            }
            Quadruple(
                effColor.copy(alpha = 0.12f),
                effColor,
                "${record.effortLevel.displayName()} recorded",
                record.effortLevel.symbol()
            )
        }
        DailyOutcome.PLANNED_REST -> {
            Quadruple(
                RestColor.copy(alpha = 0.15f),
                RestColor,
                "Rest day honored",
                "🌿"
            )
        }
        DailyOutcome.SKIPPED -> {
            Quadruple(
                SkippedColor.copy(alpha = 0.15f),
                SkippedColor,
                "Recorded skip",
                "⚪"
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
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
                color = textColor
            )
        }

        OutlinedButton(
            onClick = onReset,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
            modifier = Modifier.height(34.dp)
        ) {
            Text(text = "Change", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun UnrecordedActionRow(
    habit: Habit,
    onLog: (outcome: DailyOutcome, effort: EffortLevel) -> Unit
) {
    if (habit.trackingMode == TrackingMode.EFFORT_LEVELS) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tiny Effort Button
                EffortButton(
                    symbol = "🌱",
                    title = "Tiny",
                    subtitle = habit.tinyDescription,
                    color = TinySproutColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onLog(DailyOutcome.COMPLETED, EffortLevel.TINY) }
                )

                // Usual Effort Button
                EffortButton(
                    symbol = "🌿",
                    title = "Usual",
                    subtitle = habit.usualDescription,
                    color = UsualLeafColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onLog(DailyOutcome.COMPLETED, EffortLevel.USUAL) }
                )

                // Extra Effort Button
                EffortButton(
                    symbol = "🌳",
                    title = "Extra",
                    subtitle = habit.extraDescription,
                    color = ExtraBranchColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onLog(DailyOutcome.COMPLETED, EffortLevel.EXTRA) }
                )
            }
        }
    } else {
        Button(
            onClick = { onLog(DailyOutcome.COMPLETED, EffortLevel.STANDARD) },
            modifier = Modifier
                .fillMaxWidth()
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

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
