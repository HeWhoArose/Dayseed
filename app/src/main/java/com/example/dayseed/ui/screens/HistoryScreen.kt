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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.DailyOutcome
import com.example.dayseed.data.DailyRecord
import com.example.dayseed.data.EffortLevel
import com.example.dayseed.data.Habit
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.ui.components.HabitCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    viewModel: DayseedViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val records by viewModel.records.collectAsState()
    val journals by viewModel.journalEntries.collectAsState()
    val selectedDate by viewModel.selectedHistoryDate.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var journalSearchQuery by remember { mutableStateOf("") }

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
                selectedDate = selectedDate,
                onSelectDate = { viewModel.selectHistoryDate(it) },
                habits = habits,
                records = records,
                onLogHabit = { habit, outcome, effort ->
                    viewModel.logHabit(habit, selectedDate, outcome, effort)
                },
                onResetRecord = { habitId ->
                    viewModel.removeRecord(habitId, selectedDate)
                },
                onEditHabit = { viewModel.openEditHabit(it) },
                onArchiveHabit = { viewModel.toggleHabitArchive(it.id, !it.isArchived) }
            )
        } else {
            JournalArchiveView(
                journals = journals,
                searchQuery = journalSearchQuery,
                onSearchChange = { journalSearchQuery = it },
                onEntryClick = { viewModel.openJournalEditor(it, it.date) }
            )
        }
    }
}

@Composable
private fun DailyLogView(
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    habits: List<Habit>,
    records: List<DailyRecord>,
    onLogHabit: (Habit, DailyOutcome, EffortLevel) -> Unit,
    onResetRecord: (String) -> Unit,
    onEditHabit: (Habit) -> Unit,
    onArchiveHabit: (Habit) -> Unit
) {
    val today = LocalDate.now()
    val recentDates = remember(today) {
        (0..29).map { today.minusDays(it.toLong()) }
    }

    val selectedRecords = remember(records, selectedDate) {
        records.filter { it.date == selectedDate }
    }
    val activeHabits = remember(habits) { habits.filter { !it.isArchived } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date horizontal strip
        item {
            Text(
                text = "Select Date",
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
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    val dateRecords = records.filter { it.date == date }
                    val completedCount = dateRecords.count { it.outcome == DailyOutcome.COMPLETED }

                    DatePill(
                        date = date,
                        isSelected = isSelected,
                        isToday = isToday,
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
                            text = if (selectedDate == today) "Today" else "Past log — adjustments are welcomed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val completed = selectedRecords.count { it.outcome == DailyOutcome.COMPLETED }
                    Text(
                        text = "$completed recorded",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Habit list for this date
        items(activeHabits, key = { it.id }) { habit ->
            val record = selectedRecords.find { it.habitId == habit.id }
            HabitCard(
                habit = habit,
                record = record,
                onLog = { outcome, effort ->
                    onLogHabit(habit, outcome, effort)
                },
                onResetRecord = {
                    onResetRecord(habit.id)
                },
                onEditHabit = { onEditHabit(habit) },
                onArchiveHabit = { onArchiveHabit(habit) }
            )
        }
    }
}

@Composable
private fun DatePill(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
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
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onEntryClick: (JournalEntry) -> Unit
) {
    val filteredJournals = remember(journals, searchQuery) {
        if (searchQuery.isBlank()) journals
        else journals.filter {
            it.content.contains(searchQuery, ignoreCase = true) ||
            it.prompt.contains(searchQuery, ignoreCase = true)
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
                placeholder = { Text("Search reflection entries...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_search_input")
            )
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
                        text = if (searchQuery.isBlank()) "No reflections recorded yet.\nWrite one on the Today screen!"
                               else "No reflections matching \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredJournals, key = { it.id }) { entry ->
                JournalArchiveCard(
                    entry = entry,
                    onClick = { onEntryClick(entry) }
                )
            }
        }
    }
}

@Composable
private fun JournalArchiveCard(
    entry: JournalEntry,
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
