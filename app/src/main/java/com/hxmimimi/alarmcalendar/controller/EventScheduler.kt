package com.hxmimimi.alarmcalendar.controller

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.hxmimimi.alarmcalendar.data.EventRepository
import com.hxmimimi.alarmcalendar.model.CalendarEvent
import com.hxmimimi.alarmcalendar.model.EventRecurrenceCalculator
import com.hxmimimi.alarmcalendar.model.EventReminderKey
import com.hxmimimi.alarmcalendar.receiver.AlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId

class EventScheduler(
    private val context: Context,
    private val repository: EventRepository,
    private val recurrenceCalculator: EventRecurrenceCalculator = EventRecurrenceCalculator(),
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleAll() {
        repository.events().filter { !it.completed && it.showInNotification }.forEach(::schedule)
    }

    fun schedule(event: CalendarEvent) {
        reminderTimes(event, LocalDateTime.now()).forEachIndexed { index, time ->
            scheduleReminder(event, time, index)
        }
    }

    fun scheduleReminder(event: CalendarEvent, time: LocalDateTime, reminderIndex: Int = 0) {
        val requestCode = EventReminderKey.requestCode(event.id, reminderIndex)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java)
                .putExtra(AlarmReceiver.EXTRA_EVENT_ID, event.id)
                .putExtra(AlarmReceiver.EXTRA_EVENT_TITLE, event.title)
                .putExtra(AlarmReceiver.EXTRA_EVENT_TRIGGER_AT, time.toString()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            pendingIntent,
        )
    }

    fun cancel(eventId: Long) {
        EventReminderKey.cancelReminderIndexes().forEach { index ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                EventReminderKey.requestCode(eventId, index),
                Intent(context, AlarmReceiver::class.java).putExtra(AlarmReceiver.EXTRA_EVENT_ID, eventId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    fun reminderTimes(event: CalendarEvent, from: LocalDateTime): List<LocalDateTime> =
        recurrenceCalculator.reminderTimes(event, from)
}
