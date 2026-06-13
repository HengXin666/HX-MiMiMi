package com.hxmimimi.alarmcalendar.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object EventDisplayText {
    fun kindLabel(kind: EventKind): String = when (kind) {
        EventKind.COUNTDOWN -> "倒数日"
        EventKind.DAY_NOTE -> "事项日"
        EventKind.INTERVAL -> "间隔日"
    }

    fun repeatLabel(kind: EventRepeatKind, value: Int): String = when (kind) {
        EventRepeatKind.NONE -> "不重复"
        EventRepeatKind.MONTHLY_DAY -> "每月 ${value.coerceIn(1, 31)} 号"
        EventRepeatKind.WEEKLY -> "每星期"
        EventRepeatKind.EVERY_N_DAYS -> "每 ${value.coerceAtLeast(1)} 天"
    }

    fun countdownText(event: CalendarEvent, today: LocalDate): String =
        "还有 ${ChronoUnit.DAYS.between(today, event.date)} 天"
}
