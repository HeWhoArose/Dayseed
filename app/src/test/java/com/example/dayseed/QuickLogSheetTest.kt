package com.example.dayseed

import android.app.Application
import android.content.Context
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
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.ui.components.HabitProgress
import com.example.dayseed.ui.components.QuickLogSheetContent
import com.example.dayseed.ui.theme.DayseedTheme
import com.example.dayseed.ui.theme.ThemeModeOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigInteger

/** Renders the quick-log sheet content directly and checks its controls and callbacks. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class QuickLogSheetTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun findRoot(v: View): RootForTest? {
        if (v is RootForTest) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) findRoot(v.getChildAt(i))?.let { return it }
        return null
    }

    private fun layout(activity: ComponentActivity) {
        repeat(2) {
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(80))
            val d = activity.resources.displayMetrics.density
            val decor = activity.window.decorView
            decor.measure(
                View.MeasureSpec.makeMeasureSpec((360 * d).toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec((900 * d).toInt(), View.MeasureSpec.EXACTLY)
            )
            decor.layout(0, 0, (360 * d).toInt(), (900 * d).toInt())
        }
    }

    private fun node(activity: ComponentActivity, tag: String): SemanticsNode? {
        val root = findRoot(activity.window.decorView) ?: return null
        fun walk(n: SemanticsNode): SemanticsNode? {
            if (n.config.getOrNull(SemanticsProperties.TestTag) == tag) return n
            for (c in n.children) walk(c)?.let { return it }
            return null
        }
        return walk(root.semanticsOwner.unmergedRootSemanticsNode)
    }

    private fun show(progress: HabitProgress, onComplete: (Int) -> Unit, onLog: (Int) -> Unit): ComponentActivity {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) {
                QuickLogSheetContent(habitName = "Read", progress = progress, onComplete = onComplete, onLogAmount = onLog)
            }
        }
        layout(activity)
        return activity
    }

    @Test
    fun belowGoal_offersCompleteWithRemainingAmountAndAmountEntry() {
        var completed = -1
        val p = HabitProgress.build(10, BigInteger.valueOf(6), "pages", isWeekly = false, isSessionCount = false)
        val a = show(p, { completed = it }, {})
        assertNotNull(node(a, "quick_log_sheet"))
        assertNotNull(node(a, "quick_log_bar"))
        assertNotNull(node(a, "quick_log_amount_input"))
        assertNotNull(node(a, "quick_log_save_btn"))
        val complete = node(a, "quick_log_complete_btn")
        assertNotNull("Complete must be offered below goal", complete)
        complete!!.config.getOrNull(SemanticsActions.OnClick)!!.action!!.invoke()
        assertEquals("Complete adds exactly what is left", 4, completed)
    }

    @Test
    fun goalReached_hidesCompleteButStillAllowsLoggingMore() {
        val p = HabitProgress.build(10, BigInteger.valueOf(12), "pages", isWeekly = false, isSessionCount = false)
        val a = show(p, {}, {})
        assertFalse("Complete is pointless once the goal is met", node(a, "quick_log_complete_btn") != null)
        assertTrue(node(a, "quick_log_amount_input") != null)
        assertTrue(node(a, "quick_log_save_btn") != null)
    }
}
