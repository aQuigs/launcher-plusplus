package com.sqftware.orbitlauncher.domain.minimap

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot

/**
 * Turns OpenMapTiles vector tiles (https://openmaptiles.org/schema) into the [MapScene] a minimap draws. OpenMapTiles
 * keeps an OpenStreetMap tag's value as a feature's subclass, so the rules match on what the map's makers tagged.
 */
object OpenMapTiles {
    const val ZOOM = 14

    /** The layers the minimap draws from, and names the place it is in from; the rest (house numbers, boundaries) are never decoded. */
    val LAYERS = setOf("water", "landcover", "landuse", "transportation", "building", "poi", "place")

    /** The tags the rules read. */
    val KEYS = setOf("class", "subclass", "brunnel", "service", "layer", "level", "indoor", "ramp", "render_min_height", "name")

    /** The tiles a map in [frame] centred on [centre] needs. */
    fun tilesFor(centre: LatLon, frame: MapFrame): List<TileId> = WebMercator.tilesAround(centre, frame.reachM, ZOOM)

    fun decode(bytes: ByteArray) = VectorTile.decode(bytes, LAYERS, KEYS)

    fun scene(centre: LatLon, tiles: Map<TileId, VectorTile>, frame: MapFrame): MapScene {
        val reach = frame.reachM
        val origin = WebMercator.toMercator(centre)
        val scale = frame.groundScale(centre.lat)
        fun placed(id: TileId, layer: VectorLayer) = PlacedLayer(id, layer.extent, origin, scale, reach)
        val areas = mutableListOf<Area>()
        val ways = mutableListOf<Way>()
        val buildings = mutableListOf<Building>()
        val pois = mutableListOf<Poi>()
        var place: String? = null
        var placeDistance = Double.MAX_VALUE

        // In a fixed order, so what is placed first under a spacing rule never depends on which tile came in first.
        for ((id, tile) in tiles.entries.sortedWith(compareBy({ it.key.y }, { it.key.x }))) {
            for (name in AREA_LAYERS) {
                val layer = tile[name] ?: continue
                val placed = placed(id, layer)
                for (feature in layer.features.filter { it.type == GeometryType.Polygon }) {
                    val rule = areaRule(name, feature) ?: continue
                    val polygons = placed.polygons(feature)
                    if (polygons.isNotEmpty()) areas += Area(rule.ground, polygons, rule.order, rule.woodland, rule.builtUpOnly)
                }
            }
            tile["transportation"]?.let { layer ->
                val placed = placed(id, layer)
                for (feature in layer.features.filter { it.type == GeometryType.Line && !belowGround(it) }) {
                    val (kind, width) = wayOf(feature) ?: continue
                    feature.parts.filter(placed::near).forEach { line -> ways += Way(kind, width, line.map(placed::local)) }
                }
            }
            tile["building"]?.let { layer ->
                val placed = placed(id, layer)
                // A part lifted off the ground, a skybridge or an overhang, has no footprint of its own.
                for (feature in layer.features.filter { it.type == GeometryType.Polygon && (it.number("render_min_height") ?: 0.0) <= 0.0 }) {
                    // The tiles merge every building alike into one feature, so each polygon is a building, known by its
                    // first corner's place on Earth.
                    for (polygon in feature.polygons().filter { placed.near(it[0]) }) {
                        buildings += Building(placed.local(polygon), seedOf(placed.mercator(polygon[0][0])))
                    }
                }
            }
            tile["poi"]?.let { layer ->
                val placed = placed(id, layer)
                for (feature in layer.features.filter { it.type == GeometryType.Point }) {
                    val point = feature.parts.firstOrNull()?.firstOrNull() ?: continue
                    // A point in the tile's buffer belongs to its neighbour, which brings it too.
                    if (point.x !in 0 until layer.extent || point.y !in 0 until layer.extent) continue
                    val at = placed.local(point)
                    if (abs(at.east) <= reach && abs(at.north) <= reach) poiOf(feature, at)?.let { pois += it }
                }
            }
            // A place is mapped as a point at its middle, and the tiles carry those well beyond their edges, so the
            // nearest names the neighborhood or village the map is in.
            tile["place"]?.let { layer ->
                val placed = placed(id, layer)
                for (feature in layer.features.filter { it.type == GeometryType.Point && it.string("class") in SETTLEMENTS }) {
                    val name = feature.string("name") ?: continue
                    val at = placed.local(feature.parts.firstOrNull()?.firstOrNull() ?: continue)
                    val distance = hypot(at.east, at.north)
                    if (distance < placeDistance) {
                        place = name
                        placeDistance = distance
                    }
                }
            }
        }
        return MapScene(centre, areas, ways, buildings, pois, place = place)
    }

