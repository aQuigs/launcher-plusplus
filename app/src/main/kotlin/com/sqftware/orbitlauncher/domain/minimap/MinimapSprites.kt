package com.sqftware.orbitlauncher.domain.minimap

import com.sqftware.orbitlauncher.domain.Pixels

/** The map icons the minimap marks places with. Where they crowd, the lower [priority] shows. */
enum class MapIcon(val priority: Int) {
    Bank(0),
    Altar(1),
    QuestStart(2),
    Transportation(3),
    Pub(4),
    CookeryShop(5),
    KebabShop(5),
    FoodShop(5),
    Apothecary(6),
    WaterSource(6),
    GrandExchange(6),
    ClothesShop(7),
    JewelleryShop(7),
    Hairdresser(7),
    PetShop(7),
    GeneralStore(8),
}

/**
 * The art the painter draws with, which comes from outside the domain. A tree is one of [trees], picked by its spot and
 * standing on the point two rows above its bottom edge; an [npc] dot and each of the [icons] are centred on their spot.
 * A pixel under half opacity is not drawn, and whatever is missing is left out of the map.
 */
class MinimapSprites(
    val trees: List<Pixels> = emptyList(),
    val npc: Pixels? = null,
    val icons: Map<MapIcon, Pixels> = emptyMap(),
)
