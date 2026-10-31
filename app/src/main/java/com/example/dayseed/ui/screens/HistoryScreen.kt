package com.example.dayseed.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.DailyOutcome
import com.example.dayseed.data.DerivedDayStatus
import com.example.dayseed.data.EffortLevel
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitDateNote
import com.example.dayseed.data.HabitDayOutcome
import com.example.dayseed.data.HabitEntry
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.data.MoodCatalog
import com.example.dayseed.ui.components.HabitCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    viewModel: DayseedViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val outcomes by viewModel.habitDayOutcomes.collectAsState()
    val entries by viewModel.habitEntries.collectAsState()
    val dateNotes by viewModel.habitDateNotes.collectAsState()
    val journals by viewModel.journalEntries.collectAsState()
    val selectedDate by viewModel.selectedHistoryDate.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var journalSearchQuery by remember { mutableStateOf("") }
    var selectedMoodFilter by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen")
    ) {
        // Top Header
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(
                text = "History & Archive",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Look back without shame. Past records can always be adjusted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Daily Log", fontWeight = FontWeight.Medium) },
                modifier = Modifier.testTag("history_log_tab")
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Journal Archive", fontWeight = FontWeight.Medium) },
                modifier = Modifier.testTag("history_journal_tab")
            )
        }

        if (selectedTabIndex == 0) {
            DailyLogView(
                viewModel = viewModel,
                selectedDate = selectedDate,
                onSelectDate = { viewModel.selectHistoryDate(it) },
                habits = habits,
                outcomes = outcomes,
                entries = entries,
                dateNotes = dateNotes,
                onLogHabit = { habit, outcome, effort ->
                    viewModel.logHabit(habit, selectedDate, outcome, effort)
                },
                onLogQuantityOrDuration = { habit, units, note ->
                    viewModel.logHabitQuantityOrDuration(habit, units, selectedDate, note)
                },
                onDeleteEntry = { entryId ->
                    viewModel.deleteHabitEntry(entryId)
                },
                onSaveDateNote = { habitId, body ->
                    viewModel.saveHabitDateNote(habitId, selectedDate, body)
                },
                onResetRecord = { habitId ->
                    viewModel.removeRecord(habitId, selectedDate)
                },
                onEditHabit = { viewModel.openEditHabit(it) },
                onArchiveHabit = { viewModel.toggleHabitArchive(it.habitId, !it.isArchived) }
            )
        } else {
            JournalArchiveView(
                journals = journals,
                habitsById = habits.associate { it.habitId to it.name },
                searchQuery = journalSearchQuery,
                selectedMoodCode = selectedMoodFilter,
                onSearchChange = { journalSearchQuery = it },
                onSelectMoodFilter = { selectedMoodFilter = if (selectedMoodFilter == it) null else it },
                onEntryClick = { viewModel.openJournalEditor(it, it.date) }
            )
        }
    }
}

