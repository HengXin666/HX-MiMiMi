package com.hxmimimi.alarmcalendar.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionAwakeScoreTest {
    @Test
    fun firstSensorSampleDoesNotCountAsMovement() {
        val score = MotionAwakeScore()

        val reading = score.record(0f, 0f, 9.8f, nowMillis = 1_000L)

        assertEquals(0f, reading.score, 0.001f)
        assertFalse(reading.isAwake)
    }

    @Test
    fun ignoresSmallMotionNoise() {
        val score = MotionAwakeScore(movementDeltaThreshold = 2.2f)
        score.record(0f, 0f, 9.8f, nowMillis = 1_000L)

        val reading = score.record(0.2f, 0.3f, 9.9f, nowMillis = 2_000L)

        assertEquals(0f, reading.score, 0.001f)
        assertFalse(reading.isAwake)
    }

    @Test
    fun accumulatesDeliberateMotion() {
        val score = MotionAwakeScore(movementDeltaThreshold = 2.2f)
        score.record(0f, 0f, 9.8f, nowMillis = 1_000L)

        val reading = score.record(4f, 1f, 6.8f, nowMillis = 2_000L)

        assertEquals(8f, reading.score, 0.001f)
        assertFalse(reading.isAwake)
    }

    @Test
    fun requiresEnoughScoreAndContinuousTimeBeforeAwake() {
        val score = MotionAwakeScore(
            movementDeltaThreshold = 1f,
            awakeScoreThreshold = 10f,
            requiredContinuousMillis = 20_000L,
        )
        score.record(0f, 0f, 9.8f, nowMillis = 1_000L)

        val enoughScoreTooSoon = score.record(5f, 0f, 4.8f, nowMillis = 5_000L)
        score.record(10f, 0f, -0.2f, nowMillis = 12_000L)
        score.record(10f, 5f, -0.2f, nowMillis = 19_000L)
        val enoughScoreAndTime = score.record(15f, 5f, -0.2f, nowMillis = 25_000L)

        assertFalse(enoughScoreTooSoon.isAwake)
        assertTrue(enoughScoreAndTime.isAwake)
    }

    @Test
    fun awakeThresholdAndContinuousTimeAreConfigurable() {
        val score = MotionAwakeScore(
            movementDeltaThreshold = 1f,
            awakeScoreThreshold = 4f,
            requiredContinuousMillis = 5_000L,
        )
        score.record(0f, 0f, 9.8f, nowMillis = 1_000L)
        score.record(3f, 0f, 6.8f, nowMillis = 6_000L)

        val reading = score.record(6f, 0f, 6.8f, nowMillis = 11_000L)

        assertTrue(reading.isAwake)
    }
}
