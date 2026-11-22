package com.sqftware.orbitlauncher.apps

import android.os.Build
import android.view.Window

interface WallpaperBlur {
    /** Blurs the wallpaper behind the launcher by [radius] pixels, or not at all at 0. */
    fun blur(radius: Int)
}

/**
 * The blur of what lies behind [window], the wallpaper, where the system blurs across windows. Where it does not, as with
 * battery saver on, the wallpaper stays sharp and the backdrop's veil alone sets it apart.
 */
class WindowWallpaperBlur(private val window: Window) : WallpaperBlur {
    override fun blur(radius: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window.windowManager.isCrossWindowBlurEnabled) {
            window.setBackgroundBlurRadius(radius)
        }
    }
}
