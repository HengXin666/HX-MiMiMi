package com.hxmimimi.alarmcalendar.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hxmimimi.alarmcalendar.controller.ReliabilityAction
import com.hxmimimi.alarmcalendar.controller.ReliabilityRequirement
import com.hxmimimi.alarmcalendar.controller.ReliabilitySeverity
import com.hxmimimi.alarmcalendar.controller.ReliabilityStatus

@Composable
fun SettingsScreen(
    requirements: List<ReliabilityRequirement>,
    onRequirementAction: (ReliabilityAction) -> Unit,
    onRefreshReliability: () -> Unit,
    onExactSettings: () -> Unit,
    onExportDb: () -> Unit,
    onExportJson: () -> Unit,
) {
    SettingsCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("可靠性", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onRefreshReliability) {
                Icon(Icons.AutoMirrored.Filled.Rule, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("刷新")
            }
        }
        Text("普通划掉后台、系统回收、重启、应用升级、时间或时区变化后会重排；系统强行停止后 Android 会禁止第三方应用自启动。")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            requirements.forEach { requirement ->
                ReliabilityRequirementRow(requirement, onRequirementAction)
            }
        }
        TextButton(onClick = onExactSettings) { Text("系统精确闹钟页") }
    }
    Spacer(Modifier.height(12.dp))
    SettingsCard {
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
private fun ReliabilityRequirementRow(requirement: ReliabilityRequirement, onAction: (ReliabilityAction) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = when (requirement.status) {
                ReliabilityStatus.READY -> Icons.Default.CheckCircle
                ReliabilityStatus.NEEDS_ACTION -> Icons.Default.ReportProblem
                ReliabilityStatus.REVIEW -> Icons.Default.Tune
            },
            contentDescription = null,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(requirement.title, fontWeight = FontWeight.Bold)
            Text("${requirement.status.label()} · ${requirement.severity.label()}", style = MaterialTheme.typography.bodySmall)
            Text(requirement.detail, style = MaterialTheme.typography.bodySmall)
        }
        requirement.action?.let { action ->
            TextButton(onClick = { onAction(action) }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(action.label())
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}

private fun ReliabilityStatus.label(): String = when (this) {
    ReliabilityStatus.READY -> "已就绪"
    ReliabilityStatus.NEEDS_ACTION -> "需处理"
    ReliabilityStatus.REVIEW -> "需确认"
}

private fun ReliabilitySeverity.label(): String = when (this) {
    ReliabilitySeverity.CRITICAL -> "关键"
    ReliabilitySeverity.IMPORTANT -> "重要"
    ReliabilitySeverity.RECOMMENDED -> "建议"
}

private fun ReliabilityAction.label(): String = when (this) {
    ReliabilityAction.OPEN_NOTIFICATION_SETTINGS -> "通知"
    ReliabilityAction.OPEN_LOCATION_SETTINGS -> "定位"
    ReliabilityAction.OPEN_EXACT_ALARM_SETTINGS -> "闹钟"
    ReliabilityAction.OPEN_FULL_SCREEN_INTENT_SETTINGS -> "全屏"
    ReliabilityAction.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> "省电"
    ReliabilityAction.OPEN_OEM_AUTOSTART_SETTINGS -> "自启动"
    ReliabilityAction.OPEN_OEM_LOCK_SCREEN_SETTINGS -> "锁屏"
    ReliabilityAction.OPEN_OEM_BACKGROUND_POPUP_SETTINGS -> "后台弹出"
}
