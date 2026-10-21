package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.sqftware.orbitlauncher.domain.wrapDegrees
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/** Which way the phone faces. */
interface HeadingSource {
    /**
     * The phone's heading in degrees clockwise from north, as it turns, for as long as it is collected. Collect it only
     * while something shows it. Without a compass it ends at once, empty.
     */
    fun headings(): Flow<Float>
}

/**
 * The rotation vector sensor, which fuses the magnetometer with the gyroscope, so the heading turns smoothly. The
 * launcher is held in portrait, so the top of the screen is the way the phone faces.
 */
class SensorHeadingSource(context: Context) : HeadingSource {
    private val manager = context.getSystemService(SensorManager::class.java)

    override fun headings(): Flow<Float> = callbackFlow {
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                trySend(wrapDegrees(Math.toDegrees(orientation[0].toDouble()).toFloat()))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { manager.unregisterListener(listener) }
    }.conflate()
}
