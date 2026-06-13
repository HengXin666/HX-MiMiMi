package com.hxmimimi.alarmcalendar.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class EventDraftTest {
    private val fallbackDate = LocalDate.of(2026, 6, 13)

    @Test
    fun invalidDateFallsBackToProvidedDate() {
        val event = EventDraft(dateText = "bad-date").toEvent(fallbackDate)

        assertEquals(fallbackDate, event.date)
    }

    @Test
    fun allDayEventsHaveNoStartOrEndMinute() {
        val event = EventDraft(allDay = true, startText = "08:30", endText = "09:30").toEvent(fallbackDate)

        assertNull(event.startMinute)
        assertNull(event.endMinute)
    }

    @Test
    fun timedEventsParseClockText() {
        val event = EventDraft(allDay = false, startText = "08:30", endText = "09:45").toEvent(fallbackDate)

        assertEquals(510, event.startMinute)
        assertEquals(585, event.endMinute)
    }

    @Test
    fun invalidTimedInputUsesWorkdayDefaults() {
        val event = EventDraft(allDay = false, startText = "999", endText = "bad").toEvent(fallbackDate)

        assertEquals(9 * 60, event.startMinute)
        assertEquals(10 * 60, event.endMinute)
    }

    @Test
    fun endMinuteIsMovedAfterStartWhenNeeded() {
        val event = EventDraft(allDay = false, startText = "22:30", endText = "21:00").toEvent(fallbackDate)

        assertEquals(22 * 60 + 30, event.startMinute)
        assertEquals(23 * 60 + 30, event.endMinute)
    }

    @Test
    fun monthlyRepeatDefaultsToSelectedDayOfMonth() {
        val event = EventDraft(
            dateText = "2026-06-21",
            repeatKind = EventRepeatKind.MONTHLY_DAY,
            repeatValueText = "",
        ).toEvent(fallbackDate)

        assertEquals(21, event.repeatValue)
    }

    @Test
    fun everyNDaysRepeatClampsToAtLeastOne() {
        val event = EventDraft(
            repeatKind = EventRepeatKind.EVERY_N_DAYS,
            repeatValueText = "0",
        ).toEvent(fallbackDate)

        assertEquals(1, event.repeatValue)
    }

    @Test
    fun fromEventPreservesEditableFieldsAndIdentity() {
        val original = CalendarEvent(
            id = 37,
            title = "纪念日",
            description = "两周年",
            kind = EventKind.INTERVAL,
            date = LocalDate.of(2026, 8, 9),
            allDay = false,
            startMinute = 8 * 60 + 15,
            endMinute = 11 * 60 + 45,
            showInNotification = false,
            showOnLockScreen = false,
            remindDaysBefore = 3,
            repeatKind = EventRepeatKind.EVERY_N_DAYS,
            repeatValue = 14,
            completed = true,
        )

        val roundTrip = EventDraft.fromEvent(original).toEvent(fallbackDate)

        assertEquals(original, roundTrip)
    }

    @Test
    fun fromAllDayMonthlyEventKeepsRepeatDayAndClearsClockText() {
        val original = CalendarEvent(
            id = 8,
            title = "缴费",
            kind = EventKind.DAY_NOTE,
            date = LocalDate.of(2026, 2, 28),
            allDay = true,
            startMinute = null,
            endMinute = null,
            remindDaysBefore = 2,
            repeatKind = EventRepeatKind.MONTHLY_DAY,
            repeatValue = 31,
        )

        val draft = EventDraft.fromEvent(original)
        val roundTrip = draft.toEvent(fallbackDate)

        assertEquals("31", draft.repeatValueText)
        assertEquals(original, roundTrip)
    }
}
