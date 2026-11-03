package com.example.dayseed.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.ui.components.CollectedMomentsSummaryCard
import com.example.dayseed.ui.components.ComebackCard
import com.example.dayseed.ui.components.DaypartArtwork
import com.example.dayseed.ui.components.DaypartBackgroundLayer
import com.example.dayseed.ui.components.HabitCard
import com.example.dayseed.ui.components.ReflectionCard
import com.example.dayseed.ui.components.TodayLongestStreakIndicator
import com.example.dayseed.ui.theme.DayseedDateStyle
import com.example.dayseed.ui.theme.DayseedGreetingStyle
import com.example.dayseed.ui.theme.SoftBotanicalThemeConfig
import com.example.dayseed.ui.theme.ThemeModeOption
import com.example.dayseed.ui.theme.greetingFor
import com.example.dayseed.ui.theme.resolveDayseedArtworkBlend
import kotlinx.coroutines.delay
import java.time.YearMonth
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
    val dateExceptions by viewModel.habitDateExceptions.collectAsState()
    val habitSummaries by viewModel.habitSummaries.collectAsState()
    val journals by viewModel.journalEntries.collectAsState()
    val drafts by viewModel.journalDrafts.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val comebackInfo by viewModel.comebackInfo.collectAsState()
    val growthMetrics by viewModel.growthMetrics.collectAsState()
    val activeUndoToken by viewModel.activeUndoToken.collectAsState()
    val notScheduledExpanded by viewModel.todayNotScheduledExpanded.collectAsState()

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
    val todayExceptions = remember(dateExceptions, todayStr) {
        dateExceptions.filter { it.localDate == todayStr }
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
    val scheduledTodayHabits = remember(activeHabits, habitSummaries, today) {
        activeHabits.filter { h ->
            habitSummaries[h.habitId]?.isScheduledToday ?: h.isScheduledFor(today)
        }
    }
    val notScheduledTodayHabits = remember(activeHabits, scheduledTodayHabits) {
        val scheduledIds = scheduledTodayHabits.map { it.habitId }.toSet()
        activeHabits.filter { it.habitId !in scheduledIds }
    }
    val longestActiveStreak = remember(habitSummaries, habits) {
        viewModel.selectLongestActiveRun()
    }

    val onboardingCompleted = remember(appSettings, habits) {
        val v = appSettings["onboarding_completed"]?.valueText
        v.equals("true", ignoreCase = true) || v == "1" || habits.isNotEmpty()
    }

    var showMetricInfoDialog by remember { mutableStateOf(false) }

    val themeMode = remember(appSettings) {
        ThemeModeOption.fromSetting(appSettings["theme_mode"]?.valueText)
    }
    val isSystemDark = isSystemInDarkTheme()
    val nowLocal by produceState(initialValue = viewModel.repository.clock.nowLocalTime(), key1 = today) {
        value = viewModel.repository.clock.nowLocalTime()
        while (true) {
            delay(30_000L)
            value = viewModel.repository.clock.nowLocalTime()
        }
    }
    val daypart = remember(nowLocal) {
        SoftBotanicalThemeConfig.resolveDaypart(nowLocal)
    }
    val artworkBlend = remember(themeMode, isSystemDark, nowLocal) {
        resolveDayseedArtworkBlend(themeMode, isSystemDark, nowLocal)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateEditHabit(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_habit_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add new habit")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            DaypartBackgroundLayer(
                artworkBlend = artworkBlend,
                modifier = Modifier.fillMaxSize()
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("today_screen_list"),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Element 1 & Element 2: Date, time-aware greeting, and Daypart Artwork (§5.1)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                                style = DayseedDateStyle,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = greetingFor(daypart),
                                style = DayseedGreetingStyle,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your progress is more than your streak.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        DaypartArtwork(
                            artwork = artworkBlend.primaryArtwork,
                            secondaryArtwork = artworkBlend.secondaryArtwork,
                            blendFraction = artworkBlend.blendFraction,
                            modifier = Modifier.size(width = 132.dp, height = 112.dp)
                        )
                    }
                }

                // Element 3: Longest Active Streak Indicator (§5.1)
                item {
                    TodayLongestStreakIndicator(
                        longestSelection = longestActiveStreak,
                        onOpenHabitDetail = { habitId ->
                            viewModel.openHabitDetail(habitId, today)
                        },
                        onOpenLivingGrove = { habitId ->
                            viewModel.openLivingGrove(selectedHabitId = habitId)
                        }
                    )
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
                                            viewModel.openCreateEditHabit(null)
                                        },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("onboarding_create_first_habit_btn")
                                    ) {
                                        Text("Plant First Habit")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.repository.setSettingSync("onboarding_completed", "BOOL", "true")
                                        },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("onboarding_dismiss_btn")
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
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("undo_action_btn")
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

                // Element 4: Today's Scheduled Habits Header & List (§5.1)
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

                if (scheduledTodayHabits.isEmpty()) {
                    item {
                        EmptyHabitsCard(onAddHabit = { viewModel.openCreateEditHabit(null) })
                    }
                } else {
                    items(scheduledTodayHabits, key = { it.habitId }) { habit ->
                        val outcome = todayOutcomes.find { it.habitId == habit.habitId }
                        val habitEntriesForToday = todayEntries.filter { it.habitId == habit.habitId }
                        val dateNote = todayDateNotes.find { it.habitId == habit.habitId }
                        val restDayException = todayExceptions.find { it.habitId == habit.habitId }
                        val summary = habitSummaries[habit.habitId]
                        val retrospectiveEligible = remember(habit.habitId, outcomes, entries, appSettings) {
                            viewModel.repository.isHabitEligibleForRetrospectivePrompt(habit.habitId, today)
                        }

                        HabitCard(
                            habit = habit,
                            outcome = outcome,
                            entries = habitEntriesForToday,
                            dateNote = dateNote,
                            restDayException = restDayException,
                            summary = summary,
                            retrospectivePromptEligible = retrospectiveEligible,
                            onOpenHabitDetail = {
                                viewModel.openHabitDetail(habit.habitId, today)
                            },
                            onOpenRecordSession = { entryId ->
                                viewModel.openRecordSession(habit.habitId, today, entryId)
                            },
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
                            onTakeRestDayToday = {
                                viewModel.takeRestDayToday(habit)
                            },
                            onClearRestDayToday = {
                                viewModel.clearRestDayToday(habit)
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

                // Element 5: Expandable "Not scheduled today" Section (§5.1)
                if (notScheduledTodayHabits.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("not_scheduled_today_section"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clickable {
                                            viewModel.setTodayNotScheduledExpanded(!notScheduledExpanded)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .testTag("toggle_not_scheduled_today_btn"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Not scheduled today (${notScheduledTodayHabits.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Unscheduled days are neutral — bonus sessions are always welcome.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = if (notScheduledExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (notScheduledExpanded) "Collapse not scheduled today" else "Expand not scheduled today",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (notScheduledExpanded) {
                        items(notScheduledTodayHabits, key = { "unsched_${it.habitId}" }) { habit ->
                            val outcome = todayOutcomes.find { it.habitId == habit.habitId }
                            val habitEntriesForToday = todayEntries.filter { it.habitId == habit.habitId }
                            val dateNote = todayDateNotes.find { it.habitId == habit.habitId }
                            val restDayException = todayExceptions.find { it.habitId == habit.habitId }
                            val summary = habitSummaries[habit.habitId]

                            HabitCard(
                                habit = habit,
                                outcome = outcome,
                                entries = habitEntriesForToday,
                                dateNote = dateNote,
                                restDayException = restDayException,
                                summary = summary,
                                retrospectivePromptEligible = false,
                                onOpenHabitDetail = {
                                    viewModel.openHabitDetail(habit.habitId, today)
                                },
                                onOpenRecordSession = { entryId ->
                                    viewModel.openRecordSession(habit.habitId, today, entryId)
                                },
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
                }

                // Element 6: Daily Reflection Card (§5.1)
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    ReflectionCard(
                        entry = todayJournal,
                        draft = todayDraft,
                        onOpenEditor = { viewModel.openJournalEditor(todayJournal, today) }
                    )
                }

                // Element 7: Collected Moments Summary Card (§5.1)
                item {
                    CollectedMomentsSummaryCard(
                        metrics = growthMetrics,
                        referenceDate = today,
                        onOpenDetail = {
                            viewModel.openCollectedMomentsDetail(
                                yearMonth = YearMonth.from(today),
                                selectedDate = today
                            )
                        },
                        onInfoClick = { showMetricInfoDialog = true }
                    )
                }

                // Bottom breathing room for FAB
                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
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
            modifier = Modifier.heightIn(min = 48.dp),
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
