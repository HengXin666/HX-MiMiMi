package com.hxmimimi.alarmcalendar.controller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReliabilityAdvisorTest {
    private val advisor = ReliabilityAdvisor()

    @Test
    fun readyDeviceHasNoBlockingRequirements() {
        val requirements = advisor.assess(
            ReliabilitySignals(
                sdkInt = 35,
                manufacturer = "Google",
                notificationsGranted = true,
                locationGranted = true,
                exactAlarmAllowed = true,
                fullScreenIntentAllowed = true,
                ignoringBatteryOptimizations = true,
            ),
        )

        assertFalse(requirements.any { it.status == ReliabilityStatus.NEEDS_ACTION })
    }

    @Test
    fun deniedExactAlarmIsCritical() {
        val requirements = advisor.assess(
            ReliabilitySignals(
                sdkInt = 35,
                manufacturer = "Google",
                notificationsGranted = true,
                locationGranted = true,
                exactAlarmAllowed = false,
                fullScreenIntentAllowed = true,
                ignoringBatteryOptimizations = true,
            ),
        )

        val exactAlarm = requirements.single { it.id == ReliabilityRequirementId.EXACT_ALARM }
        assertEquals(ReliabilityStatus.NEEDS_ACTION, exactAlarm.status)
        assertEquals(ReliabilitySeverity.CRITICAL, exactAlarm.severity)
        assertEquals(ReliabilityAction.OPEN_EXACT_ALARM_SETTINGS, exactAlarm.action)
    }

    @Test
    fun android14FullScreenIntentCanRequireAction() {
        val requirements = advisor.assess(
            ReliabilitySignals(
                sdkInt = 34,
                manufacturer = "Google",
                notificationsGranted = true,
                locationGranted = true,
                exactAlarmAllowed = true,
                fullScreenIntentAllowed = false,
                ignoringBatteryOptimizations = true,
            ),
        )

        val fullScreen = requirements.single { it.id == ReliabilityRequirementId.FULL_SCREEN_INTENT }
        assertEquals(ReliabilityStatus.NEEDS_ACTION, fullScreen.status)
        assertEquals(ReliabilityAction.OPEN_FULL_SCREEN_INTENT_SETTINGS, fullScreen.action)
    }

    @Test
    fun xiaomiDevicesAddManualAutostartReview() {
        val requirements = advisor.assess(
            ReliabilitySignals(
                sdkInt = 35,
                manufacturer = "Xiaomi",
                notificationsGranted = true,
                locationGranted = true,
                exactAlarmAllowed = true,
                fullScreenIntentAllowed = true,
                ignoringBatteryOptimizations = false,
            ),
        )

        assertTrue(requirements.any { it.id == ReliabilityRequirementId.OEM_AUTOSTART })
        assertEquals(
            ReliabilitySeverity.IMPORTANT,
            requirements.single { it.id == ReliabilityRequirementId.BATTERY_OPTIMIZATION }.severity,
        )
    }

    @Test
    fun chineseOemsAddLockScreenAndBackgroundPopupReviews() {
        val requirements = advisor.assess(
            ReliabilitySignals(
                sdkInt = 35,
                manufacturer = "Xiaomi",
                notificationsGranted = true,
                locationGranted = true,
                exactAlarmAllowed = true,
                fullScreenIntentAllowed = true,
                ignoringBatteryOptimizations = true,
            ),
        )

        assertEquals(ReliabilityStatus.REVIEW, requirements.single { it.id == ReliabilityRequirementId.OEM_AUTOSTART }.status)
        assertEquals(ReliabilityStatus.REVIEW, requirements.single { it.id == ReliabilityRequirementId.OEM_LOCK_SCREEN }.status)
        assertEquals(ReliabilityStatus.REVIEW, requirements.single { it.id == ReliabilityRequirementId.OEM_BACKGROUND_POPUP }.status)
        assertTrue(requirements.single { it.id == ReliabilityRequirementId.OEM_LOCK_SCREEN }.detail.contains("锁屏"))
        assertTrue(requirements.single { it.id == ReliabilityRequirementId.OEM_BACKGROUND_POPUP }.detail.contains("后台弹出"))
    }

    @Test
    fun nonChineseOemsDoNotShowManualRomReviews() {
        val requirements = advisor.assess(
            ReliabilitySignals(
                sdkInt = 35,
                manufacturer = "Google",
                notificationsGranted = true,
                locationGranted = true,
                exactAlarmAllowed = true,
                fullScreenIntentAllowed = true,
                ignoringBatteryOptimizations = true,
            ),
        )

        assertFalse(requirements.any { it.id == ReliabilityRequirementId.OEM_AUTOSTART })
        assertFalse(requirements.any { it.id == ReliabilityRequirementId.OEM_LOCK_SCREEN })
        assertFalse(requirements.any { it.id == ReliabilityRequirementId.OEM_BACKGROUND_POPUP })
    }

    @Test
    fun missingLocationPermissionIsImportantForWakeEvidence() {
        val requirements = advisor.assess(
            ReliabilitySignals(
                sdkInt = 35,
                manufacturer = "Google",
                notificationsGranted = true,
                locationGranted = false,
                exactAlarmAllowed = true,
                fullScreenIntentAllowed = true,
                ignoringBatteryOptimizations = true,
            ),
        )

        val location = requirements.single { it.id == ReliabilityRequirementId.LOCATION }
        assertEquals(ReliabilityStatus.NEEDS_ACTION, location.status)
        assertEquals(ReliabilitySeverity.IMPORTANT, location.severity)
        assertEquals(ReliabilityAction.OPEN_LOCATION_SETTINGS, location.action)
    }
}
