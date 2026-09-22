package com.aquigs.launcherplusplus.apps

import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.core.content.edit
import java.io.File

interface Relauncher {
    /** Ends the launcher's process and opens its home screen afresh in a new one, keeping everything the launcher stores. */
    fun restart()

    /**
     * Erases everything the launcher stores, then [restart]s on an empty home, where the widget host gives back the ids of
     * the widgets the page no longer holds. What the system keeps for the launcher stays: its notification and usage
     * access, the home role and the wallpaper.
     */
    fun reset()
}

private const val TAG = "SystemRelauncher"

class SystemRelauncher(private val context: Context) : Relauncher {
    // Ending the process alone also brings the launcher back, but through a black screen while it starts. Starting a new
    // home activity first, in a cleared task, keeps the old screen up until the new one draws, as Arc's restart does.
    // Ending the process at once means the old activity's teardown never runs, so nothing is saved over a reset.
    override fun restart() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startOrLog(TAG, "the home screen") { context.startActivity(home) }
        Process.killProcess(Process.myPid())
    }

    override fun reset() {
        erase()
        restart()
    }

    // Committed rather than applied, since the process ends next.
    internal fun erase() {
        File(context.dataDir, "shared_prefs").list().orEmpty().filter { it.endsWith(".xml") }.forEach { file ->
            context.getSharedPreferences(file.removeSuffix(".xml"), Context.MODE_PRIVATE).edit(commit = true) { clear() }
        }
    }
}
