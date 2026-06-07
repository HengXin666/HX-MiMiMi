package com.hxmimimi.alarmcalendar

import android.app.Application
import com.hxmimimi.alarmcalendar.controller.NotificationController

class AlarmCalendarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationController(this).ensureChannels()
    }
}
