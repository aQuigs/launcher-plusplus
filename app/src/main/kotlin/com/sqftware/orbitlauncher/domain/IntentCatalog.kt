package com.sqftware.orbitlauncher.domain

import java.net.URLEncoder

/**
 * Every [AppIntent] the launcher knows. An app gets one by adding it here; its id is stored in icons' keys, so an intent
 * never changes its id, and one taken out leaves its icons unopenable until it is back.
 */
object IntentCatalog {
    private val all: List<AppIntent> = listOf(
        // Picked up where the phone is: https://developer.uber.com/docs/riders/ride-requests/tutorials/deep-links/introduction
        PlaceIntent("uber.ride", "com.ubercab", "ride", "Ride to a place") { place ->
            link(
                "uber://",
                "action" to "setPickup",
                "pickup" to "my_location",
                "dropoff[latitude]" to place.latitude?.plain(),
                "dropoff[longitude]" to place.longitude?.plain(),
                "dropoff[nickname]" to place.label,
                "dropoff[formatted_address]" to place.address,
            )
        },
        // As Lyft's Android SDK builds its deep link: https://github.com/lyft/lyft-android-sdk
        PlaceIntent("lyft.ride", "me.lyft.android", "ride", "Ride to a place") { place ->
            link(
                "lyft://ridetype",
                "id" to "lyft",
                "destination[latitude]" to place.latitude?.plain(),
                "destination[longitude]" to place.longitude?.plain(),
                "destination[address]" to place.address,
            )
        },
        // Undocumented; read from the app's own link handling (v26102604). It takes the destination as coordinates only,
        // so a typed address without them plans a trip from here and leaves the destination to be filled in there.
        PlaceIntent("waymo.ride", "com.waymo.carapp", "ride", "Ride to a place") { place ->
            link(
                "https://app.waymo.com/route/trip/create",
                "pickup" to "my_location",
                "dropoff_latitude" to place.latitude?.plain(),
                "dropoff_longitude" to place.longitude?.plain(),
            )
        },
        mapsDirections("maps.drive", "drive", "Drive to a place", "driving"),
        mapsDirections("maps.walk", "walk", "Walk to a place", "walking"),
        mapsDirections("maps.transit", "transit", "Take transit to a place", "transit"),
        // https://developers.google.com/waze/deeplinks. Without coordinates it searches for the address instead.
        PlaceIntent("waze.drive", "com.waze", "drive", "Drive to a place") { place ->
            val ll = place.coordinates
            link("https://waze.com/ul", "q" to if (ll == null) place.address else place.label, "ll" to ll, "navigate" to "yes")
        },
        // https://citymapper.com/news/2386. It finds the destination by its coordinates only, so a typed address without
        // them opens Citymapper with the destination left to fill in there.
        PlaceIntent("citymapper.directions", "com.citymapper.app.release", "directions", "Directions to a place") { place ->
            link(
                "citymapper://directions",
                "endcoord" to place.coordinates,
                "endname" to place.label,
                "endaddress" to place.address,
            )
        },
    )

    private val byId = all.associateBy { it.id }

    fun forPackage(packageName: String): List<AppIntent> = all.filter { it.packageName == packageName }

    fun byId(id: String): AppIntent? = byId[id]
}

/** An intent that takes a place, and opens the [link] to it. */
private class PlaceIntent(
    override val id: String,
    override val packageName: String,
    override val name: String,
    override val label: String,
    private val link: (PlaceValue) -> String,
) : AppIntent {
    override val input = InputKind.Place

    override fun request(value: InputValue?) = IntentRequest(packageName, link(value as PlaceValue))
}

/**
 * Google Maps directions from here to a place by [travelMode], starting navigation where the mode has it:
 * https://developers.google.com/maps/documentation/urls/get-started#directions-action
 */
private fun mapsDirections(id: String, name: String, label: String, travelMode: String) =
    PlaceIntent(id, "com.google.android.apps.maps", name, label) { place ->
        link(
            "https://www.google.com/maps/dir/",
            "api" to "1",
            "destination" to (place.coordinates ?: place.address),
            "travelmode" to travelMode,
            "dir_action" to "navigate",
        )
    }

/** "latitude,longitude", when both are known. */
private val PlaceValue.coordinates: String? get() = latLon?.let { "${it.lat.plain()},${it.lon.plain()}" }

/** In plain decimals, as the apps read a coordinate: never 1.0E-4. */
private fun Double.plain(): String = toBigDecimal().toPlainString()

/** [base] with a query of the [params] that have a value, each URL-encoded; the apps read their brackets unencoded. */
private fun link(base: String, vararg params: Pair<String, String?>): String =
    params.filter { it.second != null }
        .joinToString("&", prefix = "$base?") { (name, value) ->
            "$name=${URLEncoder.encode(value, "UTF-8").replace("+", "%20")}"
        }
