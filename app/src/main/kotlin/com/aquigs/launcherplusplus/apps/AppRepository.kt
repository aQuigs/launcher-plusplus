package com.aquigs.launcherplusplus.apps

import androidx.compose.ui.graphics.ImageBitmap
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.AppShortcut
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    /** The launchable apps sorted by label, loaded off the main thread and again whenever a package changes. */
    fun installedApps(): Flow<List<AppEntry>>

    /** Reads the icon from the app's package. Null when the activity is gone or its icon cannot be drawn. */
    suspend fun icon(app: AppEntry): ImageBitmap?

    /** Starts [app]. Does nothing if it has gone since the list was loaded. */
    fun launch(app: AppEntry)

    /** At most four of the app's shortcuts, manifest ones first. Empty unless the launcher is the home app. */
    suspend fun shortcuts(app: AppEntry): List<AppShortcut>

    /** The shortcut's icon. Null when it has none, has gone, or cannot be drawn. */
    suspend fun shortcutIcon(shortcut: AppShortcut): ImageBitmap?

    /** Starts [shortcut]. Does nothing if it has gone or been disabled since it was listed. */
    fun startShortcut(shortcut: AppShortcut)

    /** Opens the system's details page for [app]. */
    fun openAppInfo(app: AppEntry)

    /** Asks the system to uninstall [app], which confirms with the user first. */
    fun uninstall(app: AppEntry)
}
