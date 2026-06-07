package com.hxmimimi.alarmcalendar.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hxmimimi.alarmcalendar.controller.NotificationController
import com.hxmimimi.alarmcalendar.service.AlarmRingService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0)
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, 0)
        when {
            alarmId > 0 -> AlarmRingService.start(context, alarmId)
            eventId > 0 -> NotificationController(context).showEvent(eventId, intent.getStringExtra(EXTRA_EVENT_TITLE) ?: "日历提醒")
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_TITLE = "event_title"
    }
}
