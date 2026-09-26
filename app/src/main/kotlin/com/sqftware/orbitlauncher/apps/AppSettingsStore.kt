package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.AppSettings

interface AppSettingsStore {
    fun load(): AppSettings

    fun save(settings: AppSettings)
}

class SharedPreferencesAppSettingsStore(context: Context) : AppSettingsStore {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // The sets the preferences hand back must not be changed, so each is copied.
    override fun load() = AppSettings(
        badgeOff = prefs.getStringSet(BADGE_OFF, null).orEmpty().toSet(),
        offBuiltInCards = prefs.getStringSet(OFF_BUILT_IN_CARDS, null).orEmpty().toSet(),
    )

    override fun save(settings: AppSettings) = prefs.edit {
        putStringSet(BADGE_OFF, settings.badgeOff)
        putStringSet(OFF_BUILT_IN_CARDS, settings.offBuiltInCards)
    }

    private companion object {
        const val BADGE_OFF = "badge_off"
        const val OFF_BUILT_IN_CARDS = "off_built_in_cards"
    }
}
