package com.hxmimimi.alarmcalendar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hxmimimi.alarmcalendar.controller.AlarmScheduler
import com.hxmimimi.alarmcalendar.controller.EventScheduler
import com.hxmimimi.alarmcalendar.data.AlarmRepository
import com.hxmimimi.alarmcalendar.data.EventRepository
import com.hxmimimi.alarmcalendar.data.ExportRepository
import com.hxmimimi.alarmcalendar.data.HxDatabase
import com.hxmimimi.alarmcalendar.data.StatsRepository
import com.hxmimimi.alarmcalendar.model.Alarm
import com.hxmimimi.alarmcalendar.model.AlarmRepeatKind
import com.hxmimimi.alarmcalendar.model.CalendarEvent
import com.hxmimimi.alarmcalendar.model.EventKind
import com.hxmimimi.alarmcalendar.model.EventRepeatKind
import com.hxmimimi.alarmcalendar.view.theme.HxTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HxTheme { AppRoot() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val db = remember { HxDatabase(context) }
    val alarmRepo = remember { AlarmRepository(db) }
    val eventRepo = remember { EventRepository(db) }
    val statsRepo = remember { StatsRepository(db) }
    val alarmScheduler = remember { AlarmScheduler(context, alarmRepo) }
    val eventScheduler = remember { EventScheduler(context, eventRepo) }
    var tab by remember { mutableIntStateOf(0) }
    var alarms by remember { mutableStateOf(alarmRepo.alarms()) }
    var events by remember { mutableStateOf(eventRepo.events()) }
    var stats by remember { mutableStateOf(statsRepo.latest()) }
    var ringtone by remember { mutableStateOf<Uri?>(null) }
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { uri ->
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ringtone = uri
        }
    }
    val exportDb = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
        it?.let { uri -> ExportRepository(context, db).writeDatabaseTo(uri) }
    }
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        it?.let { uri ->
            context.contentResolver.openOutputStream(uri)?.use { out -> out.write(ExportRepository(context, db).exportJson().toByteArray()) }
        }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("HX MiMiMi") }) },
        bottomBar = {
            NavigationBar {
                val items = listOf("闹钟" to Icons.Default.Alarm, "日历" to Icons.Default.CalendarMonth, "统计" to Icons.Default.Insights, "设置" to Icons.Default.Settings)
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index; stats = statsRepo.latest() },
                        icon = { Icon(item.second, contentDescription = item.first) },
                        label = { Text(item.first) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (tab) {
                0 -> AlarmScreen(alarms, onAdd = { alarm ->
                    val id = alarmRepo.upsert(alarm.copy(ringtoneUri = alarm.ringtoneUri ?: ringtone?.toString()))
                    alarmRepo.byId(id)?.let(alarmScheduler::schedule)
                    alarms = alarmRepo.alarms()
                }, onToggle = {
                    val updated = it.copy(enabled = !it.enabled)
                    alarmRepo.upsert(updated)
                    alarmScheduler.schedule(updated)
                    alarms = alarmRepo.alarms()
                }, onDelete = {
                    alarmScheduler.cancel(it.id)
                    alarmRepo.delete(it.id)
                    alarms = alarmRepo.alarms()
                }, onPickAudio = { pickAudio.launch(arrayOf("audio/*", "application/octet-stream")) })
                1 -> EventScreen(events, onAdd = { event ->
                    val id = eventRepo.upsert(event)
                    eventRepo.events().firstOrNull { it.id == id }?.let(eventScheduler::schedule)
                    events = eventRepo.events()
                }, onDelete = { eventRepo.delete(it.id); events = eventRepo.events() })
                2 -> StatsScreen(stats)
                3 -> SettingsScreen(
                    canExact = alarmScheduler.canScheduleExactAlarms(),
                    onExactSettings = { context.startActivity(alarmScheduler.exactAlarmSettingsIntent()) },
                    onExportDb = { exportDb.launch("hx_mimimi.db") },
                    onExportJson = { exportJson.launch("hx_mimimi.json") },
                )
            }
        }
    }
}

