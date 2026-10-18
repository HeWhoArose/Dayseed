package com.example.dayseed.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.DailyOutcome
import com.example.dayseed.data.EffortLevel
import com.example.dayseed.ui.components.ComebackCard
import com.example.dayseed.ui.components.GrowthMarkBadge
import com.example.dayseed.ui.components.HabitCard
import com.example.dayseed.ui.components.ReflectionCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    viewModel: DayseedViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val records by viewModel.records.collectAsState()
    val journals by viewModel.journalEntries.collectAsState()
    val comebackInfo by viewModel.comebackInfo.collectAsState()
    val growthMetrics by viewModel.growthMetrics.collectAsState()

    val today = LocalDate.now()
    val todayRecords = remember(records, today) {
        records.filter { it.date == today }
    }
    val todayJournal = remember(journals, today) {
        journals.find { it.date == today }
    }
    val activeHabits = remember(habits) {
        habits.filter { !it.isArchived }
    }
    val scheduledTodayHabits = remember(activeHabits, today) {
        activeHabits.filter { it.isScheduledFor(today) }
    }

    var showMetricInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddHabit() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_habit_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add new habit")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("today_screen_list"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Date and humane motto
            item {
                Column {
                    Text(
                        text = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Dayseed",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your progress is more than your streak.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Comeback Card (shown if absent > 2 days)
            if (comebackInfo != null) {
                item {
                    ComebackCard(
                        info = comebackInfo!!,
                        onPrimaryAction = {
                            viewModel.dismissComeback()
                        },
                        onSecondaryAction = {
                            viewModel.dismissComeback()
                        },
                        onDismiss = {
                            viewModel.dismissComeback()
                        }
                    )
                }
            }

            // Growth Mark Summary
            item {
                GrowthMarkBadge(
                    metrics = growthMetrics,
                    onInfoClick = { showMetricInfoDialog = true }
                )
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Seeds",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    val completedToday = todayRecords.count { it.outcome == DailyOutcome.COMPLETED }
                    Text(
                        text = "$completedToday of ${scheduledTodayHabits.size} tended",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Habits List
            if (scheduledTodayHabits.isEmpty()) {
                item {
                    EmptyHabitsCard(onAddHabit = { viewModel.openAddHabit() })
                }
            } else {
                items(scheduledTodayHabits, key = { it.id }) { habit ->
                    val record = todayRecords.find { it.habitId == habit.id }
                    HabitCard(
                        habit = habit,
                        record = record,
                        onLog = { outcome, effort ->
                            viewModel.logHabit(habit, today, outcome, effort)
                        },
                        onResetRecord = {
                            viewModel.removeRecord(habit.id, today)
                        },
                        onEditHabit = {
                            viewModel.openEditHabit(habit)
                        },
                        onArchiveHabit = {
                            viewModel.toggleHabitArchive(habit.id, !habit.isArchived)
                        }
                    )
                }
            }

            // Reflection Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                ReflectionCard(
                    entry = todayJournal,
                    onOpenEditor = { viewModel.openJournalEditor(todayJournal, today) }
                )
            }

            // Bottom breathing room for FAB
            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    if (showMetricInfoDialog) {
        AlertDialog(
            onDismissRequest = { showMetricInfoDialog = false },
            title = {
                Text("How Dayseed Measures Progress", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "• Collected Moments: Every date you do even a tiny action or honor a rest day is counted.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "• Tiny Actions Count: A single stretch or reading one paragraph is 100% valid effort.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "• No Streak Resets: Missed days do not erase past progress or reset any counter to zero.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showMetricInfoDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }
}

@Composable
private fun EmptyHabitsCard(onAddHabit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Spa,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No habits scheduled for today",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Enjoy your rest day, or plant a new habit seed anytime.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddHabit,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Create a Habit")
        }
    }
}
