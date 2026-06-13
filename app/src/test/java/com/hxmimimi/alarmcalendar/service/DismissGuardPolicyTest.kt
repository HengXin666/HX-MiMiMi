package com.hxmimimi.alarmcalendar.service

import org.junit.Assert.assertEquals
import org.junit.Test

class DismissGuardPolicyTest {
    private val policy = DismissGuardPolicy(awakeScoreThreshold = 80f)

    @Test
    fun manualDismissIsBlockedWhenMovementIsInsufficient() {
        assertEquals(
            DismissGuardDecision.KEEP_RINGING,
            policy.decideManualDismiss(movementScore = 79.9f),
        )
    }

    @Test
    fun manualDismissIsAllowedAfterEnoughMovement() {
        assertEquals(
            DismissGuardDecision.DISMISS,
            policy.decideManualDismiss(movementScore = 80f),
        )
    }

    @Test
    fun manualDismissThresholdIsConfigurable() {
        val stricterPolicy = DismissGuardPolicy(awakeScoreThreshold = 150f)

        assertEquals(DismissGuardDecision.KEEP_RINGING, stricterPolicy.decideManualDismiss(movementScore = 120f))
        assertEquals(DismissGuardDecision.DISMISS, stricterPolicy.decideManualDismiss(movementScore = 150f))
    }
}
