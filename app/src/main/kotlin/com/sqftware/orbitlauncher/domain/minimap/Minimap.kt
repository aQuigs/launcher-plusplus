package com.sqftware.orbitlauncher.domain.minimap

/**
 * A painted minimap: the [disc] of ground, walls, trees and people, which turns with the phone, and the map [icons] on
 * it, kept apart so whoever shows the map keeps them upright as the game does. [Image] is what its art is held as.
 */
class Minimap<Image>(val disc: Image, val icons: List<MapMark<Image>>) {
    /** The same map with its art held as [convert] turns it. */
    fun <Other> map(convert: (Image) -> Other): Minimap<Other> =
        Minimap(convert(disc), icons.map { MapMark(convert(it.image), it.left, it.top, it.width, it.height) })
}

/** A map icon's [image], [width] by [height] px, its top left at the disc's pixel ([left], [top]). */
class MapMark<Image>(val image: Image, val left: Int, val top: Int, val width: Int, val height: Int) {
    /** Where the icon's middle lies on the disc, in pixels from its top left. */
    val centreX: Float get() = left + width / 2f
    val centreY: Float get() = top + height / 2f
}
