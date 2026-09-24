package com.sqftware.orbitlauncher.ui

import androidx.compose.ui.graphics.ImageBitmap
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.AppShortcut

/** What the screen asks of the system for an app. The suspending calls do their blocking work off the main thread. */
class AppActions(
    val icon: suspend (AppEntry) -> ImageBitmap?,
    val launch: (AppEntry) -> Unit,
    val shortcuts: suspend (AppEntry) -> List<AppShortcut>,
    val shortcutIcon: suspend (AppShortcut) -> ImageBitmap?,
    val startShortcut: (AppShortcut) -> Unit,
    val openAppInfo: (AppEntry) -> Unit,
    val uninstall: (AppEntry) -> Unit,
)
