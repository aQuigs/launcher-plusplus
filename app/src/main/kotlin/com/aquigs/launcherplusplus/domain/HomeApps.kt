package com.aquigs.launcherplusplus.domain

/** A place on the home screen where the user keeps apps. */
enum class HomePlace { Ring, Dock }

/** The apps the user keeps on the home screen: on the ring round the emblem, and in the dock under the pages. */
data class HomeApps(val ring: Favourites = Favourites(), val dock: Favourites = Favourites()) {
    operator fun get(place: HomePlace): Favourites = when (place) {
        HomePlace.Ring -> ring
        HomePlace.Dock -> dock
    }

    /** Adds [app] to [place], or takes it off if it is already there. The other place is left as it is. */
    fun toggle(place: HomePlace, app: AppEntry): HomeApps = when (place) {
        HomePlace.Ring -> copy(ring = ring.toggle(app))
        HomePlace.Dock -> copy(dock = dock.toggle(app))
    }
}
