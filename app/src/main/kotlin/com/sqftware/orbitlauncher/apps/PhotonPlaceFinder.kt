package com.sqftware.orbitlauncher.apps

import android.util.Log
import com.sqftware.orbitlauncher.domain.OsmPlace
import com.sqftware.orbitlauncher.domain.PlaceValue
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.toPlace
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import kotlin.math.roundToLong

private const val TAG = "PhotonPlaceFinder"
private const val ENDPOINT = "https://photon.komoot.io/api/"
private const val TIMEOUT_MILLIS = 5_000

// The first letters typed name nothing worth a request to a free server.
private const val SHORTEST_QUERY = 3

/**
 * [PlaceFinder] through Photon, komoot's free search of OpenStreetMap, which finds businesses and landmarks by name as
 * well as addresses. It ranks places [near] the user first when the launcher knows where they are.
 */
class PhotonPlaceFinder(private val near: () -> LatLon?) : PlaceFinder {
    override suspend fun find(query: String): List<PlaceValue> {
        if (query.trim().length < SHORTEST_QUERY) return emptyList()
        return try {
            val (code, body) = getRequest(url(query), TIMEOUT_MILLIS).cancellableResponse()
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            places(body.decodeToString())
        } catch (e: IOException) {
            Log.w(TAG, "Cannot find places", e)
            emptyList()
        } catch (e: JSONException) {
            Log.w(TAG, "Cannot read the places found", e)
            emptyList()
        }
    }

    // Rounded to about a kilometer, which is all the ranking needs, so the server is never told exactly where the user is.
    private fun url(query: String): String {
        val bias = near()?.let { "&lat=${it.lat.rounded()}&lon=${it.lon.rounded()}" }.orEmpty()
        return "$ENDPOINT?q=${URLEncoder.encode(query, "UTF-8")}&limit=$PLACES_PER_FINDER$bias"
    }

    private fun Double.rounded() = (this * 100).roundToLong() / 100.0

    private fun places(body: String): List<PlaceValue> {
        val features = JSONObject(body).getJSONArray("features")
        return (0 until features.length()).mapNotNull { index ->
            val feature = features.getJSONObject(index)
            val properties = feature.getJSONObject("properties")
            val point = feature.optJSONObject("geometry")?.optJSONArray("coordinates")
            fun text(key: String) = properties.optString(key).takeIf { it.isNotBlank() }
            OsmPlace(
                name = text("name"),
                houseNumber = text("housenumber"),
                street = text("street"),
                city = text("city") ?: text("county"),
                state = text("state"),
                postcode = text("postcode"),
                country = text("country"),
                latitude = point?.optDouble(1)?.takeUnless(Double::isNaN),
                longitude = point?.optDouble(0)?.takeUnless(Double::isNaN),
            ).toPlace()
        }
    }
}
