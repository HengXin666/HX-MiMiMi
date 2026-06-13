package com.hxmimimi.alarmcalendar.model

import java.time.LocalDate

data class EventDraft(
    val id: Long = 0,
    val title: String = "事项",
    val description: String = "",
    val dateText: String = "",
    val remindDaysText: String = "1",
    val repeatValueText: String = "1",
    val startText: String = "09:00",
    val endText: String = "10:00",
    val kind: EventKind = EventKind.DAY_NOTE,
    val repeatKind: EventRepeatKind = EventRepeatKind.NONE,
    val allDay: Boolean = true,
    val showNotification: Boolean = true,
    val showLockScreen: Boolean = true,
    val completed: Boolean = false,
) {
    fun toEvent(fallbackDate: LocalDate): CalendarEvent {
        val date = runCatching { LocalDate.parse(dateText) }.getOrDefault(fallbackDate)
        val start = if (allDay) null else parseClockMinute(startText) ?: DEFAULT_START_MINUTE
        val end = if (allDay) null else normalizedEndMinute(start ?: DEFAULT_START_MINUTE, parseClockMinute(endText))
        return CalendarEvent(
            id = id,
            title = title.ifBlank { "事项" },
            description = description,
            kind = kind,
            date = date,
            allDay = allDay,
            startMinute = start,
            endMinute = end,
            showInNotification = showNotification,
            showOnLockScreen = showLockScreen,
            remindDaysBefore = remindDaysText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            repeatKind = repeatKind,
            repeatValue = repeatValue(date),
            completed = completed,
        )
    }

    private fun repeatValue(date: LocalDate): Int = when (repeatKind) {
        EventRepeatKind.NONE -> 0
        EventRepeatKind.WEEKLY -> 0
        EventRepeatKind.MONTHLY_DAY -> repeatValueText.toIntOrNull()?.coerceIn(1, 31) ?: date.dayOfMonth
        EventRepeatKind.EVERY_N_DAYS -> repeatValueText.toIntOrNull()?.coerceAtLeast(1) ?: 1
    }

    private fun normalizedEndMinute(startMinute: Int, parsedEndMinute: Int?): Int {
        val desiredEnd = parsedEndMinute ?: DEFAULT_END_MINUTE
        return if (desiredEnd > startMinute) desiredEnd else (startMinute + 60).coerceAtMost(LAST_MINUTE_OF_DAY)
    }

    companion object {
        const val DEFAULT_START_MINUTE = 9 * 60
        const val DEFAULT_END_MINUTE = 10 * 60
        private const val LAST_MINUTE_OF_DAY = 23 * 60 + 59

        fun parseClockMinute(text: String): Int? {
            val parts = text.trim().split(":")
            if (parts.size != 2) return null
            val hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null
            if (hour !in 0..23 || minute !in 0..59) return null
            return hour * 60 + minute
        }

        fun fromEvent(event: CalendarEvent): EventDraft = EventDraft(
            id = event.id,
            title = event.title,
            description = event.description,
            dateText = event.date.toString(),
            remindDaysText = event.remindDaysBefore.toString(),
            repeatValueText = when (event.repeatKind) {
                EventRepeatKind.NONE,
                EventRepeatKind.WEEKLY,
                -> "1"
                EventRepeatKind.MONTHLY_DAY,
                EventRepeatKind.EVERY_N_DAYS,
                -> event.repeatValue.coerceAtLeast(1).toString()
            },
            startText = formatClockMinute(event.startMinute ?: DEFAULT_START_MINUTE),
            endText = formatClockMinute(event.endMinute ?: DEFAULT_END_MINUTE),
            kind = event.kind,
            repeatKind = event.repeatKind,
            allDay = event.allDay,
            showNotification = event.showInNotification,
            showLockScreen = event.showOnLockScreen,
            completed = event.completed,
        )

        private fun formatClockMinute(minuteOfDay: Int): String {
            val normalized = minuteOfDay.coerceIn(0, LAST_MINUTE_OF_DAY)
            return "%02d:%02d".format(normalized / 60, normalized % 60)
        }
    }
}
