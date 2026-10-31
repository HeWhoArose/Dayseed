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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import java.time.LocalTime

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
    outline = DarkBorder,
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
    outline = LightBorder,
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
    outline = SoftBotanicalThemeConfig.MorningBorder
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
    outline = SoftBotanicalThemeConfig.EveningBorder
)

private fun schemeForDaypart(daypart: BotanicalDaypart): ColorScheme = when (daypart) {
    BotanicalDaypart.MORNING -> MorningColorScheme
    BotanicalDaypart.DAY -> LightColorScheme
    BotanicalDaypart.EVENING -> EveningColorScheme
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

fun resolveDayseedColorScheme(
    themeMode: ThemeModeOption,
    systemDark: Boolean,
    localTime: LocalTime = LocalTime.now()
): Pair<ColorScheme, Boolean> {
    return when (themeMode) {
        ThemeModeOption.LIGHT -> LightColorScheme.copy(surface = LightSurface) to false
        ThemeModeOption.DARK -> DarkColorScheme to true
        ThemeModeOption.SYSTEM -> if (systemDark) DarkColorScheme to true else LightColorScheme to false
        ThemeModeOption.SOFT_BOTANICAL_AUTO -> {
            val blend = SoftBotanicalThemeConfig.resolveDaypartBlend(localTime)
            val fromScheme = schemeForDaypart(blend.from)
            val toScheme = schemeForDaypart(blend.to)
            val resolvedScheme = if (blend.from == blend.to || blend.fraction <= 0f) {
                fromScheme
            } else {
                lerpColorScheme(fromScheme, toScheme, blend.fraction)
            }
            val effectiveDaypart = if (blend.fraction >= 0.5f) blend.to else blend.from
            resolvedScheme to effectiveDaypart.isDark
        }
    }
}

@Composable
private fun animateThemeColor(target: Color): Color {
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "dayseed_theme_color_shift"
    )
    return animated
}

@Composable
fun DayseedTheme(
    themeMode: ThemeModeOption = ThemeModeOption.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    localTime: LocalTime = LocalTime.now(),
    content: @Composable () -> Unit
) {
    val (targetScheme, isDarkResolved) = resolveDayseedColorScheme(themeMode, darkTheme, localTime)

    val animatedColorScheme = targetScheme.copy(
        primary = animateThemeColor(targetScheme.primary),
        onPrimary = animateThemeColor(targetScheme.onPrimary),
        primaryContainer = animateThemeColor(targetScheme.primaryContainer),
        onPrimaryContainer = animateThemeColor(targetScheme.onPrimaryContainer),
        secondary = animateThemeColor(targetScheme.secondary),
        onSecondary = animateThemeColor(targetScheme.onSecondary),
        secondaryContainer = animateThemeColor(targetScheme.secondaryContainer),
        onSecondaryContainer = animateThemeColor(targetScheme.onSecondaryContainer),
        tertiary = animateThemeColor(targetScheme.tertiary),
        onTertiary = animateThemeColor(targetScheme.onTertiary),
        background = animateThemeColor(targetScheme.background),
        onBackground = animateThemeColor(targetScheme.onBackground),
        surface = animateThemeColor(targetScheme.surface),
        onSurface = animateThemeColor(targetScheme.onSurface),
        surfaceVariant = animateThemeColor(targetScheme.surfaceVariant),
        onSurfaceVariant = animateThemeColor(targetScheme.onSurfaceVariant),
        outline = animateThemeColor(targetScheme.outline),
        error = animateThemeColor(targetScheme.error),
        onError = animateThemeColor(targetScheme.onError)
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

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = DayseedTypography,
        content = content
    )
}
