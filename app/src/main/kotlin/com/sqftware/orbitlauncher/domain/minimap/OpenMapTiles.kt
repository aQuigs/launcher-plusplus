package com.sqftware.orbitlauncher.domain.minimap

import kotlin.math.abs
import kotlin.math.floor

/**
 * Turns OpenMapTiles vector tiles (https://openmaptiles.org/schema) into the [MapScene] a minimap draws. OpenMapTiles
 * keeps an OpenStreetMap tag's value as a feature's subclass, so the rules match on what the map's makers tagged.
 */
object OpenMapTiles {
    const val ZOOM = 14

    /** The layers the minimap draws from; the rest (names, house numbers, boundaries) are never decoded. */
    val LAYERS = setOf("water", "landcover", "landuse", "transportation", "building", "poi")

    /** The tags the rules read. */
    val KEYS = setOf("class", "subclass", "brunnel", "service", "layer", "level", "indoor", "ramp", "render_min_height", "name")

    /** How far around the centre the map gathers features: the disc's radius, plus room for what reaches into it. */
    const val REACH_M = MINIMAP_SIZE / 2 * METRES_PER_PIXEL + 40

    /** The tiles a minimap centred on [centre] needs. */
    fun tilesFor(centre: LatLon): List<TileId> = WebMercator.tilesAround(centre, REACH_M, ZOOM)

    fun decode(bytes: ByteArray) = VectorTile.decode(bytes, LAYERS, KEYS)

    fun scene(centre: LatLon, tiles: Map<TileId, VectorTile>): MapScene {
        val origin = WebMercator.toMercator(centre)
        val scale = WebMercator.groundScale(centre.lat)
        val areas = mutableListOf<Area>()
        val ways = mutableListOf<Way>()
        val buildings = mutableListOf<Building>()
        val pois = mutableListOf<Poi>()

        // In a fixed order, so what is placed first under a spacing rule never depends on which tile came in first.
        for ((id, tile) in tiles.entries.sortedWith(compareBy({ it.key.y }, { it.key.x }))) {
            for (name in AREA_LAYERS) {
                val layer = tile[name] ?: continue
                val frame = Frame(id, layer.extent, origin, scale)
                for (feature in layer.features.filter { it.type == GeometryType.Polygon }) {
                    val rule = areaRule(name, feature) ?: continue
                    val polygons = frame.polygons(feature)
                    if (polygons.isNotEmpty()) areas += Area(rule.ground, polygons, rule.order, rule.woodland, rule.builtUpOnly)
                }
            }
            tile["transportation"]?.let { layer ->
                val frame = Frame(id, layer.extent, origin, scale)
                for (feature in layer.features.filter { it.type == GeometryType.Line && !belowGround(it) }) {
                    val (kind, width) = wayOf(feature) ?: continue
                    feature.parts.filter(frame::near).forEach { line -> ways += Way(kind, width, line.map(frame::local)) }
                }
            }
            tile["building"]?.let { layer ->
                val frame = Frame(id, layer.extent, origin, scale)
                // A part lifted off the ground, a skybridge or an overhang, has no footprint of its own.
                for (feature in layer.features.filter { it.type == GeometryType.Polygon && (it.number("render_min_height") ?: 0.0) <= 0.0 }) {
                    // The tiles merge every building alike into one feature, so each polygon is a building, known by its
                    // first corner's place on Earth.
                    for (polygon in feature.polygons().filter { frame.near(it[0]) }) {
                        buildings += Building(frame.local(polygon), seedOf(frame.mercator(polygon[0][0])))
                    }
                }
            }
            tile["poi"]?.let { layer ->
                val frame = Frame(id, layer.extent, origin, scale)
                for (feature in layer.features.filter { it.type == GeometryType.Point }) {
                    val point = feature.parts.firstOrNull()?.firstOrNull() ?: continue
                    // A point in the tile's buffer belongs to its neighbour, which brings it too.
                    if (point.x !in 0 until layer.extent || point.y !in 0 until layer.extent) continue
                    val at = frame.local(point)
                    if (abs(at.east) <= REACH_M && abs(at.north) <= REACH_M) poiOf(feature, at)?.let { pois += it }
                }
            }
        }
        return MapScene(centre, areas, ways, buildings, pois)
    }

    /** One layer of one tile placed around the map's centre at [origin], [scale] ground metres to the mercator metre. */
    private class Frame(tile: TileId, extent: Int, private val origin: Mercator, private val scale: Double) {
        private val box = WebMercator.bounds(tile)
        private val unit = (box.maxX - box.minX) / extent
        private val centreX = (origin.x - box.minX) / unit
        private val centreY = (box.maxY - origin.y) / unit
        private val reach = REACH_M / scale / unit

        fun mercator(p: TilePoint) = Mercator(box.minX + p.x * unit, box.maxY - p.y * unit)

        fun local(p: TilePoint) = Local((box.minX + p.x * unit - origin.x) * scale, (box.maxY - p.y * unit - origin.y) * scale)

        fun local(polygon: List<List<TilePoint>>): Polygon = polygon.map { ring -> ring.map(::local) }

