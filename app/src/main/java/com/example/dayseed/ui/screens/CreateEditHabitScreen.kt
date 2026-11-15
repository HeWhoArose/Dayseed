package com.example.dayseed.ui.screens

import androidx.core.graphics.toColorInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitConfigEffectivePolicy
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.HabitMeasurementKind
import com.example.dayseed.data.HabitNaturalLanguageParser
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.UnitFormatter
import com.example.dayseed.data.WeekdayMask
import java.time.DayOfWeek
import java.util.UUID

private val HABIT_COLOR_PALETTE = listOf(
    "#286143" to "Forest Green",
    "#3E6B48" to "Moss",
    "#5A7247" to "Olive",
    "#7D6B48" to "Warm Earth",
    "#8C5A3C" to "Terracotta",
    "#4A6B7C" to "River Slate",
    "#6B5B7B" to "TwilightPlum",
    "#8A6F3E" to "Amber Ochre"
)

private val COMMON_UNIT_SUGGESTIONS = listOf(
    "pages",
    "seeds",
    "litres",
    "kilograms",
    "words",
    "steps",
    "glasses",
    "km",
    "items",
    "naira"
)

private val NATURAL_LANGUAGE_EXAMPLES = listOf(
    "Read 10 pages of a book",
    "Plant 10 seeds",
    "Drink 1.5 litres",
    "Save ₦5,000",
    "Walk for 30 minutes",
    "Meditate daily"
)

/**
 * Pure helper explaining weekly target semantics for days/week, sessions/week, and cumulative units.
 */
