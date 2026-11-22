package com.sqftware.orbitlauncher.apps

import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.MapTiles
import com.sqftware.orbitlauncher.domain.minimap.RoadRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import com.sqftware.orbitlauncher.domain.minimap.paintRetryDelay
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The way by road to the user's quest, found from map [tiles] off the main thread. */
class QuestRoutes(tiles: MapTiles) {
    private val router = RoadRouter(tiles)

    // A search runs on past a cancelled caller until it next looks, so the next caller would share the router with it.
    private val routing = Mutex()

    /**
     * The route by road to [quest] from each place the user is [at], or null when it is too far, a tile cannot be had,
     * or no road joins them. A new place cancels the search from the last.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun routes(at: Flow<LatLon>, quest: LatLon): Flow<List<LatLon>?> = at
        .transformLatest { from ->
            // Nothing else comes while the user stands still, so a route a tile was missing for is looked for again, as
            // the minimap paints again; a new place cancels the wait.
            var retry = 0
            while (true) {
                val route = routing.withLock { router.route(from, quest) }
                emit(route)
                if (route != null) break
                delay(paintRetryDelay(retry++))
            }
        }
        .flowOn(Dispatchers.Default)
}
