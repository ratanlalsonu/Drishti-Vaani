package com.example.feature.vision

import com.example.core.model.DetectedObject
import com.example.core.model.PriorityLevel
import java.util.concurrent.ConcurrentHashMap

class SpeechThrottler(
    private val defaultCooldownMs: Long = 4000L,
    private val criticalCooldownMs: Long = 1500L
) {
    // Cache tracking the last spoken timestamp for each object signature (label + position)
    private val lastAnnouncedMap = ConcurrentHashMap<String, Long>()

    /**
     * Determines if a detected object should be spoken based on cooldown,
     * position shift, and hazard priority.
     */
    fun shouldAnnounce(obj: DetectedObject): Boolean {
        val now = System.currentTimeMillis()
        val signature = "${obj.label.lowercase()}_${obj.position.name}"
        val lastTime = lastAnnouncedMap[signature] ?: 0L

        val cooldown = when (obj.priority) {
            PriorityLevel.CRITICAL -> criticalCooldownMs
            PriorityLevel.HIGH -> 3000L
            else -> defaultCooldownMs
        }

        if (now - lastTime >= cooldown) {
            lastAnnouncedMap[signature] = now
            return true
        }

        return false
    }

    fun clear() {
        lastAnnouncedMap.clear()
    }
}
