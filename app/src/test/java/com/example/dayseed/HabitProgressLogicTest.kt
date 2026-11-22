package com.example.dayseed

import com.example.dayseed.ui.components.barLayers
import com.example.dayseed.ui.components.progressFraction
import com.example.dayseed.ui.components.HabitProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class HabitProgressLogicTest {
    private fun big(n: Int) = BigInteger.valueOf(n.toLong())

    // ---- bar layers: the first lap, then a second lap drawn over it --------------------------------------

    @Test fun belowTargetFillsOnlyTheFirstLap() {
        val l = barLayers(0.6f)
        assertEquals(0.6f, l.base, 0.0001f); assertEquals(0f, l.overflow, 0.0001f); assertFalse(l.isOver)
    }

    @Test fun exactlyTheTargetFillsTheFirstLapWithNoOverflow() {
        val l = barLayers(1f)
        assertEquals(1f, l.base, 0.0001f); assertEquals(0f, l.overflow, 0.0001f); assertFalse(l.isOver)
    }

    @Test fun beyondTheTargetDrawsASecondLapOverTheFirst() {
        val l = barLayers(1.5f)
        assertEquals(1f, l.base, 0.0001f); assertEquals(0.5f, l.overflow, 0.0001f); assertTrue(l.isOver)
    }

    @Test fun overflowStopsAtAFullSecondLap() {
        val l = barLayers(3.2f)
        assertEquals(1f, l.overflow, 0.0001f); assertTrue(l.isOver)
    }

    @Test fun invalidInputIsTreatedAsZero() {
        assertEquals(0f, barLayers(-0.4f).base, 0f); assertEquals(0f, barLayers(Float.NaN).base, 0f)
    }

    @Test fun fractionHandlesZeroTargetAndOverTarget() {
        assertEquals(0f, progressFraction(big(5), big(0)), 0f)
        assertEquals(1.2f, progressFraction(big(12), big(10)), 0.0001f)
    }

    // ---- remaining / preview ----------------------------------------------------------------------------

    private fun p(current: Int, target: Int = 10, unit: String = "pages") =
        HabitProgress.build(target, big(current), unit, isWeekly = false, isSessionCount = false)

    @Test fun remainingIsWhatIsLeftToReachTheGoal() {
        assertEquals(4, p(6).remainingUnits)
        assertEquals(10, p(0).remainingUnits)
    }

    @Test fun remainingIsZeroOnceTheGoalIsReached() {
        assertEquals(0, p(10).remainingUnits); assertEquals(0, p(14).remainingUnits)
        assertTrue(p(10).isGoalReached)
    }

    @Test fun completeAddsExactlyTheRemainingAmount() {
        val start = p(6)
        val done = start.after(start.remainingUnits)
        assertTrue(done.isGoalReached); assertEquals("10 / 10 pages", done.ratioText); assertNull(done.overText())
    }

    @Test fun addingLessLeavesPartialProgress() {
        val r = p(6).after(2)
        assertEquals(80, r.percent); assertFalse(r.isGoalReached); assertEquals("8 / 10 pages", r.ratioText)
    }

    @Test fun addingMoreShowsHowFarOverTheTargetAndFillsASecondLap() {
        val r = p(6).after(7)   // 13 of 10
        assertTrue(r.isGoalReached); assertEquals(130, r.percent)
        assertEquals("3 pages over", r.overText())
        val layers = barLayers(r.fraction)
        assertEquals(1f, layers.base, 0.0001f); assertEquals(0.3f, layers.overflow, 0.0001f); assertTrue(layers.isOver)
    }

    @Test fun sessionCountHabitsCompleteWithOneMoreSession() {
        val s = HabitProgress.build(3, big(1), "sessions", isWeekly = true, isSessionCount = true)
        assertEquals(1, s.remainingUnits)
    }

    @Test fun overUnitsIsZeroUnlessOver() {
        assertEquals(BigInteger.ZERO, p(10).overUnits); assertEquals(BigInteger.valueOf(4), p(14).overUnits)
    }

    @Test fun fractionalUnitsKeepTheirPrecisionOnTheOverflowText() {
        // 1.5 litres stored at scale 2 = 150; add 0.75 L (75) to 1.0 L (100) -> 1.75 L, 0.25 L over.
        val litres = HabitProgress.build(150, big(100), "litres@scale2", isWeekly = false, isSessionCount = false)
        val r = litres.after(75)
        assertTrue(r.isGoalReached)
        assertEquals("0.25 litres over", r.overText())
    }
}
