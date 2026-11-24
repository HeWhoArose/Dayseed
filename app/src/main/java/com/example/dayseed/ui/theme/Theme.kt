package com.example.dayseed.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import java.time.LocalTime

val LocalDayseedLiveTime = compositionLocalOf<LocalTime> { LocalTime.now() }

private val DarkColorScheme = darkColorScheme(
    primary = DarkBrand,
    onPrimary = DarkOnBrand,
    primaryContainer = DarkGrowthSubtle,
    onPrimaryContainer = DarkTextPrimary,
    secondary = NightGoldAccent,
    onSecondary = DarkOnBrand,
    secondaryContainer = DarkGrowthSubtle,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = NightGoldAccent,
    onTertiary = DarkOnBrand,
    background = DarkCanvas,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkGrowthSubtle,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkControlBorder,
    outlineVariant = DarkBorder,
    error = DarkAccentWarm,
    onError = DarkOnBrand
)

private val LightColorScheme = lightColorScheme(
    primary = LightBrand,
    onPrimary = LightOnBrand,
    primaryContainer = LightGrowthSubtle,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightBrand,
    onSecondary = LightOnBrand,
    secondaryContainer = LightGrowthSubtle,
    onSecondaryContainer = LightTextPrimary,
    tertiary = LightAccentWarm,
    onTertiary = LightOnBrand,
    background = LightCanvas,
    onBackground = LightTextPrimary,
    surface = DayJournalSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightGrowthSubtle,
    onSurfaceVariant = LightTextSecondary,
    outline = LightControlBorder,
    outlineVariant = LightBorder,
    error = LightAccentWarm,
    onError = LightOnBrand
)

// Morning ("Sage Modern"): fresh leaf action on airy cream-sage canvas with crisp white cards.
private val MorningColorScheme = LightColorScheme.copy(
    primary = SoftBotanicalThemeConfig.MorningBrand,
    onPrimary = LightOnBrand,
    primaryContainer = SoftBotanicalThemeConfig.MorningGrowthSubtle,
    onPrimaryContainer = LightTextPrimary,
    secondary = SoftBotanicalThemeConfig.MorningBrand,
    onSecondary = LightOnBrand,
    secondaryContainer = SoftBotanicalThemeConfig.MorningGrowthSubtle,
    onSecondaryContainer = LightTextPrimary,
    background = SoftBotanicalThemeConfig.MorningCanvas,
    surface = SoftBotanicalThemeConfig.MorningSurface,
    surfaceVariant = SoftBotanicalThemeConfig.MorningGrowthSubtle,
    outline = LightControlBorder,
    outlineVariant = SoftBotanicalThemeConfig.MorningBorder
)

// Evening ("Earth & Clay"): terracotta action on warm stone canvas with sand-tinted cards and olive-sage accents.
private val EveningColorScheme = LightColorScheme.copy(
    primary = SoftBotanicalThemeConfig.EveningBrand,
    onPrimary = LightOnBrand,
    primaryContainer = SoftBotanicalThemeConfig.EveningGrowthSubtle,
    onPrimaryContainer = LightTextPrimary,
    secondary = SoftBotanicalThemeConfig.EveningSecondary,
    onSecondary = LightOnBrand,
    secondaryContainer = SoftBotanicalThemeConfig.EveningGrowthSubtle,
    onSecondaryContainer = LightTextPrimary,
    background = SoftBotanicalThemeConfig.EveningCanvas,
    surface = SoftBotanicalThemeConfig.EveningSurface,
    surfaceVariant = SoftBotanicalThemeConfig.EveningGrowthSubtle,
    outline = LightControlBorder,
    outlineVariant = SoftBotanicalThemeConfig.EveningBorder
)

// Light variant of Night ("Moonlit Paper") and dark variants of Morning, Day and Evening (D-245).
private val NightLightColorScheme = LightColorScheme.copy(
    primary = SoftBotanicalThemeConfig.NightLightBrand,
    onPrimary = LightOnBrand,
    primaryContainer = SoftBotanicalThemeConfig.NightLightGrowth,
    onPrimaryContainer = LightTextPrimary,
    secondary = SoftBotanicalThemeConfig.NightLightBrand,
    onSecondary = LightOnBrand,
    secondaryContainer = SoftBotanicalThemeConfig.NightLightGrowth,
    onSecondaryContainer = LightTextPrimary,
    background = SoftBotanicalThemeConfig.NightLightCanvas,
    surface = SoftBotanicalThemeConfig.NightLightSurface,
    surfaceVariant = SoftBotanicalThemeConfig.NightLightGrowth,
    outline = SoftBotanicalThemeConfig.NightLightControl,
    outlineVariant = SoftBotanicalThemeConfig.NightLightBorder
)

