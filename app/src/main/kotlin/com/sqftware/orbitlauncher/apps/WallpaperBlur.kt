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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        // Cleared whether or not the system blurs now, so a blur set before battery saver turned it off cannot come back
        // with no folder open. The system draws the blur on the window's dim layer, which it skips while it has nothing
        // to draw, so a dim too faint to see goes with it.
        val on = radius > 0 && window.windowManager.isCrossWindowBlurEnabled
        val behind = WindowManager.LayoutParams.FLAG_BLUR_BEHIND or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        val attributes = window.attributes
        val flags = if (on) attributes.flags or behind else attributes.flags and behind.inv()
        val blurRadius = if (on) radius else 0
        if (flags == attributes.flags && blurRadius == attributes.blurBehindRadius) return
        window.attributes = attributes.apply {
            this.flags = flags
            blurBehindRadius = blurRadius
            dimAmount = UNSEEN_DIM
        }
    }
}

private const val UNSEEN_DIM = 0.01f
