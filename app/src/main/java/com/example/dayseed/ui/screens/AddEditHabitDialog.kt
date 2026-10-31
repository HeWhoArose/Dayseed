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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dayseed.data.DurationNormalizer
import com.example.dayseed.data.Habit
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.WeekdayMask
import java.time.DayOfWeek
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitDialog(
    initialHabit: Habit?,
    alreadyLoggedToday: Boolean = false,
    suggestedReminderTime: String? = null,
    onDismiss: () -> Unit,
    onSave: (habit: Habit, applyTodayWhenLogged: Boolean) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val isEditing = initialHabit != null

    var name by remember { mutableStateOf(initialHabit?.name ?: "") }
    var description by remember { mutableStateOf(initialHabit?.description ?: "") }
    var trackingMode by remember {
        mutableStateOf((initialHabit?.trackingMode ?: TrackingMode.EFFORT_AWARE).canonical())
    }
    var targetValueText by remember {
        mutableStateOf((initialHabit?.targetValue ?: 15).toString())
    }
    var unitCode by remember {
        mutableStateOf(
            initialHabit?.unitCode ?: when ((initialHabit?.trackingMode ?: TrackingMode.EFFORT_AWARE).canonical()) {
                TrackingMode.DURATION -> "min"
                TrackingMode.QUANTITY -> "pages"
                else -> ""
            }
        )
    }
    var scheduleKind by remember {
        mutableStateOf(initialHabit?.scheduleKind ?: ScheduleKind.DAILY)
    }
    var tinyDesc by remember { mutableStateOf(initialHabit?.tinyDescription ?: "Deliberately small action") }
    var usualDesc by remember { mutableStateOf(initialHabit?.usualDescription ?: "The ordinary intended action") }
    var extraDesc by remember { mutableStateOf(initialHabit?.extraDescription ?: "Optional stretch action") }
    var selectedDays by remember {
        mutableStateOf(initialHabit?.scheduleDays ?: DayOfWeek.values().toSet())
    }
    var selectedColor by remember { mutableStateOf(initialHabit?.colorHex ?: "#286143") }
    var reminderEnabled by remember { mutableStateOf(initialHabit?.reminderMode == "AT_TIME") }
    var reminderTimeLocal by remember {
        mutableStateOf(initialHabit?.reminderTimeLocal ?: suggestedReminderTime ?: "09:00")
    }
    var applyTodayWhenLogged by remember { mutableStateOf(true) }
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

                Spacer(modifier = Modifier.height(12.dp))

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

                Spacer(modifier = Modifier.height(6.dp))

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

                Spacer(modifier = Modifier.height(14.dp))

                // Tracking Mode Choice (4 Canonical Modes)
                Text(
                    text = "Tracking Mode",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                val modes = listOf(
                    Triple(TrackingMode.EFFORT_AWARE, "Effort-Aware (Recommended)", "Tiny, Usual, and Extra actions count"),
                    Triple(TrackingMode.COMPLETION_ONLY, "Completion Only", "Simple done check-in"),
                    Triple(TrackingMode.DURATION, "Duration", "Track minutes or hours toward a goal"),
                    Triple(TrackingMode.QUANTITY, "Quantity", "Track counts (pages, glasses, steps) toward a goal")
                )

                for ((m, title, sub) in modes) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                trackingMode = m
                                if (m == TrackingMode.DURATION && !DurationNormalizer.isSupportedDurationUnit(unitCode)) {
                                    unitCode = "min"
                                } else if (m == TrackingMode.QUANTITY && unitCode.isBlank()) {
                                    unitCode = "pages"
                                }
                            }
                            .testTag("mode_option_${m.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = trackingMode == m,
                            onClick = {
                                trackingMode = m
                                if (m == TrackingMode.DURATION && !DurationNormalizer.isSupportedDurationUnit(unitCode)) {
                                    unitCode = "min"
                                } else if (m == TrackingMode.QUANTITY && unitCode.isBlank()) {
                                    unitCode = "pages"
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Target & Unit configuration for QUANTITY / DURATION
                if (trackingMode == TrackingMode.QUANTITY || trackingMode == TrackingMode.DURATION) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = targetValueText,
                            onValueChange = { targetValueText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Target Goal *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("habit_target_value_input")
                        )

                        if (trackingMode == TrackingMode.QUANTITY) {
                            OutlinedTextField(
                                value = unitCode,
                                onValueChange = { unitCode = it },
                                label = { Text("Unit") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("habit_unit_code_input")
                            )
                        }
                    }

                    if (trackingMode == TrackingMode.DURATION) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Duration Unit",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (u in DurationNormalizer.supportedDurationUnits) {
                                FilterChip(
                                    selected = unitCode == u,
                                    onClick = { unitCode = u },
                                    label = { Text(u) }
                                )
                            }
                        }
                    }
                }

                if (trackingMode == TrackingMode.EFFORT_AWARE) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Customize Effort Definitions",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = tinyDesc,
                        onValueChange = { tinyDesc = it },
                        label = { Text("🌱 Tiny effort (bare minimum that counts)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tiny_desc_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = usualDesc,
                        onValueChange = { usualDesc = it },
                        label = { Text("🌿 Usual effort (intended ordinary action)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("usual_desc_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = extraDesc,
                        onValueChange = { extraDesc = it },
                        label = { Text("🌳 Extra effort (optional stretch)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("extra_desc_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Schedule Kind & Days
                Text(
                    text = "Schedule Type",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = scheduleKind == ScheduleKind.DAILY,
                        onClick = {
                            scheduleKind = ScheduleKind.DAILY
                            selectedDays = DayOfWeek.values().toSet()
                        },
                        label = { Text("Daily") }
                    )
                    FilterChip(
                        selected = scheduleKind == ScheduleKind.SELECTED_WEEKDAYS,
                        onClick = { scheduleKind = ScheduleKind.SELECTED_WEEKDAYS },
                        label = { Text("Weekdays") }
                    )
                    FilterChip(
                        selected = scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
                        onClick = { scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET },
                        label = { Text("Flexible Week") }
                    )
                }

                if (scheduleKind != ScheduleKind.DAILY) {
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
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Reminder Configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gentle Daily Reminder",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (suggestedReminderTime != null) {
                            Text(
                                text = "Suggested from your rhythm: $suggestedReminderTime",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        modifier = Modifier.testTag("habit_reminder_switch")
                    )
                }

                if (reminderEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = reminderTimeLocal,
                        onValueChange = { reminderTimeLocal = it },
                        label = { Text("Reminder Time (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("habit_reminder_time_input")
                    )
                }

                // D-194 Apply today vs Apply tomorrow when editing a habit already logged today
                if (isEditing && alreadyLoggedToday) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("d194_effective_date_prompt")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "You already logged this habit today",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "When should these updated habit settings take effect?",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = applyTodayWhenLogged,
                                    onClick = { applyTodayWhenLogged = true },
                                    label = { Text("Apply today") },
                                    modifier = Modifier.testTag("apply_today_chip")
                                )
                                FilterChip(
                                    selected = !applyTodayWhenLogged,
                                    onClick = { applyTodayWhenLogged = false },
                                    label = { Text("Apply tomorrow") },
                                    modifier = Modifier.testTag("apply_tomorrow_chip")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                        val c = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (_: Exception) {
                            Color.Green
                        }
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

                Spacer(modifier = Modifier.height(20.dp))

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
                                val targetVal = if (trackingMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION)) {
                                    (targetValueText.toIntOrNull() ?: 1).coerceAtLeast(1)
                                } else {
                                    null
                                }
                                val targetPer = if (scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET) {
                                    TargetPeriod.WEEK
                                } else {
                                    TargetPeriod.DAY
                                }
                                val mask = WeekdayMask.toMask(selectedDays)
                                val habit = Habit(
                                    habitId = initialHabit?.habitId ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    description = description.trim(),
                                    trackingMode = trackingMode,
                                    unitCode = if (trackingMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION)) unitCode.trim() else null,
                                    targetPeriod = targetPer,
                                    targetValue = targetVal,
                                    scheduleKind = scheduleKind,
                                    scheduledWeekdaysMask = if (scheduleKind == ScheduleKind.SELECTED_WEEKDAYS) mask else null,
                                    preferredWeekdaysMask = if (scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET) mask else null,
                                    lifecycleState = initialHabit?.lifecycleState ?: com.example.dayseed.data.HabitLifecycleState.ACTIVE,
                                    reminderMode = if (reminderEnabled) "AT_TIME" else "OFF",
                                    reminderTimeLocal = if (reminderEnabled) reminderTimeLocal.trim() else null,
                                    tinyDescription = tinyDesc.trim(),
                                    usualDescription = usualDesc.trim(),
                                    extraDescription = extraDesc.trim(),
                                    scheduleDays = selectedDays,
                                    colorHex = selectedColor,
                                    createdAtUtcMs = initialHabit?.createdAtUtcMs ?: System.currentTimeMillis()
                                )
                                onSave(habit, applyTodayWhenLogged)
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
