package com.hxmimimi.alarmcalendar.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

enum class AlarmRepeatKind { ONCE, WEEKLY, CHINESE_WORKDAY, CHINESE_HOLIDAY }
enum class EventKind { COUNTDOWN, DAY_NOTE, INTERVAL }
enum class EventRepeatKind { NONE, MONTHLY_DAY, WEEKLY, EVERY_N_DAYS }

data class Alarm(
    val id: Long = 0,
    val title: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val repeatKind: AlarmRepeatKind = AlarmRepeatKind.ONCE,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val ringtoneUri: String? = null,
    val snoozeGuardMinutes: Int = 8,
    val welcomeMessage: String = "起床先喝杯水吧",
    val nextAt: LocalDateTime? = null,
)

data class CalendarEvent(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val kind: EventKind,
    val date: LocalDate,
    val allDay: Boolean = true,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val showInNotification: Boolean = true,
    val showOnLockScreen: Boolean = true,
    val remindDaysBefore: Int = 0,
    val repeatKind: EventRepeatKind = EventRepeatKind.NONE,
    val repeatValue: Int = 0,
    val completed: Boolean = false,
)

data class RingStat(
    val id: Long = 0,
    val alarmId: Long,
    val firedAtMillis: Long,
    val dismissedAtMillis: Long?,
    val movementScore: Float,
    val oversleepReminders: Int,
)