        /** Whether [points] come within the map's reach, judged in tile units before anything is converted. */
        fun near(points: List<TilePoint>) = points.size >= 2 &&
            points.maxOf { it.x } >= centreX - reach && points.minOf { it.x } <= centreX + reach &&
            points.maxOf { it.y } >= centreY - reach && points.minOf { it.y } <= centreY + reach

        fun polygons(feature: VectorFeature) = feature.polygons().filter { near(it[0]) }.map(::local)
    }

    private class AreaRule(val ground: Ground, val order: Int, val woodland: Boolean = false, val builtUpOnly: Boolean = false)

    private val AREA_LAYERS = listOf("water", "landcover", "landuse", "transportation")
    private val GRASS = AreaRule(Ground.Grass, 0)
    private val LANDCOVER = mapOf(
        "grass" to GRASS, "farmland" to GRASS, "wetland" to GRASS,
        "wood" to AreaRule(Ground.Grass, 0, woodland = true),
        "sand" to AreaRule(Ground.Sand, 4),
    )
    private val PITCH = AreaRule(Ground.Pitch, 3)
    private val FORECOURT = AreaRule(Ground.Plaza, -1, builtUpOnly = true)
    private val LANDUSE = mapOf("cemetery" to GRASS, "pitch" to PITCH, "playground" to PITCH, "commercial" to FORECOURT, "retail" to FORECOURT)

    private fun areaRule(layer: String, feature: VectorFeature): AreaRule? = when (layer) {
        "water" -> AreaRule(Ground.Water, 11).takeIf { feature.string("brunnel") != "tunnel" }
        "landcover" -> LANDCOVER[feature.string("class")]
        "landuse" -> LANDUSE[feature.string("class")]
        else -> when {
            belowGround(feature) -> null
            feature.string("class") == "pier" -> AreaRule(Ground.Pier, 10)
            feature.string("subclass") == "pedestrian" -> AreaRule(Ground.Plaza, 7)
            else -> null
        }
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

    private val SHOP_CLASSES = setOf(
        "shop", "grocery", "clothing_store", "alcohol_shop", "bakery", "butcher", "florist", "music", "bicycle", "laundry", "car",
    )

    private fun isShop(cls: String, subclass: String) = cls in SHOP_CLASSES || subclass == "books"

    private val ICON_RULES: List<Pair<MapIcon, (cls: String, subclass: String) -> Boolean>> = listOf(
        MapIcon.Bank to { _, s -> s == "bank" },
        MapIcon.Altar to { c, _ -> c == "place_of_worship" },
        MapIcon.QuestStart to { _, s -> s in setOf("townhall", "library", "school", "college", "university") },
        MapIcon.Transportation to { c, s ->
            s in setOf("subway_entrance", "ferry_terminal", "bus_station", "fuel") || (c == "railway" && s in setOf("station", "subway", "halt"))
        },
        MapIcon.Pub to { _, s -> s in setOf("pub", "bar", "nightclub") },
        MapIcon.CookeryShop to { _, s -> s == "restaurant" },
        MapIcon.KebabShop to { _, s -> s == "fast_food" },
        MapIcon.FoodShop to { _, s -> s in setOf("cafe", "ice_cream", "bakery") },
        MapIcon.Apothecary to { _, s -> s in setOf("pharmacy", "chemist") },
        MapIcon.WaterSource to { _, s -> s == "fountain" },
        MapIcon.GrandExchange to { _, s -> s == "marketplace" },
        MapIcon.ClothesShop to { _, s -> s in setOf("clothes", "shoes", "fashion", "boutique") },
        MapIcon.JewelleryShop to { _, s -> s in setOf("jewelry", "jewellery") },
        MapIcon.Hairdresser to { _, s -> s in setOf("hairdresser", "beauty") },
        MapIcon.PetShop to { _, s -> s == "pet" },
        MapIcon.GeneralStore to ::isShop,
    )

    // Many civic buildings carry only a name, which gives them their icon.
    private val NAME_HINTS = listOf(
        "city hall" to MapIcon.QuestStart, "town hall" to MapIcon.QuestStart, "library" to MapIcon.QuestStart,
        "church" to MapIcon.Altar, "credit union" to MapIcon.Bank,
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
        val icon = ICON_RULES.firstOrNull { (_, matches) -> matches(cls, subclass) }?.first
            ?: feature.string("name")?.takeIf { cls !in UNHINTED_CLASSES }?.lowercase()?.let { name ->
                NAME_HINTS.firstOrNull { (needle, _) -> needle in name }?.second
            }
        // Someone to meet: staffed places, shops and stops, who also stand at a door when indoors.
        val npc = subclass in NPC_SUBCLASSES || isShop(cls, subclass)
        if (icon == null && !npc) return null
        return Poi(at, icon, npc = npc, door = npc && subclass != "parking")
    }

    private fun seedOf(at: Mercator) = floor(at.x * 2).toLong() * 73_856_093L xor floor(at.y * 2).toLong() * 19_349_663L
}
