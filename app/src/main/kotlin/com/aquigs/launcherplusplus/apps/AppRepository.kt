package com.aquigs.launcherplusplus.apps

import androidx.compose.ui.graphics.ImageBitmap
import com.aquigs.launcherplusplus.domain.AppEntry

interface AppRepository {
    fun installedApps(): List<AppEntry>

    /** Blocking: reads the icon from the app's package. Null when the activity is gone. */
    fun icon(app: AppEntry): ImageBitmap?

    fun launch(app: AppEntry)
}
