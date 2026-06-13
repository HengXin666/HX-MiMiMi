package com.hxmimimi.alarmcalendar.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class AlarmDisplayTextTest {
    @Test
    fun labelsRepeatKinds() {
        assertEquals("单次", AlarmDisplayText.repeatLabel(AlarmRepeatKind.ONCE))
        assertEquals("按星期", AlarmDisplayText.repeatLabel(AlarmRepeatKind.WEEKLY))
        assertEquals("中国工作日", AlarmDisplayText.repeatLabel(AlarmRepeatKind.CHINESE_WORKDAY))
        assertEquals("中国节假日", AlarmDisplayText.repeatLabel(AlarmRepeatKind.CHINESE_HOLIDAY))
    }

    @Test
    fun formatsTime() {
        assertEquals("07:05", AlarmDisplayText.timeText(7, 5))
        assertEquals("23:59", AlarmDisplayText.timeText(23, 59))
    }

    @Test
    fun summarizesGuardSettings() {
        val alarm = AlarmDraft(
            snoozeGuardText = "6",
            dismissScoreText = "85",
            awakeScoreText = "130",
            confirmSecondsText = "25",
        ).toAlarm()

        assertEquals("防赖床 6 分钟 · 关闭分 85 · 确认分 130 · 连续 25 秒", AlarmDisplayText.guardSummary(alarm))
    }

    @Test
    fun labelsWeeklyDays() {
        val alarm = AlarmDraft(repeatKind = AlarmRepeatKind.WEEKLY).toAlarm()
            .copy(repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))

        assertEquals("周一、周三、周五", AlarmDisplayText.repeatDaysText(alarm))
    }
}
