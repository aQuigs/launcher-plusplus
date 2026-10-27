package com.sqftware.orbitlauncher.apps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.location.LocationRequestCompat
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.Executor

interface LocationSource {
    /** Whether the user lets the launcher know roughly where they are. */
    fun isGranted(): Boolean

    /** Asks the user for it; the answer comes on [answers]. */
    fun request()

    /** Each answer to a [request]: whether the user granted it. */
    fun answers(): Flow<Boolean>

    /**
     * Where the user is, roughly: a fix as soon as one is to be had, then a new one as they move, for as long as it is
     * collected. Collect it only while the launcher is visible. Without the grant it ends at once, empty.
     */
    fun locations(): Flow<LatLon>

    /** Where the phone last had a fix, from whatever asked for one, without asking for a new one; none without the grant. */
    fun lastFix(): LatLon?
}

private const val TAG = "PlatformLocationSource"
private const val REQUEST_KEY = "location"
// Often enough that the map keeps up with a car. The provider still works out a fix this often, but only while the
// launcher is in sight; the distance gate spares the repaints.
private const val UPDATE_MILLIS = 5_000L
// Also how far a fix must be from the last map's centre for LiveMinimap to paint a new one.
internal const val UPDATE_METRES = 50f

// A fix this recent is as good as a fresh one for a map that moves every 50 m.
private const val FRESH_MILLIS = 10 * 60_000L

/**
 * The platform's LocationManager, asking for precise location, since a coarse fix is snapped to a grid about 2 km wide and
 * puts the map on the wrong streets; a user who allows only an approximate place still gets a map, of roughly where they
 * are. It runs at balanced power: the last fix if it is fresh, or else one current fix, then updates no more often than
 * 5 s or 50 m apart. No Play services: the launcher has no other use for them. The
 * permission is asked through the activity's [registry], as the home role is; the adapter lives as long as the activity.
 */
class PlatformLocationSource(private val context: Context, registry: ActivityResultRegistry) : LocationSource {
    private val manager = context.getSystemService(LocationManager::class.java)

    // Kept until heard, since an answer can arrive as a recreated activity registers, before the screen collects.
    private val granted = Channel<Boolean>(Channel.CONFLATED)
    private val dialog = registry.register(REQUEST_KEY, RequestMultiplePermissions()) { answers -> granted.trySend(answers.values.any { it }) }

    override fun isGranted() =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Android asks for precise location only alongside approximate, so the user can still choose either.
    override fun request() = dialog.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))

    override fun answers(): Flow<Boolean> = granted.receiveAsFlow()

    override fun lastFix(): LatLon? {
        if (!isGranted()) return null
        return try {
            manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)?.let { LatLon(it.latitude, it.longitude) }
        } catch (e: SecurityException) {
            Log.w(TAG, "Location access was revoked", e)
            null
        }
    }

    override fun locations(): Flow<LatLon> = callbackFlow {
        val provider = manager.bestProvider()
        if (!isGranted() || provider == null) {
            close()
            return@callbackFlow
        }
        val executor = Executor { it.run() }
        val send = { location: Location? -> location?.let { trySend(LatLon(it.latitude, it.longitude)) } }
        val listener = LocationListenerCompat { send(it) }
        val cancel = CancellationSignal()
        try {
            val last = manager.getLastKnownLocation(provider)?.takeIf { System.currentTimeMillis() - it.time < FRESH_MILLIS }
            if (last != null) send(last) else LocationManagerCompat.getCurrentLocation(manager, provider, cancel, executor) { send(it) }
            val request = LocationRequestCompat.Builder(UPDATE_MILLIS)
                .setMinUpdateDistanceMeters(UPDATE_METRES)
                .setQuality(LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY)
                .build()
            LocationManagerCompat.requestLocationUpdates(manager, provider, request, executor, listener)
        } catch (e: SecurityException) {
            // Revoked between the check and the request: the map simply stops.
            Log.w(TAG, "Location access was revoked", e)
            close()
        }
        awaitClose {
            cancel.cancel()
            try {
                LocationManagerCompat.removeUpdates(manager, listener)
            } catch (e: SecurityException) {
                Log.w(TAG, "Location access was revoked", e)
            }
        }
    }.conflate()
}

// The fused provider, on Android 12 and up, mixes GPS, Wi-Fi and cell fixes as a request's quality asks; the network
// provider, before it, gives the cheap ones. GPS is the fallback where neither is on.
internal fun LocationManager.bestProvider(): String? {
    val candidates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
    } else {
        listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
    }
    return candidates.firstOrNull { it in allProviders && isProviderEnabled(it) }
}
