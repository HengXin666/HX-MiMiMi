package com.hxmimimi.alarmcalendar.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hxmimimi.alarmcalendar.controller.AlarmScheduler
import com.hxmimimi.alarmcalendar.controller.EventScheduler
import com.hxmimimi.alarmcalendar.data.AlarmRepository
import com.hxmimimi.alarmcalendar.data.EventRepository
import com.hxmimimi.alarmcalendar.data.HxDatabase

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val db = HxDatabase(context)
        AlarmScheduler(context, AlarmRepository(db)).scheduleAll()
        EventScheduler(context, EventRepository(db)).scheduleAll()
    }
}
