package com.example.dayseed.ui.screens

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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.DailyOutcome
import com.example.dayseed.data.EffortLevel
import com.example.dayseed.data.HabitEntry
import com.example.dayseed.data.HabitVersion
import com.example.dayseed.data.ProgressBandClassifier
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.StreakAndGroveEngine
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.WeekBoundaryHelper
import com.example.dayseed.data.WeekStartPolicyEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Resolves the governing [HabitVersion] for [evaluationDate] in RecordSessionSheet (Phase 2 F-03, U-12 Option B):
 * 1. Resolves the effective version on [evaluationDate] using `StreakAndGroveEngine.resolveEffectiveVersion`
 *    (falling back to [fallbackVersion]).
 * 2. Resolves the week-start policy for [evaluationDate] using `WeekBoundaryHelper.resolveWeekStartPolicyForDate`
 *    and computes `(weekStart, weekEnd)` via `WeekBoundaryHelper.weekBounds`.
 * 3. Resolves the version at `weekStart` via `StreakAndGroveEngine.resolveEffectiveVersion(versions, weekStart) ?: dateVersion`.
 * 4. If either `weekStartVersion.targetPeriod == TargetPeriod.WEEK` or `dateVersion.targetPeriod == TargetPeriod.WEEK`,
 *    returns `StreakAndGroveEngine.resolveFrozenWeekVersion(versions, weekStart, weekEnd, dateVersion)`.
 * 5. Otherwise (daily habit), returns `dateVersion`.
 */
fun resolveRecordSessionGoverningVersion(
    versions: List<HabitVersion>,
    weekStartHistory: List<WeekStartPolicyEntry>,
    evaluationDate: LocalDate,
    fallbackVersion: HabitVersion
): HabitVersion {
    val nonEmptyVersions = versions.ifEmpty { listOf(fallbackVersion) }
    val dateVersion = StreakAndGroveEngine.resolveEffectiveVersion(nonEmptyVersions, evaluationDate)
        ?: fallbackVersion
    val firstHabitDate = nonEmptyVersions
        .mapNotNull { runCatching { LocalDate.parse(it.effectiveFromLocalDate) }.getOrNull() }
        .minOrNull() ?: evaluationDate
    val weekStartIso = WeekBoundaryHelper.resolveWeekStartPolicyForDate(evaluationDate, weekStartHistory)
    val (weekStart, _) = WeekBoundaryHelper.weekBounds(evaluationDate, weekStartIso)
    val anchorDate = if (firstHabitDate.isAfter(weekStart)) firstHabitDate else weekStart
    val weekStartVersion = StreakAndGroveEngine.resolveEffectiveVersion(nonEmptyVersions, anchorDate)
        ?: dateVersion
    return if (
        weekStartVersion.targetPeriod == TargetPeriod.WEEK ||
        weekStartVersion.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET ||
        dateVersion.targetPeriod == TargetPeriod.WEEK ||
        dateVersion.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET
    ) {
        StreakAndGroveEngine.resolveFrozenWeekVersion(
            sortedVersions = nonEmptyVersions,
            weekStart = weekStart,
            firstHabitDate = firstHabitDate,
            fallbackDate = evaluationDate,
            fallbackVersion = dateVersion
        ) ?: dateVersion
    } else {
        dateVersion
    }
}

fun defaultRecordSessionValueText(
    existingEntry: HabitEntry?,
    effectiveMode: TrackingMode,
    effectiveTargetPeriod: TargetPeriod,
    effectiveUnitCode: String?,
    effectiveTargetValue: Int?
): String {
    val isWeeklySessionCount = StreakAndGroveEngine.isWeeklySessionCountHabit(
        trackingMode = effectiveMode.canonical(),
        targetPeriod = effectiveTargetPeriod,
        unitCode = effectiveUnitCode
    )
    return when {
        existingEntry != null -> existingEntry.valueUnits.toString()
        effectiveMode.canonical() == TrackingMode.COMPLETION_ONLY ||
            effectiveMode.canonical() == TrackingMode.EFFORT_AWARE -> "1"
        isWeeklySessionCount -> "1"
        else -> (effectiveTargetValue ?: 10).toString()
    }
}

