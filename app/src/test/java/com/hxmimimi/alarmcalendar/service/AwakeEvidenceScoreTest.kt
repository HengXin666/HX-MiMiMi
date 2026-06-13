package com.hxmimimi.alarmcalendar.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AwakeEvidenceScoreTest {
    @Test
    fun firstLocationSampleDoesNotCountAsMovement() {
        val score = AwakeEvidenceScore(locationMetersPerPoint = 1f)

        val reading = score.recordLocation(latitude = 31.2304, longitude = 121.4737, nowMillis = 1_000L)

        assertEquals(0f, reading.score, 0.001f)
        assertFalse(reading.isAwake)
    }

    @Test
    fun ignoresTinyLocationDrift() {
        val score = AwakeEvidenceScore(locationNoiseMeters = 8f, locationMetersPerPoint = 1f)
        score.recordLocation(latitude = 31.2304, longitude = 121.4737, nowMillis = 1_000L)

        val reading = score.recordLocation(latitude = 31.23041, longitude = 121.47371, nowMillis = 2_000L)

        assertEquals(0f, reading.score, 0.001f)
        assertFalse(reading.isAwake)
    }

    @Test
    fun locationMovementAddsToScore() {
        val score = AwakeEvidenceScore(locationNoiseMeters = 5f, locationMetersPerPoint = 1f)
        score.recordLocation(latitude = 31.2304, longitude = 121.4737, nowMillis = 1_000L)

        val reading = score.recordLocation(latitude = 31.2307, longitude = 121.4737, nowMillis = 2_000L)

        assertTrue(reading.score > 25f)
        assertFalse(reading.isAwake)
    }

    @Test
    fun accelerometerAndLocationCombineForAwakeDecision() {
        val score = AwakeEvidenceScore(
            accelerometerDeltaThreshold = 1f,
            awakeScoreThreshold = 20f,
            requiredContinuousMillis = 5_000L,
            locationNoiseMeters = 5f,
            locationMetersPerPoint = 1f,
        )
        score.recordAcceleration(0f, 0f, 9.8f, nowMillis = 1_000L)
        score.recordLocation(latitude = 31.2304, longitude = 121.4737, nowMillis = 1_000L)
        score.recordAcceleration(4f, 0f, 5.8f, nowMillis = 2_000L)
        score.recordLocation(latitude = 31.2306, longitude = 121.4737, nowMillis = 6_000L)

        val reading = score.recordAcceleration(4f, 6f, 5.8f, nowMillis = 7_000L)

        assertTrue(reading.score >= 20f)
        assertTrue(reading.isAwake)
    }

    @Test
    fun idleTimeBeforeMovementDoesNotCountAsContinuousAwakeEvidence() {
        val score = AwakeEvidenceScore(
            accelerometerDeltaThreshold = 1f,
            awakeScoreThreshold = 10f,
            requiredContinuousMillis = 5_000L,
        )
        score.recordAcceleration(0f, 0f, 9.8f, nowMillis = 1_000L)

        val reading = score.recordAcceleration(10f, 0f, -0.2f, nowMillis = 7_000L)

        assertEquals(20f, reading.score, 0.001f)
        assertFalse(reading.isAwake)
    }

    @Test
    fun continuousAwakeEvidenceStartsWithFirstRealMovement() {
        val score = AwakeEvidenceScore(
            accelerometerDeltaThreshold = 1f,
            awakeScoreThreshold = 10f,
            requiredContinuousMillis = 5_000L,
        )
        score.recordAcceleration(0f, 0f, 9.8f, nowMillis = 1_000L)
        score.recordAcceleration(10f, 0f, -0.2f, nowMillis = 7_000L)

        val reading = score.recordAcceleration(10f, 6f, -0.2f, nowMillis = 12_000L)

        assertTrue(reading.isAwake)
    }
}
