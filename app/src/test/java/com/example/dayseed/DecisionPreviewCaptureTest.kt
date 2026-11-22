package com.example.dayseed

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.Habit
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.theme.DayseedDateStyle
import com.example.dayseed.ui.theme.DayseedGreetingStyle
import com.example.dayseed.ui.theme.DayseedTheme
import com.example.dayseed.ui.theme.SoftBotanicalThemeConfig
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Review aid for two pending owner decisions. Skipped unless DAYSEED_SCREENSHOT_DIR is set.
 *
 *  1. Evening-theme sage text colour: the real app rendered with the old (#6B7B5C) and new (#606F53) value.
 *  2. A MOCK of compact one-tap Today habit rows (proposal only, test-local, built from the app's own tokens),
 *     to compare against the current two-button habit cards. It is not wired into the product.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DecisionPreviewCaptureTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var clock: TestDayseedClock
    private val dbs = mutableListOf<String>()
    private var out: File? = null

    @Before
    fun setUp() {
        val dir = System.getenv("DAYSEED_SCREENSHOT_DIR")
        assumeTrue("DAYSEED_SCREENSHOT_DIR not set; preview capture skipped", !dir.isNullOrBlank())
        out = File(dir!!).also { it.mkdirs() }
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        clock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 11, 14),
            currentLocalTime = LocalTime.of(19, 0),
            currentUtcMs = LocalDate.of(2026, 11, 14).atTime(19, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        )
    }

    @After
    fun tearDown() {
        if (!::context.isInitialized) return
        dbs.forEach { context.deleteDatabase(it) }
    }

    // ---- shared render helpers -------------------------------------------------------------------------

    private fun findComposeView(v: View): androidx.compose.ui.platform.ComposeView? {
        if (v is androidx.compose.ui.platform.ComposeView) return v
        if (v is android.view.ViewGroup) for (i in 0 until v.childCount) findComposeView(v.getChildAt(i))?.let { return it }
        return null
    }

    private fun shot(activity: ComponentActivity, name: String, wDp: Int, hDp: Int) {
        findComposeView(activity.window.decorView)?.disposeComposition()
        repeat(3) {
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(80))
            val d = activity.resources.displayMetrics.density
            val w = (wDp * d).toInt(); val h = (hDp * d).toInt()
            val decor = activity.window.decorView
            decor.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            decor.layout(0, 0, w, h)
        }
        val decor = activity.window.decorView
        val bmp = Bitmap.createBitmap(decor.width.coerceAtLeast(1), decor.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        decor.draw(AndroidCanvas(bmp))
        FileOutputStream(File(out, "$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    // ---- Decision 2: Evening sage, old vs new on the real app ------------------------------------------

    private fun seededVm(): DayseedViewModel {
        val db = "preview_${System.nanoTime()}.db"; dbs.add(db); context.deleteDatabase(db)
        val repo = DayseedRepository(context, clock = clock, databaseName = db)
        val today = clock.todayLocal()
        fun habit(id: String, name: String, mode: TrackingMode, unit: String?, target: Int, color: String) = Habit(
            habitId = id, name = name, description = "", trackingMode = mode, unitCode = unit,
            targetPeriod = TargetPeriod.DAY, targetValue = target, scheduleKind = ScheduleKind.DAILY, colorHex = color
        )
        repo.saveHabitSync(habit("h_read", "Read a book", TrackingMode.QUANTITY, "pages", 10, "#2E6F40"), effectiveDateOverride = today.minusDays(20))
        repo.saveHabitSync(habit("h_walk", "Morning walk", TrackingMode.DURATION, "min", 30, "#3B6955"), effectiveDateOverride = today.minusDays(20))
        repo.saveHabitSync(habit("h_meditate", "Meditate", TrackingMode.COMPLETION_ONLY, null, 1, "#6A8D48"), effectiveDateOverride = today.minusDays(20))
        for (d in 20 downTo 1) {
            val date = today.minusDays(d.toLong())
            if (d % 4 != 0) repo.logHabitEntrySync("h_read", date, 10, null)
            if (d % 3 != 0) repo.logHabitEntrySync("h_walk", date, 30, null)
            repo.logHabitDayOutcomeSync("h_meditate", date, "COMPLETED", null)
        }
        repo.logHabitEntrySync("h_read", today, 6, null)
        repo.logHabitEntrySync("h_walk", today, 30, null)
        // An unsaved draft makes the Journal show the "Unsaved Drafts" heading, which uses the Evening sage as text.
        repo.saveJournalDraftSync(body = "Thinking about how the week went, and what to carry into the next one.", title = "Sunday notes")
        return DayseedViewModel(application, customRepository = repo, ioDispatcher = Dispatchers.Unconfined)
    }

    private fun eveningShot(name: String, tab: NavTab, secondaryOverride: Color?) {
        val vm = seededVm()
        vm.setThemeMode(ThemeModeOption.SOFT_BOTANICAL_AUTO)
        vm.selectTab(tab)
        Thread.sleep(400) // let the asynchronous "theme set" message arrive, then drop it so it is not drawn
        vm.clearUserMessage()
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val evening = LocalTime.of(19, 0)
        activity.setContent {
            val settings by vm.appSettings.collectAsState()
            DayseedTheme(
                themeMode = ThemeModeOption.fromSetting(settings["theme_mode"]?.valueText),
                localTime = evening,
                reducedMotion = true
            ) {
                if (secondaryOverride == null) {
                    DayseedApp(viewModel = vm, liveLocalTime = evening)
                } else {
                    MaterialTheme(
                        colorScheme = MaterialTheme.colorScheme.copy(secondary = secondaryOverride),
                        typography = MaterialTheme.typography,
                        shapes = MaterialTheme.shapes
                    ) { DayseedApp(viewModel = vm, liveLocalTime = evening) }
                }
            }
        }
        shot(activity, name, 360, 800)
    }

    @Test
    fun eveningSage_oldVsNew_realScreens() {
        val old = Color(0xFF6B7B5C)
        eveningShot("evening_journal_OLD", NavTab.JOURNAL, old)
        eveningShot("evening_journal_NEW", NavTab.JOURNAL, null)
        eveningShot("evening_today_OLD", NavTab.TODAY, old)
        eveningShot("evening_today_NEW", NavTab.TODAY, null)
    }

    private fun contrast(a: Color, b: Color): Double {
        fun lin(c: Float): Double { val x = c.toDouble(); return if (x <= 0.03928) x / 12.92 else Math.pow((x + 0.055) / 1.055, 2.4) }
        fun lum(c: Color) = 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
        val la = lum(a); val lb = lum(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Composable
    private fun SwatchRow(label: String, color: Color, surface: Color) {
        val ratio = contrast(color, surface)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF444444))
            Text("Unsaved Drafts (1)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Text("1 explicit 0-minute check-in session (contributed 0m to total)", style = MaterialTheme.typography.labelSmall, color = color)
            Text(String.format("contrast on the Evening card: %.2f:1  %s", ratio, if (ratio >= 4.5) "meets 4.5:1" else "BELOW 4.5:1"),
                style = MaterialTheme.typography.labelSmall, color = Color(0xFF444444))
        }
    }

    @Test
    fun eveningSage_swatchStrip() {
        val surface = SoftBotanicalThemeConfig.EveningSurface
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) {
                Column(Modifier.fillMaxSize().background(surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    SwatchRow("BEFORE  #6B7B5C", Color(0xFF6B7B5C), surface)
                    SwatchRow("AFTER  #606F53 (proposed, same sage hue, 10% darker)", Color(0xFF606F53), surface)
                }
            }
        }
        shot(activity, "evening_swatches", 360, 330)
    }

    // ---- Decision 1: compact one-tap Today rows (MOCK) --------------------------------------------------

    private data class HabitRowSpec(val name: String, val subtitle: String, val fraction: Float?, val done: Boolean, val quantity: Boolean, val dot: Color)

    @Composable
    private fun CompactRow(r: HabitRowSpec) {
        val scheme = MaterialTheme.colorScheme
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = scheme.surface,
            border = BorderStroke(1.dp, if (r.done) scheme.primary.copy(alpha = 0.35f) else scheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(r.dot))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(r.name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), fontWeight = FontWeight.Bold, color = scheme.onSurface)
                    Text(r.subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    if (r.fraction != null) {
                        LinearProgressIndicator(
                            progress = { r.fraction },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(999.dp)),
                            color = scheme.primary,
                            trackColor = scheme.primary.copy(alpha = 0.14f)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                // One clear action on the right: a 48dp round control (ring = progress, tick = done, plus = add).
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    val ring = scheme.outline
                    val primary = scheme.primary
                    Canvas(Modifier.size(34.dp)) {
                        val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        if (r.done) {
                            drawCircle(primary)
                        } else {
                            drawCircle(ring.copy(alpha = if (r.fraction != null && r.fraction > 0f) 0.45f else 1f), style = stroke)
                            if (r.fraction != null && r.fraction > 0f) {
                                val inset = stroke.width / 2
                                drawArc(primary, -90f, 360f * r.fraction, false, topLeft = Offset(inset, inset),
                                    size = Size(size.width - stroke.width, size.height - stroke.width), style = stroke)
                            }
                        }
                    }
                    if (r.done) Icon(Icons.Default.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(20.dp))
                    else if (r.quantity) Icon(Icons.Default.Add, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    @Composable
    private fun CompactTodayMock() {
        val scheme = MaterialTheme.colorScheme
        val rows = listOf(
            HabitRowSpec("Meditate", "Every day · 12-day streak", null, false, false, Color(0xFF6A8D48)),
            HabitRowSpec("Read a book", "6 / 10 pages · Halfway · 2-day streak", 0.6f, false, true, Color(0xFF2E6F40)),
            HabitRowSpec("Morning walk", "30 / 30 min · Goal reached", 1f, true, true, Color(0xFF3B6955)),
            HabitRowSpec("Drink water", "0 / 8 glasses · 1-day streak", 0f, false, true, Color(0xFF4A7C59))
        )
        Column(Modifier.fillMaxSize().background(scheme.background).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                Text("Saturday, November 14", style = DayseedDateStyle, color = scheme.primary)
                Text("Good morning", style = DayseedGreetingStyle, color = scheme.onBackground)
                Text("Your progress is more than your streak.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(18.dp), color = scheme.primaryContainer.copy(alpha = 0.28f),
                border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.30f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Today's progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("2 of 4 completed", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = scheme.primary)
                    }
                    LinearProgressIndicator(progress = { 0.5f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                        color = scheme.primary, trackColor = scheme.primary.copy(alpha = 0.14f))
                    Text("Steady momentum — one step at a time.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
            Text("Today's Habits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onBackground)
            rows.forEach { CompactRow(it) }
            Surface(shape = RoundedCornerShape(14.dp), color = scheme.surface, border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f)), modifier = Modifier.fillMaxWidth()) {
                Text("Not scheduled today (1)", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.padding(14.dp))
            }
        }
    }

    @Test
    fun compactRows_mock() {
        for ((name, mode) in listOf("compact_today_LIGHT" to ThemeModeOption.LIGHT, "compact_today_DARK" to ThemeModeOption.DARK)) {
            val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
            activity.setContent { DayseedTheme(themeMode = mode, reducedMotion = true) { CompactTodayMock() } }
            shot(activity, name, 360, 800)
        }
    }
}
