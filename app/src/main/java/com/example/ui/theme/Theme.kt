package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ProxiyaColorScheme = darkColorScheme(
    primary = CyberSky,
    onPrimary = Color.Black,
    primaryContainer = ElectricBlue,
    onPrimaryContainer = Color.White,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    background = DarkSlate900,
    onBackground = TextPrimary,
    surface = DarkSlate800,
    onSurface = TextPrimary,
    surfaceVariant = DarkSlate700,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = AlertRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ProxiyaColorScheme,
        typography = Typography,
        content = content
    )
}

