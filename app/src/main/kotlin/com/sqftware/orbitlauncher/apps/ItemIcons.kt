package com.sqftware.orbitlauncher.apps

import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import com.sqftware.orbitlauncher.domain.IconLayers
import com.sqftware.orbitlauncher.domain.Pixels

/**
 * The icon as the layers the item filter reads, each [size] pixels square: an adaptive icon's two layers, each its whole
 * canvas, so the filter can tell the mark from its ground, or a legacy icon whole.
 */
internal fun Drawable.layers(size: Int): IconLayers =
    if (this is AdaptiveIconDrawable) {
        IconLayers(foreground.pixels(size) ?: Pixels(size, size), background?.pixels(size))
    } else {
        IconLayers(toBitmap(size, size).pixels())
    }

private fun Drawable?.pixels(size: Int): Pixels? = this?.toBitmap(size, size)?.pixels()

private fun Bitmap.pixels(): Pixels {
    val argb = IntArray(width * height)
    getPixels(argb, 0, width, 0, 0, width, height)
    return Pixels(width, height, argb)
}

internal fun Pixels.toImageBitmap(): ImageBitmap =
    createBitmap(width, height).apply { setPixels(argb, 0, width, 0, 0, width, height) }.asImageBitmap()
