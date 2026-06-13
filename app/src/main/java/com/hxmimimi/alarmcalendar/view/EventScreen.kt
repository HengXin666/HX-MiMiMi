package com.hxmimimi.alarmcalendar.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hxmimimi.alarmcalendar.model.CalendarEvent
import com.hxmimimi.alarmcalendar.model.EventDraft
import com.hxmimimi.alarmcalendar.model.EventDisplayText
import com.hxmimimi.alarmcalendar.model.EventKind
import com.hxmimimi.alarmcalendar.model.EventRepeatKind
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun EventScreen(events: List<CalendarEvent>, onAdd: (CalendarEvent) -> Unit, onDelete: (CalendarEvent) -> Unit) {
    var title by remember { mutableStateOf("事项") }
    var description by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE)) }
    var remindDays by remember { mutableStateOf("1") }
    var repeatValue by remember { mutableStateOf("1") }
    var startText by remember { mutableStateOf("09:00") }
    var endText by remember { mutableStateOf("10:00") }
    var kind by remember { mutableStateOf(EventKind.DAY_NOTE) }
    var repeat by remember { mutableStateOf(EventRepeatKind.NONE) }
    var allDay by remember { mutableStateOf(true) }
    var showNotification by remember { mutableStateOf(true) }
    var showLockScreen by remember { mutableStateOf(true) }
    var completed by remember { mutableStateOf(false) }
    var editingEventId by remember { mutableStateOf(0L) }
    fun applyDraft(draft: EventDraft) {
        editingEventId = draft.id
        title = draft.title
        description = draft.description
        dateText = draft.dateText.ifBlank { LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE) }
        remindDays = draft.remindDaysText
        repeatValue = draft.repeatValueText
        startText = draft.startText
        endText = draft.endText
        kind = draft.kind
        repeat = draft.repeatKind
        allDay = draft.allDay
        showNotification = draft.showNotification
        showLockScreen = draft.showLockScreen
        completed = draft.completed
    }
    EventCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (editingEventId == 0L) "新增日历" else "编辑日历", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            if (editingEventId != 0L) {
                IconButton(onClick = { applyDraft(EventDraft()) }) {
                    Icon(Icons.Default.Close, contentDescription = "取消编辑")
                }
            }
        }
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
                TextField(value = startText, onValueChange = { startText = it.filter { char -> char.isDigit() || char == ':' }.take(5) }, label = { Text("开始") }, modifier = Modifier.weight(1f))
                TextField(value = endText, onValueChange = { endText = it.filter { char -> char.isDigit() || char == ':' }.take(5) }, label = { Text("结束") }, modifier = Modifier.weight(1f))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EventKind.entries.forEach { option ->
                    FilterChip(
                        selected = option == kind,
                        onClick = { kind = option },
                        label = { Text(option.label()) },
                    )
                }
            }
            RepeatChipRow(
                options = listOf(EventRepeatKind.NONE, EventRepeatKind.MONTHLY_DAY),
                selected = repeat,
                repeatValue = repeatValue.toIntOrNull() ?: 1,
                onSelected = { repeat = it },
            )
            RepeatChipRow(
                options = listOf(EventRepeatKind.WEEKLY, EventRepeatKind.EVERY_N_DAYS),
                selected = repeat,
                repeatValue = repeatValue.toIntOrNull() ?: 1,
                onSelected = { repeat = it },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (repeat != EventRepeatKind.NONE && repeat != EventRepeatKind.WEEKLY) {
                TextField(value = repeatValue, onValueChange = { repeatValue = it.filter(Char::isDigit).take(2) }, label = { Text(if (repeat == EventRepeatKind.MONTHLY_DAY) "每月日期" else "间隔天数") }, modifier = Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
            Button(onClick = {
                onAdd(
                    EventDraft(
                        id = editingEventId,
                        title = title,
                        description = description,
                        dateText = dateText,
                        remindDaysText = remindDays,
                        repeatValueText = repeatValue,
                        startText = startText,
                        endText = endText,
                        allDay = allDay,
                        kind = kind,
                        repeatKind = repeat,
                        showNotification = showNotification,
                        showLockScreen = showLockScreen,
                        completed = completed,
                    ).toEvent(LocalDate.now().plusDays(1)),
                )
                applyDraft(EventDraft())
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (editingEventId == 0L) "添加" else "保存")
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(events, key = { it.id }) { event ->
            EventCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${EventDisplayText.kindLabel(event.kind)} · ${event.date.format(DateTimeFormatter.ISO_DATE)} · 提前 ${event.remindDaysBefore} 天")
                        if (event.kind == EventKind.COUNTDOWN) Text(EventDisplayText.countdownText(event, LocalDate.now()))
                        Text("通知栏 ${event.showInNotification.yesNo()} · 锁屏 ${event.showOnLockScreen.yesNo()} · ${EventDisplayText.repeatLabel(event.repeatKind, event.repeatValue)}")
                    }
                    IconButton(onClick = { applyDraft(EventDraft.fromEvent(event)) }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { onDelete(event) }) { Icon(Icons.Default.Delete, contentDescription = "删除") }
                }
            }
        }
    }
}

@Composable
private fun EventCard(content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}

private fun EventKind.label(): String = EventDisplayText.kindLabel(this)

@Composable
private fun RepeatChipRow(
    options: List<EventRepeatKind>,
    selected: EventRepeatKind,
    repeatValue: Int,
    onSelected: (EventRepeatKind) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelected(option) },
                label = { Text(EventDisplayText.repeatLabel(option, repeatValue)) },
            )
        }
    }
}

private fun Boolean.yesNo(): String = if (this) "已开启" else "未开启"
