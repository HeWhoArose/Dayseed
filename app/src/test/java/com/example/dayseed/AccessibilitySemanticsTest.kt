package com.example.dayseed

import android.app.Application
import android.content.Context
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.Habit
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.components.DayseedExpandableCard
import com.example.dayseed.ui.theme.DayseedTheme
import com.example.dayseed.ui.theme.ThemeModeOption
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
 * Code-level accessibility regression checks (headings, roles, state descriptions).
 * These inspect the Compose semantics tree; they do NOT replace TalkBack testing on a device.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AccessibilitySemanticsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var clock: TestDayseedClock
    private val dbNames = mutableListOf<String>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        clock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 11, 14),
            currentLocalTime = LocalTime.of(9, 30),
            currentUtcMs = LocalDate.of(2026, 11, 14).atTime(9, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        )
    }

    @After
    fun tearDown() {
        dbNames.forEach { context.deleteDatabase(it) }
    }

    private fun findComposeView(v: View): androidx.compose.ui.platform.ComposeView? {
        if (v is androidx.compose.ui.platform.ComposeView) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) findComposeView(v.getChildAt(i))?.let { return it }
        return null
    }

    private fun findRoot(v: View): RootForTest? {
        if (v is RootForTest) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) findRoot(v.getChildAt(i))?.let { return it }
        return null
    }

    private fun pump(activity: ComponentActivity, recompose: Boolean) {
        if (recompose) findComposeView(activity.window.decorView)?.disposeComposition()
        repeat(2) {
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(80))
            val d = activity.resources.displayMetrics.density
            val w = (360 * d).toInt(); val h = (900 * d).toInt()
            val decor = activity.window.decorView
            decor.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            decor.layout(0, 0, w, h)
        }
    }

    private fun nodes(activity: ComponentActivity): List<SemanticsNode> {
        val root = findRoot(activity.window.decorView) ?: return emptyList()
        val out = mutableListOf<SemanticsNode>()
        fun walk(n: SemanticsNode) { out.add(n); n.children.forEach { walk(it) } }
        walk(root.semanticsOwner.unmergedRootSemanticsNode)
        return out
    }

    private fun headings(activity: ComponentActivity): List<String> =
        nodes(activity).filter { it.config.contains(SemanticsProperties.Heading) }
            .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { t -> t.text } }

    @Test
    fun everyTopLevelScreenExposesAtLeastOneHeading() {
        val db = "a11y_${System.nanoTime()}.db"; dbNames.add(db); context.deleteDatabase(db)
        val repo = DayseedRepository(context, clock = clock, databaseName = db)
        repo.saveHabitSync(
            Habit(habitId = "h1", name = "Read", description = "", trackingMode = TrackingMode.QUANTITY, unitCode = "pages",
                targetPeriod = TargetPeriod.DAY, targetValue = 10, scheduleKind = ScheduleKind.DAILY, colorHex = "#2E6F40"),
            effectiveDateOverride = clock.todayLocal().minusDays(5)
        )
        val vm = DayseedViewModel(application, customRepository = repo, ioDispatcher = Dispatchers.Unconfined)
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            DayseedTheme(themeMode = ThemeModeOption.LIGHT, localTime = LocalTime.of(9, 30), reducedMotion = true) {
                DayseedApp(viewModel = vm, liveLocalTime = LocalTime.of(9, 30))
            }
        }
        for (tab in NavTab.values()) {
            vm.selectTab(tab)
            pump(activity, recompose = true)
            val found = headings(activity)
            assertTrue("Tab $tab must expose at least one heading for screen-reader navigation, found: $found", found.isNotEmpty())
        }
        vm.selectTab(NavTab.HABITS)
        vm.openHabitDetail("h1", clock.todayLocal())
        pump(activity, recompose = true)
        assertTrue("Habit Detail must expose a heading, found: ${headings(activity)}", headings(activity).any { it.contains("Habit Detail") })
    }

    private fun expandableCardNode(expanded: Boolean): SemanticsNode {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) {
                DayseedExpandableCard(
                    title = "Streak & Living Grove", summary = "12 days", expanded = expanded,
                    onToggle = {}, toggleTestTag = "toggle"
                ) { Text("inner") }
            }
        }
        pump(activity, recompose = false)
        val node = nodes(activity).firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == "toggle" }
        assertNotNull("toggle node must exist", node)
        return node!!
    }

    @Test
    fun expandableCardAnnouncesButtonRoleAndExpandedOrCollapsedState() {
        val collapsed = expandableCardNode(expanded = false)
        assertEquals(Role.Button, collapsed.config.getOrNull(SemanticsProperties.Role))
        assertEquals("Collapsed", collapsed.config.getOrNull(SemanticsProperties.StateDescription))
        val expanded = expandableCardNode(expanded = true)
        assertEquals(Role.Button, expanded.config.getOrNull(SemanticsProperties.Role))
        assertEquals("Expanded", expanded.config.getOrNull(SemanticsProperties.StateDescription))
    }

    @Test
    fun expandableCardTitleIsAHeading() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) {
                DayseedExpandableCard(title = "Lifetime Progress", summary = "", expanded = false, onToggle = {}) { Text("x") }
            }
        }
        pump(activity, recompose = false)
        assertTrue(headings(activity).any { it.contains("Lifetime Progress") })
    }
}
