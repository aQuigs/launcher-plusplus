package com.sqftware.orbitlauncher.domain.minimap

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The player's loop: along the high street, up the east lane, through the top of the park and down the west lane. */
private val VILLAGE_ROUTE = line(0 to 0, 90 to 0, 90 to 150, 40 to 175, -70 to 150, -70 to 0)

/**
 * A village: a high street with its square and fountain, a park round a pond, a church, a river with a jetty, and a
 * railway with its station. The player walks up through the park and back down the lanes.
 */
private val VILLAGE = MadeUpPlace(
    ground = MapScene(
        centre = LatLon(0.0, 0.0),
        areas = listOf(
            // Woods in sight from every step, so the painter never scatters the trees it gives a view without any.
            Area(
                Ground.Grass,
                listOf(rect(-300, 30, -85, 320), rect(96, 34, 132, 320), rect(-50, 112, 75, 140), rect(-300, -98, 135, -70)),
                order = 0,
                woodland = true,
            ),
            Area(
                Ground.Water,
                listOf(polygon(140 to -400, 162 to -400, 168 to -120, 158 to 90, 170 to 400, 146 to 400, 136 to 90, 146 to -120)),
                order = 1,
            ),
            Area(Ground.Sand, listOf(polygon(126 to 40, 140 to 30, 141 to 110, 128 to 100)), order = 0),
            Area(Ground.Water, listOf(oval(10, 78, 28, 15)), order = 1),
            Area(Ground.Plaza, listOf(rect(-20, 5, 30, 42)), order = 0),
            Area(Ground.Water, listOf(oval(5, 24, 4, 4)), order = 1),
            Area(Ground.Plaza, listOf(rect(30, -106, 90, -100)), order = 0),
            Area(Ground.Pitch, listOf(rect(-170, -210, -70, -135)), order = 0),
        ),
        ways = listOf(
            Way(WayKind.Road, 9.0, line(-320 to 0, 320 to 0)),
            Way(WayKind.Road, 7.0, line(90 to 0, 90 to 150)),
            Way(WayKind.Road, 7.0, line(-70 to 0, -70 to 150, -75 to 330)),
            Way(WayKind.Road, 6.0, line(20 to 0, 20 to -95, 25 to -320)),
            Way(WayKind.Path, 3.0, line(90 to 150, 40 to 175, -70 to 150)),
            Way(WayKind.Walk, 3.0, line(-20 to 42, -30 to 70, -12 to 100, 30 to 100, 48 to 78, 30 to 42)),
            Way(WayKind.Path, 2.0, line(40 to 175, 60 to 330)),
            Way(WayKind.Pier, 4.0, line(136 to 60, 152 to 60)),
            Way(WayKind.Rail, 1.0, line(-320 to -112, 320 to -112)),
        ),
        buildings = listOf(
            rect(-62, 6, -42, 24), rect(-40, 6, -24, 28), rect(34, 6, 52, 26), rect(55, 6, 84, 22),
            rect(-130, 6, -105, 26), rect(-100, 6, -76, 22), rect(96, 6, 122, 30),
            rect(-130, -28, -100, -6), rect(-96, -24, -76, -6), rect(-66, -30, -36, -6), rect(-32, -22, -10, -6),
            rect(-6, -26, 15, -6), rect(26, -28, 50, -6), rect(54, -22, 80, -6), rect(96, -30, 120, -6),
            rect(-100, -64, -80, -44), rect(-60, -62, -40, -44), rect(-20, -66, 5, -44), rect(32, -60, 52, -42),
            rect(60, -66, 85, -46),
            rect(-118, 50, -90, 92), rect(-90, 64, -80, 78),
            rect(98, 40, 118, 58), rect(98, 70, 120, 90),
            rect(-30, 182, -10, 198), rect(62, 186, 82, 202),
            rect(40, -96, 80, -86),
        ).mapIndexed { i, footprint -> Building(footprint, seed = i.toLong()) },
        // The painter keeps at most four icons, the nearest, and none too close together or to the player, so these lie
        // far enough apart that no step of the walk changes which show.
        pois = listOf(
            Poi(at(-51, -18), MapIcon.Bank), Poi(at(-51, -7), door = true),
            Poi(at(38, -17), MapIcon.Pub), Poi(at(38, -7), door = true),
            Poi(at(69, 14), MapIcon.GeneralStore), Poi(at(69, 7), door = true),
            Poi(at(43, 7), door = true), Poi(at(-21, -7), door = true),
            Poi(at(-104, 71), MapIcon.Altar), Poi(at(-91, 60), door = true),
            Poi(at(60, -91), MapIcon.Transportation), Poi(at(60, -95), door = true),
            Poi(at(-30, 70), MapIcon.QuestStart),
            Poi(at(108, 80), MapIcon.ClothesShop), Poi(at(99, 80), door = true),
        ),
    ),
    route = Route(VILLAGE_ROUTE),
    walkers = listOf(
        // Townsfolk pace about their spots.
        Walker(Dot.Npc, Route(line(-5 to 14, 16 to 14, 16 to 34, -5 to 34)), pace = 0.25),
        Walker(Dot.Npc, Route(line(-12 to 38, 22 to 12)), pace = 0.2, start = 10.0),
        Walker(Dot.Npc, Route(line(30 to -3, 46 to -3)), pace = 0.15),
        Walker(Dot.Npc, Route(line(-60 to -3, -42 to -3)), pace = 0.12, start = 6.0),
        Walker(Dot.Npc, Route(line(-20 to 42, -30 to 70, -12 to 100, 30 to 100, 48 to 78, 30 to 42)), pace = 0.3, start = 40.0),
        Walker(Dot.Npc, Route(line(138 to 60, 151 to 60)), pace = 0.08),
        Walker(Dot.Npc, Route(line(-88 to 50, -76 to 38)), pace = 0.1),
        Walker(Dot.Npc, Route(line(40 to -103, 85 to -103)), pace = 0.2, start = 20.0),
        Walker(Dot.Npc, Route(line(91 to 95, 91 to 125)), pace = 0.15),
        Walker(Dot.Npc, Route(line(-160 to -200, -80 to -145)), pace = 0.45),
        Walker(Dot.Npc, Route(line(-80 to -200, -160 to -145)), pace = 0.4, start = 30.0),
        // Other players pass through on errands of their own, and gather at the bank as they do in the game.
        Walker(Dot.Player, Route(line(-260 to 2, 260 to 2)), pace = 0.9, start = 240.0),
        Walker(Dot.Player, Route(VILLAGE_ROUTE.reversed()), pace = 1.1, start = 300.0),
        Walker(Dot.Player, Route(line(20 to -300, 20 to -2, 88 to -2, 88 to 150, 60 to 330, 88 to 150, 88 to -2, 20 to -2)), pace = 0.8, start = 150.0),
        Walker(Dot.Player, Route(line(-75 to 330, -72 to 2, -250 to 2, -72 to 2)), pace = 0.7, start = 90.0),
        Walker(Dot.Player, Route(line(-54 to -2, -48 to -2)), pace = 0.05),
        Walker(Dot.Player, Route(line(-49 to -3, -45 to -1)), pace = 0.04, start = 3.0),
    ),
)

