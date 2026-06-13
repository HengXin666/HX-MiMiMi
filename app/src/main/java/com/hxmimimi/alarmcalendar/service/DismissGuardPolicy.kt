package com.hxmimimi.alarmcalendar.service

enum class DismissGuardDecision {
    DISMISS,
    KEEP_RINGING,
}

class DismissGuardPolicy(
    private val awakeScoreThreshold: Float = DEFAULT_MANUAL_DISMISS_SCORE_THRESHOLD,
) {
    fun decideManualDismiss(movementScore: Float): DismissGuardDecision =
        if (movementScore >= awakeScoreThreshold) {
            DismissGuardDecision.DISMISS
        } else {
            DismissGuardDecision.KEEP_RINGING
        }

    companion object {
        const val DEFAULT_MANUAL_DISMISS_SCORE_THRESHOLD = 80f
    }
}
