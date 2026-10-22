package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.view.Surface
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/** Which way the phone faces. */
interface HeadingSource {
    /**
     * The phone's heading in degrees clockwise from true north, as it turns, for as long as it is collected. Collect it
     * only while something shows it. Without a compass it ends at once, empty.
     */
    fun headings(): Flow<Float>
}

/**
 * The rotation vector sensor, which fuses the magnetometer with the gyroscope, so the heading turns smoothly. The heading
 * is the way the top of the screen faces, in whichever way the display is turned, and is moved from magnetic north to
 * the true north a map is drawn to by the declination where the phone last had a fix, when [location] may know it.
 */
class SensorHeadingSource(private val context: Context, private val location: LocationSource) : HeadingSource {
    private val sensors = context.getSystemService(SensorManager::class.java)
    private val locations = context.getSystemService(LocationManager::class.java)

    override fun headings(): Flow<Float> = callbackFlow {
        val sensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        val declination = declination()
        val rotation = FloatArray(9)
        val remapped = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                val (x, y) = when (context.display.rotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                SensorManager.remapCoordinateSystem(rotation, x, y, remapped)
                SensorManager.getOrientation(remapped, orientation)
                trySend((Math.toDegrees(orientation[0].toDouble()).toFloat() + declination).mod(360f))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensors.unregisterListener(listener) }
    }.conflate()

    /** How far true north is clockwise of magnetic north where the phone last had a fix, or none without one. */
    private fun declination(): Float {
        if (!location.isGranted()) return 0f
        val fix = try {
            locations.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } catch (_: SecurityException) {
            null
        } ?: return 0f
        return GeomagneticField(fix.latitude.toFloat(), fix.longitude.toFloat(), fix.altitude.toFloat(), System.currentTimeMillis()).declination
    }
}
