package com.sqftware.orbitlauncher.domain

import kotlin.math.roundToInt

/** An image as packed ARGB pixels, non-premultiplied, row by row: [width] across and [height] down. */
class Pixels(val width: Int, val height: Int, val argb: IntArray = IntArray(width * height)) {
    init {
        require(argb.size == width * height)
    }

    operator fun get(x: Int, y: Int) = argb[y * width + x]
}

internal const val BLACK = 0xFF000000.toInt()

internal fun alpha(c: Int) = c ushr 24
internal fun red(c: Int) = c shr 16 and 0xFF
internal fun green(c: Int) = c shr 8 and 0xFF
internal fun blue(c: Int) = c and 0xFF
internal fun argb(a: Int, r: Int, g: Int, b: Int) = a shl 24 or (r shl 16) or (g shl 8) or b
internal fun rgb(r: Int, g: Int, b: Int) = argb(0xFF, r, g, b)
internal fun opaque(c: Int) = c or BLACK
internal fun luma(c: Int) = (red(c) + green(c) + blue(c)) / 3

internal fun mean(cs: List<Int>) = rgb(cs.sumOf { red(it) } / cs.size, cs.sumOf { green(it) } / cs.size, cs.sumOf { blue(it) } / cs.size)

/** [top] laid over [under] by its alpha. */
internal fun over(top: Int, under: Int): Int {
    val a = alpha(top) / 255f
    val blend = { t: Int, u: Int -> (t * a + u * (1 - a)).roundToInt() }
    return argb(maxOf(alpha(top), alpha(under)), blend(red(top), red(under)), blend(green(top), green(under)), blend(blue(top), blue(under)))
}

/** The opaque colour [f] of the way from [from] to [to], channel by channel. */
internal fun lerpRgb(from: Int, to: Int, f: Double): Int {
    val lerp = { a: Int, b: Int -> (a + (b - a) * f).roundToInt() }
    return rgb(lerp(red(from), red(to)), lerp(green(from), green(to)), lerp(blue(from), blue(to)))
}
