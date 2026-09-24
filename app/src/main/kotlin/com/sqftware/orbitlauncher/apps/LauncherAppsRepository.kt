package com.sqftware.orbitlauncher.apps

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.sqftware.orbitlauncher.domain.AppCategory
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.AppShortcut
import com.sqftware.orbitlauncher.domain.HomeApps
import com.sqftware.orbitlauncher.domain.isStorable
import com.sqftware.orbitlauncher.domain.sortedByLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

private const val TAG = "LauncherAppsRepository"
private const val MAX_SHORTCUTS = 4
// Room for a few hundred icons at the largest launcher size: every drawer row asks for its app's, and a cache the drawer
// churns through would evict the ring's and the dock's, which would then come back blank on the way home.
private const val ICON_CACHE_BYTES = 32 shl 20
private const val SHOWN_SHORTCUTS = ShortcutQuery.FLAG_MATCH_MANIFEST or ShortcutQuery.FLAG_MATCH_DYNAMIC

// What a square leaves free on each side when its corners touch a circle as wide: (1 - 1/√2) / 2.
private val SQUARE_IN_CIRCLE = ((1 - 1 / sqrt(2.0)) / 2).toFloat()

class LauncherAppsRepository(private val context: Context) : AppRepository {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val activityManager = context.getSystemService(ActivityManager::class.java)
    private val user = Process.myUserHandle()

