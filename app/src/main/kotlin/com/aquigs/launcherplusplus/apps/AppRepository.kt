package com.aquigs.launcherplusplus.apps

import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.AppShortcut
import com.aquigs.launcherplusplus.domain.HomeApps
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    /** The launchable apps sorted by label, loaded off the main thread and again whenever a package changes. */
    fun installedApps(): Flow<List<AppEntry>>

    /**
     * The enabled shortcuts the launcher has pinned, loaded off the main thread and again whenever a shortcut or a package
     * changes. Empty unless the launcher is the home app.
     */
    fun pinnedShortcuts(): Flow<List<AppEntry>>

    /** Reads the icon from the app's package, or the pinned shortcut's. Null when it is gone or cannot be drawn. */
    suspend fun icon(app: AppEntry): ImageBitmap?

    /** Starts [app], or the pinned shortcut. Does nothing if it has gone since the list was loaded. */
    fun launch(app: AppEntry)

    /** At most four of the app's shortcuts, manifest ones first. None for a pinned shortcut, or until the launcher is the home app. */
    suspend fun shortcuts(app: AppEntry): List<AppShortcut>

    /** The shortcut's icon. Null when it has none, has gone, or cannot be drawn. */
    suspend fun shortcutIcon(shortcut: AppShortcut): ImageBitmap?

    /** Starts [shortcut]. Does nothing if it has gone or been disabled since it was listed. */
    fun startShortcut(shortcut: AppShortcut)

    /** Opens the system's details page for [app]. */
    fun openAppInfo(app: AppEntry)

    /** Asks the system to uninstall [app], which confirms with the user first. */
    fun uninstall(app: AppEntry)

    /**
     * The request in [intent] from an app to pin one of its shortcuts, if the launcher can take it: a pending request, for
     * a shortcut of the launcher's own user whose id the home screen can store.
     */
    fun pinRequest(intent: Intent): PinItem?

    /** Unpins every shortcut the launcher has pinned that [homeApps] no longer holds, so its app stops counting it as on home. */
    suspend fun unpinAllBut(homeApps: HomeApps)
}

/** An app's pending request to pin [shortcut], which [appLabel] names the app of. */
interface PinItem {
    val shortcut: AppEntry
    val appLabel: String

    /** The shortcut's icon, which only the request has until it is pinned. */
    suspend fun icon(): ImageBitmap?

    /** Pins the shortcut, and says whether it is pinned: false once the request is answered, cancelled or out of date. */
    fun accept(): Boolean
}
