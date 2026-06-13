package com.hxmimimi.alarmcalendar.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EventDisplayTextTest {
    @Test
    fun labelsEventKinds() {
        assertEquals("倒数日", EventDisplayText.kindLabel(EventKind.COUNTDOWN))
        assertEquals("事项日", EventDisplayText.kindLabel(EventKind.DAY_NOTE))
        assertEquals("间隔日", EventDisplayText.kindLabel(EventKind.INTERVAL))
    }

    @Test
    fun labelsRepeatRules() {
        assertEquals("不重复", EventDisplayText.repeatLabel(EventRepeatKind.NONE, 0))
        assertEquals("每星期", EventDisplayText.repeatLabel(EventRepeatKind.WEEKLY, 0))
        assertEquals("每月 21 号", EventDisplayText.repeatLabel(EventRepeatKind.MONTHLY_DAY, 21))
        assertEquals("每 3 天", EventDisplayText.repeatLabel(EventRepeatKind.EVERY_N_DAYS, 3))
    }

    @Test
    fun clampsRepeatLabelsForBadValues() {
        assertEquals("每月 1 号", EventDisplayText.repeatLabel(EventRepeatKind.MONTHLY_DAY, 0))
        assertEquals("每 1 天", EventDisplayText.repeatLabel(EventRepeatKind.EVERY_N_DAYS, 0))
    }

    @Test
    fun formatsCountdownDays() {
        val event = CalendarEvent(
            title = "旅行",
            kind = EventKind.COUNTDOWN,
            date = LocalDate.of(2026, 6, 20),
        )

        assertEquals("还有 7 天", EventDisplayText.countdownText(event, LocalDate.of(2026, 6, 13)))
    }
}
