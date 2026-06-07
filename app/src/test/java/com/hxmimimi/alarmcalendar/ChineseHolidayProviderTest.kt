package com.hxmimimi.alarmcalendar

import com.hxmimimi.alarmcalendar.model.ChineseHolidayProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ChineseHolidayProviderTest {
    private val provider = ChineseHolidayProvider()

    @Test
    fun detectsAdjustedWorkday() {
        assertTrue(provider.isWorkday(LocalDate.of(2026, 2, 14)))
    }

    @Test
    fun detectsHoliday() {
        assertFalse(provider.isWorkday(LocalDate.of(2026, 10, 1)))
    }
}
