package com.hxmimimi.alarmcalendar.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class MotionAwakeDetector(
    context: Context,
    private val onAwake: (Float) -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var score = 0f
    private var activeSince = 0L
    private var awakeSent = false

    fun start() {
        activeSince = System.currentTimeMillis()
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun movementScore(): Float = score

    override fun onSensorChanged(event: SensorEvent) {
        val delta = abs(event.values[0] - lastX) + abs(event.values[1] - lastY) + abs(event.values[2] - lastZ)
        lastX = event.values[0]
        lastY = event.values[1]
        lastZ = event.values[2]
        if (delta > 2.2f) score += delta
        val enoughContinuousTime = System.currentTimeMillis() - activeSince > 20_000
        if (!awakeSent && enoughContinuousTime && score > 120f) {
            awakeSent = true
            onAwake(score)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
