package com.example.feature.vision

import com.example.core.model.DetectedObject
import com.example.core.model.PriorityLevel
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance throttler designed specifically for real-time visual assistance.
 * Ensures immediate announcement when a new obstacle enters the field of view or
 * when an obstacle approaches closer, while avoiding repetitive speech spam.
 */
class SpeechThrottler(
    private val defaultCooldownMs: Long = 1800L,
    private val criticalCooldownMs: Long = 800L
) {
    data class SpokenState(
        val timestamp: Long,
        val distanceMeters: Float,
        val priority: PriorityLevel
    )

    private val lastAnnouncedMap = ConcurrentHashMap<String, SpokenState>()
    private var lastSpokenLabel: String = ""
    private var lastSpokenTimestamp: Long = 0L

    /**
     * Determines if a detected object should be spoken based on cooldown,
     * position shift, distance changes, and hazard priority.
     */
    fun shouldAnnounce(obj: DetectedObject): Boolean {
        val now = System.currentTimeMillis()
        val signature = "${obj.label.lowercase()}_${obj.position.name}"
        val prevState = lastAnnouncedMap[signature]

        // 1. Critical collision hazards (< 1.2m) announce with highest immediacy
        if (obj.priority == PriorityLevel.CRITICAL) {
            if (prevState == null || (now - prevState.timestamp >= criticalCooldownMs) || (prevState.distanceMeters - obj.estimatedDistanceMeters > 0.35f)) {
                lastAnnouncedMap[signature] = SpokenState(now, obj.estimatedDistanceMeters, obj.priority)
                lastSpokenLabel = obj.label
                lastSpokenTimestamp = now
                return true
            }
            return false
        }

        // 2. If a completely new object or changed object is in view, announce with minimal transition gap (500ms)
        if (obj.label != lastSpokenLabel && (now - lastSpokenTimestamp >= 500L)) {
            lastAnnouncedMap[signature] = SpokenState(now, obj.estimatedDistanceMeters, obj.priority)
            lastSpokenLabel = obj.label
            lastSpokenTimestamp = now
            return true
        }

        // 3. If obstacle approaches significantly closer (getting closer by > 0.8m)
        if (prevState != null && (prevState.distanceMeters - obj.estimatedDistanceMeters > 0.8f) && (now - prevState.timestamp >= 900L)) {
            lastAnnouncedMap[signature] = SpokenState(now, obj.estimatedDistanceMeters, obj.priority)
            lastSpokenLabel = obj.label
            lastSpokenTimestamp = now
            return true
        }

        // 4. Standard cooldown for stationary objects
        val cooldown = when (obj.priority) {
            PriorityLevel.HIGH -> 1400L
            else -> defaultCooldownMs
        }

        if (prevState == null || (now - prevState.timestamp >= cooldown)) {
            lastAnnouncedMap[signature] = SpokenState(now, obj.estimatedDistanceMeters, obj.priority)
            lastSpokenLabel = obj.label
            lastSpokenTimestamp = now
            return true
        }

        return false
    }

    fun clear() {
        lastAnnouncedMap.clear()
        lastSpokenLabel = ""
        lastSpokenTimestamp = 0L
    }
}
