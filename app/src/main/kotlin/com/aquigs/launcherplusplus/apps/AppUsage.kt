package com.aquigs.launcherplusplus.apps

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import com.aquigs.launcherplusplus.domain.ForegroundTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.TimeUnit

interface AppUsage {
    /** Whether the user has granted the launcher usage access, which the Most Used card needs. */
    fun isUsageAccessGranted(): Boolean

    /**
     * The week's foreground time per package when collected, or null without usage access. The access is granted in
     * Settings, so collect afresh on each return to the front.
     */
    fun foregroundTime(): Flow<ForegroundTime?>

    /** Opens the system screen where the user grants or revokes usage access. */
    fun openUsageSettings()
}

private const val TAG = "SystemAppUsage"
private val WINDOW_MILLIS = TimeUnit.DAYS.toMillis(7)

/** Usage as the system's usage stats report it, behind the usage-access app op the user grants in Settings. */
class SystemAppUsage(private val context: Context) : AppUsage {
    private val appOps = context.getSystemService(AppOpsManager::class.java)
    private val usageStats = context.getSystemService(UsageStatsManager::class.java)

    override fun isUsageAccessGranted(): Boolean {
        val mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        // The default mode defers to the permission itself, which only a system app can hold.
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    override fun foregroundTime(): Flow<ForegroundTime?> = flow { emit(if (isUsageAccessGranted()) ForegroundTime(queryWeek()) else null) }
        .flowOn(Dispatchers.IO)

    private fun queryWeek(): Map<String, Long> {
        val now = System.currentTimeMillis()
        return usageStats.queryAndAggregateUsageStats(now - WINDOW_MILLIS, now)
            .mapValues { (_, stats) -> stats.totalTimeInForeground }
            .filterValues { it > 0L }
    }

    override fun openUsageSettings() = startOrLog(TAG, "the usage access settings") {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
