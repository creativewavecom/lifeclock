package com.lifeclock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LifeClockDarkScheme = darkColorScheme(
    primary = AppPrimary,
    onPrimary = AppOnPrimary,
    primaryContainer = AppSurfaceVariant,
    onPrimaryContainer = AppTextPrimary,
    secondary = AppSecondary,
    onSecondary = AppTextPrimary,
    background = AppBg,
    onBackground = AppTextPrimary,
    surface = AppSurface,
    onSurface = AppTextPrimary,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = AppTextSecondary,
    outline = AppOutline,
)

@Composable
fun LifeClockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // App is dark-only by design (clocks read better on dark backgrounds).
    // The user can still set their phone's UI mode independently.
    MaterialTheme(
        colorScheme = LifeClockDarkScheme,
        typography = AppTypography,
        content = content
    )
}
