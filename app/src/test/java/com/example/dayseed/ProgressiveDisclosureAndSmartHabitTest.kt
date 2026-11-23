package com.example.dayseed

import com.example.dayseed.ui.components.friendlyDate
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
import com.example.dayseed.ui.screens.defaultRecordSessionValueText
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
 * Fractional & Custom Unit Support, Incremental Progress Recording, Safe Input Defaults,
 * Multi-Width Layout Verification (320dp, 360dp, 411dp), and Backward Compatibility.
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
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
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

    private fun isNodeEnabled(activity: ComponentActivity, tag: String): Boolean {
        val node = findNodeByTag(activity, tag)
        return SemanticsProperties.Disabled !in node.config
    }

    private fun getEditableText(activity: ComponentActivity, tag: String): String {
        val node = findNodeByTag(activity, tag)
        return node.config.getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()
    }

    private fun clickTag(activity: ComponentActivity, tag: String, widthDp: Int = 360, heightDp: Int = 1800) {
        // Lay out at the requested viewport first: a LazyColumn only composes what fits, so a
        // control below the fold at the previous size is not findable yet.
        pumpActivity(activity, widthDp = widthDp, heightDp = heightDp, recompose = false)
        val node = findNodeByTag(activity, tag)
        val clickAction = node.config.getOrNull(SemanticsActions.OnClick)?.action
        assertNotNull("Expected OnClick action on node '$tag'", clickAction)
        clickAction!!.invoke()
        pumpActivity(activity, widthDp = widthDp, heightDp = heightDp, recompose = false)
    }

    private fun applySetText(activity: ComponentActivity, tag: String, text: String) {
        val node = findNodeByTag(activity, tag)
        val setTextAction = node.config.getOrNull(SemanticsActions.SetText)?.action
        assertNotNull("Expected SetText action on node '$tag'", setTextAction)
        setTextAction!!.invoke(AnnotatedString(text))
    }

    private fun setTextTag(activity: ComponentActivity, tag: String, text: String, widthDp: Int = 360, heightDp: Int = 1800) {
        applySetText(activity, tag, text)
        pumpActivity(activity, widthDp = widthDp, heightDp = heightDp, recompose = false)
        // A second settle pass lets derived state (validation, summary, previews) recompose before
        // the caller asserts on it. Requesting focus here would leak an IME session into the next
        // test in this class, so text entry deliberately never takes focus.
        pumpActivity(activity, widthDp = widthDp, heightDp = heightDp, recompose = false)

        if (getEditableText(activity, tag) != text) {
            val insertAction = findNodeByTag(activity, tag)
                .config.getOrNull(SemanticsActions.InsertTextAtCursor)?.action
            if (insertAction != null) {
                insertAction.invoke(AnnotatedString(text))
                pumpActivity(activity, widthDp = widthDp, heightDp = heightDp, recompose = false)
                pumpActivity(activity, widthDp = widthDp, heightDp = heightDp, recompose = false)
            }
        }
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
    // 1. Offline Deterministic Natural-Language Habit Interpretation & Decimal Precision
    // =========================================================================

    @Test
    fun offlineNaturalLanguageParser_interpretsAllCanonicalExamplesFractionalUnitsAndMinorCurrencies() {
        // Example 1: Read 10 pages of a book (whole-item count)
        val ex1 = HabitNaturalLanguageParser.parse("Read 10 pages of a book")
        assertTrue("Activity should start with Read (actual=${ex1.activityName})", ex1.activityName.startsWith("Read"))
        assertEquals(HabitMeasurementKind.QUANTITY, ex1.measurementKind)
        assertEquals(TrackingMode.QUANTITY, ex1.trackingMode)
        assertEquals(10, ex1.targetValue)
        assertEquals(0, ex1.valueScale)
        assertEquals("pages", ex1.unitCode)
        assertEquals("pages", ex1.storageUnitCode)
        assertEquals(ScheduleKind.DAILY, ex1.scheduleKind)
        assertEquals(TargetPeriod.DAY, ex1.targetPeriod)
        assertFalse(ex1.needsUserConfirmation)

        // Example 2: Plant 10 seeds (whole-item count)
        val ex2 = HabitNaturalLanguageParser.parse("Plant 10 seeds")
        assertEquals("Plant", ex2.activityName)
        assertEquals(HabitMeasurementKind.QUANTITY, ex2.measurementKind)
        assertEquals(TrackingMode.QUANTITY, ex2.trackingMode)
        assertEquals(10, ex2.targetValue)
        assertEquals(0, ex2.valueScale)
        assertEquals("seeds", ex2.unitCode)
        assertEquals("10 seeds", ex2.formattedTargetSummary())
        assertEquals(ScheduleKind.DAILY, ex2.scheduleKind)

        // Example 3: Sell 5 handmade bags / Sell 3 handmade bags
        val ex3 = HabitNaturalLanguageParser.parse("Sell 5 handmade bags")
        assertEquals("Sell", ex3.activityName)
        assertEquals(HabitMeasurementKind.QUANTITY, ex3.measurementKind)
        assertEquals(TrackingMode.QUANTITY, ex3.trackingMode)
        assertEquals(5, ex3.targetValue)
        assertEquals("handmade bags", ex3.unitCode)
        assertEquals(ScheduleKind.DAILY, ex3.scheduleKind)

        val ex3b = HabitNaturalLanguageParser.parse("Sell 3 handmade bags")
        assertEquals(3, ex3b.targetValue)
        assertEquals("handmade bags", ex3b.unitCode)
        assertEquals("3 handmade bags", ex3b.formattedTargetSummary())

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

        // Example 6: Save ₦5,000 (whole-unit currency) & Save ₦1,250.50 (minor-unit currency)
        val ex6 = HabitNaturalLanguageParser.parse("Save ₦5,000")
        assertEquals("Save", ex6.activityName)
        assertEquals(HabitMeasurementKind.AMOUNT_CURRENCY, ex6.measurementKind)
        assertEquals(TrackingMode.QUANTITY, ex6.trackingMode)
        assertEquals(5000, ex6.targetValue)
        assertEquals(0, ex6.valueScale)
        assertEquals("naira", ex6.unitCode)
        assertEquals("naira", ex6.storageUnitCode)
        assertEquals("₦", ex6.currencySymbol)
        assertEquals("₦5,000 naira", ex6.formattedTargetSummary())
        assertEquals(ScheduleKind.DAILY, ex6.scheduleKind)

        val minorCurrency = HabitNaturalLanguageParser.parse("Save ₦1,250.50")
        assertEquals("Save", minorCurrency.activityName)
        assertEquals(HabitMeasurementKind.AMOUNT_CURRENCY, minorCurrency.measurementKind)
        assertEquals(TrackingMode.QUANTITY, minorCurrency.trackingMode)
        assertEquals(125050, minorCurrency.targetValue)
        assertEquals(2, minorCurrency.valueScale)
        assertEquals("1250.50", minorCurrency.targetAmountText)
        assertEquals("naira", minorCurrency.unitCode)
        assertEquals("naira@scale2", minorCurrency.storageUnitCode)
        assertEquals("₦1,250.50 naira", minorCurrency.formattedTargetSummary())
        assertFalse(minorCurrency.needsUserConfirmation)

        // Example 7: Walk for 30 minutes & Walk 2.5 hours
        val ex7 = HabitNaturalLanguageParser.parse("Walk for 30 minutes")
        assertEquals("Walk", ex7.activityName)
        assertEquals(HabitMeasurementKind.DURATION, ex7.measurementKind)
        assertEquals(TrackingMode.DURATION, ex7.trackingMode)
        assertEquals(30, ex7.targetValue)
        assertEquals("min", ex7.unitCode)
        assertEquals(ScheduleKind.DAILY, ex7.scheduleKind)

        val walkDecimalHours = HabitNaturalLanguageParser.parse("Walk 2.5 hours")
        assertEquals("Walk", walkDecimalHours.activityName)
        assertEquals(HabitMeasurementKind.DURATION, walkDecimalHours.measurementKind)
        assertEquals(TrackingMode.DURATION, walkDecimalHours.trackingMode)
        assertEquals(150, walkDecimalHours.targetValue)
        assertEquals("min", walkDecimalHours.unitCode)
        assertEquals("150 min", walkDecimalHours.formattedTargetSummary())
        assertFalse(walkDecimalHours.needsUserConfirmation)

        val studyDecimalHours = HabitNaturalLanguageParser.parse("Study for 1.5 hours")
        assertEquals(HabitMeasurementKind.DURATION, studyDecimalHours.measurementKind)
        assertEquals(90, studyDecimalHours.targetValue)
        assertEquals("min", studyDecimalHours.unitCode)

        // Example 8: Meditate daily
        val ex8 = HabitNaturalLanguageParser.parse("Meditate daily")
        assertEquals("Meditate", ex8.activityName)
        assertEquals(HabitMeasurementKind.SIMPLE_CHECK_OFF, ex8.measurementKind)
        assertEquals(TrackingMode.COMPLETION_ONLY, ex8.trackingMode)
        assertNull(ex8.targetValue)
        assertNull(ex8.unitCode)
        assertEquals(ScheduleKind.DAILY, ex8.scheduleKind)
        assertFalse("Explicit 'daily' in prompt is not a silent fallback default", ex8.isDefaultSchedule)

        // Fractional measurement 1: "Drink 1.5 litres" -> preserved as 1.5 litres (never rounded to 2!)
        val drinkLitres = HabitNaturalLanguageParser.parse("Drink 1.5 litres")
        assertEquals("Drink", drinkLitres.activityName)
        assertEquals(HabitMeasurementKind.QUANTITY, drinkLitres.measurementKind)
        assertEquals(TrackingMode.QUANTITY, drinkLitres.trackingMode)
        assertEquals(150, drinkLitres.targetValue)
        assertEquals(2, drinkLitres.valueScale)
        assertEquals("1.5", drinkLitres.targetAmountText)
        assertEquals("litres", drinkLitres.unitCode)
        assertEquals("litres@scale2", drinkLitres.storageUnitCode)
        assertEquals("1.5 litres", drinkLitres.formattedTargetSummary())
        assertFalse("Known fractional unit 'litres' must not require ambiguity confirmation", drinkLitres.needsUserConfirmation)

        // Fractional measurement 2: "0.75 kilograms" -> preserved as 0.75 kilograms (never rounded to 1!)
        val kgHabit = HabitNaturalLanguageParser.parse("0.75 kilograms")
        assertEquals(HabitMeasurementKind.QUANTITY, kgHabit.measurementKind)
        assertEquals(TrackingMode.QUANTITY, kgHabit.trackingMode)
        assertEquals(75, kgHabit.targetValue)
        assertEquals(2, kgHabit.valueScale)
        assertEquals("0.75", kgHabit.targetAmountText)
        assertEquals("kilograms", kgHabit.unitCode)
        assertEquals("kilograms@scale2", kgHabit.storageUnitCode)
        assertEquals("0.75 kilograms", kgHabit.formattedTargetSummary())
        assertFalse("Known fractional unit 'kilograms' must not require ambiguity confirmation", kgHabit.needsUserConfirmation)

        // Fractional measurement 3: "Drink 2.5 liters of water" -> preserved as 2.5 liters (never rounded to 3!)
        val decQuantity = HabitNaturalLanguageParser.parse("Drink 2.5 liters of water")
        assertEquals(HabitMeasurementKind.QUANTITY, decQuantity.measurementKind)
        assertEquals(250, decQuantity.targetValue)
        assertEquals(2, decQuantity.valueScale)
        assertEquals("2.5", decQuantity.targetAmountText)
        assertEquals("liters", decQuantity.unitCode)
        assertEquals("liters@scale2", decQuantity.storageUnitCode)
        assertEquals("2.5 liters", decQuantity.formattedTargetSummary())
        assertFalse(decQuantity.needsUserConfirmation)

        // Decimal with whole-item countable unit -> flags needsUserConfirmation with clear reason
        val decWholeCountable = HabitNaturalLanguageParser.parse("Read 1.5 pages")
        assertTrue(
            "Countable whole-item unit 'pages' given a decimal must flag needsUserConfirmation",
            decWholeCountable.needsUserConfirmation
        )
        assertNotNull(decWholeCountable.ambiguityReason)

        // Input with multiple numbers -> Ambiguity flagged
        val twoTargets = HabitNaturalLanguageParser.parse("Do 2 sets of 15 squats")
        assertTrue("Multiple target numbers should flag needsUserConfirmation", twoTargets.needsUserConfirmation)
        assertNotNull(twoTargets.ambiguityReason)

        // Currency units in symbols and words
        val usdHabit = HabitNaturalLanguageParser.parse("Invest $250 every week")
        assertEquals(HabitMeasurementKind.AMOUNT_CURRENCY, usdHabit.measurementKind)
        assertEquals(250, usdHabit.targetValue)
        assertEquals("dollars", usdHabit.unitCode)
        assertEquals(TargetPeriod.WEEK, usdHabit.targetPeriod)

        // Unknown arbitrary custom units
        val customUnit = HabitNaturalLanguageParser.parse("Knit 4 coaster patterns")
        assertEquals("Knit", customUnit.activityName)
        assertEquals(HabitMeasurementKind.QUANTITY, customUnit.measurementKind)
        assertEquals(4, customUnit.targetValue)
        assertEquals("coaster patterns", customUnit.unitCode)

        // Empty or whitespace input
        val emptyInput = HabitNaturalLanguageParser.parse("   ")
        assertEquals("", emptyInput.activityName)
        assertEquals(HabitMeasurementKind.SIMPLE_CHECK_OFF, emptyInput.measurementKind)
        assertNull(emptyInput.targetValue)

        // Singular / plural, decimal, and minor-unit currency formatting
        assertEquals("1 page", UnitFormatter.formatValueWithUnit(1, "pages"))
        assertEquals("2 pages", UnitFormatter.formatValueWithUnit(2, "pages"))
        assertEquals("1 handmade bag", UnitFormatter.formatValueWithUnit(1, "handmade bags"))
        assertEquals("5 handmade bags", UnitFormatter.formatValueWithUnit(5, "handmade bags"))
        assertEquals("1.5 litres", UnitFormatter.formatValueWithUnit(150, "litres@scale2"))
        assertEquals("0.75 kilograms", UnitFormatter.formatValueWithUnit(75, "kilograms@scale2"))
        assertEquals("0.75 / 1.5 litres", UnitFormatter.formatProgressRatio(75, 150, "litres@scale2"))
        assertEquals("₦2,000 / ₦5,000 naira", UnitFormatter.formatProgressRatio(2000, 5000, "naira"))
        assertEquals("₦500.25 / ₦1,250.50 naira", UnitFormatter.formatProgressRatio(50025, 125050, "naira@scale2"))
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

        // Verify across 320dp, 360dp, and 411dp widths on first-use Today screen
        for (widthDp in listOf(320, 360, 411)) {
            pumpActivity(activity, widthDp = widthDp, heightDp = 840)
            val cardBounds = findNodeByTag(activity, "onboarding_welcome_card").boundsInRoot
            val createBtnBounds = findNodeByTag(activity, "onboarding_create_first_habit_btn").boundsInRoot
            assertTrue("Welcome card must have positive size at ${widthDp}dp", cardBounds.width > 0f && cardBounds.height > 0f)
            assertTrue("Create First Habit button must have positive size at ${widthDp}dp", createBtnBounds.width > 0f && createBtnBounds.height > 0f)
        }

        pumpActivity(activity, widthDp = 360, heightDp = 840)

        // A. Fresh install with 0 habits:
        assertNotNull(findNodeByTagOrNull(activity, "onboarding_welcome_card"))
        assertNotNull(findNodeByTagOrNull(activity, "onboarding_create_first_habit_btn"))
        assertNotNull(findNodeByTagOrNull(activity, "onboarding_setup_toggle_btn"))
        assertNotNull(findNodeByTagOrNull(activity, "onboarding_dismiss_btn"))

        assertNull("TodayLongestStreakIndicator must be hidden when 0 habits exist", findNodeByTagOrNull(activity, "today_main_streak_indicator"))
        assertNull("Today's Seeds header must be hidden when 0 habits exist", findNodeByTagOrNull(activity, "today_seeds_section_header"))
        assertNull("0 of 0 progress ratio text must be hidden when 0 habits exist", findNodeByTagOrNull(activity, "today_progress_ratio_text"))
        assertNull("Reflection pen must be hidden when 0 habits exist", findNodeByTagOrNull(activity, "today_write_reflection_btn"))
        assertNull("CollectedMomentsSummaryCard must be hidden when 0 habits exist", findNodeByTagOrNull(activity, "growth_mark_summary_card"))
        assertNull("Floating action button must be hidden when 0 habits exist", findNodeByTagOrNull(activity, "add_habit_fab"))

        val zeroHabitTexts = collectAllRenderedTexts(activity)
        val mottoOccurrences = zeroHabitTexts.count { it.contains("Your progress is more than your streak.") }
        assertEquals("Motto must appear only ONCE on TodayScreen", 1, mottoOccurrences)

        saveScreenshotPng(activity, "01_today_first_use_progressive_disclosure.png", widthDp = 360, heightDp = 800)

        // B. Tapping "Explore Dayseed" dismisses onboarding_completed -> shows today_empty_habits_card
        clickTag(activity, "onboarding_dismiss_btn", widthDp = 360, heightDp = 840)
        pumpActivity(activity, widthDp = 360, heightDp = 840, recompose = true)
        assertEquals("true", repo.getSettingString("onboarding_completed", "false"))
        assertNotNull(findNodeByTagOrNull(activity, "today_empty_habits_card"))
        assertNull(findNodeByTagOrNull(activity, "today_write_reflection_btn"))
        assertNull(findNodeByTagOrNull(activity, "growth_mark_summary_card"))

        // C. When user has 1 habit that is NOT scheduled for today (Saturday 2026-11-14, habit scheduled Monday only):
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
        assertNotNull("Header reflection pen should appear once user has at least 1 active habit", findNodeByTagOrNull(activity, "today_write_reflection_btn"))
        assertNull("The old Daily Reflection card is gone (D-236)", findNodeByTagOrNull(activity, "reflection_card"))
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

        // E. Record progress -> streak and collected-moments cards no longer appear on Today (moved to Insights, D-236)
        repo.logHabitEntrySync("h_read_pages", today, 10, "Chapter 1")
        pumpActivity(activity, widthDp = 360, heightDp = 2400, recompose = true)

        assertNull("Longest-rhythm indicator lives on Insights, not Today (D-236)", findNodeByTagOrNull(activity, "today_main_streak_indicator"))
        assertNull("Collected-moments card no longer on Today (D-236)", findNodeByTagOrNull(activity, "growth_mark_summary_card"))

        saveScreenshotPng(activity, "04_today_active_habits_and_progress.png", widthDp = 360, heightDp = 900)

        // Do not destroy the activity here: an explicit destroy cancels the shared Compose frame
        // clock, so every later test in this class would read stale, never-recomposed UI.
        // Robolectric tears the activity down at the end of the test.
        controller.pause().stop()
        repo.dbHelper.close()
    }

    // =========================================================================
    // 3. Smart Habit Creation UI: Collapsed Interpretation Controls, Adjust Interpretation & Overrides
    // =========================================================================

    @Test
    fun createEditHabitScreen_collapsesInterpretationControlsByDefaultAndRevealsOnAdjustInterpretation() {
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

        // 1. Both "Adjust interpretation" and "More Options" are collapsed by default on new habit creation
        assertNotNull(findNodeByTagOrNull(activity, "habit_name_input"))
        assertNull("Summary card stays hidden until the person types something (D-239)", findNodeByTagOrNull(activity, "smart_habit_interpretation_card"))
        assertNotNull(findNodeByTagOrNull(activity, "habit_advanced_options_toggle"))
        assertNotNull("Pinned Create button is always visible", findNodeByTagOrNull(activity, "save_habit_btn"))

        assertNull("Activity display name input must be collapsed until Adjust interpretation is tapped", findNodeByTagOrNull(activity, "smart_interpreted_activity_input"))
        assertNull("Measurement radio options must be collapsed until Adjust interpretation is tapped", findNodeByTagOrNull(activity, "mode_option_quantity"))
        assertNull("Custom unit input must be collapsed until Adjust interpretation is tapped", findNodeByTagOrNull(activity, "habit_unit_code_input"))
        assertNull("Target goal input must be collapsed until Adjust interpretation is tapped", findNodeByTagOrNull(activity, "habit_target_value_input"))
        assertNull("Category input must be hidden while More Options is collapsed", findNodeByTagOrNull(activity, "habit_category_input"))
        assertNull("Lifetime goal input must be hidden while More Options is collapsed", findNodeByTagOrNull(activity, "habit_lifetime_goal_input"))
        assertNull("Reminder toggle must be hidden while More Options is collapsed", findNodeByTagOrNull(activity, "habit_reminder_toggle"))

        // 2. Type natural-language habit: "Read 10 pages of a book"
        setTextTag(activity, "habit_name_input", "Read 10 pages of a book", widthDp = 360, heightDp = 1100)

        assertNotNull(findNodeByTagOrNull(activity, "smart_habit_interpretation_card"))
        assertNotNull(findNodeByTagOrNull(activity, "smart_adjust_interpretation_toggle"))
        assertTrue("Live note under the field confirms what was understood",
            collectAllRenderedTexts(activity).any { it.startsWith("Understood:") && it.contains("Read") })
        val textsAfterRead = collectAllRenderedTexts(activity)
        assertTrue("Summary card must show 'Here's what we understood:'", textsAfterRead.any { it.contains("Here's what we understood:") })
        assertTrue("Summary must show interpreted activity 'Read'", textsAfterRead.any { it.startsWith("Read") })
        assertTrue("Summary must show Quantity measurement type", textsAfterRead.any { it.contains("Quantity") })
        assertTrue("Summary must show target '10 pages'", textsAfterRead.any { it.contains("10 pages") })
        assertTrue("Summary must show 'Daily (default)' schedule", textsAfterRead.any { it.contains("Daily (default)") })

        // Verify detailed controls are STILL collapsed after typing natural language
        assertNull(findNodeByTagOrNull(activity, "smart_interpreted_activity_input"))
        assertNull(findNodeByTagOrNull(activity, "habit_unit_code_input"))
        assertNull(findNodeByTagOrNull(activity, "habit_target_value_input"))

        // Verify across 320dp, 360dp, and 411dp widths
        for (widthDp in listOf(320, 360, 411)) {
            pumpActivity(activity, widthDp = widthDp, heightDp = 900)
            val saveBounds = findNodeByTag(activity, "save_habit_btn").boundsInRoot
            val cancelBounds = findNodeByTag(activity, "cancel_habit_btn").boundsInRoot
            assertTrue("Save and Cancel buttons must not overlap at ${widthDp}dp", cancelBounds.right <= saveBounds.left + 1f)
        }

        // Screenshot 2: Smart habit creation with collapsed interpretation controls
        saveScreenshotPng(activity, "02_smart_habit_creation_interpreted.png", widthDp = 360, heightDp = 820)

        // 3. Tap "Adjust interpretation" -> reveals detailed editing controls
        clickTag(activity, "smart_adjust_interpretation_toggle", widthDp = 360, heightDp = 1300)
        assertNotNull(findNodeByTagOrNull(activity, "smart_interpreted_activity_input"))
        assertNotNull(findNodeByTagOrNull(activity, "mode_option_quantity"))
        assertNotNull(findNodeByTagOrNull(activity, "habit_unit_code_input"))
        assertNotNull(findNodeByTagOrNull(activity, "habit_target_value_input"))

        // Screenshot 3: Expanded "Adjust interpretation" state
        saveScreenshotPng(activity, "05_smart_habit_adjust_interpretation_expanded.png", widthDp = 360, heightDp = 1050)

        // 4. Test custom unit & user override: type "Sell 5 handmade bags", then override target to 8
        setTextTag(activity, "habit_name_input", "Sell 5 handmade bags", widthDp = 360, heightDp = 1300)
        val textsAfterBags = collectAllRenderedTexts(activity)
        assertTrue(textsAfterBags.any { it.contains("handmade bags") })

        setTextTag(activity, "habit_target_value_input", "8", widthDp = 360, heightDp = 1300)
        val textsAfterOverride = collectAllRenderedTexts(activity)
        assertTrue("Summary target must reflect user-overridden target '8 handmade bags'", textsAfterOverride.any { it.contains("8 handmade bags") })

        // 5. Expand "More Options" and verify advanced fields become accessible
        clickTag(activity, "habit_advanced_options_toggle", widthDp = 360, heightDp = 1600)
        assertNotNull(findNodeByTagOrNull(activity, "habit_reminder_toggle"))
        assertNotNull(findNodeByTagOrNull(activity, "habit_desc_input"))
        assertNotNull(findNodeByTagOrNull(activity, "habit_category_input"))
        assertNotNull(findNodeByTagOrNull(activity, "habit_lifetime_goal_input"))

        // 6. Save the habit and verify persisted Habit model
        clickTag(activity, "save_habit_btn", widthDp = 360, heightDp = 1600)
        repo.habits.awaitMatch { list -> list.any { it.name == "Sell 5 Handmade Bags" } }
        val savedHabit = repo.habits.value.first { it.name == "Sell 5 Handmade Bags" }
        assertEquals(TrackingMode.QUANTITY, savedHabit.trackingMode)
        assertEquals(8, savedHabit.targetValue)
        assertEquals("handmade bags", savedHabit.unitCode)
        assertEquals(ScheduleKind.DAILY, savedHabit.scheduleKind)

        // Do not destroy the activity here: an explicit destroy cancels the shared Compose frame
        // clock, so every later test in this class would read stale, never-recomposed UI.
        // Robolectric tears the activity down at the end of the test.
        controller.pause().stop()
        repo.dbHelper.close()
    }

    // =========================================================================
    // 4. Progress-Entry Default Value, Validation, Edit/Delete & Duplicate Guard
    // =========================================================================

    @Test
    fun recordSessionSheet_startsEmptyForNewEntryValidatesInputsAndSupportsEditAndDelete() {
        val dbName = "test_record_session_defaults_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)
        val today = testClock.todayLocal()

        // Verify pure helper for new vs existing entries across modes
        assertEquals("", defaultRecordSessionValueText(null, TrackingMode.QUANTITY, TargetPeriod.DAY, "pages", 10))
        assertEquals("", defaultRecordSessionValueText(null, TrackingMode.DURATION, TargetPeriod.DAY, "min", 30))
        assertEquals("1", defaultRecordSessionValueText(null, TrackingMode.COMPLETION_ONLY, TargetPeriod.DAY, null, 1))

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
        repo.saveHabitSync(readHabit, effectiveDateOverride = today)

        // Log 2 pages, 3 pages, 1 page -> existing progress is 6 / 10 pages (60%)
        val (e1, _) = repo.logHabitEntrySync("h_pages", today, 2, "Morning pages")
        val (e2, _) = repo.logHabitEntrySync("h_pages", today, 3, "Lunch break")
        val (e3, _) = repo.logHabitEntrySync("h_pages", today, 1, "Evening page")

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
        pumpActivity(activity, widthDp = 360, heightDp = 1500)

        // 1. New quantity session starts with EMPTY input ("") — never 10!
        assertEquals(
            "New quantity session input must start empty",
            "",
            getEditableText(activity, "record_session_value_input")
        )
        assertFalse(
            "Save Progress button must be disabled while input is empty",
            isNodeEnabled(activity, "record_session_save_btn")
        )

        val initialTexts = collectAllRenderedTexts(activity)
        assertTrue(
            "Existing progress must display '6 / 10 pages (60%)'",
            initialTexts.any { it.contains("Existing progress on ${friendlyDate(today, today)}: 6 / 10 pages (60%)") }
        )
        assertTrue(
            "Resulting daily total must remain '6 / 10 pages (60%)' before user enters a value",
            initialTexts.any { it.contains("Resulting daily total on ${friendlyDate(today, today)}: 6 / 10 pages (60%)") }
        )
        assertFalse(
            "Preview must NEVER show '16 / 10 pages (160%)' on open",
            initialTexts.any { it.contains("16 / 10 pages") || it.contains("160%") }
        )

        // Screenshot 4: Add Progress with empty initial input (6 / 10 pages (60%))
        saveScreenshotPng(activity, "03_record_session_incremental_progress.png", widthDp = 360, heightDp = 1080)

        // 2. Reject zero and negative inputs: Save button remains disabled for "0" and "-3"
        setTextTag(activity, "record_session_value_input", "0", widthDp = 360, heightDp = 1500)
        assertFalse("Save Progress must remain disabled when input is 0", isNodeEnabled(activity, "record_session_save_btn"))
        setTextTag(activity, "record_session_value_input", "-3", widthDp = 360, heightDp = 1500)
        assertFalse("Save Progress must remain disabled when input is negative", isNodeEnabled(activity, "record_session_save_btn"))
        // Reject decimal on whole-item 'pages' habit
        setTextTag(activity, "record_session_value_input", "1.5", widthDp = 360, heightDp = 1500)
        assertFalse("Save Progress must remain disabled when decimal is entered for whole-item unit 'pages'", isNodeEnabled(activity, "record_session_save_btn"))

        // 3. Enter valid amount "2" -> preview updates to New entry: +2 pages, Resulting daily total: 8 / 10 pages (80%)
        setTextTag(activity, "record_session_value_input", "2", widthDp = 360, heightDp = 1500)
        assertTrue("Save Progress must be enabled when valid positive amount '2' is entered", isNodeEnabled(activity, "record_session_save_btn"))
        val textsAfterTwo = collectAllRenderedTexts(activity)
        assertTrue(textsAfterTwo.any { it.contains("Existing progress on ${friendlyDate(today, today)}: 6 / 10 pages (60%)") })
        assertTrue(textsAfterTwo.any { it.contains("New entry: +2 pages") })
        assertTrue(textsAfterTwo.any { it.contains("Resulting daily total on ${friendlyDate(today, today)}: 8 / 10 pages (80%)") })

        // Verify across 320dp, 360dp, and 411dp widths
        for (widthDp in listOf(320, 360, 411)) {
            pumpActivity(activity, widthDp = widthDp, heightDp = 1500)
            val saveBounds = findNodeByTag(activity, "record_session_save_btn").boundsInRoot
            val cancelBounds = findNodeByTag(activity, "record_session_cancel_btn").boundsInRoot
            assertTrue("Save and Cancel buttons must not overlap at ${widthDp}dp", cancelBounds.right <= saveBounds.left + 1f)
        }

        // Screenshot 5: Add Progress after entering a value (2 -> 8 / 10 pages (80%))
        saveScreenshotPng(activity, "06_record_session_after_entering_value.png", widthDp = 360, heightDp = 1080)

        // 4. Tap Edit on the 3-page entry (e2) -> pre-populates original value "3" and note "Lunch break"
        clickTag(activity, "record_session_edit_entry_${e2.entryId}", widthDp = 360, heightDp = 1500)
        assertEquals("Editing existing entry must pre-populate its original value '3'", "3", getEditableText(activity, "record_session_value_input"))
        assertEquals("Lunch break", getEditableText(activity, "record_session_note_input"))
        val textsWhileEditingOriginal = collectAllRenderedTexts(activity)
        assertTrue(textsWhileEditingOriginal.any { it.contains("Edit Progress Entry") })
        assertTrue(textsWhileEditingOriginal.any { it.contains("Updating entry to: 3 pages") })
        assertTrue(textsWhileEditingOriginal.any { it.contains("Resulting daily total on ${friendlyDate(today, today)}: 6 / 10 pages (60%)") })

        // Screenshot 6: Existing-entry editing with original value populated
        saveScreenshotPng(activity, "07_record_session_edit_existing_entry.png", widthDp = 360, heightDp = 1080)

        // Delete the 1-page entry (e3) -> total becomes 5 / 10 pages (50%)
        clickTag(activity, "record_session_delete_entry_${e3.entryId}", widthDp = 360, heightDp = 1500)
        repo.habitEntries.awaitMatch { list -> list.none { it.entryId == e3.entryId } }
        pumpActivity(activity, widthDp = 360, heightDp = 1500, recompose = true)

        // Update the 3-page entry (e2) to 5 pages -> total becomes 2 + 5 = 7 / 10 pages (70%) without duplicating e2
        clickTag(activity, "record_session_edit_entry_${e2.entryId}", widthDp = 360, heightDp = 1500)
        setTextTag(activity, "record_session_value_input", "5", widthDp = 360, heightDp = 1500)
        clickTag(activity, "record_session_save_btn", widthDp = 360, heightDp = 1500)
        repo.habitEntries.awaitMatch { list -> list.firstOrNull { it.entryId == e2.entryId }?.valueUnits == 5 }

        val entriesAfterEdit = repo.habitEntries.value.filter { it.habitId == "h_pages" && it.localDate == today.toString() }
        assertEquals("Editing an entry must not duplicate it", 2, entriesAfterEdit.size)
        assertEquals(7, entriesAfterEdit.sumOf { it.valueUnits })
        assertNotNull(entriesAfterEdit.find { it.entryId == e1.entryId })
        assertNotNull(entriesAfterEdit.find { it.entryId == e2.entryId })

        // Do not destroy the activity here: an explicit destroy cancels the shared Compose frame
        // clock, so every later test in this class would read stale, never-recomposed UI.
        // Robolectric tears the activity down at the end of the test.
        controller.pause().stop()
        repo.dbHelper.close()
    }

    // =========================================================================
    // 5. Fractional Measurements, Minor-Unit Currency, Accumulation & Backup Round-Trip
    // =========================================================================

    @Test
    fun fractionalMeasurementsAndMinorCurrency_accumulateWithoutPrecisionLossAndRoundTripBackup() {
        val dbName = "test_fractional_measurements_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)
        val today = testClock.todayLocal()

        // 1. Create "Drink 1.5 litres" via SmartHabitInterpretation (150 stored units at @scale2)
        val parsedLitres = HabitNaturalLanguageParser.parse("Drink 1.5 litres")
        val litresHabit = Habit(
            habitId = "h_litres",
            name = parsedLitres.activityName,
            description = "Daily hydration",
            trackingMode = parsedLitres.trackingMode,
            unitCode = parsedLitres.storageUnitCode,
            targetPeriod = parsedLitres.targetPeriod,
            targetValue = parsedLitres.targetValue,
            scheduleKind = parsedLitres.scheduleKind,
            colorHex = "#286143"
        )
        // 2. Create "Weigh 0.75 kilograms" (75 stored units at @scale2)
        val parsedKg = HabitNaturalLanguageParser.parse("0.75 kilograms")
        val kgHabit = Habit(
            habitId = "h_kg",
            name = "Prep Ingredients",
            description = "",
            trackingMode = parsedKg.trackingMode,
            unitCode = parsedKg.storageUnitCode,
            targetPeriod = parsedKg.targetPeriod,
            targetValue = parsedKg.targetValue,
            scheduleKind = parsedKg.scheduleKind,
            colorHex = "#3E6B48"
        )
        // 3. Create "Save ₦1,250.50" (125050 stored units at @scale2)
        val parsedMinorNaira = HabitNaturalLanguageParser.parse("Save ₦1,250.50")
        val nairaHabit = Habit(
            habitId = "h_naira_minor",
            name = parsedMinorNaira.activityName,
            description = "",
            trackingMode = parsedMinorNaira.trackingMode,
            unitCode = parsedMinorNaira.storageUnitCode,
            targetPeriod = parsedMinorNaira.targetPeriod,
            targetValue = parsedMinorNaira.targetValue,
            scheduleKind = parsedMinorNaira.scheduleKind,
            colorHex = "#5A7247"
        )

        repo.saveHabitSync(litresHabit, effectiveDateOverride = today)
        repo.saveHabitSync(kgHabit, effectiveDateOverride = today)
        repo.saveHabitSync(nairaHabit, effectiveDateOverride = today)

        // Log 0.5 litres (50) and 0.25 litres (25) -> 0.75 / 1.5 litres (50%)
        val s1 = UnitFormatter.parseUserValueToStoredUnits("0.5", litresHabit.unitCode)!!
        val s2 = UnitFormatter.parseUserValueToStoredUnits("0.25", litresHabit.unitCode)!!
        assertEquals(50, s1)
        assertEquals(25, s2)
        repo.logHabitEntrySync("h_litres", today, s1, "Morning bottle")
        val (eLitres2, _) = repo.logHabitEntrySync("h_litres", today, s2, "Midday glass")

        // Log 0.25 kg (25) + 0.5 kg (50) -> 0.75 / 0.75 kilograms (100%)
        repo.logHabitEntrySync("h_kg", today, UnitFormatter.parseUserValueToStoredUnits("0.25", kgHabit.unitCode)!!, null)
        repo.logHabitEntrySync("h_kg", today, UnitFormatter.parseUserValueToStoredUnits("0.5", kgHabit.unitCode)!!, null)

        // Log ₦500.25 (50025) + ₦750.25 (75025) -> ₦1,250.50 / ₦1,250.50 naira (100%)
        repo.logHabitEntrySync("h_naira_minor", today, UnitFormatter.parseUserValueToStoredUnits("500.25", nairaHabit.unitCode)!!, null)
        repo.logHabitEntrySync("h_naira_minor", today, UnitFormatter.parseUserValueToStoredUnits("750.25", nairaHabit.unitCode)!!, null)

        // Render RecordSessionSheet for "Drink 1.5 litres" with 0.75 / 1.5 litres (50%) logged,
        // and type "0.75" to preview reaching 1.5 / 1.5 litres (100%)
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            DayseedTheme {
                RecordSessionSheet(
                    viewModel = vm,
                    habitId = "h_litres",
                    initialLocalDate = today.toString(),
                    onNavigateBack = {}
                )
            }
        }
        pumpActivity(activity, widthDp = 360, heightDp = 1100)

        val sheetTextsInitial = collectAllRenderedTexts(activity)
        assertTrue(
            "Header must show '1.5 litres / day' without raw '@scale2'",
            sheetTextsInitial.any { it.contains("1.5 litres / day") } &&
                sheetTextsInitial.none { it.contains("@scale2") }
        )
        assertTrue(
            "Existing progress must show '0.75 / 1.5 litres (50%)'",
            sheetTextsInitial.any { it.contains("Existing progress on ${friendlyDate(today, today)}: 0.75 / 1.5 litres (50%)") }
        )

        // Enter "0.75" in the input -> preview shows New entry: +0.75 litres, Resulting daily total: 1.5 / 1.5 litres (100%)
        setTextTag(activity, "record_session_value_input", "0.75", widthDp = 360, heightDp = 1100)
        val sheetTextsWithEntry = collectAllRenderedTexts(activity)
        assertTrue(sheetTextsWithEntry.any { it.contains("New entry: +0.75 litres") })
        assertTrue(sheetTextsWithEntry.any { it.contains("Resulting daily total on ${friendlyDate(today, today)}: 1.5 / 1.5 litres (100%)") })

        // Screenshot 7: Fractional measurement habit (1.5 litres with 0.75 existing + 0.75 new entry)
        saveScreenshotPng(activity, "08_fractional_measurement_habit_progress.png", widthDp = 360, heightDp = 980)

        // Verify editing existing 0.25 litres entry pre-populates "0.25"
        clickTag(activity, "record_session_edit_entry_${eLitres2.entryId}", widthDp = 360, heightDp = 1100)
        assertEquals("0.25", getEditableText(activity, "record_session_value_input"))

        // Verify backup and restore preserves fractional units and scaled values accurately
        val exportedJson = repo.exportPortableBackupSync(password = null)
        val restoredDbName = "test_fractional_restored_${System.nanoTime()}.db"
        val restoredRepo = newRepository(restoredDbName)
        restoredRepo.restorePortableBackupSync(exportedJson, restoreMode = "REPLACE", password = null)

        val restoredHabits = restoredRepo.habits.value.associateBy { it.habitId }
        assertEquals("litres@scale2", restoredHabits["h_litres"]?.unitCode)
        assertEquals(150, restoredHabits["h_litres"]?.targetValue)
        assertEquals("1.5 litres / day", restoredHabits["h_litres"]?.formatTargetAndPeriodSummary())

        assertEquals("kilograms@scale2", restoredHabits["h_kg"]?.unitCode)
        assertEquals(75, restoredHabits["h_kg"]?.targetValue)
        assertEquals("0.75 kilograms / day", restoredHabits["h_kg"]?.formatTargetAndPeriodSummary())

        assertEquals("naira@scale2", restoredHabits["h_naira_minor"]?.unitCode)
        assertEquals(125050, restoredHabits["h_naira_minor"]?.targetValue)
        assertEquals("₦1,250.50 naira / day", restoredHabits["h_naira_minor"]?.formatTargetAndPeriodSummary())

        val restoredNairaEntries = restoredRepo.habitEntries.value.filter { it.habitId == "h_naira_minor" }
        assertEquals(125050, restoredNairaEntries.sumOf { it.valueUnits })
        assertEquals(
            "₦1,250.50 / ₦1,250.50 naira",
            UnitFormatter.formatProgressRatio(
                restoredNairaEntries.sumOf { it.valueUnits },
                125050,
                restoredHabits["h_naira_minor"]?.unitCode
            )
        )

        // Do not destroy the activity here: an explicit destroy cancels the shared Compose frame
        // clock, so every later test in this class would read stale, never-recomposed UI.
        // Robolectric tears the activity down at the end of the test.
        controller.pause().stop()
        repo.dbHelper.close()
        restoredRepo.dbHelper.close()
    }
}
