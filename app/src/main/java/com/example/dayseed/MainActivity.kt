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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Checklist
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.dayseed.ui.components.CollectedMomentsDetailSheet
import com.example.dayseed.ui.components.LivingGroveScreen
import com.example.dayseed.ui.screens.CreateEditHabitScreen
import com.example.dayseed.ui.screens.HabitDetailScreen
import com.example.dayseed.ui.screens.HabitsScreen
import com.example.dayseed.ui.screens.HistoryScreen
import com.example.dayseed.ui.screens.InsightsScreen
import com.example.dayseed.ui.screens.JournalEditorDialog
import com.example.dayseed.ui.screens.RecordSessionSheet
import com.example.dayseed.ui.screens.SettingsScreen
import com.example.dayseed.ui.screens.TodayScreen
import com.example.dayseed.ui.theme.DayseedTheme
import com.example.dayseed.ui.theme.ThemeModeOption
import kotlinx.coroutines.delay

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
    val currentSecondaryRoute by viewModel.currentSecondaryRoute.collectAsState()
    val isAddHabitOpen by viewModel.isAddHabitOpen.collectAsState()
    val editingHabit by viewModel.editingHabit.collectAsState()
    val isJournalOpen by viewModel.isJournalEditorOpen.collectAsState()
    val editingJournal by viewModel.editingJournalEntry.collectAsState()
    val journalEditorDate by viewModel.journalEditorDate.collectAsState()
    val journalInitialLinkedHabitId by viewModel.journalInitialLinkedHabitId.collectAsState()
    val journalInitialPromptId by viewModel.journalInitialPromptId.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val streakRuns by viewModel.streakRuns.collectAsState()
    val growthMetrics by viewModel.growthMetrics.collectAsState()
    val drafts by viewModel.journalDrafts.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    // BackHandler for secondary routes, dialogs, and non-Today primary tabs
    val shouldInterceptBack = currentSecondaryRoute != null ||
        isJournalOpen ||
        isAddHabitOpen ||
        selectedTab != NavTab.TODAY
    if (shouldInterceptBack) {
        BackHandler {
            viewModel.navigateBack()
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
                    selected = selectedTab == NavTab.TODAY && currentSecondaryRoute == null,
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
                    selected = selectedTab == NavTab.HABITS && currentSecondaryRoute == null,
                    onClick = { viewModel.selectTab(NavTab.HABITS) },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = "Habits") },
                    label = { Text("Habits") },
                    modifier = Modifier.testTag("nav_habits"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == NavTab.JOURNAL && currentSecondaryRoute == null,
                    onClick = { viewModel.selectTab(NavTab.JOURNAL) },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Journal") },
                    label = { Text("Journal") },
                    modifier = Modifier.testTag("nav_journal"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == NavTab.INSIGHTS && currentSecondaryRoute == null,
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
                    selected = selectedTab == NavTab.SETTINGS && currentSecondaryRoute == null,
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
            when (val route = currentSecondaryRoute) {
                is SecondaryRoute.HabitDetail -> {
                    HabitDetailScreen(
                        viewModel = viewModel,
                        habitId = route.habitId,
                        selectedLocalDate = route.selectedDate.toString(),
                        onSelectDate = { newDate ->
                            viewModel.selectDetailDate(route.habitId, newDate)
                        },
                        onOpenRecordSession = { hId, dateStr, entryId ->
                            viewModel.openRecordSession(hId, dateStr, entryId)
                        },
                        onOpenEditHabit = { hId ->
                            val targetHabit = habits.find { it.habitId == hId }
                            if (targetHabit != null) {
                                viewModel.openEditHabit(targetHabit)
                            }
                        },
                        onOpenLivingGrove = { hId ->
                            viewModel.openLivingGrove(hId)
                        },
                        onNavigateBack = { viewModel.navigateBack() }
                    )
                }

                is SecondaryRoute.CreateEditHabit -> {
                    CreateEditHabitScreen(
                        viewModel = viewModel,
                        habitId = route.habitId,
                        onNavigateBack = { viewModel.navigateBack() }
                    )
                }

                is SecondaryRoute.RecordSession -> {
                    RecordSessionSheet(
                        viewModel = viewModel,
                        habitId = route.habitId,
                        initialLocalDate = route.date.toString(),
                        entryId = route.entryId,
                        onNavigateBack = { viewModel.navigateBack() }
                    )
                }

                is SecondaryRoute.LivingGrove -> {
                    LivingGroveScreen(
                        viewModel = viewModel,
                        initialHabitId = route.selectedHabitId,
                        highlightedRunId = route.highlightedRunId,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                is SecondaryRoute.CollectedMomentsDetail -> {
                    CollectedMomentsDetailSheet(
                        metrics = growthMetrics,
                        yearMonth = route.yearMonth,
                        selectedDate = route.selectedDate,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                null -> {
                    when (selectedTab) {
                        NavTab.TODAY -> TodayScreen(viewModel = viewModel)
                        NavTab.HABITS -> HabitsScreen(viewModel = viewModel)
                        NavTab.JOURNAL -> HistoryScreen(viewModel = viewModel)
                        NavTab.INSIGHTS -> InsightsScreen(viewModel = viewModel)
                        NavTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
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
