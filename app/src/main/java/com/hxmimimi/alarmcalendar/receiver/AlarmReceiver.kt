package com.hxmimimi.alarmcalendar.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hxmimimi.alarmcalendar.controller.EventScheduler
import com.hxmimimi.alarmcalendar.controller.NotificationController
import com.hxmimimi.alarmcalendar.data.EventRepository
import com.hxmimimi.alarmcalendar.data.HxDatabase
import com.hxmimimi.alarmcalendar.model.EventRecurrenceCalculator
import com.hxmimimi.alarmcalendar.service.AlarmRingService
import java.time.LocalDateTime

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0)
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, 0)
        when {
            alarmId > 0 -> AlarmRingService.start(context, alarmId)
            eventId > 0 -> {
                NotificationController(context).showEvent(eventId, intent.getStringExtra(EXTRA_EVENT_TITLE) ?: "日历提醒")
                scheduleNextEventOccurrence(context, eventId, intent.getStringExtra(EXTRA_EVENT_TRIGGER_AT))
            }
        }
    }

    private fun scheduleNextEventOccurrence(context: Context, eventId: Long, triggerAtText: String?) {
        val triggeredAt = triggerAtText?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: return
        val db = HxDatabase(context)
        val repository = EventRepository(db)
        val event = repository.byId(eventId)?.takeIf { !it.completed && it.showInNotification } ?: return
        val nextReminder = EventRecurrenceCalculator().nextRescheduleFromTrigger(event, triggeredAt) ?: return
        EventScheduler(context, repository).scheduleReminder(event, nextReminder)
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_TITLE = "event_title"
        const val EXTRA_EVENT_TRIGGER_AT = "event_trigger_at"
    }
}
