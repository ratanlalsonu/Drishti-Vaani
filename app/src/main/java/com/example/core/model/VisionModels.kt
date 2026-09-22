package com.example.core.model

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
    VERY_CLOSE("appears very close", "bahut paas hai"),
    NEARBY("is nearby", "paas mein hai"),
    FAR("ahead in the distance", "door samne hai")
}

data class DetectedObject(
    val label: String,
    val hindiLabel: String,
    val confidence: Float,
    val boundingBox: android.graphics.RectF,
    val position: SpatialPosition,
    val proximity: ProximityTier,
    val priority: PriorityLevel,
    val timestamp: Long = System.currentTimeMillis()
)
