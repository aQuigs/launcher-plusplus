package com.aquigs.launcherplusplus.apps

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
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
                val flags = info.applicationInfo.flags
                AppEntry(
                    label = info.label.toString(),
                    packageName = info.componentName.packageName,
                    activityName = info.componentName.className,
                    // An app built into the system can only lose its updates, so without any there is nothing to remove.
                    canUninstall = flags and ApplicationInfo.FLAG_SYSTEM == 0 || flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0,
                )
            }
            .sortedByLabel()

    override fun icon(app: AppEntry): ImageBitmap? =
        try {
            val size = activityManager.launcherLargeIconSize
            launcherApps.resolveActivity(Intent().setComponent(app.component), user)
                ?.getIcon(activityManager.launcherLargeIconDensity)
                ?.toBitmap(size, size)
                ?.asImageBitmap()
        } catch (e: RuntimeException) {
            // A favourite whose icon cannot be drawn would otherwise crash the launcher on every start.
            Log.w(TAG, "No icon for ${app.key}", e)
            null
        }

    override fun launch(app: AppEntry) = startOrLog(app.key) { launcherApps.startMainActivity(app.component, user, null, null) }

    override fun shortcuts(app: AppEntry): List<AppShortcut> =
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

    override fun shortcutIcon(shortcut: AppShortcut): ImageBitmap? =
        try {
            val size = activityManager.launcherLargeIconSize
            launcherApps.getShortcuts(shortcutQuery(shortcut.packageName).setShortcutIds(listOf(shortcut.id)), user)
                ?.firstOrNull()
                ?.let { launcherApps.getShortcutIconDrawable(it, activityManager.launcherLargeIconDensity) }
                ?.toBitmap(size, size)
                ?.asImageBitmap()
        } catch (e: RuntimeException) {
            Log.w(TAG, "No icon for ${shortcut.packageName} shortcut ${shortcut.id}", e)
            null
        }

    override fun startShortcut(shortcut: AppShortcut) = startOrLog("${shortcut.packageName} shortcut ${shortcut.id}") {
        launcherApps.startShortcut(shortcut.packageName, shortcut.id, null, null, user)
    }

    override fun openAppInfo(app: AppEntry) = startOrLog("app info for ${app.key}") {
        launcherApps.startAppDetailsActivity(app.component, user, null, null)
    }

    override fun uninstall(app: AppEntry) = startOrLog("uninstall for ${app.key}") {
        context.startActivity(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null)))
    }

    // What the user tapped may have gone since it was listed: an uninstall the package callback has not reported yet, a
    // shortcut the app disabled, a locked user. None of that may crash the launcher.
    private inline fun startOrLog(what: String, start: () -> Unit) {
        try {
            start()
        } catch (e: RuntimeException) {
            if (e !is ActivityNotFoundException && e !is SecurityException && e !is IllegalStateException) throw e
            Log.w(TAG, "Cannot start $what", e)
        }
    }

    private fun shortcutQuery(packageName: String) =
        ShortcutQuery().setPackage(packageName).setQueryFlags(ShortcutQuery.FLAG_MATCH_MANIFEST or ShortcutQuery.FLAG_MATCH_DYNAMIC)

    private val AppEntry.component get() = ComponentName(packageName, activityName)

    // Any of these can add, remove or relabel launchable activities, so each reloads the whole list.
    private class PackageChanges(private val onChange: () -> Unit) : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) = onChange()

        override fun onPackageRemoved(packageName: String, user: UserHandle) = onChange()

        override fun onPackageChanged(packageName: String, user: UserHandle) = onChange()

        override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = onChange()

        override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = onChange()
    }
}
