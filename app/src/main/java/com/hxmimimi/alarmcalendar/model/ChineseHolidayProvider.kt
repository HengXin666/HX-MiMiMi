package com.hxmimimi.alarmcalendar.model

import java.time.LocalDate

/**
 * 中国节假日逻辑集中在这里，后续可替换为远端同步或年度 JSON。
 * 当前内置常见法定节日和示例调休表，避免闹钟重复逻辑散落在调度器里。
 */
class ChineseHolidayProvider {
    private val adjustedWorkdays = setOf(
        LocalDate.of(2026, 2, 14),
        LocalDate.of(2026, 2, 28),
        LocalDate.of(2026, 5, 9),
        LocalDate.of(2026, 9, 20),
        LocalDate.of(2026, 10, 10),
    )

    private val adjustedHolidays = setOf(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 18),
        LocalDate.of(2026, 2, 19), LocalDate.of(2026, 2, 20), LocalDate.of(2026, 2, 21),
        LocalDate.of(2026, 2, 22), LocalDate.of(2026, 4, 5), LocalDate.of(2026, 5, 1),
        LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 3), LocalDate.of(2026, 6, 19),
        LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2),
        LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 5),
        LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 7),
    )

    fun isWorkday(date: LocalDate): Boolean {
        if (date in adjustedWorkdays) return true
        if (date in adjustedHolidays) return false
        return date.dayOfWeek.value in 1..5
    }

    fun isHoliday(date: LocalDate): Boolean = !isWorkday(date)
}
