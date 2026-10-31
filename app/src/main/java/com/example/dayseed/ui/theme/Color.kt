package com.example.dayseed.ui.theme

import androidx.compose.ui.graphics.Color
import java.time.LocalTime
import kotlin.math.abs

// Canonical Light Theme Tokens from DESIGN.md ("Field Journal" / Botanical Day)
val LightCanvas = Color(0xFFF7F5EE)
val LightSurface = Color(0xFFFFFFFF)
val DayJournalSurface = Color(0xFFF3F4EC) // Warm cream-sage card tint from Field Journal reference
val LightTextPrimary = Color(0xFF24352B)
val LightTextSecondary = Color(0xFF59655C)
val LightBorder = Color(0xFFD8DED5)
val LightBrand = Color(0xFF286143)
val LightOnBrand = Color(0xFFFFFFFF)
val LightGrowthSubtle = Color(0xFFE4EBDD)
val LightAccentWarm = Color(0xFFB84A3A)

// Canonical Dark Theme Tokens from DESIGN.md ("Night Garden")
val DarkCanvas = Color(0xFF121A15)
val DarkSurface = Color(0xFF1A251E)
val DarkTextPrimary = Color(0xFFF3F2E8)
val DarkTextSecondary = Color(0xFFC1CBC1)
val DarkBorder = Color(0xFF334437)
val DarkBrand = Color(0xFF88B38C)
val DarkOnBrand = Color(0xFF132017)
val DarkGrowthSubtle = Color(0xFF253529)
val DarkAccentWarm = Color(0xFFF0A092)
val NightGoldAccent = Color(0xFFD8B984) // Warm golden-clay highlight from Night Garden reference

// Semantic Effort Level Colors (calm earth tones)
val TinySproutColor = Color(0xFF558B6E)
val UsualLeafColor = Color(0xFF286143)
val ExtraBranchColor = Color(0xFF1B4332)
val RestColor = Color(0xFF708D81)
val SkippedColor = Color(0xFF8D99AE)

enum class ThemeModeOption(val settingValue: String, val label: String) {
    SYSTEM("SYSTEM", "System Default"),
    LIGHT("LIGHT", "Light (Botanical Day)"),
    DARK("DARK", "Dark (Quiet Night)"),
    SOFT_BOTANICAL_AUTO("SOFT_BOTANICAL_AUTO", "Soft Botanical (Time of Day)");

    companion object {
        fun fromSetting(value: String?): ThemeModeOption {
            return entries.find { it.settingValue.equals(value, ignoreCase = true) } ?: SYSTEM
        }
    }
}

enum class BotanicalDaypart(val displayName: String, val isDark: Boolean) {
    MORNING("Morning Dew", false),
    DAY("Daylight Leaf", false),
    EVENING("Dusk Moss", false),
    NIGHT("Night Canopy", true)
}

/**
 * Represents a continuous, gradual transition state between two adjacent dayparts
 * so that themes and illustrations shift smoothly around dawn, midday, dusk, and nightfall
 * instead of snapping abruptly at the hour boundary.
 */
data class DaypartBlend(
    val from: BotanicalDaypart,
    val to: BotanicalDaypart,
    val fraction: Float // 0f = 100% 'from', 1f = 100% 'to' (smoothstepped)
)

/**
 * Centralised Provisional Soft Botanical Configuration (D-204, D-207, D-210).
 * Keeps DESIGN.md canonical tokens intact while defining per-daypart palettes and
 * continuous boundary transition blending.
 */
object SoftBotanicalThemeConfig {
    const val MORNING_START_HOUR = 5   // 05:00 - 10:59
    const val DAY_START_HOUR = 11      // 11:00 - 16:59
    const val EVENING_START_HOUR = 17  // 17:00 - 20:59
    const val NIGHT_START_HOUR = 21    // 21:00 - 04:59

    // Gradual transition half-window in minutes around each boundary (total window = 90 mins: ±45m).
    // e.g. 04:15..05:45 (Night -> Morning), 10:15..11:45 (Morning -> Day),
    // 16:15..17:45 (Day -> Evening), 20:15..21:45 (Evening -> Night).
    const val TRANSITION_HALF_WINDOW_MINUTES = 45f

    // Provisional Morning tokens — "Sage Modern" (D-210): airy cream-sage canvas, fresh leaf green action,
    // crisp white cards, and soft stone borders.
    val MorningCanvas = Color(0xFFF7F9F1)
    val MorningSurface = Color(0xFFFFFFFF)
    val MorningGrowthSubtle = Color(0xFFE7EFE6)
    val MorningBrand = Color(0xFF3E7C59)
    val MorningBorder = Color(0xFFD6E0D4)

    // Provisional Evening tokens — "Earth & Clay" (D-210): warm stone canvas, terracotta/clay action,
    // warm sand card containers, and muted olive-sage secondary tones.
    val EveningCanvas = Color(0xFFF4EBDD)
    val EveningSurface = Color(0xFFF9F1E4)
    val EveningGrowthSubtle = Color(0xFFEFE2D0)
    val EveningBrand = Color(0xFFAC5530)
    val EveningSecondary = Color(0xFF6B7B5C)
    val EveningBorder = Color(0xFFDECDB4)

    fun resolveDaypart(localTime: LocalTime): BotanicalDaypart {
        val h = localTime.hour
        return when {
            h in MORNING_START_HOUR until DAY_START_HOUR -> BotanicalDaypart.MORNING
            h in DAY_START_HOUR until EVENING_START_HOUR -> BotanicalDaypart.DAY
            h in EVENING_START_HOUR until NIGHT_START_HOUR -> BotanicalDaypart.EVENING
            else -> BotanicalDaypart.NIGHT
        }
    }

    /**
     * Computes a smooth, gradual blend across daypart boundaries using a cubic Hermite
     * smoothstep curve (3t^2 - 2t^3). Outside the ±45-minute transition windows around
     * 05:00, 11:00, 17:00, and 21:00, the daypart is at its pure steady-state palette (fraction = 0f).
     */
    fun resolveDaypartBlend(localTime: LocalTime): DaypartBlend {
        val minuteOfDay = localTime.hour * 60f + localTime.minute + (localTime.second / 60f)

        data class Boundary(
            val minute: Float,
            val from: BotanicalDaypart,
            val to: BotanicalDaypart
        )

        val boundaries = listOf(
            Boundary(MORNING_START_HOUR * 60f, BotanicalDaypart.NIGHT, BotanicalDaypart.MORNING),
            Boundary(DAY_START_HOUR * 60f, BotanicalDaypart.MORNING, BotanicalDaypart.DAY),
            Boundary(EVENING_START_HOUR * 60f, BotanicalDaypart.DAY, BotanicalDaypart.EVENING),
            Boundary(NIGHT_START_HOUR * 60f, BotanicalDaypart.EVENING, BotanicalDaypart.NIGHT)
        )

        for (b in boundaries) {
            val delta = minuteOfDay - b.minute
            if (abs(delta) < TRANSITION_HALF_WINDOW_MINUTES) {
                val rawT = ((delta + TRANSITION_HALF_WINDOW_MINUTES) / (2f * TRANSITION_HALF_WINDOW_MINUTES))
                    .coerceIn(0f, 1f)
                val smoothT = rawT * rawT * (3f - 2f * rawT)
                return DaypartBlend(from = b.from, to = b.to, fraction = smoothT)
            }
        }

        val steady = resolveDaypart(localTime)
        return DaypartBlend(from = steady, to = steady, fraction = 0f)
    }
}
