package com.gabriel.minimal.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Deliberately flat and low-contrast-on-purpose: the point of the launcher is to
// be boring enough that you stop opening it out of habit.
private val Dark = darkColorScheme(
    background = Color(0xFF0B0B0B),
    surface = Color(0xFF141414),
    onBackground = Color(0xFFE8E8E8),
    onSurface = Color(0xFFE8E8E8),
    primary = Color(0xFFE8E8E8),
    onPrimary = Color(0xFF0B0B0B),
    outline = Color(0xFF3A3A3A),
)

private val Light = lightColorScheme(
    background = Color(0xFFFAFAF8),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF161616),
    onSurface = Color(0xFF161616),
    primary = Color(0xFF161616),
    onPrimary = Color(0xFFFAFAF8),
    outline = Color(0xFFCFCFCF),
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
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        content = content,
    )
}
