package com.example.dayseed

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.Habit
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.screens.AboutPanel
import com.example.dayseed.ui.screens.AppearancePanel
import com.example.dayseed.ui.screens.PrivacyPanel
import com.example.dayseed.ui.screens.NotificationsPanel
import com.example.dayseed.ui.screens.RemovedHabitsPanel
import com.example.dayseed.ui.theme.DayseedTheme
import com.example.dayseed.ui.theme.ThemeModeOption
import kotlinx.coroutines.Dispatchers
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

/** Opt-in render of the Settings panels (normally inside a bottom sheet) for visual review. Never asserts. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SettingsPanelsRenderTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun renderPanels() {
        val dir = System.getenv("DAYSEED_SCREENSHOT_DIR") ?: return
        val context: Context = ApplicationProvider.getApplicationContext()
        val clock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 11, 14), currentLocalTime = LocalTime.of(9, 30),
            currentUtcMs = LocalDate.of(2026, 11, 14).atTime(9, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        val repo = DayseedRepository(context, clock = clock, databaseName = "panels_${System.nanoTime()}.db")
        repo.saveHabitSync(
            Habit(habitId = "h1", name = "Read", description = "", trackingMode = TrackingMode.COMPLETION_ONLY,
                targetPeriod = TargetPeriod.DAY, targetValue = 1, scheduleKind = ScheduleKind.DAILY),
            effectiveDateOverride = LocalDate.of(2026, 10, 1)
        )
        val vm = DayseedViewModel(context as Application, customRepository = repo, ioDispatcher = Dispatchers.Unconfined)
        val panels: List<Pair<String, @androidx.compose.runtime.Composable () -> Unit>> = listOf(
            "appearance" to { AppearancePanel(vm, ThemeModeOption.SOFT_BOTANICAL_AUTO) },
            "notifications" to { NotificationsPanel(vm) },
            "privacy" to { PrivacyPanel() },
            "about" to { AboutPanel() },
            "removed" to { RemovedHabitsPanel(vm, emptyList()) }
        )
        for ((name, panel) in panels) {
            val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
            activity.setContent {
                DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) {
                    Column(Modifier.padding(horizontal = 20.dp)) { panel() }
                }
            }
            repeat(2) {
                androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
                Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(80))
                val d = activity.resources.displayMetrics.density
                val decor = activity.window.decorView
                decor.measure(View.MeasureSpec.makeMeasureSpec((360 * d).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec((2400 * d).toInt(), View.MeasureSpec.EXACTLY))
                decor.layout(0, 0, (360 * d).toInt(), (2400 * d).toInt())
            }
            val decor = activity.window.decorView
            val bmp = Bitmap.createBitmap(decor.width.coerceAtLeast(1), decor.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(bmp))
            FileOutputStream(File(dir, "panel_$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        repo.dbHelper.close()
    }
}
