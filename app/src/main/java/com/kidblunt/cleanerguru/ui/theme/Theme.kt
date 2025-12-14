package com.kidblunt.cleanerguru.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorPalette = lightColors(
    primary = NeonPink,
    primaryVariant = PinkPrimaryDark,
    secondary = ElectricBlue,
    secondaryVariant = CloudBlueDark,
    background = Color(0xFFF0F0FF),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E)
)

private val DarkColorPalette = darkColors(
    primary = NeonPink,
    primaryVariant = PinkPrimaryDark,
    secondary = ElectricBlue,
    secondaryVariant = CloudBlueDark,
    background = DarkBackground,
    surface = CardBackground,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE6E1FF),
    onSurface = Color(0xFFE6E1FF)
)

@Composable
fun CleanerGuruTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}