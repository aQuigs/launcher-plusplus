package com.sqftware.orbitlauncher.domain

/**
 * How an app's icon is drawn: as the app made it, or, for a theme set in a game, as one of the game's inventory items
 * with the app's mark at most [itemSize] pixels across.
 */
enum class IconStyle(val itemSize: Int?) {
    Plain(null),
    Item(26),
    SmallItem(17),
    ;

    /** This style where an icon sits in a smaller frame, as in the game's round orbs: an item a size down. */
    val small: IconStyle
        get() = if (this == Item) SmallItem else this
}
