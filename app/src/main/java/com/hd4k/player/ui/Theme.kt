package com.hd4k.player.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF0B1020)
val Panel = Color(0xFF151C33)
val Accent = Color(0xFFFFB020)
val TextDim = Color(0xFF9AA3BD)

@Composable
fun HdTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent, onPrimary = Color.Black,
            background = Bg, onBackground = Color.White,
            surface = Panel, onSurface = Color.White,
        ),
        content = content,
    )
}
