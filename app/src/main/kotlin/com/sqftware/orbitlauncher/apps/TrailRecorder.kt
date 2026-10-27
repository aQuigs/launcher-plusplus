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
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationRequestCompat
import com.sqftware.orbitlauncher.R
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.Trails
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface TrailRecorder {
    /** The trails the user has logged, the one being logged last, growing as they move. */
    val trails: StateFlow<Trails>

    /**
     * Logs a new trail of where the user goes, with the screen off and the launcher out of sight, until [stop]ped. Only
     * with precise location granted, and only while the launcher is in sight, as Android starts a location service then.
     */
    fun start()

    fun stop()
}

private const val TAG = "TrailRecorder"
private const val NOTIFICATIONS_KEY = "trail_notifications"

/**
 * Logs trails in [TrailService], which says it is logging in a notification, so the first start asks to post one
 * through the activity's [registry].
 */
class ServiceTrailRecorder(private val context: Context, registry: ActivityResultRegistry) : TrailRecorder {
    private val notifications = registry.register(NOTIFICATIONS_KEY, RequestPermission()) {}

    override val trails = TrailBook.of(context).trails

    override fun start() {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        ContextCompat.startForegroundService(context, Intent(context, TrailService::class.java))
        val asked = Manifest.permission.POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.checkSelfPermission(asked) != PackageManager.PERMISSION_GRANTED) {
            notifications.launch(asked)
        }
    }

    override fun stop() {
        context.stopService(Intent(context, TrailService::class.java))
    }
}

// Often enough that a run's last stretch is kept if the launcher is ended mid-run, without rewriting it every fix.
private const val SAVE_EVERY_FIXES = 10

/** The trails, kept for the one process the service that logs them and the screen that shows them share, on its main thread. */
internal class TrailBook private constructor(private val store: TrailStore) {
    private val state = MutableStateFlow(store.load())
    val trails = state.asStateFlow()

    fun change(how: (Trails) -> Trails) {
        val before = state.value
        val after = how(before)
        state.value = after
        if (after.finished != before.finished) store.saveFinished(after.finished)
        val growing = after.growing
        if (growing == null || growing.size % SAVE_EVERY_FIXES == 0) store.saveGrowing(growing)
    }

    companion object {
        private var shared: TrailBook? = null

        fun of(context: Context) = shared ?: TrailBook(SharedPreferencesTrailStore(context.applicationContext)).also { shared = it }
    }
}

private const val CHANNEL_ID = "trail"
private const val NOTIFICATION_ID = 1
private const val ACTION_STOP = "com.sqftware.orbitlauncher.STOP_TRAIL"

/**
 * Logs a trail as a foreground service, whose notification says so and stops it. Android lends it the location access
 * the user gave the launcher for as long as it runs.
 */
class TrailService : Service() {
    private val scope = MainScope()
    private var fixes: Job? = null
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
        if (fixes == null) {
            book.change { it.started() }
            // The fixes end only when there is no way to have one, as with location off; a trail then has nothing to log.
            fixes = scope.launch {
                preciseFixes().collect { fix -> book.change { it.logged(fix) } }
                stopSelf()
            }
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
            .setContentText("Stop it here or from the world map.")
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

/**
 * Where the user is, as precisely as the phone can tell, as they move, for as long as it is collected: from the fused
 * provider on Android 12 and up, which at high accuracy leads with GPS, and before it from GPS itself, as the network
 * provider's fixes are too rough for a trail. Empty without one.
 */
private fun Context.preciseFixes(): Flow<LatLon> = flow {
    val manager = getSystemService(LocationManager::class.java)
    val precise = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) LocationManager.FUSED_PROVIDER else LocationManager.GPS_PROVIDER
    val provider = manager.firstEnabled(listOf(precise)) ?: return@flow
    val request = LocationRequestCompat.Builder(TRAIL_UPDATE_MILLIS)
        .setMinUpdateDistanceMeters(TRAIL_UPDATE_METERS)
        .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
        .build()
    emitAll(
        manager.updates(provider, request)
            .filter { !it.hasAccuracy() || it.accuracy <= TRAIL_ACCURACY_METERS }
            .map { LatLon(it.latitude, it.longitude) },
    )
}
