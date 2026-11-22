package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.sqftware.orbitlauncher.domain.PlaceValue
import com.sqftware.orbitlauncher.domain.combinedPlaces
import com.sqftware.orbitlauncher.domain.isWrittenAddress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.coroutines.resume

private const val TAG = "PlaceSearch"

/** The most places one finder offers. */
internal const val PLACES_PER_FINDER = 5

// A finder slower than this is left out, so a server that is slow or blocked never holds up the others' places.
private const val FINDER_TIMEOUT_MILLIS = 4_000L

/** One way of finding places. */
interface PlaceFinder {
    /** The places [query] names, best first; none when it names none or the search cannot be made. */
    suspend fun find(query: String): List<PlaceValue>
}

/** Finds places for the prompt that asks for one. */
interface PlaceSearch : PlaceFinder {
    /** Opens the maps app, where a place found can be shared back to the launcher, and says whether it did. */
    fun openMaps(): Boolean
}

/**
 * [PlaceSearch] asking [byName] and [byAddress] at once and listing their places, each once: [byAddress]'s first for
 * a [written address][isWrittenAddress], which it finds exactly where a search by name matches loosely, else [byName]'s.
 */
class SystemPlaceSearch(
    private val context: Context,
    private val byName: PlaceFinder,
    private val byAddress: PlaceFinder,
) : PlaceSearch {
    override suspend fun find(query: String): List<PlaceValue> {
        if (query.isBlank()) return emptyList()
        val finders = if (isWrittenAddress(query)) listOf(byAddress, byName) else listOf(byName, byAddress)
        return coroutineScope { combinedPlaces(finders.map { async { it.findSafely(query) } }.awaitAll()) }
    }

    // One finder failing, however it fails, leaves the others' places.
    private suspend fun PlaceFinder.findSafely(query: String): List<PlaceValue> = try {
        withTimeoutOrNull(FINDER_TIMEOUT_MILLIS) { find(query) }.orEmpty()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "A place search failed", e)
        emptyList()
    }

    override fun openMaps() = started(TAG, "maps") {
        context.startActivity(mapsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** [PlaceFinder] through the system's geocoder, which needs no key of the launcher's own. */
class GeocoderPlaceFinder(context: Context) : PlaceFinder {
    private val geocoder = Geocoder(context)

    override suspend fun find(query: String): List<PlaceValue> {
        if (!Geocoder.isPresent()) return emptyList()
        return addresses(query).map { it.toPlace() }
    }

    private suspend fun addresses(query: String): List<Address> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { done ->
                geocoder.getFromLocationName(query, PLACES_PER_FINDER, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = done.resume(addresses)

                    override fun onError(errorMessage: String?) {
                        Log.w(TAG, "Cannot find places: $errorMessage")
                        done.resume(emptyList())
                    }
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, PLACES_PER_FINDER).orEmpty()
                } catch (e: IOException) {
                    Log.w(TAG, "Cannot find places", e)
                    emptyList()
                }
            }
        }

    // An address the geocoder found for a business names it as its feature; for a street address, the feature is the house
    // number, which says less than the start of the address, as far as its first comma: the street.
    private fun Address.toPlace(): PlaceValue {
        val lines = (0..maxAddressLineIndex).map(::getAddressLine)
        val address = lines.joinToString(", ")
        val first = lines.firstOrNull().orEmpty().substringBefore(",")
        val name = featureName?.takeIf { it.isNotBlank() && !it.all(Char::isDigit) && !first.startsWith(it) } ?: first
        // Some geocoders leave out where a place is, and reading it then throws.
        return PlaceValue(
            label = name.ifEmpty { address },
            address = address,
            latitude = latitude.takeIf { hasLatitude() },
            longitude = longitude.takeIf { hasLongitude() },
        )
    }
}
