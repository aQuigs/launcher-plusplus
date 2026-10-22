package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Where the user reads the licence of the data the minimap is drawn from. */
interface MapLicence {
    fun open()
}

private const val TAG = "OpenStreetMapLicence"

/** OpenStreetMap's copyright page, which names the licence of its data. */
class OpenStreetMapLicence(private val context: Context) : MapLicence {
    override fun open() = startOrLog(TAG, "the map data's licence") {
        val page = Intent(Intent.ACTION_VIEW, "https://www.openstreetmap.org/copyright".toUri())
        context.startActivity(page.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
