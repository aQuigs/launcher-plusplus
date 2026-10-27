package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.minimap.Trail
import com.sqftware.orbitlauncher.domain.minimap.Trails

interface TrailStore {
    /** The trails kept, none being logged: one logging when the launcher was ended stops where it was last saved. */
    fun load(): Trails

    fun saveFinished(trails: List<Trail>)

    fun saveGrowing(trail: Trail?)
}

/** Keeps the finished trails apart from the growing one, so a fix rewrites only the run it is part of. */
class SharedPreferencesTrailStore(context: Context) : TrailStore {
    private val prefs = context.getSharedPreferences("trails", Context.MODE_PRIVATE)

    override fun load() = Trails(Trails.parse(prefs.getString(FINISHED, null).orEmpty()) + Trails.parse(prefs.getString(GROWING, null).orEmpty()))

    override fun saveFinished(trails: List<Trail>) = prefs.edit { putString(FINISHED, Trails.encode(trails)) }

    override fun saveGrowing(trail: Trail?) = prefs.edit { putString(GROWING, Trails.encode(listOfNotNull(trail))) }

    private companion object {
        const val FINISHED = "finished"
        const val GROWING = "growing"
    }
}
