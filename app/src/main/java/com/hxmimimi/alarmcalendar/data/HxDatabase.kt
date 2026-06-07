package com.hxmimimi.alarmcalendar.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import com.hxmimimi.alarmcalendar.model.Alarm
import com.hxmimimi.alarmcalendar.model.AlarmRepeatKind
import com.hxmimimi.alarmcalendar.model.CalendarEvent
import com.hxmimimi.alarmcalendar.model.EventKind
import com.hxmimimi.alarmcalendar.model.EventRepeatKind
import com.hxmimimi.alarmcalendar.model.RingStat
import com.hxmimimi.alarmcalendar.util.toDaySet
import com.hxmimimi.alarmcalendar.util.toDb
import com.hxmimimi.alarmcalendar.util.toDbDays
import com.hxmimimi.alarmcalendar.util.toLocalDate
import com.hxmimimi.alarmcalendar.util.toLocalDateTimeOrNull
import java.io.File

class HxDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE alarms(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                enabled INTEGER NOT NULL,
                repeat_kind TEXT NOT NULL,
                repeat_days TEXT NOT NULL,
                ringtone_uri TEXT,
                snooze_guard_minutes INTEGER NOT NULL,
                welcome_message TEXT NOT NULL,
                next_at TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE events(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                kind TEXT NOT NULL,
                date TEXT NOT NULL,
                all_day INTEGER NOT NULL,
                start_minute INTEGER,
                end_minute INTEGER,
                show_notification INTEGER NOT NULL,
                show_lockscreen INTEGER NOT NULL,
                remind_days_before INTEGER NOT NULL,
                repeat_kind TEXT NOT NULL,
                repeat_value INTEGER NOT NULL,
                completed INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE ring_stats(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                alarm_id INTEGER NOT NULL,
                fired_at INTEGER NOT NULL,
                dismissed_at INTEGER,
                movement_score REAL NOT NULL,
                oversleep_reminders INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // 预留迁移入口；正式升级时保留用户闹钟与日历数据。
        }
    }

    fun databaseFile(context: Context): File = context.getDatabasePath(DB_NAME)

    companion object {
        const val DB_NAME = "hx_mimimi.db"
        private const val DB_VERSION = 1
    }
}

class AlarmRepository(private val db: HxDatabase) {
    fun alarms(): List<Alarm> = db.readableDatabase.query("alarms", null, null, null, null, null, "hour, minute")
        .useRows { cursor -> cursor.toAlarm() }

    fun enabledAlarms(): List<Alarm> = db.readableDatabase.query(
        "alarms",
        null,
        "enabled=1",
        null,
        null,
        null,
        "hour, minute",
    ).useRows { it.toAlarm() }

    fun upsert(alarm: Alarm): Long {
        val values = ContentValues().apply {
            put("title", alarm.title)
            put("hour", alarm.hour)
            put("minute", alarm.minute)
            put("enabled", if (alarm.enabled) 1 else 0)
            put("repeat_kind", alarm.repeatKind.name)
            put("repeat_days", alarm.repeatDays.toDbDays())
            put("ringtone_uri", alarm.ringtoneUri)
            put("snooze_guard_minutes", alarm.snoozeGuardMinutes)
            put("welcome_message", alarm.welcomeMessage)
            put("next_at", alarm.nextAt?.toDb())
        }
        return if (alarm.id == 0L) {
            db.writableDatabase.insert("alarms", null, values)
        } else {
            db.writableDatabase.update("alarms", values, "id=?", arrayOf(alarm.id.toString()))
            alarm.id
        }
    }

    fun setNextAt(id: Long, nextAt: String?) {
        db.writableDatabase.update("alarms", ContentValues().apply { put("next_at", nextAt) }, "id=?", arrayOf(id.toString()))
    }

    fun byId(id: Long): Alarm? = db.readableDatabase.query("alarms", null, "id=?", arrayOf(id.toString()), null, null, null)
        .useRows { it.toAlarm() }.firstOrNull()

    fun delete(id: Long) {
        db.writableDatabase.delete("alarms", "id=?", arrayOf(id.toString()))
    }
}

class EventRepository(private val db: HxDatabase) {
    fun events(): List<CalendarEvent> = db.readableDatabase.query("events", null, null, null, null, null, "date")
        .useRows { it.toEvent() }

    fun upsert(event: CalendarEvent): Long {
        val values = ContentValues().apply {
            put("title", event.title)
            put("description", event.description)
            put("kind", event.kind.name)
            put("date", event.date.toDb())
            put("all_day", if (event.allDay) 1 else 0)
            put("start_minute", event.startMinute)
            put("end_minute", event.endMinute)
            put("show_notification", if (event.showInNotification) 1 else 0)
            put("show_lockscreen", if (event.showOnLockScreen) 1 else 0)
            put("remind_days_before", event.remindDaysBefore)
            put("repeat_kind", event.repeatKind.name)
            put("repeat_value", event.repeatValue)
            put("completed", if (event.completed) 1 else 0)
        }
        return if (event.id == 0L) db.writableDatabase.insert("events", null, values)
        else {
            db.writableDatabase.update("events", values, "id=?", arrayOf(event.id.toString()))
            event.id
        }
    }

    fun delete(id: Long) {
        db.writableDatabase.delete("events", "id=?", arrayOf(id.toString()))
    }
}

