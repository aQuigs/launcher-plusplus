package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemSpriteTest {
    private val red = 0xFFE01010.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private val blue = 0xFF1A73E8.toInt()

    private fun square(side: Int, colour: (Int, Int) -> Int) = Pixels(side, side, IntArray(side * side) { colour(it % side, it / side) })

    private fun disc(side: Int, colour: Int) = square(side) { x, y ->
        val r = side / 2f
        if ((x + 0.5f - r) * (x + 0.5f - r) + (y + 0.5f - r) * (y + 0.5f - r) <= r * r) colour else 0
    }

    private fun Pixels.colours() = argb.filter { it != 0 }.toSet()

    @Test
    fun anItemFillsTheGameItemCanvas() {
        val item = itemSprite(disc(96, red), size = 26)
        assertEquals(ITEM_WIDTH, item.width)
        assertEquals(ITEM_HEIGHT, item.height)
    }

    @Test
    fun theMarkKeepsWithinItsSizeAndIsOutlinedInBlack() {
        val item = itemSprite(disc(96, red), size = 17)
        val solid = item.argb.indices.filter { item.argb[it] != 0 && item.argb[it] != black && item.argb[it] != 0xFF302020.toInt() }
        val xs = solid.map { it % ITEM_WIDTH }
        assertTrue(xs.max() - xs.min() + 1 <= 17)
        assertTrue(black in item.colours())
    }

    @Test
    fun everyPixelIsOpaqueOrClear() {
        val soft = square(96) { x, _ -> (x * 255 / 95) shl 24 or 0x2050A0 }
        val item = itemSprite(soft, size = 26)
        assertTrue(item.argb.all { it == 0 || it ushr 24 == 255 })
    }

    @Test
    fun aGradientIsCutToTheGivenNumberOfColours() {
        val gradient = square(96) { x, y -> 0xFF000000.toInt() or (x * 2 shl 16) or (y * 2 shl 8) or 0x40 }
        val item = itemSprite(gradient, size = 26, colours = 6)
        // The mark's colours, plus the outline and the shadow.
        assertTrue(item.colours().size <= 6 + 2)
    }

    @Test
    fun aDarkTileUnderAMarkIsDropped() {
        val netflix = square(96) { x, _ -> if (x in 36..59) red else black }
        val item = itemSprite(netflix, size = 26)
        val reddish = item.colours().filter { (it shr 16 and 0xFF) > 120 }
        assertTrue(reddish.isNotEmpty())
        val solid = item.argb.count { it != 0 && it != black && it != 0xFF302020.toInt() }
        assertTrue("only the mark is left, not the tile: $solid", solid < 26 * 26 / 2)
    }

    @Test
    fun aDarkMarkWithNoTileIsKeptAndOutlinedInWhite() {
        val item = itemSprite(disc(96, 0xFF202020.toInt()), size = 26)
        assertTrue(item.argb.count { it != 0 } > 26 * 26 / 2)
        assertTrue(white in item.colours())
        assertTrue(black !in item.colours())
    }

    @Test
    fun aWhiteTileUnderAMarkIsDropped() {
        val play = square(96) { x, y -> if (x in 30..65 && y in 30..65) blue else white }
        val item = itemSprite(play, size = 26)
        val whiteish = item.argb.count { it != 0 && luma(it) > 215 }
        assertTrue("only the mark is left, not the tile: $whiteish", whiteish < 26 * 4)
    }

    @Test
    fun anAdaptiveIconOnAPlainLightGroundShowsItsForegroundAlone() {
        val logo = square(108) { x, y -> if (x in 44..63 && y in 44..63) red else 0 }
        val mark = itemMark(IconLayers(logo, square(108) { _, _ -> white }))
        assertTrue(white !in mark.colours())
    }

    @Test
    fun anAdaptiveIconWithItsLogoOnALightGroundShowsTheGround() {
        val ground = square(108) { x, y -> if (x in 44..63 && y in 44..63) red else white }
        val mark = itemMark(IconLayers(square(108) { _, _ -> 0 }, ground))
        assertTrue(red in mark.colours())
    }

    @Test
    fun anAdaptiveIconOnAColouredGroundKeepsItsGroundInADisc() {
        val logo = square(108) { x, y -> if (x in 44..63 && y in 44..63) white else 0 }
        val mark = itemMark(IconLayers(logo, square(108) { _, _ -> blue }))
        assertEquals(blue, mark[mark.width / 2, 2])
        assertEquals(0, mark[0, 0])
    }

    @Test
    fun aSmallLogoIsDrawnAsLargeAsAFullMark() {
        val logo = square(72) { x, y -> if (x in 26..45 && y in 26..45) red else 0 }
        assertEquals(26, markSpan(itemSprite(logo, size = 26)))
    }

    @Test
    fun theMarkLeftByATileIsDrawnAsLargeAsTheTileWas() {
        val play = square(96) { x, y -> if (x in 30..65 && y in 30..65) blue else white }
        assertEquals(26, markSpan(itemSprite(play, size = 26)))
    }

    private fun markSpan(item: Pixels): Int {
        val xs = item.argb.indices.filter { item.argb[it] != 0 && item.argb[it] != black && item.argb[it] != 0xFF302020.toInt() }.map { it % ITEM_WIDTH }
        return xs.max() - xs.min() + 1
    }

    @Test
    fun scalingIgnoresTheColourOfClearPixels() {
        val half = square(4) { x, _ -> if (x < 2) red else 0x00FFFFFF }
        assertEquals(red and 0xFFFFFF, scaled(half, 1)[0, 0] and 0xFFFFFF)
    }
}
