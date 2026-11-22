package com.sqftware.orbitlauncher.domain

/** Where an app's icon is shown, for a theme that frames the home ring's icons, and an open folder's round it, apart from the rest. */
enum class IconPlace { Ring, OpenFolder, Elsewhere }

/** How an app's icon is drawn: as the app made it, or, for a theme set in a game, as one of the game's inventory items. */
sealed interface IconStyle {
    data object Plain : IconStyle

    /** An item with the app's mark at most [size] pixels across. */
    data class Item(val size: Int) : IconStyle
}
