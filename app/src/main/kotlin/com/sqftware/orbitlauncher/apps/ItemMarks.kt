package com.sqftware.orbitlauncher.apps

import android.util.LruCache
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.Pixels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/**
 * Each app's item mark by its key, [made][make] once for every size of item drawn from it. An app on the ring and in the
 * dock asks for both sizes at once, so a second ask waits for the mark the first is making rather than making its own,
 * and the making goes on when the first stops waiting. Forgotten when its package changes, as an update may bring a new
 * icon; a mark that could not be made is not kept.
 */
internal class ItemMarks(maxApps: Int, private val make: (AppEntry) -> Pixels?) {
    private class Making(val packageName: String, val mark: Deferred<Pixels?>)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val marks = LruCache<String, Making>(maxApps)

    suspend operator fun get(app: AppEntry): Pixels? {
        val making = synchronized(this) {
            marks[app.key] ?: Making(app.packageName, scope.async { make(app) }).also { marks.put(app.key, it) }
        }
        return making.mark.await() ?: null.also { synchronized(this) { if (marks[app.key] === making) marks.remove(app.key) } }
    }

    fun forget(packageNames: Collection<String>) = synchronized(this) {
        marks.snapshot().forEach { (key, making) -> if (making.packageName in packageNames) marks.remove(key) }
    }
}
