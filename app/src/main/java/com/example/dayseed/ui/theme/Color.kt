package com.example.dayseed.ui.theme

import androidx.compose.ui.graphics.Color
import java.time.LocalTime

// Canonical Light Theme Tokens from DESIGN.md
val LightCanvas = Color(0xFFF7F5EE)
val LightSurface = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF24352B)
val LightTextSecondary = Color(0xFF59655C)
val LightBorder = Color(0xFFD8DED5)
val LightBrand = Color(0xFF286143)
val LightOnBrand = Color(0xFFFFFFFF)
val LightGrowthSubtle = Color(0xFFE4EBDD)
val LightAccentWarm = Color(0xFFB84A3A)

// Canonical Dark Theme Tokens from DESIGN.md
val DarkCanvas = Color(0xFF121A15)
val DarkSurface = Color(0xFF1D2820)
val DarkTextPrimary = Color(0xFFF3F2E8)
val DarkTextSecondary = Color(0xFFC1CBC1)
val DarkBorder = Color(0xFF3C4A3E)
val DarkBrand = Color(0xFF9CCB9C)
val DarkOnBrand = Color(0xFF17251C)
val DarkGrowthSubtle = Color(0xFF293A2D)
val DarkAccentWarm = Color(0xFFF0A092)

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
 * Centralised Provisional Soft Botanical Configuration (D-204, D-207).
 * Note: Exact daypart hex variations and hour boundaries are isolated here as provisional defaults
 * while keeping DESIGN.md canonical Light/Dark tokens intact.
 */
object SoftBotanicalThemeConfig {
    const val MORNING_START_HOUR = 5   // 05:00 - 10:59
    const val DAY_START_HOUR = 11      // 11:00 - 16:59
    const val EVENING_START_HOUR = 17  // 17:00 - 20:59
    const val NIGHT_START_HOUR = 21    // 21:00 - 04:59

    // Provisional Morning tokens (warm mist & fresh sage)
    val MorningCanvas = Color(0xFFF8F6EF)
    val MorningSurface = Color(0xFFFFFFFF)
    val MorningGrowthSubtle = Color(0xFFE6EFE2)
    val MorningBrand = Color(0xFF2D6A4F)

    // Provisional Evening tokens (soft amber parchment & deep moss)
    val EveningCanvas = Color(0xFFF4EFE6)
    val EveningSurface = Color(0xFFFCF9F2)
    val EveningGrowthSubtle = Color(0xFFE2E5D8)
    val EveningBrand = Color(0xFF286143)

    fun resolveDaypart(localTime: LocalTime): BotanicalDaypart {
        val h = localTime.hour
        return when {
            h in MORNING_START_HOUR until DAY_START_HOUR -> BotanicalDaypart.MORNING
            h in DAY_START_HOUR until EVENING_START_HOUR -> BotanicalDaypart.DAY
            h in EVENING_START_HOUR until NIGHT_START_HOUR -> BotanicalDaypart.EVENING
            else -> BotanicalDaypart.NIGHT
        }
    }
}
