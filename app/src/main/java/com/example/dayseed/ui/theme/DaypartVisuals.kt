package com.example.dayseed.ui.theme

import java.time.LocalTime

/**
 * Centralised daypart visual resolution (D-210).
 *
 * Maps a resolved theme mode + local time to the corresponding painterly botanical/landscape
 * picture and supports gradual cross-blending across time-of-day boundaries so neither the
 * palette nor the illustration ever snaps abruptly.
 */
enum class DayseedArtwork {
    MORNING_LEAF,   // "Sage Modern": lush watercolor sage branch with veined overlapping leaves
    DAY_BRANCH,     // "Field Journal": naturalist olive-sage botanical sprigs with warm clay flower buds
    EVENING_HILLS,  // "Earth & Clay": layered sunset hills, glowing low sun, and hillside pine silhouettes
    NIGHT_MOON      // "Night Garden": glowing golden crescent moon with climbing nocturnal foliage & buds
}

data class DayseedArtworkBlend(
    val primaryArtwork: DayseedArtwork,
    val secondaryArtwork: DayseedArtwork = primaryArtwork,
    val blendFraction: Float = 0f // 0f = 100% primaryArtwork, 1f = 100% secondaryArtwork
)

fun artworkForDaypart(daypart: BotanicalDaypart): DayseedArtwork = when (daypart) {
    BotanicalDaypart.MORNING -> DayseedArtwork.MORNING_LEAF
    BotanicalDaypart.DAY -> DayseedArtwork.DAY_BRANCH
    BotanicalDaypart.EVENING -> DayseedArtwork.EVENING_HILLS
    BotanicalDaypart.NIGHT -> DayseedArtwork.NIGHT_MOON
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
        ThemeModeOption.SOFT_BOTANICAL_AUTO -> artworkForDaypart(
            SoftBotanicalThemeConfig.resolveDaypart(localTime)
        )
    }
}

/**
 * Resolves a continuous blend between two adjacent daypart pictures during dawn, midday,
 * dusk, and nightfall transition windows when in SOFT_BOTANICAL_AUTO mode.
 */
fun resolveDayseedArtworkBlend(
    themeMode: ThemeModeOption,
    systemDark: Boolean,
    localTime: LocalTime
): DayseedArtworkBlend {
    return when (themeMode) {
        ThemeModeOption.LIGHT -> DayseedArtworkBlend(DayseedArtwork.DAY_BRANCH)
        ThemeModeOption.DARK -> DayseedArtworkBlend(DayseedArtwork.NIGHT_MOON)
        ThemeModeOption.SYSTEM -> {
            val art = if (systemDark) DayseedArtwork.NIGHT_MOON else DayseedArtwork.DAY_BRANCH
            DayseedArtworkBlend(art)
        }
        ThemeModeOption.SOFT_BOTANICAL_AUTO -> {
            val blend = SoftBotanicalThemeConfig.resolveDaypartBlend(localTime)
            val fromArt = artworkForDaypart(blend.from)
            val toArt = artworkForDaypart(blend.to)
            DayseedArtworkBlend(
                primaryArtwork = fromArt,
                secondaryArtwork = toArt,
                blendFraction = if (fromArt == toArt) 0f else blend.fraction
            )
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
