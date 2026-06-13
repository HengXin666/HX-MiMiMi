package com.hxmimimi.alarmcalendar.model

import java.time.DayOfWeek

data class AlarmDraft(
    val id: Long = 0,
    val title: String = "早安闹钟",
    val hourText: String = "7",
    val minuteText: String = "30",
    val snoozeGuardText: String = "8",
    val dismissScoreText: String = "80",
    val awakeScoreText: String = "120",
    val confirmSecondsText: String = "20",
    val welcomeMessage: String = "起床先喝杯水吧",
    val repeatKind: AlarmRepeatKind = AlarmRepeatKind.WEEKLY,
    val repeatDays: Set<DayOfWeek>? = null,
    val enabled: Boolean = true,
    val ringtoneUri: String? = null,
) {
    fun toAlarm(): Alarm = Alarm(
        id = id,
        title = title.ifBlank { DEFAULT_TITLE },
        hour = hourText.toIntOrNull()?.takeIf { it in 0..23 } ?: DEFAULT_HOUR,
        minute = minuteText.toIntOrNull()?.takeIf { it in 0..59 } ?: DEFAULT_MINUTE,
        repeatKind = repeatKind,
        repeatDays = when (repeatKind) {
            AlarmRepeatKind.WEEKLY -> repeatDays?.takeIf { it.isNotEmpty() } ?: DayOfWeek.entries.toSet()
            else -> emptySet()
        },
        snoozeGuardMinutes = snoozeGuardText.toIntOrNull()?.coerceAtLeast(1) ?: DEFAULT_GUARD_MINUTES,
        dismissMovementScore = dismissScoreText.toFloatOrNull()?.coerceIn(20f, 300f) ?: DEFAULT_DISMISS_SCORE,
        awakeMovementScore = awakeScoreText.toFloatOrNull()?.coerceIn(40f, 500f) ?: DEFAULT_AWAKE_SCORE,
        awakeConfirmSeconds = confirmSecondsText.toIntOrNull()?.coerceIn(5, 90) ?: DEFAULT_CONFIRM_SECONDS,
        welcomeMessage = welcomeMessage.ifBlank { DEFAULT_WELCOME },
        enabled = enabled,
        ringtoneUri = ringtoneUri,
    )

    companion object {
        private const val DEFAULT_TITLE = "闹钟"
        private const val DEFAULT_HOUR = 7
        private const val DEFAULT_MINUTE = 30
        private const val DEFAULT_GUARD_MINUTES = 8
        private const val DEFAULT_DISMISS_SCORE = 80f
        private const val DEFAULT_AWAKE_SCORE = 120f
        private const val DEFAULT_CONFIRM_SECONDS = 20
        private const val DEFAULT_WELCOME = "起床先喝杯水吧"

        fun fromAlarm(alarm: Alarm): AlarmDraft = AlarmDraft(
            id = alarm.id,
            title = alarm.title,
            hourText = alarm.hour.toString(),
            minuteText = alarm.minute.toString(),
            snoozeGuardText = alarm.snoozeGuardMinutes.toString(),
            dismissScoreText = "%.0f".format(alarm.dismissMovementScore),
            awakeScoreText = "%.0f".format(alarm.awakeMovementScore),
            confirmSecondsText = alarm.awakeConfirmSeconds.toString(),
            welcomeMessage = alarm.welcomeMessage,
            repeatKind = alarm.repeatKind,
            repeatDays = alarm.repeatDays,
            enabled = alarm.enabled,
            ringtoneUri = alarm.ringtoneUri,
        )
    }
}
