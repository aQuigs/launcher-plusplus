package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.sqftware.orbitlauncher.domain.PlaceValue
import com.sqftware.orbitlauncher.domain.combinedPlaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume

private const val TAG = "PlaceSearch"
private const val MAX_RESULTS = 5

/** Finds places for the prompt that asks for one. */
interface PlaceSearch {
    /** The places [query] names, best first; none when it names none or the search cannot be made. */
    suspend fun find(query: String): List<PlaceValue>

    /** Opens the maps app, where a place found can be shared back to the launcher, and says whether it did. */
    fun openMaps(): Boolean
}

/** One way of finding places, which a [PlaceSearch] gathers with others. */
fun interface PlaceFinder {
    /** The places [query] names, best first; none when it names none or the search cannot be made. */
    suspend fun find(query: String): List<PlaceValue>
}

/** [PlaceSearch] asking each of [finders] at once and listing their places in that order, each place once. */
class SystemPlaceSearch(private val context: Context, private val finders: List<PlaceFinder>) : PlaceSearch {
    override suspend fun find(query: String): List<PlaceValue> {
        if (query.isBlank()) return emptyList()
        return coroutineScope { combinedPlaces(finders.map { async { it.find(query) } }.awaitAll()) }
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
                geocoder.getFromLocationName(query, MAX_RESULTS, object : Geocoder.GeocodeListener {
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
                    geocoder.getFromLocationName(query, MAX_RESULTS).orEmpty()
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
