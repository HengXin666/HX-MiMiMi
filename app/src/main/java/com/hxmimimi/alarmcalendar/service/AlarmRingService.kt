package com.hxmimimi.alarmcalendar.service

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.hxmimimi.alarmcalendar.controller.AlarmScheduler
import com.hxmimimi.alarmcalendar.controller.NotificationController
import com.hxmimimi.alarmcalendar.data.AlarmRepository
import com.hxmimimi.alarmcalendar.data.HxDatabase
import com.hxmimimi.alarmcalendar.data.StatsRepository
import com.hxmimimi.alarmcalendar.model.AlarmRepeatKind
import com.hxmimimi.alarmcalendar.model.RingStat
import com.hxmimimi.alarmcalendar.receiver.AlarmReceiver

class AlarmRingService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var detector: MotionAwakeDetector? = null
    private var alarmId: Long = 0
    private var firedAt: Long = 0
    private var oversleepReminders = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISS) {
            dismissByUser()
            return START_NOT_STICKY
        }
        alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0) ?: 0
        firedAt = System.currentTimeMillis()
        val db = HxDatabase(this)
        val alarm = AlarmRepository(db).byId(alarmId) ?: return START_NOT_STICKY
        startForeground(NOTIFICATION_ID, NotificationController(this).alarmNotification(alarmId, alarm.title))
        playRingtone(alarm.ringtoneUri)
        vibrate(longArrayOf(0, 700, 400, 700), repeat = 0)
        detector = MotionAwakeDetector(this) { score ->
            vibrate(longArrayOf(0, 180, 120, 180), repeat = -1)
            NotificationController(this).showStatus(30_000 + alarmId.toInt(), "闹钟已关闭", "今日「${alarm.title}」已关闭。${alarm.welcomeMessage}")
            recordAndStop(score)
        }.also { it.start() }
        handler.postDelayed({ checkOversleep(alarm.snoozeGuardMinutes) }, alarm.snoozeGuardMinutes.coerceAtLeast(1) * 60_000L)
        val alarmRepo = AlarmRepository(db)
        if (alarm.repeatKind == AlarmRepeatKind.ONCE) {
            alarmRepo.upsert(alarm.copy(enabled = false))
        } else {
            AlarmScheduler(this, alarmRepo).schedule(alarm)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        detector?.stop()
        mediaPlayer?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun playRingtone(uriString: String?) {
        val uri = uriString?.let(Uri::parse) ?: android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            setDataSource(this@AlarmRingService, uri)
            isLooping = true
            prepare()
            start()
        }
    }

    private fun checkOversleep(minutes: Int) {
        val score = detector?.movementScore() ?: 0f
        if (score < 80f) {
            oversleepReminders += 1
            NotificationController(this).showStatus(40_000 + alarmId.toInt(), "防赖床提醒", "${minutes} 分钟内动静不足，闹钟继续提醒")
            vibrate(longArrayOf(0, 1000, 300, 1000), repeat = -1)
            handler.postDelayed({ checkOversleep(minutes) }, minutes.coerceAtLeast(1) * 60_000L)
        }
    }

    private fun dismissByUser() {
        recordAndStop(detector?.movementScore() ?: 0f)
    }

    private fun recordAndStop(score: Float) {
        val db = HxDatabase(this)
        StatsRepository(db).insert(RingStat(alarmId = alarmId, firedAtMillis = firedAt, dismissedAtMillis = System.currentTimeMillis(), movementScore = score, oversleepReminders = oversleepReminders))
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun vibrate(pattern: LongArray, repeat: Int) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, repeat)
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.hxmimimi.alarmcalendar.action.DISMISS"
        private const val NOTIFICATION_ID = 10_001

        fun start(context: android.content.Context, alarmId: Long) {
            val intent = Intent(context, AlarmRingService::class.java).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
