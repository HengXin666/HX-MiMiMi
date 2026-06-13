package com.hxmimimi.alarmcalendar.service

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class AwakeEvidenceScore(
    private val accelerometerDeltaThreshold: Float = MotionAwakeScore.DEFAULT_MOVEMENT_DELTA_THRESHOLD,
    private val awakeScoreThreshold: Float = MotionAwakeScore.DEFAULT_AWAKE_SCORE_THRESHOLD,
    private val requiredContinuousMillis: Long = MotionAwakeScore.DEFAULT_REQUIRED_CONTINUOUS_MILLIS,
    private val locationNoiseMeters: Float = DEFAULT_LOCATION_NOISE_METERS,
    private val locationMetersPerPoint: Float = DEFAULT_LOCATION_METERS_PER_POINT,
) {
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var hasAccelerationBaseline = false
    private var lastLatitude = 0.0
    private var lastLongitude = 0.0
    private var hasLocationBaseline = false
    private var activeSince = 0L
    private var lastMovementAt = 0L
    private var score = 0f
    private var awakeSent = false

    fun reset(@Suppress("UNUSED_PARAMETER") nowMillis: Long) {
        lastX = 0f
        lastY = 0f
        lastZ = 0f
        hasAccelerationBaseline = false
        lastLatitude = 0.0
        lastLongitude = 0.0
        hasLocationBaseline = false
        activeSince = 0L
        lastMovementAt = 0L
        score = 0f
        awakeSent = false
    }

    fun recordAcceleration(x: Float, y: Float, z: Float, nowMillis: Long): MotionAwakeReading {
        if (!hasAccelerationBaseline) {
            hasAccelerationBaseline = true
            lastX = x
            lastY = y
            lastZ = z
            return reading(nowMillis)
        }

        val delta = abs(x - lastX) + abs(y - lastY) + abs(z - lastZ)
        lastX = x
        lastY = y
        lastZ = z
        if (delta > accelerometerDeltaThreshold) {
            score += delta
            recordMovement(nowMillis)
        }
        return reading(nowMillis)
    }

    fun recordLocation(latitude: Double, longitude: Double, nowMillis: Long): MotionAwakeReading {
        if (!hasLocationBaseline) {
            hasLocationBaseline = true
            lastLatitude = latitude
            lastLongitude = longitude
            return reading(nowMillis)
        }

        val distanceMeters = haversineMeters(lastLatitude, lastLongitude, latitude, longitude).toFloat()
        lastLatitude = latitude
        lastLongitude = longitude
        if (distanceMeters > locationNoiseMeters) {
            score += distanceMeters / locationMetersPerPoint.coerceAtLeast(0.1f)
            recordMovement(nowMillis)
        }
        return reading(nowMillis)
    }

    fun score(): Float = score

    private fun recordMovement(nowMillis: Long) {
        if (activeSince == 0L || nowMillis - lastMovementAt > MOVEMENT_IDLE_RESET_MILLIS) {
            activeSince = nowMillis
        }
        lastMovementAt = nowMillis
    }

    private fun reading(nowMillis: Long): MotionAwakeReading {
        val enoughContinuousTime = activeSince != 0L && nowMillis - activeSince >= requiredContinuousMillis
        val isAwake = !awakeSent && enoughContinuousTime && score >= awakeScoreThreshold
        if (isAwake) awakeSent = true
        return MotionAwakeReading(score = score, isAwake = isAwake)
    }

    private fun haversineMeters(startLat: Double, startLon: Double, endLat: Double, endLon: Double): Double {
        val startLatRad = Math.toRadians(startLat)
        val endLatRad = Math.toRadians(endLat)
        val deltaLat = Math.toRadians(endLat - startLat)
        val deltaLon = Math.toRadians(endLon - startLon)
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(startLatRad) * cos(endLatRad) * sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    companion object {
        const val DEFAULT_LOCATION_NOISE_METERS = 8f
        const val DEFAULT_LOCATION_METERS_PER_POINT = 1.5f
        private const val MOVEMENT_IDLE_RESET_MILLIS = 10_000L
        private const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
