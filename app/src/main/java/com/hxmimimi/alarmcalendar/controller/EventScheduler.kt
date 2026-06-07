package com.hxmimimi.alarmcalendar.controller

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.hxmimimi.alarmcalendar.data.EventRepository
import com.hxmimimi.alarmcalendar.model.CalendarEvent
import com.hxmimimi.alarmcalendar.model.EventRepeatKind
import com.hxmimimi.alarmcalendar.receiver.AlarmReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class EventScheduler(
    private val context: Context,
    private val repository: EventRepository,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleAll() {
        repository.events().filter { !it.completed && it.showInNotification }.forEach(::schedule)
    }

    fun schedule(event: CalendarEvent) {
        reminderTimes(event, LocalDateTime.now()).forEachIndexed { index, time ->
            val requestCode = (event.id * 10 + index).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, AlarmReceiver::class.java)
                    .putExtra(AlarmReceiver.EXTRA_EVENT_ID, event.id)
                    .putExtra(AlarmReceiver.EXTRA_EVENT_TITLE, event.title),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                pendingIntent,
            )
        }
    }

    fun reminderTimes(event: CalendarEvent, from: LocalDateTime): List<LocalDateTime> {
        val occurrence = nextOccurrence(event, from.toLocalDate()) ?: return emptyList()
        val baseTime = if (event.allDay) LocalTime.of(9, 0) else {
            val minuteOfDay = event.startMinute ?: 9 * 60
            LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        }
        val base = LocalDateTime.of(occurrence, baseTime)
        return listOf(
            base.minusDays(event.remindDaysBefore.toLong()),
            base.minusMinutes(15),
            base.minusMinutes(2),
        ).distinct().filter { it.isAfter(from) }
    }

    private fun nextOccurrence(event: CalendarEvent, from: LocalDate): LocalDate? = when (event.repeatKind) {
        EventRepeatKind.NONE -> event.date.takeIf { !it.isBefore(from) }
        EventRepeatKind.WEEKLY -> generateSequence(event.date) { it.plusWeeks(1) }.first { !it.isBefore(from) }
        EventRepeatKind.EVERY_N_DAYS -> {
            val days = event.repeatValue.coerceAtLeast(1).toLong()
            generateSequence(event.date) { it.plusDays(days) }.first { !it.isBefore(from) }
        }
        EventRepeatKind.MONTHLY_DAY -> {
            val desiredDay = event.repeatValue.takeIf { it > 0 } ?: event.date.dayOfMonth
            generateSequence(event.date.withDayOfMonth(event.date.dayOfMonth.coerceAtMost(event.date.lengthOfMonth()))) {
                val nextMonth = it.plusMonths(1)
                nextMonth.withDayOfMonth(desiredDay.coerceAtMost(nextMonth.lengthOfMonth()))
            }.first { !it.isBefore(from) }
        }
    }
}
