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
import androidx.compose.ui.text.AnnotatedString
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.ui.screens.JournalEditorDialog
import com.example.dayseed.ui.theme.DayseedTheme
import com.example.dayseed.ui.theme.ThemeModeOption
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** The reflection editor rendered inline (no pop-up window), so its controls can be inspected and driven. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class JournalEditorTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val day = LocalDate.of(2026, 11, 14)

    private fun findRoot(v: View): RootForTest? {
        if (v is RootForTest) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) findRoot(v.getChildAt(i))?.let { return it }
        return null
    }

    private fun layout(activity: ComponentActivity, wDp: Int = 360, hDp: Int = 1400) {
        repeat(2) {
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(80))
            val d = activity.resources.displayMetrics.density
            val decor = activity.window.decorView
            decor.measure(
                View.MeasureSpec.makeMeasureSpec((wDp * d).toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec((hDp * d).toInt(), View.MeasureSpec.EXACTLY)
            )
            decor.layout(0, 0, (wDp * d).toInt(), (hDp * d).toInt())
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

    private fun typeInto(activity: ComponentActivity, tag: String, text: String) {
        val n = node(activity, tag)
        assertNotNull("Expected '$tag'", n)
        n!!.config.getOrNull(SemanticsActions.SetText)!!.action!!.invoke(AnnotatedString(text))
        layout(activity)
    }

    private fun show(
        entry: JournalEntry?,
        onSave: (JournalEntry, String?) -> Unit = { _, _ -> },
        onDelete: (String) -> Unit = {},
        dark: Boolean = false
    ): ComponentActivity {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            DayseedTheme(themeMode = if (dark) ThemeModeOption.DARK else ThemeModeOption.LIGHT, reducedMotion = true) {
                JournalEditorDialog(
                    initialEntry = entry,
                    selectedDate = day,
                    habits = emptyList(),
                    asWindow = false,
                    clockNowMs = { 1_000L },
                    onDismiss = {},
                    onSave = onSave,
                    onDelete = onDelete
                )
            }
        }
        layout(activity)
        return activity
    }

    @Test
    fun newReflection_hasOneMoodRow_noOldChipStrips_andSavesTheBody() {
        var saved: JournalEntry? = null
        val a = show(entry = null, onSave = { e, _ -> saved = e })
        assertNotNull(node(a, "journal_editor_dialog"))
        assertNotNull("One mood row opens the emoji pop-up", node(a, "journal_mood_row"))
        assertNull("The old primary-mood chip strip is gone", node(a, "primary_mood_CALM"))
        assertNull("The old secondary-mood chip strip is gone", node(a, "secondary_mood_CALM"))
        assertEquals("Draft", node(a, "journal_editor_status_badge")!!.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text })
        assertNull("No delete for a new entry", node(a, "journal_delete_entry_button"))

        click(a, "journal_save_button")
        assertNotNull("An empty body must be refused", node(a, "journal_body_error_text"))
        assertNull(saved)

        typeInto(a, "journal_content_input", "A calm evening.")
        click(a, "journal_save_button")
        assertEquals("A calm evening.", saved?.body)
        assertEquals("2026-11-14", saved?.localDate)
    }

    @Test
    fun savedEntry_showsSavedBadgeAndDeleteAsksFirst() {
        var deleted: String? = null
        val entry = JournalEntry(journalEntryId = "j1", localDate = "2026-11-14", body = "Walked.", title = "Walk", primaryMoodCode = "CALM")
        val a = show(entry = entry, onDelete = { deleted = it })
        assertEquals("Saved Entry", node(a, "journal_editor_status_badge")!!.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text })
        click(a, "journal_delete_entry_button")
        // The confirmation is a separate dialog window, so deletion must not have happened yet.
        assertNull("Delete must be confirmed first", deleted)
    }

    /** Opt-in render for visual review: set DAYSEED_SCREENSHOT_DIR. Never asserts. */
    @Test
    fun renderEditorForReview() {
        val dir = System.getenv("DAYSEED_SCREENSHOT_DIR") ?: return
        val entry = JournalEntry(journalEntryId = "j1", localDate = "2026-11-14", body = "Finished a chapter on the train. Quiet and good.", title = "Reading again", primaryMoodCode = "CALM", secondaryMoodCode = "GRATEFUL")
        for ((name, dark, font, w) in listOf(
            Quad("light", false, 1.0f, 360), Quad("dark", true, 1.0f, 360), Quad("font150", false, 1.5f, 360), Quad("small320", false, 1.0f, 320)
        )) {
            RuntimeEnvironment.setFontScale(font)
            val a = show(entry = entry, dark = dark)
            layout(a, w, 1100)
            val decor = a.window.decorView
            val bmp = android.graphics.Bitmap.createBitmap(decor.width.coerceAtLeast(1), decor.height.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
            decor.draw(android.graphics.Canvas(bmp))
            java.io.FileOutputStream(java.io.File(dir, "editor_$name.png")).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
        RuntimeEnvironment.setFontScale(1.0f)
    }

    private data class Quad(val a: String, val b: Boolean, val c: Float, val d: Int)
}