/** Where the harbour's beach meets the sea, south to north. */
private val COAST = listOf(50 to -400, 58 to -200, 52 to 0, 60 to 200, 55 to 400)

/**
 * A fishing harbour on an eastern sea: a coast road with cottages backing onto a wood, a fish market and an inn, huts on
 * the beach, and piers with boats moored and fishers at their ends. The player walks up the road and back along the sand.
 */
private val HARBOUR = MadeUpPlace(
    ground = MapScene(
        centre = LatLon(0.0, 0.0),
        areas = listOf(
            // The wood behind the cottages stays in sight from all of the walk, as the village's woods do.
            Area(Ground.Grass, listOf(rect(-400, -400, -20, 400)), order = 0, woodland = true),
            Area(Ground.Water, listOf(polygon(*(COAST + listOf(400 to 400, 400 to -400)).toTypedArray())), order = 1),
            Area(Ground.Sand, listOf(polygon(*(listOf(28 to -400) + COAST + listOf(28 to 400)).toTypedArray())), order = 0),
            Area(Ground.Sand, listOf(oval(115, 150, 16, 12)), order = 0),
        ),
        ways = listOf(
            Way(WayKind.Road, 7.0, line(0 to -420, 0 to 420)),
            Way(WayKind.Path, 3.0, line(0 to -60, 30 to -60)),
            Way(WayKind.Path, 3.0, line(0 to 70, 30 to 70)),
            Way(WayKind.Pier, 5.0, line(40 to -50, 112 to -50)),
            Way(WayKind.Pier, 5.0, line(112 to -62, 112 to -38)),
            Way(WayKind.Pier, 5.0, line(40 to 50, 104 to 50)),
            Way(WayKind.Pier, 3.0, line(48 to 0, 82 to 0)),
        ),
        buildings = listOf(
            rect(-18, -100, -6, -86), rect(-18, -40, -6, -26), rect(-19, 20, -6, 34), rect(-18, 80, -6, 94), rect(-18, 140, -6, 152),
            rect(8, -14, 26, 14), rect(8, 104, 26, 122), rect(8, -80, 24, -66),
            rect(29, -30, 38, -18), rect(29, 18, 38, 30), rect(29, -112, 38, -98), rect(29, 95, 38, 108),
            rect(62, -61, 74, -56), rect(82, -44, 96, -39), rect(70, 55, 82, 60), rect(64, -6, 74, -2),
            rect(110, 145, 120, 155),
        ).mapIndexed { i, footprint -> Building(footprint, seed = 100L + i) },
        pois = listOf(
            Poi(at(17, 0), MapIcon.FoodShop), Poi(at(7, 0), door = true),
            Poi(at(17, 113), MapIcon.Pub), Poi(at(7, 113), door = true),
            Poi(at(16, -73), MapIcon.GeneralStore), Poi(at(7, -73), door = true),
            Poi(at(104, -50), MapIcon.Transportation),
            Poi(at(-12, 26), door = true), Poi(at(-12, 86), door = true), Poi(at(39, -24), door = true), Poi(at(39, 24), door = true),
        ),
    ),
    route = Route(line(0 to -130, 0 to 130, 44 to 140, 44 to -140)),
    walkers = listOf(
        // Fishers at the ends of the piers, traders at the market and the inn, and someone on the beach.
        Walker(Dot.Npc, Route(line(108 to -60, 108 to -42)), pace = 0.05),
        Walker(Dot.Npc, Route(line(100 to 52, 100 to 47)), pace = 0.03, start = 2.0),
        Walker(Dot.Npc, Route(line(78 to 2, 80 to -2)), pace = 0.03),
        Walker(Dot.Npc, Route(line(5 to -18, 5 to 18)), pace = 0.15),
        Walker(Dot.Npc, Route(line(5 to 100, 5 to 126)), pace = 0.12, start = 9.0),
        Walker(Dot.Npc, Route(line(36 to -45, 36 to 45)), pace = 0.2, start = 30.0),
        Walker(Dot.Player, Route(line(3 to -400, 3 to 400)), pace = 0.9, start = 380.0),
        Walker(Dot.Player, Route(line(48 to -300, 48 to 300)), pace = 0.7, start = 100.0),
        Walker(Dot.Player, Route(line(46 to -50, 100 to -50)), pace = 0.5, start = 40.0),
        Walker(Dot.Player, Route(line(46 to 50, 98 to 50)), pace = 0.4),
    ),
)

