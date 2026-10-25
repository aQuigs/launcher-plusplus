package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.sqftware.orbitlauncher.R
import com.sqftware.orbitlauncher.domain.Pixels
import com.sqftware.orbitlauncher.domain.minimap.MapIcon
import com.sqftware.orbitlauncher.domain.minimap.MapStyle
import com.sqftware.orbitlauncher.domain.minimap.MinimapSprites

interface MinimapArt {
    /** The sprites the map in [style] draws with; any it lacks are left off the map. Call it off the main thread. */
    fun sprites(style: MapStyle): MinimapSprites
}

/**
 * How many pixels across Skyrim's map icons are drawn from their vectors: enough to stay sharp at the size the map
 * shows them, which scales them smoothly rather than by whole pixels.
 */
const val SKYRIM_ICON_PX = 72

/**
 * The map's sprites from the app's drawables, decoded once: Old School's at their own pixel size, as the game draws
 * them, and Skyrim's icons drawn from their vectors at [SKYRIM_ICON_PX].
 */
class ResourceMinimapArt(private val context: Context) : MinimapArt {
    private val oldSchool by lazy {
        MinimapSprites(
            trees = listOf(R.drawable.osrs_map_tree_0, R.drawable.osrs_map_tree_1).map(::sprite),
            npc = sprite(R.drawable.osrs_300_1),
            player = sprite(R.drawable.osrs_map_player),
            icons = MapIcon.entries.associateWith { sprite(oldSchoolDrawableOf(it)) },
        )
    }

    private val skyrim by lazy {
        val drawn = mutableMapOf<Int, Pixels>()
        MinimapSprites(icons = MapIcon.entries.associateWith { drawn.getOrPut(skyrimDrawableOf(it)) { vector(skyrimDrawableOf(it)) } })
    }

    override fun sprites(style: MapStyle) = when (style) {
        MapStyle.OldSchool -> oldSchool
        MapStyle.Skyrim -> skyrim
    }

    private fun oldSchoolDrawableOf(icon: MapIcon) = when (icon) {
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

    // Skyrim marks few kinds of place: where to drink, to trade, to pray, to take a carriage, to buy potions or draw
    // water, and the quest marker's arrow where a town's business is done.
    private fun skyrimDrawableOf(icon: MapIcon) = when (icon) {
        MapIcon.Pub, MapIcon.CookeryShop, MapIcon.KebabShop -> R.drawable.skyrim_inn
        MapIcon.Bank, MapIcon.GrandExchange, MapIcon.GeneralStore, MapIcon.FoodShop, MapIcon.ClothesShop,
        MapIcon.JewelleryShop, MapIcon.Hairdresser, MapIcon.PetShop -> R.drawable.skyrim_merchant
        MapIcon.Altar -> R.drawable.skyrim_shrine
        MapIcon.Transportation -> R.drawable.skyrim_stables
        MapIcon.Apothecary -> R.drawable.skyrim_apothecary
        MapIcon.WaterSource -> R.drawable.skyrim_well
        MapIcon.QuestStart -> R.drawable.skyrim_quest
    }

    private fun sprite(@DrawableRes id: Int): Pixels =
        BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply { inScaled = false }).pixels()

    private fun vector(@DrawableRes id: Int): Pixels =
        checkNotNull(ContextCompat.getDrawable(context, id)).toBitmap(SKYRIM_ICON_PX, SKYRIM_ICON_PX).pixels()
}
