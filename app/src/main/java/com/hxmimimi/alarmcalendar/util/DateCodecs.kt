package com.hxmimimi.alarmcalendar.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

fun LocalDateTime.toDb(): String = toString()
fun String.toLocalDateTimeOrNull(): LocalDateTime? = runCatching { LocalDateTime.parse(this) }.getOrNull()
fun LocalDate.toDb(): String = toString()
fun String.toLocalDate(): LocalDate = LocalDate.parse(this)

fun Set<DayOfWeek>.toDbDays(): String = sortedBy { it.value }.joinToString(",") { it.value.toString() }

fun String.toDaySet(): Set<DayOfWeek> =
    split(",").mapNotNull { it.toIntOrNull() }.mapNotNull { value ->
        DayOfWeek.entries.firstOrNull { it.value == value }
    }.toSet()
