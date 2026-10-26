package com.sqftware.orbitlauncher.domain.minimap

/** A point in ground metres east and north of the map's centre. */
data class Local(val east: Double, val north: Double)

/** A polygon: its exterior ring, then its holes, each ring open. */
typealias Polygon = List<List<Local>>

/** What a 4 px tile of the minimap is made of, which decides its colour. */
enum class Ground { Grass, Road, Walk, Path, Plaza, Parking, Sand, Water, Pier, FloorStone, FloorWood, Pitch }

/**
 * Ground of one kind over [polygons]. Bigger areas are drawn first, so what lies on top of them (a fountain in a park)
 * survives; between areas of one size, a higher [order] draws later. A [woodland] gets trees scattered over its grass.
 * An area [builtUpOnly] counts only where buildings stand close together, as shops' forecourts do in a town centre.
 */
data class Area(
    val ground: Ground,
    val polygons: List<Polygon>,
    val order: Int,
    val woodland: Boolean = false,
    val builtUpOnly: Boolean = false,
)

enum class WayKind { Road, Walk, Path, Parking, Pier, Rail }

/** A line on the ground: [points] [widthM] metres wide, or a 1 px line for a [WayKind.Rail]. */
data class Way(val kind: WayKind, val widthM: Double, val points: List<Local>)

/** One building's footprint. Touching footprints merge into one walled structure. [seed] keeps its look stable as the map moves. */
data class Building(val polygon: Polygon, val seed: Long)

/**
 * Something mapped at a spot: its map [icon], whether someone stands there ([npc]), and whether it is entered from the
 * street, so the wall of the building it is in gets a [door].
 */
data class Poi(val at: Local, val icon: MapIcon? = null, val npc: Boolean = false, val door: Boolean = false)

/** The game's dot for someone on the minimap: yellow for an NPC, white for another player. */
enum class Dot { Npc, Player }

/** Someone moving about the map at a spot, drawn as their [dot]. */
data class Figure(val at: Local, val dot: Dot)

/** Everything a minimap centred on [centre] draws, in ground metres around it, and the name of the [place] it is in, if known. */
data class MapScene(
    val centre: LatLon,
    val areas: List<Area> = emptyList(),
    val ways: List<Way> = emptyList(),
    val buildings: List<Building> = emptyList(),
    val pois: List<Poi> = emptyList(),
    val figures: List<Figure> = emptyList(),
    val place: String? = null,
)
