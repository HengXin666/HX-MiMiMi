package com.hxmimimi.alarmcalendar.controller

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.hxmimimi.alarmcalendar.data.AlarmRepository
import com.hxmimimi.alarmcalendar.model.Alarm
import com.hxmimimi.alarmcalendar.model.AlarmRepeatKind
import com.hxmimimi.alarmcalendar.model.ChineseHolidayProvider
import com.hxmimimi.alarmcalendar.receiver.AlarmReceiver
import com.hxmimimi.alarmcalendar.util.toDb
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class AlarmScheduler(
    private val context: Context,
    private val repository: AlarmRepository,
    private val holidays: ChineseHolidayProvider = ChineseHolidayProvider(),
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun exactAlarmSettingsIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun scheduleAll() {
        repository.enabledAlarms().forEach(::schedule)
    }

    fun schedule(alarm: Alarm) {
        if (!alarm.enabled) {
            cancel(alarm.id)
            return
        }
        val nextAt = computeNext(alarm, LocalDateTime.now()) ?: return
        repository.setNextAt(alarm.id, nextAt.toDb())
        val operation = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            Intent(context, AlarmReceiver::class.java).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val showIntent = PendingIntent.getActivity(
            context,
            (alarm.id + 100_000).toInt(),
            Intent(context, com.hxmimimi.alarmcalendar.view.AlarmRingActivity::class.java)
                .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerAt = nextAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent)
        alarmManager.setAlarmClock(info, operation)
    }

    fun cancel(alarmId: Long) {
        val operation = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            Intent(context, AlarmReceiver::class.java).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(operation)
    }

    fun computeNext(alarm: Alarm, from: LocalDateTime): LocalDateTime? {
        var date = from.toLocalDate()
        repeat(370) {
            val candidate = LocalDateTime.of(date, LocalTime.of(alarm.hour, alarm.minute))
            if (candidate.isAfter(from) && matches(alarm, date)) return candidate
            date = date.plusDays(1)
        }
        return null
    }

    private fun matches(alarm: Alarm, date: LocalDate): Boolean = when (alarm.repeatKind) {
        AlarmRepeatKind.ONCE -> true
        AlarmRepeatKind.WEEKLY -> date.dayOfWeek in alarm.repeatDays
        AlarmRepeatKind.CHINESE_WORKDAY -> holidays.isWorkday(date)
        AlarmRepeatKind.CHINESE_HOLIDAY -> holidays.isHoliday(date)
    }
}
