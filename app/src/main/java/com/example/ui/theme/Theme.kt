package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DrishtiDarkColorScheme = darkColorScheme(
    primary = HighContrastYellow,
    onPrimary = AccessibleBlack,
    primaryContainer = HighContrastAmber,
    onPrimaryContainer = AccessibleBlack,
    secondary = HighContrastCyan,
    onSecondary = AccessibleBlack,
    background = AccessibleBlack,
    onBackground = HighContrastTextPrimary,
    surface = AccessibleDarkSurface,
    onSurface = HighContrastTextPrimary,
    surfaceVariant = AccessibleElevatedCard,
    onSurfaceVariant = HighContrastTextSecondary,
    error = HighContrastRed,
    onError = AccessibleBlack,
    outline = HighContrastBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // For blind and low-vision assistance, strict high-contrast dark palette is enforced
    MaterialTheme(
        colorScheme = DrishtiDarkColorScheme,
        typography = Typography,
        content = content
    )
}
