package com.sqftware.orbitlauncher.domain.minimap

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Turns OpenMapTiles vector tiles (https://openmaptiles.org/schema) into the [MapScene] a minimap draws. OpenMapTiles
 * keeps an OpenStreetMap tag's value as a feature's subclass, so the rules match on what the map's makers tagged.
 */
object OpenMapTiles {
    const val ZOOM = 14

    /** The layers the minimap draws from; the rest (names, house numbers, boundaries) are never decoded. */
    val LAYERS = setOf("water", "landcover", "landuse", "transportation", "building", "poi")

    /** How far around the centre the map gathers features: the disc's radius, plus room for what reaches into it. */
    const val REACH_M = MINIMAP_SIZE / 2 * METRES_PER_PIXEL + 40

    /** The tiles a minimap centred on [centre] needs. */
    fun tilesFor(centre: LatLon): List<TileId> = WebMercator.tilesAround(centre, REACH_M, ZOOM)

    fun scene(centre: LatLon, tiles: Map<TileId, VectorTile>): MapScene {
        val origin = WebMercator.toMercator(centre)
        val scale = WebMercator.groundScale(centre.lat)
        val areas = mutableListOf<Area>()
        val ways = mutableListOf<Way>()
        val buildings = mutableListOf<Building>()
        val pois = mutableListOf<Poi>()

        for ((id, tile) in tiles) {
            fun local(extent: Int, p: TilePoint) = WebMercator.fromTile(id, extent, p).let {
                Local((it.x - origin.x) * scale, (it.y - origin.y) * scale)
            }

            fun polygons(layer: VectorLayer, feature: VectorFeature) =
                feature.polygons().map { polygon -> polygon.map { ring -> ring.map { local(layer.extent, it) } } }.filter(::nearby)

            fun lines(layer: VectorLayer, feature: VectorFeature) =
                feature.parts.map { line -> line.map { local(layer.extent, it) } }.filter { nearby(listOf(it)) }

            tile["water"]?.let { layer ->
                layer.features.filter { it.type == GeometryType.Polygon && it.string("brunnel") != "tunnel" }.forEach {
                    areas += Area(Ground.Water, polygons(layer, it), order = 11)
                }
            }
            tile["landcover"]?.let { layer ->
                layer.features.filter { it.type == GeometryType.Polygon }.forEach { feature ->
                    val area = when (feature.string("class")) {
                        "grass", "farmland", "wetland" -> Area(Ground.Grass, polygons(layer, feature), order = 0)
                        "wood" -> Area(Ground.Grass, polygons(layer, feature), order = 0, woodland = true)
                        "sand" -> Area(Ground.Sand, polygons(layer, feature), order = 4)
                        else -> null
                    }
                    area?.let { areas += it }
                }
            }
            tile["landuse"]?.let { layer ->
                layer.features.filter { it.type == GeometryType.Polygon }.forEach { feature ->
                    val area = when (feature.string("class")) {
                        "cemetery" -> Area(Ground.Grass, polygons(layer, feature), order = 0)
                        "pitch", "playground" -> Area(Ground.Pitch, polygons(layer, feature), order = 3)
                        "commercial", "retail" -> Area(Ground.Plaza, polygons(layer, feature), order = -1, builtUpOnly = true)
                        else -> null
                    }
                    area?.let { areas += it }
                }
            }
            tile["transportation"]?.let { layer ->
                layer.features.filterNot(::belowGround).forEach { feature ->
                    if (feature.type == GeometryType.Polygon) {
                        val area = when {
                            feature.string("class") == "pier" -> Area(Ground.Pier, polygons(layer, feature), order = 10)
                            feature.string("subclass") == "pedestrian" -> Area(Ground.Plaza, polygons(layer, feature), order = 7)
                            else -> null
                        }
                        area?.let { areas += it }
                    } else if (feature.type == GeometryType.Line) {
                        wayOf(feature)?.let { (kind, width) -> lines(layer, feature).forEach { ways += Way(kind, width, it) } }
                    }
                }
            }
            tile["building"]?.let { layer ->
                // A part lifted off the ground, a skybridge or an overhang, has no footprint of its own.
                layer.features.filter { it.type == GeometryType.Polygon && (it.number("render_min_height") ?: 0.0) <= 0.0 }.forEach { feature ->
                    // The tiles merge every building alike into one feature, so each polygon is a building, known by its
                    // first corner's place on Earth.
                    feature.polygons().forEach { polygon ->
                        val ring = polygon.map { ring -> ring.map { local(layer.extent, it) } }
                        if (nearby(ring)) buildings += Building(ring, seedOf(WebMercator.fromTile(id, layer.extent, polygon[0][0])))
                    }
                }
            }
            tile["poi"]?.let { layer ->
                layer.features.filter { it.type == GeometryType.Point }.forEach { feature ->
                    val point = feature.parts.firstOrNull()?.firstOrNull() ?: return@forEach
                    // A point in the tile's buffer belongs to its neighbour, which brings it too.
                    if (point.x !in 0 until layer.extent || point.y !in 0 until layer.extent) return@forEach
                    val at = local(layer.extent, point)
                    if (abs(at.east) <= REACH_M && abs(at.north) <= REACH_M) poiOf(feature, at)?.let { pois += it }
                }
            }
        }
        return MapScene(centre, areas.filter { it.polygons.isNotEmpty() }, ways, buildings, pois)
    }

    private fun nearby(polygon: List<List<Local>>): Boolean {
        val ring = polygon.firstOrNull() ?: return false
        var minE = Double.MAX_VALUE
        var maxE = -Double.MAX_VALUE
        var minN = Double.MAX_VALUE
        var maxN = -Double.MAX_VALUE
        for (p in ring) {
            minE = min(minE, p.east)
            maxE = max(maxE, p.east)
            minN = min(minN, p.north)
            maxN = max(maxN, p.north)
        }
        return ring.size >= 2 && maxE >= -REACH_M && minE <= REACH_M && maxN >= -REACH_M && minN <= REACH_M
    }

    private fun belowGround(feature: VectorFeature) =
        feature.string("brunnel") == "tunnel" ||
            (feature.number("layer") ?: 0.0) < 0 ||
            (feature.number("level") ?: 0.0) < 0 ||
            feature.number("indoor") == 1.0

    private val ROAD_WIDTH_M = mapOf(
        "motorway" to 16.0, "trunk" to 15.0, "primary" to 14.0, "secondary" to 12.0, "tertiary" to 11.0,
        "minor" to 9.0, "busway" to 9.0, "service" to 5.5,
    )
    private const val RAMP_WIDTH_M = 8.0
    private val FOOT_WIDTH_M = mapOf(
        "pedestrian" to 6.0, "footway" to 3.0, "steps" to 3.0, "path" to 2.5, "cycleway" to 2.5, "bridleway" to 2.5,
    )
    private val RAILS = setOf("rail", "tram", "light_rail")

    private fun wayOf(feature: VectorFeature): Pair<WayKind, Double>? {
        val cls = feature.string("class")
        val subclass = feature.string("subclass")
        return when {
            cls == "service" && feature.string("service") == "parking_aisle" -> WayKind.Parking to ROAD_WIDTH_M.getValue("service")
            cls in ROAD_WIDTH_M -> WayKind.Road to if (feature.number("ramp") == 1.0 && cls != "service") RAMP_WIDTH_M else ROAD_WIDTH_M.getValue(cls!!)
            cls == "track" -> WayKind.Path to 3.0
            cls == "path" && subclass == "pedestrian" -> WayKind.Walk to FOOT_WIDTH_M.getValue("pedestrian")
            // OpenMapTiles leaves out sidewalks and crossings, the game's grey walks, so a footway left is a path.
            cls == "path" && subclass in FOOT_WIDTH_M -> WayKind.Path to FOOT_WIDTH_M.getValue(subclass!!)
            (cls == "rail" || cls == "transit") && subclass in RAILS -> WayKind.Rail to 0.0
            cls == "pier" -> WayKind.Pier to 4.0
            else -> null
        }
    }

    private class IconRule(val icon: MapIcon, val priority: Int, val matches: (cls: String, subclass: String) -> Boolean)

    private val SHOP_CLASSES = setOf(
        "shop", "grocery", "clothing_store", "alcohol_shop", "bakery", "butcher", "florist", "music", "bicycle", "laundry", "car",
    )

    private fun isShop(cls: String, subclass: String) = cls in SHOP_CLASSES || subclass == "books"

    private val ICON_RULES = listOf(
        IconRule(MapIcon.Bank, 0) { _, s -> s == "bank" },
        IconRule(MapIcon.Altar, 1) { c, _ -> c == "place_of_worship" },
        IconRule(MapIcon.QuestStart, 2) { _, s -> s in setOf("townhall", "library", "school", "college", "university") },
        IconRule(MapIcon.Transportation, 3) { c, s ->
            s in setOf("subway_entrance", "ferry_terminal", "bus_station", "fuel") || (c == "railway" && s in setOf("station", "subway", "halt"))
        },
        IconRule(MapIcon.Pub, 4) { _, s -> s in setOf("pub", "bar", "nightclub") },
        IconRule(MapIcon.CookeryShop, 5) { _, s -> s == "restaurant" },
        IconRule(MapIcon.KebabShop, 5) { _, s -> s == "fast_food" },
        IconRule(MapIcon.FoodShop, 5) { _, s -> s in setOf("cafe", "ice_cream", "bakery") },
        IconRule(MapIcon.Apothecary, 6) { _, s -> s in setOf("pharmacy", "chemist") },
        IconRule(MapIcon.WaterSource, 6) { _, s -> s == "fountain" },
        IconRule(MapIcon.GrandExchange, 6) { _, s -> s == "marketplace" },
        IconRule(MapIcon.ClothesShop, 7) { _, s -> s in setOf("clothes", "shoes", "fashion", "boutique") },
        IconRule(MapIcon.JewelleryShop, 7) { _, s -> s in setOf("jewelry", "jewellery") },
        IconRule(MapIcon.Hairdresser, 7) { _, s -> s in setOf("hairdresser", "beauty") },
        IconRule(MapIcon.PetShop, 7) { _, s -> s == "pet" },
        IconRule(MapIcon.GeneralStore, 8) { c, s -> isShop(c, s) },
    )

    // Many civic buildings carry only a name; these give them the kind a map icon needs.
    private val NAME_HINTS = listOf(
        "city hall" to "townhall", "town hall" to "townhall", "library" to "library", "church" to "place_of_worship",
        "credit union" to "bank",
    )

    // Stops and car parks are named after the streets they are on, so "Church St" says nothing of a church.
    private val UNHINTED_CLASSES = setOf("bus", "railway", "entrance", "parking")

    private val NPC_SUBCLASSES = setOf(
        "restaurant", "cafe", "fast_food", "bar", "pub", "bank", "pharmacy", "ice_cream", "food_court", "post_office", "library",
        "marketplace", "theatre", "cinema", "ferry_terminal", "townhall", "parking", "subway_entrance", "station", "tram_stop",
    )

    private fun poiOf(feature: VectorFeature, at: Local): Poi? {
        val cls = feature.string("class") ?: return null
        val subclass = feature.string("subclass") ?: cls
        val rule = ICON_RULES.firstOrNull { it.matches(cls, subclass) } ?: feature.string("name")?.takeIf { cls !in UNHINTED_CLASSES }?.lowercase()?.let { name ->
            NAME_HINTS.firstOrNull { (needle, _) -> needle in name }?.let { (_, hinted) ->
                ICON_RULES.firstOrNull { it.matches(if (hinted == "place_of_worship") hinted else cls, hinted) }
            }
        }
        // Someone to meet: staffed places, shops and stops, who also stand at a door when indoors.
        val npc = subclass in NPC_SUBCLASSES || isShop(cls, subclass)
        if (rule == null && !npc) return null
        return Poi(at, rule?.icon, rule?.priority ?: Int.MAX_VALUE, npc = npc, door = npc && subclass != "parking")
    }

    private fun seedOf(at: Mercator) = floor(at.x * 2).toLong() * 73_856_093L xor floor(at.y * 2).toLong() * 19_349_663L
}
