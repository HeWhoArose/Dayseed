package com.example.dayseed

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
import com.example.dayseed.data.MoodCatalog
import com.example.dayseed.ui.components.MoodPickerContent
import com.example.dayseed.ui.theme.DayseedTheme
import com.example.dayseed.ui.theme.ThemeModeOption
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The emoji mood pop-up: main feeling first, optional second, tap again to clear. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class MoodPickerTest {

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
                View.MeasureSpec.makeMeasureSpec((1200 * d).toInt(), View.MeasureSpec.EXACTLY)
            )
            decor.layout(0, 0, (360 * d).toInt(), (1200 * d).toInt())
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

    private fun click(activity: ComponentActivity, tag: String) {
        val n = node(activity, tag)
        assertNotNull("Expected '$tag'", n)
        n!!.config.getOrNull(SemanticsActions.OnClick)!!.action!!.invoke()
        layout(activity)
    }

    @Test
    fun pickerChoosesMainThenOptionalSecondAndCanClear() {
        var result: Pair<String?, String?>? = null
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) {
                MoodPickerContent(initialPrimary = null, initialSecondary = null, onConfirm = { p, s -> result = p to s })
            }
        }
        layout(activity)

        // Every offered mood is an emoji cell; the legacy duplicate spelling is not offered.
        MoodCatalog.pickerMoods.forEach { assertNotNull("Cell for ${it.code}", node(activity, "primary_mood_${it.code}")) }
        assertNull("Legacy duplicate 'Energized' must not be offered", node(activity, "primary_mood_ENERGIZED"))
        assertNull("No second row before a main feeling is chosen", node(activity, "secondary_mood_CALM"))
        assertEquals("Emoji must be unique across the offered moods", MoodCatalog.pickerMoods.size, MoodCatalog.pickerMoods.map { it.symbol }.toSet().size)

        click(activity, "primary_mood_CALM")
        assertNotNull("Second row appears after the main feeling", node(activity, "secondary_mood_GRATEFUL"))
        assertNull("The main feeling is not offered again as the second", node(activity, "secondary_mood_CALM"))
        click(activity, "secondary_mood_GRATEFUL")
        click(activity, "mood_picker_done_btn")
        assertEquals("CALM" to "GRATEFUL", result)

        click(activity, "primary_mood_CALM") // tapping the main feeling again clears both
        click(activity, "mood_picker_done_btn")
        assertEquals(null to null, result)
        assertTrue(node(activity, "mood_picker_clear_btn") == null)
    }

    /** Opt-in render for visual review: set DAYSEED_SCREENSHOT_DIR. Never asserts. */
    @Test
    fun renderPickerForReview() {
        val dir = System.getenv("DAYSEED_SCREENSHOT_DIR") ?: return
        for (dark in listOf(false, true)) {
            val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
            activity.setContent {
                DayseedTheme(themeMode = if (dark) ThemeModeOption.DARK else ThemeModeOption.LIGHT, reducedMotion = true) {
                    androidx.compose.foundation.layout.Column(
                        androidx.compose.ui.Modifier.padding(16.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                    ) {
                        com.example.dayseed.ui.components.MoodSelectorRow(null, null, {})
                        com.example.dayseed.ui.components.MoodSelectorRow("CALM", "GRATEFUL", {})
                        MoodPickerContent(initialPrimary = "CALM", initialSecondary = null, onConfirm = { _, _ -> })
                    }
                }
            }
            layout(activity)
            val decor = activity.window.decorView
            val bmp = android.graphics.Bitmap.createBitmap(decor.width.coerceAtLeast(1), decor.height.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
            decor.draw(android.graphics.Canvas(bmp))
            java.io.FileOutputStream(java.io.File(dir, "mood_picker_${if (dark) "dark" else "light"}.png")).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
