package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.minimap.Journal
import com.sqftware.orbitlauncher.domain.minimap.LatLon

interface JournalStore {
    fun load(): Journal

    fun save(journal: Journal)
}

class SharedPreferencesJournalStore(context: Context) : JournalStore {
    private val prefs = context.getSharedPreferences("journal", Context.MODE_PRIVATE)

    override fun load() = Journal(
        quest = if (prefs.contains(QUEST_LAT)) LatLon(prefs.getDouble(QUEST_LAT), prefs.getDouble(QUEST_LON)) else null,
        discovered = prefs.getStringSet(DISCOVERED, null).orEmpty().toSet(),
    )

    override fun save(journal: Journal) = prefs.edit {
        val quest = journal.quest
        if (quest == null) {
            remove(QUEST_LAT)
            remove(QUEST_LON)
        } else {
            putDouble(QUEST_LAT, quest.lat)
            putDouble(QUEST_LON, quest.lon)
        }
        putStringSet(DISCOVERED, journal.discovered)
    }

    private companion object {
        const val QUEST_LAT = "quest_lat"
        const val QUEST_LON = "quest_lon"
        const val DISCOVERED = "discovered"
    }
}
