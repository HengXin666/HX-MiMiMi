package com.hxmimimi.alarmcalendar.view

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hxmimimi.alarmcalendar.receiver.AlarmReceiver
import com.hxmimimi.alarmcalendar.service.AlarmRingService
import com.hxmimimi.alarmcalendar.view.theme.HxTheme

class AlarmRingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        )
        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0)
        setContent {
            HxTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("闹钟响铃", style = MaterialTheme.typography.displaySmall)
                    Text("持续活动后会自动确认醒来")
                    Button(onClick = {
                        startService(Intent(this@AlarmRingActivity, AlarmRingService::class.java).setAction(AlarmRingService.ACTION_DISMISS))
                        finish()
                    }) {
                        Icon(Icons.Default.AlarmOff, contentDescription = null)
                        Text("关闭")
                    }
                }
            }
        }
    }
}
