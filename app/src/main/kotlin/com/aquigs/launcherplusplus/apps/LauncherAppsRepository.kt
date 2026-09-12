package com.aquigs.launcherplusplus.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
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

    override fun icon(app: AppEntry): ImageBitmap? =
        launcherApps.resolveActivity(Intent().setComponent(app.component), user)
            ?.getIcon(0)
            ?.toBitmap()
            ?.asImageBitmap()

    override fun launch(app: AppEntry) {
        launcherApps.startMainActivity(app.component, user, null, null)
    }

    private val AppEntry.component get() = ComponentName(packageName, activityName)
}
