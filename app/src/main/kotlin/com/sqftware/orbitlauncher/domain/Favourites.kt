package com.sqftware.orbitlauncher.domain

/** Favourite apps in one place on the home screen, in order, kept by [AppEntry.key] so a reload of the app list keeps them. */
data class Favourites(val keys: List<String> = emptyList()) {
    operator fun contains(app: AppEntry): Boolean = app.key in keys

    /** Adds [app] at the end, unless it is already there. */
    fun add(app: AppEntry): Favourites = if (app in this) this else Favourites(keys + app.key)

    /** Takes [app] off; an app that is not there changes nothing. */
    fun remove(app: AppEntry): Favourites = if (app in this) Favourites(keys - app.key) else this

    /** Adds [app] at the end, or takes it off if it is already there. */
    fun toggle(app: AppEntry): Favourites = if (app in this) remove(app) else add(app)

    /** Moves [app] to [target]'s place as [mode] says; either missing changes nothing. */
    fun move(app: AppEntry, target: AppEntry, mode: ReorderMode): Favourites =
        Favourites(keys.reordered(keys.indexOf(app.key), keys.indexOf(target.key), mode))

    /**
     * The installed favourites in order. A favourite whose app is missing is skipped but kept, so an app that disappears
     * while it updates comes back in its old place.
     */
    fun resolve(apps: List<AppEntry>): List<AppEntry> {
        val byKey = apps.associateBy { it.key }
        return keys.mapNotNull(byKey::get)
    }
}