fun computeRecordSessionDailyPreviewTotal(
    existingEntriesOnDate: List<HabitEntry>,
    previewValueUnits: Int,
    effectiveMode: TrackingMode,
    effectiveTargetPeriod: TargetPeriod,
    effectiveUnitCode: String?
): Int {
    val isWeeklySessionCount = StreakAndGroveEngine.isWeeklySessionCountHabit(
        trackingMode = effectiveMode.canonical(),
        targetPeriod = effectiveTargetPeriod,
        unitCode = effectiveUnitCode
    )
    return if (isWeeklySessionCount) {
        existingEntriesOnDate.count { it.valueUnits > 0 } + (if (previewValueUnits > 0) 1 else 0)
    } else {
        existingEntriesOnDate.sumOf { it.valueUnits } + previewValueUnits
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    val entries by viewModel.habitEntries.collectAsState()
    val outcomes by viewModel.habitDayOutcomes.collectAsState()
    val weekStartHistory by viewModel.weekStartHistory.collectAsState()

    val habit = habits.find { it.habitId == habitId }
    if (habit == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Habit not found.", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onNavigateBack) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val existingEntry = remember(entries, entryId) {
        entryId?.let { id -> entries.find { it.entryId == id } }
    }
    val isEditing = existingEntry != null
    val today = viewModel.repository.clock.todayLocal()

    val versionsForHabit = remember(habitVersions, habit.habitId) {
        habitVersions.filter { it.habitId == habit.habitId }
    }

    val earliestEffectiveDate: LocalDate = remember(versionsForHabit, habit.createdAtUtcMs) {
        val minVerDate = versionsForHabit.minByOrNull { it.effectiveFromLocalDate }?.effectiveFromLocalDate
        if (minVerDate != null) {
            runCatching { LocalDate.parse(minVerDate) }.getOrElse {
                Instant.ofEpochMilli(habit.createdAtUtcMs).atZone(ZoneOffset.UTC).toLocalDate()
            }
        } else {
            Instant.ofEpochMilli(habit.createdAtUtcMs).atZone(ZoneOffset.UTC).toLocalDate()
        }
    }

    var dateInputText by remember(existingEntry, initialLocalDate) {
        mutableStateOf(existingEntry?.localDate ?: initialLocalDate)
    }

    val parsedDate = remember(dateInputText) {
        runCatching { LocalDate.parse(dateInputText.trim()) }.getOrNull()
    }

    val dateValidationError = remember(parsedDate, today, earliestEffectiveDate) {
        when {
            parsedDate == null -> "Enter a valid date in YYYY-MM-DD format."
            parsedDate.isAfter(today) -> "Future dates cannot be logged (D-188). Select today ($today) or an earlier date."
            parsedDate.isBefore(earliestEffectiveDate) -> "Cannot log before this habit's creation date ($earliestEffectiveDate)."
            else -> null
        }
    }

    val effectiveDate = parsedDate?.takeIf { dateValidationError == null } ?: today
    val fallbackVer = remember(habit, earliestEffectiveDate) {
        habit.currentVersion ?: HabitVersion(
            versionId = "fallback_${habit.habitId}",
            habitId = habit.habitId,
            effectiveFromLocalDate = earliestEffectiveDate.toString(),
            name = habit.name,
            description = habit.description,
            trackingMode = habit.trackingMode,
            unitCode = habit.unitCode,
            targetPeriod = habit.targetPeriod,
            targetValue = habit.targetValue,
            scheduleKind = habit.scheduleKind,
            scheduledWeekdaysMask = habit.scheduledWeekdaysMask,
            preferredWeekdaysMask = habit.preferredWeekdaysMask,
            lifecycleState = habit.lifecycleState,
            createdAtUtcMs = habit.createdAtUtcMs
        )
    }
    val governingVersion = remember(habit.habitId, effectiveDate, versionsForHabit, weekStartHistory, fallbackVer) {
        resolveRecordSessionGoverningVersion(
            versions = versionsForHabit,
            weekStartHistory = weekStartHistory,
            evaluationDate = effectiveDate,
            fallbackVersion = fallbackVer
        )
    }
    val effectiveMode = governingVersion.trackingMode.canonical()
    val effectiveTargetPeriod = governingVersion.targetPeriod
    val effectiveUnitCode = governingVersion.unitCode
    val effectiveTargetValue = governingVersion.targetValue
    val isWeeklyHabit = effectiveTargetPeriod == TargetPeriod.WEEK
    val isWeeklySessionCount = StreakAndGroveEngine.isWeeklySessionCountHabit(governingVersion)

    var valueText by remember(
        existingEntry,
        habit.habitId,
        effectiveDate,
        governingVersion.versionId,
        isWeeklySessionCount,
        effectiveMode,
        effectiveTargetPeriod,
        effectiveUnitCode,
        effectiveTargetValue
    ) {
        mutableStateOf(
            defaultRecordSessionValueText(
                existingEntry = existingEntry,
                effectiveMode = effectiveMode,
                effectiveTargetPeriod = effectiveTargetPeriod,
                effectiveUnitCode = effectiveUnitCode,
                effectiveTargetValue = effectiveTargetValue
            )
        )
    }
    var noteText by remember(existingEntry) {
        mutableStateOf(existingEntry?.entryNote ?: "")
    }
    var valueError by remember { mutableStateOf<String?>(null) }

    val parsedValueUnits = valueText.trim().toIntOrNull()
    val previewValueUnits = (parsedValueUnits ?: 0).coerceAtLeast(0)

    // Compute live preview for selected date & week
    val existingEntriesOnDate = remember(entries, habit.habitId, effectiveDate, existingEntry) {
        entries.filter {
            it.habitId == habit.habitId &&
                it.localDate == effectiveDate.toString() &&
                it.entryId != existingEntry?.entryId
        }
    }
    val projectedDailyTotal = computeRecordSessionDailyPreviewTotal(
        existingEntriesOnDate = existingEntriesOnDate,
        previewValueUnits = previewValueUnits,
        effectiveMode = effectiveMode,
        effectiveTargetPeriod = effectiveTargetPeriod,
        effectiveUnitCode = effectiveUnitCode
    )
    val dailyTarget = (effectiveTargetValue ?: 1).coerceAtLeast(1)

    val projectedWeeklySnapshot = remember(
        habit,
        versionsForHabit,
        entries,
        outcomes,
        weekStartHistory,
        effectiveDate,
        previewValueUnits,
        existingEntry,
        governingVersion,
        isWeeklyHabit
    ) {
        if (!isWeeklyHabit) {
            null
        } else {
            val simulatedEntry = HabitEntry(
                entryId = existingEntry?.entryId ?: "preview_entry",
                habitId = habit.habitId,
                localDate = effectiveDate.toString(),
                valueUnits = previewValueUnits,
                entryNote = noteText.ifBlank { null },
                createdAtUtcMs = System.currentTimeMillis(),
                updatedAtUtcMs = System.currentTimeMillis(),
                idempotencyKey = "preview_key"
            )
            val simulatedEntries = entries
                .filter { it.habitId == habit.habitId && it.entryId != existingEntry?.entryId } + simulatedEntry
            val habitOutcomes = outcomes.filter { it.habitId == habit.habitId }
            StreakAndGroveEngine.computeWeeklyProgressSnapshot(
                versions = versionsForHabit.ifEmpty { listOf(governingVersion) },
                entries = simulatedEntries,
                outcomes = habitOutcomes,
                weekStartHistory = weekStartHistory,
                evaluationDate = effectiveDate,
                fallbackVersion = governingVersion
            )
        }
    }

    val previewBand = if (isWeeklyHabit && projectedWeeklySnapshot != null) {
        projectedWeeklySnapshot.weekProgressBand
    } else {
        ProgressBandClassifier.classify(projectedDailyTotal, dailyTarget)
    }

    val previewPercentage = if (isWeeklyHabit && projectedWeeklySnapshot != null) {
        projectedWeeklySnapshot.weekPercentage
    } else {
        ((projectedDailyTotal.toDouble() / dailyTarget.toDouble()) * 100.0).toInt()
    }

    val recentSelectableDates = remember(today, earliestEffectiveDate) {
        (0..13)
            .map { today.minusDays(it.toLong()) }
            .filter { !it.isBefore(earliestEffectiveDate) }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("record_session_sheet"),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = if (isEditing) "Edit Session" else "Record Session",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${governingVersion.name} • ${governingVersion.formatTargetAndPeriodSummary()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("record_session_header_subtitle")
                        )
                    }
                }

                if (isEditing && existingEntry != null) {
                    IconButton(
                        onClick = {
                            viewModel.deleteHabitEntry(existingEntry.entryId)
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("record_session_delete_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete session",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Date Selection Section
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Session Date",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (recentSelectableDates.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(recentSelectableDates, key = { it.toString() }) { d ->
                                val isPicked = dateInputText.trim() == d.toString()
                                val label = when (d) {
                                    today -> "Today"
                                    today.minusDays(1) -> "Yesterday"
                                    else -> d.format(DateTimeFormatter.ofPattern("MMM d"))
                                }
                                FilterChip(
                                    selected = isPicked,
                                    onClick = { dateInputText = d.toString() },
                                    label = { Text(label) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("record_session_date_chip_$d")
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = dateInputText,
                        onValueChange = { dateInputText = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        isError = dateValidationError != null,
                        supportingText = {
                            if (dateValidationError != null) {
                                Text(
                                    text = dateValidationError,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("record_session_date_error")
                                )
                            } else {
                                Text("Allowed range: $earliestEffectiveDate to $today")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewOnFocus()
                            .testTag("record_session_date_input")
                    )
                }
            }

            // Value / Effort Input Section
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val unitLabel = effectiveUnitCode?.takeIf { it.isNotBlank() }
                        ?: if (effectiveMode == TrackingMode.DURATION) "min" else "units"
                    Text(
                        text = if (effectiveMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION)) {
                            "Amount ($unitLabel)"
                        } else {
                            "Check-In Value"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = valueText,
                        onValueChange = {
                            valueText = it.filter { ch -> ch.isDigit() || ch == '-' }
                            valueError = null
                        },
                        label = {
                            Text(
                                if (effectiveMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION)) {
                                    "Session $unitLabel (0 or more)"
                                } else {
                                    "Sessions (1 = completed, 0 = honest check-in)"
                                }
                            )
                        },
                        isError = valueError != null,
                        supportingText = {
                            if (valueError != null) {
                                Text(valueError!!, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Logging 0 records an honest check-in without shame.")
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewOnFocus()
                            .testTag("record_session_value_input")
                    )

                    // Quick Preset Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = previewValueUnits == 0,
                            onClick = { valueText = "0" },
                            label = { Text("0 (Honest check-in)") },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("preset_zero_chip")
                        )

                        if (effectiveMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION)) {
                            val quarter = if (isWeeklySessionCount) 1 else (dailyTarget / 4).coerceAtLeast(1)
                            val half = if (isWeeklySessionCount) 1 else (dailyTarget / 2).coerceAtLeast(1)
                            val full = if (isWeeklySessionCount) 1 else dailyTarget
                            val extra = if (isWeeklySessionCount) 2 else ((dailyTarget * 5) / 4).coerceAtLeast(full + 1)

                            if (!isWeeklySessionCount) {
                                FilterChip(
                                    selected = previewValueUnits == quarter,
                                    onClick = { valueText = quarter.toString() },
                                    label = { Text("+$quarter $unitLabel") },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("preset_quarter_chip")
                                )
                                FilterChip(
                                    selected = previewValueUnits == half,
                                    onClick = { valueText = half.toString() },
                                    label = { Text("+$half $unitLabel") },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("preset_half_chip")
                                )
                            }
                            FilterChip(
                                selected = previewValueUnits == full,
                                onClick = { valueText = full.toString() },
                                label = {
                                    Text(
                                        if (isWeeklySessionCount) "1 session"
                                        else "$full $unitLabel (Target)"
                                    )
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("preset_full_chip")
                            )
                            FilterChip(
                                selected = previewValueUnits == extra,
                                onClick = { valueText = extra.toString() },
                                label = {
                                    Text(
                                        if (isWeeklySessionCount) "2 sessions"
                                        else "+$extra $unitLabel"
                                    )
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("preset_extra_chip")
                            )
                        } else {
                            FilterChip(
                                selected = previewValueUnits >= 1,
                                onClick = { valueText = "1" },
                                label = { Text("1 (Completed)") },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("preset_full_chip")
                            )
                        }
                    }

                    if (effectiveMode == TrackingMode.EFFORT_AWARE) {
                        Text(
                            text = "Legacy effort habit: records a completion check-in. Edit habit to switch to Quantity, Duration, or Simple Check-Off for target-based progress.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("record_session_legacy_effort_notice")
                        )
                    }

                    // Optional Session Note
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Session note (optional)") },
                        placeholder = { Text("Any brief context about this session...") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewOnFocus()
                            .testTag("record_session_note_input")
                    )
                }
            }

            // Live Preview Card (Daily Total, Weekly Aggregate, D-040 Progress Band & Dual Quotes)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("record_session_live_preview")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Progress Preview",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${previewBand.label} (${previewBand.shortBadge})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("record_session_preview_band")
                            )
                        }
                    }

                    val unitSuffix = effectiveUnitCode?.let { " $it" } ?: ""
                    Text(
                        text = "Resulting daily total on $effectiveDate: $projectedDailyTotal$unitSuffix",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("record_session_preview_daily_total")
                    )

                    if (isWeeklyHabit && projectedWeeklySnapshot != null) {
                        Text(
                            text = "Resulting week total (${projectedWeeklySnapshot.weekStartLocalDate}..${projectedWeeklySnapshot.weekEndLocalDate}): " +
                                "${projectedWeeklySnapshot.weekTotalUnits} / ${projectedWeeklySnapshot.weekTargetUnits}$unitSuffix ($previewPercentage%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("record_session_preview_weekly_total")
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (previewPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )

                    Text(
                        text = previewBand.encouragementQuote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = previewBand.motivationQuote,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        if (dateValidationError != null || parsedDate == null) {
                            return@Button
                        }
                        val units = valueText.trim().toIntOrNull()
                        if (units == null || units < 0) {
                            valueError = "Please enter a non-negative integer (0 or more)."
                            return@Button
                        }

                        val governedHabit = habit.copy(
                            currentVersion = governingVersion,
                            name = governingVersion.name,
                            trackingMode = governingVersion.trackingMode,
                            unitCode = governingVersion.unitCode,
                            targetPeriod = governingVersion.targetPeriod,
                            targetValue = governingVersion.targetValue,
                            scheduleKind = governingVersion.scheduleKind,
                            scheduledWeekdaysMask = governingVersion.scheduledWeekdaysMask,
                            preferredWeekdaysMask = governingVersion.preferredWeekdaysMask
                        )

                        if (isEditing && existingEntry != null) {
                            viewModel.updateHabitEntry(
                                entryId = existingEntry.entryId,
                                newValueUnits = units,
                                newEntryNote = noteText.trim().ifBlank { null }
                            )
                        } else {
                            if (effectiveMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION)) {
                                viewModel.logHabitQuantityOrDuration(
                                    habit = governedHabit,
                                    valueUnits = units,
                                    date = parsedDate,
                                    entryNote = noteText.trim().ifBlank { null }
                                )
                            } else {
                                if (units == 0) {
                                    viewModel.logHabitQuantityOrDuration(
                                        habit = governedHabit,
                                        valueUnits = 0,
                                        date = parsedDate,
                                        entryNote = noteText.trim().ifBlank { null }
                                    )
                                } else {
                                    viewModel.logHabit(
                                        habit = governedHabit,
                                        date = parsedDate,
                                        outcome = DailyOutcome.COMPLETED,
                                        effortLevel = EffortLevel.STANDARD,
                                        note = noteText.trim()
                                    )
                                }
                            }
                        }
                        onNavigateBack()
                    },
                    enabled = dateValidationError == null,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("record_session_save_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (isEditing) "Save Changes" else "Record Session")
                }
            }
        }
    }
}