/** The forest path, from the woodcutters' camp over the river and back by the lake. */
private val FOREST_ROUTE = line(
    0 to -30, 40 to -90, 70 to -110, 115 to -110, 135 to -60, 125 to 10, 110 to 40, 65 to 40, 50 to 90, -20 to 130,
    -60 to 125, -120 to 60, -95 to -15, -40 to -50,
)

/**
 * A forest: a woodcutters' camp in a clearing, a winding path over a river by two bridges, a lake with a shrine, and a
 * ruined tower. The player follows the path round.
 */
private val FOREST = MadeUpPlace(
    ground = MapScene(
        centre = LatLon(0.0, 0.0),
        areas = listOf(
            // Woods everywhere but the clearings, which are holes in them, since grass inside a wood grows its trees.
            Area(
                Ground.Grass,
                listOf(rect(-500, -500, 500, 500) + oval(0, 0, 45, 35) + oval(-80, 120, 20, 12) + oval(126, -74, 16, 16)),
                order = 0,
                woodland = true,
            ),
            Area(
                Ground.Water,
                listOf(polygon(80 to -400, 95 to -400, 100 to -200, 92 to 0, 102 to 200, 96 to 400, 81 to 400, 87 to 200, 77 to 0, 85 to -200)),
                order = 1,
            ),
            Area(Ground.Water, listOf(oval(-100, 80, 30, 18)), order = 1),
        ),
        ways = listOf(
            Way(WayKind.Path, 4.0, FOREST_ROUTE + FOREST_ROUTE.first()),
            Way(WayKind.Path, 2.0, line(-60 to 125, -80 to 115)),
            Way(WayKind.Path, 2.0, line(125 to 10, 200 to 60, 300 to 80)),
            Way(WayKind.Path, 2.0, line(-40 to -50, -60 to -160, -40 to -300)),
            Way(WayKind.Pier, 6.0, line(68 to -110, 117 to -110)),
            Way(WayKind.Pier, 6.0, line(64 to 40, 108 to 40)),
        ),
        buildings = listOf(
            rect(-22, -16, -6, -4), rect(8, -22, 22, -10), rect(-14, 10, 4, 22), rect(18, 6, 30, 16),
            rect(-84, 116, -76, 124), rect(121, -79, 131, -69),
        ).mapIndexed { i, footprint -> Building(footprint, seed = 200L + i) },
        pois = listOf(
            Poi(at(-14, -10), MapIcon.GeneralStore), Poi(at(-14, -3), door = true),
            Poi(at(10, 28), MapIcon.QuestStart),
            Poi(at(-80, 120), MapIcon.Altar), Poi(at(-80, 115), door = true),
            Poi(at(15, -9), door = true), Poi(at(-5, 9), door = true),
        ),
    ),
    route = Route(FOREST_ROUTE),
    walkers = listOf(
        // Woodcutters about the camp, a monk at the shrine and a guard at the tower.
        Walker(Dot.Npc, Route(line(-30 to 0, -20 to 25)), pace = 0.1),
        Walker(Dot.Npc, Route(line(30 to -5, 38 to 20)), pace = 0.12, start = 5.0),
        Walker(Dot.Npc, Route(line(-5 to -25, 15 to -28)), pace = 0.08),
        Walker(Dot.Npc, Route(line(-75 to 108, -90 to 110)), pace = 0.05),
        Walker(Dot.Npc, Route(line(120 to -85, 135 to -80)), pace = 0.06),
        Walker(Dot.Player, Route(FOREST_ROUTE.reversed()), pace = 0.9, start = 200.0),
        Walker(Dot.Player, Route(FOREST_ROUTE), pace = 0.6, start = 450.0),
        Walker(Dot.Player, Route(line(125 to 10, 200 to 60, 300 to 80)), pace = 0.5),
        Walker(Dot.Player, Route(line(-40 to -50, -60 to -160, -40 to -300)), pace = 0.4, start = 60.0),
    ),
)

