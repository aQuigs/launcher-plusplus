package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Cards, folders and tonal buttons are faint panes the wallpaper shows through, as Arc's are, rather than Material's
// solid greys. What floats over other content (menus, dialogs, a bin, edit handles) takes a solid container role instead.
private val Glass = Color.White.copy(alpha = 0.1f)

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    val material = darkColorScheme()
    // One content colour for both glass roles, which Material otherwise tells apart by value and cannot once they match.
    val colorScheme = material.copy(surfaceVariant = Glass, secondaryContainer = Glass, onSecondaryContainer = material.onSurface)
    MaterialTheme(colorScheme = colorScheme) {
        // Pages sit straight on the wallpaper, so text defaults to the on-background colour; surfaces set their own.
        CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground, content = content)
    }
}
