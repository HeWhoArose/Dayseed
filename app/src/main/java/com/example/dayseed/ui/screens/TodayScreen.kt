package com.example.dayseed.ui.screens

import com.example.dayseed.ui.components.computeHabitProgress
import com.example.dayseed.ui.components.QuickLogSheetContent
import com.example.dayseed.ui.components.rememberCompletionUndoToken
import com.example.dayseed.ui.components.CompactHabitRow
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import com.example.dayseed.ui.components.dayseedHeading
import androidx.compose.ui.semantics.Role
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.NavTab
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitDateException
import com.example.dayseed.data.HabitDateExceptionKind
import com.example.dayseed.data.HabitDayOutcome
import com.example.dayseed.data.HabitEntry
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.UndoEntityType
import com.example.dayseed.data.HabitSummary
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.ui.components.CollectedMomentsSummaryCard
import com.example.dayseed.ui.components.ComebackCard
import com.example.dayseed.ui.components.CompactWeeklyCalendarStrip
import com.example.dayseed.ui.components.DaypartArtwork
import com.example.dayseed.ui.components.DayseedCard
import com.example.dayseed.ui.components.DayseedLogoSymbol
import com.example.dayseed.ui.components.DayseedShapes
import com.example.dayseed.ui.components.DayseedSpacing
import com.example.dayseed.ui.components.rememberIsLargeText
import com.example.dayseed.ui.components.TodayLongestStreakIndicator
import com.example.dayseed.ui.theme.DayseedDateStyle
import com.example.dayseed.ui.theme.DayseedGreetingStyle
import com.example.dayseed.ui.theme.LocalDayseedLiveTime
import com.example.dayseed.ui.theme.SoftBotanicalThemeConfig
import com.example.dayseed.ui.theme.ThemeModeOption
import com.example.dayseed.ui.theme.greetingFor
import com.example.dayseed.ui.theme.resolveDayseedArtworkBlend
import kotlinx.coroutines.delay
import java.math.BigInteger
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.DateTimeFormatter

fun isScheduledHabitTendedToday(
    habit: Habit,
    summary: HabitSummary?,
    todayOutcomes: List<HabitDayOutcome>,
    todayEntries: List<HabitEntry>,
    todayExceptions: List<HabitDateException>
): Boolean {
    val weekSatisfied = summary != null &&
        (summary.effectiveVersion.targetPeriod == TargetPeriod.WEEK ||
            summary.effectiveVersion.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET) &&
        summary.weekTargetUnits > BigInteger.ZERO &&
        summary.weekTotalUnits >= summary.weekTargetUnits
    val hasRestDayToday = todayExceptions.any {
        it.habitId == habit.habitId && it.exceptionKind == HabitDateExceptionKind.REST_DAY
    }
    return todayOutcomes.any { it.habitId == habit.habitId } ||
        todayEntries.any { it.habitId == habit.habitId && it.valueUnits > 0 } ||
        hasRestDayToday ||
        weekSatisfied
}

