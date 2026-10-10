package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.RingSpinMode

interface RingSpinStore {
    /** What spins the ring, after a press until the user picks otherwise. */
    fun load(): RingSpinMode

    fun save(mode: RingSpinMode)
}

class SharedPreferencesRingSpinStore(context: Context) : RingSpinStore {
    private val prefs = context.getSharedPreferences("spin", Context.MODE_PRIVATE)

    // Before the mode there was only a switch, which a user may have turned off.
    override fun load() = RingSpinMode.entries.find { it.name == prefs.getString(MODE, null) }
        ?: if (prefs.getBoolean(SWITCH, true)) RingSpinMode.AfterPress else RingSpinMode.Off

    override fun save(mode: RingSpinMode) = prefs.edit { putString(MODE, mode.name) }

    private companion object {
        const val MODE = "mode"
        const val SWITCH = "ring"
    }
}
