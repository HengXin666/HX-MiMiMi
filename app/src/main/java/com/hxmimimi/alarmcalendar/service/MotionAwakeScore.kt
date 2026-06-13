package com.hxmimimi.alarmcalendar.service

data class MotionAwakeReading(
    val score: Float,
    val isAwake: Boolean,
)

class MotionAwakeScore(
    private val movementDeltaThreshold: Float = DEFAULT_MOVEMENT_DELTA_THRESHOLD,
    private val awakeScoreThreshold: Float = DEFAULT_AWAKE_SCORE_THRESHOLD,
    private val requiredContinuousMillis: Long = DEFAULT_REQUIRED_CONTINUOUS_MILLIS,
) {
    private val evidenceScore = AwakeEvidenceScore(
        accelerometerDeltaThreshold = movementDeltaThreshold,
        awakeScoreThreshold = awakeScoreThreshold,
        requiredContinuousMillis = requiredContinuousMillis,
    )

    fun reset(nowMillis: Long) {
        evidenceScore.reset(nowMillis)
    }

    fun record(x: Float, y: Float, z: Float, nowMillis: Long): MotionAwakeReading =
        evidenceScore.recordAcceleration(x, y, z, nowMillis)

    fun score(): Float = evidenceScore.score()

    companion object {
        const val DEFAULT_MOVEMENT_DELTA_THRESHOLD = 2.2f
        const val DEFAULT_AWAKE_SCORE_THRESHOLD = 120f
        const val DEFAULT_REQUIRED_CONTINUOUS_MILLIS = 20_000L
    }
}
