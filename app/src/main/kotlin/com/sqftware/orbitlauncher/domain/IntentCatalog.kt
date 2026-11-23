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
                "dropoff[latitude]" to place.latitude?.toString(),
                "dropoff[longitude]" to place.longitude?.toString(),
                "dropoff[nickname]" to place.label,
                "dropoff[formatted_address]" to place.address,
            )
        },
        // As Lyft's Android SDK builds its deep link: https://github.com/lyft/lyft-android-sdk
        PlaceIntent("lyft.ride", "me.lyft.android", "ride", "Ride to a place") { place ->
            link(
                "lyft://ridetype",
                "id" to "lyft",
                "destination[latitude]" to place.latitude?.toString(),
                "destination[longitude]" to place.longitude?.toString(),
                "destination[address]" to place.address,
            )
        },
        // Undocumented; read from the app's own link handling (v26102604). It takes coordinates only, so a typed address
        // without them opens the trip planner from here, to fill in the destination there.
        PlaceIntent("waymo.ride", "com.waymo.carapp", "ride", "Ride to a place") { place ->
            link(
                "https://app.waymo.com/route/trip/create",
                "pickup" to "my_location",
                "dropoff_latitude" to place.latitude?.toString(),
                "dropoff_longitude" to place.longitude?.toString(),
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

/** [base] with a query of the [params] that have a value, each URL-encoded; the apps read their brackets unencoded. */
private fun link(base: String, vararg params: Pair<String, String?>): String =
    params.filter { it.second != null }
        .joinToString("&", prefix = "$base?") { (name, value) ->
            "$name=${URLEncoder.encode(value, "UTF-8").replace("+", "%20")}"
        }