    // An icon is asked for again each time its app comes back into view, and a collection card brings dozens back at a
    // tap; a drawn one is kept until the package list changes, as an update may bring a new icon.
    private val icons = object : LruCache<String, ImageBitmap>(ICON_CACHE_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    // One at a time and in order, so unpinning for an older home screen never lands after a newer one's.
    private val pinWork = Dispatchers.IO.limitedParallelism(1)

    override fun installedApps(): Flow<List<AppEntry>> =
        changes(withShortcuts = false) { icons.evictAll() }
            .map { loadApps() }
            .flowOn(Dispatchers.IO)

    override fun pinnedShortcuts(): Flow<List<AppEntry>> =
        changes(withShortcuts = true)
            .map { pinned().filter { it.isEnabled }.mapNotNull { it.toEntry() } }
            .flowOn(Dispatchers.IO)

    // Once at first, then again after each change the callback reports; changes while a load runs make one more load.
    private fun changes(withShortcuts: Boolean, onChange: () -> Unit = {}): Flow<Unit> =
        callbackFlow {
            val callback = PackageChanges(withShortcuts) {
                onChange()
                trySend(Unit)
            }
            launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
            send(Unit)
            awaitClose { launcherApps.unregisterCallback(callback) }
        }.conflate()

    private fun loadApps(): List<AppEntry> =
        launcherApps.getActivityList(null, user)
            .map { info ->
                AppEntry(
                    label = info.label.toString(),
                    packageName = info.componentName.packageName,
                    activityName = info.componentName.className,
                    // An app built into the system can only lose its updates, which its App info page offers.
                    canUninstall = info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0,
                    installedAt = info.firstInstallTime,
                    category = categoryHint(info.applicationInfo.category),
                )
            }
            .sortedByLabel()

    override suspend fun icon(app: AppEntry): ImageBitmap? = icons.get(app.key)
        ?: draw(app.key) { density ->
            when (val id = app.shortcutId) {
                null -> activityIcon(app, density)
                else -> pinnedIcon(shortcutInfo(app.packageName, id, ShortcutQuery.FLAG_MATCH_PINNED), app, density)
            }
        }?.also { icons.put(app.key, it) }

    private fun activityIcon(app: AppEntry, density: Int): Drawable? =
        launcherApps.resolveActivity(Intent().setComponent(app.component), user)?.getIcon(density)

    // The ring and the dialog that asks share it through the cache. A square icon, as a web page's is, would lose its
    // corners to the disc, so it shrinks to fit whole on the disc's tint; an adaptive one is made for the cut. A shortcut
    // without an icon of its own wears its app's, as on other launchers.
    private fun pinnedIcon(info: ShortcutInfo?, shortcut: AppEntry, density: Int): Drawable? =
        info?.let { launcherApps.getShortcutIconDrawable(it, density) }
            ?.let { if (it is AdaptiveIconDrawable) it else InsetDrawable(it, SQUARE_IN_CIRCLE) }
            ?: activityIcon(shortcut, density)

    override fun launch(app: AppEntry) = startOrLog(TAG, app.key) {
        when (val id = app.shortcutId) {
            null -> launcherApps.startMainActivity(app.component, user, null, null)
            else -> launcherApps.startShortcut(app.packageName, id, null, null, user)
        }
    }

    override suspend fun shortcuts(app: AppEntry): List<AppShortcut> = withContext(Dispatchers.IO) {
        if (app.shortcutId != null) return@withContext emptyList()
        query(shortcutQuery(app.packageName).setActivity(app.component), app.key)
            .filter { it.isEnabled }
            .sortedWith(compareBy({ !it.isDeclaredInManifest }, { it.rank }))
            .take(MAX_SHORTCUTS)
            .map { AppShortcut(packageName = it.`package`, id = it.id, label = it.label) }
    }

    override suspend fun shortcutIcon(shortcut: AppShortcut): ImageBitmap? = draw(shortcut.logName) { density ->
        shortcutInfo(shortcut.packageName, shortcut.id, SHOWN_SHORTCUTS)?.let { launcherApps.getShortcutIconDrawable(it, density) }
    }

    private fun shortcutInfo(packageName: String, id: String, flags: Int): ShortcutInfo? =
        launcherApps.getShortcuts(shortcutQuery(packageName, flags).setShortcutIds(listOf(id)), user)?.firstOrNull()

    override fun startShortcut(shortcut: AppShortcut) = startOrLog(TAG, shortcut.logName) {
        launcherApps.startShortcut(shortcut.packageName, shortcut.id, null, null, user)
    }

    override fun openAppInfo(app: AppEntry) = startOrLog(TAG, "app info for ${app.key}") {
        launcherApps.startAppDetailsActivity(app.component, user, null, null)
    }

    override fun uninstall(app: AppEntry) = startOrLog(TAG, "uninstall for ${app.key}") {
        // In a task of its own, left out of recents: in the launcher's task, HOME would clear the confirmation and count as
        // a press inside the launcher.
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS))
    }

    override fun pinRequest(intent: Intent): PinItem? {
        val request = launcherApps.getPinItemRequest(intent) ?: return null
        val info = request.shortcutInfo
        if (request.requestType != PinItemRequest.REQUEST_TYPE_SHORTCUT || info?.userHandle != user || !request.isValid) return null
        val shortcut = info.toEntry() ?: return null
        return object : PinItem {
            override val shortcut = shortcut
            override val appLabel by lazy { labelOf(info.`package`) }

            // Until it is pinned, a lookup by id finds nothing, since the app may only have made it for the request. Kept,
            // so the ring shows it at once if the user adds it.
            override suspend fun icon() = draw(shortcut.key) { density -> pinnedIcon(info, shortcut, density) }
                ?.also { icons.put(shortcut.key, it) }

            override fun accept() = try {
                request.accept()
            } catch (e: RuntimeException) {
                // Accepted already, or the launcher is no longer the home app the request was made to.
                Log.w(TAG, "Cannot pin ${shortcut.key}", e)
                false
            }
        }
    }

    override suspend fun unpinAllBut(homeApps: HomeApps) = withContext(pinWork) {
        val pinned = pinned(ShortcutQuery.FLAG_GET_KEY_FIELDS_ONLY).groupBy({ it.`package` }, { it.id })
        homeApps.keptPins(pinned).forEach { (packageName, ids) ->
            try {
                launcherApps.pinShortcuts(packageName, ids, user)
            } catch (e: RuntimeException) {
                // The app went meanwhile, or the launcher stopped being the home app.
                Log.w(TAG, "Cannot unpin the shortcuts of $packageName", e)
            }
        }
    }

    // The launcher's own pins, disabled ones too.
    private fun pinned(extraFlags: Int = 0) = query(ShortcutQuery().setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED or extraFlags), "pins")

    // Only the default home app may read other apps' shortcuts; until then there are none, and a menu offers the options
    // alone.
    private fun query(query: ShortcutQuery, what: String): List<ShortcutInfo> = try {
        if (launcherApps.hasShortcutHostPermission()) launcherApps.getShortcuts(query, user).orEmpty() else emptyList()
    } catch (e: RuntimeException) {
        // A locked user, or an app removed since the list loaded.
        Log.w(TAG, "No shortcuts for $what", e)
        emptyList()
    }

    private val ShortcutInfo.label get() = (shortLabel ?: longLabel ?: id).toString()

    private fun ShortcutInfo.toEntry(): AppEntry? = AppEntry(
        label = label,
        packageName = `package`,
        activityName = activity?.className.orEmpty(),
        canUninstall = false,
        shortcutId = id,
    ).takeIf { isStorable(it.key) }

    private fun labelOf(packageName: String): String = try {
        context.packageManager.run { getApplicationInfo(packageName, 0).loadLabel(this).toString() }
    } catch (_: PackageManager.NameNotFoundException) {
        // An app the launcher cannot see, having no launchable activity.
        packageName
    }

    private suspend fun draw(what: String, drawable: (density: Int) -> Drawable?): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            val size = activityManager.launcherLargeIconSize
            drawable(activityManager.launcherLargeIconDensity)?.toBitmap(size, size)?.asImageBitmap()
        } catch (e: RuntimeException) {
            // An app on the home screen whose icon cannot be drawn would otherwise crash the launcher on every start.
            Log.w(TAG, "No icon for $what", e)
            null
        }
    }

    private fun shortcutQuery(packageName: String, flags: Int = SHOWN_SHORTCUTS) =
        ShortcutQuery().setPackage(packageName).setQueryFlags(flags)

    private val AppEntry.component get() = ComponentName(packageName, activityName)

    // Only the system categories with a card of the same meaning; the rest, undefined included, say nothing.
    private fun categoryHint(category: Int): AppCategory? = when (category) {
        ApplicationInfo.CATEGORY_GAME -> AppCategory.Games
        ApplicationInfo.CATEGORY_AUDIO -> AppCategory.Music
        ApplicationInfo.CATEGORY_VIDEO -> AppCategory.Video
        ApplicationInfo.CATEGORY_IMAGE -> AppCategory.Photos
        ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.Social
        ApplicationInfo.CATEGORY_NEWS -> AppCategory.Media
        ApplicationInfo.CATEGORY_MAPS -> AppCategory.Transport
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.Productivity
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.Tools
        else -> null
    }

    private val AppShortcut.logName get() = "$packageName shortcut $id"

    // Any of these can add, remove or relabel launchable activities, so each reloads the whole list. Apps change their
    // shortcuts often, so only a list of shortcuts hears of that too.
    private class PackageChanges(private val withShortcuts: Boolean, private val onChange: () -> Unit) : LauncherApps.Callback() {
        override fun onShortcutsChanged(packageName: String, shortcuts: List<ShortcutInfo>, user: UserHandle) {
            if (withShortcuts) onChange()
        }

        override fun onPackageAdded(packageName: String, user: UserHandle) = onChange()

        override fun onPackageRemoved(packageName: String, user: UserHandle) = onChange()

        override fun onPackageChanged(packageName: String, user: UserHandle) = onChange()

        override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = onChange()

        override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = onChange()
    }
}
