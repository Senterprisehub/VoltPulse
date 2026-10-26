package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VoltPulseDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianBg,
    primaryContainer = ObsidianSurfaceElevated,
    onPrimaryContainer = NeonCyan,
    secondary = NeonLime,
    onSecondary = ObsidianBg,
    secondaryContainer = ObsidianSurfaceElevated,
    onSecondaryContainer = NeonLime,
    tertiary = NeonAmber,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextWhite,
    surface = ObsidianSurface,
    onSurface = TextWhite,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextMuted,
    outline = ObsidianCardBorder,
    error = NeonCrimson,
    onError = TextWhite
)

@Composable
fun VoltPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VoltPulseDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    VoltPulseTheme(content = content)
}
