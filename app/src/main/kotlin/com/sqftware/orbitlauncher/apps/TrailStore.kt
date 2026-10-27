package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.minimap.Trails

interface TrailStore {
    fun load(): Trails

    fun save(trails: Trails)
}

class SharedPreferencesTrailStore(context: Context) : TrailStore {
    private val prefs = context.getSharedPreferences("trails", Context.MODE_PRIVATE)

    override fun load() = Trails.parse(prefs.getString(TRAILS, null).orEmpty())

    override fun save(trails: Trails) = prefs.edit { putString(TRAILS, trails.encode()) }

    private companion object {
        const val TRAILS = "trails"
    }
}