private fun darkTinted(
    canvas: Color, surface: Color, growth: Color, brand: Color, onBrand: Color, border: Color, control: Color,
    secondary: Color = brand
) = DarkColorScheme.copy(
    primary = brand, onPrimary = onBrand,
    primaryContainer = growth, onPrimaryContainer = DarkTextPrimary,
    secondary = secondary, onSecondary = onBrand,
    secondaryContainer = growth, onSecondaryContainer = DarkTextPrimary,
    background = canvas, surface = surface, surfaceVariant = growth,
    outline = control, outlineVariant = border
)

private val MorningDarkColorScheme = with(SoftBotanicalThemeConfig) {
    darkTinted(MorningDarkCanvas, MorningDarkSurface, MorningDarkGrowth, MorningDarkBrand, MorningDarkOnBrand, MorningDarkBorder, MorningDarkControl)
}
private val DayDarkColorScheme = with(SoftBotanicalThemeConfig) {
    darkTinted(DayDarkCanvas, DayDarkSurface, DayDarkGrowth, DayDarkBrand, DayDarkOnBrand, DayDarkBorder, DayDarkControl)
}
private val EveningDarkColorScheme = with(SoftBotanicalThemeConfig) {
    darkTinted(EveningDarkCanvas, EveningDarkSurface, EveningDarkGrowth, EveningDarkBrand, EveningDarkOnBrand, EveningDarkBorder, EveningDarkControl, EveningDarkSecondary)
}

/** Time-of-day palette that is light by day and dark at night (the "Time of day" theme). */
private fun schemeForDaypart(daypart: BotanicalDaypart): ColorScheme = when (daypart) {
    BotanicalDaypart.MORNING -> MorningColorScheme
    BotanicalDaypart.DAY -> LightColorScheme
    BotanicalDaypart.EVENING -> EveningColorScheme
    BotanicalDaypart.NIGHT -> DarkColorScheme
}

/** Time-of-day palette that always stays light. */
internal fun lightSchemeForDaypart(daypart: BotanicalDaypart): ColorScheme = when (daypart) {
    BotanicalDaypart.MORNING -> MorningColorScheme
    BotanicalDaypart.DAY -> LightColorScheme
    BotanicalDaypart.EVENING -> EveningColorScheme
    BotanicalDaypart.NIGHT -> NightLightColorScheme
}

/** Time-of-day palette that always stays dark. */
internal fun darkSchemeForDaypart(daypart: BotanicalDaypart): ColorScheme = when (daypart) {
    BotanicalDaypart.MORNING -> MorningDarkColorScheme
    BotanicalDaypart.DAY -> DayDarkColorScheme
    BotanicalDaypart.EVENING -> EveningDarkColorScheme
    BotanicalDaypart.NIGHT -> DarkColorScheme
}

/**
 * Smoothly interpolates all semantic slots between two ColorSchemes so daypart transitions
 * across dawn, midday, dusk, and nightfall shift gradually instead of snapping.
 */
fun lerpColorScheme(start: ColorScheme, stop: ColorScheme, fraction: Float): ColorScheme {
    val t = fraction.coerceIn(0f, 1f)
    if (t <= 0f) return start
    if (t >= 1f) return stop
    return start.copy(
        primary = lerp(start.primary, stop.primary, t),
        onPrimary = lerp(start.onPrimary, stop.onPrimary, t),
        primaryContainer = lerp(start.primaryContainer, stop.primaryContainer, t),
        onPrimaryContainer = lerp(start.onPrimaryContainer, stop.onPrimaryContainer, t),
        secondary = lerp(start.secondary, stop.secondary, t),
        onSecondary = lerp(start.onSecondary, stop.onSecondary, t),
        secondaryContainer = lerp(start.secondaryContainer, stop.secondaryContainer, t),
        onSecondaryContainer = lerp(start.onSecondaryContainer, stop.onSecondaryContainer, t),
        tertiary = lerp(start.tertiary, stop.tertiary, t),
        onTertiary = lerp(start.onTertiary, stop.onTertiary, t),
        background = lerp(start.background, stop.background, t),
        onBackground = lerp(start.onBackground, stop.onBackground, t),
        surface = lerp(start.surface, stop.surface, t),
        onSurface = lerp(start.onSurface, stop.onSurface, t),
        surfaceVariant = lerp(start.surfaceVariant, stop.surfaceVariant, t),
        onSurfaceVariant = lerp(start.onSurfaceVariant, stop.onSurfaceVariant, t),
        outline = lerp(start.outline, stop.outline, t),
        outlineVariant = lerp(start.outlineVariant, stop.outlineVariant, t),
        error = lerp(start.error, stop.error, t),
        onError = lerp(start.onError, stop.onError, t)
    )
}

