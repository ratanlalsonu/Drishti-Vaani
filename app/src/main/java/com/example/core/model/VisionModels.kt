package com.example.core.model

import android.graphics.RectF

enum class PriorityLevel {
    CRITICAL,
    HIGH,
    NORMAL,
    LOW
}

enum class SpatialPosition(val spokenLabelEn: String, val spokenLabelHi: String) {
    LEFT("on your left", "aapke bayein taraf"),
    CENTER("ahead", "samne"),
    RIGHT("on your right", "aapke dayein taraf")
}

enum class ProximityTier(val spokenDescEn: String, val spokenDescHi: String) {
    VERY_CLOSE("very close", "bahut paas hai"),
    NEARBY("nearby", "paas mein hai"),
    FAR("in the distance", "door samne hai")
}

enum class DetectionRangeLimit(val maxMeters: Float, val labelEn: String, val labelHi: String) {
    SHORT_5M(5.0f, "5m (Indoor)", "5 मीटर (अंदर)"),
    STANDARD_10M(10.0f, "10m (Recommended)", "10 मीटर (अनुशंसित)"),
    UNLIMITED(30.0f, "All (Unlimited)", "सभी दूरी")
}

data class DetectedObject(
    val label: String,
    val hindiLabel: String,
    val confidence: Float,
    val boundingBox: RectF,
    val position: SpatialPosition,
    val proximity: ProximityTier,
    val priority: PriorityLevel,
    val estimatedDistanceMeters: Float = 2.0f,
    val distanceDescriptionHi: String = "lagbhag 2 meter",
    val distanceDescriptionEn: String = "approx 2 meters",
    val category: ObjectCategory = ObjectCategory.OBSTACLE,
    val isBeyondThreshold: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
