package com.example.dayseed

import com.example.dayseed.ui.theme.BotanicalDaypart
import com.example.dayseed.ui.theme.DarkCanvas
import com.example.dayseed.ui.theme.DayseedArtwork
import com.example.dayseed.ui.theme.LightBrand
import com.example.dayseed.ui.theme.LightCanvas
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
 * Appearance resolution (D-204, D-210, D-245): four botanical themes follow the time of day, three plain themes
 * draw no artwork, and every theme greets by the clock.
 */
class DayseedVisualResolutionTest {

    private fun t(hour: Int, minute: Int = 0): LocalTime = LocalTime.of(hour, minute)
    private val botanical = ThemeModeOption.entries.filter { it.isBotanical }
    private val plain = ThemeModeOption.entries.filter { !it.isBotanical }

    @Test
    fun daypartClockBoundaries_evening_is_4pm_to_7pm() {
        assertEquals(BotanicalDaypart.NIGHT, SoftBotanicalThemeConfig.resolveDaypart(t(4, 59)))
        assertEquals(BotanicalDaypart.MORNING, SoftBotanicalThemeConfig.resolveDaypart(t(5)))
        assertEquals(BotanicalDaypart.MORNING, SoftBotanicalThemeConfig.resolveDaypart(t(10, 59)))
        assertEquals(BotanicalDaypart.DAY, SoftBotanicalThemeConfig.resolveDaypart(t(11)))
        assertEquals(BotanicalDaypart.DAY, SoftBotanicalThemeConfig.resolveDaypart(t(15, 59)))
        assertEquals(BotanicalDaypart.EVENING, SoftBotanicalThemeConfig.resolveDaypart(t(16)))
        assertEquals(BotanicalDaypart.EVENING, SoftBotanicalThemeConfig.resolveDaypart(t(18, 59)))
        assertEquals(BotanicalDaypart.NIGHT, SoftBotanicalThemeConfig.resolveDaypart(t(19)))
        assertEquals(BotanicalDaypart.NIGHT, SoftBotanicalThemeConfig.resolveDaypart(t(23, 59)))
        assertEquals(BotanicalDaypart.NIGHT, SoftBotanicalThemeConfig.resolveDaypart(t(0)))
    }

    @Test
    fun everyHourOfTheDayHasExactlyOneDaypartAndTheFourAreAllReached() {
        val seen = (0..23).map { SoftBotanicalThemeConfig.resolveDaypart(t(it)) }
        assertEquals(BotanicalDaypart.entries.toSet(), seen.toSet())
        // Hours per daypart: Morning 6, Day 5, Evening 3, Night 10.
        assertEquals(6, seen.count { it == BotanicalDaypart.MORNING })
        assertEquals(5, seen.count { it == BotanicalDaypart.DAY })
        assertEquals(3, seen.count { it == BotanicalDaypart.EVENING })
        assertEquals(10, seen.count { it == BotanicalDaypart.NIGHT })
    }

    @Test
    fun botanicalThemesShowTheDaypartArtworkAndPlainThemesShowNone() {
        for (mode in botanical) {
            assertTrue("$mode shows artwork", resolveDayseedArtworkBlend(mode, false, t(12)).visible)
        }
        for (mode in plain) {
            for (hour in 0..23) {
                assertFalse("$mode must draw no artwork at $hour:00", resolveDayseedArtworkBlend(mode, hour % 2 == 0, t(hour)).visible)
            }
        }
        for (mode in botanical) {
            assertEquals(DayseedArtwork.MORNING_LEAF, resolveDayseedArtwork(mode, false, t(7)))
            assertEquals(DayseedArtwork.DAY_BRANCH, resolveDayseedArtwork(mode, false, t(13)))
            assertEquals(DayseedArtwork.EVENING_HILLS, resolveDayseedArtwork(mode, false, t(17)))
            assertEquals(DayseedArtwork.NIGHT_MOON, resolveDayseedArtwork(mode, false, t(22)))
        }
    }

    @Test
    fun plainThemesUseOneFixedPaletteAtEveryHour() {
        for (hour in 0..23) {
            assertEquals(LightCanvas, resolveDayseedColorScheme(ThemeModeOption.LIGHT, true, t(hour)).first.background)
            assertEquals(DarkCanvas, resolveDayseedColorScheme(ThemeModeOption.DARK, false, t(hour)).first.background)
            assertEquals(LightCanvas, resolveDayseedColorScheme(ThemeModeOption.SYSTEM, false, t(hour)).first.background)
            assertEquals(DarkCanvas, resolveDayseedColorScheme(ThemeModeOption.SYSTEM, true, t(hour)).first.background)
        }
    }

    @Test
    fun timeOfDayTheme_isLightByDayAndDarkAtNight() {
        val m = ThemeModeOption.SOFT_BOTANICAL_AUTO
        assertFalse(resolveDayseedColorScheme(m, false, t(8)).second)
        assertFalse(resolveDayseedColorScheme(m, false, t(13)).second)
        assertFalse(resolveDayseedColorScheme(m, false, t(17)).second)
        assertTrue(resolveDayseedColorScheme(m, false, t(21)).second)
        assertEquals(SoftBotanicalThemeConfig.EveningBrand, resolveDayseedColorScheme(m, false, t(17)).first.primary)
        assertEquals(SoftBotanicalThemeConfig.MorningBrand, resolveDayseedColorScheme(m, false, t(8)).first.primary)
        assertEquals(LightBrand, resolveDayseedColorScheme(m, false, t(13)).first.primary)
    }

