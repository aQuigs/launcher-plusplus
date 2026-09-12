package com.aquigs.launcherplusplus.apps

import androidx.compose.ui.graphics.ImageBitmap
import com.aquigs.launcherplusplus.domain.AppEntry
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    /** The launchable apps sorted by label, loaded off the main thread and again whenever a package changes. */
    fun installedApps(): Flow<List<AppEntry>>

    /** Blocking: reads the icon from the app's package. Null when the activity is gone or its icon cannot be drawn. */
    fun icon(app: AppEntry): ImageBitmap?

    /** Starts [app]. Does nothing if it has gone since the list was loaded. */
    fun launch(app: AppEntry)
}
