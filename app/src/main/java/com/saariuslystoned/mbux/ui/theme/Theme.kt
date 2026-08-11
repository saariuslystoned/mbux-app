package com.saariuslystoned.mbux.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CompanionColors = darkColorScheme(
    primary = Color(0xFF7CC7FF),
    onPrimary = Color(0xFF002F48),
    secondary = Color(0xFF9FD4C4),
    secondaryContainer = Color(0xFF173F3A),
    onSecondaryContainer = Color(0xFFC2F0E4),
    background = Color(0xFF08131F),
    onBackground = Color(0xFFE7F1FA),
    surface = Color(0xFF111F2D),
    onSurface = Color(0xFFE7F1FA),
    surfaceVariant = Color(0xFF1B2A39),
    onSurfaceVariant = Color(0xFFBDCAD6),
    outline = Color(0xFF708090),
    error = Color(0xFFFFB4AB),
)

@Composable
fun MbuxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CompanionColors,
        content = content,
    )
}
