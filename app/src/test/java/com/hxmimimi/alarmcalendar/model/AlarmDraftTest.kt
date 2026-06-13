package com.hxmimimi.alarmcalendar.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class AlarmDraftTest {
    @Test
    fun invalidTimeFallsBackToMorningDefault() {
        val alarm = AlarmDraft(hourText = "99", minuteText = "bad").toAlarm()

        assertEquals(7, alarm.hour)
        assertEquals(30, alarm.minute)
    }

    @Test
    fun parsesAndClampsTime() {
        val alarm = AlarmDraft(hourText = "23", minuteText = "59").toAlarm()

        assertEquals(23, alarm.hour)
        assertEquals(59, alarm.minute)
    }

    @Test
    fun weeklyRepeatDefaultsToEveryDay() {
        val alarm = AlarmDraft(repeatKind = AlarmRepeatKind.WEEKLY).toAlarm()

        assertEquals(DayOfWeek.entries.toSet(), alarm.repeatDays)
    }

    @Test
    fun weeklyRepeatUsesSelectedDays() {
        val alarm = AlarmDraft(
            repeatKind = AlarmRepeatKind.WEEKLY,
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        ).toAlarm()

        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), alarm.repeatDays)
    }

    @Test
    fun emptyWeeklySelectionFallsBackToEveryDay() {
        val alarm = AlarmDraft(
            repeatKind = AlarmRepeatKind.WEEKLY,
            repeatDays = emptySet(),
        ).toAlarm()

        assertEquals(DayOfWeek.entries.toSet(), alarm.repeatDays)
    }

    @Test
    fun nonWeeklyRepeatHasNoWeekdaySet() {
        val alarm = AlarmDraft(repeatKind = AlarmRepeatKind.CHINESE_WORKDAY).toAlarm()

        assertTrue(alarm.repeatDays.isEmpty())
    }

    @Test
    fun guardAndMovementThresholdsAreClamped() {
        val alarm = AlarmDraft(
            snoozeGuardText = "0",
            dismissScoreText = "1",
            awakeScoreText = "999",
            confirmSecondsText = "1",
        ).toAlarm()

        assertEquals(1, alarm.snoozeGuardMinutes)
        assertEquals(20f, alarm.dismissMovementScore, 0.001f)
        assertEquals(500f, alarm.awakeMovementScore, 0.001f)
        assertEquals(5, alarm.awakeConfirmSeconds)
    }

    @Test
    fun blankTitleAndWelcomeUseDefaults() {
        val alarm = AlarmDraft(title = "", welcomeMessage = "").toAlarm()

        assertEquals("闹钟", alarm.title)
        assertEquals("起床先喝杯水吧", alarm.welcomeMessage)
    }

    @Test
    fun fromAlarmPreservesEditableFieldsAndIdentity() {
        val original = Alarm(
            id = 42,
            title = "工作日起床",
            hour = 6,
            minute = 45,
            enabled = false,
            repeatKind = AlarmRepeatKind.WEEKLY,
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            ringtoneUri = "content://tone",
            snoozeGuardMinutes = 6,
            dismissMovementScore = 90f,
            awakeMovementScore = 140f,
            awakeConfirmSeconds = 25,
            welcomeMessage = "喝水",
            nextAt = LocalDateTime.of(2026, 6, 14, 6, 45),
        )

        val roundTrip = AlarmDraft.fromAlarm(original).toAlarm()

        assertEquals(original.copy(nextAt = null), roundTrip)
    }
}
