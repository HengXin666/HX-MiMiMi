package com.hxmimimi.alarmcalendar.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hxmimimi.alarmcalendar.model.RingStat

@Composable
fun StatsScreen(stats: List<RingStat>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(stats, key = { it.id }) {
            StatsCard {
                Text("闹钟 #${it.alarmId}", fontWeight = FontWeight.Bold)
                Text("运动分数 ${"%.1f".format(it.movementScore)} · 防赖床提醒 ${it.oversleepReminders} 次")
                Text("响铃 ${java.time.Instant.ofEpochMilli(it.firedAtMillis)}")
            }
        }
    }
}

@Composable
private fun StatsCard(content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}
