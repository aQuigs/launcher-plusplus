package com.aquigs.launcherplusplus.domain

import java.io.Serializable

/** A place on the home screen where the user keeps apps. Serializable so the screen can save which one it is picking for. */
sealed interface HomePlace : Serializable {
    data object Ring : HomePlace

    data object Dock : HomePlace

    /** The folder in the ring's slot [index]. */
    data class Folder(val index: Int) : HomePlace
}

/** The apps the user keeps on the home screen: on the ring round the emblem, and in the dock under the pages. */
data class HomeApps(val ring: Ring = Ring(), val dock: Favourites = Favourites()) {
    /** The apps at [place]: those in slots of their own for the ring, and none for a folder that is not there. */
    operator fun get(place: HomePlace): Favourites = when (place) {
        HomePlace.Ring -> ring.apps
        HomePlace.Dock -> dock
        is HomePlace.Folder -> Favourites(ring.folder(place.index)?.keys.orEmpty())
    }

    /** Adds [app] to [place], unless it is already there. The other places are left as they are. */
    fun add(place: HomePlace, app: AppEntry): HomeApps = when (place) {
        HomePlace.Ring -> copy(ring = ring.add(app))
        HomePlace.Dock -> copy(dock = dock.add(app))
        is HomePlace.Folder -> copy(ring = ring.add(place.index, app))
    }

    /**
     * Adds [app] to [place], or takes it off if it is already there. The other places are left as they are, and so is a
     * folder left with one app or none: only "Remove folder" takes a folder off.
     */
    fun toggle(place: HomePlace, app: AppEntry): HomeApps = when (place) {
        HomePlace.Ring -> copy(ring = ring.toggle(app))
        HomePlace.Dock -> copy(dock = dock.toggle(app))
        is HomePlace.Folder -> copy(ring = ring.toggle(place.index, app))
    }
}