    /**
     * One layer of one tile placed around the map's centre at [origin], [scale] ground metres to the mercator metre, for a
     * map that gathers features [reachM] metres round it.
     */
    private class PlacedLayer(private val tile: TileId, private val extent: Int, private val origin: Mercator, private val scale: Double, reachM: Double) {
        private val box = WebMercator.bounds(tile)
        private val unit = (box.maxX - box.minX) / extent
        private val centreX = (origin.x - box.minX) / unit
        private val centreY = (box.maxY - origin.y) / unit
        private val reach = reachM / scale / unit

        fun mercator(p: TilePoint) = WebMercator.fromTile(tile, extent, p)

        fun local(p: TilePoint) = mercator(p).let { Local((it.x - origin.x) * scale, (it.y - origin.y) * scale) }

        fun local(polygon: List<List<TilePoint>>): Polygon = polygon.map { ring -> ring.map(::local) }

        /** Whether [points] come within the map's reach, judged in tile units before anything is converted. */
        fun near(points: List<TilePoint>) = points.size >= 2 &&
            points.maxOf { it.x } >= centreX - reach && points.minOf { it.x } <= centreX + reach &&
            points.maxOf { it.y } >= centreY - reach && points.minOf { it.y } <= centreY + reach

        fun polygons(feature: VectorFeature) = feature.polygons().filter { near(it[0]) }.map(::local)
    }

    private class AreaRule(val ground: Ground, val order: Int, val woodland: Boolean = false, val builtUpOnly: Boolean = false)

    private val AREA_LAYERS = listOf("water", "landcover", "landuse", "transportation")

    /** The places someone is in, from a city down to a neighborhood; a country or an island is too big to say where. */
    private val SETTLEMENTS = setOf("city", "town", "village", "hamlet", "suburb", "quarter", "neighbourhood", "isolated_dwelling")
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

    private val QUEST_STARTS = setOf("townhall", "library", "school", "college", "university")
    private val STOPS = setOf("subway_entrance", "ferry_terminal", "bus_station", "fuel")
    private val RAILWAY_STATIONS = setOf("station", "subway", "halt")
    private val PUBS = setOf("pub", "bar", "nightclub")
    private val FOOD_SHOPS = setOf("cafe", "ice_cream", "bakery")
    private val APOTHECARIES = setOf("pharmacy", "chemist")
    private val CLOTHES_SHOPS = setOf("clothes", "shoes", "fashion", "boutique")
    private val JEWELLERS = setOf("jewelry", "jewellery")
    private val HAIRDRESSERS = setOf("hairdresser", "beauty")

    private val ICON_RULES: List<Pair<MapIcon, (cls: String, subclass: String) -> Boolean>> = listOf(
        MapIcon.Bank to { _, s -> s == "bank" },
        MapIcon.Altar to { c, _ -> c == "place_of_worship" },
        MapIcon.QuestStart to { _, s -> s in QUEST_STARTS },
        MapIcon.Transportation to { c, s -> s in STOPS || (c == "railway" && s in RAILWAY_STATIONS) },
        MapIcon.Pub to { _, s -> s in PUBS },
        MapIcon.CookeryShop to { _, s -> s == "restaurant" },
        MapIcon.KebabShop to { _, s -> s == "fast_food" },
        MapIcon.FoodShop to { _, s -> s in FOOD_SHOPS },
        MapIcon.Apothecary to { _, s -> s in APOTHECARIES },
        MapIcon.WaterSource to { _, s -> s == "fountain" },
        MapIcon.GrandExchange to { _, s -> s == "marketplace" },
        MapIcon.ClothesShop to { _, s -> s in CLOTHES_SHOPS },
        MapIcon.JewelleryShop to { _, s -> s in JEWELLERS },
        MapIcon.Hairdresser to { _, s -> s in HAIRDRESSERS },
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
