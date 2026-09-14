package com.aquigs.launcherplusplus.apps

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.aquigs.launcherplusplus.domain.AppCategory
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.AppShortcut
import com.aquigs.launcherplusplus.domain.sortedByLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val TAG = "LauncherAppsRepository"
private const val MAX_SHORTCUTS = 4

class LauncherAppsRepository(private val context: Context) : AppRepository {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val activityManager = context.getSystemService(ActivityManager::class.java)
    private val user = Process.myUserHandle()

    override fun installedApps(): Flow<List<AppEntry>> =
        callbackFlow {
            val callback = PackageChanges { trySend(Unit) }
            launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
            send(Unit)
            awaitClose { launcherApps.unregisterCallback(callback) }
        }
            .conflate()
            .map { loadApps() }
            .flowOn(Dispatchers.IO)

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

    override suspend fun icon(app: AppEntry): ImageBitmap? = draw(app.key) { density ->
        launcherApps.resolveActivity(Intent().setComponent(app.component), user)?.getIcon(density)
    }

    override fun launch(app: AppEntry) = startOrLog(TAG, app.key) { launcherApps.startMainActivity(app.component, user, null, null) }

    override suspend fun shortcuts(app: AppEntry): List<AppShortcut> = withContext(Dispatchers.IO) {
        try {
            if (launcherApps.hasShortcutHostPermission()) {
                launcherApps.getShortcuts(shortcutQuery(app.packageName).setActivity(app.component), user).orEmpty()
                    .filter { it.isEnabled }
                    .sortedWith(compareBy({ !it.isDeclaredInManifest }, { it.rank }))
                    .take(MAX_SHORTCUTS)
                    .map { AppShortcut(packageName = it.`package`, id = it.id, label = (it.shortLabel ?: it.longLabel ?: it.id).toString()) }
            } else {
                // Only the default home app may read other apps' shortcuts; until then the menu offers the options alone.
                emptyList()
            }
        } catch (e: RuntimeException) {
            // A locked user, or an app removed since the list loaded.
            Log.w(TAG, "No shortcuts for ${app.key}", e)
            emptyList()
        }
    }

    override suspend fun shortcutIcon(shortcut: AppShortcut): ImageBitmap? = draw(shortcut.logName) { density ->
        launcherApps.getShortcuts(shortcutQuery(shortcut.packageName).setShortcutIds(listOf(shortcut.id)), user)
            ?.firstOrNull()
            ?.let { launcherApps.getShortcutIconDrawable(it, density) }
    }

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

    private fun shortcutQuery(packageName: String) =
        ShortcutQuery().setPackage(packageName).setQueryFlags(ShortcutQuery.FLAG_MATCH_MANIFEST or ShortcutQuery.FLAG_MATCH_DYNAMIC)

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

    // Any of these can add, remove or relabel launchable activities, so each reloads the whole list.
    private class PackageChanges(private val onChange: () -> Unit) : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) = onChange()

        override fun onPackageRemoved(packageName: String, user: UserHandle) = onChange()

        override fun onPackageChanged(packageName: String, user: UserHandle) = onChange()

        override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = onChange()

        override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = onChange()
    }
}