class StatsRepository(private val db: HxDatabase) {
    fun insert(stat: RingStat): Long {
        val values = ContentValues().apply {
            put("alarm_id", stat.alarmId)
            put("fired_at", stat.firedAtMillis)
            put("dismissed_at", stat.dismissedAtMillis)
            put("movement_score", stat.movementScore)
            put("oversleep_reminders", stat.oversleepReminders)
        }
        return db.writableDatabase.insert("ring_stats", null, values)
    }

    fun latest(limit: Int = 20): List<RingStat> = db.readableDatabase.query(
        "ring_stats",
        null,
        null,
        null,
        null,
        null,
        "fired_at DESC",
        limit.toString(),
    ).useRows { it.toStat() }
}

class ExportRepository(private val context: Context, private val db: HxDatabase) {
    fun writeDatabaseTo(uri: Uri) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            db.databaseFile(context).inputStream().use { input -> input.copyTo(output) }
        }
    }

    fun exportJson(): String {
        val alarms = AlarmRepository(db).alarms().joinToString(prefix = "[", postfix = "]") {
            """{"id":${it.id},"title":"${it.title.escapeJson()}","hour":${it.hour},"minute":${it.minute},"enabled":${it.enabled},"repeatKind":"${it.repeatKind}","repeatDays":"${it.repeatDays.toDbDays()}","ringtoneUri":${it.ringtoneUri.jsonOrNull()},"snoozeGuardMinutes":${it.snoozeGuardMinutes},"welcomeMessage":"${it.welcomeMessage.escapeJson()}"}"""
        }
        val events = EventRepository(db).events().joinToString(prefix = "[", postfix = "]") {
            """{"id":${it.id},"title":"${it.title.escapeJson()}","description":"${it.description.escapeJson()}","kind":"${it.kind}","date":"${it.date}","allDay":${it.allDay},"showInNotification":${it.showInNotification},"showOnLockScreen":${it.showOnLockScreen},"remindDaysBefore":${it.remindDaysBefore},"repeatKind":"${it.repeatKind}","repeatValue":${it.repeatValue},"completed":${it.completed}}"""
        }
        return """{"version":1,"alarms":$alarms,"events":$events}"""
    }
}

private fun Cursor.toAlarm() = Alarm(
    id = getLong(getColumnIndexOrThrow("id")),
    title = getString(getColumnIndexOrThrow("title")),
    hour = getInt(getColumnIndexOrThrow("hour")),
    minute = getInt(getColumnIndexOrThrow("minute")),
    enabled = getInt(getColumnIndexOrThrow("enabled")) == 1,
    repeatKind = AlarmRepeatKind.valueOf(getString(getColumnIndexOrThrow("repeat_kind"))),
    repeatDays = getString(getColumnIndexOrThrow("repeat_days")).toDaySet(),
    ringtoneUri = getStringOrNull("ringtone_uri"),
    snoozeGuardMinutes = getInt(getColumnIndexOrThrow("snooze_guard_minutes")),
    welcomeMessage = getString(getColumnIndexOrThrow("welcome_message")),
    nextAt = getStringOrNull("next_at")?.toLocalDateTimeOrNull(),
)

private fun Cursor.toEvent() = CalendarEvent(
    id = getLong(getColumnIndexOrThrow("id")),
    title = getString(getColumnIndexOrThrow("title")),
    description = getString(getColumnIndexOrThrow("description")),
    kind = EventKind.valueOf(getString(getColumnIndexOrThrow("kind"))),
    date = getString(getColumnIndexOrThrow("date")).toLocalDate(),
    allDay = getInt(getColumnIndexOrThrow("all_day")) == 1,
    startMinute = getIntOrNull("start_minute"),
    endMinute = getIntOrNull("end_minute"),
    showInNotification = getInt(getColumnIndexOrThrow("show_notification")) == 1,
    showOnLockScreen = getInt(getColumnIndexOrThrow("show_lockscreen")) == 1,
    remindDaysBefore = getInt(getColumnIndexOrThrow("remind_days_before")),
    repeatKind = EventRepeatKind.valueOf(getString(getColumnIndexOrThrow("repeat_kind"))),
    repeatValue = getInt(getColumnIndexOrThrow("repeat_value")),
    completed = getInt(getColumnIndexOrThrow("completed")) == 1,
)

private fun Cursor.toStat() = RingStat(
    id = getLong(getColumnIndexOrThrow("id")),
    alarmId = getLong(getColumnIndexOrThrow("alarm_id")),
    firedAtMillis = getLong(getColumnIndexOrThrow("fired_at")),
    dismissedAtMillis = getLongOrNull("dismissed_at"),
    movementScore = getFloat(getColumnIndexOrThrow("movement_score")),
    oversleepReminders = getInt(getColumnIndexOrThrow("oversleep_reminders")),
)

private inline fun <T> Cursor.useRows(mapper: (Cursor) -> T): List<T> = use {
    val cursor = this
    buildList {
        while (cursor.moveToNext()) add(mapper(cursor))
    }
}

private fun Cursor.getStringOrNull(column: String): String? {
    val index = getColumnIndexOrThrow(column)
    return if (isNull(index)) null else getString(index)
}

private fun Cursor.getIntOrNull(column: String): Int? {
    val index = getColumnIndexOrThrow(column)
    return if (isNull(index)) null else getInt(index)
}

private fun Cursor.getLongOrNull(column: String): Long? {
    val index = getColumnIndexOrThrow(column)
    return if (isNull(index)) null else getLong(index)
}

private fun String.escapeJson(): String = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
private fun String?.jsonOrNull(): String = this?.let { "\"${it.escapeJson()}\"" } ?: "null"