fun computeTendedTodayCount(
    scheduledTodayHabits: List<Habit>,
    habitSummaries: Map<String, HabitSummary>,
    todayOutcomes: List<HabitDayOutcome>,
    todayEntries: List<HabitEntry>,
    todayExceptions: List<HabitDateException>
): Int {
    return scheduledTodayHabits.count { h ->
        isScheduledHabitTendedToday(
            habit = h,
            summary = habitSummaries[h.habitId],
            todayOutcomes = todayOutcomes,
            todayEntries = todayEntries,
            todayExceptions = todayExceptions
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    val activeUndoToken = rememberCompletionUndoToken(viewModel)
    val notScheduledExpanded by viewModel.todayNotScheduledExpanded.collectAsState()
    val onboardingSetupExpanded by viewModel.onboardingSetupExpanded.collectAsState()

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
    val todayJournals = remember(journals, todayStr) {
        journals.filter { it.localDate == todayStr }
    }
    val todayJournal = remember(todayJournals) {
        todayJournals.firstOrNull()
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

    val context = LocalContext.current
    var notifPermissionTick by remember { mutableIntStateOf(0) }
    val onboardingPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        notifPermissionTick++
    }
    val hasPostNotificationsPermission = remember(context, notifPermissionTick) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
    val currentWeekStartDay = remember(appSettings, today) {
        val raw = appSettings["week_start"]?.valueText
        if (raw.equals("SUNDAY", ignoreCase = true) || raw == "7") {
            DayOfWeek.SUNDAY
        } else {
            DayOfWeek.MONDAY
        }
    }

    val onboardingCompleted = remember(appSettings, habits) {
        val v = appSettings["onboarding_completed"]?.valueText
        v.equals("true", ignoreCase = true) || v == "1" || habits.isNotEmpty()
    }

    // Which amount habit the quick-log bottom sheet is open for (null = closed).
    var quickLogHabitId by remember { mutableStateOf<String?>(null) }

    val hasShownColdLaunchScene by viewModel.hasShownColdLaunchScene.collectAsState()
    val themeMode = remember(appSettings) {
        ThemeModeOption.fromSetting(appSettings["theme_mode"]?.valueText)
    }
    val reducedMotion = remember(appSettings) { viewModel.isReducedMotionEnabled() }
    val isSystemDark = isSystemInDarkTheme()
    val nowLocal = LocalDayseedLiveTime.current
    val daypart = remember(nowLocal) {
        SoftBotanicalThemeConfig.resolveDaypart(nowLocal)
    }
    val artworkBlend = remember(themeMode, isSystemDark, nowLocal) {
        resolveDayseedArtworkBlend(themeMode, isSystemDark, nowLocal)
    }

    LaunchedEffect(hasShownColdLaunchScene, reducedMotion) {
        if (!hasShownColdLaunchScene) {
            delay(if (reducedMotion) 400L else 1200L)
            viewModel.markColdLaunchSceneShown()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (activeHabits.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { viewModel.openCreateEditHabit(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_habit_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add new habit")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // First-use scene: the whole Today surface is composed as one intentional, vertically
            // balanced scene. Once at least one habit exists this branch disappears and the normal
            // Today list takes over, so a new user never feels stuck inside onboarding.
            if (activeHabits.isEmpty()) {
                FirstUseScene(
                    onboardingCompleted = onboardingCompleted,
                    showSetupChecklist = onboardingSetupExpanded,
                    onToggleSetupChecklist = {
                        viewModel.setOnboardingSetupExpanded(!onboardingSetupExpanded)
                    },
                    currentWeekStartDay = currentWeekStartDay,
                    hasPostNotificationsPermission = hasPostNotificationsPermission,
                    onCreateFirstHabit = {
                        viewModel.markColdLaunchSceneShown()
                        viewModel.openCreateEditHabit(null)
                    },
                    onRestoreBackup = {
                        viewModel.markColdLaunchSceneShown()
                        viewModel.selectTab(NavTab.SETTINGS)
                    },
                    onToggleWeekStart = {
                        val nextDay = if (currentWeekStartDay == DayOfWeek.MONDAY) {
                            DayOfWeek.SUNDAY
                        } else {
                            DayOfWeek.MONDAY
                        }
                        viewModel.repository.setSettingSync("week_start", "ENUM", nextDay.name)
                    },
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            onboardingPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onExploreDayseed = {
                        viewModel.markColdLaunchSceneShown()
                        viewModel.repository.setSettingSync("onboarding_completed", "BOOL", "true")
                    },
                    dateLabel = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                    greeting = greetingFor(daypart),
                    showColdLaunchTag = !hasShownColdLaunchScene,
                    onUndo = if (activeUndoToken != null) {
                        { viewModel.triggerUndo() }
                    } else {
                        null
                    },
                    illustration = {
                        DaypartArtwork(
                            artwork = artworkBlend.primaryArtwork,
                            secondaryArtwork = artworkBlend.secondaryArtwork,
                            blendFraction = artworkBlend.blendFraction,
                            reducedMotion = reducedMotion,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                )
            } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("today_screen_list"),
                contentPadding = PaddingValues(
                    start = DayseedSpacing.ScreenHorizontal,
                    end = DayseedSpacing.ScreenHorizontal,
                    top = 6.dp,
                    bottom = DayseedSpacing.FabBottomClearance
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Element 1 & Element 2: Date, time-aware greeting, single canonical motto, and Daypart Artwork (§5.1)
                item {
                    val largeText = rememberIsLargeText()
                    // Narrow phones (<340dp) also drop the small corner artwork so the greeting stays on one line.
                    val narrowScreen = LocalConfiguration.current.screenWidthDp < 340
                    val showCornerArtwork = !largeText && !narrowScreen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (!hasShownColdLaunchScene) {
                                    Modifier.testTag("dayseed_cold_launch_scene")
                                } else {
                                    Modifier
                                }
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TodayHeaderText(
                            dateLabel = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                            greeting = greetingFor(daypart),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.openJournalEditor(todayJournal, today) },
                            modifier = Modifier.size(48.dp).testTag("today_write_reflection_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Write a reflection for today",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (showCornerArtwork) {
                            Spacer(modifier = Modifier.width(8.dp))
                            DaypartArtwork(
                                artwork = artworkBlend.primaryArtwork,
                                secondaryArtwork = artworkBlend.secondaryArtwork,
                                blendFraction = artworkBlend.blendFraction,
                                reducedMotion = reducedMotion,
                                modifier = Modifier.size(width = 84.dp, height = 66.dp)
                            )
                        }
                    }
                }

                // 8-Second Guarded Undo Bar (D-209) - shared with the first-run scene
                if (activeUndoToken != null) {
                    item {
                        UndoBanner(onUndo = { viewModel.triggerUndo() })
                    }
                }

                    // Section 2: Compact Daily Progress Summary Card (when scheduled habits exist today)
                    if (scheduledTodayHabits.isNotEmpty()) {
                        item {
                            val tendedToday = computeTendedTodayCount(
                                scheduledTodayHabits = scheduledTodayHabits,
                                habitSummaries = habitSummaries,
                                todayOutcomes = todayOutcomes,
                                todayEntries = todayEntries,
                                todayExceptions = todayExceptions
                            )
                            val totalScheduled = scheduledTodayHabits.size
                            val progressFraction = if (totalScheduled > 0) {
                                (tendedToday.toFloat() / totalScheduled.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            val encouragingLine = when {
                                tendedToday >= totalScheduled -> "All scheduled habits tended today."
                                tendedToday > 0 -> "Steady momentum — one step at a time."
                                else -> "Ready when you are — start small."
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("today_progress_summary_card"),
                                shape = DayseedShapes.Card,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Today's progress",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$tendedToday of $totalScheduled completed",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.testTag("today_progress_ratio_text")
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(DayseedShapes.ProgressTrack),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    )
                                    Text(
                                        text = encouragingLine,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                    CompactWeeklyCalendarStrip(
                                        referenceToday = today,
                                        selectedDate = today,
                                        weekStartDay = currentWeekStartDay,
                                        habits = activeHabits,
                                        evaluateDateStateForHabit = { habitId, date ->
                                            viewModel.repository.evaluateHonestCalendarDateState(habitId, date, today)
                                        },
                                        onSelectDate = { tappedDate -> viewModel.openDayDetail(tappedDate) },
                                        onOpenFullCalendar = {
                                            viewModel.markColdLaunchSceneShown()
                                            viewModel.selectTab(NavTab.INSIGHTS)
                                        },
                                        embedded = true,
                                        modifier = Modifier.testTag("today_weekly_calendar_strip")
                                    )
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

                    if (scheduledTodayHabits.isNotEmpty()) {
                        // Section 4: Today's Habits Header & List (§5.1)
                        item {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("today_seeds_section_header"),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Today's Habits",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.testTag("today_seeds_heading_text").dayseedHeading()
                                )
                            }
                        }

                        items(scheduledTodayHabits, key = { it.habitId }) { habit ->
                            val outcome = todayOutcomes.find { it.habitId == habit.habitId }
                            val habitEntriesForToday = todayEntries.filter { it.habitId == habit.habitId }
                            val dateNote = todayDateNotes.find { it.habitId == habit.habitId }
                            val restDayException = todayExceptions.find {
                                it.habitId == habit.habitId && it.exceptionKind == HabitDateExceptionKind.REST_DAY
                            }
                            val summary = habitSummaries[habit.habitId]
                            val retrospectiveEligible = remember(habit.habitId, outcomes, entries, appSettings) {
                                viewModel.repository.isHabitEligibleForRetrospectivePrompt(habit.habitId, today)
                            }

                            CompactHabitRow(
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
                                onOpenQuickLog = { quickLogHabitId = habit.habitId },
                                onLogOutcome = { outKind, effort ->
                                    viewModel.logHabit(habit, today, outKind, effort)
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
                                }
                            )
                        }
                    } else {
                        // Intentional Rest-Day State (habits exist in Dayseed, none scheduled today)
                        item {
                            EmptyHabitsCard(
                                onAddHabit = { viewModel.openCreateEditHabit(null) },
                                onViewHabits = {
                                    viewModel.markColdLaunchSceneShown()
                                    viewModel.selectTab(NavTab.HABITS)
                                }
                            )
                        }
                    }

                    // Expandable "Not scheduled today" Section (§5.1)
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
                                            .clickable(role = Role.Button) {
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
                                val restDayException = todayExceptions.find {
                                    it.habitId == habit.habitId && it.exceptionKind == HabitDateExceptionKind.REST_DAY
                                }
                                val summary = habitSummaries[habit.habitId]

                                CompactHabitRow(
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
                                    onOpenQuickLog = { quickLogHabitId = habit.habitId },
                                    onLogOutcome = { outKind, effort ->
                                        viewModel.logHabit(habit, today, outKind, effort)
                                    },
                                    onResetRecord = {
                                        viewModel.removeRecord(habit.habitId, today)
                                    }
                                )
                            }
                        }
                    }

                    // Bottom breathing room so final items scroll completely clear of the FAB
                    item {
                        Spacer(modifier = Modifier.height(96.dp))
                    }
            }
            }
        }
    }

    quickLogHabitId?.let { id ->
        val habit = habits.find { it.habitId == id }
        val progress = habit?.let { h ->
            computeHabitProgress(h, habitSummaries[id], todayEntries.filter { it.habitId == id })
        }
        if (habit == null || progress == null) {
            // The habit disappeared or is not an amount habit any more: close quietly.
            androidx.compose.runtime.LaunchedEffect(id) { quickLogHabitId = null }
        } else {
            val summary = habitSummaries[id]
            val hasRestDay = todayExceptions.any {
                it.habitId == id && it.exceptionKind == HabitDateExceptionKind.REST_DAY
            }
            ModalBottomSheet(
                onDismissRequest = { quickLogHabitId = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                QuickLogSheetContent(
                    habitName = habit.name,
                    progress = progress,
                    onComplete = { units ->
                        viewModel.logHabitQuantityOrDuration(habit, units, today, null)
                        quickLogHabitId = null
                    },
                    onLogAmount = { units ->
                        viewModel.logHabitQuantityOrDuration(habit, units, today, null)
                        quickLogHabitId = null
                    },
                    onTakeRestDay = if (summary?.canTakeRestDayToday == true && !hasRestDay) {
                        {
                            viewModel.takeRestDayToday(habit)
                            quickLogHabitId = null
                        }
                    } else null,
                    onOpenRecord = {
                        viewModel.openRecordSession(habit.habitId, today, null)
                        quickLogHabitId = null
                    }
                )
            }
        }
    }
}

/**
 * Date, greeting and tagline shared by BOTH Today states (normal list and first-run scene) so the
 * header cannot drift between them. The tagline may wrap to two lines instead of truncating.
 */
@Composable
private fun TodayHeaderText(
    dateLabel: String,
    greeting: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = dateLabel,
            style = DayseedDateStyle,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = greeting,
            style = DayseedGreetingStyle,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.dayseedHeading()
        )
        Text(
            text = "Your progress is more than your streak.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Short (5-second) undo banner for a just-registered completion (D-209, D-237), shared by both Today states. */
@Composable
private fun UndoBanner(
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite } // announce "Change recorded" without moving focus
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
                text = "Change recorded",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onUndo,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("undo_action_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Undo", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }
    }
}

/**
 * Intentional first-use scene shown only while the user has no active habits.
 *
 * The composition is deliberately not a single card floating at the top of an empty screen: the
 * identity block sits at the top, a larger botanical illustration claims the middle, and the
 * welcome actions anchor the bottom. On short screens the whole scene scrolls instead of clipping.
 */
@Composable
private fun FirstUseScene(
    onboardingCompleted: Boolean,
    showSetupChecklist: Boolean,
    onToggleSetupChecklist: () -> Unit,
    currentWeekStartDay: DayOfWeek,
    hasPostNotificationsPermission: Boolean,
    onCreateFirstHabit: () -> Unit,
    onRestoreBackup: () -> Unit,
    onToggleWeekStart: () -> Unit,
    onRequestNotifications: () -> Unit,
    onExploreDayseed: () -> Unit,
    dateLabel: String,
    greeting: String,
    showColdLaunchTag: Boolean,
    onUndo: (() -> Unit)?,
    illustration: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (showColdLaunchTag) {
                    Modifier.testTag("dayseed_cold_launch_scene")
                } else {
                    Modifier
                }
            )
    ) {
        val viewportHeight = maxHeight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewportHeight)
                    .padding(horizontal = DayseedSpacing.ScreenHorizontal, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TodayHeaderText(
                    dateLabel = dateLabel,
                    greeting = greeting,
                    modifier = Modifier.fillMaxWidth()
                )

                if (onUndo != null) {
                    UndoBanner(onUndo = onUndo, modifier = Modifier.padding(top = 2.dp))
                }

                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .size(width = 236.dp, height = 188.dp),
                    contentAlignment = Alignment.Center
                ) {
                    illustration()
                }

                FirstUseWelcomeCard(
                    onboardingCompleted = onboardingCompleted,
                    showSetupChecklist = showSetupChecklist,
                    onToggleSetupChecklist = onToggleSetupChecklist,
                    currentWeekStartDay = currentWeekStartDay,
                    hasPostNotificationsPermission = hasPostNotificationsPermission,
                    onCreateFirstHabit = onCreateFirstHabit,
                    onRestoreBackup = onRestoreBackup,
                    onToggleWeekStart = onToggleWeekStart,
                    onRequestNotifications = onRequestNotifications,
                    onExploreDayseed = onExploreDayseed
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FirstUseWelcomeCard(
    onboardingCompleted: Boolean,
    showSetupChecklist: Boolean,
    onToggleSetupChecklist: () -> Unit,
    currentWeekStartDay: DayOfWeek,
    hasPostNotificationsPermission: Boolean,
    onCreateFirstHabit: () -> Unit,
    onRestoreBackup: () -> Unit,
    onToggleWeekStart: () -> Unit,
    onRequestNotifications: () -> Unit,
    onExploreDayseed: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (!onboardingCompleted) "onboarding_welcome_card" else "today_empty_habits_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Eyebrow + hero heading. The time-aware greeting and the canonical motto belong to
            // the surrounding first-use scene, so they are intentionally not repeated here.
            Text(
                text = if (!onboardingCompleted) "Welcome to Dayseed" else "Your garden is waiting",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("onboarding_welcome_eyebrow")
            )
            Text(
                text = "Your garden starts here.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("onboarding_welcome_heading")
            )
            Text(
                text = "Choose one small thing you'd like to nurture in your everyday life.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onCreateFirstHabit,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("onboarding_create_first_habit_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Plant Your First Habit", fontWeight = FontWeight.SemiBold, maxLines = 1)
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextButton(
                    onClick = onToggleSetupChecklist,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("onboarding_setup_toggle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set Up Dayseed", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (showSetupChecklist) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (!onboardingCompleted) {
                    TextButton(
                        onClick = onExploreDayseed,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("onboarding_dismiss_btn")
                    ) {
                        Text("Explore Dayseed")
                    }
                }
            }

            Text(
                text = "Private by design · Offline · Yours",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("onboarding_reassurance_text")
            )

            if (showSetupChecklist) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_setup_checklist")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Week starts: ${if (currentWeekStartDay == DayOfWeek.MONDAY) "Monday" else "Sunday"} (tap to switch)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clickable(role = Role.Button, onClick = onToggleWeekStart)
                                    .padding(vertical = 4.dp)
                                    .testTag("onboarding_week_start_toggle_btn")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (hasPostNotificationsPermission) {
                                    "Alerts: Ready"
                                } else {
                                    "Alerts: Off"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("onboarding_notification_status_text")
                            )
                            if (Build.VERSION.SDK_INT >= 33 && !hasPostNotificationsPermission) {
                                OutlinedButton(
                                    onClick = onRequestNotifications,
                                    modifier = Modifier
                                        .heightIn(min = 40.dp)
                                        .testTag("onboarding_request_notifications_btn")
                                ) {
                                    Text("Allow")
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Moving from another device?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = onRestoreBackup,
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .testTag("onboarding_restore_backup_btn")
                            ) {
                                Text("Restore Backup")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyHabitsCard(
    onAddHabit: () -> Unit,
    onViewHabits: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_empty_habits_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DayseedLogoSymbol(
                    size = 32.dp,
                    contentDesc = null
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "No habits scheduled for today.",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enjoy your rest day, view your habits, or plant another habit anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onViewHabits != null) {
                    OutlinedButton(
                        onClick = onViewHabits,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("today_rest_day_view_habits_btn"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("View Habits")
                    }
                }
                Button(
                    onClick = onAddHabit,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("today_rest_day_create_habit_btn"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Create Another Habit")
                }
            }
        }
    }
}
