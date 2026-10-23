package com.sqftware.orbitlauncher.apps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.StepTally
import com.sqftware.orbitlauncher.domain.Steps
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface StepCounter {
    /** Whether the user lets the launcher count their steps. */
    fun isGranted(): Boolean

    /** Asks the user for it; the answer comes on [answers]. */
    fun request()

    /** Each answer to a [request]: whether the user granted it. */
    fun answers(): Flow<Boolean>

    /** Today's [Steps] as they stand. */
    fun stepsNow(): Steps

    /**
     * Today's [Steps], now and as they change, for as long as it is collected. Collecting it with the grant starts the
     * count, which goes on out of sight until [stop].
     */
    fun steps(): Flow<Steps>

    /** Stops counting. */
    fun stop()

    /** Opens the user's fitness app, if they have one. */
    fun openFitness()
}

private const val STEPS_TAG = "SensorStepCounter"
private const val REQUEST_KEY = "steps"
private const val PREFS = "steps"

// Batched, since nothing shows a step the moment it is taken: the counter wakes the launcher at most once a minute.
private const val BATCH_MICROS = 60_000_000

// A batch brings a reading every few steps; the tally is kept once a batch, not for every reading in it.
private const val SAVE_MILLIS = 30_000L

/**
 * The phone's step counter, which counts from each boot, and only while some app listens: so the launcher listens from
 * the first time its steps are collected with the grant until [stop], in sight or not, and keeps its tally of the day in
 * its own preferences. The permission is asked through the activity's [registry]; the adapter lives as long as the
 * activity.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SensorStepCounter(private val context: Context, registry: ActivityResultRegistry) : StepCounter {
    private val sensors = context.getSystemService(SensorManager::class.java)
    private val sensor = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val boot = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
    private val tally = MutableStateFlow(load())
    private val allowed = MutableStateFlow(isGranted())
    private var listening = false
    private var savedAt = 0L

    // Kept until heard, since an answer can arrive as a recreated activity registers, before the screen collects.
    private val answered = Channel<Boolean>(Channel.CONFLATED)
    private val dialog = registry.register(REQUEST_KEY, RequestPermission()) { granted ->
        allowed.value = granted
        answered.trySend(granted)
    }

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val counter = event.values[0].toLong()
            // Dated by when the step was taken, since a batch can arrive after midnight with the evening's steps in it.
            val takenAt = System.currentTimeMillis() - (SystemClock.elapsedRealtimeNanos() - event.timestamp) / 1_000_000
            val on = Instant.ofEpochMilli(takenAt).atZone(ZoneId.systemDefault()).toLocalDate()
            val read = tally.value?.read(counter, on, boot) ?: StepTally.first(counter, on, boot)
            tally.value = read
            if (SystemClock.elapsedRealtime() - savedAt >= SAVE_MILLIS) save(read)
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }

    override fun isGranted() =
        context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    override fun request() = dialog.launch(Manifest.permission.ACTIVITY_RECOGNITION)

    override fun answers(): Flow<Boolean> = answered.receiveAsFlow()

    override fun stepsNow(): Steps = when {
        sensor == null -> Steps.Unavailable
        !isGranted() -> Steps.NotAllowed
        else -> Steps.Today(todayOf(tally.value))
    }

    // The grant is read again each time, since it can be given or taken away in Settings.
    override fun steps(): Flow<Steps> =
        if (sensor == null) {
            flowOf(Steps.Unavailable)
        } else {
            allowed.onStart { allowed.value = isGranted() }.flatMapLatest { granted -> if (granted) counted() else flowOf(Steps.NotAllowed) }
        }

    // Asks for what the counter has batched, so the launcher coming into sight shows the steps up to now.
    private fun counted(): Flow<Steps> = tally
        .onStart { if (start()) sensors.flush(listener) }
        .map { Steps.Today(todayOf(it)) }
        .distinctUntilChanged()

    /** Starts listening if it is not already, and says whether it is. */
    private fun start(): Boolean {
        if (!listening && sensor != null && isGranted()) {
            listening = sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, BATCH_MICROS)
        }
        return listening
    }

    override fun stop() {
        if (!listening) return
        sensors.unregisterListener(listener)
        listening = false
        tally.value?.let(::save)
    }

    override fun openFitness() = startOrLog(STEPS_TAG, "a fitness app") {
        context.startActivity(
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_FITNESS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun todayOf(tally: StepTally?) = tally?.takeIf { it.day == LocalDate.now() }?.today ?: 0

    private fun load(): StepTally? {
        if (!prefs.contains("day")) return null
        return StepTally(
            day = LocalDate.ofEpochDay(prefs.getLong("day", 0)),
            boot = prefs.getInt("boot", 0),
            start = prefs.getLong("start", 0),
            last = prefs.getLong("last", 0),
            before = prefs.getLong("before", 0),
        )
    }

    private fun save(tally: StepTally) {
        savedAt = SystemClock.elapsedRealtime()
        prefs.edit {
            putLong("day", tally.day.toEpochDay())
            putInt("boot", tally.boot)
            putLong("start", tally.start)
            putLong("last", tally.last)
            putLong("before", tally.before)
        }
    }
}
