package com.hxmimimi.alarmcalendar.service

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat

class MotionAwakeDetector(
    context: Context,
    awakeScoreThreshold: Float = MotionAwakeScore.DEFAULT_AWAKE_SCORE_THRESHOLD,
    requiredContinuousMillis: Long = MotionAwakeScore.DEFAULT_REQUIRED_CONTINUOUS_MILLIS,
    private val onReading: (MotionAwakeReading) -> Unit = {},
    private val onAwake: (Float) -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val locationManager = context.getSystemService(LocationManager::class.java)
    private val hasFineLocationPermission =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    private val hasCoarseLocationPermission =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    private val score = AwakeEvidenceScore(
        awakeScoreThreshold = awakeScoreThreshold,
        requiredContinuousMillis = requiredContinuousMillis,
    )
    private val locationListener = LocationListener { location -> recordLocation(location) }

    fun start() {
        score.reset(System.currentTimeMillis())
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        registerLocationListener()
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        if (hasFineLocationPermission || hasCoarseLocationPermission) {
            runCatching { locationManager.removeUpdates(locationListener) }
        }
    }

    fun movementScore(): Float = score.score()

    override fun onSensorChanged(event: SensorEvent) {
        val reading = score.recordAcceleration(event.values[0], event.values[1], event.values[2], System.currentTimeMillis())
        onReading(reading)
        if (reading.isAwake) onAwake(reading.score)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun registerLocationListener() {
        if (!hasFineLocationPermission && !hasCoarseLocationPermission) return
        val provider = runCatching {
            when {
                hasFineLocationPermission && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
        }.getOrNull() ?: return
        runCatching {
            locationManager.requestLocationUpdates(provider, LOCATION_MIN_TIME_MILLIS, LOCATION_MIN_DISTANCE_METERS, locationListener)
        }
    }

    private fun recordLocation(location: Location) {
        val reading = score.recordLocation(location.latitude, location.longitude, System.currentTimeMillis())
        onReading(reading)
        if (reading.isAwake) onAwake(reading.score)
    }

    companion object {
        private const val LOCATION_MIN_TIME_MILLIS = 5_000L
        private const val LOCATION_MIN_DISTANCE_METERS = 5f
    }
}
