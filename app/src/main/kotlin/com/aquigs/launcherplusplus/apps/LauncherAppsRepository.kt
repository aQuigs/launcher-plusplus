package com.aquigs.launcherplusplus.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.sortedByLabel

class LauncherAppsRepository(context: Context) : AppRepository {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val user = Process.myUserHandle()

    override fun installedApps(): List<AppEntry> =
        launcherApps.getActivityList(null, user)
            .map { info ->
                AppEntry(
                    label = info.label.toString(),
                    packageName = info.componentName.packageName,
                    activityName = info.componentName.className,
                )
            }
            .sortedByLabel()

    override fun launch(app: AppEntry) {
        launcherApps.startMainActivity(ComponentName(app.packageName, app.activityName), user, null, null)
    }
}
