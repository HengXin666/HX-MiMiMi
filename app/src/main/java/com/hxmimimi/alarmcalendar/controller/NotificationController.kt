package com.hxmimimi.alarmcalendar.controller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hxmimimi.alarmcalendar.MainActivity
import com.hxmimimi.alarmcalendar.R
import com.hxmimimi.alarmcalendar.receiver.AlarmReceiver
import com.hxmimimi.alarmcalendar.service.AlarmRingService
import com.hxmimimi.alarmcalendar.view.AlarmRingActivity

class NotificationController(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val systemManager = context.getSystemService(NotificationManager::class.java)
        systemManager.createNotificationChannel(NotificationChannel(CHANNEL_ALARM, context.getString(R.string.notification_channel_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
        })
        systemManager.createNotificationChannel(NotificationChannel(CHANNEL_EVENT, context.getString(R.string.notification_channel_event), NotificationManager.IMPORTANCE_HIGH))
        systemManager.createNotificationChannel(NotificationChannel(CHANNEL_STATUS, context.getString(R.string.notification_channel_status), NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun alarmNotification(alarmId: Long, title: String): Notification {
        val fullScreen = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            Intent(context, AlarmRingActivity::class.java).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val guardedDismiss = PendingIntent.getService(
            context,
            (alarmId + 200_000).toInt(),
            Intent(context, AlarmRingService::class.java)
                .setAction(AlarmRingService.ACTION_DISMISS)
                .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(title)
            .setContentText("起身活动后才能关闭")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .addAction(R.drawable.ic_notification_alarm, "尝试关闭", guardedDismiss)
            .build()
    }

    fun showEvent(eventId: Long, title: String) {
        val content = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify((20_000 + eventId).toInt(), NotificationCompat.Builder(context, CHANNEL_EVENT)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle("日历提醒")
            .setContentText(title)
            .setContentIntent(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build())
    }

    fun showStatus(id: Int, title: String, text: String) {
        manager.notify(id, NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build())
    }

    companion object {
        const val CHANNEL_ALARM = "alarm"
        const val CHANNEL_EVENT = "event"
        const val CHANNEL_STATUS = "status"
    }
}