@Composable
private fun AlarmScreen(alarms: List<Alarm>, onAdd: (Alarm) -> Unit, onToggle: (Alarm) -> Unit, onDelete: (Alarm) -> Unit, onPickAudio: () -> Unit) {
    var title by remember { mutableStateOf("早安闹钟") }
    var hour by remember { mutableStateOf("7") }
    var minute by remember { mutableStateOf("30") }
    var guard by remember { mutableStateOf("8") }
    var welcome by remember { mutableStateOf("起床先喝杯水吧") }
    var repeatKind by remember { mutableStateOf(AlarmRepeatKind.WEEKLY) }
    HxCard {
        Text("新增闹钟", fontWeight = FontWeight.Bold)
        TextField(value = title, onValueChange = { title = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(value = hour, onValueChange = { hour = it.filter(Char::isDigit).take(2) }, label = { Text("时") }, modifier = Modifier.weight(1f))
            TextField(value = minute, onValueChange = { minute = it.filter(Char::isDigit).take(2) }, label = { Text("分") }, modifier = Modifier.weight(1f))
            TextField(value = guard, onValueChange = { guard = it.filter(Char::isDigit).take(2) }, label = { Text("防赖床") }, modifier = Modifier.weight(1f))
        }
        TextField(value = welcome, onValueChange = { welcome = it }, label = { Text("醒来提示") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { repeatKind = repeatKind.next() }) { Text(repeatKind.label()) }
            Button(onClick = onPickAudio) {
                Icon(Icons.Default.MusicNote, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("铃声")
            }
            Button(onClick = {
                onAdd(
                    Alarm(
                        title = title.ifBlank { "闹钟" },
                        hour = hour.toIntOrNull()?.coerceIn(0, 23) ?: 7,
                        minute = minute.toIntOrNull()?.coerceIn(0, 59) ?: 30,
                        repeatKind = repeatKind,
                        repeatDays = if (repeatKind == AlarmRepeatKind.WEEKLY) DayOfWeek.entries.toSet() else emptySet(),
                        snoozeGuardMinutes = guard.toIntOrNull()?.coerceAtLeast(1) ?: 8,
                        welcomeMessage = welcome.ifBlank { "起床先喝杯水吧" },
                    ),
                )
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("添加")
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(alarms, key = { it.id }) { alarm ->
            HxCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("%02d:%02d".format(alarm.hour, alarm.minute), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("${alarm.title} · ${alarm.repeatKind.label()} · 防赖床 ${alarm.snoozeGuardMinutes} 分钟")
                        alarm.nextAt?.let { Text("下次 $it", style = MaterialTheme.typography.bodySmall) }
                    }
                    Switch(checked = alarm.enabled, onCheckedChange = { onToggle(alarm) })
                    IconButton(onClick = { onDelete(alarm) }) { Icon(Icons.Default.Delete, contentDescription = "删除") }
                }
            }
        }
    }
}

@Composable
private fun EventScreen(events: List<CalendarEvent>, onAdd: (CalendarEvent) -> Unit, onDelete: (CalendarEvent) -> Unit) {
    var title by remember { mutableStateOf("事项") }
    var dateText by remember { mutableStateOf(LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE)) }
    var remindDays by remember { mutableStateOf("1") }
    var repeatValue by remember { mutableStateOf("1") }
    var startMinute by remember { mutableStateOf("540") }
    var endMinute by remember { mutableStateOf("600") }
    var kind by remember { mutableStateOf(EventKind.DAY_NOTE) }
    var repeat by remember { mutableStateOf(EventRepeatKind.NONE) }
    var allDay by remember { mutableStateOf(true) }
    var showNotification by remember { mutableStateOf(true) }
    var showLockScreen by remember { mutableStateOf(true) }
    HxCard {
        Text("新增日历", fontWeight = FontWeight.Bold)
        TextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(value = dateText, onValueChange = { dateText = it.take(10) }, label = { Text("日期") }, modifier = Modifier.weight(1.4f))
            TextField(value = remindDays, onValueChange = { remindDays = it.filter(Char::isDigit).take(3) }, label = { Text("提前天") }, modifier = Modifier.weight(1f))
        }
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("全天", modifier = Modifier.weight(1f))
                Switch(checked = allDay, onCheckedChange = { allDay = it })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("通知栏显示", modifier = Modifier.weight(1f))
                Switch(checked = showNotification, onCheckedChange = { showNotification = it })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("锁屏显示", modifier = Modifier.weight(1f))
                Switch(checked = showLockScreen, onCheckedChange = { showLockScreen = it })
            }
        }
        if (!allDay) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = startMinute, onValueChange = { startMinute = it.filter(Char::isDigit).take(4) }, label = { Text("开始分钟") }, modifier = Modifier.weight(1f))
                TextField(value = endMinute, onValueChange = { endMinute = it.filter(Char::isDigit).take(4) }, label = { Text("结束分钟") }, modifier = Modifier.weight(1f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { kind = kind.next() }) { Text(kind.label()) }
            Button(onClick = { repeat = repeat.next() }) { Text(repeat.label(repeatValue.toIntOrNull() ?: 1)) }
            if (repeat != EventRepeatKind.NONE && repeat != EventRepeatKind.WEEKLY) {
                TextField(value = repeatValue, onValueChange = { repeatValue = it.filter(Char::isDigit).take(2) }, label = { Text("重复值") }, modifier = Modifier.weight(1f))
            }
            Button(onClick = {
                val date = runCatching { LocalDate.parse(dateText) }.getOrElse { LocalDate.now().plusDays(1) }
                onAdd(
                    CalendarEvent(
                        title = title.ifBlank { "事项" },
                        kind = kind,
                        date = date,
                        allDay = allDay,
                        startMinute = if (allDay) null else startMinute.toIntOrNull()?.coerceIn(0, 1439),
                        endMinute = if (allDay) null else endMinute.toIntOrNull()?.coerceIn(0, 1439),
                        showInNotification = showNotification,
                        showOnLockScreen = showLockScreen,
                        remindDaysBefore = remindDays.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                        repeatKind = repeat,
                        repeatValue = repeatValue.toIntOrNull()?.coerceAtLeast(1) ?: date.dayOfMonth,
                    ),
                )
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("添加")
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(events, key = { it.id }) { event ->
            HxCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${event.kind.label()} · ${event.date.format(DateTimeFormatter.ISO_DATE)} · 提前 ${event.remindDaysBefore} 天")
                        if (event.kind == EventKind.COUNTDOWN) Text("还有 ${ChronoUnit.DAYS.between(LocalDate.now(), event.date)} 天")
                        Text("通知栏 ${event.showInNotification.yesNo()} · 锁屏 ${event.showOnLockScreen.yesNo()} · ${event.repeatKind.label(event.repeatValue)}")
                    }
                    IconButton(onClick = { onDelete(event) }) { Icon(Icons.Default.Delete, contentDescription = "删除") }
                }
            }
        }
    }
}

