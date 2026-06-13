package com.hxmimimi.alarmcalendar.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmRingStatusTextTest {
    @Test
    fun formatsProgressAsClampedPercentage() {
        assertEquals(0, AlarmRingStatusText.progressPercent(score = -5f, threshold = 80f))
        assertEquals(50, AlarmRingStatusText.progressPercent(score = 40f, threshold = 80f))
        assertEquals(100, AlarmRingStatusText.progressPercent(score = 120f, threshold = 80f))
    }

    @Test
    fun handlesZeroThresholdAsCompletedProgress() {
        assertEquals(100, AlarmRingStatusText.progressPercent(score = 0f, threshold = 0f))
    }

    @Test
    fun statusTextChangesWhenThresholdIsReached() {
        assertEquals(
            "继续走动，达到关闭条件后会自动停止",
            AlarmRingStatusText.status(score = 79f, threshold = 80f),
        )
        assertEquals(
            "运动证据已达标，保持活动等待确认",
            AlarmRingStatusText.status(score = 80f, threshold = 80f),
        )
    }
}
