package com.example.dayseed

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.GroveTreeState
import com.example.dayseed.data.Habit
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.components.LivingGroveScreen
import com.example.dayseed.ui.theme.DayseedTheme
import com.example.dayseed.ui.theme.ThemeModeOption
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class GroveAndKeyboardVerificationTest {

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
        heightDp: Int = 920,
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

    private fun clickTag(activity: ComponentActivity, tag: String, widthDp: Int = 360, heightDp: Int = 920) {
        pumpActivity(activity, widthDp = widthDp, heightDp = heightDp, recompose = false)
        val node = findNodeByTag(activity, tag)
        val clickAction = node.config.getOrNull(SemanticsActions.OnClick)?.action
        assertNotNull("Expected OnClick action on node '$tag'", clickAction)
        clickAction!!.invoke()
        pumpActivity(activity, widthDp = widthDp, heightDp = heightDp, recompose = false)
    }

    private fun saveScreenshotPng(activity: ComponentActivity, fileName: String, widthDp: Int = 360, heightDp: Int = 920) {
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

    @Test
    fun groveBotanicalPainting_rendersEmptyStateSingleTreeAllStagesAndTouchActivatedControls() {
        val dbName = "test_grove_painting_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)
        val today = testClock.todayLocal()

        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        var darkThemeState = androidx.compose.runtime.mutableStateOf(false)

        activity.setContent {
            DayseedTheme(
                themeMode = if (darkThemeState.value) ThemeModeOption.DARK else ThemeModeOption.LIGHT
            ) {
                LivingGroveScreen(
                    viewModel = vm,
                    onBack = {}
                )
            }
        }

        // -----------------------------------------------------------------
        // CHECKPOINT A: Stage 1 Empty Landscape (0 habits)
        // -----------------------------------------------------------------
        pumpActivity(activity, widthDp = 360, heightDp = 820)
        assertNotNull(findNodeByTagOrNull(activity, "grove_botanical_painting_card"))
        assertNotNull(findNodeByTagOrNull(activity, "grove_botanical_painting_canvas"))
        assertNull(
            "Painting controls must be hidden when no leaf is touched",
            findNodeByTagOrNull(activity, "grove_touched_plant_controls")
        )

        // Verify summary stat cards have equal heights and do not overlap across 320dp, 360dp, 411dp
        for (widthDp in listOf(320, 360, 411)) {
            pumpActivity(activity, widthDp = widthDp, heightDp = 820)
            val saplingCard = findNodeByTag(activity, "grove_stat_saplings").boundsInRoot
            val growingCard = findNodeByTag(activity, "grove_stat_growing").boundsInRoot
            val dormantCard = findNodeByTag(activity, "grove_stat_dormant").boundsInRoot

            assertTrue("Sapling stat card must have positive size at ${widthDp}dp", saplingCard.width > 0f && saplingCard.height > 0f)
            assertTrue("Sapling and Growing cards must not overlap at ${widthDp}dp", saplingCard.right <= growingCard.left + 1f)
            assertTrue("Growing and Dormant cards must not overlap at ${widthDp}dp", growingCard.right <= dormantCard.left + 1f)
            assertTrue(
                "All three stat cards must have matching heights at ${widthDp}dp",
                abs(saplingCard.height - growingCard.height) <= 2f &&
                    abs(growingCard.height - dormantCard.height) <= 2f
            )
        }

        saveScreenshotPng(activity, "grove_01_empty_landscape.png", widthDp = 360, heightDp = 780)

        // -----------------------------------------------------------------
        // CHECKPOINT B: Early Visual Checkpoint — 1 Representative Growing Tree (10 active-span days)
        // -----------------------------------------------------------------
        val treeHabit = Habit(
            habitId = "h_morning_walk",
            name = "Morning Walk",
            description = "Brisk outdoor walk",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.DAY,
            targetValue = 1,
            scheduleKind = ScheduleKind.DAILY,
            colorHex = "#2E6F40"
        )
        repo.saveHabitSync(treeHabit, effectiveDateOverride = today.minusDays(9))
        for (d in 9 downTo 0) {
            repo.logHabitDayOutcomeSync("h_morning_walk", today.minusDays(d.toLong()), "COMPLETED", null)
        }

        pumpActivity(activity, widthDp = 360, heightDp = 960, recompose = true)
        assertEquals(1, vm.growthMetrics.value.growingTreesCount)
        assertEquals(0, vm.growthMetrics.value.activeSaplingsCount)
        assertEquals(0, vm.growthMetrics.value.dormantTreesCount)

        // Verify leaf touch target exists for the single growing tree, and controls are STILL hidden before touch
        assertNotNull(findNodeByTagOrNull(activity, "grove_leaf_target_h_morning_walk"))
        assertNull(
            "Painting controls must remain hidden until the tree's leaf is touched",
            findNodeByTagOrNull(activity, "grove_touched_plant_controls")
        )

        saveScreenshotPng(activity, "grove_02_single_growing_tree.png", widthDp = 360, heightDp = 820)

        // -----------------------------------------------------------------
        // CHECKPOINT C: Full Woodland Across All Stages (Seed, Sapling, Young Tree, Mature Tree, Dormant Tree)
        // -----------------------------------------------------------------
        // 1. Newly planted seed (0 active streak days)
        repo.saveHabitSync(
            Habit(
                habitId = "h_evening_journal",
                name = "Evening Journal",
                description = "",
                trackingMode = TrackingMode.COMPLETION_ONLY,
                targetPeriod = TargetPeriod.DAY,
                targetValue = 1,
                scheduleKind = ScheduleKind.DAILY,
                colorHex = "#6A8D48"
            ),
            effectiveDateOverride = today
        )

        // 2. Active Sapling (4 active-span days: 1..6d)
        repo.saveHabitSync(
            Habit(
                habitId = "h_read_book",
                name = "Read 10 Pages",
                description = "",
                trackingMode = TrackingMode.QUANTITY,
                unitCode = "pages",
                targetPeriod = TargetPeriod.DAY,
                targetValue = 10,
                scheduleKind = ScheduleKind.DAILY,
                colorHex = "#4C8547"
            ),
            effectiveDateOverride = today.minusDays(3)
        )
        for (d in 3 downTo 0) {
            repo.logHabitEntrySync("h_read_book", today.minusDays(d.toLong()), 10, null)
        }

        // 3. Mature Woodland Tree (24 active-span days) + an earlier Preserved Dormant Tree (9 active-span days)
        repo.saveHabitSync(
            Habit(
                habitId = "h_deep_focus",
                name = "Deep Focus Study",
                description = "",
                trackingMode = TrackingMode.DURATION,
                unitCode = "min",
                targetPeriod = TargetPeriod.DAY,
                targetValue = 30,
                scheduleKind = ScheduleKind.DAILY,
                colorHex = "#245C36"
            ),
            effectiveDateOverride = today.minusDays(38)
        )
        // Closed 9-day streak run (days -38..-30, missed on -29..-25) -> Preserved Dormant Tree
        for (d in 38 downTo 30) {
            repo.logHabitEntrySync("h_deep_focus", today.minusDays(d.toLong()), 30, null)
        }
        // Active 24-day streak run (days -23..0) -> Mature Growing Tree
        for (d in 23 downTo 0) {
            repo.logHabitEntrySync("h_deep_focus", today.minusDays(d.toLong()), 30, null)
        }

        pumpActivity(activity, widthDp = 360, heightDp = 1200, recompose = true)

        assertEquals(1, vm.growthMetrics.value.activeSaplingsCount)
        assertEquals(2, vm.growthMetrics.value.growingTreesCount)
        assertEquals(1, vm.growthMetrics.value.dormantTreesCount)

        // Verify StreakAndGroveEngine classification integrity
        val focusSummary = vm.habitSummaries.value["h_deep_focus"]!!
        assertEquals(GroveTreeState.GROWING_TREE, focusSummary.activeRun?.treeState)
        assertEquals(24, focusSummary.activeRun?.elapsedActiveSpanCount)
        assertTrue(focusSummary.groveRuns.any { !it.isActive && it.treeState == GroveTreeState.DORMANT_TREE && it.elapsedActiveSpanCount == 9 })

        saveScreenshotPng(activity, "grove_03_thriving_woodland_all_stages.png", widthDp = 360, heightDp = 860)

        // -----------------------------------------------------------------
        // CHECKPOINT D: Touching a Leaf Reveals Painting Controls; Dismissing Hides Them
        // -----------------------------------------------------------------
        assertNull(
            "Controls must be hidden before touching a leaf",
            findNodeByTagOrNull(activity, "grove_touched_plant_controls")
        )
        clickTag(activity, "grove_leaf_target_h_deep_focus", widthDp = 360, heightDp = 1200)
        pumpActivity(activity, widthDp = 360, heightDp = 1200, recompose = true)
        assertNotNull(
            "Touching a tree's leaf must reveal grove_touched_plant_controls",
            findNodeByTagOrNull(activity, "grove_touched_plant_controls")
        )
        assertNotNull(findNodeByTagOrNull(activity, "grove_filter_all"))
        assertNotNull(findNodeByTagOrNull(activity, "grove_filter_h_deep_focus"))
        assertNotNull(findNodeByTagOrNull(activity, "grove_touched_inspect_btn"))

        saveScreenshotPng(activity, "grove_04_touched_leaf_controls.png", widthDp = 360, heightDp = 1040)

        // Dismiss the controls card -> returns to pure uncrowded painting
        clickTag(activity, "grove_touched_close_btn", widthDp = 360, heightDp = 1200)
        pumpActivity(activity, widthDp = 360, heightDp = 1200, recompose = true)
        assertNull(
            "Dismissing leaf controls must hide grove_touched_plant_controls again",
            findNodeByTagOrNull(activity, "grove_touched_plant_controls")
        )

        // -----------------------------------------------------------------
        // CHECKPOINT E: Dark Theme Woodland Painting
        // -----------------------------------------------------------------
        darkThemeState.value = true
        pumpActivity(activity, widthDp = 360, heightDp = 860, recompose = true)
        saveScreenshotPng(activity, "grove_05_dark_theme_woodland.png", widthDp = 360, heightDp = 860)

        controller.pause().stop()
        repo.dbHelper.close()
    }

    @Test
    fun keyboardLayoutCorrection_appliesImeInsetsOnceAndKeepsCreateHabitInputsVisible() {
        val dbName = "test_keyboard_ime_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)
        vm.openCreateEditHabit(null)

        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            DayseedTheme {
                DayseedApp(
                    viewModel = vm,
                    liveLocalTime = testClock.nowLocalTime()
                )
            }
        }

        // Measure at 360x800dp with keyboard closed
        pumpActivity(activity, widthDp = 360, heightDp = 800)
        val density = activity.resources.displayMetrics.density
        val screenHeightPx = (800 * density).toInt()
        val imeHeightPx = (300 * density).toInt() // 300dp software keyboard
        val statusBarPx = (24 * density).toInt()
        val navBarPx = (48 * density).toInt()

        // Simulate Android dispatching WindowInsets with an open 300dp IME keyboard
        val decorView = activity.window.decorView
        val imeInsetsCompat = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, statusBarPx, 0, 0))
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, navBarPx))
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, imeHeightPx))
            .setVisible(WindowInsetsCompat.Type.ime(), true)
            .build()
        val platformInsets: WindowInsets? = imeInsetsCompat.toWindowInsets()
        if (platformInsets != null) {
            decorView.onApplyWindowInsets(platformInsets)
        }
        ViewCompat.dispatchApplyWindowInsets(decorView, imeInsetsCompat)
        pumpActivity(activity, widthDp = 360, heightDp = 800)

        val nameBounds = findNodeByTag(activity, "habit_name_input").boundsInRoot
        val visibleRegionBottomPx = (screenHeightPx - imeHeightPx).toFloat()

        // Previously, double IME padding subtracted 2 * 300dp = 600dp, leaving < 176dp total height
        // and clipping the input. Now, single IME padding leaves ~476dp of clean viewport above the keyboard!
        assertTrue(
            "Habit name input top (${nameBounds.top}) must be cleanly below status bar ($statusBarPx)",
            nameBounds.top >= statusBarPx - 2f
        )
        assertTrue(
            "Habit name input bottom (${nameBounds.bottom}) must be completely visible above the keyboard ($visibleRegionBottomPx)",
            nameBounds.bottom <= visibleRegionBottomPx
        )
        assertTrue(
            "Habit name input height (${nameBounds.height}) must not be crushed by double IME insets",
            nameBounds.height >= 48f * density
        )

        saveScreenshotPng(activity, "keyboard_01_create_habit_ime_open.png", widthDp = 360, heightDp = 800)

        controller.pause().stop()
        repo.dbHelper.close()
    }

    @Test
    fun saplingEntryAnimation_animatesPoppingUpFromSoilWhenNewHabitIsCreated() {
        val dbName = "test_sapling_entry_anim_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)
        val today = testClock.todayLocal()

        // Pre-existing habit before opening Grove should already be fully sprouted (no entry animation)
        repo.saveHabitSync(
            Habit(
                habitId = "h_existing_sapling",
                name = "Morning Stretch",
                description = "",
                trackingMode = TrackingMode.COMPLETION_ONLY,
                targetPeriod = TargetPeriod.DAY,
                targetValue = 1,
                scheduleKind = ScheduleKind.DAILY,
                colorHex = "#3E7A4B"
            ),
            effectiveDateOverride = today.minusDays(2)
        )
        repo.logHabitDayOutcomeSync("h_existing_sapling", today.minusDays(2), "COMPLETED", null)
        repo.logHabitDayOutcomeSync("h_existing_sapling", today.minusDays(1), "COMPLETED", null)
        repo.logHabitDayOutcomeSync("h_existing_sapling", today, "COMPLETED", null)

        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            DayseedTheme {
                LivingGroveScreen(
                    viewModel = vm,
                    onBack = {}
                )
            }
        }

        pumpActivity(activity, widthDp = 360, heightDp = 860)
        val existingNode = findNodeByTag(activity, "grove_leaf_target_h_existing_sapling")
        assertEquals(
            "Sprouted",
            existingNode.config.getOrNull(SemanticsProperties.StateDescription)
        )

        // Create a brand new habit via ViewModel.saveHabit -> triggers 'popping up from soil' Animatable entry animation
        val newHabit = Habit(
            habitId = "h_new_sapling",
            name = "Evening Tea Ritual",
            description = "Unwind with herbal tea",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.DAY,
            targetValue = 1,
            scheduleKind = ScheduleKind.DAILY,
            colorHex = "#4C8547"
        )
        vm.saveHabit(newHabit)
        assertTrue(
            "Saving a new habit must queue sapling entry animation in ViewModel",
            "h_new_sapling" in vm.newlyCreatedSaplingHabitIds.value
        )

        // The save completes on a real IO thread, so the new habit reaches the UI after an
        // unpredictable amount of real time. Wait (bounded) for it to appear instead of assuming a fixed
        // number of pumps is enough; the assertions below are unchanged and just as strict.
        val appearDeadline = System.currentTimeMillis() + 5_000L
        while (findNodeByTagOrNull(activity, "grove_leaf_target_h_new_sapling") == null &&
            System.currentTimeMillis() < appearDeadline
        ) {
            pumpActivity(activity, widthDp = 360, heightDp = 860, recompose = false)
            Thread.sleep(10)
        }

        // Advance frame clock enough to complete the spring Animatable and verify it settles to Sprouted
        for (step in 1..25) {
            pumpActivity(activity, widthDp = 360, heightDp = 860, recompose = false)
        }
        val sproutedNode = findNodeByTag(activity, "grove_leaf_target_h_new_sapling")
        assertEquals(
            "Sprouted",
            sproutedNode.config.getOrNull(SemanticsProperties.StateDescription)
        )
        assertTrue(
            "Completed sapling entry animation must consume pending habitId flag",
            "h_new_sapling" !in vm.newlyCreatedSaplingHabitIds.value
        )

        controller.pause().stop()
        repo.dbHelper.close()
    }
}

