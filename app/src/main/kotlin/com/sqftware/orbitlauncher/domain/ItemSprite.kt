package com.sqftware.orbitlauncher.domain

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * An app's icon as the system gives it: an adaptive icon's two layers, each the full 108-unit canvas, or a legacy
 * icon as its [foreground] alone with no [background].
 */
class IconLayers(val foreground: Pixels, val background: Pixels? = null)

/** The game's item canvas, which every item sprite fills. */
internal const val ITEM_WIDTH = 36
internal const val ITEM_HEIGHT = 32

/**
 * How many pixels across to draw an icon's layers for its [itemMark]: one to each of an adaptive icon's 108 units, which
 * leaves a mark of 72, a few pixels to each of the largest item's. Fixed, so an item looks the same on every phone.
 */
const val ITEM_LAYER_SIZE = 108

/**
 * An app's [mark], as [itemMark] takes it from its icon, drawn as one of the game's inventory items: at most [size]
 * pixels across on the 36 by 32 item canvas, in a few flat colours with a hard edge, a black outline and the dark shadow
 * every item sprite carries. A dark item is outlined in white instead, as the game marks a chosen item, since it would
 * vanish into the black of an orb or a panel.
 */
fun itemSprite(mark: Pixels, size: Int, colours: Int = 14): Pixels {
    val small = downscale(mark, size)
    val item = Pixels(ITEM_WIDTH, ITEM_HEIGHT)
    val left = (ITEM_WIDTH - small.width) / 2
    val top = (ITEM_HEIGHT - small.height) / 2 - 1
    for (y in 0 until small.height) for (x in 0 until small.width) {
        val c = small[x, y]
        item.argb[(top + y) * ITEM_WIDTH + left + x] = if (alpha(c) >= 128) opaque(c) else 0
    }
    dropBackingTile(item)
    posterize(item, colours)
    outline(item, if (isDark(item)) WHITE else BLACK)
    return item
}

/**
 * What of [icon] the item shows. An adaptive icon on a plain light ground is its foreground alone, the way the brand
 * draws its logo; on a coloured ground the colour is part of the mark (a blue bubble, a green phone), so both layers
 * go into a disc, as they do when the logo is drawn on the ground and the foreground is all but empty. Either way only
 * the middle of the canvas, where the system keeps the mark, is taken. The same for every size of item, so made once.
 */
fun itemMark(icon: IconLayers): Pixels {
    val background = icon.background ?: return icon.foreground
    val fore = safeZone(icon.foreground)
    val back = safeZone(background)
    if (isPlainLight(back) && !isNearlyEmpty(fore)) return fore
    val out = Pixels(fore.width, fore.height)
    val r = fore.width / 2f
    for (y in 0 until fore.height) for (x in 0 until fore.width) {
        val dx = x + 0.5f - r
        val dy = y + 0.5f - r
        if (dx * dx + dy * dy <= r * r) out.argb[y * fore.width + x] = over(fore[x, y], back[x, y])
    }
    return out
}

// The system masks an adaptive icon to its middle 72 of 108 units; the mark itself keeps within the middle 66.
private fun safeZone(layer: Pixels): Pixels {
    val side = (layer.width * 72 / 108f).roundToInt()
    val from = (layer.width - side) / 2
    return Pixels(side, side, IntArray(side * side) { i -> layer[from + i % side, from + i / side] })
}

private fun isNearlyEmpty(layer: Pixels) = layer.argb.count { alpha(it) >= 128 } < layer.argb.size / 50

private fun isPlainLight(layer: Pixels): Boolean {
    val opaque = layer.argb.filter { alpha(it) >= 128 }
    if (opaque.size < layer.argb.size / 2) return false
    return opaque.count { luma(it) > 200 } >= opaque.size * 0.9
}

/** [pixels] shrunk to fit [size] by averaging each block, weighted by alpha so a transparent edge does not darken it. */
internal fun downscale(pixels: Pixels, size: Int): Pixels {
    val scale = size.toFloat() / maxOf(pixels.width, pixels.height)
    if (scale >= 1f) return pixels
    val w = (pixels.width * scale).roundToInt().coerceAtLeast(1)
    val h = (pixels.height * scale).roundToInt().coerceAtLeast(1)
    val out = Pixels(w, h)
    for (y in 0 until h) for (x in 0 until w) {
        val x0 = x * pixels.width / w
        val x1 = maxOf(x0 + 1, (x + 1) * pixels.width / w)
        val y0 = y * pixels.height / h
        val y1 = maxOf(y0 + 1, (y + 1) * pixels.height / h)
        var a = 0L
        var r = 0L
        var g = 0L
        var b = 0L
        for (sy in y0 until y1) for (sx in x0 until x1) {
            val c = pixels[sx, sy]
            val ca = alpha(c)
            a += ca
            r += red(c) * ca
            g += green(c) * ca
            b += blue(c) * ca
        }
        val n = (x1 - x0) * (y1 - y0)
        out.argb[y * w + x] = if (a == 0L) 0 else argb((a / n).toInt(), (r / a).toInt(), (g / a).toInt(), (b / a).toInt())
    }
    return out
}

