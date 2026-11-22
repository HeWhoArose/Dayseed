package com.example.dayseed.ui.screens

import com.example.dayseed.ui.components.friendlyDate
import com.example.dayseed.ui.components.DayseedStatusPill
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.dayseed.ui.components.spokenDescription
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.HabitEntry
import com.example.dayseed.data.HabitVersion
import com.example.dayseed.data.ProgressBandClassifier
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.StreakAndGroveEngine
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.UnitFormatter
import com.example.dayseed.data.WeekBoundaryHelper
import com.example.dayseed.data.WeekStartPolicyEntry
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Resolves the governing [HabitVersion] for a session recorded on [evaluationDate]:
 * - Weekly habits (`targetPeriod == TargetPeriod.WEEK` or `scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET`)
 *   use the frozen start-of-week version under U-12 Option B (`StreakAndGroveEngine.resolveFrozenWeekVersion`).
 * - Daily habits use the effective version on [evaluationDate] (`StreakAndGroveEngine.resolveEffectiveVersion`).
 */
internal fun resolveRecordSessionGoverningVersion(
    versions: List<HabitVersion>,
    weekStartHistory: List<WeekStartPolicyEntry>,
    evaluationDate: LocalDate,
    fallbackVersion: HabitVersion
): HabitVersion {
    if (versions.isEmpty()) return fallbackVersion
    val dateVersion = StreakAndGroveEngine.resolveEffectiveVersion(versions, evaluationDate)
        ?: versions.minWithOrNull(compareBy<HabitVersion> { it.effectiveFromLocalDate }.thenBy { it.createdAtUtcMs })
        ?: fallbackVersion
    val firstHabitDate = versions
        .mapNotNull { runCatching { LocalDate.parse(it.effectiveFromLocalDate) }.getOrNull() }
        .minOrNull() ?: evaluationDate
    val weekStartIso = WeekBoundaryHelper.resolveWeekStartPolicyForDate(evaluationDate, weekStartHistory)
    val (weekStart, _) = WeekBoundaryHelper.weekBounds(evaluationDate, weekStartIso)
    val anchorDate = if (firstHabitDate.isAfter(weekStart)) firstHabitDate else weekStart
    val weekStartVersion = StreakAndGroveEngine.resolveEffectiveVersion(versions, anchorDate)
        ?: dateVersion
    return if (
        weekStartVersion.targetPeriod == TargetPeriod.WEEK ||
        weekStartVersion.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET ||
        dateVersion.targetPeriod == TargetPeriod.WEEK ||
        dateVersion.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET
    ) {
        StreakAndGroveEngine.resolveFrozenWeekVersion(
            sortedVersions = versions,
            weekStart = weekStart,
            firstHabitDate = firstHabitDate,
            fallbackDate = evaluationDate,
            fallbackVersion = fallbackVersion
        ) ?: dateVersion
    } else {
        dateVersion
    }
}

/**
 * Computes the daily preview total on [RecordSessionSheet], distinguishing session-count weekly
 * habits (`unitCode == "sessions"`) from cumulative quantity/duration habits.
 */
internal fun computeRecordSessionDailyPreviewTotal(
    existingEntriesOnDate: List<HabitEntry>,
    previewValueUnits: Int,
    effectiveMode: TrackingMode,
    effectiveTargetPeriod: TargetPeriod,
    effectiveUnitCode: String?
): Int {
    val isWeeklySessionCount = effectiveTargetPeriod == TargetPeriod.WEEK &&
        effectiveMode.canonical() == TrackingMode.QUANTITY &&
        UnitFormatter.cleanUnitCode(effectiveUnitCode).equals("sessions", ignoreCase = true)
    return if (isWeeklySessionCount) {
        val existingSessions = existingEntriesOnDate.count { it.valueUnits > 0 }
        val newSession = if (previewValueUnits > 0) 1 else 0
        existingSessions + newSession
    } else {
        existingEntriesOnDate.sumOf { it.valueUnits } + previewValueUnits.coerceAtLeast(0)
    }
}

/**
 * Pure helper for RecordSessionSheet initial value text.
 *
 * - Editing an existing entry (`existingEntry != null`): pre-populates the original recorded value
 *   (formatted in user-facing units, preserving decimal precision where applicable).
 * - New Simple Check-Off (`COMPLETION_ONLY` / `EFFORT_AWARE`): defaults to `"1"`.
 * - New Quantity or Duration entry (`existingEntry == null`): starts with an empty input (`""`)
 *   so the user never sees or accidentally submits an assumed full-target amount.
 */
