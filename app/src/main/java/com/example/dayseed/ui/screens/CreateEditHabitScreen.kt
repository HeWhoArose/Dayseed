package com.example.dayseed.ui.screens

import androidx.compose.ui.semantics.Role
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitConfigEffectivePolicy
import com.example.dayseed.data.HabitDurationKind
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.HabitLifetimeCalculator
import com.example.dayseed.data.HabitMeasurementKind
import com.example.dayseed.data.HabitNaturalLanguageParser
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.UnitFormatter
import com.example.dayseed.data.WeekdayMask
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
    "10 push-ups daily",
    "Read 20 pages every evening",
    "Drink 1.5 litres",
    "Walk for 30 minutes",
    "Save ₦5,000",
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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

    // Optional habit duration / end-date configuration
    val habitStartDate = remember(initialHabit, activeVersion, today) {
        val rawStart = initialHabit?.startLocalDate ?: activeVersion?.effectiveFromLocalDate
        rawStart?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
    }
    var durationKind by remember(initialHabit) {
        mutableStateOf(initialHabit?.durationKind ?: HabitDurationKind.NONE)
    }
    var durationValueText by remember(initialHabit) {
        val defaultVal = initialHabit?.durationValue ?: 30
        mutableStateOf(defaultVal.toString())
    }
    var specificEndDateText by remember(initialHabit) {
        mutableStateOf(initialHabit?.scheduledEndLocalDate ?: "")
    }
    var showEndDatePickerDialog by remember { mutableStateOf(false) }

    val durationCalculationResult = remember(
        habitStartDate,
        durationKind,
        durationValueText,
        specificEndDateText
    ) {
        val parsedVal = durationValueText.trim().toIntOrNull()
        HabitLifetimeCalculator.calculateEndDateResult(
            startDate = habitStartDate,
            durationKind = durationKind,
            durationValue = parsedVal,
            specificEndDateIso = specificEndDateText.trim().ifBlank { null }
        )
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    var scheduleExpanded by rememberSaveable { mutableStateOf(false) }
    var durationExpanded by rememberSaveable { mutableStateOf(false) }

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
            interpretedActivityName = parsed.displayName
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
        parsedInterpretation.displayName.ifBlank {
            parsedInterpretation.activityName.ifBlank { rawHabitText.trim() }
        }
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
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // STEP 1: Natural-Language Habit Prompt
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            placeholder = { Text("e.g. Read 20 pages daily", maxLines = 1) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnFocus()
                                .semantics { contentDescription = "Your habit" }
                                .testTag("habit_name_input")
                        )

                        if (rawHabitText.isNotBlank()) {
                            val liveParts = buildList {
                                if (effectiveActivityTitle.isNotBlank()) add(effectiveActivityTitle)
                                if (selectedMode.canonical() == TrackingMode.COMPLETION_ONLY) add("check off")
                                add(effectiveScheduleSummary.replace(" (default)", "").let { if (it == "Daily") "every day" else it.lowercase() })
                            }
                            Text(
                                text = "Understood: " + liveParts.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                                    .testTag("habit_live_understanding")
                            )
                        }
                    }
                }
            }

            // Legacy EFFORT_AWARE conversion guidance (D-233). Dayseed no longer asks for manual
            // Tiny/Usual/Extra check-ins; the banner explains how the habit maps onto an
            // evidence-derived goal without discarding its history.
            if (isEditing && initialMode == TrackingMode.EFFORT_AWARE) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("legacy_effort_conversion_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "This habit uses the older Tiny / Usual / Extra check-in",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Dayseed now derives progress from what you record. Pick a Quantity, Duration or Simple check-off goal below to keep this habit's history and carry on without manual effort chips.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // STEP 3: Concise Schedule Selector
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FormSectionHeader(
                            title = "Schedule",
                            summary = effectiveScheduleSummary.replace(" (default)", "").let { if (it == "Daily") "Every day" else it },
                            expanded = scheduleExpanded,
                            onToggle = { scheduleExpanded = !scheduleExpanded },
                            tag = "habit_schedule_header"
                        )
                        if (scheduleExpanded) {

                        SegmentedChoice(
                            labels = listOf("Every day", "Selected days", "Weekly target"),
                            tags = listOf("schedule_kind_daily", "schedule_kind_selected_weekdays", "target_period_week"),
                            selected = when {
                                targetPeriod == TargetPeriod.WEEK -> 2
                                scheduleKind == ScheduleKind.SELECTED_WEEKDAYS -> 1
                                else -> 0
                            },
                            onSelect = { index ->
                                userOverrodeSchedule = true
                                when (index) {
                                    0 -> {
                                        scheduleKind = ScheduleKind.DAILY
                                        targetPeriod = TargetPeriod.DAY
                                        weekdaysMask = WeekdayMask.ALL_DAYS_MASK
                                    }
                                    1 -> {
                                        scheduleKind = ScheduleKind.SELECTED_WEEKDAYS
                                        targetPeriod = TargetPeriod.DAY
                                        if (weekdaysMask <= 0) weekdaysMask = WeekdayMask.ALL_DAYS_MASK
                                    }
                                    else -> {
                                        targetPeriod = TargetPeriod.WEEK
                                        if (scheduleKind == ScheduleKind.DAILY) scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
                                        if (selectedMode.canonical() == TrackingMode.COMPLETION_ONLY) {
                                            val cur = targetValueText.toIntOrNull() ?: 3
                                            if (cur !in 1..7) targetValueText = "3"
                                        }
                                    }
                                }
                            }
                        )

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
            }

            // STEP 4B: Optional Duration & End Date ("How long do you want to maintain this habit?")
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_duration_section")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FormSectionHeader(
                            title = "How long?",
                            summary = when (durationKind) {
                                HabitDurationKind.NONE -> "No end date"
                                HabitDurationKind.SPECIFIC_DATE -> if (specificEndDateText.isBlank()) "Until a date" else "Until ${specificEndDateText.trim()}"
                                else -> "For ${durationValueText.ifBlank { "…" }} ${durationKind.name.lowercase()}"
                            },
                            expanded = durationExpanded,
                            onToggle = { durationExpanded = !durationExpanded },
                            tag = "habit_duration_header"
                        )
                        if (durationExpanded) {

                        val timeUnits = listOf(HabitDurationKind.DAYS, HabitDurationKind.WEEKS, HabitDurationKind.MONTHS, HabitDurationKind.YEARS)
                        fun chooseTimeUnit(kind: HabitDurationKind) {
                            durationKind = kind
                            validationError = null
                            if (durationValueText.toIntOrNull() == null || durationValueText.toInt() <= 0) {
                                durationValueText = when (kind) {
                                    HabitDurationKind.DAYS -> "30"
                                    HabitDurationKind.WEEKS -> "4"
                                    HabitDurationKind.MONTHS -> "3"
                                    else -> "1"
                                }
                            }
                        }
                        SegmentedChoice(
                            labels = listOf("No end date", "A set time", "Until a date"),
                            tags = listOf("habit_duration_mode_NONE", "habit_duration_mode_SET", "habit_duration_mode_DATE"),
                            selected = when (durationKind) {
                                HabitDurationKind.NONE -> 0
                                HabitDurationKind.SPECIFIC_DATE -> 2
                                else -> 1
                            },
                            onSelect = { index ->
                                validationError = null
                                when (index) {
                                    0 -> durationKind = HabitDurationKind.NONE
                                    1 -> if (durationKind !in timeUnits) chooseTimeUnit(HabitDurationKind.DAYS)
                                    else -> {
                                        durationKind = HabitDurationKind.SPECIFIC_DATE
                                        if (specificEndDateText.isBlank()) {
                                            specificEndDateText = habitStartDate.plusDays(30).toString()
                                        }
                                    }
                                }
                            }
                        )

                        when (durationKind) {
                            HabitDurationKind.NONE -> Unit
                            HabitDurationKind.DAYS,
                            HabitDurationKind.WEEKS,
                            HabitDurationKind.MONTHS,
                            HabitDurationKind.YEARS -> {
                                SegmentedChoice(
                                    labels = listOf("Days", "Weeks", "Months", "Years"),
                                    tags = timeUnits.map { "habit_duration_kind_${it.name}" },
                                    selected = timeUnits.indexOf(durationKind),
                                    onSelect = { chooseTimeUnit(timeUnits[it]) }
                                )
                                val unitWord = when (durationKind) {
                                    HabitDurationKind.DAYS -> "days"
                                    HabitDurationKind.WEEKS -> "weeks"
                                    HabitDurationKind.MONTHS -> "months"
                                    else -> "years"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val current = durationValueText.toIntOrNull() ?: 0
                                    IconButton(
                                        onClick = { durationValueText = (current - 1).coerceAtLeast(1).toString(); validationError = null },
                                        modifier = Modifier.size(48.dp).testTag("habit_duration_minus_btn")
                                    ) { Icon(Icons.Default.Remove, contentDescription = "One $unitWord less") }
                                    OutlinedTextField(
                                        value = durationValueText,
                                        onValueChange = { raw ->
                                            durationValueText = raw.filter { it.isDigit() }.take(4)
                                            validationError = null
                                        },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .width(96.dp)
                                            .bringIntoViewOnFocus()
                                            .semantics { contentDescription = "Number of $unitWord" }
                                            .testTag("habit_duration_value_input")
                                    )
                                    IconButton(
                                        onClick = { durationValueText = (current + 1).coerceAtMost(9999).toString(); validationError = null },
                                        modifier = Modifier.size(48.dp).testTag("habit_duration_plus_btn")
                                    ) { Icon(Icons.Default.Add, contentDescription = "One $unitWord more") }
                                    Text(
                                        text = unitWord,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }

                                val calcDate = durationCalculationResult.getOrNull()
                                val calcErr = durationCalculationResult.exceptionOrNull()?.message
                                if (calcDate != null) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Scheduled end date: ${HabitLifetimeCalculator.formatHumanReadableDate(calcDate)} ($calcDate)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(10.dp)
                                                .testTag("habit_calculated_end_date_text")
                                        )
                                    }
                                } else if (calcErr != null) {
                                    Text(
                                        text = calcErr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.testTag("habit_duration_validation_text")
                                    )
                                }
                            }
                            HabitDurationKind.SPECIFIC_DATE -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = specificEndDateText,
                                        onValueChange = {
                                            specificEndDateText = it
                                            validationError = null
                                        },
                                        label = { Text("End date (YYYY-MM-DD)") },
                                        placeholder = { Text("e.g. 2026-12-25") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .bringIntoViewOnFocus()
                                            .testTag("habit_specific_end_date_input")
                                    )
                                    OutlinedButton(
                                        onClick = { showEndDatePickerDialog = true },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("habit_pick_end_date_btn")
                                    ) {
                                        Text("Calendar")
                                    }
                                }

                                val calcDate = durationCalculationResult.getOrNull()
                                val calcErr = durationCalculationResult.exceptionOrNull()?.message
                                if (calcDate != null) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Selected end date: ${HabitLifetimeCalculator.formatHumanReadableDate(calcDate)} ($calcDate)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(10.dp)
                                                .testTag("habit_calculated_end_date_text")
                                        )
                                    }
                                } else if (calcErr != null) {
                                    Text(
                                        text = calcErr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.testTag("habit_duration_validation_text")
                                    )
                                }
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
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(role = Role.Button) { showAdvancedOptions = !showAdvancedOptions }
                                .padding(vertical = 2.dp)
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
                                    text = if (reminderEnabled) {
                                        "Reminder at ${reminderHour12Text.ifBlank { "--" }}:${reminderMinuteText.padStart(2, '0')} ${if (reminderIsPm) "PM" else "AM"} · note, category, colour, goal"
                                    } else {
                                        "Reminder off · note, category, colour, lifetime goal"
                                    },
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
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
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
                                            .clickable(role = Role.Button) { selectedColorHex = hex }
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

            // STEP 5B (shown last, after schedule and duration): Concise Interpretation Summary Card & "Adjust interpretation" Progressive Disclosure
            item {
                if (isEditing || rawHabitText.isNotBlank()) Card(
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                InterpretationSummaryRow(
                                    label = "Habit name",
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
                                if (!parsedInterpretation.preferredTimeHint.isNullOrBlank()) {
                                    InterpretationSummaryRow(
                                        label = "Preferred time",
                                        value = parsedInterpretation.preferredTimeHint,
                                        testTag = "smart_interpreted_time_hint_value"
                                    )
                                }
                                if (durationKind != HabitDurationKind.NONE) {
                                    InterpretationSummaryRow(
                                        label = "Duration",
                                        value = durationCalculationResult.getOrNull()?.let { endDate ->
                                            "Until ${HabitLifetimeCalculator.formatHumanReadableDate(endDate)} ($endDate)"
                                        } ?: "Select valid end date",
                                        testTag = "smart_interpreted_end_date_value"
                                    )
                                }
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
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
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
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )

                            OutlinedTextField(
                                value = interpretedActivityName,
                                onValueChange = {
                                    interpretedActivityName = it
                                    userEditedActivityName = true
                                },
                                label = { Text("Habit display name") },
                                placeholder = { Text("e.g. 10 Push-ups, Read 20 Pages, Drink Water") },
                                supportingText = { Text("Displayed on Today, Habits, and History. Editing this won't change your target or schedule.") },
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
                                        .clickable(role = Role.Button) {
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
                                        .padding(vertical = 2.dp)
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
                                        .clickable(role = Role.Button) {
                                            userOverrodeMeasurement = true
                                            selectedMode = TrackingMode.EFFORT_AWARE
                                        }
                                        .padding(vertical = 2.dp)
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
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
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

        }
        Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f), tonalElevation = 2.dp) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
                            if (cleanName.trimEnd('.', '!', ' ').lowercase() in BARE_ACTION_WORDS) {
                                // A habit called just "Do" tells the person nothing; ask for the rest.
                                validationError = "\"$cleanName\" on its own is not a habit name. Add what you will do, for example \"10 push-ups daily\"."
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

                            val resolvedEndDateResult = HabitLifetimeCalculator.calculateEndDateResult(
                                startDate = habitStartDate,
                                durationKind = durationKind,
                                durationValue = durationValueText.trim().toIntOrNull(),
                                specificEndDateIso = specificEndDateText.trim().ifBlank { null }
                            )
                            if (resolvedEndDateResult.isFailure) {
                                validationError = resolvedEndDateResult.exceptionOrNull()?.message
                                    ?: "Please enter a valid duration or end date."
                                return@Button
                            }
                            val resolvedEndDate = resolvedEndDateResult.getOrNull()
                            val resolvedDurationValue = when (durationKind) {
                                HabitDurationKind.DAYS,
                                HabitDurationKind.WEEKS,
                                HabitDurationKind.MONTHS,
                                HabitDurationKind.YEARS -> durationValueText.trim().toIntOrNull()
                                else -> null
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
                                startLocalDate = habitStartDate.toString(),
                                scheduledEndLocalDate = resolvedEndDate?.toString(),
                                actualEndLocalDate = if (resolvedEndDate != null && !today.isBefore(resolvedEndDate)) {
                                    resolvedEndDate.toString()
                                } else {
                                    initialHabit?.actualEndLocalDate
                                },
                                endReason = initialHabit?.endReason,
                                durationKind = durationKind,
                                durationValue = resolvedDurationValue,
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

    if (showEndDatePickerDialog) {
        val initialSelectedDate = runCatching { LocalDate.parse(specificEndDateText.trim()) }.getOrNull()
            ?: habitStartDate.plusDays(30)
        val initialMillis = initialSelectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showEndDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val pickedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            specificEndDateText = pickedDate.toString()
                            validationError = null
                        }
                        showEndDatePickerDialog = false
                    },
                    modifier = Modifier.testTag("confirm_end_date_picker_btn")
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEndDatePickerDialog = false },
                    modifier = Modifier.testTag("cancel_end_date_picker_btn")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("habit_end_date_picker_dialog")
        ) {
            DatePicker(state = datePickerState)
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

/** A single-row segmented control: one rounded container, the selected segment filled. */
@Composable
private fun SegmentedChoice(
    labels: List<String>,
    tags: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .selectableGroup()
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(index) })
                    .testTag(tags[index])
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/** Action words that are meaningless as a whole habit name. */
private val BARE_ACTION_WORDS = setOf("do", "doing", "make", "go", "try", "have", "perform", "get", "take")

/** Section title with the current choice summarised on one line; tap to expand or collapse. */
@Composable
private fun FormSectionHeader(
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClickLabel = if (expanded) "Collapse $title" else "Change $title") { onToggle() }
            .heightIn(min = 48.dp)
            .padding(vertical = 2.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null
        )
    }
}
