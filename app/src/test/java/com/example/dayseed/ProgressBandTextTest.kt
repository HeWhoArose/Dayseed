package com.example.dayseed

import com.example.dayseed.data.ProgressBand
import com.example.dayseed.ui.components.spokenDescription
import com.example.dayseed.ui.components.spokenRange
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Owner decision (option B): the visible band chip shows the label only; the range is spoken in plain words.
 * These tests keep the spoken text consistent with the strict D-040 `shortBadge` boundaries.
 */
class ProgressBandTextTest {

    @Test
    fun spokenRangeUsesWordsNotSymbols() {
        for (band in ProgressBand.values()) {
            val spoken = band.spokenRange()
            assertFalse("${band.name} must not contain '<' or '>': $spoken", spoken.contains('<') || spoken.contains('>'))
            assertFalse("${band.name} must not contain a range dash: $spoken", spoken.contains('–') || spoken.contains('-'))
            assertFalse("${band.name} must not contain a percent sign: $spoken", spoken.contains('%'))
        }
    }

    @Test
    fun spokenRangeCarriesTheSameNumbersAsTheStrictBadge() {
        for (band in ProgressBand.values()) {
            val badgeNumbers = Regex("\\d+").findAll(band.shortBadge).map { it.value }.toList()
            val spoken = band.spokenRange()
            for (n in badgeNumbers) {
                assertTrue("${band.name}: spoken range '$spoken' must mention $n from '${band.shortBadge}'", spoken.contains(n))
            }
        }
    }

    @Test
    fun spokenDescriptionStartsWithTheVisibleLabel() {
        for (band in ProgressBand.values()) {
            assertTrue(band.spokenDescription().startsWith(band.label))
        }
        assertTrue(ProgressBand.HALFWAY.spokenDescription().contains("50 percent to under 75 percent"))
        assertTrue(ProgressBand.GOAL_MET.spokenDescription().contains("exactly 100 percent"))
    }
}