    @Test
    fun botanicalLight_staysLightAllDayButStillShifts() {
        val m = ThemeModeOption.BOTANICAL_LIGHT
        val canvases = (0..23 step 6).map { resolveDayseedColorScheme(m, true, t(it, 30)) }
        canvases.forEach { assertFalse(it.second) }
        assertEquals(SoftBotanicalThemeConfig.NightLightCanvas, resolveDayseedColorScheme(m, false, t(23)).first.background)
        assertEquals(SoftBotanicalThemeConfig.MorningCanvas, resolveDayseedColorScheme(m, false, t(8)).first.background)
        assertEquals(SoftBotanicalThemeConfig.EveningCanvas, resolveDayseedColorScheme(m, false, t(17)).first.background)
        assertTrue("palette differs between morning and night", canvases.map { it.first.background }.toSet().size > 1)
    }

    @Test
    fun botanicalDark_staysDarkAllDayAndTintsByDaypart() {
        val m = ThemeModeOption.DARK_DAYPART_ART
        for (hour in 0..23) assertTrue("hour=$hour must be dark", resolveDayseedColorScheme(m, false, t(hour, 30)).second)
        assertEquals(SoftBotanicalThemeConfig.MorningDarkCanvas, resolveDayseedColorScheme(m, false, t(8)).first.background)
        assertEquals(SoftBotanicalThemeConfig.DayDarkCanvas, resolveDayseedColorScheme(m, false, t(13)).first.background)
        assertEquals(SoftBotanicalThemeConfig.EveningDarkCanvas, resolveDayseedColorScheme(m, false, t(17)).first.background)
        assertEquals(DarkCanvas, resolveDayseedColorScheme(m, false, t(23)).first.background)
    }

    @Test
    fun botanicalSystem_followsThePhoneLightOrDark() {
        val m = ThemeModeOption.BOTANICAL_SYSTEM
        val light = resolveDayseedColorScheme(m, false, t(17))
        val dark = resolveDayseedColorScheme(m, true, t(17))
        assertFalse(light.second)
        assertTrue(dark.second)
        assertEquals(SoftBotanicalThemeConfig.EveningCanvas, light.first.background)
        assertEquals(SoftBotanicalThemeConfig.EveningDarkCanvas, dark.first.background)
    }

    @Test
    fun colourShiftBlendsGraduallyAround4pm() {
        val m = ThemeModeOption.SOFT_BOTANICAL_AUTO
        val before = resolveDayseedColorScheme(m, false, t(15, 0)).first.background // before the window
        val mid = resolveDayseedColorScheme(m, false, t(16, 0)).first.background
        val after = resolveDayseedColorScheme(m, false, t(17, 0)).first.background // after the window
        assertEquals(LightCanvas, before)
        assertEquals(SoftBotanicalThemeConfig.EveningCanvas, after)
        assertNotEquals(before, mid)
        assertNotEquals(after, mid)
        val blend = SoftBotanicalThemeConfig.resolveDaypartBlend(t(16, 0))
        assertEquals(BotanicalDaypart.DAY, blend.from)
        assertEquals(BotanicalDaypart.EVENING, blend.to)
        assertEquals(0.5f, blend.fraction, 0.01f)
        // The shift for evening starts 45 minutes early and settles 45 minutes late.
        assertEquals(0f, SoftBotanicalThemeConfig.resolveDaypartBlend(t(15, 14)).fraction, 0.001f)
        assertTrue(SoftBotanicalThemeConfig.resolveDaypartBlend(t(15, 30)).fraction > 0f)
        assertEquals(BotanicalDaypart.EVENING, SoftBotanicalThemeConfig.resolveDaypartBlend(t(16, 50)).from)
    }

    @Test
    fun everyThemeGreetsByTheTimeOfDay() {
        // The greeting depends only on the clock, never on the chosen theme.
        assertEquals("Good morning", greetingFor(SoftBotanicalThemeConfig.resolveDaypart(t(6))))
        assertEquals("Good afternoon", greetingFor(SoftBotanicalThemeConfig.resolveDaypart(t(13))))
        assertEquals("Good evening", greetingFor(SoftBotanicalThemeConfig.resolveDaypart(t(17))))
        assertEquals("Good night", greetingFor(SoftBotanicalThemeConfig.resolveDaypart(t(21))))
    }

    @Test
    fun themeModeSettingValuesRoundTrip() {
        for (mode in ThemeModeOption.entries) assertEquals(mode, ThemeModeOption.fromSetting(mode.settingValue))
        // Backward compatibility: values written by earlier versions keep resolving.
        assertEquals(ThemeModeOption.SYSTEM, ThemeModeOption.fromSetting("SYSTEM"))
        assertEquals(ThemeModeOption.LIGHT, ThemeModeOption.fromSetting("light"))
        assertEquals(ThemeModeOption.DARK, ThemeModeOption.fromSetting("Dark"))
        assertEquals(ThemeModeOption.SOFT_BOTANICAL_AUTO, ThemeModeOption.fromSetting("SOFT_BOTANICAL_AUTO"))
        assertEquals(ThemeModeOption.DARK_DAYPART_ART, ThemeModeOption.fromSetting("dark_daypart_art"))
        // Unknown or missing falls back to the botanical system theme.
        assertEquals(ThemeModeOption.BOTANICAL_SYSTEM, ThemeModeOption.fromSetting("NIGHT_SHIFT"))
        assertEquals(ThemeModeOption.BOTANICAL_SYSTEM, ThemeModeOption.fromSetting(null))
    }
}
