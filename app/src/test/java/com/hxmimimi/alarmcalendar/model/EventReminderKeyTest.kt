package com.hxmimimi.alarmcalendar.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EventReminderKeyTest {
    @Test
    fun requestCodeCombinesEventIdAndReminderIndex() {
        assertEquals(420, EventReminderKey.requestCode(eventId = 42, reminderIndex = 0))
        assertEquals(421, EventReminderKey.requestCode(eventId = 42, reminderIndex = 1))
        assertEquals(422, EventReminderKey.requestCode(eventId = 42, reminderIndex = 2))
    }

    @Test
    fun cancelIndexesCoverAllScheduledReminderSlots() {
        assertEquals(listOf(0, 1, 2), EventReminderKey.cancelReminderIndexes())
    }
}
