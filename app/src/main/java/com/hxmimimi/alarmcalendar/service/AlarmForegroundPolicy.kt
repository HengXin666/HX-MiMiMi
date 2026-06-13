package com.hxmimimi.alarmcalendar.service

enum class AlarmForegroundType {
    SPECIAL_USE,
    SPECIAL_USE_WITH_LOCATION,
}

object AlarmForegroundPolicy {
    fun foregroundType(hasLocationPermission: Boolean): AlarmForegroundType =
        if (hasLocationPermission) AlarmForegroundType.SPECIAL_USE_WITH_LOCATION else AlarmForegroundType.SPECIAL_USE
}
