package com.hxmimimi.alarmcalendar.service

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmForegroundPolicyTest {
    @Test
    fun specialUseOnlyWhenLocationIsMissing() {
        assertEquals(
            AlarmForegroundType.SPECIAL_USE,
            AlarmForegroundPolicy.foregroundType(hasLocationPermission = false),
        )
    }

    @Test
    fun includesLocationWhenPermissionIsGranted() {
        assertEquals(
            AlarmForegroundType.SPECIAL_USE_WITH_LOCATION,
            AlarmForegroundPolicy.foregroundType(hasLocationPermission = true),
        )
    }
}
