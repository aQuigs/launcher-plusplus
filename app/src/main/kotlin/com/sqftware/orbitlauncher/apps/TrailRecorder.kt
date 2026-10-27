package com.sqftware.orbitlauncher.apps

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.sqftware.orbitlauncher.R
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.Trails
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

interface TrailRecorder {
    /** The trails the user has logged, the one being logged last, growing as they move. */
    val trails: StateFlow<Trails>

    /** Logs a new trail of where the user goes, with the screen off and the launcher out of sight, until [stop]ped. */
    fun start()

    fun stop()
}

private const val TAG = "TrailRecorder"
private const val NOTIFICATIONS_KEY = "trail_notifications"

/**
 * Logs trails in [TrailService], which shows that it is logging in a notification, so the launcher asks, once, through
 * the activity's [registry], to post one. Without the location grant it logs nothing.
 */
class ServiceTrailRecorder(private val context: Context, registry: ActivityResultRegistry) : TrailRecorder {
    private val notifications = registry.register(NOTIFICATIONS_KEY, RequestPermission()) {}

    override val trails = TrailBook.of(context).trails

    override fun start() {
        if (!context.granted(Manifest.permission.ACCESS_COARSE_LOCATION)) return
        ContextCompat.startForegroundService(context, Intent(context, TrailService::class.java))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !context.granted(Manifest.permission.POST_NOTIFICATIONS)) {
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun stop() {
        context.stopService(Intent(context, TrailService::class.java))
    }
}

private fun Context.granted(permission: String) = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

/** The trails, kept for the one process the service that logs them and the screen that shows them share. */
internal class TrailBook private constructor(private val store: TrailStore) {
    private val state = MutableStateFlow(store.load())
    val trails = state.asStateFlow()

    fun change(how: (Trails) -> Trails) = store.save(state.updateAndGet(how))

    companion object {
        @Volatile
        private var shared: TrailBook? = null

        fun of(context: Context) = shared ?: synchronized(this) {
            shared ?: TrailBook(SharedPreferencesTrailStore(context.applicationContext)).also { shared = it }
        }
    }
}

private const val CHANNEL_ID = "trail"
private const val NOTIFICATION_ID = 1
private const val ACTION_STOP = "com.sqftware.orbitlauncher.STOP_TRAIL"

/**
 * Logs a trail as a foreground service, whose notification says so and stops it. Android lets a location service start
 * only while the launcher is in sight, and then lends it the location access the user gave the launcher for as long as
 * it runs.
 */
class TrailService : Service() {
    private val scope = MainScope()
    private var logging: Job? = null
    private val book by lazy { TrailBook.of(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } catch (e: SecurityException) {
            Log.w(TAG, "Location access was revoked", e)
            stopSelf()
            return START_NOT_STICKY
        }
        if (logging == null) {
            book.change { it.started() }
            logging = scope.launch { preciseFixes().collect { fix -> book.change { it.logged(fix) } } }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        book.change { it.stopped() }
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Trail logging", NotificationManager.IMPORTANCE_LOW))
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(packageName)
        val stop = Intent(this, TrailService::class.java).setAction(ACTION_STOP)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_trail)
            .setContentTitle("Logging your trail")
            .setContentText("It shows on the world map.")
            .setOngoing(true)
            .setContentIntent(PendingIntent.getActivity(this, 0, home, PendingIntent.FLAG_IMMUTABLE))
            .addAction(0, "Stop", PendingIntent.getService(this, 0, stop, PendingIntent.FLAG_IMMUTABLE))
            .build()
    }
}

// Close enough together for a trail that follows the streets, and no closer, as a fix standing still wanders a few meters.
private const val TRAIL_UPDATE_MILLIS = 2_000L
private const val TRAIL_UPDATE_METERS = 5f

// A fix less sure than this, as under trees or between towers, would put a kink in the trail.
private const val TRAIL_ACCURACY_METERS = 25f

/** Where the user is, as precisely as the phone can tell, as they move, for as long as it is collected; empty without access. */
private fun Context.preciseFixes(): Flow<LatLon> = callbackFlow {
    val manager = getSystemService(LocationManager::class.java)
    val provider = manager.bestProvider()
    if (provider == null) {
        close()
        return@callbackFlow
    }
    val listener = LocationListenerCompat { location: Location ->
        if (!location.hasAccuracy() || location.accuracy <= TRAIL_ACCURACY_METERS) trySend(LatLon(location.latitude, location.longitude))
    }
    val request = LocationRequestCompat.Builder(TRAIL_UPDATE_MILLIS)
        .setMinUpdateDistanceMeters(TRAIL_UPDATE_METERS)
        .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
        .build()
    try {
        LocationManagerCompat.requestLocationUpdates(manager, provider, request, ContextCompat.getMainExecutor(this@preciseFixes), listener)
    } catch (e: SecurityException) {
        Log.w(TAG, "Location access was revoked", e)
        close()
    }
    awaitClose {
        try {
            LocationManagerCompat.removeUpdates(manager, listener)
        } catch (e: SecurityException) {
            Log.w(TAG, "Location access was revoked", e)
        }
    }
}
