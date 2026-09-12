package com.aquigs.launcherplusplus.domain

/** The apps in one place on the home screen, in order, kept by [AppEntry.key] so a reload of the app list keeps them. */
data class Favourites(val keys: List<String> = emptyList()) {
    operator fun contains(app: AppEntry): Boolean = app.key in keys

    /** Adds [app] at the end, or takes it off if it is already there. */
    fun toggle(app: AppEntry): Favourites = Favourites(if (app in this) keys - app.key else keys + app.key)

    /**
     * The installed apps in order. An app that is missing is skipped but kept, so an app that disappears while it updates
     * comes back in its old place.
     */
    fun resolve(apps: List<AppEntry>): List<AppEntry> {
        val byKey = apps.associateBy { it.key }
        return keys.mapNotNull(byKey::get)
    }
}