@Composable
private fun StatsScreen(stats: List<com.hxmimimi.alarmcalendar.model.RingStat>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(stats, key = { it.id }) {
            HxCard {
                Text("闹钟 #${it.alarmId}", fontWeight = FontWeight.Bold)
                Text("运动分数 ${"%.1f".format(it.movementScore)} · 防赖床提醒 ${it.oversleepReminders} 次")
                Text("响铃 ${java.time.Instant.ofEpochMilli(it.firedAtMillis)}")
            }
        }
    }
}

@Composable
private fun SettingsScreen(canExact: Boolean, onExactSettings: () -> Unit, onExportDb: () -> Unit, onExportJson: () -> Unit) {
    HxCard {
        Text("可靠性", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("精确闹钟权限：${canExact.yesNo()}")
        Text("普通杀后台、系统回收、重启后会重排；系统强行停止后 Android 会禁止第三方应用自启动。")
        TextButton(onClick = onExactSettings) { Text("打开精确闹钟设置") }
    }
    Spacer(Modifier.height(12.dp))
    HxCard {
        Text("导出", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onExportJson) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("JSON")
            }
            Button(onClick = onExportDb) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("SQLite")
            }
        }
    }
}

@Composable
private fun HxCard(content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}

private fun AlarmRepeatKind.label(): String = when (this) {
    AlarmRepeatKind.ONCE -> "单次"
    AlarmRepeatKind.WEEKLY -> "按星期"
    AlarmRepeatKind.CHINESE_WORKDAY -> "中国工作日"
    AlarmRepeatKind.CHINESE_HOLIDAY -> "中国节假日"
}

private fun AlarmRepeatKind.next(): AlarmRepeatKind = when (this) {
    AlarmRepeatKind.ONCE -> AlarmRepeatKind.WEEKLY
    AlarmRepeatKind.WEEKLY -> AlarmRepeatKind.CHINESE_WORKDAY
    AlarmRepeatKind.CHINESE_WORKDAY -> AlarmRepeatKind.CHINESE_HOLIDAY
    AlarmRepeatKind.CHINESE_HOLIDAY -> AlarmRepeatKind.ONCE
}

private fun EventKind.label(): String = when (this) {
    EventKind.COUNTDOWN -> "倒数日"
    EventKind.DAY_NOTE -> "事项日"
    EventKind.INTERVAL -> "间隔日"
}

private fun EventKind.next(): EventKind = when (this) {
    EventKind.COUNTDOWN -> EventKind.DAY_NOTE
    EventKind.DAY_NOTE -> EventKind.INTERVAL
    EventKind.INTERVAL -> EventKind.COUNTDOWN
}

private fun EventRepeatKind.label(value: Int): String = when (this) {
    EventRepeatKind.NONE -> "不重复"
    EventRepeatKind.MONTHLY_DAY -> "每月 ${value} 号"
    EventRepeatKind.WEEKLY -> "每星期"
    EventRepeatKind.EVERY_N_DAYS -> "每 ${value.coerceAtLeast(1)} 天"
}

private fun EventRepeatKind.next(): EventRepeatKind = when (this) {
    EventRepeatKind.NONE -> EventRepeatKind.MONTHLY_DAY
    EventRepeatKind.MONTHLY_DAY -> EventRepeatKind.WEEKLY
    EventRepeatKind.WEEKLY -> EventRepeatKind.EVERY_N_DAYS
    EventRepeatKind.EVERY_N_DAYS -> EventRepeatKind.NONE
}

private fun Boolean.yesNo(): String = if (this) "已开启" else "未开启"
