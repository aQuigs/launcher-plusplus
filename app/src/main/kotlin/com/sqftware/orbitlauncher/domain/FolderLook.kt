package com.sqftware.orbitlauncher.domain

/** How the folders on the home screen are drawn, all of them planets of a kind: a launcher-wide setting. */
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
}

/** The planets of the [FolderLook.SolarSystem] look, in order from the Sun. Pluto counts. */
enum class Planet { Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, Neptune, Pluto }

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
