package com.example.dayseed

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.Habit
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.theme.DayseedTheme
import com.example.dayseed.ui.theme.ThemeModeOption
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Review-only screenshot generator. It renders every top-level destination and key secondary
 * screens with realistic seeded data in several conditions (light, dark, small phone, large font)
 * and writes PNGs to the directory named by the DAYSEED_SCREENSHOT_DIR environment variable.
 *
 * It is skipped unless that variable is set, so it never affects the normal test run. Output is
 * a Robolectric software render, NOT a physical-device capture, and proves nothing about device
 * behaviour, TalkBack or real font rendering.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class UiScreenshotCaptureTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var testClock: TestDayseedClock
    private val tempDbNames = mutableListOf<String>()
    private var outDir: File? = null

    @Before
    fun setUp() {
        val dir = System.getenv("DAYSEED_SCREENSHOT_DIR")
        assumeTrue("DAYSEED_SCREENSHOT_DIR not set; screenshot capture skipped", !dir.isNullOrBlank())
        outDir = File(dir!!).also { it.mkdirs() }
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        testClock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 11, 14),
            currentLocalTime = LocalTime.of(9, 30),
            currentUtcMs = LocalDate.of(2026, 11, 14).atTime(9, 30)
                .toInstant(ZoneOffset.UTC).toEpochMilli()
        )
    }

    @After
    fun tearDown() {
        if (!::context.isInitialized) return
        RuntimeEnvironment.setFontScale(1.0f)
        tempDbNames.forEach { context.deleteDatabase(it) }
        tempDbNames.clear()
    }

    private fun seed(repo: DayseedRepository, withData: Boolean) {
        if (!withData) return
        val today = testClock.todayLocal()
        val start = today.minusDays(40)

        fun habit(id: String, name: String, mode: TrackingMode, unit: String?, target: Int, color: String) =
            Habit(
                habitId = id, name = name, description = "", trackingMode = mode, unitCode = unit,
                targetPeriod = TargetPeriod.DAY, targetValue = target,
                scheduleKind = ScheduleKind.DAILY, colorHex = color
            )

        repo.saveHabitSync(habit("h_read", "Read a book", TrackingMode.QUANTITY, "pages", 10, "#2E6F40"), effectiveDateOverride = start)
        repo.saveHabitSync(habit("h_walk", "Morning walk", TrackingMode.DURATION, "min", 30, "#3B6955"), effectiveDateOverride = start)
        repo.saveHabitSync(habit("h_meditate", "Meditate", TrackingMode.COMPLETION_ONLY, null, 1, "#6A8D48"), effectiveDateOverride = start)
        repo.saveHabitSync(habit("h_water", "Drink water", TrackingMode.QUANTITY, "glasses", 8, "#4A7C59"), effectiveDateOverride = start)

        // A varied history: strong stretch, a gap, recent consistency.
        for (d in 40 downTo 1) {
            val date = today.minusDays(d.toLong())
            val gap = d in 14..17
            if (!gap && d % 7 != 3) repo.logHabitEntrySync("h_read", date, if (d % 4 == 0) 6 else 10, null)
            if (!gap && d % 3 != 0) repo.logHabitEntrySync("h_walk", date, if (d % 5 == 0) 15 else 30, null)
            if (d <= 12 || d % 2 == 0) repo.logHabitDayOutcomeSync("h_meditate", date, "COMPLETED", null)
            if (!gap && d % 2 == 1) repo.logHabitEntrySync("h_water", date, 8, null)
        }
        // Today: partial progress on two habits, one untouched.
        repo.logHabitEntrySync("h_read", today, 6, "Evening page")
        repo.logHabitEntrySync("h_walk", today, 30, null)

        listOf(
            Triple(1, "A slow start", "Woke late but still took the walk. Felt lighter afterwards."),
            Triple(3, "Reading again", "Finished a chapter on the train. Quiet and good."),
            Triple(8, "Hard day", "Skipped most things. That is okay; tomorrow is a new page.")
        ).forEachIndexed { i, (ago, title, body) ->
            repo.saveJournalEntrySync(
                JournalEntry(
                    journalEntryId = "j_$i",
                    localDate = today.minusDays(ago.toLong()).toString(),
                    body = body, title = title
                )
            )
        }
    }

    private fun findComposeView(view: View): androidx.compose.ui.platform.ComposeView? {
        if (view is androidx.compose.ui.platform.ComposeView) return view
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) findComposeView(view.getChildAt(i))?.let { return it }
        }
        return null
    }

    private fun pump(activity: ComponentActivity, wDp: Int, hDp: Int) {
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(80))
        val density = activity.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        val decor = activity.window.decorView
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(wPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(hPx, View.MeasureSpec.EXACTLY)
        )
        decor.layout(0, 0, wPx, hPx)
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(80))
    }

    private fun shot(activity: ComponentActivity, name: String, wDp: Int, hDp: Int) {
        // Robolectric has no frame clock, so navigation state changes are not redrawn unless the
        // composition is recreated (same technique the existing UI tests use: recompose = true).
        findComposeView(activity.window.decorView)?.disposeComposition()
        pump(activity, wDp, hDp)
        pump(activity, wDp, hDp)
        val decor = activity.window.decorView
        val bmp = Bitmap.createBitmap(decor.width.coerceAtLeast(1), decor.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        decor.draw(Canvas(bmp))
        FileOutputStream(File(outDir, "$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Runs one rendering condition across the requested scenes. */
    private fun suite(
        prefix: String,
        wDp: Int,
        hDp: Int,
        dark: Boolean = false,
        fontScale: Float = 1.0f,
        withData: Boolean = true,
        scenes: List<String>
    ) {
        RuntimeEnvironment.setFontScale(fontScale)
        val dbName = "shots_${prefix}_${System.nanoTime()}.db"
        context.deleteDatabase(dbName)
        tempDbNames.add(dbName)
        val repo = DayseedRepository(context, clock = testClock, databaseName = dbName)
        seed(repo, withData)
        val vm = DayseedViewModel(application, customRepository = repo, ioDispatcher = Dispatchers.Unconfined)
        vm.setThemeMode(if (dark) ThemeModeOption.DARK else ThemeModeOption.LIGHT)

        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            val settings by vm.appSettings.collectAsState()
            DayseedTheme(
                themeMode = ThemeModeOption.fromSetting(settings["theme_mode"]?.valueText),
                localTime = LocalTime.of(9, 30),
                reducedMotion = true
            ) {
                DayseedApp(viewModel = vm, liveLocalTime = LocalTime.of(9, 30))
            }
        }
        pump(activity, wDp, hDp)

        val today = testClock.todayLocal()
        for (scene in scenes) {
            when (scene) {
                "today" -> vm.selectTab(NavTab.TODAY)
                "habits" -> vm.selectTab(NavTab.HABITS)
                "journal" -> vm.selectTab(NavTab.JOURNAL)
                "insights" -> vm.selectTab(NavTab.INSIGHTS)
                "settings" -> vm.selectTab(NavTab.SETTINGS)
                "new_habit" -> { vm.selectTab(NavTab.TODAY); vm.openAddHabit() }
                "habit_detail" -> { vm.selectTab(NavTab.HABITS); vm.openHabitDetail("h_read", today) }
                "record" -> { vm.selectTab(NavTab.TODAY); vm.openRecordSession("h_read", today) }
                "day_detail" -> { vm.selectTab(NavTab.INSIGHTS); vm.openDayDetail(today.minusDays(1)) }
                "grove" -> { vm.selectTab(NavTab.INSIGHTS); vm.openLivingGrove() }
                "journal_editor" -> { vm.selectTab(NavTab.JOURNAL); vm.openJournalEditor(forDate = today) }
            }
            vm.clearUserMessage() // drop the theme-change toast raised by test setup
            shot(activity, "${prefix}_$scene", wDp, hDp)
            if (scene in setOf("journal_editor")) vm.closeJournalEditor()
        }
    }

    private val allScenes = listOf(
        "today", "habits", "habit_detail", "record", "new_habit", "journal", "journal_editor",
        "insights", "day_detail", "grove", "settings"
    )

    @Test fun light_360() = suite("light360", 360, 800, scenes = allScenes)
    @Test fun dark_360() = suite("dark360", 360, 800, dark = true, scenes = allScenes)
    @Test fun small_320() = suite("small320", 320, 640, scenes = listOf("today", "new_habit", "record", "settings"))
    @Test fun largefont_360() = suite("font150", 360, 800, fontScale = 1.5f, scenes = listOf("today", "new_habit", "record", "habits", "settings"))
    @Test fun wide_411() = suite("wide411", 411, 891, scenes = listOf("today", "insights"))
    @Test fun tall_360() = suite("tall360", 360, 1700, scenes = listOf("today", "insights"))
    @Test fun firstrun_360() = suite("firstrun360", 360, 800, withData = false, scenes = listOf("today", "habits", "journal", "insights"))
}
