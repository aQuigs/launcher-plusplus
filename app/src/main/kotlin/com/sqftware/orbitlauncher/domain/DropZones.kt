package com.sqftware.orbitlauncher.domain

import kotlin.math.hypot
import kotlin.math.min

/** A rectangle on the screen in pixels. Its edges count as inside. */
data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom

    /** Whether ([x], [y]) is within the largest disc that fits, centred, or that disc shrunk to [fraction] of it. */
    fun discContains(x: Float, y: Float, fraction: Float = 1f): Boolean =
        hypot(x - (left + right) / 2, y - (top + bottom) / 2) <= min(right - left, bottom - top) * fraction / 2
}

/**
 * Where an app dragged over the home screen can land: the disc the [ring] is laid out in, which its slots never leave,
 * and the [dock]'s row. Either is null until it has been laid out.
 */
data class DropZones(val ring: Bounds? = null, val dock: Bounds? = null) {
    /** The place under ([x], [y]), or null over neither. */
    fun placeAt(x: Float, y: Float): HomePlace? = when {
        ring?.discContains(x, y) == true -> HomePlace.Ring
        dock?.contains(x, y) == true -> HomePlace.Dock
        else -> null
    }
}
