package com.sqftware.orbitlauncher.apps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.StepTally
import com.sqftware.orbitlauncher.domain.Steps
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.LocalDate

interface StepCounter {
    /** Whether the phone counts steps at all. */
    val available: Boolean

    /** Whether the user lets the launcher count their steps. */
    fun isGranted(): Boolean

    /** Asks the user for it, and starts counting if they agree. */
    fun request()

    /** Whether the user lets it, now and after each answer to a [request]. */
    fun granted(): Flow<Boolean>

    /** Each answer to a [request]: whether the user granted it. */
    fun answers(): Flow<Boolean>

    /** Steps taken today, now and as they mount, for as long as it is collected; nothing without the grant. */
    fun today(): Flow<Int>

    /** Opens the user's fitness app, if they have one. */
    fun openFitness()
}

private const val STEPS_TAG = "SensorStepCounter"
private const val REQUEST_KEY = "steps"
private const val PREFS = "steps"

// Batched, since nothing shows a step the moment it is taken: the counter wakes the launcher at most once a minute.
private const val BATCH_MICROS = 60_000_000

/**
 * The phone's step counter, which counts from each boot, and only while some app listens: so the launcher listens for as
 * long as it runs, from [start] until [close], not only while it is in sight, and keeps its tally of the day in its own
 * preferences. The permission is asked through the activity's [registry]; the adapter lives as long as the activity.
 */
class SensorStepCounter(private val context: Context, registry: ActivityResultRegistry) : StepCounter, AutoCloseable {
    private val sensors = context.getSystemService(SensorManager::class.java)
    private val sensor = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val tally = MutableStateFlow(load())
    private val allowed = MutableStateFlow(isGranted())

    // Kept until heard, since an answer can arrive as a recreated activity registers, before the screen collects.
    private val answered = Channel<Boolean>(Channel.CONFLATED)
    private val dialog = registry.register(REQUEST_KEY, RequestPermission()) { granted ->
        allowed.value = granted
        answered.trySend(granted)
        if (granted) start()
    }
    private var listening = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val counter = event.values[0].toLong()
            val today = LocalDate.now()
            val read = tally.value?.read(counter, today) ?: StepTally.first(counter, today)
            tally.value = read
            save(read)
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }

    override val available: Boolean get() = sensor != null

    override fun isGranted() =
        context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    override fun request() = dialog.launch(Manifest.permission.ACTIVITY_RECOGNITION)

    // Read again each time, since the grant can be given or taken away in Settings.
    override fun granted(): Flow<Boolean> = allowed.onStart { allowed.value = isGranted() }

    override fun answers(): Flow<Boolean> = answered.receiveAsFlow()

    /** Starts counting, if the phone can and the user lets it. */
    fun start() {
        if (listening || sensor == null || !isGranted()) return
        listening = sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, BATCH_MICROS)
    }

    // Asks for what the counter has batched, so the launcher coming into sight shows the steps up to now.
    override fun today(): Flow<Int> = tally
        .onStart { if (listening) sensors.flush(listener) }
        .map { it?.takeIf { tally -> tally.day == LocalDate.now() }?.today ?: 0 }
        .distinctUntilChanged()

    override fun openFitness() = startOrLog(STEPS_TAG, "a fitness app") {
        context.startActivity(
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_FITNESS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override fun close() {
        if (listening) sensors.unregisterListener(listener)
        listening = false
    }

    private fun load(): StepTally? {
        if (!prefs.contains("day")) return null
        return StepTally(
            day = LocalDate.ofEpochDay(prefs.getLong("day", 0)),
            start = prefs.getLong("start", 0),
            last = prefs.getLong("last", 0),
            before = prefs.getLong("before", 0),
        )
    }

    private fun save(tally: StepTally) = prefs.edit {
        putLong("day", tally.day.toEpochDay())
        putLong("start", tally.start)
        putLong("last", tally.last)
        putLong("before", tally.before)
    }
}

/** Today's [Steps]: none on a phone without a counter, and none while the user has not let it count. */
@OptIn(ExperimentalCoroutinesApi::class)
fun StepCounter.steps(): Flow<Steps> =
    if (!available) {
        flowOf(Steps.Unavailable)
    } else {
        granted().flatMapLatest { granted -> if (granted) today().map(Steps::Today) else flowOf(Steps.NotAllowed) }
    }
