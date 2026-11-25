package com.example.dayseed.ui.screens

import com.example.dayseed.ui.components.dayseedHeading
import com.example.dayseed.ui.components.friendlyDate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.JournalDraft
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.data.MoodCatalog
import com.example.dayseed.ui.components.SharedMonthCalendar
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(viewModel: DayseedViewModel) {
    val journalEntries by viewModel.journalEntries.collectAsState()
    val journalDrafts by viewModel.journalDrafts.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val searchQuery by viewModel.journalSearchQuery.collectAsState()
    val selectedMoodFilter by viewModel.journalSelectedMoodFilter.collectAsState()
    val selectedDate by viewModel.selectedHistoryDate.collectAsState()

    val referenceToday = viewModel.repository.clock.todayLocal()
    var isDateJumpExpanded by rememberSaveable { mutableStateOf(false) }
    var isDateFilterActive by rememberSaveable { mutableStateOf(false) }
    var displayedMonthStr by rememberSaveable { mutableStateOf(YearMonth.from(selectedDate).toString()) }
    val displayedMonth = remember(displayedMonthStr) {
        runCatching { YearMonth.parse(displayedMonthStr) }.getOrElse { YearMonth.from(referenceToday) }
    }
    var draftPendingDiscard by remember { mutableStateOf<JournalDraft?>(null) }

    val habitsById = remember(habits) { habits.associateBy { it.habitId } }
    val monthStart = displayedMonth.atDay(1)
    val monthEnd = displayedMonth.atEndOfMonth()
    val habitsForMonth = remember(habits, displayedMonth) {
        viewModel.repository.getHabitsRelevantForDateRange(monthStart, monthEnd)
    }

    val filteredEntries = remember(journalEntries, searchQuery, selectedMoodFilter, isDateFilterActive, selectedDate) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        journalEntries.filter { entry ->
            val matchesDate = !isDateFilterActive || entry.localDate == selectedDate.toString()
            val matchesSearch = q.isEmpty() ||
                (entry.title?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                entry.body.lowercase(Locale.ROOT).contains(q)
            // A chosen mood highlights its days on the calendar (D-250); it no longer hides other entries.
            matchesDate && matchesSearch
        }
    }

    val entryCountByDate = remember(journalEntries) {
        journalEntries.groupingBy { it.localDate }.eachCount()
    }

    val hasActiveFilters = searchQuery.isNotBlank() || !selectedMoodFilter.isNullOrBlank() || isDateFilterActive

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp)
            .testTag("journal_screen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            val reflectionCountText = "${journalEntries.size} saved ${if (journalEntries.size == 1) "reflection" else "reflections"}"
            var overflowOpen by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Journal",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.testTag("journal_header_title").dayseedHeading()
                    )
                    Text(
                        text = reflectionCountText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(
                        onClick = { overflowOpen = true },
                        modifier = Modifier.size(48.dp).testTag("journal_overflow_btn")
                    ) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More journal actions")
                    }
                    DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Export journal") },
                            onClick = {
                                overflowOpen = false
                                viewModel.openJournalExportInSettings()
                            },
                            modifier = Modifier.testTag("journal_export_btn")
                        )
                    }
                }
            }
        }

        // Mood row: tap a mood to highlight the days it was recorded on the calendar below (D-250)
        item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMoodFilter == null,
                        onClick = { viewModel.setJournalMoodFilter(null) },
                        label = { Text("All") },
                        modifier = Modifier.testTag("journal_mood_filter_ALL")
                    )
                    MoodCatalog.pickerMoods.forEach { mood ->
                        FilterChip(
                            selected = selectedMoodFilter.equals(mood.code, ignoreCase = true),
                            onClick = {
                                viewModel.setJournalMoodFilter(
                                    if (selectedMoodFilter.equals(mood.code, ignoreCase = true)) null else mood.code
                                )
                            },
                            label = {
                                // Emoji only until chosen, so the whole mood range is scannable at a glance.
                                val chosen = selectedMoodFilter.equals(mood.code, ignoreCase = true)
                                Text(if (chosen) "${mood.symbol} ${mood.label}" else mood.symbol)
                            },
                            modifier = Modifier
                                .semantics { contentDescription = "Filter by ${mood.label}" }
                                .testTag("journal_mood_filter_${mood.code}")
                        )
                    }
                }

        }

        // Collapsible Date Jump Calendar (A2)
        item {
            run {
                Column(
                    modifier = Modifier.testTag("journal_date_jump_calendar"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SharedMonthCalendar(
                        displayedMonth = displayedMonth,
                        selectedDate = selectedDate,
                        referenceToday = referenceToday,
                        weekStartDay = viewModel.repository.getWeekStartDay(monthStart),
                        habitsForMonth = habitsForMonth,
                        selectedHabitFilterId = null,
                        onSelectHabitFilter = null,
                        evaluateDateStateForHabit = { hId, d ->
                            viewModel.repository.evaluateHonestCalendarDateState(hId, d, referenceToday)
                        },
                        journalEntries = journalEntries,
                        onPreviousMonth = { displayedMonthStr = displayedMonth.minusMonths(1).toString() },
                        onNextMonth = { displayedMonthStr = displayedMonth.plusMonths(1).toString() },
                        onJumpToToday = {
                            displayedMonthStr = YearMonth.from(referenceToday).toString()
                            viewModel.selectHistoryDate(referenceToday)
                            isDateFilterActive = false
                        },
                        onSelectDate = { clickedDate ->
                            viewModel.selectHistoryDate(clickedDate)
                            isDateFilterActive = true
                        },
                        testTagPrefix = "journal_date_jump",
                        journalMode = true,
                        highlightMoodCode = selectedMoodFilter
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = { viewModel.openDayDetail(selectedDate) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("journal_open_day_detail_button")
                        ) {
                            Text("Open ${friendlyDate(selectedDate, referenceToday)}")
                        }
                        if (isDateFilterActive) {
                            TextButton(
                                onClick = { isDateFilterActive = false },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("journal_clear_date_filter")
                            ) {
                                Text("Show All Dates")
                            }
                        }
                    }
                }
            }
        }

        // Unsaved Drafts Banner / Section
        if (journalDrafts.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_drafts_section"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Unsaved Drafts (${journalDrafts.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    journalDrafts.forEach { draft ->
                        val draftDate = runCatching { LocalDate.parse(draft.localDate) }.getOrElse { referenceToday }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) {
                                    viewModel.openJournalEditor(
                                        entry = null,
                                        forDate = draftDate,
                                        linkedHabitId = draft.linkedHabitId,
                                        promptId = draft.promptId,
                                        draftId = draft.draftId
                                    )
                                }
                                .testTag("journal_draft_card_${draft.draftId}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Draft • ${draft.localDate}",
                                        style = MaterialTheme.typography.labelSmall,
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
                                        text = draft.body.ifBlank { "(Tap to continue writing...)" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { draftPendingDiscard = draft },
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .testTag("journal_discard_draft_${draft.draftId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Discard draft"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search Input + Mood Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setJournalSearchQuery(it) },
                    placeholder = { Text("Search your journal") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.setJournalSearchQuery("") },
                                    modifier = Modifier.testTag("journal_search_clear_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_search_input")
                )

                if (hasActiveFilters) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filterSummary = buildList {
                            if (isDateFilterActive) add("Date: ${friendlyDate(selectedDate, referenceToday)}")
                            if (!selectedMoodFilter.isNullOrBlank()) {
                                val m = MoodCatalog.findByCode(selectedMoodFilter)
                                add("Mood: ${m?.label ?: selectedMoodFilter}")
                            }
                            if (searchQuery.isNotBlank()) add("Query: \"$searchQuery\"")
                        }.joinToString(" • ")

                        Text(
                            text = filterSummary,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                viewModel.setJournalSearchQuery("")
                                viewModel.setJournalMoodFilter(null)
                                isDateFilterActive = false
                            },
                            modifier = Modifier.testTag("journal_clear_filters_btn")
                        ) {
                            Text("Clear search")
                        }
                    }
                }
            }
        }

        // Entries List or Empty / No-Results State
        if (journalEntries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_empty_archive_state"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "No journal entries yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Capture a reflection, mood, or note whenever you want — independent of your habit streaks.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.openJournalEditor(entry = null, forDate = referenceToday) },
                            modifier = Modifier.testTag("journal_empty_write_first_btn")
                        ) {
                            Text("Write your first entry")
                        }
                    }
                }
            }
        } else if (filteredEntries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_no_results_state"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val noResultsTitle = if (searchQuery.isNotBlank()) {
                            "No entries found for '${searchQuery.trim()}'"
                        } else {
                            "No entries found for the selected filter"
                        }
                        Text(
                            text = noResultsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try clearing your search, mood chip, or date filter to view all ${journalEntries.size} saved reflections.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                viewModel.setJournalSearchQuery("")
                                viewModel.setJournalMoodFilter(null)
                                isDateFilterActive = false
                            },
                            modifier = Modifier.testTag("journal_no_results_clear_btn")
                        ) {
                            Text("Clear search")
                        }
                    }
                }
            }
        } else {
            items(filteredEntries, key = { it.journalEntryId }) { entry ->
                val linkedHabitName = entry.linkedHabitId?.let { habitsById[it]?.name }
                val sameDateCount = entryCountByDate[entry.localDate] ?: 1
                JournalArchiveCard(
                    entry = entry,
                    linkedHabitName = linkedHabitName,
                    sameDateTotalCount = sameDateCount,
                    onClick = {
                        viewModel.openJournalEditor(entry = entry, forDate = entry.date)
                    },
                    onOpenDayDetail = {
                        viewModel.openDayDetail(entry.date)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (draftPendingDiscard != null) {
        val targetDraft = draftPendingDiscard!!
        AlertDialog(
            onDismissRequest = { draftPendingDiscard = null },
            modifier = Modifier.testTag("confirm_discard_draft_dialog"),
            title = { Text("Discard Unsaved Draft?") },
            text = {
                Text("Discard unsaved draft? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.discardJournalDraft(targetDraft.draftId)
                        draftPendingDiscard = null
                    },
                    modifier = Modifier.testTag("confirm_discard_draft_btn")
                ) {
                    Text("Discard Draft")
                }
            },
            dismissButton = {
                TextButton(onClick = { draftPendingDiscard = null }) {
                    Text("Keep Editing")
                }
            }
        )
    }
    FloatingActionButton(
        onClick = {
            val targetDate = if (isDateFilterActive) selectedDate else referenceToday
            viewModel.openJournalEditor(entry = null, forDate = targetDate)
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp)
            .testTag("journal_add_entry_fab")
    ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "Write a new reflection")
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JournalArchiveCard(
    entry: JournalEntry,
    linkedHabitName: String?,
    sameDateTotalCount: Int,
    onClick: () -> Unit,
    onOpenDayDetail: () -> Unit
) {
    val primaryMood = MoodCatalog.findByCode(entry.primaryMoodCode)
    val secondaryMood = MoodCatalog.findByCode(entry.secondaryMoodCode)
    val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())
    // Plain time (for example 9:30 AM) shown only when the entry was written on the date it belongs to,
    // so a back-dated entry never shows a misleading time.
    val context = androidx.compose.ui.platform.LocalContext.current
    val timeText = remember(entry.createdAtUtcMs, entry.date) {
        journalCardTimeText(
            entry.createdAtUtcMs, entry.date,
            android.text.format.DateFormat.is24HourFormat(context), java.time.ZoneId.systemDefault()
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { onClick() }
            .testTag("journal_card_${entry.journalEntryId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.date.format(dateFmt) + (timeText?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(role = Role.Button) { onOpenDayDetail() }
                    )
                    if (sameDateTotalCount > 1) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "$sameDateTotalCount on date",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (primaryMood != null) {
                    val moodText = buildString {
                        append("${primaryMood.symbol} ${primaryMood.label}")
                        if (secondaryMood != null) {
                            append(" + ${secondaryMood.symbol} ${secondaryMood.label}")
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = moodText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (!entry.title.isNullOrBlank()) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (entry.prompt.isNotBlank()) {
                Text(
                    text = "Prompt: ${entry.prompt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = entry.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (!linkedHabitName.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "🌱 Linked habit: $linkedHabitName",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * Plain time for a Journal card, for example "9:30 AM" (or "09:30" in 24-hour mode).
 * Only returned when the entry was written on the date it belongs to, so a back-dated entry never shows a misleading time.
 */
internal fun journalCardTimeText(
    createdAtUtcMs: Long,
    entryDate: LocalDate,
    is24Hour: Boolean,
    zone: java.time.ZoneId,
    locale: Locale = Locale.getDefault()
): String? = runCatching {
    if (createdAtUtcMs <= 0L) return@runCatching null
    val created = java.time.Instant.ofEpochMilli(createdAtUtcMs).atZone(zone)
    if (created.toLocalDate() != entryDate) return@runCatching null
    created.toLocalTime().format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", locale))
}.getOrNull()
