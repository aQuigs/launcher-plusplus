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
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
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
}

private const val TAG = "PlatformLocationSource"
private const val REQUEST_KEY = "location"
private const val UPDATE_MILLIS = 60_000L
private const val UPDATE_METRES = 50f

// A fix this recent is as good as a fresh one for a map that moves every 50 m.
private const val FRESH_MILLIS = 10 * 60_000L

/**
 * The platform's LocationManager with coarse location, at low power: the last fix if it is fresh, one current fix, then
 * updates no more often than a minute or 50 m apart. No Play services: the launcher has no other use for them. The
 * permission is asked through the activity's [registry], as the home role is; the adapter lives as long as the activity.
 */
class PlatformLocationSource(private val context: Context, registry: ActivityResultRegistry) : LocationSource {
    private val manager = context.getSystemService(LocationManager::class.java)

    // Kept until heard, since an answer can arrive as a recreated activity registers, before the screen collects.
    private val granted = Channel<Boolean>(Channel.CONFLATED)
    private val dialog = registry.register(REQUEST_KEY, RequestPermission()) { granted.trySend(it) }

    override fun isGranted() =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun request() = dialog.launch(Manifest.permission.ACCESS_COARSE_LOCATION)

    override fun answers(): Flow<Boolean> = granted.receiveAsFlow()

    override fun locations(): Flow<LatLon> = callbackFlow {
        val provider = provider()
        if (!isGranted() || provider == null) {
            close()
            return@callbackFlow
        }
        val executor = Executor { it.run() }
        val send = { location: Location? -> location?.let { trySend(LatLon(it.latitude, it.longitude)) } }
        val listener = LocationListenerCompat { send(it) }
        val cancel = CancellationSignal()
        try {
            manager.getLastKnownLocation(provider)?.takeIf { System.currentTimeMillis() - it.time < FRESH_MILLIS }?.let(send)
            LocationManagerCompat.getCurrentLocation(manager, provider, cancel, executor) { send(it) }
            val request = LocationRequestCompat.Builder(UPDATE_MILLIS)
                .setMinUpdateDistanceMeters(UPDATE_METRES)
                .setQuality(LocationRequestCompat.QUALITY_LOW_POWER)
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

    // The fused provider, on Android 12 and up, mixes in Wi-Fi and cell fixes and spends the least power for a rough
    // place; the network provider does the same before it. GPS is the fallback where neither is on, from Android 12,
    // which serves it to coarse access; Android 11 wants fine access for it.
    private fun provider(): String? {
        val candidates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        } else {
            listOf(LocationManager.NETWORK_PROVIDER)
        }
        return candidates.firstOrNull { it in manager.allProviders && manager.isProviderEnabled(it) }
    }
}
