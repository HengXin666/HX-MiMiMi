package com.hxmimimi.alarmcalendar.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EventRecurrenceCalculatorTest {
    private val calculator = EventRecurrenceCalculator()

    @Test
    fun nonRepeatingEventDoesNotOccurAfterItsDate() {
        val event = event(date = LocalDate.of(2026, 6, 1), repeatKind = EventRepeatKind.NONE)

        assertEquals(null, calculator.nextOccurrence(event, LocalDate.of(2026, 6, 2)))
    }

    @Test
    fun weeklyEventFindsNextWeekWhenStartDatePassed() {
        val event = event(date = LocalDate.of(2026, 6, 1), repeatKind = EventRepeatKind.WEEKLY)

        assertEquals(LocalDate.of(2026, 6, 8), calculator.nextOccurrence(event, LocalDate.of(2026, 6, 2)))
    }

    @Test
    fun everyNDaysSkipsToFirstOccurrenceOnOrAfterFromDate() {
        val event = event(
            date = LocalDate.of(2026, 6, 1),
            repeatKind = EventRepeatKind.EVERY_N_DAYS,
            repeatValue = 3,
        )

        assertEquals(LocalDate.of(2026, 6, 7), calculator.nextOccurrence(event, LocalDate.of(2026, 6, 6)))
    }

    @Test
    fun monthlyDayClampsShortMonths() {
        val event = event(
            date = LocalDate.of(2026, 1, 31),
            repeatKind = EventRepeatKind.MONTHLY_DAY,
            repeatValue = 31,
        )

        assertEquals(LocalDate.of(2026, 2, 28), calculator.nextOccurrence(event, LocalDate.of(2026, 2, 1)))
    }

    @Test
    fun reminderTimesUseStartMinuteForTimedEvents() {
        val event = event(
            date = LocalDate.of(2026, 6, 20),
            allDay = false,
            startMinute = 8 * 60 + 30,
            remindDaysBefore = 1,
        )

        assertEquals(
            listOf(
                LocalDateTime.of(2026, 6, 19, 8, 30),
                LocalDateTime.of(2026, 6, 20, 8, 15),
                LocalDateTime.of(2026, 6, 20, 8, 28),
            ),
            calculator.reminderTimes(event, LocalDateTime.of(2026, 6, 18, 12, 0)),
        )
    }

    @Test
    fun reminderTimesFilterPastInstants() {
        val event = event(date = LocalDate.of(2026, 6, 20), remindDaysBefore = 1)

        val reminders = calculator.reminderTimes(event, LocalDateTime.of(2026, 6, 20, 8, 50))

        assertEquals(listOf(LocalDateTime.of(2026, 6, 20, 8, 58)), reminders)
        assertTrue(reminders.all { it.isAfter(LocalDateTime.of(2026, 6, 20, 8, 50)) })
    }

    @Test
    fun nextRescheduleTimeIsEmptyBeforeLastReminderOfOccurrence() {
        val event = event(
            date = LocalDate.of(2026, 6, 20),
            repeatKind = EventRepeatKind.WEEKLY,
            remindDaysBefore = 1,
        )

        assertEquals(
            null,
            calculator.nextRescheduleFromTrigger(event, LocalDateTime.of(2026, 6, 19, 9, 0)),
        )
    }

    @Test
    fun nextRescheduleTimeReturnsFirstReminderOfNextWeeklyOccurrenceAfterLastReminder() {
        val event = event(
            date = LocalDate.of(2026, 6, 20),
            repeatKind = EventRepeatKind.WEEKLY,
            remindDaysBefore = 1,
        )

        assertEquals(
            LocalDateTime.of(2026, 6, 26, 9, 0),
            calculator.nextRescheduleFromTrigger(event, LocalDateTime.of(2026, 6, 20, 8, 58)),
        )
    }

    @Test
    fun nextRescheduleTimeContinuesWithinNextOccurrenceAfterItsFirstReminder() {
        val event = event(
            date = LocalDate.of(2026, 6, 20),
            repeatKind = EventRepeatKind.WEEKLY,
            remindDaysBefore = 1,
        )

        assertEquals(
            null,
            calculator.nextRescheduleFromTrigger(event, LocalDateTime.of(2026, 6, 26, 9, 0)),
        )
    }

    @Test
    fun nextRescheduleTimeIsEmptyForOneShotEvents() {
        val event = event(date = LocalDate.of(2026, 6, 20), repeatKind = EventRepeatKind.NONE)

        assertEquals(
            null,
            calculator.nextRescheduleFromTrigger(event, LocalDateTime.of(2026, 6, 20, 8, 58)),
        )
    }

    private fun event(
        date: LocalDate,
        repeatKind: EventRepeatKind,
        repeatValue: Int = 0,
        allDay: Boolean = true,
        startMinute: Int? = null,
        remindDaysBefore: Int = 0,
    ): CalendarEvent = CalendarEvent(
        title = "事项",
        kind = EventKind.DAY_NOTE,
        date = date,
        repeatKind = repeatKind,
        repeatValue = repeatValue,
        allDay = allDay,
        startMinute = startMinute,
        remindDaysBefore = remindDaysBefore,
    )

    private fun event(
        date: LocalDate,
        allDay: Boolean = true,
        startMinute: Int? = null,
        remindDaysBefore: Int = 0,
    ): CalendarEvent = event(
        date = date,
        repeatKind = EventRepeatKind.NONE,
        allDay = allDay,
        startMinute = startMinute,
        remindDaysBefore = remindDaysBefore,
    )
}
