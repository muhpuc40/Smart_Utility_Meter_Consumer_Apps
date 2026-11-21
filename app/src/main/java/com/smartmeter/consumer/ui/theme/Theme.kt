package com.smartmeter.consumer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Accent colors used across screens
val EnergyColor = Color(0xFFF59E0B)
val WaterColor = Color(0xFF0288D1)
val SuccessColor = Color(0xFF16A34A)
val DangerColor = Color(0xFFDC2626)
val WarningColor = Color(0xFFD97706)

private val LightColors = lightColorScheme(
    primary = Color(0xFF00796B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF0277BD),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E8F7),
    onSecondaryContainer = Color(0xFF001D31),
    background = Color(0xFFF4F7F7),
    onBackground = Color(0xFF161D1C),
    surface = Color.White,
    onSurface = Color(0xFF161D1C),
    surfaceVariant = Color(0xFFE3EBE9),
    onSurfaceVariant = Color(0xFF3F4947),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DB6AC),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFF81C7F5),
    onSecondary = Color(0xFF003450),
    secondaryContainer = Color(0xFF004C72),
    onSecondaryContainer = Color(0xFFD0E8F7),
    background = Color(0xFF0F1514),
    onBackground = Color(0xFFDDE4E2),
    surface = Color(0xFF171D1C),
    onSurface = Color(0xFFDDE4E2),
    surfaceVariant = Color(0xFF3F4947),
    onSurfaceVariant = Color(0xFFBEC9C6),
    error = Color(0xFFFFB4AB),
)

@Composable
fun SmartMeterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
