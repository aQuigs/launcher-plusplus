package com.sqftware.orbitlauncher.domain

import java.net.URLEncoder

/**
 * Every [AppIntent] the launcher knows. An app gets one by adding it here; its id is stored in icons' keys, so an intent
 * never changes its id, and one taken out leaves its icons unopenable until it is back.
 */
object IntentCatalog {
    val all: List<AppIntent> = listOf(UberRide, LyftRide)

    private val byId = all.associateBy { it.id }

    fun forPackage(packageName: String): List<AppIntent> = all.filter { it.packageName == packageName }

    fun byId(id: String): AppIntent? = byId[id]
}

/** Uber's ride request, picked up where the phone is: https://developer.uber.com/docs/riders/ride-requests/tutorials/deep-links/introduction */
private object UberRide : AppIntent {
    override val id = "uber.ride"
    override val packageName = "com.ubercab"
    override val name = "ride"
    override val label = "Ride to a place"
    override val input = InputKind.Place

    override fun request(value: InputValue?): IntentRequest {
        val place = value as PlaceValue
        return IntentRequest(
            packageName,
            link(
                "uber://",
                "action" to "setPickup",
                "pickup" to "my_location",
                "dropoff[latitude]" to place.latitude?.toString(),
                "dropoff[longitude]" to place.longitude?.toString(),
                "dropoff[nickname]" to place.label,
                "dropoff[formatted_address]" to place.address,
            ),
        )
    }
}

/** Lyft's ride, as its Android SDK's deep link builds it: https://github.com/lyft/lyft-android-sdk */
private object LyftRide : AppIntent {
    override val id = "lyft.ride"
    override val packageName = "me.lyft.android"
    override val name = "ride"
    override val label = "Ride to a place"
    override val input = InputKind.Place

    override fun request(value: InputValue?): IntentRequest {
        val place = value as PlaceValue
        return IntentRequest(
            packageName,
            link(
                "lyft://ridetype",
                "id" to "lyft",
                "destination[latitude]" to place.latitude?.toString(),
                "destination[longitude]" to place.longitude?.toString(),
                "destination[address]" to place.address,
            ),
        )
    }
}

/** [base] with a query of the [params] that have a value, each URL-encoded; the apps read their brackets unencoded. */
private fun link(base: String, vararg params: Pair<String, String?>): String =
    params.filter { it.second != null }
        .joinToString("&", prefix = "$base?") { (name, value) ->
            "$name=${URLEncoder.encode(value, "UTF-8").replace("+", "%20")}"
        }
