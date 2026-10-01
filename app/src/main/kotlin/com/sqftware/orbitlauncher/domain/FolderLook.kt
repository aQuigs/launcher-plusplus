package com.sqftware.orbitlauncher.domain

/** How the folders on the home screen are drawn. Each [Theme] has its own, and the user picks one of the theme's. */
enum class FolderLook(val label: String) {
    /** The folder's disc of previews, with a moon on its edge for every app in it. */
    Rim("Moons on the rim"),

    /** The disc of previews crossed by a tilted ring, like Saturn's. */
    Ringed("Ringed planet"),

    /** A small planet with its apps going round it. */
    Orbit("Moons in orbit"),

    /** The disc of previews alone. */
    Plain("Plain folder"),

    /** Each folder a different planet of the Solar system, in order. */
    SolarSystem("Solar system"),

    /** A small dial of its own set into the watch's face, its previews in the middle. */
    SubDial("Sub-dial"),

    /** A brass cog with the previews in its hub. */
    Gear("Gear"),

    /** An island ringed by contour lines, its previews on the land. */
    Island("Island"),

    /** A small globe, its previews over the seas. */
    Globe("Globe"),

    /** A map folded in three, its route marked to an X. */
    FoldedMap("Folded map"),

    /** A smooth stone, its previews on it. */
    Stone("Stone"),

    /** A pond with koi swimming round under its previews. */
    KoiPond("Koi pond"),

    /** A patch of moss, its previews on it. */
    Moss("Moss"),

    /** A grey wash brushed round in one ink stroke. */
    Enso("Ensō"),

    /** A red seal pressed a little askew, its previews in the carved square. */
    Seal("Seal"),

    /** A drop of ink bleeding out into the paper under its previews. */
    InkDrop("Ink drop"),

    /** A rod-shaped bacterium waving its flagella, its previews inside. */
    Bacillus("Bacillus"),

    /** A virus turning slowly, its previews on it. */
    Virus("Virus"),

    /** A cell whose membrane wobbles, its previews inside. */
    Cell("Cell"),

    /** A square of paper folded on its point, its previews on it. */
    FoldedSquare("Folded square"),

    /** A paper envelope, its previews on it. */
    Envelope("Envelope"),

    /** A paper pinwheel spinning in a breeze under its previews. */
    Pinwheel("Pinwheel"),

    /** A glass cube seen corner on, its previews on it. */
    Cube("Cube"),

    /** A glass octahedron turning slowly under its previews. */
    Octahedron("Octahedron"),

    /** A geode broken open, its previews in the hollow. */
    Geode("Geode"),
}

/**
 * Whether folders in this look move as the folders' sky turns: in the Solar system only if one of the [planets] shown
 * moves, and in the looks that turn whenever there are [folders] at all.
 */
fun FolderLook.moves(folders: Boolean, planets: Collection<Planet>) = when (this) {
    FolderLook.SolarSystem -> planets.any(Planet::moves)
    FolderLook.Orbit, FolderLook.Gear, FolderLook.Globe, FolderLook.KoiPond, FolderLook.InkDrop,
    FolderLook.Bacillus, FolderLook.Virus, FolderLook.Cell, FolderLook.Pinwheel, FolderLook.Octahedron -> folders
    FolderLook.Rim, FolderLook.Ringed, FolderLook.Plain, FolderLook.SubDial, FolderLook.Island, FolderLook.FoldedMap,
    FolderLook.Stone, FolderLook.Moss, FolderLook.Enso, FolderLook.Seal,
    FolderLook.FoldedSquare, FolderLook.Envelope,
    FolderLook.Cube, FolderLook.Geode -> false
}

/** The planets of the [FolderLook.SolarSystem] look, in order from the Sun. Pluto counts. Those that [move] turn with the sky. */
enum class Planet(val moves: Boolean = true) {
    Mercury(moves = false),
    Venus,
    Earth,
    Mars,
    Jupiter,
    Saturn(moves = false),
    Uranus(moves = false),
    Neptune,
    Pluto,
}

/** Which planet a folder is in the Solar system: the first free one, a plain folder, or one the user picked. */
sealed interface PlanetPick {
    data object Auto : PlanetPick

    data object Plain : PlanetPick

    data class Of(val planet: Planet) : PlanetPick
}

/**
 * The planet each of the folders with these [picks], in order, is: the one picked for it, or else the first that no
 * folder was given before it nor picked, Mercury first, until they have all been given. Then, and where plain was
 * picked, a folder is not a planet.
 */
fun planetsFor(picks: List<PlanetPick>): List<Planet?> {
    val free = (Planet.entries - picks.filterIsInstance<PlanetPick.Of>().map { it.planet }.toSet()).iterator()
    return picks.map { pick ->
        when (pick) {
            PlanetPick.Auto -> if (free.hasNext()) free.next() else null
            PlanetPick.Plain -> null
            is PlanetPick.Of -> pick.planet
        }
    }
}

/** The planet each folder round the [ring] from its top, then along the [dock], is, as [planetsFor] gives them. */
fun planetsOf(ring: List<RingItem>, dock: List<RingItem>): Map<HomePlace.Folder, Planet> {
    val folders = (ring + dock).filterIsInstance<RingItem.Folder>()
    return folders.zip(planetsFor(folders.map { it.pick })).mapNotNull { (folder, planet) -> planet?.let { folder.at to it } }.toMap()
}
