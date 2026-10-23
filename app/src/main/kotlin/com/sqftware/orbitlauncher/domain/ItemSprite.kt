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
 * An app's [mark], as [itemMark] takes it from its icon, drawn as one of the game's inventory items: [size] pixels
 * across on the 36 by 32 item canvas, in a few flat colours with a hard edge, a black outline and the dark shadow every
 * item sprite carries. A dark item is outlined in white instead, as the game marks a chosen item, since it would vanish
 * into the black of an orb or a panel.
 */
fun itemSprite(mark: Pixels, size: Int, colours: Int = 14): Pixels {
    var small = hardEdged(scaled(mark, size))
    val tile = dropBackingTile(small)
    // What the item keeps of a mark (all but its tile, or a logo drawn small on its canvas) is cut out of the whole mark
    // and drawn again, so it fills the item as a whole icon does: the same shape, larger. Only what the item keeps is
    // cut, since the whole mark would bring back tile the item dropped, such as the ground inside a ring.
    var whole = mark.drawnFrom(small) { small.argb[it] != 0 }
    // Drawn larger, the tile that shared an edge pixel with the mark shows as a pixel of its own and is dropped, but
    // only along that edge: ground the item kept, sealed inside a ring, stays. An edge pixel can also fall below half
    // cover and clear, so the mark is cut again until it fills the item.
    val edge = edge(small).toSet()
    var band = mark.drawnFrom(small) { it in edge }
    for (pass in 1..3) {
        val kept = keptArea(small) ?: break
        val area = kept.scaledTo(small, whole)
        whole = whole.crop(area)
        band = band.crop(area)
        small = hardEdged(scaled(whole, size))
        if (tile != null) dropEdgeColour(small, tile, band.drawnOver(small))
    }
    posterize(small, colours)
    val item = placed(small)
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

/**
 * [pixels] scaled so its longer side is [size], each pixel the average of the block it covers, weighted by alpha so a
 * transparent edge does not darken it.
 */
internal fun scaled(pixels: Pixels, size: Int): Pixels {
    val scale = size.toFloat() / maxOf(pixels.width, pixels.height)
    val w = (pixels.width * scale).roundToInt().coerceAtLeast(1)
    val h = (pixels.height * scale).roundToInt().coerceAtLeast(1)
    val out = Pixels(w, h)
    for (y in 0 until h) for (x in 0 until w) {
        val xs = block(x, w, pixels.width)
        val ys = block(y, h, pixels.height)
        var a = 0L
        var r = 0L
        var g = 0L
        var b = 0L
        for (sy in ys) for (sx in xs) {
            val c = pixels[sx, sy]
            val ca = alpha(c)
            a += ca
            r += red(c) * ca
            g += green(c) * ca
            b += blue(c) * ca
        }
        val n = (xs.last - xs.first + 1) * (ys.last - ys.first + 1)
        out.argb[y * w + x] = if (a == 0L) 0 else argb((a / n).toInt(), (r / a).toInt(), (g / a).toInt(), (b / a).toInt())
    }
    return out
}

/** The pixels of a side [whole] long that [scaled] draws pixel [i] of [n] from. */
private fun block(i: Int, n: Int, whole: Int): IntRange {
    val from = i * whole / n
    return from until maxOf(from + 1, (i + 1) * whole / n)
}

/** This mark with only the pixels that [small], [scaled] from it, draws into the pixels [picked] picks. */
private fun Pixels.drawnFrom(small: Pixels, picked: (Int) -> Boolean): Pixels {
    val shown = BooleanArray(argb.size)
    for (sy in 0 until small.height) for (sx in 0 until small.width) {
        if (!picked(sy * small.width + sx)) continue
        for (y in block(sy, small.height, height)) for (x in block(sx, small.width, width)) shown[y * width + x] = true
    }
    return Pixels(width, height, IntArray(argb.size) { if (shown[it]) argb[it] else 0 })
}

/** Which pixels of [small], [scaled] from this, are drawn from any of its pixels. */
private fun Pixels.drawnOver(small: Pixels) = BooleanArray(small.argb.size) { i ->
    val xs = block(i % small.width, small.width, width)
    block(i / small.width, small.height, height).any { y -> xs.any { x -> this[x, y] != 0 } }
}

private class Area(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    /** This area of [small], drawn from [whole], as the area of [whole] it was drawn from. */
    fun scaledTo(small: Pixels, whole: Pixels) = Area(
        block(left, small.width, whole.width).first,
        block(top, small.height, whole.height).first,
        block(right - 1, small.width, whole.width).last + 1,
        block(bottom - 1, small.height, whole.height).last + 1,
    )
}

/** The part of [small] its mark takes up, or null when that is all of it or there is no mark. */
private fun keptArea(small: Pixels): Area? {
    val solid = small.argb.indices.filter { small.argb[it] != 0 }
    if (solid.isEmpty()) return null
    val area = Area(
        solid.minOf { it % small.width },
        solid.minOf { it / small.width },
        solid.maxOf { it % small.width } + 1,
        solid.maxOf { it / small.width } + 1,
    )
    return area.takeUnless { it.left == 0 && it.top == 0 && it.right == small.width && it.bottom == small.height }
}

private fun Pixels.crop(area: Area): Pixels {
    val w = area.right - area.left
    val h = area.bottom - area.top
    return Pixels(w, h, IntArray(w * h) { this[area.left + it % w, area.top + it / w] })
}

private fun hardEdged(pixels: Pixels) = Pixels(pixels.width, pixels.height, IntArray(pixels.argb.size) {
    val c = pixels.argb[it]
    if (alpha(c) >= 128) opaque(c) else 0
})

private fun placed(small: Pixels): Pixels {
    val item = Pixels(ITEM_WIDTH, ITEM_HEIGHT)
    val left = (ITEM_WIDTH - small.width) / 2
    val top = (ITEM_HEIGHT - small.height) / 2 - 1
    for (y in 0 until small.height) for (x in 0 until small.width) item.argb[(top + y) * ITEM_WIDTH + left + x] = small[x, y]
    return item
}

/**
 * A mark on a tile (a red letter on black, a logo on a white square) is no item's shape, and a dark tile would vanish
 * into the game's dark panels, so a tile is dropped: the very dark or very light colour that runs in from the mark's
 * whole edge, when it is most of the mark and leaves a mark of some size. Gives the tile's colour if it was dropped.
 */
private fun dropBackingTile(small: Pixels): Int? {
    val edge = edge(small)
    if (edge.isEmpty()) return null
    val dark = edge.count { luma(small.argb[it]) < 60 }
    val light = edge.count { luma(small.argb[it]) > 215 }
    if (maxOf(dark, light) < edge.size * 0.85) return null
    val colour = mean(edge.map { small.argb[it] })
    val tile = reachedFromEdge(small, edge, colour)
    val solid = small.argb.count { it != 0 }
    if (tile.size <= solid * 0.4 || solid - tile.size <= 20) return null
    tile.forEach { small.argb[it] = 0 }
    return colour
}

private fun dropEdgeColour(pixels: Pixels, colour: Int, within: BooleanArray) =
    reachedFromEdge(pixels, edge(pixels), colour, within).forEach { pixels.argb[it] = 0 }

private fun edge(pixels: Pixels) =
    pixels.argb.indices.filter { i -> pixels.argb[i] != 0 && neighbours(pixels, i).any { it == null || pixels.argb[it] == 0 } }

private fun reachedFromEdge(pixels: Pixels, edge: List<Int>, colour: Int, within: BooleanArray? = null): Set<Int> {
    val reached = HashSet<Int>()
    val stack = ArrayDeque(edge)
    while (stack.isNotEmpty()) {
        val i = stack.removeLast()
        if (i in reached || pixels.argb[i] == 0 || within?.get(i) == false || distance(pixels.argb[i], colour) >= 60) continue
        reached += i
        neighbours(pixels, i).filterNotNull().forEach(stack::addLast)
    }
    return reached
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
    for (i in item.argb.indices) if (!solid[i] && neighbours(item, i).any { it != null && solid[it] }) item.argb[i] = edge
    for (y in ITEM_HEIGHT - 1 downTo 1) for (x in ITEM_WIDTH - 1 downTo 1) {
        val i = y * ITEM_WIDTH + x
        if (item.argb[i] == 0 && item.argb[i - ITEM_WIDTH - 1] != 0) item.argb[i] = SHADOW
    }
}

private fun neighbours(pixels: Pixels, i: Int): List<Int?> {
    val x = i % pixels.width
    val y = i / pixels.width
    return listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1).map { (nx, ny) ->
        if (nx in 0 until pixels.width && ny in 0 until pixels.height) ny * pixels.width + nx else null
    }
}

private fun distance(c: Int, d: Int) = abs(red(c) - red(d)) + abs(green(c) - green(d)) + abs(blue(c) - blue(d))
