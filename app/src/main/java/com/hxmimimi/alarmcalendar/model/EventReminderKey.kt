package com.hxmimimi.alarmcalendar.model

object EventReminderKey {
    private const val REMINDER_SLOTS = 3

    fun requestCode(eventId: Long, reminderIndex: Int): Int =
        (eventId * 10 + reminderIndex.coerceIn(0, REMINDER_SLOTS - 1)).toInt()

    fun cancelReminderIndexes(): List<Int> = (0 until REMINDER_SLOTS).toList()
}
