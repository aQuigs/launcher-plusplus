package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.net.toUri
import com.sqftware.orbitlauncher.domain.AppRole
import com.sqftware.orbitlauncher.domain.DefaultApps
import com.sqftware.orbitlauncher.domain.activityKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface DefaultAppFinder {
    suspend fun find(): DefaultApps
}

/** The apps the system resolves each role's intent to, as the user has set their defaults. */
class SystemDefaultAppFinder(private val context: Context) : DefaultAppFinder {
    private val packageManager = context.packageManager
    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    override suspend fun find(): DefaultApps = withContext(Dispatchers.IO) {
        val byRole = AppRole.entries.mapNotNull { role -> handler(intentFor(role))?.let(::launchKey)?.let { role to it } }.toMap()
        // This launcher among them.
        val launchers = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
        DefaultApps(byRole, skips = launchers.toSet() + listOfNotNull(handler(Intent(Settings.ACTION_SETTINGS))?.packageName))
    }

    private fun intentFor(role: AppRole): Intent = when (role) {
        AppRole.Phone -> Intent(Intent.ACTION_DIAL)
        AppRole.Messages -> Intent(Intent.ACTION_SENDTO, "smsto:".toUri())
        AppRole.Browser -> Intent(Intent.ACTION_VIEW, "https://".toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
        AppRole.Camera -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        AppRole.Email -> Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
        AppRole.Maps -> Intent(Intent.ACTION_VIEW, "geo:0,0".toUri())
        AppRole.Photos -> Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY)
        AppRole.Calendar -> Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
        AppRole.Store -> Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri())
        AppRole.Clock -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
    }

    // With no default set, the system resolves to its chooser, which is none of the handlers and may belong to any package;
    // then the app built into the phone, as the chooser would offer first, else any.
    private fun handler(intent: Intent): ActivityInfo? {
        val all = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).map { it.activityInfo }
        val chosen = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
        return all.firstOrNull { it.packageName == chosen?.packageName && it.name == chosen.name }
            ?: all.firstOrNull { it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 }
            ?: all.firstOrNull()
    }

    // The handler itself when the drawer lists it, as a phone's dialer shares its package with contacts on some phones;
    // else the package's first listed activity, as a role's own activity is often not a launchable one.
    private fun launchKey(handler: ActivityInfo): String? {
        val listed = launcherApps.getActivityList(handler.packageName, Process.myUserHandle()).map { it.componentName }
        val launched = listed.firstOrNull { it.className == handler.name } ?: listed.firstOrNull() ?: return null
        return activityKey(launched.packageName, launched.className)
    }
}