@Composable
private fun DailyLogView(
    viewModel: DayseedViewModel,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    habits: List<Habit>,
    outcomes: List<HabitDayOutcome>,
    entries: List<HabitEntry>,
    dateNotes: List<HabitDateNote>,
    onLogHabit: (Habit, DailyOutcome, EffortLevel) -> Unit,
    onLogQuantityOrDuration: (Habit, Int, String?) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onSaveDateNote: (String, String) -> Unit,
    onResetRecord: (String) -> Unit,
    onEditHabit: (Habit) -> Unit,
    onArchiveHabit: (Habit) -> Unit
) {
    val today = viewModel.repository.clock.todayLocal()
    val recentDates = remember(today) {
        (0..29).map { today.minusDays(it.toLong()) }
    }

    val selectedDateStr = selectedDate.toString()
    val selectedOutcomes = remember(outcomes, selectedDateStr) {
        outcomes.filter { it.localDate == selectedDateStr }
    }
    val selectedEntries = remember(entries, selectedDateStr) {
        entries.filter { it.localDate == selectedDateStr }
    }
    val selectedNotes = remember(dateNotes, selectedDateStr) {
        dateNotes.filter { it.localDate == selectedDateStr }
    }
    val activeHabits = remember(habits) {
        habits.filter { it.lifecycleState != HabitLifecycleState.ARCHIVED }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date horizontal strip
        item {
            Text(
                text = "Select Date (Past 30 Days)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(recentDates) { date ->
                    val dStr = date.toString()
                    val isSelected = date == selectedDate
                    val completedCount = outcomes.count { it.localDate == dStr } +
                        entries.filter { it.localDate == dStr && it.valueUnits > 0 }.map { it.habitId }.distinct().size

                    DatePill(
                        date = date,
                        isSelected = isSelected,
                        completedCount = completedCount,
                        onClick = { onSelectDate(date) }
                    )
                }
            }
        }

        // Selected Date Summary Bar
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedDate == today) "Today" else "Backdated log — adjustments are welcomed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val tendedCount = activeHabits.count { h ->
                        selectedOutcomes.any { it.habitId == h.habitId } ||
                            selectedEntries.any { it.habitId == h.habitId && it.valueUnits > 0 }
                    }
                    Text(
                        text = "$tendedCount tended",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Habit list for this date with effective-dated DerivedDayStatus
        items(activeHabits, key = { it.habitId }) { habit ->
            val outcome = selectedOutcomes.find { it.habitId == habit.habitId }
            val habitEntriesOnDate = selectedEntries.filter { it.habitId == habit.habitId }
            val dateNote = selectedNotes.find { it.habitId == habit.habitId }
            val derivedStatus = remember(habit.habitId, selectedDate, outcomes, entries) {
                viewModel.repository.evaluateDerivedDayStatus(habit.habitId, selectedDate)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val statusLabel = when (derivedStatus) {
                    DerivedDayStatus.COMPLETED -> "Goal met / Completed"
                    DerivedDayStatus.PARTIAL -> "Partial progress recorded"
                    DerivedDayStatus.LOGGED_ZERO -> "Checked in (0 recorded)"
                    DerivedDayStatus.OPEN_TODAY -> "Open for today"
                    DerivedDayStatus.DERIVED_MISSED -> "Quiet day (unlogged past scheduled day)"
                    DerivedDayStatus.UNSCHEDULED -> "Rest / Unscheduled day"
                    DerivedDayStatus.INACTIVE_LIFECYCLE -> "Paused or inactive on this date"
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("history_status_${habit.habitId}")
                )

                HabitCard(
                    habit = habit,
                    outcome = outcome,
                    entries = habitEntriesOnDate,
                    dateNote = dateNote,
                    onLogOutcome = { outKind, effort ->
                        onLogHabit(habit, outKind, effort)
                    },
                    onLogQuantityOrDuration = { units, note ->
                        onLogQuantityOrDuration(habit, units, note)
                    },
                    onDeleteEntry = onDeleteEntry,
                    onSaveDateNote = { body ->
                        onSaveDateNote(habit.habitId, body)
                    },
                    onResetRecord = {
                        onResetRecord(habit.habitId)
                    },
                    onEditHabit = { onEditHabit(habit) },
                    onArchiveHabit = { onArchiveHabit(habit) }
                )
            }
        }
    }
}

@Composable
private fun DatePill(
    date: LocalDate,
    isSelected: Boolean,
    completedCount: Int,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEE")),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f)
            )
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            if (completedCount > 0) {
                Text(
                    text = "🌱".repeat(completedCount.coerceAtMost(3)),
                    fontSize = 10.sp
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun JournalArchiveView(
    journals: List<JournalEntry>,
    habitsById: Map<String, String>,
    searchQuery: String,
    selectedMoodCode: String?,
    onSearchChange: (String) -> Unit,
    onSelectMoodFilter: (String) -> Unit,
    onEntryClick: (JournalEntry) -> Unit
) {
    val filteredJournals = remember(journals, searchQuery, selectedMoodCode) {
        journals.filter { entry ->
            val matchesQuery = searchQuery.isBlank() ||
                entry.body.contains(searchQuery, ignoreCase = true) ||
                (entry.title?.contains(searchQuery, ignoreCase = true) == true) ||
                entry.prompt.contains(searchQuery, ignoreCase = true)
            val matchesMood = selectedMoodCode == null ||
                entry.primaryMoodCode == selectedMoodCode ||
                entry.secondaryMoodCode == selectedMoodCode
            matchesQuery && matchesMood
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search reflections by keyword or title...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_search_input")
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MoodCatalog.starterMoods) { mood ->
                    FilterChip(
                        selected = selectedMoodCode == mood.code,
                        onClick = { onSelectMoodFilter(mood.code) },
                        label = { Text("${mood.symbol} ${mood.label}") }
                    )
                }
            }
        }

        if (filteredJournals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank() && selectedMoodCode == null) {
                            "No reflections recorded yet.\nWrite one on the Today screen!"
                        } else {
                            "No reflections matching your filter."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredJournals, key = { it.journalEntryId }) { entry ->
                JournalArchiveCard(
                    entry = entry,
                    linkedHabitName = entry.linkedHabitId?.let { habitsById[it] },
                    onClick = { onEntryClick(entry) }
                )
            }
        }
    }
}

@Composable
private fun JournalArchiveCard(
    entry: JournalEntry,
    linkedHabitName: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("journal_card_${entry.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit reflection",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (!entry.title.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val moods = listOfNotNull(entry.primaryMoodCode, entry.secondaryMoodCode)
                .mapNotNull { MoodCatalog.findByCode(it) }
            if (moods.isNotEmpty() || linkedHabitName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (m in moods) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "${m.symbol} ${m.label}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (linkedHabitName != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "🌱 $linkedHabitName",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (entry.prompt.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.prompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
