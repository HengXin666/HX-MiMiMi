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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hxmimimi.alarmcalendar.controller.AlarmScheduler
import com.hxmimimi.alarmcalendar.controller.EventScheduler
import com.hxmimimi.alarmcalendar.controller.PermissionReliabilityController
import com.hxmimimi.alarmcalendar.data.AlarmRepository
import com.hxmimimi.alarmcalendar.data.EventRepository
import com.hxmimimi.alarmcalendar.data.ExportRepository
import com.hxmimimi.alarmcalendar.data.HxDatabase
import com.hxmimimi.alarmcalendar.data.StatsRepository
import com.hxmimimi.alarmcalendar.model.Alarm
import com.hxmimimi.alarmcalendar.view.AlarmScreen
import com.hxmimimi.alarmcalendar.view.EventScreen
import com.hxmimimi.alarmcalendar.view.SettingsScreen
import com.hxmimimi.alarmcalendar.view.StatsScreen
import com.hxmimimi.alarmcalendar.view.theme.HxTheme

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
    val reliabilityController = remember { PermissionReliabilityController(context) }
    var tab by remember { mutableIntStateOf(0) }
    var alarms by remember { mutableStateOf(alarmRepo.alarms()) }
    var events by remember { mutableStateOf(eventRepo.events()) }
    var stats by remember { mutableStateOf(statsRepo.latest()) }
    var reliabilityRequirements by remember { mutableStateOf(reliabilityController.requirements()) }
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
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!locationGranted) {
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
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
                    val alarmToSave = if (alarm.id == 0L) {
                        alarm.copy(ringtoneUri = alarm.ringtoneUri ?: ringtone?.toString())
                    } else {
                        alarm
                    }
                    val id = alarmRepo.upsert(
                        alarmToSave,
                    )
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
                    if (event.id != 0L) eventScheduler.cancel(event.id)
                    val id = eventRepo.upsert(event)
                    eventRepo.byId(id)?.takeIf { !it.completed && it.showInNotification }?.let(eventScheduler::schedule)
                    events = eventRepo.events()
                }, onDelete = {
                    eventScheduler.cancel(it.id)
                    eventRepo.delete(it.id)
                    events = eventRepo.events()
                })
                2 -> StatsScreen(stats)
                3 -> SettingsScreen(
                    requirements = reliabilityRequirements,
                    onRequirementAction = { action -> context.startActivity(reliabilityController.intentFor(action)) },
                    onRefreshReliability = { reliabilityRequirements = reliabilityController.requirements() },
                    onExactSettings = { context.startActivity(alarmScheduler.exactAlarmSettingsIntent()) },
                    onExportDb = { exportDb.launch("hx_mimimi.db") },
                    onExportJson = { exportJson.launch("hx_mimimi.json") },
                )
            }
        }
    }
}
