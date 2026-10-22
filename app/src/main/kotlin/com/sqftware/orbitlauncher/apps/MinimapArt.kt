package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.graphics.BitmapFactory
import com.sqftware.orbitlauncher.R
import com.sqftware.orbitlauncher.domain.Pixels
import com.sqftware.orbitlauncher.domain.minimap.MapIcon
import com.sqftware.orbitlauncher.domain.minimap.MinimapSprites

interface MinimapArt {
    /** The sprites the minimap draws with; any it lacks are left off the map. Call it off the main thread. */
    fun sprites(): MinimapSprites
}

/** The minimap's sprites from the app's drawables, at their own pixel size, as the game draws them. */
class ResourceMinimapArt(private val context: Context) : MinimapArt {
    override fun sprites() = MinimapSprites(
        trees = listOf(R.drawable.osrs_map_tree_0, R.drawable.osrs_map_tree_1).map(::sprite),
        npc = sprite(R.drawable.osrs_300_1),
        icons = MapIcon.entries.associateWith { sprite(drawableOf(it)) },
    )

    private fun drawableOf(icon: MapIcon) = when (icon) {
        MapIcon.Bank -> R.drawable.osrs_map_bank
        MapIcon.Altar -> R.drawable.osrs_map_altar
        MapIcon.QuestStart -> R.drawable.osrs_map_quest_start
        MapIcon.Transportation -> R.drawable.osrs_map_transportation
        MapIcon.Pub -> R.drawable.osrs_map_pub
        MapIcon.CookeryShop -> R.drawable.osrs_map_cookery_shop
        MapIcon.KebabShop -> R.drawable.osrs_map_kebab_shop
        MapIcon.FoodShop -> R.drawable.osrs_map_food_shop
        MapIcon.Apothecary -> R.drawable.osrs_map_apothecary
        MapIcon.WaterSource -> R.drawable.osrs_map_water_source
        MapIcon.GrandExchange -> R.drawable.osrs_map_grand_exchange
        MapIcon.ClothesShop -> R.drawable.osrs_map_clothes_shop
        MapIcon.JewelleryShop -> R.drawable.osrs_map_jewellery_shop
        MapIcon.Hairdresser -> R.drawable.osrs_map_hairdresser
        MapIcon.PetShop -> R.drawable.osrs_map_pet_shop
        MapIcon.GeneralStore -> R.drawable.osrs_map_general_store
    }

    private fun sprite(id: Int): Pixels =
        BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply { inScaled = false }).pixels()
}
