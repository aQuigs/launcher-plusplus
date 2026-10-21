package com.sqftware.orbitlauncher.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/**
 * The minimap of where the user is: a 152 px square holding an opaque disc on clear corners, meant to be scaled up by a
 * whole number with FilterQuality.None so its pixels stay crisp. Null while "Show where I am" is off, location access
 * is denied, or no map has been drawn yet. It changes as the user moves, so it is read where it is drawn.
 */
val LocalMinimap = compositionLocalOf<ImageBitmap?> { null }
