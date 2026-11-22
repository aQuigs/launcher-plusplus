package com.sqftware.orbitlauncher.apps

import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.MapTiles
import com.sqftware.orbitlauncher.domain.minimap.RoadRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Finds the way by road to the user's quest from map [tiles], off the main thread. */
class QuestRoutes(tiles: MapTiles) {
    private val router = RoadRouter(tiles)

    // A search runs on past a cancelled caller until it next suspends, so the next caller would share the router with it.
    private val routing = Mutex()

    /** The route by road from [from] to [quest], or null when it is too far, a tile cannot be had, or no road joins them. */
    suspend fun route(from: LatLon, quest: LatLon): List<LatLon>? = withContext(Dispatchers.Default) {
        routing.withLock { router.route(from, quest) }
    }
}
