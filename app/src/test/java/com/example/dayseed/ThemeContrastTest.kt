package com.example.dayseed

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.dayseed.ui.theme.BotanicalDaypart
import com.example.dayseed.ui.theme.ThemeModeOption
import com.example.dayseed.ui.theme.resolveDayseedColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/** WCAG AA contrast for every botanical palette at the middle of each daypart (steady state, no blending). */
class ThemeContrastTest {
    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble(); val l2 = b.luminance().toDouble()
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }
    private val probes = mapOf(
        BotanicalDaypart.MORNING to LocalTime.of(8, 0), BotanicalDaypart.DAY to LocalTime.of(13, 0),
        BotanicalDaypart.EVENING to LocalTime.of(17, 30), BotanicalDaypart.NIGHT to LocalTime.of(23, 0)
    )

    @Test
    fun everyThemeAtEveryDaypartMeetsAA() {
        for (mode in ThemeModeOption.entries) for (systemDark in listOf(false, true)) for ((part, time) in probes) {
            val s = resolveDayseedColorScheme(mode, systemDark, time).first
            val tag = "$mode/systemDark=$systemDark/$part"
            assertTrue("$tag text on canvas", ratio(s.onBackground, s.background) >= 4.5)
            assertTrue("$tag text on card", ratio(s.onSurface, s.surface) >= 4.5)
            assertTrue("$tag secondary text on card", ratio(s.onSurfaceVariant, s.surface) >= 4.5)
            assertTrue("$tag brand on card (links, icons)", ratio(s.primary, s.surface) >= 4.5)
            assertTrue("$tag button label on brand", ratio(s.onPrimary, s.primary) >= 4.5)
            assertTrue("$tag control border on card", ratio(s.outline, s.surface) >= 3.0)
        }
    }
}
