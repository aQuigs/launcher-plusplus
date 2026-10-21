package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.util.Log
import com.sqftware.orbitlauncher.domain.minimap.TileId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

interface MapTiles {
    /** The vector tile [id], an OpenMapTiles Mapbox Vector Tile, or null when it cannot be had now (offline, say). */
    suspend fun tile(id: TileId): ByteArray?
}

private const val TAG = "OpenFreeMapTiles"
private const val TILE_JSON = "https://tiles.openfreemap.org/planet"
private const val TIMEOUT_MILLIS = 15_000
private const val KEPT_TILES = 64

// The tile server turns away some generic clients; this one names itself.
private const val USER_AGENT = "OrbitLauncher (Android)"

/**
 * OpenFreeMap's planet tiles: free, keyless, and in the OpenMapTiles schema. A tile once fetched is kept in the cache
 * directory, so a place already seen draws offline and nothing is fetched again until the user moves onto new tiles.
 */
class OpenFreeMapTiles(context: Context) : MapTiles {
    private val dir = File(context.cacheDir, "map-tiles")
    private val fetching = Mutex()

    // The tile URLs carry the planet build's date, which moves weekly, so they come from the TileJSON, once per run.
    private var template: String? = null

    override suspend fun tile(id: TileId): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(dir, "${id.z}/${id.x}/${id.y}.mvt")
        // One fetch at a time: the map asks for a handful of tiles, and the server is a free one.
        fetching.withLock {
            if (file.exists()) return@withLock file.readBytes().also { file.setLastModified(System.currentTimeMillis()) }
            try {
                val bytes = fetch(id) ?: return@withLock null
                file.parentFile?.mkdirs()
                val partial = File(file.path + ".part")
                partial.writeBytes(bytes)
                partial.renameTo(file)
                prune()
                bytes
            } catch (e: IOException) {
                Log.i(TAG, "Could not fetch tile $id", e)
                null
            } catch (e: JSONException) {
                Log.w(TAG, "Unreadable TileJSON", e)
                null
            }
        }
    }

    private fun fetch(id: TileId): ByteArray? {
        repeat(2) {
            val url = (template ?: resolveTemplate()).replace("{z}", "${id.z}").replace("{x}", "${id.x}").replace("{y}", "${id.y}")
            val (code, body) = get(url)
            when {
                code == HttpURLConnection.HTTP_OK -> return body
                // A build is retired some weeks on, so its URLs go; the TileJSON names the current one.
                code == HttpURLConnection.HTTP_NOT_FOUND -> template = null
                else -> throw IOException("HTTP $code for tile $id")
            }
        }
        return null
    }

    private fun resolveTemplate(): String {
        val (code, body) = get(TILE_JSON)
        if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code for the TileJSON")
        return JSONObject(String(body)).getJSONArray("tiles").getString(0).also { template = it }
    }

    // HttpURLConnection asks for gzip and inflates it on its own.
    private fun get(url: String): Pair<Int, ByteArray> {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            val code = connection.responseCode
            val body = if (code == HttpURLConnection.HTTP_OK) connection.inputStream.use { it.readBytes() } else ByteArray(0)
            return code to body
        } finally {
            connection.disconnect()
        }
    }

    // The system clears the cache directory when space runs short, but a commuter's tiles would pile up until then.
    private fun prune() {
        val tiles = dir.walkBottomUp().filter { it.isFile }.sortedByDescending { it.lastModified() }.toList()
        tiles.drop(KEPT_TILES).forEach { it.delete() }
    }
}
