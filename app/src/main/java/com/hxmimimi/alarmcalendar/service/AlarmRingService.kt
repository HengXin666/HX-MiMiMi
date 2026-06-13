package com.hxmimimi.alarmcalendar.service

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
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
    private var dismissMovementScore = DismissGuardPolicy.DEFAULT_MANUAL_DISMISS_SCORE_THRESHOLD
    private var awakeMovementScore = MotionAwakeScore.DEFAULT_AWAKE_SCORE_THRESHOLD
    private var dismissGuardPolicy = DismissGuardPolicy()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISS) {
            alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            dismissByUser()
            return START_NOT_STICKY
        }
        alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0) ?: 0
        firedAt = System.currentTimeMillis()
        val db = HxDatabase(this)
        val alarm = AlarmRepository(db).byId(alarmId) ?: return START_NOT_STICKY
        dismissMovementScore = alarm.dismissMovementScore
        awakeMovementScore = alarm.awakeMovementScore
        dismissGuardPolicy = DismissGuardPolicy(dismissMovementScore)
        startAlarmForeground(NotificationController(this).alarmNotification(alarmId, alarm.title))
        playRingtone(alarm.ringtoneUri)
        vibrate(longArrayOf(0, 700, 400, 700), repeat = 0)
        publishRingStatus(score = 0f, message = "起身活动后才能关闭闹钟")
        detector = MotionAwakeDetector(
            context = this,
            awakeScoreThreshold = awakeMovementScore,
            requiredContinuousMillis = alarm.awakeConfirmSeconds.coerceAtLeast(1) * 1_000L,
            onReading = { reading ->
                publishRingStatus(
                    score = reading.score,
                    message = if (reading.score >= dismissMovementScore) {
                        "运动分数已达到手动关闭线，请保持活动等待确认"
                    } else {
                        "继续走动，运动不足时无法关闭"
                    },
                )
            },
        ) { score ->
            vibrate(longArrayOf(0, 180, 120, 180), repeat = -1)
            NotificationController(this).showStatus(30_000 + alarmId.toInt(), "闹钟已关闭", "今日「${alarm.title}」已关闭。${alarm.welcomeMessage}")
            publishRingStatus(score = score, message = "闹钟已关闭")
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

    private fun startAlarmForeground(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundType = when (AlarmForegroundPolicy.foregroundType(hasLocationPermission())) {
                AlarmForegroundType.SPECIAL_USE -> ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                AlarmForegroundType.SPECIAL_USE_WITH_LOCATION ->
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            }
            startForeground(NOTIFICATION_ID, notification, foregroundType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

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
        if (dismissGuardPolicy.decideManualDismiss(score) == DismissGuardDecision.KEEP_RINGING) {
            oversleepReminders += 1
            NotificationController(this).showStatus(40_000 + alarmId.toInt(), "防赖床提醒", "${minutes} 分钟内动静不足，闹钟继续提醒")
            publishRingStatus(score = score, message = "${minutes} 分钟内动静不足，闹钟继续提醒")
            vibrate(longArrayOf(0, 1000, 300, 1000), repeat = -1)
            handler.postDelayed({ checkOversleep(minutes) }, minutes.coerceAtLeast(1) * 60_000L)
        }
    }

    private fun dismissByUser() {
        val score = detector?.movementScore() ?: 0f
        when (dismissGuardPolicy.decideManualDismiss(score)) {
            DismissGuardDecision.DISMISS -> recordAndStop(score)
            DismissGuardDecision.KEEP_RINGING -> {
                oversleepReminders += 1
                val message = "当前运动分数 ${"%.0f".format(score)}，还不能关闭"
                NotificationController(this).showStatus(
                    40_000 + alarmId.toInt(),
                    "还不能关闭",
                    "需要起身活动后才能关闭闹钟。$message",
                )
                publishRingStatus(score = score, message = message)
                vibrate(longArrayOf(0, 600, 200, 600), repeat = -1)
            }
        }
    }

    private fun recordAndStop(score: Float) {
        val db = HxDatabase(this)
        StatsRepository(db).insert(RingStat(alarmId = alarmId, firedAtMillis = firedAt, dismissedAtMillis = System.currentTimeMillis(), movementScore = score, oversleepReminders = oversleepReminders))
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun publishRingStatus(score: Float, message: String) {
        sendBroadcast(
            Intent(ACTION_RING_STATUS)
                .setPackage(packageName)
                .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                .putExtra(EXTRA_MOVEMENT_SCORE, score)
                .putExtra(EXTRA_DISMISS_SCORE, dismissMovementScore)
                .putExtra(EXTRA_AWAKE_SCORE, awakeMovementScore)
                .putExtra(EXTRA_OVERSLEEP_REMINDERS, oversleepReminders)
                .putExtra(EXTRA_RING_STATUS_MESSAGE, message),
        )
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
        const val ACTION_RING_STATUS = "com.hxmimimi.alarmcalendar.action.RING_STATUS"
        const val EXTRA_MOVEMENT_SCORE = "movement_score"
        const val EXTRA_DISMISS_SCORE = "dismiss_score"
        const val EXTRA_AWAKE_SCORE = "awake_score"
        const val EXTRA_OVERSLEEP_REMINDERS = "oversleep_reminders"
        const val EXTRA_RING_STATUS_MESSAGE = "ring_status_message"
        private const val NOTIFICATION_ID = 10_001

        fun start(context: android.content.Context, alarmId: Long) {
            val intent = Intent(context, AlarmRingService::class.java).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
