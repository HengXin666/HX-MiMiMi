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
import androidx.compose.material.icons.filled.MusicNote
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
import com.hxmimimi.alarmcalendar.model.Alarm
import com.hxmimimi.alarmcalendar.model.AlarmDisplayText
import com.hxmimimi.alarmcalendar.model.AlarmDraft
import com.hxmimimi.alarmcalendar.model.AlarmRepeatKind
import java.time.DayOfWeek

@Composable
fun AlarmScreen(
    alarms: List<Alarm>,
    onAdd: (Alarm) -> Unit,
    onToggle: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
    onPickAudio: () -> Unit,
) {
    var title by remember { mutableStateOf("早安闹钟") }
    var hour by remember { mutableStateOf("7") }
    var minute by remember { mutableStateOf("30") }
    var guard by remember { mutableStateOf("8") }
    var dismissScore by remember { mutableStateOf("80") }
    var awakeScore by remember { mutableStateOf("120") }
    var confirmSeconds by remember { mutableStateOf("20") }
    var welcome by remember { mutableStateOf("起床先喝杯水吧") }
    var repeatKind by remember { mutableStateOf(AlarmRepeatKind.WEEKLY) }
    var repeatDays by remember { mutableStateOf(DayOfWeek.entries.toSet()) }
    var editingAlarmId by remember { mutableStateOf(0L) }
    var editingEnabled by remember { mutableStateOf(true) }
    var editingRingtoneUri by remember { mutableStateOf<String?>(null) }
    fun applyDraft(draft: AlarmDraft) {
        editingAlarmId = draft.id
        editingEnabled = draft.enabled
        editingRingtoneUri = draft.ringtoneUri
        title = draft.title
        hour = draft.hourText
        minute = draft.minuteText
        guard = draft.snoozeGuardText
        dismissScore = draft.dismissScoreText
        awakeScore = draft.awakeScoreText
        confirmSeconds = draft.confirmSecondsText
        welcome = draft.welcomeMessage
        repeatKind = draft.repeatKind
        repeatDays = draft.repeatDays ?: DayOfWeek.entries.toSet()
    }
    AlarmCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (editingAlarmId == 0L) "新增闹钟" else "编辑闹钟", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            if (editingAlarmId != 0L) {
                IconButton(onClick = {
                    applyDraft(AlarmDraft())
                }) {
                    Icon(Icons.Default.Close, contentDescription = "取消编辑")
                }
            }
        }
        TextField(value = title, onValueChange = { title = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(value = hour, onValueChange = { hour = it.filter(Char::isDigit).take(2) }, label = { Text("时") }, modifier = Modifier.weight(1f))
            TextField(value = minute, onValueChange = { minute = it.filter(Char::isDigit).take(2) }, label = { Text("分") }, modifier = Modifier.weight(1f))
            TextField(value = guard, onValueChange = { guard = it.filter(Char::isDigit).take(2) }, label = { Text("防赖床") }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(value = dismissScore, onValueChange = { dismissScore = it.filter(Char::isDigit).take(3) }, label = { Text("关闭分") }, modifier = Modifier.weight(1f))
            TextField(value = awakeScore, onValueChange = { awakeScore = it.filter(Char::isDigit).take(3) }, label = { Text("确认分") }, modifier = Modifier.weight(1f))
            TextField(value = confirmSeconds, onValueChange = { confirmSeconds = it.filter(Char::isDigit).take(2) }, label = { Text("确认秒") }, modifier = Modifier.weight(1f))
        }
        TextField(value = welcome, onValueChange = { welcome = it }, label = { Text("醒来提示") }, modifier = Modifier.fillMaxWidth())
        if (repeatKind == AlarmRepeatKind.WEEKLY) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.entries.forEach { day ->
                    FilterChip(
                        selected = day in repeatDays,
                        onClick = {
                            repeatDays = if (day in repeatDays) repeatDays - day else repeatDays + day
                        },
                        label = { Text(AlarmDisplayText.dayLabel(day)) },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { repeatKind = repeatKind.next() }) { Text(AlarmDisplayText.repeatLabel(repeatKind)) }
            Button(onClick = onPickAudio) {
                Icon(Icons.Default.MusicNote, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("铃声")
            }
            Button(onClick = {
                onAdd(
                    AlarmDraft(
                        id = editingAlarmId,
                        title = title,
                        hourText = hour,
                        minuteText = minute,
                        snoozeGuardText = guard,
                        dismissScoreText = dismissScore,
                        awakeScoreText = awakeScore,
                        confirmSecondsText = confirmSeconds,
                        welcomeMessage = welcome,
                        repeatKind = repeatKind,
                        repeatDays = repeatDays,
                        enabled = editingEnabled,
                        ringtoneUri = editingRingtoneUri,
                    ).toAlarm(),
                )
                applyDraft(AlarmDraft())
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (editingAlarmId == 0L) "添加" else "保存")
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(alarms, key = { it.id }) { alarm ->
            AlarmCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(AlarmDisplayText.timeText(alarm.hour, alarm.minute), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("${alarm.title} · ${AlarmDisplayText.repeatDaysText(alarm)}")
                        Text(AlarmDisplayText.guardSummary(alarm))
                        alarm.nextAt?.let { Text("下次 $it", style = MaterialTheme.typography.bodySmall) }
                    }
                    Switch(checked = alarm.enabled, onCheckedChange = { onToggle(alarm) })
                    IconButton(onClick = {
                        applyDraft(AlarmDraft.fromAlarm(alarm))
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { onDelete(alarm) }) { Icon(Icons.Default.Delete, contentDescription = "删除") }
                }
            }
        }
    }
}

@Composable
private fun AlarmCard(content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}

private fun AlarmRepeatKind.next(): AlarmRepeatKind = when (this) {
    AlarmRepeatKind.ONCE -> AlarmRepeatKind.WEEKLY
    AlarmRepeatKind.WEEKLY -> AlarmRepeatKind.CHINESE_WORKDAY
    AlarmRepeatKind.CHINESE_WORKDAY -> AlarmRepeatKind.CHINESE_HOLIDAY
    AlarmRepeatKind.CHINESE_HOLIDAY -> AlarmRepeatKind.ONCE
}