internal fun defaultRecordSessionValueText(
    existingEntry: HabitEntry?,
    effectiveMode: TrackingMode,
    effectiveTargetPeriod: TargetPeriod = TargetPeriod.DAY,
    effectiveUnitCode: String? = null,
    effectiveTargetValue: Int? = null
): String {
    if (existingEntry != null) {
        return UnitFormatter.formatStoredUnitsForInput(existingEntry.valueUnits, effectiveUnitCode)
    }
    return when (effectiveMode.canonical()) {
        TrackingMode.COMPLETION_ONLY, TrackingMode.EFFORT_AWARE, TrackingMode.EFFORT_LEVELS -> "1"
        TrackingMode.QUANTITY, TrackingMode.DURATION -> ""
    }
}

/**
 * Pure helper for RecordSessionSheet date selection validation (D-193: future dates rejected).
 */
internal fun validateRecordSessionDate(
    selectedDate: LocalDate,
    today: LocalDate,
    habitCreatedDate: LocalDate
): String? {
    return when {
        selectedDate.isAfter(today) -> "Future dates cannot be logged (D-193)."
        selectedDate.isBefore(habitCreatedDate) ->
            "Date is before this habit was created ($habitCreatedDate)."
        else -> null
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordSessionSheet(
    viewModel: DayseedViewModel,
    habitId: String,
    initialLocalDate: String,
    entryId: String? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val habitVersions by viewModel.habitVersions.collectAsState()
    val weekStartHistory by viewModel.weekStartHistory.collectAsState()
    val entries by viewModel.habitEntries.collectAsState()
    val summaries by viewModel.habitSummaries.collectAsState()

    val habit = habits.find { it.habitId == habitId }
    if (habit == null) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .testTag("record_session_sheet"),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Habit not found.", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("secondary_back_btn")
                    ) {
                        Text("Back")
                    }
                }
            }
        }
        return
    }

    val today = viewModel.repository.clock.todayLocal()
    var activeEditingEntryId by remember(entryId) { mutableStateOf(entryId) }
    val existingEntry = remember(entries, activeEditingEntryId) {
        activeEditingEntryId?.let { id -> entries.find { it.entryId == id } }
    }

    var selectedDateStr by remember(initialLocalDate, existingEntry) {
        mutableStateOf(existingEntry?.localDate ?: initialLocalDate)
    }
    val selectedDate = remember(selectedDateStr) {
        runCatching { LocalDate.parse(selectedDateStr) }.getOrDefault(today)
    }

    val versionsForHabit = remember(habitVersions, habit.habitId) {
        habitVersions.filter { it.habitId == habit.habitId }
            .sortedBy { it.effectiveFromLocalDate }
    }

    val habitCreatedDate = remember(versionsForHabit, habit.createdAtUtcMs, today) {
        versionsForHabit.minOfOrNull { it.effectiveFromLocalDate }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: if (habit.createdAtUtcMs > 0L) {
                Instant.ofEpochMilli(habit.createdAtUtcMs).atZone(ZoneId.systemDefault()).toLocalDate()
            } else {
                today.minusDays(30)
            }
    }

    // Resolve governing HabitVersion on selectedDate (supporting U-12 Option B frozen weekly rules)
    val effectiveVersion = remember(versionsForHabit, weekStartHistory, habit.habitId, selectedDate, habitCreatedDate) {
        val fallback = versionsForHabit.lastOrNull() ?: HabitVersion(
            versionId = "fallback_${habit.habitId}",
            habitId = habit.habitId,
            effectiveFromLocalDate = habitCreatedDate.toString(),
            name = habit.name,
            trackingMode = habit.trackingMode,
            unitCode = habit.unitCode,
            targetPeriod = habit.targetPeriod,
            targetValue = habit.targetValue,
            scheduleKind = habit.scheduleKind,
            scheduledWeekdaysMask = habit.scheduledWeekdaysMask,
            preferredWeekdaysMask = habit.preferredWeekdaysMask,
            reminderMode = habit.reminderMode,
            reminderTimeLocal = habit.reminderTimeLocal,
            lifecycleState = habit.lifecycleState,
            createdAtUtcMs = habit.createdAtUtcMs
        )
        resolveRecordSessionGoverningVersion(
            versions = versionsForHabit,
            weekStartHistory = weekStartHistory,
            evaluationDate = selectedDate,
            fallbackVersion = fallback
        )
    }

    val effectiveMode = effectiveVersion.trackingMode.canonical()
    val effectiveTarget = (effectiveVersion.targetValue ?: habit.targetValue ?: 1).coerceAtLeast(1)
    val effectiveTargetPeriod = effectiveVersion.targetPeriod
    val effectiveUnitCode = effectiveVersion.unitCode ?: habit.unitCode
        ?: if (effectiveMode == TrackingMode.DURATION) "min" else "units"
    val cleanUnitLabel = UnitFormatter.cleanUnitCode(effectiveUnitCode).ifBlank {
        if (effectiveMode == TrackingMode.DURATION) "min" else "units"
    }
    val isMeasuredMode = effectiveMode == TrackingMode.QUANTITY || effectiveMode == TrackingMode.DURATION

    var valueText by remember(existingEntry, effectiveVersion.versionId, effectiveMode, effectiveTarget, effectiveUnitCode) {
        mutableStateOf(
            defaultRecordSessionValueText(
                existingEntry = existingEntry,
                effectiveMode = effectiveMode,
                effectiveTargetPeriod = effectiveTargetPeriod,
                effectiveUnitCode = effectiveUnitCode,
                effectiveTargetValue = effectiveTarget
            )
        )
    }
    var entryNote by remember(existingEntry) {
        mutableStateOf(existingEntry?.entryNote ?: "")
    }
    var valueError by remember { mutableStateOf<String?>(null) }
    var hasSubmitted by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val recentDates = remember(today, habitCreatedDate) {
        (0..13).map { today.minusDays(it.toLong()) }
            .filter { !it.isBefore(habitCreatedDate) }
    }

    val dateValidationError = remember(selectedDate, today, habitCreatedDate) {
        validateRecordSessionDate(selectedDate, today, habitCreatedDate)
    }

    // Existing entries on selectedDate
    val entriesOnSelectedDate = remember(entries, habitId, selectedDateStr) {
        entries.filter { it.habitId == habitId && it.localDate == selectedDateStr }
            .sortedBy { it.createdAtUtcMs }
    }
    val otherEntriesOnDate = remember(entriesOnSelectedDate, existingEntry) {
        entriesOnSelectedDate.filter { it.entryId != existingEntry?.entryId }
    }

    val parsedStoredUnits = remember(valueText, effectiveUnitCode) {
        UnitFormatter.parseUserValueToStoredUnits(valueText, effectiveUnitCode)
    }
    val hasValidPositiveInput = parsedStoredUnits != null && parsedStoredUnits > 0
    val previewValueUnits = if (hasValidPositiveInput) parsedStoredUnits!! else 0
    val previewValueBig = BigInteger.valueOf(previewValueUnits.toLong())

    val existingRecordedDateTotal = remember(entriesOnSelectedDate) {
        ProgressBandClassifier.sumUnitsExact(entriesOnSelectedDate.map { it.valueUnits })
    }
    val existingDateTotalExcludingCurrent = remember(otherEntriesOnDate) {
        ProgressBandClassifier.sumUnitsExact(otherEntriesOnDate.map { it.valueUnits })
    }
    val projectedDailyTotal = if (existingEntry != null) {
        if (hasValidPositiveInput) {
            existingDateTotalExcludingCurrent.add(previewValueBig)
        } else {
            existingRecordedDateTotal
        }
    } else {
        existingRecordedDateTotal.add(previewValueBig)
    }

    val summary = summaries[habitId]
    val isWeeklyTarget = effectiveTargetPeriod == TargetPeriod.WEEK
    val targetUnitsBig = BigInteger.valueOf(effectiveTarget.toLong())

    val existingActiveTotal = if (isWeeklyTarget && summary != null) {
        summary.weekTotalUnits
    } else {
        existingRecordedDateTotal
    }

    val projectedActiveTotal = if (isWeeklyTarget && summary != null) {
        val prevEntryVal = BigInteger.valueOf((existingEntry?.valueUnits ?: 0).toLong())
        if (existingEntry != null && !hasValidPositiveInput) {
            summary.weekTotalUnits
        } else {
            summary.weekTotalUnits.subtract(prevEntryVal).max(BigInteger.ZERO).add(previewValueBig)
        }
    } else {
        projectedDailyTotal
    }

    val existingBand = ProgressBandClassifier.classify(existingActiveTotal, targetUnitsBig)
    val existingPct = ProgressBandClassifier.displayPercentage(existingActiveTotal, targetUnitsBig)
    val projectedBand = ProgressBandClassifier.classify(projectedActiveTotal, targetUnitsBig)
    val projectedPct = ProgressBandClassifier.displayPercentage(projectedActiveTotal, targetUnitsBig)

    val context = LocalContext.current
    val is24HourFormat = remember(context) { DateFormat.is24HourFormat(context) }

    val canSave = valueText.isNotBlank() &&
        hasValidPositiveInput &&
        dateValidationError == null &&
        !hasSubmitted

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("record_session_sheet"),
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
                    Column {
                        Text(
                            text = if (existingEntry != null) "Edit Progress Entry" else "Add Progress",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${habit.name} • ${summary?.formatTargetAndPeriodSummary() ?: habit.formatTargetAndPeriodSummary()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Date Selection Strip (Today or recent past dates; future dates blocked per D-193)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Session Date",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Selected: ${friendlyDate(selectedDate, today)}" + if (selectedDate == today) " (Today)" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("record_session_selected_date")
                                )
                            }
                            OutlinedButton(
                                onClick = { showDatePickerDialog = true },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("record_session_pick_date_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose date")
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recentDates, key = { it.toString() }) { d ->
                                val dStr = d.toString()
                                val isSelected = dStr == selectedDateStr
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedDateStr = dStr
                                        activeEditingEntryId = null
                                        valueError = null
                                    },
                                    label = {
                                        val labelText = if (d == today) {
                                            "Today (${d.format(DateTimeFormatter.ofPattern("MMM d"))})"
                                        } else {
                                            d.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                                        }
                                        Text(labelText)
                                    },
                                    modifier = Modifier.testTag("record_session_date_chip_$dStr")
                                )
                            }
                        }

                        if (dateValidationError != null) {
                            Text(
                                text = dateValidationError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag("record_session_date_error")
                            )
                        }
                    }
                }
            }

            // 2. Value Entry & Quick Increment Chips
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isMeasuredMode) {
                                if (existingEntry != null) {
                                    "Updated Amount ($cleanUnitLabel)"
                                } else {
                                    "Amount to Add ($cleanUnitLabel)"
                                }
                            } else {
                                "Check-Off Count"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (isMeasuredMode) {
                            // Friendly, scale-exact increments (for example +0.25 / +0.5 litres for a
                            // 1.5 litre target) instead of the old quarter-division artefact +0.37.
                            val quickIncrements = UnitFormatter.quickIncrementStoredUnits(
                                effectiveTarget,
                                effectiveUnitCode
                            )
                            val stepQuarter = quickIncrements?.first
                            val stepHalf = quickIncrements?.second
                            val scale = UnitFormatter.extractScale(effectiveUnitCode)
                            val quarterChipLabel = stepQuarter?.let { step ->
                                if (scale > 0 || UnitFormatter.isCurrencyUnit(effectiveUnitCode)) {
                                    "+${UnitFormatter.formatValueWithUnit(step, effectiveUnitCode)}"
                                } else {
                                    "+$step ${UnitFormatter.formatUnitForCount(step, effectiveUnitCode)}"
                                }
                            }
                            val halfChipLabel = stepHalf?.let { step ->
                                if (scale > 0 || UnitFormatter.isCurrencyUnit(effectiveUnitCode)) {
                                    "+${UnitFormatter.formatValueWithUnit(step, effectiveUnitCode)}"
                                } else {
                                    "+$step ${UnitFormatter.formatUnitForCount(step, effectiveUnitCode)}"
                                }
                            }
                            val targetChipLabel = "${UnitFormatter.formatValueWithUnit(effectiveTarget, effectiveUnitCode)} (Target)"

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val quarterStep = stepQuarter
                                val halfStep = stepHalf
                                val quarterLabel = quarterChipLabel
                                val halfLabel = halfChipLabel
                                if (quarterStep != null && quarterLabel != null) {
                                    OutlinedButton(
                                        onClick = {
                                            val current = (parsedStoredUnits ?: 0).coerceAtLeast(0)
                                            valueText = UnitFormatter.formatStoredUnitsForInput(current + quarterStep, effectiveUnitCode)
                                            valueError = null
                                        },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("record_session_quick_inc_quarter")
                                    ) {
                                        Text(quarterLabel)
                                    }
                                }
                                if (halfStep != null && halfLabel != null && halfStep != quarterStep) {
                                    OutlinedButton(
                                        onClick = {
                                            val current = (parsedStoredUnits ?: 0).coerceAtLeast(0)
                                            valueText = UnitFormatter.formatStoredUnitsForInput(current + halfStep, effectiveUnitCode)
                                            valueError = null
                                        },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("record_session_quick_inc_half")
                                    ) {
                                        Text(halfLabel)
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        valueText = UnitFormatter.formatStoredUnitsForInput(effectiveTarget, effectiveUnitCode)
                                        valueError = null
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("record_session_quick_set_target")
                                ) {
                                    Text(targetChipLabel)
                                }
                            }
                        }

                        val scale = UnitFormatter.extractScale(effectiveUnitCode)
                        val placeholderExample = if (scale > 0) {
                            UnitFormatter.formatStoredUnitsForInput((effectiveTarget / 2).coerceAtLeast(1), effectiveUnitCode)
                        } else {
                            "e.g. 2"
                        }

                        OutlinedTextField(
                            value = valueText,
                            onValueChange = { raw ->
                                valueText = raw.filter { ch -> ch.isDigit() || ch == '-' || ch == '.' }
                                valueError = null
                            },
                            label = {
                                Text(
                                    if (existingEntry != null) {
                                        "Entry amount ($cleanUnitLabel)"
                                    } else {
                                        "Amount to add ($cleanUnitLabel)"
                                    }
                                )
                            },
                            placeholder = {
                                Text(placeholderExample)
                            },
                            singleLine = true,
                            isError = valueError != null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (scale > 0) KeyboardType.Decimal else KeyboardType.Number
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnFocus()
                                .testTag("record_session_value_input")
                        )

                        if (valueError != null) {
                            Text(
                                text = valueError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag("record_session_value_error")
                            )
                        }

                        OutlinedTextField(
                            value = entryNote,
                            onValueChange = { entryNote = it },
                            label = { Text("Session note (optional)") },
                            placeholder = { Text("Saved with this session only — e.g. what went well") },
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnFocus()
                                .testTag("record_session_note_input")
                        )
                    }
                }
            }

            // 3. Clear Progress State Preview (Existing vs New Entry vs Projected Daily Total & D-040 Band)
            item {
                val formattedExistingRatio = UnitFormatter.formatProgressRatioBig(
                    existingRecordedDateTotal,
                    targetUnitsBig,
                    effectiveUnitCode
                )
                val formattedProjectedDailyRatio = UnitFormatter.formatProgressRatioBig(
                    projectedDailyTotal,
                    targetUnitsBig,
                    effectiveUnitCode
                )
                val newEntrySummaryText = when {
                    valueText.isBlank() -> "Enter an amount above to preview new total"
                    !hasValidPositiveInput -> "Invalid amount"
                    existingEntry != null -> "Updating entry to: ${UnitFormatter.formatValueWithUnit(previewValueUnits, effectiveUnitCode)}"
                    else -> "New entry: +${UnitFormatter.formatValueWithUnit(previewValueUnits, effectiveUnitCode)}"
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("record_session_preview_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Progress Summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            DayseedStatusPill(
                                text = projectedBand.label,
                                modifier = Modifier
                                    .semantics { contentDescription = projectedBand.spokenDescription() }
                                    .testTag("record_session_preview_band")
                            )
                        }

                        Text(
                            text = "Existing progress on ${friendlyDate(selectedDate, today)}: $formattedExistingRatio ($existingPct%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("record_session_preview_existing")
                        )

                        Text(
                            text = newEntrySummaryText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (hasValidPositiveInput) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.testTag("record_session_preview_new_entry")
                        )

                        Text(
                            text = "Resulting daily total on ${friendlyDate(selectedDate, today)}: $formattedProjectedDailyRatio ($projectedPct%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("record_session_preview_daily_total")
                        )

                        if (isWeeklyTarget) {
                            val formattedWeeklyRatio = UnitFormatter.formatProgressRatioBig(
                                projectedActiveTotal,
                                targetUnitsBig,
                                effectiveUnitCode
                            )
                            Text(
                                text = "Resulting weekly total: $formattedWeeklyRatio ($projectedPct%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("record_session_preview_weekly_total")
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (projectedPct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )

                        if (projectedActiveTotal > BigInteger.ZERO) {
                            Text(
                                text = projectedBand.encouragementQuote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = projectedBand.motivationQuote,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = existingBand.encouragementQuote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 4. Existing Progress Entries on Selected Date (Inline Edit & Delete)
            if (entriesOnSelectedDate.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("record_session_existing_entries_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Recorded on ${friendlyDate(selectedDate, today)} (${entriesOnSelectedDate.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            for (entry in entriesOnSelectedDate) {
                                val isBeingEdited = entry.entryId == activeEditingEntryId
                                val timeFormatted = remember(entry.createdAtUtcMs, is24HourFormat) {
                                    runCatching {
                                        val rawHhMm = Instant.ofEpochMilli(entry.createdAtUtcMs)
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalTime()
                                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                                        SettingsSupport.formatUnambiguousTimeDisplay(rawHhMm, is24HourFormat)
                                    }.getOrDefault("")
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isBeingEdited) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("record_session_entry_row_${entry.entryId}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = UnitFormatter.formatValueWithUnit(entry.valueUnits, effectiveUnitCode) +
                                                    if (timeFormatted.isNotBlank()) " • $timeFormatted" else "",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (!entry.entryNote.isNullOrBlank()) {
                                                Text(
                                                    text = entry.entryNote,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    activeEditingEntryId = entry.entryId
                                                    valueText = UnitFormatter.formatStoredUnitsForInput(
                                                        entry.valueUnits,
                                                        effectiveUnitCode
                                                    )
                                                    entryNote = entry.entryNote ?: ""
                                                    valueError = null
                                                },
                                                modifier = Modifier
                                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                                    .testTag("record_session_edit_entry_${entry.entryId}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit entry",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (activeEditingEntryId == entry.entryId) {
                                                        activeEditingEntryId = null
                                                        valueText = defaultRecordSessionValueText(
                                                            existingEntry = null,
                                                            effectiveMode = effectiveMode,
                                                            effectiveTargetValue = effectiveTarget,
                                                            effectiveUnitCode = effectiveUnitCode
                                                        )
                                                        entryNote = ""
                                                    }
                                                    viewModel.deleteHabitEntry(entry.entryId)
                                                },
                                                modifier = Modifier
                                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                                    .testTag("record_session_delete_entry_${entry.entryId}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete entry",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Save / Cancel Buttons
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
                            .testTag("record_session_cancel_btn")
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (hasSubmitted) return@Button
                            val trimmed = valueText.trim()
                            if (trimmed.isEmpty()) {
                                valueError = "Please enter an amount to record."
                                return@Button
                            }
                            val parsed = UnitFormatter.parseUserValueToStoredUnits(trimmed, effectiveUnitCode)
                            if (parsed == null) {
                                val bd = trimmed.replace(",", "").toBigDecimalOrNull()
                                valueError = if (bd != null && UnitFormatter.extractScale(effectiveUnitCode) == 0) {
                                    "Whole-number amount required for $cleanUnitLabel."
                                } else {
                                    "Please enter a valid positive amount."
                                }
                                return@Button
                            }
                            if (parsed <= 0) {
                                valueError = "Amount must be greater than 0."
                                return@Button
                            }
                            if (dateValidationError != null) {
                                return@Button
                            }
                            hasSubmitted = true
                            val cleanNote = entryNote.trim().ifBlank { null }
                            if (existingEntry != null) {
                                viewModel.updateHabitEntry(existingEntry.entryId, parsed, cleanNote)
                            } else {
                                viewModel.logHabitQuantityOrDuration(
                                    habit = habit,
                                    valueUnits = parsed,
                                    date = selectedDate,
                                    entryNote = cleanNote
                                )
                            }
                            onNavigateBack()
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("record_session_save_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (existingEntry != null) "Update Entry" else "Save Progress")
                    }
                }
            }
        }
    }

    if (showDatePickerDialog) {
        val initialMillis = remember(selectedDate) {
            selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            selectedDateStr = picked.toString()
                            activeEditingEntryId = null
                            valueError = null
                        }
                        showDatePickerDialog = false
                    },
                    modifier = Modifier.testTag("record_session_datepicker_confirm")
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