/**
 * A mark on a tile (a red letter on black, a logo on a white square) is no item's shape, and a dark tile would vanish
 * into the game's dark panels, so a tile is dropped: the very dark or very light colour that runs in from the item's
 * whole edge, when it is most of the item and leaves a mark of some size.
 */
private fun dropBackingTile(item: Pixels) {
    val solid = item.argb.indices.filter { item.argb[it] != 0 }
    val edge = solid.filter { i -> neighbours(i).any { it == null || item.argb[it] == 0 } }
    if (edge.isEmpty()) return
    val dark = edge.count { luma(item.argb[it]) < 60 }
    val light = edge.count { luma(item.argb[it]) > 215 }
    if (maxOf(dark, light) < edge.size * 0.85) return
    val mean = mean(edge.map { item.argb[it] })
    val tile = HashSet<Int>()
    val stack = ArrayDeque(edge)
    while (stack.isNotEmpty()) {
        val i = stack.removeLast()
        if (i in tile || item.argb[i] == 0 || distance(item.argb[i], mean) >= 60) continue
        tile += i
        neighbours(i).filterNotNull().forEach(stack::addLast)
    }
    if (tile.size > solid.size * 0.4 && solid.size - tile.size > 20) tile.forEach { item.argb[it] = 0 }
}

/** The opaque pixels pulled to at most [colours] flat colours by median cut, each a touch muted toward the game's range. */
private fun posterize(item: Pixels, colours: Int) {
    val boxes = mutableListOf(item.argb.indices.filter { item.argb[it] != 0 })
    while (boxes.size < colours) {
        val at = boxes.indices.maxBy { boxes[it].size }
        val biggest = boxes[at]
        if (biggest.size < 2) break
        val channel = CHANNELS.maxBy { ch -> biggest.maxOf { ch(item.argb[it]) } - biggest.minOf { ch(item.argb[it]) } }
        val sorted = biggest.sortedBy { channel(item.argb[it]) }
        boxes.removeAt(at)
        boxes += sorted.subList(0, sorted.size / 2)
        boxes += sorted.subList(sorted.size / 2, sorted.size)
    }
    for (box in boxes.filter { it.isNotEmpty() }) {
        val m = mean(box.map { item.argb[it] })
        val grey = (red(m) + green(m) + blue(m)) / 3f
        val muted = { v: Int -> ((v * 0.85f + grey * 0.15f) * 0.92f).roundToInt() }
        val colour = rgb(muted(red(m)), muted(green(m)), muted(blue(m)))
        box.forEach { item.argb[it] = colour }
    }
}

private val CHANNELS = listOf(::red, ::green, ::blue)

private const val SHADOW = 0xFF302020.toInt()

/** Whether most of [item] is too dark to tell from the black it sits on. */
private fun isDark(item: Pixels): Boolean {
    val lumas = item.argb.filter { it != 0 }.map(::luma).sorted()
    return lumas.isNotEmpty() && lumas[lumas.size / 2] < 50
}

private fun outline(item: Pixels, edge: Int) {
    val solid = BooleanArray(item.argb.size) { item.argb[it] != 0 }
    for (i in item.argb.indices) if (!solid[i] && neighbours(i).any { it != null && solid[it] }) item.argb[i] = edge
    for (y in ITEM_HEIGHT - 1 downTo 1) for (x in ITEM_WIDTH - 1 downTo 1) {
        val i = y * ITEM_WIDTH + x
        if (item.argb[i] == 0 && item.argb[i - ITEM_WIDTH - 1] != 0) item.argb[i] = SHADOW
    }
}

private fun neighbours(i: Int): List<Int?> {
    val x = i % ITEM_WIDTH
    val y = i / ITEM_WIDTH
    return listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1).map { (nx, ny) ->
        if (nx in 0 until ITEM_WIDTH && ny in 0 until ITEM_HEIGHT) ny * ITEM_WIDTH + nx else null
    }
}

private fun distance(c: Int, d: Int) = abs(red(c) - red(d)) + abs(green(c) - green(d)) + abs(blue(c) - blue(d))
