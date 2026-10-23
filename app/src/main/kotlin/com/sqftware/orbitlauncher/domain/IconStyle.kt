package com.sqftware.orbitlauncher.domain

/**
 * Where an app's icon is shown, for a theme that frames the home ring's icons apart from the rest, or draws a folder's
 * previews, small squares, apart from icons of their own.
 */
enum class IconPlace { Ring, Preview, Elsewhere }

/** How an app's icon is drawn: as the app made it, or, for a theme set in a game, as one of the game's inventory items. */
sealed interface IconStyle {
    data object Plain : IconStyle

    /**
     * An item with the app's mark at most [size] pixels across, on the game's item canvas, or, if [cut], trimmed to the
     * mark and cut to its square, so it fills a small square such as a folder's preview.
     */
    data class Item(val size: Int, val cut: Boolean = false) : IconStyle
}
