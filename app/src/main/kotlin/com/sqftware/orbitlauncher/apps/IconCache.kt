package com.sqftware.orbitlauncher.apps

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import com.sqftware.orbitlauncher.domain.AppEntry

/**
 * Drawn icons by key, each forgotten when its package changes, as an update may bring a new icon. One drawn while a
 * package changed is not kept: it may have been read from the package as it was.
 */
internal class IconCache(maxBytes: Int) {
    private class Drawn(val packageName: String, val bitmap: ImageBitmap)

    private val drawn = object : LruCache<String, Drawn>(maxBytes) {
        override fun sizeOf(key: String, value: Drawn) = value.bitmap.width * value.bitmap.height * 4
    }
    private var generation = 0

    operator fun get(key: String): ImageBitmap? = drawn.get(key)?.bitmap

    /** Draws [app]'s icon, and keeps it unless a package changed meanwhile. */
    suspend fun keep(app: AppEntry, draw: suspend () -> ImageBitmap?): ImageBitmap? {
        val before = synchronized(this) { generation }
        return draw()?.also { bitmap ->
            synchronized(this) { if (generation == before) drawn.put(app.key, Drawn(app.packageName, bitmap)) }
        }
    }

    fun forget(packageNames: Collection<String>) = synchronized(this) {
        generation++
        drawn.snapshot().forEach { (key, icon) -> if (icon.packageName in packageNames) drawn.remove(key) }
    }
}
