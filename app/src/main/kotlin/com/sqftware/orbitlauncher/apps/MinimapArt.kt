package com.sqftware.orbitlauncher.apps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import com.sqftware.orbitlauncher.domain.minimap.MapIcon
import com.sqftware.orbitlauncher.domain.minimap.MinimapSprites
import com.sqftware.orbitlauncher.domain.minimap.Sprite

interface MinimapArt {
    /** The sprites the minimap draws with; any it lacks are left off the map. Call it off the main thread. */
    fun sprites(): MinimapSprites
}

/** Which drawables hold the minimap's sprites, by resource name. */
class MinimapSpriteNames(
    val trees: List<String> = emptyList(),
    val npc: String? = null,
    val icons: Map<MapIcon, String> = emptyMap(),
)

/** The minimap's sprites from the app's drawables, at their own pixel size, as the game draws them. */
class ResourceMinimapArt(private val context: Context, private val names: MinimapSpriteNames) : MinimapArt {
    override fun sprites() = MinimapSprites(
        trees = names.trees.mapNotNull(::sprite),
        npc = names.npc?.let(::sprite),
        icons = names.icons.mapNotNull { (icon, name) -> sprite(name)?.let { icon to it } }.toMap(),
    )

    // By name, since the art ships with the theme that draws it and a build without it must still draw a map.
    @SuppressLint("DiscouragedApi")
    private fun sprite(name: String): Sprite? {
        val id = context.resources.getIdentifier(name, "drawable", context.packageName).takeIf { it != 0 } ?: return null
        val bitmap = BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply { inScaled = false }) ?: return null
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return Sprite(bitmap.width, bitmap.height, pixels)
    }
}
