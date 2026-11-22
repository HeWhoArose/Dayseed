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
            assertNotNull("Primary Add Progress button must render ($theme)", findNodeByTagOrNull(activity, "open_record_session_h_pushups"))
            assertNotNull("Secondary View Details button must render ($theme)", findNodeByTagOrNull(activity, "habit_view_details_h_pushups"))

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
        assertNotNull("Edit Habit secondary CTA must be immediately visible", findNodeByTagOrNull(detailActivity, "detail_primary_edit_btn"))
        assertNotNull("Expandable Streak & Living Grove toggle must exist", findNodeByTagOrNull(detailActivity, "detail_toggle_streak_grove"))
        assertNotNull("Expandable Lifetime Progress toggle must exist", findNodeByTagOrNull(detailActivity, "detail_toggle_lifetime"))
        assertNotNull("End this habit CTA must remain directly accessible", findNodeByTagOrNull(detailActivity, "detail_end_habit_btn"))
        assertNotNull("Danger zone must visually separate permanent deletion", findNodeByTagOrNull(detailActivity, "habit_detail_danger_zone"))

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
        val overviewNode = findNodeByTagOrNull(insightsActivity, "insights_section_overview")
        assertNotNull("Monthly Calendar section must render at top of Insights", calNode)
        assertNotNull("Inline Selected-Day Activity Summary must render", daySummaryNode)
        assertNotNull("Overview metrics section must render", overviewNode)
        assertTrue(
            "Monthly calendar must appear above Overview metrics",
            calNode!!.boundsInRoot.top < overviewNode!!.boundsInRoot.top
        )
        assertNotNull(
            "Selected-day summary must show tappable habit row",
            findNodeByTagOrNull(insightsActivity, "insights_day_summary_habit_h_water")
        )
        assertNotNull(
            "Full Day Detail link must exist on selected-day summary",
            findNodeByTagOrNull(insightsActivity, "insights_open_selected_day_detail")
        )
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
}
