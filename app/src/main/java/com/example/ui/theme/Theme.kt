package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DidehbonDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CyberNavyDark,
    primaryContainer = CyberNavySurfaceVariant,
    onPrimaryContainer = NeonCyan,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    tertiary = CyberTeal,
    onTertiary = Color.Black,
    background = CyberNavyDark,
    onBackground = TextPrimary,
    surface = CyberNavySurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberNavySurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder,
    error = MarketRed,
    onError = Color.White
)

@Composable
fun DidehbonTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DidehbonDarkColorScheme,
        typography = Typography,
        content = content
    )
}

// Retain alias for any existing reference
@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) = DidehbonTheme(content = content)
