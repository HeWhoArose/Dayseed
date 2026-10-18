package com.example.dayseed.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dayseed.data.Habit
import com.example.dayseed.data.TrackingMode
import java.time.DayOfWeek
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitDialog(
    initialHabit: Habit?,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val isEditing = initialHabit != null

    var name by remember { mutableStateOf(initialHabit?.name ?: "") }
    var description by remember { mutableStateOf(initialHabit?.description ?: "") }
    var trackingMode by remember { mutableStateOf(initialHabit?.trackingMode ?: TrackingMode.EFFORT_LEVELS) }
    var tinyDesc by remember { mutableStateOf(initialHabit?.tinyDescription ?: "Deliberately small action") }
    var usualDesc by remember { mutableStateOf(initialHabit?.usualDescription ?: "The ordinary intended action") }
    var extraDesc by remember { mutableStateOf(initialHabit?.extraDescription ?: "Optional stretch action") }
    var selectedDays by remember {
        mutableStateOf(initialHabit?.scheduleDays ?: DayOfWeek.values().toSet())
    }
    var selectedColor by remember { mutableStateOf(initialHabit?.colorHex ?: "#286143") }
    var nameError by remember { mutableStateOf(false) }

    val colorOptions = listOf("#286143", "#3B6955", "#4A7C59", "#708D81", "#8D6B94", "#C08552")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("add_edit_habit_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Habit" else "New Habit",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("Habit name *") },
                    placeholder = { Text("e.g. Read, Gentle Stretch, Hydrate") },
                    isError = nameError,
                    supportingText = {
                        if (nameError) Text("Please enter a habit name")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Purpose or note (optional)") },
                    placeholder = { Text("e.g. For peace of mind and clarity") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_desc_input"),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tracking Mode Choice
                Text(
                    text = "Tracking Mode",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { trackingMode = TrackingMode.EFFORT_LEVELS },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = trackingMode == TrackingMode.EFFORT_LEVELS,
                        onClick = { trackingMode = TrackingMode.EFFORT_LEVELS }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Effort Levels (Recommended)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Tiny, Usual, and Extra actions count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { trackingMode = TrackingMode.COMPLETION_ONLY },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = trackingMode == TrackingMode.COMPLETION_ONLY,
                        onClick = { trackingMode = TrackingMode.COMPLETION_ONLY }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Completion Only", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Single done/not done action", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (trackingMode == TrackingMode.EFFORT_LEVELS) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Customize Effort Definitions",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = tinyDesc,
                        onValueChange = { tinyDesc = it },
                        label = { Text("🌱 Tiny effort (bare minimum that counts)") },
                        modifier = Modifier.fillMaxWidth().testTag("tiny_desc_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = usualDesc,
                        onValueChange = { usualDesc = it },
                        label = { Text("🌿 Usual effort (intended ordinary action)") },
                        modifier = Modifier.fillMaxWidth().testTag("usual_desc_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = extraDesc,
                        onValueChange = { extraDesc = it },
                        label = { Text("🌳 Extra effort (optional stretch)") },
                        modifier = Modifier.fillMaxWidth().testTag("extra_desc_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Schedule Days
                Text(
                    text = "Scheduled Days",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val days = listOf(
                        DayOfWeek.MONDAY to "M",
                        DayOfWeek.TUESDAY to "T",
                        DayOfWeek.WEDNESDAY to "W",
                        DayOfWeek.THURSDAY to "T",
                        DayOfWeek.FRIDAY to "F",
                        DayOfWeek.SATURDAY to "S",
                        DayOfWeek.SUNDAY to "S"
                    )
                    for ((d, label) in days) {
                        val isSelected = selectedDays.contains(d)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected) {
                                    if (selectedDays.size > 1) selectedDays - d else selectedDays
                                } else {
                                    selectedDays + d
                                }
                            },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color Tag
                Text(
                    text = "Habit Color",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (hex in colorOptions) {
                        val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Green }
                        val isPicked = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (isPicked) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions: Save, Cancel, and optional Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (initialHabit != null && onDelete != null) {
                        IconButton(
                            onClick = { onDelete(initialHabit.id) },
                            modifier = Modifier.testTag("delete_habit_btn")
                        ) {

                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete habit",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                            } else {
                                val habit = Habit(
                                    id = initialHabit?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    description = description.trim(),
                                    trackingMode = trackingMode,
                                    tinyDescription = tinyDesc.trim(),
                                    usualDescription = usualDesc.trim(),
                                    extraDescription = extraDesc.trim(),
                                    scheduleDays = selectedDays,
                                    isArchived = initialHabit?.isArchived ?: false,
                                    colorHex = selectedColor,
                                    createdAt = initialHabit?.createdAt ?: System.currentTimeMillis()
                                )
                                onSave(habit)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_habit_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