/** The desert town's loop: along the main street, up past the palace and round by the oasis. */
private val DESERT_ROUTE = line(-60 to -8, 130 to -8, 130 to 130, 10 to 130, -60 to 110, -130 to 60, -130 to -8)

/**
 * A desert town: sand all round, a market square, a walled palace round its courtyard, flat-roofed houses along the
 * streets, a gate east, and an oasis. No grass grows here, so neither do trees.
 */
private val DESERT = MadeUpPlace(
    ground = MapScene(
        centre = LatLon(0.0, 0.0),
        areas = listOf(
            Area(Ground.Sand, listOf(rect(-500, -500, 500, 500)), order = 0),
            Area(Ground.Plaza, listOf(rect(-28, -6, 28, 40)), order = 0),
            Area(Ground.Water, listOf(oval(-160, 60, 18, 12)), order = 1),
        ),
        ways = listOf(
            Way(WayKind.Path, 7.0, line(-400 to -8, 400 to -8)),
            Way(WayKind.Path, 6.0, line(130 to -400, 130 to 400)),
            Way(WayKind.Path, 6.0, line(-130 to -400, -130 to 400)),
            Way(WayKind.Path, 6.0, line(-400 to 130, 400 to 130)),
            Way(WayKind.Path, 4.0, line(10 to 130, -60 to 110, -130 to 60)),
            Way(WayKind.Path, 4.0, line(0 to -8, 0 to -150)),
        ),
        buildings = listOf(
            // The palace's courtyard is a hole in its footprint, so its walls run round the inside as well.
            rect(40, 40, 110, 110) + rect(60, 60, 90, 90),
            rect(-60, 4, -36, 22), rect(-60, 26, -36, 44), rect(-32, 46, -10, 62), rect(-6, 46, 16, 62),
            rect(-30, -34, -8, -14), rect(6, -34, 28, -14), rect(34, -30, 58, -14),
            rect(66, -34, 90, -14), rect(96, -30, 120, -14), rect(140, -30, 160, -14), rect(140, 4, 162, 22),
            rect(-120, 4, -96, 24), rect(-92, 4, -70, 20), rect(-120, -34, -96, -14), rect(-90, -32, -66, -14),
            rect(-120, 70, -100, 88), rect(-96, 92, -76, 106), rect(-50, 70, -30, 90), rect(-24, 70, 0, 92),
            rect(20, 138, 44, 156), rect(60, 138, 84, 156), rect(96, 138, 120, 156), rect(140, 60, 160, 82),
            rect(140, 100, 160, 120), rect(-20, -60, -4, -42), rect(14, -62, 40, -44),
        ).mapIndexed { i, footprint -> Building(footprint, seed = 300L + i) },
        pois = listOf(
            Poi(at(-48, 35), MapIcon.Bank), Poi(at(-35, 35), door = true),
            Poi(at(17, -24), MapIcon.KebabShop), Poi(at(17, -13), door = true),
            Poi(at(-48, 12), MapIcon.GeneralStore), Poi(at(-35, 12), door = true),
            Poi(at(150, 70), MapIcon.Transportation), Poi(at(139, 70), door = true),
            Poi(at(-108, -24), MapIcon.Hairdresser), Poi(at(-108, -13), door = true),
            Poi(at(75, 39), door = true), Poi(at(108, 139), door = true),
        ),
    ),
    route = Route(DESERT_ROUTE),
    walkers = listOf(
        // Traders crossing the market, a palace guard, a gatekeeper and someone drawing water at the oasis.
        Walker(Dot.Npc, Route(line(-20 to 0, 20 to 34)), pace = 0.15),
        Walker(Dot.Npc, Route(line(20 to 0, -20 to 34)), pace = 0.12, start = 20.0),
        Walker(Dot.Npc, Route(line(-24 to 20, 24 to 20)), pace = 0.2),
        Walker(Dot.Npc, Route(line(70 to 36, 80 to 36)), pace = 0.05),
        Walker(Dot.Npc, Route(line(126 to 60, 126 to 80)), pace = 0.1),
        Walker(Dot.Npc, Route(line(-145 to 72, -175 to 72)), pace = 0.08),
        Walker(Dot.Player, Route(line(-400 to -6, 400 to -6)), pace = 0.9, start = 300.0),
        Walker(Dot.Player, Route(DESERT_ROUTE.reversed()), pace = 1.1, start = 250.0),
        Walker(Dot.Player, Route(line(-38 to 30, -38 to 36)), pace = 0.04),
        Walker(Dot.Player, Route(line(0 to -8, 0 to -150)), pace = 0.6, start = 30.0),
    ),
)

/**
 * The made-up places the minimap wanders through without where the user is, each in the game's manner and about two
 * minutes round at a stroll, while townsfolk keep to their spots and other players pass through.
 */
val MADE_UP_TOUR = MadeUpTour(listOf(VILLAGE, HARBOUR, FOREST, DESERT))

private fun at(east: Int, north: Int) = Local(east.toDouble(), north.toDouble())

private fun line(vararg points: Pair<Int, Int>) = points.map { (east, north) -> at(east, north) }

private fun polygon(vararg points: Pair<Int, Int>): Polygon = listOf(line(*points))

private fun rect(west: Int, south: Int, east: Int, north: Int) = polygon(west to south, east to south, east to north, west to north)

private fun oval(east: Int, north: Int, radiusEast: Int, radiusNorth: Int): Polygon = listOf(
    List(OVAL_POINTS) { i ->
        val angle = 2 * PI * i / OVAL_POINTS
        Local(east + radiusEast * cos(angle), north + radiusNorth * sin(angle))
    },
)

private const val OVAL_POINTS = 24
