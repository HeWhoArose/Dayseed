package com.example.dayseed.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import java.time.LocalTime

private val DarkColorScheme = darkColorScheme(
    primary = DarkBrand,
    onPrimary = DarkOnBrand,
    primaryContainer = DarkGrowthSubtle,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkBrand,
    onSecondary = DarkOnBrand,
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
    background = LightCanvas,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightGrowthSubtle,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = LightAccentWarm,
    onError = LightOnBrand
)

private val MorningColorScheme = LightColorScheme.copy(
    primary = SoftBotanicalThemeConfig.MorningBrand,
    primaryContainer = SoftBotanicalThemeConfig.MorningGrowthSubtle,
    background = SoftBotanicalThemeConfig.MorningCanvas,
    surface = SoftBotanicalThemeConfig.MorningSurface,
    surfaceVariant = SoftBotanicalThemeConfig.MorningGrowthSubtle
)

private val EveningColorScheme = LightColorScheme.copy(
    primary = SoftBotanicalThemeConfig.EveningBrand,
    primaryContainer = SoftBotanicalThemeConfig.EveningGrowthSubtle,
    background = SoftBotanicalThemeConfig.EveningCanvas,
    surface = SoftBotanicalThemeConfig.EveningSurface,
    surfaceVariant = SoftBotanicalThemeConfig.EveningGrowthSubtle
)

fun resolveDayseedColorScheme(
    themeMode: ThemeModeOption,
    systemDark: Boolean,
    localTime: LocalTime = LocalTime.now()
): Pair<ColorScheme, Boolean> {
    return when (themeMode) {
        ThemeModeOption.LIGHT -> LightColorScheme to false
        ThemeModeOption.DARK -> DarkColorScheme to true
        ThemeModeOption.SYSTEM -> if (systemDark) DarkColorScheme to true else LightColorScheme to false
        ThemeModeOption.SOFT_BOTANICAL_AUTO -> {
            when (SoftBotanicalThemeConfig.resolveDaypart(localTime)) {
                BotanicalDaypart.MORNING -> MorningColorScheme to false
                BotanicalDaypart.DAY -> LightColorScheme to false
                BotanicalDaypart.EVENING -> EveningColorScheme to false
                BotanicalDaypart.NIGHT -> DarkColorScheme to true
            }
        }
    }
}

@Composable
fun DayseedTheme(
    themeMode: ThemeModeOption = ThemeModeOption.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    localTime: LocalTime = LocalTime.now(),
    content: @Composable () -> Unit
) {
    val (colorScheme, isDarkResolved) = resolveDayseedColorScheme(themeMode, darkTheme, localTime)

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
        colorScheme = colorScheme,
        typography = DayseedTypography,
        content = content
    )
}
