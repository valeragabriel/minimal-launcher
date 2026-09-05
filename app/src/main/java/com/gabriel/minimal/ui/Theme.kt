package com.gabriel.minimal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Always black, regardless of the system light/dark setting: the point of the
// launcher is a dark, quiet surface, and a background photo sits on top of it.
// The greys are deliberately lighter than a typical dark theme — on pure black
// a #3A grey is effectively invisible.
private val Palette = darkColorScheme(
    background = Color(0xFF000000),
    surface = Color(0xFF101010),
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED),
    primary = Color(0xFFEDEDED),
    onPrimary = Color(0xFF000000),
    outline = Color(0xFF6E6E6E),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Light),
    bodyLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 15.sp),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun MinimalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Palette,
        typography = AppTypography,
        content = content,
    )
}
