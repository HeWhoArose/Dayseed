package com.example.dayseed.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.ui.components.ComebackCard
import com.example.dayseed.ui.components.GrowthMarkBadge
import com.example.dayseed.ui.components.HabitCard
import com.example.dayseed.ui.components.ReflectionCard
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    viewModel: DayseedViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val outcomes by viewModel.habitDayOutcomes.collectAsState()
    val entries by viewModel.habitEntries.collectAsState()
    val dateNotes by viewModel.habitDateNotes.collectAsState()
    val journals by viewModel.journalEntries.collectAsState()
    val drafts by viewModel.journalDrafts.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val comebackInfo by viewModel.comebackInfo.collectAsState()
    val growthMetrics by viewModel.growthMetrics.collectAsState()
    val activeUndoToken by viewModel.activeUndoToken.collectAsState()

    val today = viewModel.repository.clock.todayLocal()
    val todayStr = today.toString()

    val todayOutcomes = remember(outcomes, todayStr) {
        outcomes.filter { it.localDate == todayStr }
    }
    val todayEntries = remember(entries, todayStr) {
        entries.filter { it.localDate == todayStr }
    }
    val todayDateNotes = remember(dateNotes, todayStr) {
        dateNotes.filter { it.localDate == todayStr }
    }
    val todayJournal = remember(journals, todayStr) {
        journals.find { it.localDate == todayStr }
    }
    val todayDraft = remember(drafts, todayStr) {
        drafts.find { it.localDate == todayStr && it.sourceContext == "JOURNAL_COMPOSER" }
    }

    val activeHabits = remember(habits) {
        habits.filter { it.lifecycleState == HabitLifecycleState.ACTIVE }
    }
    val scheduledTodayHabits = remember(activeHabits, today) {
        activeHabits.filter { it.isScheduledFor(today) }
    }

    val onboardingCompleted = remember(appSettings) {
        val v = appSettings["onboarding_completed"]?.valueText
        v.equals("true", ignoreCase = true) || v == "1" || habits.isNotEmpty()
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

            // First-run Onboarding Banner (D-015..D-021)
            if (!onboardingCompleted) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_welcome_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Welcome to Dayseed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "100% private and offline. No accounts, no cloud tracking, and no streak penalties. Tiny actions and partial progress always count.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.repository.setSettingSync("onboarding_completed", "BOOL", "true")
                                        viewModel.openAddHabit()
                                    },
                                    modifier = Modifier.testTag("onboarding_create_first_habit_btn")
                                ) {
                                    Text("Plant First Habit")
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.repository.setSettingSync("onboarding_completed", "BOOL", "true")
                                    },
                                    modifier = Modifier.testTag("onboarding_dismiss_btn")
                                ) {
                                    Text("Explore First")
                                }
                            }
                        }
                    }
                }
            }

            // 8-Second Guarded Undo Bar (D-209)
            if (activeUndoToken != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("undo_action_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent change recorded (8s undo window)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { viewModel.triggerUndo() },
                                modifier = Modifier.testTag("undo_action_btn")
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Undo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Comeback Card (14-day or 30-day inactivity return)
            if (comebackInfo != null) {
                item {
                    ComebackCard(
                        info = comebackInfo!!,
                        habits = activeHabits,
                        onApplyAdaptiveRestart = { resumeIds, pauseIds, lowered, reflection ->
                            viewModel.repository.applyComebackAdjustmentsSync(
                                resumeHabitIds = resumeIds,
                                pauseHabitIds = pauseIds,
                                loweredTargetByHabitId = lowered,
                                comebackReflectionBody = reflection,
                                saveReflectionImmediately = true
                            )
                        },
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

            // Growth Mark Summary (G2 + G3)
            item {
                GrowthMarkBadge(
                    metrics = growthMetrics,
                    referenceDate = today,
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
                    val tendedToday = scheduledTodayHabits.count { h ->
                        todayOutcomes.any { it.habitId == h.habitId } ||
                            todayEntries.any { it.habitId == h.habitId && it.valueUnits > 0 }
                    }
                    Text(
                        text = "$tendedToday of ${scheduledTodayHabits.size} tended",
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
                items(scheduledTodayHabits, key = { it.habitId }) { habit ->
                    val outcome = todayOutcomes.find { it.habitId == habit.habitId }
                    val habitEntriesForToday = todayEntries.filter { it.habitId == habit.habitId }
                    val dateNote = todayDateNotes.find { it.habitId == habit.habitId }
                    val retrospectiveEligible = remember(habit.habitId, outcomes, entries, appSettings) {
                        viewModel.repository.isHabitEligibleForRetrospectivePrompt(habit.habitId, today)
                    }

                    HabitCard(
                        habit = habit,
                        outcome = outcome,
                        entries = habitEntriesForToday,
                        dateNote = dateNote,
                        retrospectivePromptEligible = retrospectiveEligible,
                        onLogOutcome = { outKind, effort ->
                            viewModel.logHabit(habit, today, outKind, effort)
                        },
                        onLogQuantityOrDuration = { units, note ->
                            viewModel.logHabitQuantityOrDuration(habit, units, today, note)
                        },
                        onDeleteEntry = { entryId ->
                            viewModel.deleteHabitEntry(entryId)
                        },
                        onSaveDateNote = { body ->
                            viewModel.saveHabitDateNote(habit.habitId, today, body)
                        },
                        onOpenRetrospectiveJournal = {
                            viewModel.openJournalEditor(
                                entry = null,
                                forDate = today,
                                linkedHabitId = habit.habitId,
                                promptId = "prompt_retrospective"
                            )
                        },
                        onDismissRetrospectivePrompt = {
                            viewModel.repository.dismissRetrospectivePromptSync(habit.habitId, today)
                        },
                        onResetRecord = {
                            viewModel.removeRecord(habit.habitId, today)
                        },
                        onEditHabit = {
                            viewModel.openEditHabit(habit)
                        },
                        onArchiveHabit = {
                            viewModel.toggleHabitArchive(habit.habitId, !habit.isArchived)
                        },
                        onPauseResumeHabit = { newState ->
                            viewModel.updateHabitLifecycle(habit.habitId, newState)
                        }
                    )
                }
            }

            // Reflection Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                ReflectionCard(
                    entry = todayJournal,
                    draft = todayDraft,
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
                        "• Collected moments: Shows your monthly duration total across compatible time-tracked habits alongside your daily session rhythm.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "• Tiny & Partial Actions Count: Logging 1 unit or a tiny step is 100% valid effort.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "• No Streak Resets: Missed days never erase past progress or reset any counter to zero.",
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
