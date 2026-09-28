package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NexusPrimary,
    onPrimary = Color.White,
    primaryContainer = NexusDarkSurfaceVariant,
    onPrimaryContainer = Color.White,
    secondary = NexusAccent,
    onSecondary = Color.Black,
    background = NexusDarkBackground,
    onBackground = NexusDarkTextPrimary,
    surface = NexusDarkSurface,
    onSurface = NexusDarkTextPrimary,
    surfaceVariant = NexusDarkSurfaceVariant,
    onSurfaceVariant = NexusDarkTextSecondary,
    outline = NexusDarkDivider,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = NexusPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = NexusPrimary,
    secondary = NexusPrimaryDark,
    onSecondary = Color.White,
    background = NexusLightBackground,
    onBackground = NexusLightTextPrimary,
    surface = NexusLightSurface,
    onSurface = NexusLightTextPrimary,
    surfaceVariant = NexusLightSurfaceVariant,
    onSurfaceVariant = NexusLightTextSecondary,
    outline = NexusLightDivider,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to Dark Theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
