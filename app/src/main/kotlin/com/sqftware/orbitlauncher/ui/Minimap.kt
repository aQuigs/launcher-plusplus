package com.sqftware.orbitlauncher.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.sqftware.orbitlauncher.domain.minimap.Minimap

/**
 * The minimap of where the user is: a 152 px square holding an opaque disc on clear corners, and the icons on it, meant
 * to be scaled up by a whole number with FilterQuality.None so their pixels stay crisp. Null while "Show where I am" is off, location access
 * is denied, or no map has been drawn yet. It changes as the user moves, so it is read where it is drawn.
 */
val LocalMinimap = staticCompositionLocalOf<() -> Minimap<ImageBitmap>?> { { null } }

/**
 * Which way the phone faces, in degrees clockwise from north, or null without a compass or while the map is off. Read
 * where it is drawn, so its many small changes redraw only what turns with it.
 */
val LocalHeading = staticCompositionLocalOf<() -> Float?> { { null } }
