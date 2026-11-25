package com.example.dayseed

import android.app.Application
import android.content.Context
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitNaturalLanguageParser
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.screens.HabitDetailScreen
import com.example.dayseed.ui.screens.HabitsScreen
import com.example.dayseed.ui.screens.InsightsScreen
import com.example.dayseed.ui.screens.RecordSessionSheet
import com.example.dayseed.ui.screens.SettingsScreen
import com.example.dayseed.ui.theme.DayseedTheme
import com.example.dayseed.ui.theme.ThemeModeOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Verification suite for the Dayseed UI/UX Redesign and Navigation Improvement.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UiUxRedesignVerificationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var testClock: TestDayseedClock
    private val tempDbNames = mutableListOf<String>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        testClock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 11, 14),
            currentLocalTime = LocalTime.of(9, 30),
            currentUtcMs = LocalDate.of(2026, 11, 14)
                .atTime(9, 30)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        )
    }

    @After
    fun tearDown() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        for (name in tempDbNames) {
            context.deleteDatabase(name)
        }
        tempDbNames.clear()
    }

    private fun newRepository(dbName: String): DayseedRepository {
        context.deleteDatabase(dbName)
        tempDbNames.add(dbName)
        return DayseedRepository(context, clock = testClock, databaseName = dbName)
    }

    private fun newViewModel(repo: DayseedRepository): DayseedViewModel {
        return DayseedViewModel(application, customRepository = repo, ioDispatcher = Dispatchers.Unconfined)
    }

    private fun <T> kotlinx.coroutines.flow.StateFlow<T>.awaitMatch(
        timeoutMs: Long = 3_000L,
        predicate: (T) -> Boolean
    ): T {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val current = this.value
            if (predicate(current)) return current
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(25))
            Thread.sleep(10)
        }
        val finalVal = this.value
        assertTrue("StateFlow did not match predicate within ${timeoutMs}ms", predicate(finalVal))
        return finalVal
    }

    private fun findComposeView(view: View): androidx.compose.ui.platform.ComposeView? {
        if (view is androidx.compose.ui.platform.ComposeView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findComposeView(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun findRootForTest(view: View): RootForTest? {
        if (view is RootForTest) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findRootForTest(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun layoutAndIdle(
        activity: ComponentActivity,
        widthDp: Int = 360,
        heightDp: Int = 1200,
        recompose: Boolean = false
    ) {
        if (recompose) {
            findComposeView(activity.window.decorView)?.disposeComposition()
        }
        val density = activity.resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt()
        val heightPx = (heightDp * density).toInt()
        val root = activity.window.decorView
        repeat(3) {
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(80))
            root.measure(
                View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
            )
            root.layout(0, 0, widthPx, heightPx)
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(80))
        }
    }

    private fun collectNodes(activity: ComponentActivity): List<SemanticsNode> {
        val composeView = findComposeView(activity.window.decorView) ?: return emptyList()
        val rootForTest = findRootForTest(composeView) ?: return emptyList()
        val unmergedRoot = rootForTest.semanticsOwner.unmergedRootSemanticsNode
        val result = mutableListOf<SemanticsNode>()
        fun dfs(node: SemanticsNode) {
            result.add(node)
            node.children.forEach { dfs(it) }
        }
        dfs(unmergedRoot)
        return result
    }

    private fun findNodeByTagOrNull(activity: ComponentActivity, tag: String): SemanticsNode? {
        return collectNodes(activity).firstOrNull {
            it.config.getOrNull(SemanticsProperties.TestTag) == tag
        }
    }

    private fun collectAllTexts(activity: ComponentActivity): List<String> {
        val texts = mutableListOf<String>()
        for (node in collectNodes(activity)) {
            node.config.getOrNull(SemanticsProperties.Text)?.forEach { texts.add(it.text) }
        }
        return texts
    }

    private fun clickTag(activity: ComponentActivity, tag: String, widthDp: Int = 360, heightDp: Int = 1200) {
        layoutAndIdle(activity, widthDp, heightDp, recompose = false)
        val node = findNodeByTagOrNull(activity, tag)
        assertNotNull("Expected node with tag '$tag' to exist", node)
        val action = node!!.config.getOrNull(SemanticsActions.OnClick)
        assertNotNull("Expected node '$tag' to be clickable", action)
        action!!.action?.invoke()
        layoutAndIdle(activity, widthDp, heightDp, recompose = false)
    }

    // =========================================================================
    // 1. Natural-Language Habit Naming Fix
    // =========================================================================

    @Test
    fun naturalLanguageParser_preservesMeaningfulDisplayNamesAndTimeHints() {
        val pushupsDirect = HabitNaturalLanguageParser.parse("10 push-ups daily")
        assertEquals("10 Push-ups", pushupsDirect.displayName)
        assertEquals(TrackingMode.QUANTITY, pushupsDirect.trackingMode)
        assertEquals(10, pushupsDirect.targetValue)
        assertEquals("push-ups", pushupsDirect.unitCode)
        assertEquals(ScheduleKind.DAILY, pushupsDirect.scheduleKind)

        val pushupsWithDo = HabitNaturalLanguageParser.parse("Do 10 push-ups daily")
        assertEquals("10 Push-ups", pushupsWithDo.displayName)
        assertFalse("Activity name must never degrade to bare 'Do'", pushupsWithDo.activityName.equals("Do", ignoreCase = true))
        assertEquals(10, pushupsWithDo.targetValue)

        val readEvening = HabitNaturalLanguageParser.parse("Read 20 pages every evening")
        assertEquals("Read 20 Pages", readEvening.displayName)
        assertEquals("Evening", readEvening.preferredTimeHint)
        assertEquals(20, readEvening.targetValue)
        assertEquals("pages", readEvening.unitCode)
        assertEquals(ScheduleKind.DAILY, readEvening.scheduleKind)

        val walkDuration = HabitNaturalLanguageParser.parse("Walk for 30 minutes")
        assertEquals("Walk 30 Minutes", walkDuration.displayName)
        assertEquals(TrackingMode.DURATION, walkDuration.trackingMode)
        assertEquals(30, walkDuration.targetValue)

        val waterCups = HabitNaturalLanguageParser.parse("Drink 4 cups of water daily")
        assertEquals("Drink 4 Cups of Water", waterCups.displayName)
        assertEquals(4, waterCups.targetValue)
        assertEquals("cups", waterCups.unitCode)
    }

    // =========================================================================
    // 2. Today Screen: Daily Summary, Weekly Strip, Card Action Hierarchy & Dark Mode
    // =========================================================================

    @Test
    fun todayScreen_showsProgressSummaryWeeklyStripAndClearCardHierarchyInLightAndDark() {
        val dbName = "test_redesign_today_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)
        val today = testClock.todayLocal()

        val qtyHabit = Habit(
            habitId = "h_pushups",
            name = "10 Push-ups",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "push-ups",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 10,
            scheduleKind = ScheduleKind.DAILY
        )
        runBlocking { repo.saveHabit(qtyHabit) }
        repo.habits.awaitMatch { it.any { h -> h.habitId == "h_pushups" } }

        for (theme in listOf(ThemeModeOption.LIGHT, ThemeModeOption.DARK)) {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).create().start().resume().visible()
            val activity = controller.get()
            activity.setContent {
                DayseedTheme(themeMode = theme, reducedMotion = true) {
                    DayseedApp(viewModel = vm)
                }
            }
            layoutAndIdle(activity, widthDp = 360, heightDp = 1200)

            assertNotNull("Daily progress summary card must render ($theme)", findNodeByTagOrNull(activity, "today_progress_summary_card"))
            assertNotNull("Compact 7-day weekly calendar strip must render ($theme)", findNodeByTagOrNull(activity, "today_weekly_calendar_strip"))
            assertNotNull("Compact row round action must render ($theme)", findNodeByTagOrNull(activity, "quick_log_btn_h_pushups") ?: findNodeByTagOrNull(activity, "habit_done_btn_h_pushups"))
            assertNotNull("Compact row detail opener must render ($theme)", findNodeByTagOrNull(activity, "open_habit_detail_h_pushups"))

            // Bottom navigation must be visible on primary Today tab
            assertNotNull("Bottom nav must be visible on Today tab ($theme)", findNodeByTagOrNull(activity, "nav_today"))

            // Opening Habit Detail must hide bottom navigation bar for distraction-free focus
            vm.openHabitDetail("h_pushups", today)
            layoutAndIdle(activity, widthDp = 360, heightDp = 1200, recompose = true)
            assertNotNull("Habit Detail screen must open ($theme)", findNodeByTagOrNull(activity, "habit_detail_screen"))
            assertNull("Bottom nav must be hidden on secondary Habit Detail route ($theme)", findNodeByTagOrNull(activity, "nav_today"))

            vm.navigateBack()
            layoutAndIdle(activity, widthDp = 360, heightDp = 1200, recompose = true)
            assertNotNull("Bottom nav must return when navigating back to Today ($theme)", findNodeByTagOrNull(activity, "nav_today"))

            controller.pause().stop()
        }
        repo.dbHelper.close()
    }

    // =========================================================================
    // 3. Habits Screen: 'Your Habits', Secondary Grove CTA & Overflow Reorder
    // =========================================================================

    @Test
    fun habitsScreen_displaysYourHabitsHeaderAndMovesReorderIntoOverflowMenu() {
        val dbName = "test_redesign_habits_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)

        runBlocking {
            repo.saveHabit(Habit(habitId = "h1", name = "10 Push-ups", sortOrder = 0))
            repo.saveHabit(Habit(habitId = "h2", name = "Read 20 Pages", sortOrder = 1))
        }
        repo.habits.awaitMatch { it.size == 2 }

        val controller = Robolectric.buildActivity(ComponentActivity::class.java).create().start().resume().visible()
        val activity = controller.get()
        activity.setContent {
            DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) {
                HabitsScreen(viewModel = vm)
            }
        }
        layoutAndIdle(activity, widthDp = 360, heightDp = 1000)

        val texts = collectAllTexts(activity)
        assertTrue("Habits header must say 'Your Habits'", texts.any { it == "Your Habits" })
        assertNotNull("Living Grove secondary button must remain accessible", findNodeByTagOrNull(activity, "habits_open_grove_btn"))

        // Reorder icons should NOT clutter the card surface directly before opening the overflow menu
        assertNull("Reorder up should be inside overflow menu, not cluttering card", findNodeByTagOrNull(activity, "reorder_up_h2"))
        assertNotNull("Single overflow menu button must exist on card", findNodeByTagOrNull(activity, "habits_card_menu_h2"))

        controller.pause().stop()
        repo.dbHelper.close()
    }

    // =========================================================================
    // 4. Habit Detail & Record Session Redesign
    // =========================================================================

    @Test
    fun habitDetailAndRecordSession_exposeImmediateActionsExpandableSectionsAndDatePicker() {
        val dbName = "test_redesign_detail_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)
        val today = testClock.todayLocal()

        val habit = Habit(
            habitId = "h_read",
            name = "Read 20 Pages",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20,
            scheduleKind = ScheduleKind.DAILY
        )
        runBlocking { repo.saveHabit(habit) }
        repo.habits.awaitMatch { it.any { h -> h.habitId == "h_read" } }

        // A) Verify HabitDetailScreen layout & expandable cards
        val detailController = Robolectric.buildActivity(ComponentActivity::class.java).create().start().resume().visible()
        val detailActivity = detailController.get()
        detailActivity.setContent {
            DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) {
                HabitDetailScreen(
                    viewModel = vm,
                    habitId = "h_read",
                    selectedLocalDate = today.toString(),
                    onSelectDate = {},
                    onOpenRecordSession = { _, _, _ -> },
                    onOpenEditHabit = {},
                    onOpenLivingGrove = {},
                    onNavigateBack = {}
                )
            }
        }
        layoutAndIdle(detailActivity, widthDp = 360, heightDp = 1800)

        assertNotNull("Add Progress CTA must be immediately visible in Today's Progress card", findNodeByTagOrNull(detailActivity, "detail_record_session_btn"))
        assertNotNull("Edit lives once, in the header", findNodeByTagOrNull(detailActivity, "detail_edit_habit_btn"))
        assertNull("No second Edit button inside the progress card (D-240)", findNodeByTagOrNull(detailActivity, "detail_primary_edit_btn"))
        assertNull("No third Edit button inside Lifecycle (D-240)", findNodeByTagOrNull(detailActivity, "detail_lifecycle_edit_btn"))
        assertNotNull("Expandable Streak & Living Grove toggle must exist", findNodeByTagOrNull(detailActivity, "detail_toggle_streak_grove"))
        assertNotNull("Expandable Lifetime Progress toggle must exist", findNodeByTagOrNull(detailActivity, "detail_toggle_lifetime"))
        assertNotNull("End this habit CTA must remain directly accessible", findNodeByTagOrNull(detailActivity, "detail_end_habit_btn"))
        assertNotNull("Deleting a habit is a trash icon in the top bar that asks first (D-248)", findNodeByTagOrNull(detailActivity, "detail_delete_habit_btn"))
        assertNull("The big Delete button card is gone", findNodeByTagOrNull(detailActivity, "habit_detail_danger_zone"))
        assertNull("The note card is gone (D-248)", findNodeByTagOrNull(detailActivity, "habit_detail_note_input"))

        // The end-date editor uses the same mode + unit + stepper controls as New Habit (D-241).
        clickTag(detailActivity, "detail_edit_end_date_btn", widthDp = 360, heightDp = 2600)
        assertNotNull("End-date editor must open", findNodeByTagOrNull(detailActivity, "detail_end_date_editor"))
        clickTag(detailActivity, "detail_duration_mode_SET", widthDp = 360, heightDp = 2600)
        assertNotNull("Unit selector must appear for a set time", findNodeByTagOrNull(detailActivity, "detail_duration_kind_WEEKS"))
        assertNotNull("Stepper must appear for a set time", findNodeByTagOrNull(detailActivity, "detail_duration_plus_btn"))
        assertNotNull("Typed number field must remain", findNodeByTagOrNull(detailActivity, "detail_duration_value_input"))

        // Expand Streak & Living Grove and verify inner content appears
        assertNull("Streak details should be collapsed by default", findNodeByTagOrNull(detailActivity, "detail_open_grove_btn"))
        clickTag(detailActivity, "detail_toggle_streak_grove", widthDp = 360, heightDp = 2000)
        assertNotNull("Streak details should appear when expanded", findNodeByTagOrNull(detailActivity, "detail_open_grove_btn"))

        detailController.pause().stop()

        // B) Verify RecordSessionSheet exposes DatePicker button alongside recent chips
        val recordController = Robolectric.buildActivity(ComponentActivity::class.java).create().start().resume().visible()
        val recordActivity = recordController.get()
        recordActivity.setContent {
            DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) {
                RecordSessionSheet(
                    viewModel = vm,
                    habitId = "h_read",
                    initialLocalDate = today.toString(),
                    onNavigateBack = {}
                )
            }
        }
        layoutAndIdle(recordActivity, widthDp = 360, heightDp = 1200)
        assertNotNull("Choose date button for Material DatePickerDialog must render", findNodeByTagOrNull(recordActivity, "record_session_pick_date_btn"))

        recordController.pause().stop()
        repo.dbHelper.close()
    }

    // =========================================================================
    // 5. Insights Calendar-First + Selected Day Summary & Settings Categories
    // =========================================================================

    @Test
    fun insightsAndSettings_renderCalendarFirstSummaryAndRefinedGroupingsAt320dp() {
        val dbName = "test_redesign_insights_settings_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)
        val today = testClock.todayLocal()

        val habit = Habit(
            habitId = "h_water",
            name = "Drink 4 Cups Of Water",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "cups",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 4,
            scheduleKind = ScheduleKind.DAILY
        )
        runBlocking { repo.saveHabit(habit) }
        repo.habits.awaitMatch { it.any { h -> h.habitId == "h_water" } }
        vm.logHabitQuantityOrDuration(habit, 2, today)
        repo.habitEntries.awaitMatch { it.isNotEmpty() }

        // A) Insights Screen at narrow 320dp width
        val insightsController = Robolectric.buildActivity(ComponentActivity::class.java).create().start().resume().visible()
        val insightsActivity = insightsController.get()
        insightsActivity.setContent {
            DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) {
                InsightsScreen(viewModel = vm)
            }
        }
        layoutAndIdle(insightsActivity, widthDp = 320, heightDp = 1400)

        val calNode = findNodeByTagOrNull(insightsActivity, "insights_section_calendar")
        val daySummaryNode = findNodeByTagOrNull(insightsActivity, "insights_selected_day_summary")
        assertNotNull("Monthly Calendar section must render at top of Insights", calNode)
        assertNotNull("Inline Selected-Day Activity Summary must render", daySummaryNode)
        assertNotNull("Weekly board must render in Calendar mode", findNodeByTagOrNull(insightsActivity, "insights_section_habits"))
        assertNull("Statistics are the secondary mode, not shown with the calendar (D-244)", findNodeByTagOrNull(insightsActivity, "insights_section_overview"))
        assertNotNull(
            "Selected-day summary must show tappable habit row",
            findNodeByTagOrNull(insightsActivity, "insights_day_summary_habit_h_water")
        )
        assertNotNull(
            "Full Day Detail link must exist on selected-day summary",
            findNodeByTagOrNull(insightsActivity, "insights_open_selected_day_detail")
        )

        // Switching to Stats shows the statistics and hides the calendar.
        clickTag(insightsActivity, "insights_mode_stats", widthDp = 320, heightDp = 2600)
        layoutAndIdle(insightsActivity, widthDp = 320, heightDp = 2600)
        assertNotNull("Overview metrics section must render in Stats mode", findNodeByTagOrNull(insightsActivity, "insights_section_overview"))
        assertNotNull("Mood card must render in Stats mode", findNodeByTagOrNull(insightsActivity, "mood_insights_card"))
        assertNull("Calendar is hidden in Stats mode", findNodeByTagOrNull(insightsActivity, "insights_section_calendar"))
        insightsController.pause().stop()

        // B) Settings Screen refined headings
        val settingsController = Robolectric.buildActivity(ComponentActivity::class.java).create().start().resume().visible()
        val settingsActivity = settingsController.get()
        settingsActivity.setContent {
            DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) {
                SettingsScreen(viewModel = vm)
            }
        }
        layoutAndIdle(settingsActivity, widthDp = 360, heightDp = 1800)

        val settingsTexts = collectAllTexts(settingsActivity)
        assertTrue(settingsTexts.any { it == "Appearance & personalisation" })
        assertTrue(settingsTexts.any { it == "Habits & reminders" })
        assertTrue(settingsTexts.any { it == "Data, backup & journal" })
        assertTrue(settingsTexts.any { it == "Privacy & about" })
        settingsController.pause().stop()

        repo.dbHelper.close()
    }

    @Test
    fun todayUndoBanner_appearsOnlyForRegisteredCompletions() {
        val repo = newRepository("test_undo_banner_${System.nanoTime()}.db")
        val vm = newViewModel(repo)
        val today = testClock.todayLocal()
        fun habit(id: String) = Habit(
            habitId = id, name = id, trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.DAY, targetValue = 1, scheduleKind = ScheduleKind.DAILY
        )
        runBlocking { repo.saveHabit(habit("h_keep")); repo.saveHabit(habit("h_drop")) }
        repo.habits.awaitMatch { it.size >= 2 }

        val controller = Robolectric.buildActivity(ComponentActivity::class.java).create().start().resume().visible()
        val activity = controller.get()
        activity.setContent { DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) { DayseedApp(viewModel = vm) } }
        layoutAndIdle(activity, widthDp = 360, heightDp = 1200)
        assertNull("No banner before anything is registered", findNodeByTagOrNull(activity, "undo_action_banner"))

        // Deleting a habit raises an undo token in the data layer, but Today must not show a banner for it.
        repo.deleteHabitWithUndoSync("h_drop")
        layoutAndIdle(activity, widthDp = 360, heightDp = 1200, recompose = true)
        assertNull("Habit changes must not raise the Today undo banner", findNodeByTagOrNull(activity, "undo_action_banner"))

        // Registering a completion does.
        repo.logHabitDayOutcomeSync("h_keep", today, "COMPLETED")
        layoutAndIdle(activity, widthDp = 360, heightDp = 1200, recompose = true)
        assertNotNull("A registered completion must offer Undo", findNodeByTagOrNull(activity, "undo_action_btn"))

        controller.pause().stop()
        repo.dbHelper.close()
    }

    @Test
    fun newlyCreatedHabits_showTheRightRoundControlStraightAway() {
        val repo = newRepository("test_new_habit_controls_${System.nanoTime()}.db")
        val vm = newViewModel(repo)
        runBlocking {
            repo.saveHabit(Habit(habitId = "h_qty", name = "10 Push-ups", trackingMode = TrackingMode.QUANTITY, unitCode = "push-ups",
                targetPeriod = TargetPeriod.DAY, targetValue = 10, scheduleKind = ScheduleKind.DAILY))
            repo.saveHabit(Habit(habitId = "h_dur", name = "Walk", trackingMode = TrackingMode.DURATION, unitCode = "min",
                targetPeriod = TargetPeriod.DAY, targetValue = 30, scheduleKind = ScheduleKind.DAILY))
            repo.saveHabit(Habit(habitId = "h_chk", name = "Meditate", trackingMode = TrackingMode.COMPLETION_ONLY,
                targetPeriod = TargetPeriod.DAY, targetValue = 1, scheduleKind = ScheduleKind.DAILY))
        }
        repo.habits.awaitMatch { it.size >= 3 }
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).create().start().resume().visible()
        val activity = controller.get()
        activity.setContent { DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) { DayseedApp(viewModel = vm) } }
        layoutAndIdle(activity, widthDp = 360, heightDp = 1400)
        assertNotNull("A new quantity habit gets the + that opens the quick-log pop-up", findNodeByTagOrNull(activity, "quick_log_btn_h_qty"))
        assertNotNull("A new duration habit gets the + too", findNodeByTagOrNull(activity, "quick_log_btn_h_dur"))
        assertNotNull("A check-off habit has nothing to measure, so it keeps the one-tap circle", findNodeByTagOrNull(activity, "habit_done_btn_h_chk"))
        controller.pause().stop()
        repo.dbHelper.close()
    }

    private fun setTextTag(activity: ComponentActivity, tag: String, text: String, widthDp: Int = 360, heightDp: Int = 1600) {
        layoutAndIdle(activity, widthDp, heightDp, recompose = false)
        val node = findNodeByTagOrNull(activity, tag)
        assertNotNull("Expected node with tag '$tag'", node)
        node!!.config.getOrNull(SemanticsActions.SetText)!!.action!!.invoke(androidx.compose.ui.text.AnnotatedString(text))
        layoutAndIdle(activity, widthDp, heightDp, recompose = false)
        layoutAndIdle(activity, widthDp, heightDp + 1, recompose = false)
    }

    /** What "Here's what we understood" shows must be exactly what gets saved, and Today must then offer the right control. */
    @Test
    fun understoodSummaryMatchesTheSavedHabit_andTodayOffersThePlusForAmountHabits() {
        val repo = newRepository("test_understood_${System.nanoTime()}.db")
        val vm = newViewModel(repo)
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).create().start().resume().visible()
        val activity = controller.get()
        activity.setContent { DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) { DayseedApp(viewModel = vm) } }
        layoutAndIdle(activity, widthDp = 360, heightDp = 1600)

        data class Case(val typed: String, val name: String, val mode: TrackingMode, val target: Int?, val unit: String?, val summaryHas: List<String>)
        val cases = listOf(
            Case("Do 10 push-ups daily", "10 Push-ups", TrackingMode.QUANTITY, 10, "push-ups", listOf("Quantity", "10 push-ups", "Daily")),
            Case("Walk for 30 minutes", "Walk 30 Minutes", TrackingMode.DURATION, 30, "min", listOf("Duration", "30 min")),
            Case("Meditate daily", "Meditate", TrackingMode.COMPLETION_ONLY, null, null, listOf("Simple check-off", "Complete once"))
        )
        for ((index, c) in cases.withIndex()) {
            vm.openAddHabit()
            layoutAndIdle(activity, widthDp = 360, heightDp = 1600, recompose = true)
            setTextTag(activity, "habit_name_input", c.typed)
            // 1. The summary card describes what was understood
            val texts = collectAllTexts(activity)
            assertTrue("Summary card shown for '${c.typed}'", texts.any { it.contains("Here's what we understood") })
            c.summaryHas.forEach { piece ->
                assertTrue("Summary for '${c.typed}' must mention '$piece', got: ${texts.filter { it.length < 60 }}", texts.any { it.contains(piece) })
            }
            // 2. Saving stores exactly that
            clickTag(activity, "save_habit_btn", heightDp = 1600)
            repo.habits.awaitMatch { it.size == index + 1 }
            val saved = repo.habits.value.first { it.name == c.name }
            assertEquals("mode for '${c.typed}'", c.mode, saved.trackingMode)
            assertEquals("target for '${c.typed}'", c.target, saved.targetValue)
            assertEquals("unit for '${c.typed}'", c.unit, saved.unitCode)
            assertEquals("Every day by default", ScheduleKind.DAILY, saved.scheduleKind)
            layoutAndIdle(activity, widthDp = 360, heightDp = 1600, recompose = true)
        }
        // 3. Today: amount habits get the + straight away; check-off keeps the circle
        vm.selectTab(NavTab.TODAY)
        layoutAndIdle(activity, widthDp = 360, heightDp = 1600, recompose = true)
        val byName = repo.habits.value.associateBy { it.name }
        assertNotNull("+ for the push-ups habit", findNodeByTagOrNull(activity, "quick_log_btn_${byName.getValue("10 Push-ups").habitId}"))
        assertNotNull("+ for the walking habit", findNodeByTagOrNull(activity, "quick_log_btn_${byName.getValue("Walk 30 Minutes").habitId}"))
        assertNotNull("circle for the check-off habit", findNodeByTagOrNull(activity, "habit_done_btn_${byName.getValue("Meditate").habitId}"))
        controller.pause().stop()
        repo.dbHelper.close()
    }
}