private fun blendedDaypartScheme(
    localTime: LocalTime,
    pick: (BotanicalDaypart) -> ColorScheme
): ColorScheme {
    val blend = SoftBotanicalThemeConfig.resolveDaypartBlend(localTime)
    val fromScheme = pick(blend.from)
    val toScheme = pick(blend.to)
    return if (blend.from == blend.to || blend.fraction <= 0f) fromScheme else lerpColorScheme(fromScheme, toScheme, blend.fraction)
}

fun resolveDayseedColorScheme(
    themeMode: ThemeModeOption,
    systemDark: Boolean,
    localTime: LocalTime = LocalTime.now()
): Pair<ColorScheme, Boolean> {
    val blend = SoftBotanicalThemeConfig.resolveDaypartBlend(localTime)
    val effectiveDaypart = if (blend.fraction >= 0.5f) blend.to else blend.from
    return when (themeMode) {
        // Plain themes: one fixed palette, no time-of-day tint.
        ThemeModeOption.LIGHT -> LightColorScheme to false
        ThemeModeOption.DARK -> DarkColorScheme to true
        ThemeModeOption.SYSTEM -> if (systemDark) DarkColorScheme to true else LightColorScheme to false
        // Botanical themes follow the time of day.
        ThemeModeOption.SOFT_BOTANICAL_AUTO -> blendedDaypartScheme(localTime, ::schemeForDaypart) to effectiveDaypart.isDark
        ThemeModeOption.BOTANICAL_LIGHT -> blendedDaypartScheme(localTime, ::lightSchemeForDaypart) to false
        ThemeModeOption.DARK_DAYPART_ART -> blendedDaypartScheme(localTime, ::darkSchemeForDaypart) to true
        ThemeModeOption.BOTANICAL_SYSTEM ->
            if (systemDark) blendedDaypartScheme(localTime, ::darkSchemeForDaypart) to true
            else blendedDaypartScheme(localTime, ::lightSchemeForDaypart) to false
    }
}

internal fun themeTransitionDurationMillis(reducedMotion: Boolean): Int =
    if (reducedMotion) 0 else 1200

@Composable
private fun animateThemeColor(target: Color, reducedMotion: Boolean): Color {
    if (reducedMotion) return target
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = themeTransitionDurationMillis(reducedMotion),
            easing = FastOutSlowInEasing
        ),
        label = "dayseed_theme_color_shift"
    )
    return animated
}

@Composable
fun DayseedTheme(
    themeMode: ThemeModeOption = ThemeModeOption.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    localTime: LocalTime = LocalTime.now(),
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val (targetScheme, isDarkResolved) = resolveDayseedColorScheme(themeMode, darkTheme, localTime)

    val animatedColorScheme = targetScheme.copy(
        primary = animateThemeColor(targetScheme.primary, reducedMotion),
        onPrimary = animateThemeColor(targetScheme.onPrimary, reducedMotion),
        primaryContainer = animateThemeColor(targetScheme.primaryContainer, reducedMotion),
        onPrimaryContainer = animateThemeColor(targetScheme.onPrimaryContainer, reducedMotion),
        secondary = animateThemeColor(targetScheme.secondary, reducedMotion),
        onSecondary = animateThemeColor(targetScheme.onSecondary, reducedMotion),
        secondaryContainer = animateThemeColor(targetScheme.secondaryContainer, reducedMotion),
        onSecondaryContainer = animateThemeColor(targetScheme.onSecondaryContainer, reducedMotion),
        tertiary = animateThemeColor(targetScheme.tertiary, reducedMotion),
        onTertiary = animateThemeColor(targetScheme.onTertiary, reducedMotion),
        background = animateThemeColor(targetScheme.background, reducedMotion),
        onBackground = animateThemeColor(targetScheme.onBackground, reducedMotion),
        surface = animateThemeColor(targetScheme.surface, reducedMotion),
        onSurface = animateThemeColor(targetScheme.onSurface, reducedMotion),
        surfaceVariant = animateThemeColor(targetScheme.surfaceVariant, reducedMotion),
        onSurfaceVariant = animateThemeColor(targetScheme.onSurfaceVariant, reducedMotion),
        outline = animateThemeColor(targetScheme.outline, reducedMotion),
        outlineVariant = animateThemeColor(targetScheme.outlineVariant, reducedMotion),
        error = animateThemeColor(targetScheme.error, reducedMotion),
        onError = animateThemeColor(targetScheme.onError, reducedMotion)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkResolved
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkResolved
            }
        }
    }

    CompositionLocalProvider(LocalDayseedLiveTime provides localTime) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            typography = DayseedTypography,
            content = content
        )
    }
}
