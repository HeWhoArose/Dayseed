package com.example.dayseed

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitMeasurementKind
import com.example.dayseed.data.HabitNaturalLanguageParser
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.UnitFormatter
import com.example.dayseed.data.WeekdayMask
import com.example.dayseed.ui.screens.CreateEditHabitScreen
import com.example.dayseed.ui.screens.RecordSessionSheet
import com.example.dayseed.ui.theme.DayseedTheme
import com.example.dayseed.ui.theme.ThemeModeOption
import kotlinx.coroutines.Dispatchers
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
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Comprehensive verification suite for Progressive Disclosure, Smart Habit Creation,
 * Custom Unit Support, Incremental Progress Recording, and Backward Compatibility.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ProgressiveDisclosureAndSmartHabitTest {

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
        // Saturday 2026-11-14 09:30 AM
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
        assertTrue("StateFlow did not match predicate within ${timeoutMs}ms (last value=$finalVal)", predicate(finalVal))
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

    private fun pumpActivity(
        activity: ComponentActivity,
        widthDp: Int = 360,
        heightDp: Int = 1800,
        recompose: Boolean = false
    ) {
        if (recompose) {
            findComposeView(activity.window.decorView)?.disposeComposition()
        }
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(60))
        val density = activity.resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt()
        val heightPx = (heightDp * density).toInt()
        val decor = activity.window.decorView
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        )
        decor.layout(0, 0, widthPx, heightPx)
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(60))
    }

    private fun collectAllNodes(activity: ComponentActivity): List<SemanticsNode> {
        val root = findRootForTest(activity.window.decorView)
        assertNotNull("AndroidComposeView RootForTest must be attached", root)
        val result = mutableListOf<SemanticsNode>()
        fun walk(node: SemanticsNode) {
            result.add(node)
            for (child in node.children) {
                walk(child)
            }
        }
        walk(root!!.semanticsOwner.unmergedRootSemanticsNode)
        return result
    }

    private fun findNodeByTagOrNull(activity: ComponentActivity, tag: String): SemanticsNode? {
        return collectAllNodes(activity).firstOrNull {
            it.config.getOrNull(SemanticsProperties.TestTag) == tag
        }
    }

    private fun findNodeByTag(activity: ComponentActivity, tag: String): SemanticsNode {
        val node = findNodeByTagOrNull(activity, tag)
        assertNotNull("Expected SemanticsNode with testTag '$tag'", node)
        return node!!
    }

    private fun clickTag(activity: ComponentActivity, tag: String, widthDp: Int = 360, heightDp: Int = 1800) {
        val node = findNodeByTag(activity, tag)
        val clickAction = node.config.getOrNull(SemanticsActions.OnClick)?.action
        assertNotNull("Expected OnClick action on node '$tag'", clickAction)
        clickAction!!.invoke()
        pumpActivity(activity, widthDp = widthDp, heightDp = heightDp, recompose = false)
    }

    private fun setTextTag(activity: ComponentActivity, tag: String, text: String, widthDp: Int = 360, heightDp: Int = 1800) {
        val node = findNodeByTag(activity, tag)
        val setTextAction = node.config.getOrNull(SemanticsActions.SetText)?.action
        assertNotNull("Expected SetText action on node '$tag'", setTextAction)
        setTextAction!!.invoke(AnnotatedString(text))
        pumpActivity(activity, widthDp = widthDp, heightDp = heightDp, recompose = false)
    }

    private fun collectAllRenderedTexts(activity: ComponentActivity): List<String> {
        val out = mutableListOf<String>()
        for (node in collectAllNodes(activity)) {
            val textList = node.config.getOrNull(SemanticsProperties.Text)
            if (textList != null) {
                for (item in textList) {
                    out.add(item.text)
                }
            }
            val editableText = node.config.getOrNull(SemanticsProperties.EditableText)
            if (editableText != null) {
                out.add(editableText.text)
            }
        }
        return out
    }

    private fun saveScreenshotPng(activity: ComponentActivity, fileName: String, widthDp: Int = 360, heightDp: Int = 800) {
        pumpActivity(activity, widthDp = widthDp, heightDp = heightDp, recompose = false)
        val decor = activity.window.decorView
        val bitmap = Bitmap.createBitmap(decor.width.coerceAtLeast(1), decor.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        decor.draw(canvas)
        val candidateDirs = listOf(
            File("../docs/evidence"),
            File("docs/evidence")
        )
        val outDir = candidateDirs.firstOrNull { it.exists() || it.parentFile?.exists() == true } ?: File("docs/evidence")
        outDir.mkdirs()
        val outFile = File(outDir, fileName)
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
    }

    // =========================================================================
    // 1. Offline Deterministic Natural-Language Habit Interpretation & Edge Cases
    // =========================================================================

    @Test
    fun offlineNaturalLanguageParser_interpretsAllEightCanonicalExamplesAndEdgeCases() {
        // Example 1: Read 10 pages of a book
        val ex1 = HabitNaturalLanguageParser.parse("Read 10 pages of a book")
        assertTrue("Activity should start with Read (actual=${ex1.activityName})", ex1.activityName.startsWith("Read"))
        assertEquals(HabitMeasurementKind.QUANTITY, ex1.measurementKind)
        assertEquals(TrackingMode.QUANTITY, ex1.trackingMode)
        assertEquals(10, ex1.targetValue)
        assertEquals("pages", ex1.unitCode)
        assertEquals(ScheduleKind.DAILY, ex1.scheduleKind)
        assertEquals(TargetPeriod.DAY, ex1.targetPeriod)
        assertFalse(ex1.needsUserConfirmation)

        // Example 2: Plant 10 seeds
        val ex2 = HabitNaturalLanguageParser.parse("Plant 10 seeds")
        assertEquals("Plant", ex2.activityName)
        assertEquals(HabitMeasurementKind.QUANTITY, ex2.measurementKind)
        assertEquals(TrackingMode.QUANTITY, ex2.trackingMode)
        assertEquals(10, ex2.targetValue)
        assertEquals("seeds", ex2.unitCode)
        assertEquals(ScheduleKind.DAILY, ex2.scheduleKind)

        // Example 3: Sell 5 handmade bags
        val ex3 = HabitNaturalLanguageParser.parse("Sell 5 handmade bags")
        assertEquals("Sell", ex3.activityName)
        assertEquals(HabitMeasurementKind.QUANTITY, ex3.measurementKind)
        assertEquals(TrackingMode.QUANTITY, ex3.trackingMode)
        assertEquals(5, ex3.targetValue)
        assertEquals("handmade bags", ex3.unitCode)
        assertEquals(ScheduleKind.DAILY, ex3.scheduleKind)

        // Example 4: Feed 3 animals
        val ex4 = HabitNaturalLanguageParser.parse("Feed 3 animals")
        assertEquals("Feed", ex4.activityName)
        assertEquals(HabitMeasurementKind.QUANTITY, ex4.measurementKind)
        assertEquals(TrackingMode.QUANTITY, ex4.trackingMode)
        assertEquals(3, ex4.targetValue)
        assertEquals("animals", ex4.unitCode)
        assertEquals(ScheduleKind.DAILY, ex4.scheduleKind)

        // Example 5: Write 500 words
        val ex5 = HabitNaturalLanguageParser.parse("Write 500 words")
        assertEquals("Write", ex5.activityName)
        assertEquals(HabitMeasurementKind.QUANTITY, ex5.measurementKind)
        assertEquals(TrackingMode.QUANTITY, ex5.trackingMode)
        assertEquals(500, ex5.targetValue)
        assertEquals("words", ex5.unitCode)
        assertEquals(ScheduleKind.DAILY, ex5.scheduleKind)

        // Example 6: Save ₦5,000
        val ex6 = HabitNaturalLanguageParser.parse("Save ₦5,000")
        assertEquals("Save", ex6.activityName)
        assertEquals(HabitMeasurementKind.AMOUNT_CURRENCY, ex6.measurementKind)
        assertEquals(TrackingMode.QUANTITY, ex6.trackingMode)
        assertEquals(5000, ex6.targetValue)
        assertEquals("naira", ex6.unitCode)
        assertEquals("₦", ex6.currencySymbol)
        assertEquals(ScheduleKind.DAILY, ex6.scheduleKind)

        // Example 7: Walk for 30 minutes
        val ex7 = HabitNaturalLanguageParser.parse("Walk for 30 minutes")
        assertEquals("Walk", ex7.activityName)
        assertEquals(HabitMeasurementKind.DURATION, ex7.measurementKind)
        assertEquals(TrackingMode.DURATION, ex7.trackingMode)
        assertEquals(30, ex7.targetValue)
        assertEquals("min", ex7.unitCode)
        assertEquals(ScheduleKind.DAILY, ex7.scheduleKind)

        // Example 8: Meditate daily
        val ex8 = HabitNaturalLanguageParser.parse("Meditate daily")
        assertEquals("Meditate", ex8.activityName)
        assertEquals(HabitMeasurementKind.SIMPLE_CHECK_OFF, ex8.measurementKind)
        assertEquals(TrackingMode.COMPLETION_ONLY, ex8.trackingMode)
        assertNull(ex8.targetValue)
        assertNull(ex8.unitCode)
        assertEquals(ScheduleKind.DAILY, ex8.scheduleKind)

        // Edge Case A: Input with no number -> Simple check-off
        val noNum = HabitNaturalLanguageParser.parse("Practice gratitude before sleep")
        assertEquals("Practice gratitude before sleep", noNum.activityName)
        assertEquals(HabitMeasurementKind.SIMPLE_CHECK_OFF, noNum.measurementKind)
        assertEquals(TrackingMode.COMPLETION_ONLY, noNum.trackingMode)
        assertNull(noNum.targetValue)

        // Edge Case B: Input with multiple numbers -> Ambiguity flagged
        val twoTargets = HabitNaturalLanguageParser.parse("Do 2 sets of 15 squats")
        assertTrue("Multiple target numbers should flag needsUserConfirmation", twoTargets.needsUserConfirmation)
        assertNotNull(twoTargets.ambiguityReason)

        // Edge Case C: Input with decimal values
        val decHours = HabitNaturalLanguageParser.parse("Study for 1.5 hours")
        assertEquals(HabitMeasurementKind.DURATION, decHours.measurementKind)
        assertEquals(90, decHours.targetValue)
        assertEquals("min", decHours.unitCode)

        val decQuantity = HabitNaturalLanguageParser.parse("Drink 2.5 liters of water")
        assertEquals(HabitMeasurementKind.QUANTITY, decQuantity.measurementKind)
        assertEquals(3, decQuantity.targetValue)
        assertTrue(decQuantity.needsUserConfirmation)
        assertNotNull(decQuantity.ambiguityReason)

        // Edge Case D: Currency units in symbols and words
        val usdHabit = HabitNaturalLanguageParser.parse("Invest $250 every week")
        assertEquals(HabitMeasurementKind.AMOUNT_CURRENCY, usdHabit.measurementKind)
        assertEquals(250, usdHabit.targetValue)
        assertEquals("dollars", usdHabit.unitCode)
        assertEquals(TargetPeriod.WEEK, usdHabit.targetPeriod)

        // Edge Case E: Unknown arbitrary custom units
        val customUnit = HabitNaturalLanguageParser.parse("Knit 4 coaster patterns")
        assertEquals("Knit", customUnit.activityName)
        assertEquals(HabitMeasurementKind.QUANTITY, customUnit.measurementKind)
        assertEquals(4, customUnit.targetValue)
        assertEquals("coaster patterns", customUnit.unitCode)

        // Edge Case F: Empty or whitespace input
        val emptyInput = HabitNaturalLanguageParser.parse("   ")
        assertEquals("", emptyInput.activityName)
        assertEquals(HabitMeasurementKind.SIMPLE_CHECK_OFF, emptyInput.measurementKind)
        assertNull(emptyInput.targetValue)

        // Singular / plural natural unit formatting
        assertEquals("1 page", UnitFormatter.formatValueWithUnit(1, "pages"))
        assertEquals("2 pages", UnitFormatter.formatValueWithUnit(2, "pages"))
        assertEquals("1 handmade bag", UnitFormatter.formatValueWithUnit(1, "handmade bags"))
        assertEquals("5 handmade bags", UnitFormatter.formatValueWithUnit(5, "handmade bags"))
        assertEquals("₦2,000 / ₦5,000 naira", UnitFormatter.formatProgressRatio(2000, 5000, "naira"))
    }

    // =========================================================================
    // 2. Today Screen Progressive Disclosure (0 Habits, Rest Day, Active Habits)
    // =========================================================================

    @Test
    fun todayScreen_progressiveDisclosureHidesEmptySectionsAndRevealsAsUserProgresses() {
        val dbName = "test_today_progressive_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)

        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            DayseedTheme(
                themeMode = ThemeModeOption.SOFT_BOTANICAL_AUTO,
                localTime = testClock.nowLocalTime(),
                reducedMotion = false
            ) {
                DayseedApp(
                    viewModel = vm,
                    liveLocalTime = testClock.nowLocalTime()
                )
            }
        }
        pumpActivity(activity, widthDp = 360, heightDp = 840)

        // A. Fresh install with 0 habits:
        //    - Shows Greeting + single motto ("Your progress is more than your streak.")
        //    - Shows onboarding_welcome_card with Create Your First Habit, Set Up Dayseed, Explore Dayseed
        //    - Hides Longest Streak, Today's Seeds (0 of 0 tended), Daily Reflection, Collected Moments, and FAB
        assertNotNull(findNodeByTagOrNull(activity, "onboarding_welcome_card"))
        assertNotNull(findNodeByTagOrNull(activity, "onboarding_create_first_habit_btn"))
        assertNotNull(findNodeByTagOrNull(activity, "onboarding_setup_toggle_btn"))
        assertNotNull(findNodeByTagOrNull(activity, "onboarding_dismiss_btn"))

        assertNull("TodayLongestStreakIndicator must be hidden when 0 habits exist", findNodeByTagOrNull(activity, "today_main_streak_indicator"))
        assertNull("Today's Seeds header must be hidden when 0 habits exist", findNodeByTagOrNull(activity, "today_seeds_section_header"))
        assertNull("0 of 0 tended text must be hidden when 0 habits exist", findNodeByTagOrNull(activity, "today_tended_count_text"))
        assertNull("ReflectionCard must be hidden when 0 habits exist", findNodeByTagOrNull(activity, "reflection_card"))
        assertNull("CollectedMomentsSummaryCard must be hidden when 0 habits exist", findNodeByTagOrNull(activity, "growth_mark_summary_card"))
        assertNull("Floating action button must be hidden when 0 habits exist", findNodeByTagOrNull(activity, "add_habit_fab"))

        val zeroHabitTexts = collectAllRenderedTexts(activity)
        val mottoOccurrences = zeroHabitTexts.count { it.contains("Your progress is more than your streak.") }
        assertEquals("Motto must appear only ONCE on TodayScreen", 1, mottoOccurrences)

        saveScreenshotPng(activity, "01_today_first_use_progressive_disclosure.png", widthDp = 360, heightDp = 800)

        // B. Tapping "Explore Dayseed" dismisses onboarding_completed -> shows today_empty_habits_card
        //    while still hiding premature sections when 0 habits exist
        clickTag(activity, "onboarding_dismiss_btn", widthDp = 360, heightDp = 840)
        pumpActivity(activity, widthDp = 360, heightDp = 840, recompose = true)
        assertEquals("true", repo.getSettingString("onboarding_completed", "false"))
        assertNotNull(findNodeByTagOrNull(activity, "today_empty_habits_card"))
        assertNull(findNodeByTagOrNull(activity, "reflection_card"))
        assertNull(findNodeByTagOrNull(activity, "growth_mark_summary_card"))

        // C. When user has 1 habit that is NOT scheduled for today (Saturday 2026-11-14, habit scheduled Monday only):
        //    Shows rest-day card ("No habits scheduled for today.") with View Habits and Create Another Habit
        val today = testClock.todayLocal() // Saturday
        val mondayMask = WeekdayMask.toMask(setOf(DayOfWeek.MONDAY))
        repo.saveHabitSync(
            Habit(
                habitId = "h_monday_only",
                name = "Weekly Planning",
                description = "",
                trackingMode = TrackingMode.COMPLETION_ONLY,
                targetPeriod = TargetPeriod.DAY,
                targetValue = 1,
                scheduleKind = ScheduleKind.SELECTED_WEEKDAYS,
                scheduledWeekdaysMask = mondayMask,
                colorHex = "#286143"
            ),
            effectiveDateOverride = today.minusDays(5)
        )
        pumpActivity(activity, widthDp = 360, heightDp = 840, recompose = true)

        assertNotNull("Rest-day card must be visible when habits exist but none scheduled today", findNodeByTagOrNull(activity, "today_empty_habits_card"))
        assertNotNull(findNodeByTagOrNull(activity, "today_rest_day_view_habits_btn"))
        assertNotNull(findNodeByTagOrNull(activity, "today_rest_day_create_habit_btn"))
        assertTrue(collectAllRenderedTexts(activity).any { it.contains("No habits scheduled for today.") })
        // Compact reflection prompt is now available because user has at least 1 active habit
        assertNotNull("Compact ReflectionCard should appear once user has at least 1 active habit", findNodeByTagOrNull(activity, "reflection_card"))
        assertTrue(collectAllRenderedTexts(activity).any { it.contains("Write Today's Reflection") })
        // Collected moments and longest streak remain hidden because no activity has been recorded yet
        assertNull("CollectedMomentsSummaryCard must stay hidden when 0 activity recorded", findNodeByTagOrNull(activity, "growth_mark_summary_card"))
        assertNull("TodayLongestStreakIndicator must stay hidden when no active streak exists", findNodeByTagOrNull(activity, "today_main_streak_indicator"))

        // D. Add a habit scheduled for today -> Today's Seeds section appears
        repo.saveHabitSync(
            Habit(
                habitId = "h_read_pages",
                name = "Read a book",
                description = "",
                trackingMode = TrackingMode.QUANTITY,
                unitCode = "pages",
                targetPeriod = TargetPeriod.DAY,
                targetValue = 10,
                scheduleKind = ScheduleKind.DAILY,
                colorHex = "#286143"
            ),
            effectiveDateOverride = today
        )
        pumpActivity(activity, widthDp = 360, heightDp = 1100, recompose = true)

        assertNotNull(findNodeByTagOrNull(activity, "today_seeds_section_header"))
        assertNotNull(findNodeByTagOrNull(activity, "habit_card_h_read_pages"))
        assertNull("CollectedMomentsSummaryCard still hidden before first progress entry", findNodeByTagOrNull(activity, "growth_mark_summary_card"))

        // E. Record progress -> CollectedMomentsSummaryCard and TodayLongestStreakIndicator appear
        repo.logHabitEntrySync("h_read_pages", today, 10, "Chapter 1")
        pumpActivity(activity, widthDp = 360, heightDp = 2400, recompose = true)

        assertNotNull("TodayLongestStreakIndicator appears after habit is tended", findNodeByTagOrNull(activity, "today_main_streak_indicator"))
        assertNotNull("CollectedMomentsSummaryCard appears after meaningful progress is recorded", findNodeByTagOrNull(activity, "growth_mark_summary_card"))

        saveScreenshotPng(activity, "04_today_active_habits_and_progress.png", widthDp = 360, heightDp = 900)

        controller.pause().stop().destroy()
        repo.dbHelper.close()
    }

    // =========================================================================
    // 3. Smart Habit Creation UI, Live Summary Card, User Override & Progressive Options
    // =========================================================================

    @Test
    fun createEditHabitScreen_parsesNaturalLanguageShowsLiveSummarySupportsOverrideAndCollapsesAdvancedOptions() {
        val dbName = "test_smart_habit_ui_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)

        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            DayseedTheme {
                CreateEditHabitScreen(
                    viewModel = vm,
                    habitId = null,
                    onNavigateBack = {}
                )
            }
        }
        pumpActivity(activity, widthDp = 360, heightDp = 1100)

        // 1. Advanced options ("More Options") are collapsed by default on new habit creation
        assertNotNull(findNodeByTagOrNull(activity, "habit_name_input"))
        assertNotNull(findNodeByTagOrNull(activity, "smart_habit_interpretation_card"))
        assertNotNull(findNodeByTagOrNull(activity, "habit_advanced_options_toggle"))
        assertNull("Category input must be hidden while More Options is collapsed", findNodeByTagOrNull(activity, "habit_category_input"))
        assertNull("Lifetime goal input must be hidden while More Options is collapsed", findNodeByTagOrNull(activity, "habit_lifetime_goal_input"))
        assertNull("Reminder time controls must be hidden while Gentle Reminder toggle is off", findNodeByTagOrNull(activity, "habit_reminder_hour12_input"))

        // 2. Type natural-language habit: "Read 10 pages of a book"
        setTextTag(activity, "habit_name_input", "Read 10 pages of a book", widthDp = 360, heightDp = 1100)

        val textsAfterRead = collectAllRenderedTexts(activity)
        assertTrue("Summary card must show 'Here's what we understood:'", textsAfterRead.any { it.contains("Here's what we understood:") })
        assertTrue("Summary must show interpreted activity 'Read'", textsAfterRead.any { it.startsWith("Read") })
        assertTrue("Summary must show Quantity measurement type", textsAfterRead.any { it.contains("Quantity") })
        assertTrue("Summary must show target '10 pages'", textsAfterRead.any { it.contains("10 pages") })

        saveScreenshotPng(activity, "02_smart_habit_creation_interpreted.png", widthDp = 360, heightDp = 950)

        // 3. Test custom unit & user override: type "Sell 5 handmade bags", then override target to 8
        setTextTag(activity, "habit_name_input", "Sell 5 handmade bags", widthDp = 360, heightDp = 1100)
        val textsAfterBags = collectAllRenderedTexts(activity)
        assertTrue(textsAfterBags.any { it.contains("handmade bags") })

        // Override target value from 5 to 8 and verify summary reflects user override
        setTextTag(activity, "habit_target_value_input", "8", widthDp = 360, heightDp = 1100)
        val textsAfterOverride = collectAllRenderedTexts(activity)
        assertTrue("Summary target must reflect user-overridden target '8 handmade bags'", textsAfterOverride.any { it.contains("8 handmade bags") })

        // 4. Expand "More Options" and verify advanced fields become accessible
        clickTag(activity, "habit_advanced_options_toggle", widthDp = 360, heightDp = 1400)
        assertNotNull(findNodeByTagOrNull(activity, "habit_desc_input"))
        assertNotNull(findNodeByTagOrNull(activity, "habit_category_input"))
        assertNotNull(findNodeByTagOrNull(activity, "habit_lifetime_goal_input"))

        // 5. Save the habit and verify persisted Habit model
        clickTag(activity, "save_habit_btn", widthDp = 360, heightDp = 1400)
        repo.habits.awaitMatch { list -> list.any { it.name == "Sell" } }
        val savedHabit = repo.habits.value.first { it.name == "Sell" }
        assertEquals(TrackingMode.QUANTITY, savedHabit.trackingMode)
        assertEquals(8, savedHabit.targetValue)
        assertEquals("handmade bags", savedHabit.unitCode)
        assertEquals(ScheduleKind.DAILY, savedHabit.scheduleKind)

        controller.pause().stop().destroy()
        repo.dbHelper.close()
    }

    // =========================================================================
    // 4. Incremental Progress Recording, Custom Units, Edit/Delete & Backup Compatibility
    // =========================================================================

    @Test
    fun incrementalProgressRecording_accumulatesCustomAndCurrencyUnitsSupportsEditDeleteAndBackupRoundTrip() {
        val dbName = "test_incremental_progress_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)
        val today = testClock.todayLocal()

        // Create 3 habits from the prompt:
        // 1) Read 10 pages
        val readHabit = Habit(
            habitId = "h_pages",
            name = "Read a book",
            description = "",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 10,
            scheduleKind = ScheduleKind.DAILY,
            colorHex = "#286143"
        )
        // 2) Plant 10 seeds
        val seedsHabit = Habit(
            habitId = "h_seeds",
            name = "Plant",
            description = "",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "seeds",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 10,
            scheduleKind = ScheduleKind.DAILY,
            colorHex = "#3E6B48"
        )
        // 3) Save ₦5,000
        val saveHabit = Habit(
            habitId = "h_naira",
            name = "Save",
            description = "",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "naira",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 5000,
            scheduleKind = ScheduleKind.DAILY,
            colorHex = "#5A7247"
        )

        repo.saveHabitSync(readHabit, effectiveDateOverride = today)
        repo.saveHabitSync(seedsHabit, effectiveDateOverride = today)
        repo.saveHabitSync(saveHabit, effectiveDateOverride = today)

        // Scenario A: Record 2 pages, 3 pages, 1 page -> 6 / 10 pages (60%)
        val (e1, _) = repo.logHabitEntrySync("h_pages", today, 2, "Morning pages")
        val (e2, _) = repo.logHabitEntrySync("h_pages", today, 3, "Lunch break")
        val (e3, _) = repo.logHabitEntrySync("h_pages", today, 1, "Evening page")

        // Scenario B: Record 2 seeds, 3 seeds, 4 seeds -> 9 / 10 seeds (90%)
        repo.logHabitEntrySync("h_seeds", today, 2, null)
        repo.logHabitEntrySync("h_seeds", today, 3, null)
        repo.logHabitEntrySync("h_seeds", today, 4, null)

        // Scenario C: Record ₦2,000 + ₦3,000 -> ₦5,000 / ₦5,000 naira (100%)
        repo.logHabitEntrySync("h_naira", today, 2000, "Transfer 1")
        repo.logHabitEntrySync("h_naira", today, 3000, "Transfer 2")

        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            DayseedTheme {
                RecordSessionSheet(
                    viewModel = vm,
                    habitId = "h_pages",
                    initialLocalDate = today.toString(),
                    onNavigateBack = {}
                )
            }
        }
        pumpActivity(activity, widthDp = 360, heightDp = 1100)

        // Verify RecordSessionSheet shows "Add Progress", preview total "6 / 10 pages (60%)", and existing entries list
        val sheetTexts = collectAllRenderedTexts(activity)
        assertTrue("RecordSessionSheet must title 'Add Progress'", sheetTexts.any { it.contains("Add Progress") })
        assertTrue("RecordSessionSheet must show '6 / 10 pages (60%)'", sheetTexts.any { it.contains("6 / 10 pages (60%)") })
        assertNotNull("Existing entries card must be visible", findNodeByTagOrNull(activity, "record_session_existing_entries_card"))
        assertNotNull(findNodeByTagOrNull(activity, "record_session_edit_entry_${e1.entryId}"))
        assertNotNull(findNodeByTagOrNull(activity, "record_session_delete_entry_${e3.entryId}"))

        saveScreenshotPng(activity, "03_record_session_incremental_progress.png", widthDp = 360, heightDp = 950)

        // Delete the 1-page entry via the inline Delete button -> total becomes 5 / 10 pages (50%)
        clickTag(activity, "record_session_delete_entry_${e3.entryId}", widthDp = 360, heightDp = 1100)
        repo.habitEntries.awaitMatch { list -> list.none { it.entryId == e3.entryId } }
        pumpActivity(activity, widthDp = 360, heightDp = 1100, recompose = true)
        val textsAfterDelete = collectAllRenderedTexts(activity)
        assertTrue("After deleting 1-page entry, total must update to 5 / 10 pages (50%)", textsAfterDelete.any { it.contains("5 / 10 pages (50%)") })

        // Edit the 3-page entry (e2) to 5 pages -> total becomes 7 / 10 pages (70%)
        clickTag(activity, "record_session_edit_entry_${e2.entryId}", widthDp = 360, heightDp = 1100)
        setTextTag(activity, "record_session_value_input", "5", widthDp = 360, heightDp = 1100)
        clickTag(activity, "record_session_save_btn", widthDp = 360, heightDp = 1100)
        repo.habitEntries.awaitMatch { list -> list.firstOrNull { it.entryId == e2.entryId }?.valueUnits == 5 }

        val pagesTotal = repo.habitEntries.value
            .filter { it.habitId == "h_pages" && it.localDate == today.toString() }
            .sumOf { it.valueUnits }
        assertEquals(7, pagesTotal)

        val seedsTotal = repo.habitEntries.value
            .filter { it.habitId == "h_seeds" && it.localDate == today.toString() }
            .sumOf { it.valueUnits }
        assertEquals(9, seedsTotal)

        val nairaTotal = repo.habitEntries.value
            .filter { it.habitId == "h_naira" && it.localDate == today.toString() }
            .sumOf { it.valueUnits }
        assertEquals(5000, nairaTotal)

        // Verify portable JSON backup & restore preserves custom units ("pages", "seeds", "naira") and all entries
        val exportedJson = repo.exportPortableBackupSync(password = null)
        val restoredDbName = "test_incremental_restored_${System.nanoTime()}.db"
        val restoredRepo = newRepository(restoredDbName)
        restoredRepo.restorePortableBackupSync(exportedJson, restoreMode = "REPLACE", password = null)

        val restoredHabitsById = restoredRepo.habits.value.associateBy { it.habitId }
        assertEquals("pages", restoredHabitsById["h_pages"]?.unitCode)
        assertEquals("seeds", restoredHabitsById["h_seeds"]?.unitCode)
        assertEquals("naira", restoredHabitsById["h_naira"]?.unitCode)

        val restoredNairaTotal = restoredRepo.habitEntries.value
            .filter { it.habitId == "h_naira" && it.localDate == today.toString() }
            .sumOf { it.valueUnits }
        assertEquals(5000, restoredNairaTotal)

        controller.pause().stop().destroy()
        repo.dbHelper.close()
        restoredRepo.dbHelper.close()
    }
}
