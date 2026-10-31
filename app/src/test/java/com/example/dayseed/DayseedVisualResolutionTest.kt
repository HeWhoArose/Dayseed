package com.example.dayseed

import com.example.dayseed.ui.theme.BotanicalDaypart
import com.example.dayseed.ui.theme.DayseedArtwork
import com.example.dayseed.ui.theme.LightBrand
import com.example.dayseed.ui.theme.SoftBotanicalThemeConfig
import com.example.dayseed.ui.theme.ThemeModeOption
import com.example.dayseed.ui.theme.greetingFor
import com.example.dayseed.ui.theme.resolveDayseedArtwork
import com.example.dayseed.ui.theme.resolveDayseedArtworkBlend
import com.example.dayseed.ui.theme.resolveDayseedColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/**
 * Time-of-day visual resolution & gradual shift coverage (D-204, D-210).
 *
 * Verifies the centralised daypart/artwork resolution that drives the owner's five-panel
 * reference direction: distinct palettes per daypart, stable artwork for fixed themes,
 * unchanged clock boundaries (D-204), time-aware greeting copy, and smooth gradual shifts
 * across daypart boundaries instead of snapping.
 */
class DayseedVisualResolutionTest {

    private fun t(hour: Int, minute: Int = 0): LocalTime = LocalTime.of(hour, minute)

    // --- Daypart clock boundaries remain the provisional D-204 contract (05/11/17/21) ---

    @Test
    fun daypartBoundariesRemainProvisionalContract() {
        assertEquals(BotanicalDaypart.MORNING, SoftBotanicalThemeConfig.resolveDaypart(t(5)))
        assertEquals(BotanicalDaypart.MORNING, SoftBotanicalThemeConfig.resolveDaypart(t(10, 59)))
        assertEquals(BotanicalDaypart.DAY, SoftBotanicalThemeConfig.resolveDaypart(t(11)))
        assertEquals(BotanicalDaypart.DAY, SoftBotanicalThemeConfig.resolveDaypart(t(16, 59)))
        assertEquals(BotanicalDaypart.EVENING, SoftBotanicalThemeConfig.resolveDaypart(t(17)))
        assertEquals(BotanicalDaypart.EVENING, SoftBotanicalThemeConfig.resolveDaypart(t(20, 59)))
        assertEquals(BotanicalDaypart.NIGHT, SoftBotanicalThemeConfig.resolveDaypart(t(21)))
        assertEquals(BotanicalDaypart.NIGHT, SoftBotanicalThemeConfig.resolveDaypart(t(4, 59)))
    }

    // --- Each automatic daypart resolves a distinct artwork (D-210) ---

    @Test
    fun autoDaypartsMapToDistinctArtwork() {
        assertEquals(DayseedArtwork.MORNING_LEAF, resolveDayseedArtwork(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, t(6)))
        assertEquals(DayseedArtwork.DAY_BRANCH, resolveDayseedArtwork(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, t(14)))
        assertEquals(DayseedArtwork.EVENING_HILLS, resolveDayseedArtwork(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, t(19)))
        assertEquals(DayseedArtwork.NIGHT_MOON, resolveDayseedArtwork(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, t(23)))
        assertEquals(DayseedArtwork.NIGHT_MOON, resolveDayseedArtwork(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, t(3)))
    }

    @Test
    fun fixedLightAndSystemModesUseStableArtwork() {
        assertEquals(DayseedArtwork.DAY_BRANCH, resolveDayseedArtwork(ThemeModeOption.LIGHT, false, t(19)))
        assertEquals(DayseedArtwork.NIGHT_MOON, resolveDayseedArtwork(ThemeModeOption.DARK, true, t(6)))
        assertEquals(DayseedArtwork.NIGHT_MOON, resolveDayseedArtwork(ThemeModeOption.SYSTEM, true, t(6)))
        assertEquals(DayseedArtwork.DAY_BRANCH, resolveDayseedArtwork(ThemeModeOption.SYSTEM, false, t(6)))
    }

