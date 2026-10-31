package com.example.dayseed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.dayseed.ui.screens.AddEditHabitDialog
import com.example.dayseed.ui.screens.HistoryScreen
import com.example.dayseed.ui.screens.InsightsScreen
import com.example.dayseed.ui.screens.JournalEditorDialog
import com.example.dayseed.ui.screens.SettingsScreen
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import com.example.dayseed.ui.screens.TodayScreen
import com.example.dayseed.ui.theme.DayseedTheme
import com.example.dayseed.ui.theme.ThemeModeOption

class MainActivity : ComponentActivity() {
    private val viewModel: DayseedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val appSettings by viewModel.appSettings.collectAsState()
            val themeMode = remember(appSettings) {
                ThemeModeOption.fromSetting(appSettings["theme_mode"]?.valueText)
            }
            val liveLocalTime by produceState(initialValue = viewModel.repository.clock.nowLocalTime()) {
                while (true) {
                    delay(30_000L)
                    value = viewModel.repository.clock.nowLocalTime()
                }
            }
            DayseedTheme(
                themeMode = themeMode,
                localTime = liveLocalTime
            ) {
                DayseedApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DayseedApp(viewModel: DayseedViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isAddHabitOpen by viewModel.isAddHabitOpen.collectAsState()
    val editingHabit by viewModel.editingHabit.collectAsState()
    val isJournalOpen by viewModel.isJournalEditorOpen.collectAsState()
    val editingJournal by viewModel.editingJournalEntry.collectAsState()
    val journalEditorDate by viewModel.journalEditorDate.collectAsState()
    val journalInitialLinkedHabitId by viewModel.journalInitialLinkedHabitId.collectAsState()
    val journalInitialPromptId by viewModel.journalInitialPromptId.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val drafts by viewModel.journalDrafts.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    // Handle back button to return to Today tab
    if (selectedTab != NavTab.TODAY) {
        BackHandler {
            viewModel.selectTab(NavTab.TODAY)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = selectedTab == NavTab.TODAY,
                    onClick = { viewModel.selectTab(NavTab.TODAY) },
                    icon = { Icon(Icons.Default.Spa, contentDescription = "Today") },
                    label = { Text("Today") },
                    modifier = Modifier.testTag("nav_today"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == NavTab.HISTORY,
                    onClick = { viewModel.selectTab(NavTab.HISTORY) },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "History") },
                    label = { Text("History") },
                    modifier = Modifier.testTag("nav_history"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == NavTab.INSIGHTS,
                    onClick = { viewModel.selectTab(NavTab.INSIGHTS) },
                    icon = { Icon(Icons.Default.Eco, contentDescription = "Insights") },
                    label = { Text("Insights") },
                    modifier = Modifier.testTag("nav_insights"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == NavTab.SETTINGS,
                    onClick = { viewModel.selectTab(NavTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_settings"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavTab.TODAY -> TodayScreen(viewModel = viewModel)
                NavTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                NavTab.INSIGHTS -> InsightsScreen(viewModel = viewModel)
                NavTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }

    if (isAddHabitOpen) {
        val today = viewModel.repository.clock.todayLocal()
        val alreadyLogged = editingHabit?.let {
            viewModel.repository.hasQualifyingActivityOnDate(it.habitId, today)
        } ?: false
        val suggestedReminder = editingHabit?.let {
            viewModel.repository.getSuggestedReminderTimeForHabit(it.habitId)
        }

        AddEditHabitDialog(
            initialHabit = editingHabit,
            alreadyLoggedToday = alreadyLogged,
            suggestedReminderTime = suggestedReminder,
            onDismiss = { viewModel.closeAddEditHabit() },
            onSave = { habit, applyTodayWhenLogged ->
                viewModel.saveHabit(habit, applyTodayWhenLogged)
            },
            onDelete = { habitId ->
                viewModel.deleteHabit(habitId)
                viewModel.closeAddEditHabit()
            }
        )
    }

    if (isJournalOpen) {
        val activeDraft = drafts.find {
            it.localDate == journalEditorDate.toString() && it.sourceContext == "JOURNAL_COMPOSER"
        }
        JournalEditorDialog(
            initialEntry = editingJournal,
            initialDraft = activeDraft,
            selectedDate = journalEditorDate,
            habits = habits,
            initialLinkedHabitId = journalInitialLinkedHabitId,
            initialPromptId = journalInitialPromptId,
            onAutosaveDraft = { title, body, primaryMood, secondaryMood, promptId, linkedHabitId, date ->
                viewModel.repository.saveJournalDraftSync(
                    draftId = "active_draft_$date",
                    localDate = date,
                    title = title,
                    body = body,
                    primaryMoodCode = primaryMood,
                    secondaryMoodCode = secondaryMood,
                    promptId = promptId,
                    linkedHabitId = linkedHabitId,
                    sourceContext = "JOURNAL_COMPOSER"
                )
            },
            onDismiss = { viewModel.closeJournalEditor() },
            onSave = { entry -> viewModel.saveJournalEntry(entry) },
            onDelete = { id -> viewModel.deleteJournalEntry(id) }
        )
    }
}
