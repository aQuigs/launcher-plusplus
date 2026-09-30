package com.sqftware.orbitlauncher.apps

import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

interface SystemBars {
    /** Shows the status and navigation bars, or hides them until shown again. */
    fun setShown(shown: Boolean)
}

/** The bars over [window]. While they are hidden, a swipe in from the edge they sit on shows them for a moment. */
class WindowSystemBars(window: Window) : SystemBars {
    private val controller = WindowCompat.getInsetsController(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun setShown(shown: Boolean) {
        val bars = WindowInsetsCompat.Type.systemBars()
        if (shown) controller.show(bars) else controller.hide(bars)
    }
}
