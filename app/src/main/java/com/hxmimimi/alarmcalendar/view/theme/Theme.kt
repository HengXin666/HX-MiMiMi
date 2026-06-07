package com.hxmimimi.alarmcalendar.view.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PinkScheme = darkColorScheme(
    primary = Color(0xffff7aa8),
    secondary = Color(0xffffb3c9),
    tertiary = Color(0xff92e0d3),
    background = Color(0xff3c3c3c),
    surface = Color(0xff484848),
    surfaceVariant = Color(0xff555555),
    onPrimary = Color(0xff3c1020),
    onSecondary = Color(0xff3c1020),
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun HxTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PinkScheme, typography = MaterialTheme.typography, content = content)
}
