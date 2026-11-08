package com.example.dayseed.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.DayDetailHabitItem
import com.example.dayseed.data.DayRecordState
import com.example.dayseed.data.HonestCalendarDateState
import com.example.dayseed.data.JournalDraft
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.data.MoodCatalog
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TrackingMode
import java.math.BigInteger
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DayDetailScreen(
    date: LocalDate,
    viewModel: DayseedViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val habits by viewModel.habits.collectAsState()
    val habitVersions by viewModel.habitVersions.collectAsState()
    val habitEntries by viewModel.habitEntries.collectAsState()
    val habitDayOutcomes by viewModel.habitDayOutcomes.collectAsState()
    val habitDateNotes by viewModel.habitDateNotes.collectAsState()
    val habitDateExceptions by viewModel.habitDateExceptions.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()
    val journalDrafts by viewModel.journalDrafts.collectAsState()

    val referenceToday = viewModel.repository.clock.todayLocal()
    val summary = remember(
        date,
        referenceToday,
        habits,
        habitVersions,
        habitEntries,
        habitDayOutcomes,
        habitDateNotes,
        habitDateExceptions,
        journalEntries,
        journalDrafts
    ) {
        viewModel.repository.computeDayDetailSummary(date, referenceToday)
    }

    val fullDateFmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("day_detail_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("day_detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = summary.date.format(fullDateFmt),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("day_detail_date_title")
                    )
                    Text(
                        text = when {
                            summary.isToday -> "Today"
                            summary.isFutureDate -> "Future Date (Scheduled Preview)"
                            summary.isNeutralTransitionGap -> "Week-Start Transition Day"
                            else -> "Historical Day Record"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = { viewModel.selectHistoryDate(date.minusDays(1)) },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("day_detail_prev_day")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous day"
                        )
                    }
                    IconButton(
                        onClick = { viewModel.selectHistoryDate(date.plusDays(1)) },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("day_detail_next_day")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next day"
                        )
                    }
                }
            }
        }

        // Mood & Collected Moments Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("day_detail_overview_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Day Mood & Moments",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            val moodText = if (summary.primaryMood != null) {
                                val sec = summary.secondaryMood?.let { " + ${it.symbol} ${it.label}" }.orEmpty()
                                "${summary.primaryMood.symbol} ${summary.primaryMood.label}$sec"
                            } else {
                                "No mood recorded for this date"
                            }
                            Text(
                                text = moodText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("day_detail_mood_summary")
                            )
                        }
                        if (summary.collectedMomentsSummary != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "⏳ ${summary.collectedMomentsSummary.formattedTotalDuration}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("day_detail_duration_badge")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Habits Section
        item {
            Text(
                text = "Habits on ${summary.date}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (summary.habitItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("day_detail_no_habits_card"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "No habits were active or recorded on this date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(summary.habitItems, key = { it.habit.habitId }) { item ->
                DayDetailHabitCard(
                    item = item,
                    date = summary.date,
                    isFutureDate = summary.isFutureDate,
                    onRecordOrEditSession = { entryId ->
                        viewModel.openRecordSession(item.habit.habitId, summary.date, entryId)
                    },
                    onOpenHabitDetail = {
                        viewModel.openHabitDetail(item.habit.habitId, summary.date)
                    }
                )
            }
        }

        // Journal Reflections Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Journal Reflections (${summary.journalEntries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { viewModel.openJournalEditor(entry = null, forDate = summary.date) },
                    modifier = Modifier.testTag("day_detail_new_journal_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Write Reflection")
                }
            }
        }

        if (summary.unsavedDrafts.isNotEmpty()) {
            items(summary.unsavedDrafts, key = { it.draftId }) { draft ->
                DayDetailDraftCard(
                    draft = draft,
                    onResumeDraft = {
                        viewModel.openJournalEditor(
                            entry = null,
                            forDate = summary.date,
                            linkedHabitId = draft.linkedHabitId,
                            promptId = draft.promptId,
                            draftId = draft.draftId
                        )
                    }
                )
            }
        }

        if (summary.journalEntries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("day_detail_no_journals_card"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "No saved journal entries for ${summary.date}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(summary.journalEntries, key = { it.journalEntryId }) { entry ->
                DayDetailJournalEntryCard(
                    entry = entry,
                    onEdit = { viewModel.openJournalEditor(entry = entry, forDate = summary.date) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DayDetailHabitCard(
    item: DayDetailHabitItem,
    date: LocalDate,
    isFutureDate: Boolean,
    onRecordOrEditSession: (String?) -> Unit,
    onOpenHabitDetail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("day_detail_habit_${item.habit.habitId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenHabitDetail() }
                ) {
                    Text(
                        text = item.effectiveVersion.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${item.honestState.nonColorIndicator} ${item.honestState.displayLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = when (item.honestState) {
                            HonestCalendarDateState.COMPLETED_TARGET_MET -> MaterialTheme.colorScheme.primary
                            HonestCalendarDateState.MISSED_SCHEDULED_DATE -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.testTag("day_detail_habit_state_${item.habit.habitId}")
                    )
                }

                if (!isFutureDate) {
                    OutlinedButton(
                        onClick = { onRecordOrEditSession(null) },
                        modifier = Modifier.testTag("day_detail_log_session_${item.habit.habitId}")
                    ) {
                        Text("Log / Edit")
                    }
                }
            }

            if (!isFutureDate) {
                if (item.effectiveVersion.targetPeriod == TargetPeriod.WEEK) {
                    val contributionUnitLabel = when (item.effectiveVersion.trackingMode.canonical()) {
                        TrackingMode.QUANTITY, TrackingMode.DURATION ->
                            item.effectiveVersion.unitCode?.ifBlank { "units" } ?: "units"
                        else -> if (item.totalUnits == BigInteger.ONE) "day" else "days"
                    }
                    val contributionText = when {
                        item.dayRecordState == DayRecordState.EXPLICIT_ZERO ->
                            "Selected date contribution: 0 $contributionUnitLabel (explicit 0 check-in)"
                        else ->
                            "Selected date contribution: ${item.totalUnits} $contributionUnitLabel"
                    }
                    Text(
                        text = contributionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("day_detail_habit_band_${item.habit.habitId}")
                    )

                    if (item.weekProgressSummary != null) {
                        Text(
                            text = item.weekProgressSummary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("day_detail_habit_week_summary_${item.habit.habitId}")
                        )
                    }
                } else {
                    val unitLabel = item.effectiveVersion.unitCode ?: "sessions"
                    Text(
                        text = "${item.totalUnits} / ${item.targetUnits} $unitLabel • ${item.progressBand.displayName} (${item.progressBand.rangeLabel})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("day_detail_habit_band_${item.habit.habitId}")
                    )
                }
            }

            if (item.restDayException != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "⏸ Rest Day${item.restDayException.note?.let { ": $it" }.orEmpty()}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            item.entries.forEach { entry ->
                val unit = item.effectiveVersion.unitCode ?: "units"
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRecordOrEditSession(entry.entryId) }
                        .testTag("day_detail_entry_${entry.entryId}"),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
                                text = if (entry.valueUnits == 0) {
                                    "0 $unit logged (explicit check-in)"
                                } else {
                                    "${entry.valueUnits} $unit"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (!entry.entryNote.isNullOrBlank()) {
                                Text(
                                    text = entry.entryNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit session",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item.outcomes.forEach { outcome ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (outcome.outcomeKind == "EFFORT_LOGGED") {
                            "Effort recorded: ${outcome.effortLevel ?: "USUAL"}"
                        } else {
                            "✓ Completed on $date"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            item.dateNotes.forEach { note ->
                if (note.body.isNotBlank()) {
                    Text(
                        text = "Note: ${note.body}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("day_detail_note_${note.noteId}")
                    )
                }
            }
        }
    }
}

@Composable
private fun DayDetailDraftCard(
    draft: JournalDraft,
    onResumeDraft: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onResumeDraft() }
            .testTag("day_detail_draft_${draft.draftId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Unsaved Draft • Tap to resume",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            if (!draft.title.isNullOrBlank()) {
                Text(
                    text = draft.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = draft.body.ifBlank { "(Empty draft body)" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DayDetailJournalEntryCard(
    entry: JournalEntry,
    onEdit: () -> Unit
) {
    val primaryMood = MoodCatalog.findByCode(entry.primaryMoodCode)
    val secondaryMood = MoodCatalog.findByCode(entry.secondaryMoodCode)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("day_detail_journal_${entry.journalEntryId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.title?.takeIf { it.isNotBlank() } ?: "Reflection",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (primaryMood != null) {
                    val moodBadge = buildString {
                        append("${primaryMood.symbol} ${primaryMood.label}")
                        if (secondaryMood != null) {
                            append(" • ${secondaryMood.symbol} ${secondaryMood.label}")
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = moodBadge,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Text(
                text = entry.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
