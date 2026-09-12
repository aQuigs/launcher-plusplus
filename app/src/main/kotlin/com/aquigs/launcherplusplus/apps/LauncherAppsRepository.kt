package com.aquigs.launcherplusplus.apps

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.sortedByLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

private const val TAG = "LauncherAppsRepository"

class LauncherAppsRepository(context: Context) : AppRepository {
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

    override fun launch(app: AppEntry) {
        try {
            launcherApps.startMainActivity(app.component, user, null, null)
        } catch (e: RuntimeException) {
            // The list trails an uninstall until the package callback arrives; a tap in between must not crash the launcher.
            if (e !is ActivityNotFoundException && e !is SecurityException) throw e
            Log.w(TAG, "Cannot launch ${app.key}", e)
        }
    }

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
