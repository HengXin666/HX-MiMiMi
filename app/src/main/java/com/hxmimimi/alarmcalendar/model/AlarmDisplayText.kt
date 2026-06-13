package com.hxmimimi.alarmcalendar.model

import java.time.DayOfWeek

object AlarmDisplayText {
    fun repeatLabel(kind: AlarmRepeatKind): String = when (kind) {
        AlarmRepeatKind.ONCE -> "单次"
        AlarmRepeatKind.WEEKLY -> "按星期"
        AlarmRepeatKind.CHINESE_WORKDAY -> "中国工作日"
        AlarmRepeatKind.CHINESE_HOLIDAY -> "中国节假日"
    }

    fun timeText(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

    fun guardSummary(alarm: Alarm): String =
        "防赖床 ${alarm.snoozeGuardMinutes} 分钟 · 关闭分 ${"%.0f".format(alarm.dismissMovementScore)} · 确认分 ${"%.0f".format(alarm.awakeMovementScore)} · 连续 ${alarm.awakeConfirmSeconds} 秒"

    fun repeatDaysText(alarm: Alarm): String =
        if (alarm.repeatKind == AlarmRepeatKind.WEEKLY && alarm.repeatDays.isNotEmpty()) {
            alarm.repeatDays.sortedBy { it.value }.joinToString("、") { dayLabel(it) }
        } else {
            repeatLabel(alarm.repeatKind)
        }

    fun dayLabel(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
}
