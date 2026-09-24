package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme()
    MaterialTheme(colorScheme = colorScheme) {
        // Pages sit straight on the wallpaper, so text defaults to the on-background colour; surfaces set their own.
        CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground, content = content)
    }
}
