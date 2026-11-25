package com.example.dayseed

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.ui.components.SharedMonthCalendar
import com.example.dayseed.ui.components.TrendDay
import com.example.dayseed.ui.components.TrendsChartCard
import com.example.dayseed.data.HonestCalendarDateState
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** D-250: the Insights trends graph and the Journal's compact mood-highlighting calendar. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class Phase250ScreensTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 11, 14)

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
            decor.measure(View.MeasureSpec.makeMeasureSpec((360 * d).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec((1400 * d).toInt(), View.MeasureSpec.EXACTLY))
            decor.layout(0, 0, (360 * d).toInt(), (1400 * d).toInt())
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

    @Test
    fun trendsChart_summarisesMetDaysAndOpensADay() {
        var opened: LocalDate? = null
        val days = (13 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            when {
                back == 0 -> TrendDay(d, met = 2, partial = 1, scheduled = 4)
                back == 1 -> TrendDay(d, met = 0, partial = 0, scheduled = 0)
                else -> TrendDay(d, met = 3, partial = 0, scheduled = 3)
            }
        }
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) {
                TrendsChartCard(days = days, today = today, onSelectDay = { opened = it })
            }
        }
        layout(activity)
        val summary = node(activity, "insights_trends_summary")!!.config.getOrNull(SemanticsProperties.Text)!!.joinToString("") { it.text }
        // 12 full days x 3 + today 2 of 4 = 38 met of 40 scheduled; one day with nothing scheduled adds none.
        assertEquals("38 of 40 habit-days met", summary)
        val quietDay = node(activity, "trend_day_${today.minusDays(1)}")!!.config.getOrNull(SemanticsProperties.ContentDescription)!!.joinToString(" ")
        assertTrue(quietDay, quietDay.contains("nothing scheduled"))
        node(activity, "trend_day_$today")!!.config.getOrNull(androidx.compose.ui.semantics.SemanticsActions.OnClick)!!.action!!.invoke()
        assertEquals(today, opened)
    }

    @Test
    fun journalCalendar_isCompactAndHighlightsTheChosenMood() {
        val entries = listOf(
            JournalEntry("j1", "2026-11-10", "a", primaryMoodCode = "CALM", createdAtUtcMs = 1),
            JournalEntry("j2", "2026-11-12", "b", primaryMoodCode = "TIRED", createdAtUtcMs = 2),
            JournalEntry("j3", "2026-11-13", "c", primaryMoodCode = "CALM", createdAtUtcMs = 3)
        )
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            DayseedTheme(themeMode = ThemeModeOption.LIGHT, reducedMotion = true) {
                SharedMonthCalendar(
                    displayedMonth = YearMonth.of(2026, 11), selectedDate = today, referenceToday = today,
                    weekStartDay = DayOfWeek.MONDAY, habitsForMonth = emptyList(), selectedHabitFilterId = null,
                    evaluateDateStateForHabit = { _, _ -> HonestCalendarDateState.UNSCHEDULED_DATE },
                    journalEntries = entries, onPreviousMonth = {}, onNextMonth = {}, onJumpToToday = {}, onSelectDate = {},
                    testTagPrefix = "journal_date_jump", journalMode = true, highlightMoodCode = "CALM"
                )
            }
        }
        layout(activity)
        fun state(date: String) = node(activity, "journal_date_jump_date_$date")!!.config.getOrNull(SemanticsProperties.StateDescription)
        assertEquals("Highlighted: Calm recorded", state("2026-11-10"))
        assertEquals("Highlighted: Calm recorded", state("2026-11-13"))
        assertNull("A day with a different mood is not highlighted", state("2026-11-12"))
        assertNull("A day without entries is not highlighted", state("2026-11-11"))
        assertNull("The Journal calendar shows no habit legend", node(activity, "journal_date_jump_legend"))
        assertNotNull(node(activity, "journal_date_jump_mood_2026-11-12"))
    }
}