internal fun weeklyTargetSemanticsExplanation(
    trackingMode: TrackingMode,
    unitCode: String?
): String {
    val mode = trackingMode.canonical()
    val cleanUnit = UnitFormatter.cleanUnitCode(unitCode).ifBlank {
        if (mode == TrackingMode.DURATION) "min" else "units"
    }
    return when {
        mode == TrackingMode.COMPLETION_ONLY || mode == TrackingMode.EFFORT_AWARE ->
            "Weekly target counts completed days/week (1–7 days)."
        mode == TrackingMode.QUANTITY && cleanUnit.equals("sessions", ignoreCase = true) ->
            "Weekly target counts distinct logged sessions/week across the week."
        else ->
            "Weekly target progress accumulates $cleanUnit across all days in the week."
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateEditHabitScreen(
    viewModel: DayseedViewModel,
    habitId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val habitVersions by viewModel.habitVersions.collectAsState()
    val entries by viewModel.habitEntries.collectAsState()
    val outcomes by viewModel.habitDayOutcomes.collectAsState()
    val exceptions by viewModel.habitDateExceptions.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()

    val initialHabit = remember(habits, habitId) {
        habitId?.let { id -> habits.find { it.habitId == id } }
    }
    val isEditing = initialHabit != null

    val today = viewModel.repository.clock.todayLocal()
    val todayStr = today.toString()

    val hasActivityToday = remember(habitId, entries, outcomes, exceptions, todayStr) {
        if (habitId == null) {
            false
        } else {
            entries.any { it.habitId == habitId && it.localDate == todayStr } ||
                outcomes.any { it.habitId == habitId && it.localDate == todayStr } ||
                exceptions.any { it.habitId == habitId && it.localDate == todayStr }
        }
    }

    val activeVersion = remember(habitVersions, habitId, todayStr) {
        if (habitId == null) {
            null
        } else {
            val versionsForHabit = habitVersions.filter { it.habitId == habitId }
                .sortedBy { it.effectiveFromLocalDate }
            versionsForHabit.lastOrNull { it.effectiveFromLocalDate <= todayStr }
                ?: versionsForHabit.lastOrNull()
        }
    }

    // Primary natural-language input or habit name
    var rawHabitText by remember(initialHabit) { mutableStateOf(initialHabit?.name ?: "") }
    var interpretedActivityName by remember(initialHabit) { mutableStateOf(initialHabit?.name ?: "") }
    var userEditedActivityName by remember(initialHabit) { mutableStateOf(isEditing) }

    val initialMode = (activeVersion?.trackingMode ?: initialHabit?.trackingMode ?: TrackingMode.COMPLETION_ONLY).canonical()
    val initialRawUnitCode = activeVersion?.unitCode ?: initialHabit?.unitCode ?: ""
    val initialCleanUnitCode = UnitFormatter.cleanUnitCode(initialRawUnitCode)
    val initialMeasurementKind = remember(initialMode, initialCleanUnitCode) {
        when (initialMode) {
            TrackingMode.DURATION -> HabitMeasurementKind.DURATION
            TrackingMode.QUANTITY -> {
                if (UnitFormatter.isCurrencyUnit(initialCleanUnitCode)) {
                    HabitMeasurementKind.AMOUNT_CURRENCY
                } else {
                    HabitMeasurementKind.QUANTITY
                }
            }
            else -> HabitMeasurementKind.SIMPLE_CHECK_OFF
        }
    }

    var selectedMode by remember(initialHabit, activeVersion) {
        mutableStateOf(initialMode)
    }
    var selectedMeasurementKind by remember(initialHabit, activeVersion) {
        mutableStateOf(initialMeasurementKind)
    }
    var userOverrodeMeasurement by remember(initialHabit) { mutableStateOf(isEditing) }

    var customUnitText by remember(initialHabit, activeVersion) {
        val raw = activeVersion?.unitCode ?: initialHabit?.unitCode
        val clean = UnitFormatter.cleanUnitCode(raw)
        val defaultUnit = when (initialMode) {
            TrackingMode.DURATION -> clean.ifBlank { "min" }
            TrackingMode.QUANTITY -> clean.ifBlank { "pages" }
            else -> ""
        }
        mutableStateOf(defaultUnit)
    }
    var userOverrodeUnit by remember(initialHabit) { mutableStateOf(isEditing) }

    var targetPeriod by remember(initialHabit, activeVersion) {
        mutableStateOf(activeVersion?.targetPeriod ?: initialHabit?.targetPeriod ?: TargetPeriod.DAY)
    }
    var targetValueText by remember(initialHabit, activeVersion) {
        val storedV = activeVersion?.targetValue ?: initialHabit?.targetValue ?: 1
        val rawUnit = activeVersion?.unitCode ?: initialHabit?.unitCode
        mutableStateOf(UnitFormatter.formatStoredUnitsForInput(storedV, rawUnit))
    }
    var userOverrodeTarget by remember(initialHabit) { mutableStateOf(isEditing) }

    var scheduleKind by remember(initialHabit, activeVersion) {
        mutableStateOf(activeVersion?.scheduleKind ?: initialHabit?.scheduleKind ?: ScheduleKind.DAILY)
    }
    var weekdaysMask by remember(initialHabit, activeVersion) {
        mutableIntStateOf(
            activeVersion?.scheduledWeekdaysMask
                ?: initialHabit?.scheduledWeekdaysMask
                ?: WeekdayMask.ALL_DAYS_MASK
        )
    }
    var preferredWeekdaysMask by remember(initialHabit, activeVersion) {
        mutableIntStateOf(
            activeVersion?.preferredWeekdaysMask
                ?: initialHabit?.preferredWeekdaysMask
                ?: 0
        )
    }
    var userOverrodeSchedule by remember(initialHabit) { mutableStateOf(isEditing) }

    // Optional / Advanced fields
    var description by remember(initialHabit) { mutableStateOf(initialHabit?.description ?: "") }
    var category by remember(initialHabit) { mutableStateOf(initialHabit?.category ?: "") }
    var selectedColorHex by remember(initialHabit) {
        mutableStateOf(initialHabit?.colorHex ?: HABIT_COLOR_PALETTE.first().first)
    }
    var lifetimeGoalText by remember(initialHabit) {
        val lg = initialHabit?.lifetimeGoalDays
        mutableStateOf(lg?.toString() ?: "")
    }
    var reminderEnabled by remember(initialHabit, activeVersion) {
        val mode = activeVersion?.reminderMode ?: initialHabit?.reminderMode ?: "OFF"
        mutableStateOf(mode == "AT_TIME")
    }
    val initialReminderParts = remember(initialHabit, activeVersion) {
        val rawTime = activeVersion?.reminderTimeLocal ?: initialHabit?.reminderTimeLocal ?: "08:00"
        SettingsSupport.parseTwentyFourHourToTwelveHour(rawTime) ?: Triple(8, 0, false)
    }
    var reminderHour12Text by remember(initialReminderParts) {
        mutableStateOf(initialReminderParts.first.toString())
    }
    var reminderMinuteText by remember(initialReminderParts) {
        mutableStateOf("%02d".format(initialReminderParts.second))
    }
    var reminderIsPm by remember(initialReminderParts) {
        mutableStateOf(initialReminderParts.third)
    }

    // Progressive disclosure:
    // 1. Detailed interpretation override controls are collapsed behind "Adjust interpretation" by default.
    var showAdjustInterpretation by remember(initialHabit) {
        mutableStateOf(false)
    }

    // 2. Advanced options (Reminder, Note, Category, Colour, Lifetime Goal) are collapsed by default
    //    unless editing a habit that already uses one of these fields.
    var showAdvancedOptions by remember(initialHabit) {
        mutableStateOf(
            isEditing && (
                !initialHabit?.description.isNullOrBlank() ||
                    !initialHabit?.category.isNullOrBlank() ||
                    initialHabit?.lifetimeGoalDays != null ||
                    (activeVersion?.reminderMode ?: initialHabit?.reminderMode) == "AT_TIME"
                )
        )
    }

    var effectivePolicy by remember(hasActivityToday) {
        mutableStateOf(
            if (hasActivityToday) HabitConfigEffectivePolicy.APPLY_TOMORROW
            else HabitConfigEffectivePolicy.APPLY_TODAY
        )
    }

    var validationError by remember { mutableStateOf<String?>(null) }

    // Live deterministic natural-language interpretation
    val parsedInterpretation = remember(rawHabitText) {
        HabitNaturalLanguageParser.parse(rawHabitText)
    }

    // Apply parsed values whenever the user edits the main natural-language prompt
    fun applySmartInterpretationFromInput(newInput: String) {
        rawHabitText = newInput
        validationError = null
        val parsed = HabitNaturalLanguageParser.parse(newInput)
        if (!userEditedActivityName) {
            interpretedActivityName = parsed.activityName
        }
        if (!userOverrodeMeasurement) {
            selectedMeasurementKind = parsed.measurementKind
            selectedMode = parsed.trackingMode
        }
        if (!userOverrodeUnit) {
            customUnitText = when (parsed.measurementKind) {
                HabitMeasurementKind.DURATION -> parsed.unitCode ?: "min"
                HabitMeasurementKind.AMOUNT_CURRENCY -> parsed.unitCode ?: "naira"
                HabitMeasurementKind.QUANTITY -> parsed.unitCode ?: "items"
                HabitMeasurementKind.SIMPLE_CHECK_OFF -> ""
            }
        }
        if (!userOverrodeTarget) {
            targetValueText = (parsed.targetAmountText ?: parsed.targetValue?.toString() ?: "1")
        }
        if (!userOverrodeSchedule) {
            scheduleKind = parsed.scheduleKind
            targetPeriod = parsed.targetPeriod
            weekdaysMask = parsed.scheduledWeekdaysMask
        }
    }

    val effectiveActivityTitle = interpretedActivityName.trim().ifBlank {
        parsedInterpretation.activityName.ifBlank { rawHabitText.trim() }
    }

    val parsedTargetTriple = remember(targetValueText, customUnitText, selectedMeasurementKind) {
        val fallbackCleanUnit = when (selectedMeasurementKind) {
            HabitMeasurementKind.DURATION -> "min"
            HabitMeasurementKind.AMOUNT_CURRENCY -> customUnitText.trim().ifBlank { "naira" }
            HabitMeasurementKind.QUANTITY -> customUnitText.trim().ifBlank { "items" }
            HabitMeasurementKind.SIMPLE_CHECK_OFF -> ""
        }
        UnitFormatter.parseTargetInputToStoredUnitsAndUnit(
            targetInput = targetValueText,
            cleanUnit = fallbackCleanUnit,
            isCurrency = selectedMeasurementKind == HabitMeasurementKind.AMOUNT_CURRENCY,
            isDuration = selectedMeasurementKind == HabitMeasurementKind.DURATION
        )
    }

    val effectiveTargetSummary = remember(
        selectedMode,
        selectedMeasurementKind,
        parsedTargetTriple,
        customUnitText,
        targetPeriod
    ) {
        when (selectedMode.canonical()) {
            TrackingMode.COMPLETION_ONLY -> {
                if (targetPeriod == TargetPeriod.WEEK) {
                    val v = (parsedTargetTriple?.first ?: 3).coerceIn(1, 7)
                    "$v ${if (v == 1) "day" else "days"} per week"
                } else {
                    "Complete once"
                }
            }
            TrackingMode.DURATION -> {
                val v = parsedTargetTriple?.first ?: 15
                UnitFormatter.formatValueWithUnit(v, "min")
            }
            TrackingMode.QUANTITY -> {
                val v = parsedTargetTriple?.first ?: 1
                val encodedUnit = parsedTargetTriple?.second
                    ?: customUnitText.trim().ifBlank {
                        if (selectedMeasurementKind == HabitMeasurementKind.AMOUNT_CURRENCY) "naira" else "items"
                    }
                UnitFormatter.formatValueWithUnit(v, encodedUnit)
            }
            else -> "Effort check-in"
        }
    }

    val effectiveScheduleSummary = remember(
        scheduleKind,
        targetPeriod,
        weekdaysMask,
        parsedTargetTriple,
        parsedInterpretation.isDefaultSchedule,
        userOverrodeSchedule
    ) {
        val baseSchedule = when {
            scheduleKind == ScheduleKind.SELECTED_WEEKDAYS -> {
                val days = WeekdayMask.fromMask(weekdaysMask)
                if (days.size == 7) {
                    "Daily"
                } else {
                    DayOfWeek.entries.filter { it in days }.joinToString(", ") {
                        it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }
                    }
                }
            }
            targetPeriod == TargetPeriod.WEEK -> "Weekly target"
            else -> "Daily"
        }
        if (baseSchedule == "Daily" && parsedInterpretation.isDefaultSchedule && !userOverrodeSchedule) {
            "Daily (default)"
        } else {
            baseSchedule
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("create_edit_habit_screen"),
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
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEditing) "Edit Habit" else "New Habit",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (validationError != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("habit_form_error_banner")
                    ) {
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // STEP 1: Natural-Language Habit Prompt
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "What would you like to do?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Describe your habit naturally — include an amount, duration, or leave it simple.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = rawHabitText,
                            onValueChange = { applySmartInterpretationFromInput(it) },
                            label = { Text("Your habit") },
                            placeholder = { Text("e.g. Read 10 pages, Drink 1.5 litres, Save ₦5,000") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnFocus()
                                .testTag("habit_name_input")
                        )

                        if (!isEditing && rawHabitText.isBlank()) {
                            Text(
                                text = "Try an example:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NATURAL_LANGUAGE_EXAMPLES.forEachIndexed { idx, example ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { applySmartInterpretationFromInput(example) },
                                        label = { Text(example) },
                                        modifier = Modifier.testTag("habit_example_chip_$idx")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // STEP 2: Concise Interpretation Summary Card & "Adjust interpretation" Progressive Disclosure
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_habit_interpretation_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Here's what we understood:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = selectedMeasurementKind.displayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("smart_interpreted_measurement_badge")
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                InterpretationSummaryRow(
                                    label = "Activity",
                                    value = effectiveActivityTitle.ifBlank { "—" },
                                    testTag = "smart_interpreted_activity_value"
                                )
                                InterpretationSummaryRow(
                                    label = "Target",
                                    value = effectiveTargetSummary,
                                    testTag = "smart_interpreted_target_value"
                                )
                                InterpretationSummaryRow(
                                    label = "Schedule",
                                    value = effectiveScheduleSummary,
                                    testTag = "smart_interpreted_schedule_value"
                                )
                            }
                        }

                        if (parsedInterpretation.needsUserConfirmation && parsedInterpretation.ambiguityReason != null) {
                            Text(
                                text = "Note: ${parsedInterpretation.ambiguityReason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("smart_habit_ambiguity_notice")
                            )
                        }

                        // Compact "Adjust interpretation" toggle
                        TextButton(
                            onClick = { showAdjustInterpretation = !showAdjustInterpretation },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("smart_adjust_interpretation_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showAdjustInterpretation) "Hide adjustments" else "Adjust interpretation",
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (showAdjustInterpretation) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Detailed editing controls revealed ONLY when user taps "Adjust interpretation"
                        if (showAdjustInterpretation) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )

                            OutlinedTextField(
                                value = interpretedActivityName,
                                onValueChange = {
                                    interpretedActivityName = it
                                    userEditedActivityName = true
                                },
                                label = { Text("Activity display name") },
                                placeholder = { Text("e.g. Read a book, Drink water, Save") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewOnFocus()
                                    .testTag("smart_interpreted_activity_input")
                            )

                            Text(
                                text = "How would you like to measure it?",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val measurementOptions = listOf(
                                Triple(
                                    HabitMeasurementKind.SIMPLE_CHECK_OFF,
                                    "Simple check-off",
                                    "Mark done once per day (e.g. Meditate)"
                                ),
                                Triple(
                                    HabitMeasurementKind.DURATION,
                                    "Duration",
                                    "Minutes spent per day or week (e.g. Walk 30 min)"
                                ),
                                Triple(
                                    HabitMeasurementKind.QUANTITY,
                                    "Quantity (Custom or Fractional Unit)",
                                    "Any whole or decimal count: pages, seeds, litres, kg..."
                                ),
                                Triple(
                                    HabitMeasurementKind.AMOUNT_CURRENCY,
                                    "Amount / Currency",
                                    "Track savings or amounts (e.g. ₦5,000, ₦1,250.50, $50)"
                                )
                            )

                            measurementOptions.forEach { (kind, label, subtitle) ->
                                val tag = when (kind) {
                                    HabitMeasurementKind.SIMPLE_CHECK_OFF -> "mode_option_completion_only"
                                    HabitMeasurementKind.DURATION -> "mode_option_duration"
                                    HabitMeasurementKind.QUANTITY -> "mode_option_quantity"
                                    HabitMeasurementKind.AMOUNT_CURRENCY -> "mode_option_amount_currency"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            userOverrodeMeasurement = true
                                            selectedMeasurementKind = kind
                                            selectedMode = when (kind) {
                                                HabitMeasurementKind.SIMPLE_CHECK_OFF -> TrackingMode.COMPLETION_ONLY
                                                HabitMeasurementKind.DURATION -> TrackingMode.DURATION
                                                HabitMeasurementKind.QUANTITY,
                                                HabitMeasurementKind.AMOUNT_CURRENCY -> TrackingMode.QUANTITY
                                            }
                                            if (kind == HabitMeasurementKind.DURATION) {
                                                customUnitText = "min"
                                            } else if (kind == HabitMeasurementKind.AMOUNT_CURRENCY &&
                                                !UnitFormatter.isCurrencyUnit(customUnitText)
                                            ) {
                                                customUnitText = "naira"
                                            } else if (kind == HabitMeasurementKind.QUANTITY &&
                                                (customUnitText.isBlank() || customUnitText == "min")
                                            ) {
                                                customUnitText = "items"
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                        .testTag(tag),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedMeasurementKind == kind &&
                                            selectedMode.canonical() != TrackingMode.EFFORT_AWARE,
                                        onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (isEditing && initialMode == TrackingMode.EFFORT_AWARE) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            userOverrodeMeasurement = true
                                            selectedMode = TrackingMode.EFFORT_AWARE
                                        }
                                        .padding(vertical = 4.dp)
                                        .testTag("mode_option_effort_aware"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedMode.canonical() == TrackingMode.EFFORT_AWARE,
                                        onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Effort-Aware (Legacy)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Tiny Sprout, Usual Leaf, or Extra Branch",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (selectedMode.canonical() == TrackingMode.QUANTITY) {
                                OutlinedTextField(
                                    value = customUnitText,
                                    onValueChange = {
                                        customUnitText = it
                                        userOverrodeUnit = true
                                    },
                                    label = {
                                        Text(
                                            if (selectedMeasurementKind == HabitMeasurementKind.AMOUNT_CURRENCY) {
                                                "Currency or unit (e.g. naira, dollars, euros)"
                                            } else {
                                                "Unit (type any unit: pages, litres, kg, handmade bags...)"
                                            }
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bringIntoViewOnFocus()
                                        .testTag("habit_unit_code_input")
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    COMMON_UNIT_SUGGESTIONS.forEach { preset ->
                                        FilterChip(
                                            selected = customUnitText.equals(preset, ignoreCase = true),
                                            onClick = {
                                                customUnitText = preset
                                                userOverrodeUnit = true
                                                if (UnitFormatter.isCurrencyUnit(preset)) {
                                                    selectedMeasurementKind = HabitMeasurementKind.AMOUNT_CURRENCY
                                                } else {
                                                    selectedMeasurementKind = HabitMeasurementKind.QUANTITY
                                                }
                                            },
                                            label = { Text(preset) },
                                            modifier = Modifier.testTag("unit_preset_$preset")
                                        )
                                    }
                                }
                            }

                            val needsNumericTarget = selectedMode.canonical() == TrackingMode.QUANTITY ||
                                selectedMode.canonical() == TrackingMode.DURATION ||
                                targetPeriod == TargetPeriod.WEEK
                            if (needsNumericTarget) {
                                OutlinedTextField(
                                    value = targetValueText,
                                    onValueChange = { raw ->
                                        targetValueText = raw.filter { ch -> ch.isDigit() || ch == '.' }
                                        userOverrodeTarget = true
                                    },
                                    label = {
                                        val unitDesc = when (selectedMode.canonical()) {
                                            TrackingMode.DURATION -> "minutes"
                                            TrackingMode.QUANTITY -> customUnitText.ifBlank { "units" }
                                            else -> "days per week (1–7)"
                                        }
                                        val periodDesc = if (targetPeriod == TargetPeriod.WEEK) "per week" else "per day"
                                        Text("Target goal ($unitDesc $periodDesc)")
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bringIntoViewOnFocus()
                                        .testTag("habit_target_value_input")
                                )
                            }
                        }
                    }
                }
            }

            // STEP 3: Concise Schedule Selector
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = scheduleKind == ScheduleKind.DAILY && targetPeriod == TargetPeriod.DAY,
                                onClick = {
                                    userOverrodeSchedule = true
                                    scheduleKind = ScheduleKind.DAILY
                                    targetPeriod = TargetPeriod.DAY
                                    weekdaysMask = WeekdayMask.ALL_DAYS_MASK
                                },
                                label = { Text("Every day") },
                                modifier = Modifier.testTag("schedule_kind_daily")
                            )
                            FilterChip(
                                selected = scheduleKind == ScheduleKind.SELECTED_WEEKDAYS && targetPeriod == TargetPeriod.DAY,
                                onClick = {
                                    userOverrodeSchedule = true
                                    scheduleKind = ScheduleKind.SELECTED_WEEKDAYS
                                    targetPeriod = TargetPeriod.DAY
                                    if (weekdaysMask <= 0) {
                                        weekdaysMask = WeekdayMask.ALL_DAYS_MASK
                                    }
                                },
                                label = { Text("Selected days") },
                                modifier = Modifier.testTag("schedule_kind_selected_weekdays")
                            )
                            FilterChip(
                                selected = targetPeriod == TargetPeriod.WEEK,
                                onClick = {
                                    userOverrodeSchedule = true
                                    targetPeriod = TargetPeriod.WEEK
                                    if (scheduleKind == ScheduleKind.DAILY) {
                                        scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
                                    }
                                    if (selectedMode.canonical() == TrackingMode.COMPLETION_ONLY) {
                                        val cur = targetValueText.toIntOrNull() ?: 3
                                        if (cur !in 1..7) targetValueText = "3"
                                    }
                                },
                                label = { Text("Weekly target") },
                                modifier = Modifier.testTag("target_period_week")
                            )
                        }

                        // Hidden compatibility chip tag for tests targeting target_period_day directly
                        if (targetPeriod == TargetPeriod.WEEK) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        userOverrodeSchedule = true
                                        targetPeriod = TargetPeriod.DAY
                                        if (scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET) {
                                            scheduleKind = ScheduleKind.DAILY
                                        }
                                    },
                                    label = { Text("Switch back to Daily Target") },
                                    modifier = Modifier.testTag("target_period_day")
                                )
                                FilterChip(
                                    selected = scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
                                    onClick = {
                                        userOverrodeSchedule = true
                                        scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
                                    },
                                    label = { Text("Flexible weekly") },
                                    modifier = Modifier.testTag("schedule_kind_flexible_weekly")
                                )
                            }
                        }

                        if (scheduleKind == ScheduleKind.SELECTED_WEEKDAYS) {
                            Text(
                                text = "Scheduled weekdays:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DayOfWeek.entries.forEach { day ->
                                    val isSelected = WeekdayMask.contains(weekdaysMask, day)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            userOverrodeSchedule = true
                                            val currentSet = WeekdayMask.fromMask(weekdaysMask).toMutableSet()
                                            if (isSelected && currentSet.size > 1) {
                                                currentSet.remove(day)
                                            } else {
                                                currentSet.add(day)
                                            }
                                            weekdaysMask = WeekdayMask.toMask(currentSet)
                                        },
                                        label = {
                                            Text(day.name.take(3).lowercase().replaceFirstChar { it.uppercase() })
                                        },
                                        modifier = Modifier.testTag("weekday_chip_${day.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        if (scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET) {
                            Text(
                                text = "Preferred days (optional):",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DayOfWeek.entries.forEach { day ->
                                    val isSelected = WeekdayMask.contains(preferredWeekdaysMask, day)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val currentSet = WeekdayMask.fromMask(preferredWeekdaysMask).toMutableSet()
                                            if (isSelected) currentSet.remove(day) else currentSet.add(day)
                                            preferredWeekdaysMask = WeekdayMask.toMask(currentSet)
                                        },
                                        label = {
                                            Text(day.name.take(3).lowercase().replaceFirstChar { it.uppercase() })
                                        },
                                        modifier = Modifier.testTag("pref_weekday_chip_${day.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // STEP 4: Progressive Disclosure — Collapsed "More Options" (Reminder, Note, Category, Colour, Lifetime Goal)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showAdvancedOptions = !showAdvancedOptions }
                                .padding(vertical = 4.dp)
                                .testTag("habit_advanced_options_toggle"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "More Options",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Gentle reminder, note, category, colour, and optional lifetime goal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showAdvancedOptions) "Collapse more options" else "Expand more options"
                            )
                        }

                        if (showAdvancedOptions) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )

                            // Gentle Reminder inside More Options
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Gentle Reminder",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Optional daily notification at your chosen time.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = reminderEnabled,
                                    onCheckedChange = { reminderEnabled = it },
                                    modifier = Modifier.testTag("habit_reminder_toggle")
                                )
                            }

                            if (reminderEnabled) {
                                val resolved24h = SettingsSupport.formatTwelveHourToTwentyFourHour(
                                    hour12 = reminderHour12Text.toIntOrNull() ?: 8,
                                    minute = reminderMinuteText.toIntOrNull() ?: 0,
                                    isPm = reminderIsPm
                                ) ?: "08:00"
                                val unambiguousPreview = SettingsSupport.formatUnambiguousTimeDisplay(resolved24h, is24HourPreference = false)
                                val quietHoursEnabled = appSettings["quiet_hours_enabled"]?.valueText != "false"
                                val quietHoursStart = appSettings["quiet_hours_start"]?.valueText ?: "22:00"
                                val quietHoursEnd = appSettings["quiet_hours_end"]?.valueText ?: "07:00"
                                val fallsInQuietHours = quietHoursEnabled &&
                                    SettingsSupport.isTimeWithinQuietHours(resolved24h, quietHoursStart, quietHoursEnd)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = reminderHour12Text,
                                        onValueChange = { raw ->
                                            reminderHour12Text = raw.filter { it.isDigit() }.take(2)
                                        },
                                        label = { Text("Hour (1–12)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .bringIntoViewOnFocus()
                                            .testTag("habit_reminder_hour12_input")
                                    )
                                    Text(
                                        text = ":",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    OutlinedTextField(
                                        value = reminderMinuteText,
                                        onValueChange = { raw ->
                                            reminderMinuteText = raw.filter { it.isDigit() }.take(2)
                                        },
                                        label = { Text("Min (00–59)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .bringIntoViewOnFocus()
                                            .testTag("habit_reminder_minute_input")
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FilterChip(
                                            selected = !reminderIsPm,
                                            onClick = { reminderIsPm = false },
                                            label = { Text("AM") },
                                            modifier = Modifier.testTag("habit_reminder_am_chip")
                                        )
                                        FilterChip(
                                            selected = reminderIsPm,
                                            onClick = { reminderIsPm = true },
                                            label = { Text("PM") },
                                            modifier = Modifier.testTag("habit_reminder_pm_chip")
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("habit_reminder_resolved_time_preview")
                                ) {
                                    Text(
                                        text = "Scheduled time: $unambiguousPreview",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                if (fallsInQuietHours) {
                                    val qStartDisp = SettingsSupport.formatUnambiguousTimeDisplay(quietHoursStart, false)
                                    val qEndDisp = SettingsSupport.formatUnambiguousTimeDisplay(quietHoursEnd, false)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("habit_reminder_quiet_hours_warning")
                                    ) {
                                        Text(
                                            text = "Note: $unambiguousPreview falls within your Quiet Hours ($qStartDisp – $qEndDisp). Reminders are silenced during Quiet Hours.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Description or gentle note (optional)") },
                                placeholder = { Text("Why does this habit matter to you?") },
                                maxLines = 2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewOnFocus()
                                    .testTag("habit_desc_input")
                            )

                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = { Text("Category (optional, e.g. Mindfulness, Health)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewOnFocus()
                                    .testTag("habit_category_input")
                            )

                            Text(
                                text = "Accent Colour",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HABIT_COLOR_PALETTE.forEach { (hex, label) ->
                                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                                    val swatchColor = try {
                                        Color(hex.toColorInt())
                                    } catch (_: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(swatchColor)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                                else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColorHex = hex }
                                            .semantics { contentDescription = "Select colour $label" }
                                            .testTag("color_swatch_${hex.removePrefix("#")}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = lifetimeGoalText,
                                onValueChange = { raw ->
                                    lifetimeGoalText = raw.filter { it.isDigit() }
                                },
                                label = { Text("Lifetime active days goal (optional, e.g. 66)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewOnFocus()
                                    .testTag("habit_lifetime_goal_input")
                            )
                        }
                    }
                }
            }

            // STEP 5: Effective Date Policy (when editing an existing habit)
            if (isEditing) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("habit_effective_policy_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "When should rule changes take effect?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (hasActivityToday) {
                                Text(
                                    text = "Activity is already logged today ($todayStr), so rule changes apply starting tomorrow to protect today's record (D-194).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.testTag("habit_locked_today_notice")
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = effectivePolicy == HabitConfigEffectivePolicy.APPLY_TODAY,
                                    onClick = {
                                        if (!hasActivityToday) {
                                            effectivePolicy = HabitConfigEffectivePolicy.APPLY_TODAY
                                        }
                                    },
                                    enabled = !hasActivityToday,
                                    label = { Text("Apply Today") },
                                    modifier = Modifier.testTag("policy_apply_today")
                                )
                                FilterChip(
                                    selected = effectivePolicy == HabitConfigEffectivePolicy.APPLY_TOMORROW,
                                    onClick = {
                                        effectivePolicy = HabitConfigEffectivePolicy.APPLY_TOMORROW
                                    },
                                    label = { Text("Apply Tomorrow") },
                                    modifier = Modifier.testTag("policy_apply_tomorrow")
                                )
                                if (targetPeriod == TargetPeriod.WEEK) {
                                    FilterChip(
                                        selected = effectivePolicy == HabitConfigEffectivePolicy.DEFER_TO_NEXT_WEEK_BOUNDARY,
                                        onClick = {
                                            effectivePolicy = HabitConfigEffectivePolicy.DEFER_TO_NEXT_WEEK_BOUNDARY
                                        },
                                        label = { Text("Next Week Boundary") },
                                        modifier = Modifier.testTag("policy_defer_next_week")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // STEP 6: Save & Cancel Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("cancel_habit_btn")
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val cleanName = effectiveActivityTitle.trim()
                            if (cleanName.isEmpty()) {
                                validationError = "Please describe what you would like to do."
                                return@Button
                            }

                            val finalTargetValue: Int
                            val finalUnitCode: String?

                            when (selectedMode.canonical()) {
                                TrackingMode.QUANTITY, TrackingMode.DURATION -> {
                                    if (parsedTargetTriple == null || parsedTargetTriple.first <= 0) {
                                        showAdjustInterpretation = true
                                        // Distinguish "too large to store" from "not a positive number" so an
                                        // out-of-range target is rejected with a clear message instead of
                                        // being silently shrunk to a different goal.
                                        validationError = UnitFormatter.describeTargetInputProblem(
                                            targetInput = targetValueText,
                                            isCurrency = selectedMeasurementKind == HabitMeasurementKind.AMOUNT_CURRENCY,
                                            isDuration = selectedMeasurementKind == HabitMeasurementKind.DURATION
                                        ) ?: "Target must be a positive number."
                                        return@Button
                                    }
                                    finalTargetValue = parsedTargetTriple.first
                                    finalUnitCode = parsedTargetTriple.second
                                }
                                TrackingMode.COMPLETION_ONLY -> {
                                    if (targetPeriod == TargetPeriod.WEEK) {
                                        val parsedTarget = targetValueText.trim().toBigDecimalOrNull()?.toInt()
                                        if (parsedTarget == null || parsedTarget <= 0) {
                                            showAdjustInterpretation = true
                                            validationError = "Weekly target days must be between 1 and 7."
                                            return@Button
                                        }
                                        finalTargetValue = parsedTarget.coerceIn(1, 7)
                                    } else {
                                        finalTargetValue = 1
                                    }
                                    finalUnitCode = null
                                }
                                else -> {
                                    finalTargetValue = (targetValueText.trim().toIntOrNull() ?: 1).coerceAtLeast(1)
                                    finalUnitCode = null
                                }
                            }

                            val parsedLifetime = lifetimeGoalText.trim().let {
                                if (it.isEmpty()) null else it.toIntOrNull()
                            }
                            if (lifetimeGoalText.isNotBlank() && (parsedLifetime == null || parsedLifetime <= 0)) {
                                showAdvancedOptions = true
                                validationError = "Lifetime goal days must be a positive whole number."
                                return@Button
                            }

                            val resolvedReminderTime = if (reminderEnabled) {
                                val h12 = reminderHour12Text.trim().toIntOrNull()
                                val min = reminderMinuteText.trim().toIntOrNull()
                                val formatted24 = if (h12 != null && min != null) {
                                    SettingsSupport.formatTwelveHourToTwentyFourHour(h12, min, reminderIsPm)
                                } else {
                                    null
                                }
                                if (formatted24 == null) {
                                    showAdvancedOptions = true
                                    validationError = "Please enter a valid reminder time (Hour 1–12, Minute 00–59, and AM or PM)."
                                    return@Button
                                }
                                formatted24
                            } else {
                                null
                            }

                            val newHabit = Habit(
                                habitId = initialHabit?.habitId ?: "habit_${UUID.randomUUID()}",
                                name = cleanName,
                                description = description.trim(),
                                category = category.trim().ifBlank { null },
                                trackingMode = selectedMode.canonical(),
                                unitCode = finalUnitCode,
                                targetPeriod = targetPeriod,
                                targetValue = finalTargetValue,
                                scheduleKind = scheduleKind,
                                scheduledWeekdaysMask = if (scheduleKind == ScheduleKind.SELECTED_WEEKDAYS) weekdaysMask else null,
                                preferredWeekdaysMask = if (scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET && preferredWeekdaysMask > 0) preferredWeekdaysMask else null,
                                reminderMode = if (reminderEnabled) "AT_TIME" else "OFF",
                                reminderTimeLocal = resolvedReminderTime,
                                lifetimeGoalDays = parsedLifetime,
                                lifecycleState = initialHabit?.lifecycleState ?: HabitLifecycleState.ACTIVE,
                                sortOrder = initialHabit?.sortOrder ?: habits.size,
                                colorHex = selectedColorHex,
                                createdAtUtcMs = initialHabit?.createdAtUtcMs ?: viewModel.repository.clock.nowUtcMs(),
                                updatedAtUtcMs = viewModel.repository.clock.nowUtcMs()
                            )

                            viewModel.saveHabit(
                                habit = newHabit,
                                policy = if (isEditing) effectivePolicy else HabitConfigEffectivePolicy.APPLY_TODAY
                            )
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("save_habit_btn")
                    ) {
                        Text(if (isEditing) "Save Changes" else "Create Habit")
                    }
                }
            }
        }
    }
}

@Composable
private fun InterpretationSummaryRow(
    label: String,
    value: String,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(testTag)
        )
    }
}
