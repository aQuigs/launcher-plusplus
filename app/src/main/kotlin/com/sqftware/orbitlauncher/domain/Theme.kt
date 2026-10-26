package com.sqftware.orbitlauncher.domain

import com.sqftware.orbitlauncher.domain.minimap.MapStyle

/**
 * How the whole launcher looks, a launcher-wide setting: every surface a theme owns (CLAUDE.md lists them) drawn its way,
 * with its own [folderLooks] and [colourways] to pick from, the first of each until the user picks, apps' icons in the
 * [iconStyle] it gives each place, and, if it has a [mapStyle], a map of where the user is in that style once they turn
 * it on.
 */
enum class Theme(
    val label: String,
    val folderLooks: List<FolderLook>,
    val colourways: List<Colourway>,
    private val iconStyles: (IconPlace) -> IconStyle = { IconStyle.Plain },
    val mapStyle: MapStyle? = null,
) {
    Space(
        "Space",
        listOf(FolderLook.Rim, FolderLook.Ringed, FolderLook.Orbit, FolderLook.Plain, FolderLook.SolarSystem),
        listOf(Colourway.Midnight, Colourway.Nebula, Colourway.Aurora, Colourway.Mars),
    ),
    Clockwork(
        "Clockwork",
        listOf(FolderLook.SubDial, FolderLook.Gear),
        listOf(Colourway.Brass, Colourway.Steel, Colourway.RoseGold, Colourway.Emerald),
    ),
    Atlas(
        "Atlas",
        listOf(FolderLook.Island, FolderLook.Globe, FolderLook.FoldedMap),
        listOf(Colourway.Parchment, Colourway.Nautical, Colourway.Desert, Colourway.Forest),
    ),
    ZenGarden(
        "Zen garden",
        listOf(FolderLook.Stone, FolderLook.KoiPond, FolderLook.Moss),
        listOf(Colourway.Sand, Colourway.Fern, Colourway.Slate, Colourway.Sakura),
    ),
    Ink("Ink", listOf(FolderLook.Enso, FolderLook.Seal, FolderLook.InkDrop), listOf(Colourway.Sumi, Colourway.Indigo, Colourway.Sepia)),
    Germ("Germ", listOf(FolderLook.Bacillus, FolderLook.Virus, FolderLook.Cell), listOf(Colourway.Lime)),
    Paper("Paper", listOf(FolderLook.FoldedSquare, FolderLook.Envelope, FolderLook.Pinwheel), listOf(Colourway.Coral)),
    CrystalCity("Crystal city", listOf(FolderLook.Cube, FolderLook.Octahedron, FolderLook.Geode), listOf(Colourway.Ice)),
    GreenCore("Green core", listOf(FolderLook.WireSphere, FolderLook.Orb, FolderLook.HexCell), listOf(Colourway.Green)),
    OldSchool(
        "Old School",
        listOf(FolderLook.Items),
        listOf(Colourway.Classic),
        // The ring's icons sit in the game's round orbs, a smaller frame than an inventory slot, so an item there is a
        // size down: as large as fits inside the orb's black with its outline.
        iconStyles = { place -> IconStyle.Item(if (place == IconPlace.Ring) 22 else 26) },
        mapStyle = MapStyle.OldSchool,
    ),
    Skyrim("Skyrim", listOf(FolderLook.Shield), listOf(Colourway.Nord), mapStyle = MapStyle.Skyrim),
    ;

    fun iconStyle(place: IconPlace): IconStyle = iconStyles(place)

    val mapsWhereIAm: Boolean get() = mapStyle != null

    /** Whether the theme marks a quest the user sets on its compass, as the game's does. */
    val marksQuests: Boolean get() = mapStyle == MapStyle.Skyrim

    /** Whether the user has a folder look to choose, which a theme with one has not. */
    val choosesFolderLook: Boolean get() = folderLooks.size > 1

    /** Whether the user has a colourway to choose, which a theme with one has not. */
    val choosesColourway: Boolean get() = colourways.size > 1
}

/**
 * One of the few things each theme [offers][offered] its own of, picked in each theme and kept apart, so coming back to
 * a theme finds its own pick. A theme with none picked, or with a pick it does not offer, shows its first.
 */
class ThemePicks<T>(private val offered: (Theme) -> List<T>, private val picks: Map<Theme, T> = emptyMap()) {
    fun of(theme: Theme): T = picked(theme) ?: offered(theme).first()

    /** What was picked in [theme], if one of its own was. */
    fun picked(theme: Theme): T? = picks[theme]?.takeIf { it in offered(theme) }

    fun with(theme: Theme, pick: T): ThemePicks<T> = ThemePicks(offered, picks + (theme to pick))

    override fun equals(other: Any?) = other is ThemePicks<*> && other.picks == picks

    override fun hashCode() = picks.hashCode()

    override fun toString() = "ThemePicks($picks)"
}

typealias FolderLooks = ThemePicks<FolderLook>

/** No folder look picked in any theme. */
fun FolderLooks(): FolderLooks = ThemePicks(Theme::folderLooks)

typealias Colourways = ThemePicks<Colourway>

/** No colourway picked in any theme. */
fun Colourways(): Colourways = ThemePicks(Theme::colourways)