    // --- Evening has a terracotta/clay primary, not the daylight green (D-210 audit gap A) ---

    @Test
    fun eveningResolvesClayPrimary() {
        val (scheme, isDark) = resolveDayseedColorScheme(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, t(19))
        assertFalse(isDark)
        assertEquals(SoftBotanicalThemeConfig.EveningBrand, scheme.primary)
        assertNotEquals(LightBrand, scheme.primary)
    }

    @Test
    fun morningResolvesDistinctLeafPrimary() {
        val (scheme, _) = resolveDayseedColorScheme(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, t(7))
        assertEquals(SoftBotanicalThemeConfig.MorningBrand, scheme.primary)
        assertNotEquals(LightBrand, scheme.primary)
        assertNotEquals(SoftBotanicalThemeConfig.EveningBrand, scheme.primary)
    }

    @Test
    fun daylightResolvesCanonicalBrand() {
        val (scheme, isDark) = resolveDayseedColorScheme(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, t(14))
        assertFalse(isDark)
        assertEquals(LightBrand, scheme.primary)
    }

    // --- Gradual shift across daypart boundaries (no abrupt snap at 05:00, 11:00, 17:00, 21:00) ---

    @Test
    fun gradualShiftInterpolatesSmoothlyAcrossDaypartBoundaries() {
        // Dusk boundary (16:15 -> 17:45): Day -> Evening
        val blendPreDusk = SoftBotanicalThemeConfig.resolveDaypartBlend(t(16, 40))
        val blendAtDusk = SoftBotanicalThemeConfig.resolveDaypartBlend(t(17, 0))
        val blendPostDusk = SoftBotanicalThemeConfig.resolveDaypartBlend(t(17, 20))

        assertEquals(BotanicalDaypart.DAY, blendAtDusk.from)
        assertEquals(BotanicalDaypart.EVENING, blendAtDusk.to)
        assertTrue(blendPreDusk.fraction in 0.05f..0.45f)
        assertEquals(0.5f, blendAtDusk.fraction, 0.01f)
        assertTrue(blendPostDusk.fraction in 0.55f..0.95f)
        assertTrue(blendPreDusk.fraction < blendAtDusk.fraction && blendAtDusk.fraction < blendPostDusk.fraction)

        // Verify ColorScheme primary and background shift progressively rather than snapping
        val (dayScheme, _) = resolveDayseedColorScheme(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, t(14, 0))
        val (midTransitionScheme, _) = resolveDayseedColorScheme(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, t(17, 0))
        val (eveningScheme, _) = resolveDayseedColorScheme(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, t(19, 0))

        assertNotEquals(dayScheme.primary, midTransitionScheme.primary)
        assertNotEquals(eveningScheme.primary, midTransitionScheme.primary)
        assertNotEquals(dayScheme.background, midTransitionScheme.background)
        assertNotEquals(eveningScheme.background, midTransitionScheme.background)

        // Verify artwork blend cross-dissolves across the boundary
        val artBlend = resolveDayseedArtworkBlend(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, t(17, 0))
        assertEquals(DayseedArtwork.DAY_BRANCH, artBlend.primaryArtwork)
        assertEquals(DayseedArtwork.EVENING_HILLS, artBlend.secondaryArtwork)
        assertEquals(0.5f, artBlend.blendFraction, 0.01f)
    }

    // --- Greeting copy is one job per daypart (D-210 header layout) ---

    @Test
    fun greetingMatchesDaypart() {
        assertEquals("Good morning", greetingFor(BotanicalDaypart.MORNING))
        assertEquals("Good afternoon", greetingFor(BotanicalDaypart.DAY))
        assertEquals("Good evening", greetingFor(BotanicalDaypart.EVENING))
        assertEquals("Good night", greetingFor(BotanicalDaypart.NIGHT))
    }
}
