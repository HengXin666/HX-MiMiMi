package com.hxmimimi.alarmcalendar.model

object AlarmRingStatusText {
    fun progressPercent(score: Float, threshold: Float): Int {
        if (threshold <= 0f) return 100
        return ((score.coerceAtLeast(0f) / threshold) * 100f).toInt().coerceIn(0, 100)
    }

    fun status(score: Float, threshold: Float): String =
        if (score >= threshold) {
            "运动证据已达标，保持活动等待确认"
        } else {
            "继续走动，达到关闭条件后会自动停止"
        }
}
