package com.aquigs.launcherplusplus.domain

/** The apps on the home ring, clockwise from the top, kept by [AppEntry.key] so a reload of the app list keeps them. */
data class Favourites(val keys: List<String> = emptyList()) {
    operator fun contains(app: AppEntry): Boolean = app.key in keys

    /** Adds [app] at the end of the ring, or takes it off if it is already there. */
    fun toggle(app: AppEntry): Favourites = Favourites(if (app in this) keys - app.key else keys + app.key)

    /**
     * The installed favourites in ring order. A favourite whose app is missing is skipped but kept, so an app that
     * disappears while it updates comes back in its old place.
     */
    fun resolve(apps: List<AppEntry>): List<AppEntry> {
        val byKey = apps.associateBy { it.key }
        return keys.mapNotNull(byKey::get)
    }
}
