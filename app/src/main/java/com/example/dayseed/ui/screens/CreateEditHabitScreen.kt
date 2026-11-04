package com.example.dayseed.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.DurationNormalizer
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.WeekdayMask
import java.time.DayOfWeek
import java.util.UUID

fun weeklyTargetSemanticsExplanation(trackingMode: TrackingMode, unitCode: String?): String {
    val canonicalMode = trackingMode.canonical()
    return when {
        canonicalMode == TrackingMode.COMPLETION_ONLY || canonicalMode == TrackingMode.EFFORT_AWARE ->
            "Days/week counts distinct qualifying calendar dates (at most 1 day per date, 1–7 days/week)."
        canonicalMode == TrackingMode.QUANTITY && unitCode?.trim().equals("sessions", ignoreCase = true) ->
            "Sessions/week counts each logged session (value > 0) toward your weekly goal — multiple sessions on the same day count separately (unlike days/week)."
        else ->
            "Weekly target progress accumulates across all sessions logged during your configured week."
    }
}

@Composable
fun CreateEditHabitScreen(
    viewModel: DayseedViewModel,
    habitId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val habitEntries by viewModel.habitEntries.collectAsState()
    val habitDayOutcomes by viewModel.habitDayOutcomes.collectAsState()
    val saveError by viewModel.lastSaveError.collectAsState()
    val initialHabit = remember(habits, habitId) {
        habitId?.let { id -> habits.find { it.habitId == id } }
    }
    val today = viewModel.repository.clock.todayLocal()
    val alreadyLoggedToday = remember(initialHabit, today, habitEntries, habitDayOutcomes) {
        initialHabit?.let { viewModel.repository.hasRecordedActivityForVersionLockOnDate(it.habitId, today) } ?: false
    }
    val suggestedReminder = remember(initialHabit) {
        initialHabit?.let { viewModel.repository.getSuggestedReminderTimeForHabit(it.habitId) }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("create_edit_habit_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        CreateEditHabitFormContent(
            initialHabit = initialHabit,
            alreadyLoggedToday = alreadyLoggedToday,
            suggestedReminderTime = suggestedReminder,
            isFullScreen = true,
            saveErrorMessage = saveError,
            onClearSaveError = { viewModel.clearSaveError() },
            onDismiss = onNavigateBack,
            onSave = { habit, applyTodayWhenLogged ->
                viewModel.saveHabit(habit, applyTodayWhenLogged) != null
            },
            onDelete = if (initialHabit != null) {
                { id ->
                    viewModel.deleteHabit(id)
                }
            } else {
                null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEditHabitFormContent(
    initialHabit: Habit?,
    alreadyLoggedToday: Boolean = false,
    suggestedReminderTime: String? = null,
    isFullScreen: Boolean = false,
    saveErrorMessage: String? = null,
    onClearSaveError: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (habit: Habit, applyTodayWhenLogged: Boolean) -> Boolean,
    onDelete: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isEditing = initialHabit != null

    var name by remember(initialHabit) { mutableStateOf(initialHabit?.name ?: "") }
    var description by remember(initialHabit) { mutableStateOf(initialHabit?.description ?: "") }
    var category by remember(initialHabit) { mutableStateOf(initialHabit?.category ?: "") }
    var lifetimeGoalText by remember(initialHabit) {
        mutableStateOf(initialHabit?.lifetimeGoalDays?.toString() ?: "")
    }

    var trackingMode by remember(initialHabit) {
        mutableStateOf((initialHabit?.trackingMode ?: TrackingMode.COMPLETION_ONLY).canonical())
    }
    var targetPeriod by remember(initialHabit) {
        mutableStateOf(
            if (initialHabit?.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET) {
                TargetPeriod.WEEK
            } else {
                initialHabit?.targetPeriod ?: TargetPeriod.DAY
            }
        )
    }
    var targetValueText by remember(initialHabit) {
        mutableStateOf(
            (initialHabit?.targetValue ?: when {
                (initialHabit?.trackingMode ?: TrackingMode.COMPLETION_ONLY).canonical() == TrackingMode.DURATION -> 15
                (initialHabit?.trackingMode ?: TrackingMode.COMPLETION_ONLY).canonical() == TrackingMode.QUANTITY -> 10
                else -> 3
            }).toString()
        )
    }
    var unitCode by remember(initialHabit) {
        mutableStateOf(
            initialHabit?.unitCode ?: when ((initialHabit?.trackingMode ?: TrackingMode.COMPLETION_ONLY).canonical()) {
                TrackingMode.DURATION -> "min"
                TrackingMode.QUANTITY -> "pages"
                else -> ""
            }
        )
    }
    var scheduleKind by remember(initialHabit) {
        mutableStateOf(initialHabit?.scheduleKind ?: ScheduleKind.DAILY)
    }
    var tinyDesc by remember(initialHabit) {
        mutableStateOf(initialHabit?.tinyDescription ?: "Deliberately small action")
    }
    var usualDesc by remember(initialHabit) {
        mutableStateOf(initialHabit?.usualDescription ?: "The ordinary intended action")
    }
    var extraDesc by remember(initialHabit) {
        mutableStateOf(initialHabit?.extraDescription ?: "Optional stretch action")
    }
    var selectedDays by remember(initialHabit) {
        val initDays = when {
            initialHabit == null -> DayOfWeek.values().toSet()
            initialHabit.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> {
                val prefMask = initialHabit.preferredWeekdaysMask
                if (prefMask == null || prefMask == 0 || prefMask == WeekdayMask.ALL_DAYS_MASK) {
                    emptySet()
                } else {
                    WeekdayMask.fromMask(prefMask)
                }
            }
            else -> initialHabit.scheduleDays
        }
        mutableStateOf(initDays)
    }
    var selectedColor by remember(initialHabit) {
        mutableStateOf(initialHabit?.colorHex ?: "#286143")
    }
    var reminderEnabled by remember(initialHabit) {
        mutableStateOf(initialHabit?.reminderMode == "AT_TIME")
    }
    var reminderTimeLocal by remember(initialHabit, suggestedReminderTime) {
        mutableStateOf(initialHabit?.reminderTimeLocal ?: suggestedReminderTime ?: "09:00")
    }
    var applyTodayWhenLogged by remember(alreadyLoggedToday) {
        mutableStateOf(!alreadyLoggedToday)
    }

    var nameError by remember { mutableStateOf<String?>(null) }
    var targetValueError by remember { mutableStateOf<String?>(null) }
    var lifetimeGoalError by remember { mutableStateOf<String?>(null) }
    var reminderTimeError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val colorOptions = listOf("#286143", "#3B6955", "#4A7C59", "#708D81", "#8D6B94", "#C08552")
    val categorySuggestions = listOf("Mindfulness", "Movement", "Reading", "Health", "Focus")
    val quantityUnitPresets = listOf("pages", "reps", "glasses", "km", "sessions")

    val hasMidWeekIncompatibleChange = remember(
        isEditing,
        initialHabit,
        trackingMode,
        unitCode,
        targetPeriod
    ) {
        if (!isEditing || initialHabit == null) {
            false
        } else {
            val wasWeekly = initialHabit.targetPeriod == TargetPeriod.WEEK ||
                initialHabit.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET
            val isNowWeekly = targetPeriod == TargetPeriod.WEEK ||
                scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET
            (wasWeekly || isNowWeekly) && (
                initialHabit.trackingMode.canonical() != trackingMode ||
                    (initialHabit.unitCode ?: "") != unitCode.trim() ||
                    initialHabit.targetPeriod != targetPeriod
                )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(if (isFullScreen) 18.dp else 20.dp)
            .testTag("add_edit_habit_dialog"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isFullScreen) {
                    IconButton(
                        onClick = onDismiss,
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
                }
                Text(
                    text = if (isEditing) "Edit Habit" else "New Habit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (!isFullScreen) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close dialog")
                }
            }
        }

        // SECTION 1: Identity & Category
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Identity & Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                        onClearSaveError()
                    },
                    label = { Text("Habit name *") },
                    placeholder = { Text("e.g. Read, Gentle Stretch, Hydrate") },
                    isError = nameError != null,
                    supportingText = {
                        if (nameError != null) {
                            Text(nameError!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("${name.trim().length}/80 characters")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        onClearSaveError()
                    },
                    label = { Text("Purpose or note (optional)") },
                    placeholder = { Text("e.g. For peace of mind and clarity") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_desc_input"),
                    maxLines = 2
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                        onClearSaveError()
                    },
                    label = { Text("Category (optional)") },
                    placeholder = { Text("e.g. Mindfulness, Movement, Reading") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_category_input"),
                    singleLine = true
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (cat in categorySuggestions) {
                        FilterChip(
                            selected = category.equals(cat, ignoreCase = true),
                            onClick = {
                                category = if (category.equals(cat, ignoreCase = true)) "" else cat
                                onClearSaveError()
                            },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(
                    value = lifetimeGoalText,
                    onValueChange = {
                        lifetimeGoalText = it.filter { ch -> ch.isDigit() }
                        lifetimeGoalError = null
                        onClearSaveError()
                    },
                    label = { Text("Lifetime goal active days (optional, 1–36500)") },
                    placeholder = { Text("e.g. 90 or 365") },
                    isError = lifetimeGoalError != null,
                    supportingText = {
                        if (lifetimeGoalError != null) {
                            Text(lifetimeGoalError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_lifetime_goal_input")
                )

                Text(
                    text = "Habit Color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable {
                                    selectedColor = hex
                                    onClearSaveError()
                                }
                                .then(
                                    if (isPicked) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                        )
                    }
                }
            }
        }

        // SECTION 2: Measurement Mode & Units
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "2. Measurement Mode & Units",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val creatableOptions = listOf(
                    Triple(TrackingMode.COMPLETION_ONLY, "Simple Check-Off", "Simple done check-in"),
                    Triple(TrackingMode.DURATION, "Duration", "Track minutes or hours toward a goal"),
                    Triple(TrackingMode.QUANTITY, "Quantity", "Track counts (pages, glasses, sessions) toward a goal")
                )
                val modes = if (isEditing && initialHabit?.trackingMode?.canonical() == TrackingMode.EFFORT_AWARE) {
                    listOf(
                        Triple(TrackingMode.EFFORT_AWARE, "Effort-Aware (Legacy)", "Tiny, Usual, and Extra actions count")
                    ) + creatableOptions
                } else {
                    creatableOptions
                }

                for ((m, title, sub) in modes) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                trackingMode = m
                                if (m == TrackingMode.DURATION && !DurationNormalizer.isSupportedDurationUnit(unitCode)) {
                                    unitCode = "min"
                                    if (targetValueText.toIntOrNull() == null || targetValueText == "1") {
                                        targetValueText = "15"
                                    }
                                } else if (m == TrackingMode.QUANTITY && unitCode.isBlank()) {
                                    unitCode = "pages"
                                    if (targetValueText.toIntOrNull() == null || targetValueText == "1") {
                                        targetValueText = "10"
                                    }
                                } else if (m == TrackingMode.COMPLETION_ONLY && targetPeriod == TargetPeriod.WEEK) {
                                    val currentVal = targetValueText.toIntOrNull() ?: 3
                                    if (currentVal !in 1..7) targetValueText = "3"
                                }
                                targetValueError = null
                                onClearSaveError()
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
                                } else if (m == TrackingMode.COMPLETION_ONLY && targetPeriod == TargetPeriod.WEEK) {
                                    val currentVal = targetValueText.toIntOrNull() ?: 3
                                    if (currentVal !in 1..7) targetValueText = "3"
                                }
                                targetValueError = null
                                onClearSaveError()
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (trackingMode == TrackingMode.DURATION) {
                    Text(
                        text = "Duration Unit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (u in DurationNormalizer.supportedDurationUnits) {
                            FilterChip(
                                selected = unitCode == u,
                                onClick = {
                                    unitCode = u
                                    onClearSaveError()
                                },
                                label = { Text(u) },
                                modifier = Modifier.testTag("unit_preset_$u")
                            )
                        }
                    }
                } else if (trackingMode == TrackingMode.QUANTITY) {
                    OutlinedTextField(
                        value = unitCode,
                        onValueChange = {
                            unitCode = it
                            onClearSaveError()
                        },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("habit_unit_code_input")
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (preset in quantityUnitPresets) {
                            FilterChip(
                                selected = unitCode.equals(preset, ignoreCase = true),
                                onClick = {
                                    unitCode = preset
                                    onClearSaveError()
                                },
                                label = { Text(preset) },
                                modifier = Modifier.testTag("unit_preset_$preset")
                            )
                        }
                    }
                } else if (trackingMode == TrackingMode.EFFORT_AWARE) {
                    Text(
                        text = "Customize Effort Definitions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = tinyDesc,
                        onValueChange = {
                            tinyDesc = it
                            onClearSaveError()
                        },
                        label = { Text("🌱 Tiny effort (bare minimum that counts)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tiny_desc_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = usualDesc,
                        onValueChange = {
                            usualDesc = it
                            onClearSaveError()
                        },
                        label = { Text("🌿 Usual effort (intended ordinary action)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("usual_desc_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = extraDesc,
                        onValueChange = {
                            extraDesc = it
                            onClearSaveError()
                        },
                        label = { Text("🌳 Extra effort (optional stretch)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("extra_desc_input"),
                        singleLine = true
                    )
                }
            }
        }

        // SECTION 3: Target & Period
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "3. Target & Period",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = targetPeriod == TargetPeriod.DAY,
                        onClick = {
                            targetPeriod = TargetPeriod.DAY
                            if (scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET) {
                                scheduleKind = ScheduleKind.DAILY
                                if (selectedDays.isEmpty()) {
                                    selectedDays = DayOfWeek.values().toSet()
                                }
                            }
                            targetValueError = null
                            onClearSaveError()
                        },
                        label = { Text("Per Day") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("target_period_day_chip")
                    )
                    FilterChip(
                        selected = targetPeriod == TargetPeriod.WEEK,
                        onClick = {
                            targetPeriod = TargetPeriod.WEEK
                            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
                            if (selectedDays.size == 7) {
                                selectedDays = emptySet()
                            }
                            if (trackingMode == TrackingMode.COMPLETION_ONLY) {
                                val v = targetValueText.toIntOrNull() ?: 3
                                if (v !in 1..7) targetValueText = "3"
                            }
                            targetValueError = null
                            onClearSaveError()
                        },
                        label = { Text("Per Week") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("target_period_week_chip")
                    )
                }

                val showTargetValueField = trackingMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION) ||
                    targetPeriod == TargetPeriod.WEEK

                if (showTargetValueField) {
                    val targetFieldLabel = when {
                        trackingMode == TrackingMode.COMPLETION_ONLY && targetPeriod == TargetPeriod.WEEK ->
                            "Target Days per Week (1–7) *"
                        targetPeriod == TargetPeriod.WEEK ->
                            "Weekly Target Goal (${unitCode.ifBlank { "units" }}) *"
                        else ->
                            "Target Goal (${unitCode.ifBlank { "units" }}) *"
                    }

                    OutlinedTextField(
                        value = targetValueText,
                        onValueChange = {
                            targetValueText = it.filter { ch -> ch.isDigit() }
                            targetValueError = null
                            onClearSaveError()
                        },
                        label = { Text(targetFieldLabel) },
                        isError = targetValueError != null,
                        supportingText = {
                            if (targetValueError != null) {
                                Text(
                                    text = targetValueError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("habit_target_value_error")
                                )
                            } else if (targetPeriod == TargetPeriod.WEEK) {
                                Text(
                                    text = weeklyTargetSemanticsExplanation(trackingMode, unitCode),
                                    modifier = Modifier.testTag("weekly_target_semantics_helper")
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("habit_target_value_input")
                    )
                }
            }
        }

        // SECTION 4: Schedule
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "4. Schedule",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = scheduleKind == ScheduleKind.DAILY,
                        onClick = {
                            scheduleKind = ScheduleKind.DAILY
                            targetPeriod = TargetPeriod.DAY
                            selectedDays = DayOfWeek.values().toSet()
                            onClearSaveError()
                        },
                        label = { Text("Daily") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("schedule_kind_daily_chip")
                    )
                    FilterChip(
                        selected = scheduleKind == ScheduleKind.SELECTED_WEEKDAYS,
                        onClick = {
                            scheduleKind = ScheduleKind.SELECTED_WEEKDAYS
                            targetPeriod = TargetPeriod.DAY
                            if (selectedDays.isEmpty()) {
                                selectedDays = DayOfWeek.values().toSet()
                            }
                            onClearSaveError()
                        },
                        label = { Text("Weekdays") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("schedule_kind_weekdays_chip")
                    )
                    FilterChip(
                        selected = scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
                        onClick = {
                            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
                            targetPeriod = TargetPeriod.WEEK
                            if (selectedDays.size == 7) {
                                selectedDays = emptySet()
                            }
                            if (trackingMode == TrackingMode.COMPLETION_ONLY) {
                                val v = targetValueText.toIntOrNull() ?: 3
                                if (v !in 1..7) targetValueText = "3"
                            }
                            onClearSaveError()
                        },
                        label = { Text("Flexible Week") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("schedule_kind_flexible_chip")
                    )
                }

                if (scheduleKind != ScheduleKind.DAILY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (scheduleKind == ScheduleKind.SELECTED_WEEKDAYS) {
                                "Select scheduled weekdays (unselected days are neutral rest days):"
                            } else {
                                "Preferred days (optional — leave all unselected for any day of the week):"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET && selectedDays.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    selectedDays = emptySet()
                                    onClearSaveError()
                                },
                                modifier = Modifier.testTag("clear_preferred_days_btn")
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
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
                                        if (scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET || selectedDays.size > 1) {
                                            selectedDays - d
                                        } else {
                                            selectedDays
                                        }
                                    } else {
                                        selectedDays + d
                                    }
                                    onClearSaveError()
                                },
                                label = { Text(label) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("weekday_chip_${d.name}")
                            )
                        }
                    }
                }
            }
        }

        // SECTION 5: Reminder & Effective-Date Policy (D-194 & U-12)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "5. Reminder & Effective Timing",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

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
                        onCheckedChange = {
                            reminderEnabled = it
                            if (!it) reminderTimeError = null
                            onClearSaveError()
                        },
                        modifier = Modifier.testTag("habit_reminder_switch")
                    )
                }

                if (reminderEnabled) {
                    OutlinedTextField(
                        value = reminderTimeLocal,
                        onValueChange = {
                            reminderTimeLocal = it
                            reminderTimeError = null
                            onClearSaveError()
                        },
                        label = { Text("Reminder Time (HH:mm)") },
                        isError = reminderTimeError != null,
                        supportingText = {
                            if (reminderTimeError != null) {
                                Text(
                                    text = reminderTimeError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("habit_reminder_time_error")
                                )
                            } else {
                                Text("24-hour local time, e.g. 08:30 or 19:00")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("habit_reminder_time_input")
                    )
                }

                if (hasMidWeekIncompatibleChange) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("u12_weekly_freeze_notice")
                    ) {
                        Text(
                            text = "Weekly rule protection (U-12): Your current week keeps its start-of-week unit and mode so already-logged sessions are never mixed with incompatible units. This unit/mode change applies cleanly from your next week.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (isEditing && alreadyLoggedToday) {
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
                                    onClick = {
                                        applyTodayWhenLogged = true
                                        onClearSaveError()
                                    },
                                    label = { Text("Apply today") },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("apply_today_chip")
                                )
                                FilterChip(
                                    selected = !applyTodayWhenLogged,
                                    onClick = {
                                        applyTodayWhenLogged = false
                                        onClearSaveError()
                                    },
                                    label = { Text("Apply tomorrow") },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("apply_tomorrow_chip")
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!saveErrorMessage.isNullOrBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_save_error_banner")
            ) {
                Text(
                    text = saveErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Actions: Save, Cancel, and optional Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (initialHabit != null && onDelete != null) {
                IconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("delete_habit_btn")
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
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    onClearSaveError()
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty() || trimmedName.length > 80) {
                        nameError = "Please enter a habit name (1–80 characters)."
                        return@Button
                    }

                    val parsedLifetimeGoal = if (lifetimeGoalText.isBlank()) {
                        null
                    } else {
                        val goal = lifetimeGoalText.toIntOrNull()
                        if (goal == null || goal !in 1..36500) {
                            lifetimeGoalError = "Lifetime goal must be between 1 and 36,500 days."
                            return@Button
                        }
                        goal
                    }

                    val normalizedReminderTime = if (reminderEnabled) {
                        val parsedTime = SettingsSupport.parseStrictHhMm(reminderTimeLocal)
                        if (parsedTime == null) {
                            reminderTimeError = "Enter a valid 24-hour time in HH:mm format (00:00–23:59)."
                            return@Button
                        }
                        SettingsSupport.formatQuietHours(parsedTime)
                    } else {
                        null
                    }

                    val effectiveTargetPeriod = if (scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET) {
                        TargetPeriod.WEEK
                    } else {
                        targetPeriod
                    }

                    val targetVal = if (
                        trackingMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION) ||
                        effectiveTargetPeriod == TargetPeriod.WEEK
                    ) {
                        val parsed = targetValueText.toIntOrNull()
                        if (parsed == null || parsed < 1) {
                            targetValueError = "Please enter a positive target of at least 1."
                            return@Button
                        }
                        if (effectiveTargetPeriod == TargetPeriod.WEEK &&
                            (trackingMode == TrackingMode.COMPLETION_ONLY || trackingMode == TrackingMode.EFFORT_AWARE) &&
                            parsed !in 1..7
                        ) {
                            targetValueError = "Weekly check-off target must be between 1 and 7 days per week."
                            return@Button
                        }
                        parsed
                    } else {
                        null
                    }

                    val effectiveScheduleKind = if (effectiveTargetPeriod == TargetPeriod.WEEK) {
                        ScheduleKind.FLEXIBLE_WEEKLY_TARGET
                    } else {
                        scheduleKind
                    }

                    val mask = WeekdayMask.toMask(selectedDays)
                    val preferredMask = if (
                        effectiveScheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET &&
                        selectedDays.isNotEmpty() &&
                        selectedDays.size < 7
                    ) {
                        mask
                    } else {
                        null
                    }
                    val habit = Habit(
                        habitId = initialHabit?.habitId ?: UUID.randomUUID().toString(),
                        name = trimmedName,
                        description = description.trim(),
                        category = category.trim().ifBlank { null },
                        trackingMode = trackingMode,
                        unitCode = if (trackingMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION)) {
                            unitCode.trim().ifBlank { if (trackingMode == TrackingMode.DURATION) "min" else "units" }
                        } else {
                            null
                        },
                        targetPeriod = effectiveTargetPeriod,
                        targetValue = targetVal,
                        scheduleKind = effectiveScheduleKind,
                        scheduledWeekdaysMask = if (effectiveScheduleKind == ScheduleKind.SELECTED_WEEKDAYS) mask else null,
                        preferredWeekdaysMask = preferredMask,
                        lifecycleState = initialHabit?.lifecycleState ?: HabitLifecycleState.ACTIVE,
                        lifetimeGoalDays = parsedLifetimeGoal,
                        sortOrder = initialHabit?.sortOrder ?: 0,
                        reminderMode = if (reminderEnabled) "AT_TIME" else "OFF",
                        reminderTimeLocal = normalizedReminderTime,
                        tinyDescription = tinyDesc.trim(),
                        usualDescription = usualDesc.trim(),
                        extraDescription = extraDesc.trim(),
                        scheduleDays = if (effectiveScheduleKind == ScheduleKind.DAILY) {
                            DayOfWeek.values().toSet()
                        } else {
                            selectedDays
                        },
                        colorHex = selectedColor,
                        createdAtUtcMs = initialHabit?.createdAtUtcMs ?: System.currentTimeMillis()
                    )
                    onSave(habit, applyTodayWhenLogged)
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
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

    if (showDeleteConfirmDialog && initialHabit != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete \"${initialHabit.name}\" permanently?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "This removes this habit, its configuration versions, and all logged sessions/outcomes. " +
                        "Any linked journal reflections will be kept safely on your device without the habit link. " +
                        "This action is permanent and cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(initialHabit.id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_habit_btn")
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_delete_habit_btn")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("create_edit_delete_confirm_dialog")
        )
    }
}
