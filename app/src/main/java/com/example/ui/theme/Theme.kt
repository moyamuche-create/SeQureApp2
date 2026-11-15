package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = PurpleSurfaceTint,
    onPrimaryContainer = PurpleDeep,
    secondary = PurpleAccent,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = NeutralTextPrimary,
    tertiary = TacticalGreen,
    onTertiary = Color.White,
    error = EmergencyRed,
    onError = Color.White,
    errorContainer = EmergencyRedDark,
    onErrorContainer = Color.White,
    background = SecurityNavyDark,
    onBackground = NeutralTextPrimary,
    surface = SecurityNavySurface,
    onSurface = NeutralTextPrimary,
    surfaceVariant = PurpleSurfaceTint,
    onSurfaceVariant = NeutralTextSecondary,
    outline = NeutralBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
