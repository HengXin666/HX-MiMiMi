package com.hxmimimi.alarmcalendar.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hxmimimi.alarmcalendar.receiver.AlarmReceiver
import com.hxmimimi.alarmcalendar.model.AlarmRingStatusText
import com.hxmimimi.alarmcalendar.service.AlarmRingService
import com.hxmimimi.alarmcalendar.view.theme.HxTheme

class AlarmRingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }
        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0)
        setContent {
            HxTheme {
                var movementScore by remember { mutableFloatStateOf(0f) }
                var dismissScore by remember { mutableFloatStateOf(80f) }
                var awakeScore by remember { mutableFloatStateOf(120f) }
                var oversleepReminders by remember { mutableIntStateOf(0) }
                var statusMessage by remember { mutableStateOf("起身活动后才能关闭闹钟") }
                DisposableEffect(alarmId) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            if (intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0) != alarmId) return
                            movementScore = intent.getFloatExtra(AlarmRingService.EXTRA_MOVEMENT_SCORE, movementScore)
                            dismissScore = intent.getFloatExtra(AlarmRingService.EXTRA_DISMISS_SCORE, dismissScore)
                            awakeScore = intent.getFloatExtra(AlarmRingService.EXTRA_AWAKE_SCORE, awakeScore)
                            oversleepReminders = intent.getIntExtra(AlarmRingService.EXTRA_OVERSLEEP_REMINDERS, oversleepReminders)
                            statusMessage = intent.getStringExtra(AlarmRingService.EXTRA_RING_STATUS_MESSAGE) ?: statusMessage
                        }
                    }
                    ContextCompat.registerReceiver(
                        this@AlarmRingActivity,
                        receiver,
                        IntentFilter(AlarmRingService.ACTION_RING_STATUS),
                        ContextCompat.RECEIVER_NOT_EXPORTED,
                    )
                    onDispose { unregisterReceiver(receiver) }
                }
                val progressPercent = AlarmRingStatusText.progressPercent(movementScore, dismissScore)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(28.dp),
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null)
                        Text("起床闹钟", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text("起身活动一段时间后，闹钟会自动确认关闭。")
                        Text("运动不足时，按钮只会暂缓提示，不会停止防赖床检查。", style = MaterialTheme.typography.bodyMedium)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            LinearProgressIndicator(
                                progress = { progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                "${"%.0f".format(movementScore)} / ${"%.0f".format(dismissScore)} 关闭分 · ${"%.0f".format(awakeScore)} 确认分",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(AlarmRingStatusText.status(movementScore, dismissScore), style = MaterialTheme.typography.bodyMedium)
                            Text(statusMessage, style = MaterialTheme.typography.bodySmall)
                            if (oversleepReminders > 0) {
                                Text("已提醒 $oversleepReminders 次", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { finish() }) {
                                Text("返回锁屏")
                            }
                            Button(onClick = {
                                startService(
                                    Intent(this@AlarmRingActivity, AlarmRingService::class.java)
                                        .setAction(AlarmRingService.ACTION_DISMISS)
                                        .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId),
                                )
                            }) {
                                Icon(Icons.Default.AlarmOff, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("尝试关闭")
                            }
                        }
                    }
                }
            }
        }
    }
}
