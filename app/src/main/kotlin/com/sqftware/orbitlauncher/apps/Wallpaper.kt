package com.sqftware.orbitlauncher.apps

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

interface Wallpaper {
    /** Whether the home screen's wallpaper is light enough to want dark text, as the system judges it. */
    fun isLight(): Boolean

    /** [isLight] now, then again whenever the wallpaper changes. Collect while the launcher is visible. */
    fun lightness(): Flow<Boolean>

    /** Makes [image] the home screen's wallpaper. It blocks, so call it off the main thread. */
    fun set(image: Bitmap)
}

/** The system's home screen wallpaper, through the colours the system extracts from it. */
class SystemWallpaper(context: Context) : Wallpaper {
    private val manager = WallpaperManager.getInstance(context)

    // A live wallpaper may not report its colours; the launcher's own dark look suits one it cannot judge.
    override fun isLight(): Boolean = manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.wantsDarkText() == true

    override fun lightness(): Flow<Boolean> =
        callbackFlow {
            val changes = WallpaperManager.OnColorsChangedListener { colors, which ->
                if (which and WallpaperManager.FLAG_SYSTEM != 0) trySend(colors?.wantsDarkText() == true)
            }
            manager.addOnColorsChangedListener(changes, Handler(Looper.getMainLooper()))
            send(isLight())
            awaitClose { manager.removeOnColorsChangedListener(changes) }
        }
            .conflate()
            .distinctUntilChanged()

    override fun set(image: Bitmap) {
        // A wallpaper the system cannot take, or a system server that has gone, may not crash the launcher.
        try {
            manager.setBitmap(image, null, true, WallpaperManager.FLAG_SYSTEM)
        } catch (e: IOException) {
            Log.w(TAG, "Cannot set the wallpaper", e)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Cannot set the wallpaper", e)
        }
    }

    private companion object {
        const val TAG = "SystemWallpaper"
    }
}

// The hint the system sets for launchers is public only from Android 12; before it, judge by the dominant colour.
private fun WallpaperColors.wantsDarkText(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
    } else {
        primaryColor.luminance() > 0.5f
    }
