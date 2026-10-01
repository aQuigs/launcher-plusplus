package com.sqftware.orbitlauncher.domain

/**
 * A palette a theme comes in, one of those each [Theme] lists as its own: the theme's shapes in other colours. A theme
 * whose art is drawn in its own accent, not in materials of their own colour, has only the one it was made in.
 */
enum class Colourway(val label: String) {
    Midnight("Midnight"),
    Nebula("Nebula"),
    Aurora("Aurora"),
    Mars("Mars"),
    Brass("Brass"),
    Steel("Steel"),
    RoseGold("Rose gold"),
    Emerald("Emerald"),
    Parchment("Parchment"),
    Nautical("Nautical"),
    Desert("Desert"),
    Forest("Forest"),
    Sand("Sand"),
    Fern("Fern"),
    Slate("Slate"),
    Sakura("Sakura"),
    Sumi("Sumi"),
    Indigo("Indigo"),
    Sepia("Sepia"),
    Lime("Lime"),
    Coral("Coral"),
    Ice("Ice"),
    Green("Green"),
}
