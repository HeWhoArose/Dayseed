package com.example.dayseed.ui.theme

import java.time.LocalTime

/**
 * Centralised daypart visual resolution (D-210).
 *
 * The owner's five-panel reference defines the theme direction as the colours, designs and
 * per-time-of-day pictures. This file is the single source that maps a resolved theme mode +
 * local time to one of four header artworks, and the daypart to its greeting line. Keep the
 * rules here; do not scatter daypart/selection logic into individual screens.
 *
 * Fixed LIGHT/DARK modes use stable artwork (Field Journal branch by day, Night Garden moon
 * after dark) rather than switching at clock boundaries, because the user explicitly chose a
 * fixed look. Automatic mode follows the Soft Botanical daypart contract.
 */
enum class DayseedArtwork {
    MORNING_LEAF,   // "Sage Modern": fresh ascending sprig
    DAY_BRANCH,     // "Field Journal": warm leafy branch
    EVENING_HILLS,  // "Earth & Clay": layered sunset hills with low sun
    NIGHT_MOON      // "Night Garden": golden crescent moon with night foliage
}

fun resolveDayseedArtwork(
    themeMode: ThemeModeOption,
    systemDark: Boolean,
    localTime: LocalTime
): DayseedArtwork {
    return when (themeMode) {
        ThemeModeOption.LIGHT -> DayseedArtwork.DAY_BRANCH
        ThemeModeOption.DARK -> DayseedArtwork.NIGHT_MOON
        ThemeModeOption.SYSTEM -> if (systemDark) DayseedArtwork.NIGHT_MOON else DayseedArtwork.DAY_BRANCH
        ThemeModeOption.SOFT_BOTANICAL_AUTO -> when (SoftBotanicalThemeConfig.resolveDaypart(localTime)) {
            BotanicalDaypart.MORNING -> DayseedArtwork.MORNING_LEAF
            BotanicalDaypart.DAY -> DayseedArtwork.DAY_BRANCH
            BotanicalDaypart.EVENING -> DayseedArtwork.EVENING_HILLS
            BotanicalDaypart.NIGHT -> DayseedArtwork.NIGHT_MOON
        }
    }
}

/**
 * Time-aware greeting copy (D-210). One job per line: orient the person in their day.
 * Period wording stays a clock concept; it never implies a fixed duration ("month"-style
 * duration rules do not apply here).
 */
fun greetingFor(daypart: BotanicalDaypart): String = when (daypart) {
    BotanicalDaypart.MORNING -> "Good morning"
    BotanicalDaypart.DAY -> "Good afternoon"
    BotanicalDaypart.EVENING -> "Good evening"
    BotanicalDaypart.NIGHT -> "Good night"
}
