package com.sqftware.orbitlauncher.ui

import androidx.compose.ui.graphics.ImageBitmap
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.AppShortcut

/**
 * What the screen asks of the system for an app. The suspending calls do their blocking work off the main thread. An
 * app's [icon] is drawn in the theme's style, and its [ringIcon] in the style for the home ring's smaller frames.
 */
class AppActions(
    val icon: suspend (AppEntry) -> ImageBitmap?,
    val launch: (AppEntry) -> Unit,
    val clearBadge: (AppEntry) -> Unit,
    val shortcuts: suspend (AppEntry) -> List<AppShortcut>,
    val shortcutIcon: suspend (AppShortcut) -> ImageBitmap?,
    val startShortcut: (AppShortcut) -> Unit,
    val openAppInfo: (AppEntry) -> Unit,
    val hasStorePage: suspend (AppEntry) -> Boolean,
    val openStorePage: (AppEntry) -> Unit,
    val uninstall: (AppEntry) -> Unit,
    val ringIcon: suspend (AppEntry) -> ImageBitmap? = icon,
)
