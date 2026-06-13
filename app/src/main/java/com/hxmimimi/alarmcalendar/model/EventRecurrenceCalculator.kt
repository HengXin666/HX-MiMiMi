package com.hxmimimi.alarmcalendar.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class EventRecurrenceCalculator {
    fun nextOccurrence(event: CalendarEvent, from: LocalDate): LocalDate? = when (event.repeatKind) {
        EventRepeatKind.NONE -> event.date.takeIf { !it.isBefore(from) }
        EventRepeatKind.WEEKLY -> nextByWholeSteps(event.date, from, stepDays = 7)
        EventRepeatKind.EVERY_N_DAYS -> nextByWholeSteps(event.date, from, stepDays = event.repeatValue.coerceAtLeast(1).toLong())
        EventRepeatKind.MONTHLY_DAY -> nextMonthlyDay(event, from)
    }

    fun reminderTimes(event: CalendarEvent, from: LocalDateTime): List<LocalDateTime> {
        val occurrence = nextOccurrence(event, from.toLocalDate()) ?: return emptyList()
        return reminderTimesForOccurrence(event, occurrence).filter { it.isAfter(from) }
    }

    fun nextRescheduleFromTrigger(event: CalendarEvent, triggeredAt: LocalDateTime): LocalDateTime? {
        if (event.repeatKind == EventRepeatKind.NONE) return null
        val occurrence = occurrenceOnOrBefore(event, triggeredAt.toLocalDate()) ?: return null
        val reminders = reminderTimesForOccurrence(event, occurrence)
        val lastReminder = reminders.lastOrNull() ?: return null
        if (!sameMinute(triggeredAt, lastReminder)) return null
        val nextOccurrence = nextOccurrence(event, occurrence.plusDays(1)) ?: return null
        return reminderTimesForOccurrence(event, nextOccurrence).firstOrNull { it.isAfter(triggeredAt) }
    }

    fun reminderTimesForOccurrence(event: CalendarEvent, occurrence: LocalDate): List<LocalDateTime> {
        val base = LocalDateTime.of(occurrence, event.startTime())
        return listOf(
            base.minusDays(event.remindDaysBefore.toLong()),
            base.minusMinutes(15),
            base.minusMinutes(2),
        ).distinct().sorted()
    }

    private fun nextByWholeSteps(start: LocalDate, from: LocalDate, stepDays: Long): LocalDate {
        if (!start.isBefore(from)) return start
        val elapsedDays = ChronoUnit.DAYS.between(start, from)
        val steps = (elapsedDays + stepDays - 1) / stepDays
        return start.plusDays(steps * stepDays)
    }

    private fun nextMonthlyDay(event: CalendarEvent, from: LocalDate): LocalDate {
        val desiredDay = event.repeatValue.takeIf { it > 0 } ?: event.date.dayOfMonth
        var candidate = event.date.withDayOfMonth(desiredDay.coerceAtMost(event.date.lengthOfMonth()))
        while (candidate.isBefore(from)) {
            val nextMonth = candidate.plusMonths(1)
            candidate = nextMonth.withDayOfMonth(desiredDay.coerceAtMost(nextMonth.lengthOfMonth()))
        }
        return candidate
    }

    private fun occurrenceOnOrBefore(event: CalendarEvent, date: LocalDate): LocalDate? {
        val nextAfterDate = nextOccurrence(event, date.plusDays(1)) ?: return null
        val stepBackDate = when (event.repeatKind) {
            EventRepeatKind.NONE -> event.date
            EventRepeatKind.WEEKLY -> nextAfterDate.minusWeeks(1)
            EventRepeatKind.EVERY_N_DAYS -> nextAfterDate.minusDays(event.repeatValue.coerceAtLeast(1).toLong())
            EventRepeatKind.MONTHLY_DAY -> {
                val desiredDay = event.repeatValue.takeIf { it > 0 } ?: event.date.dayOfMonth
                val previousMonth = nextAfterDate.minusMonths(1)
                previousMonth.withDayOfMonth(desiredDay.coerceAtMost(previousMonth.lengthOfMonth()))
            }
        }
        return stepBackDate.takeIf { !it.isBefore(event.date) && !it.isAfter(date) }
    }

    private fun sameMinute(first: LocalDateTime, second: LocalDateTime): Boolean =
        first.withSecond(0).withNano(0) == second.withSecond(0).withNano(0)

    private fun CalendarEvent.startTime(): LocalTime {
        if (allDay) return LocalTime.of(9, 0)
        val minuteOfDay = startMinute ?: 9 * 60
        return LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    }
}
