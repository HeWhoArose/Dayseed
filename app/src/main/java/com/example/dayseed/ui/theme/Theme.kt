package com.example.dayseed.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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

@Composable
fun DayseedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }


    MaterialTheme(
        colorScheme = colorScheme,
        typography = DayseedTypography,
        content = content
    )
}
