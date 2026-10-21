package com.sqftware.orbitlauncher.domain.minimap

/** A small image: [argb] holds its pixels row by row, non-premultiplied. A pixel under half opacity is not drawn. */
class Sprite(val width: Int, val height: Int, val argb: IntArray) {
    init {
        require(argb.size == width * height) { "A ${width}x$height sprite needs ${width * height} pixels" }
    }
}

/** The map icons the minimap marks places with. */
enum class MapIcon {
    Bank,
    Altar,
    QuestStart,
    Transportation,
    Pub,
    CookeryShop,
    KebabShop,
    FoodShop,
    Apothecary,
    WaterSource,
    GrandExchange,
    ClothesShop,
    JewelleryShop,
    Hairdresser,
    PetShop,
    GeneralStore,
}

/**
 * The art the painter draws with, which comes from outside the domain. A tree is one of [trees], picked by its spot and
 * standing on the point two rows above its bottom edge; an [npc] dot and each of the [icons] are centred on their spot.
 * Whatever is missing is left out of the map.
 */
class MinimapSprites(
    val trees: List<Sprite> = emptyList(),
    val npc: Sprite? = null,
    val icons: Map<MapIcon, Sprite> = emptyMap(),
)
