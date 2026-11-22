package com.sqftware.orbitlauncher.ui

import androidx.compose.ui.graphics.ImageBitmap
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.AppShortcut
import com.sqftware.orbitlauncher.domain.IconPlace
import com.sqftware.orbitlauncher.domain.InputKind
import com.sqftware.orbitlauncher.domain.InputValue
import com.sqftware.orbitlauncher.domain.PlaceValue

/** Draws an app's icon as the theme shows it in a place, off the main thread; null while it has none. */
typealias IconLoader = suspend (AppEntry, IconPlace) -> ImageBitmap?

/**
 * What the screen asks of the system for an app. The suspending calls do their blocking work off the main thread. [launch]
 * takes the answers to what the app [asks][com.sqftware.orbitlauncher.domain.asks] for, which [findPlaces] and [openMaps]
 * help find.
 */
class AppActions(
    val icon: IconLoader,
    val launch: (AppEntry, Map<InputKind, InputValue>) -> Unit,
    val clearBadge: (AppEntry) -> Unit,
    val shortcuts: suspend (AppEntry) -> List<AppShortcut>,
    val shortcutIcon: suspend (AppShortcut) -> ImageBitmap?,
    val startShortcut: (AppShortcut) -> Unit,
    val openAppInfo: (AppEntry) -> Unit,
    val hasStorePage: suspend (AppEntry) -> Boolean,
    val openStorePage: (AppEntry) -> Unit,
    val uninstall: (AppEntry) -> Unit,
    val findPlaces: suspend (String) -> List<PlaceValue>,
    val openMaps: () -> Unit,
)
