package com.sqftware.orbitlauncher.apps

import android.os.Build
import android.view.Window
import android.view.WindowManager

interface WallpaperBlur {
    /** Blurs the wallpaper behind the launcher by [radius] pixels, or not at all at 0. */
    fun blur(radius: Int)
}

/**
 * The blur of all that lies behind [window], the wallpaper, where the system blurs across windows. Where it does not, as
 * with battery saver on, the wallpaper stays sharp and the backdrop's veil alone sets it apart. A window's background
 * blur would need a translucent window, which a home screen is not.
 */
class WindowWallpaperBlur(private val window: Window) : WallpaperBlur {
    override fun blur(radius: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !window.windowManager.isCrossWindowBlurEnabled) return
        // The system draws the blur on the window's dim layer, which it skips while it has nothing to draw, so a dim too
        // faint to see goes with it.
        val behind = WindowManager.LayoutParams.FLAG_BLUR_BEHIND or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        window.attributes = window.attributes.apply {
            blurBehindRadius = radius
            dimAmount = UNSEEN_DIM
            flags = if (radius > 0) flags or behind else flags and behind.inv()
        }
    }
}

private const val UNSEEN_DIM = 0.01f
