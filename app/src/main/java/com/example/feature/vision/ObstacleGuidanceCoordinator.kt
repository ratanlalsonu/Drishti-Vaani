package com.example.feature.vision

import com.example.core.model.AssistantLanguage
import com.example.core.model.DetectedObject
import com.example.core.model.PriorityLevel
import com.example.core.model.SpatialPosition

data class GuidanceSpeechResult(
    val speech: String,
    val priority: PriorityLevel = PriorityLevel.HIGH,
    val isCollisionAlert: Boolean = false
)

/**
 * Coordinates intelligent voice guidance for visual obstacle avoidance:
 * 1. Strictly tracks only 3 obstacle types: Room Pillars, Chairs, Tables.
 * 2. Issues at most 2 warnings per obstacle encounter.
 * 3. Watches the user until they safely move forward past the obstacle without collision.
 * 4. Measures accurate distance in feet and guides user to move aside by 2 to 3 feet.
 * 5. Handles multiple simultaneous obstacles by warning one-by-one professionally and suggesting the best path.
 * 6. Remains completely silent after warnings and guidance until user clears or a new obstacle emerges.
 * 7. When the path is clear, announces once: "रास्ता आगे एकदम क्लियर है, आप सीधे चल सकते हैं। जैसे ही कोई वस्तु बीच में आती है मैं आपको वार्न करुँगी।"
 */
class ObstacleGuidanceCoordinator {

    // Tracks whether the clear path announcement has been made
    var hasAnnouncedClearPath: Boolean = false
        private set

    // Tracks whether an active obstacle encounter is currently being observed/monitored
    var isWatchingObstacle: Boolean = false
        private set

    // Signature representing the set of active obstacles being monitored
    private var activeObstacleSignature: String = ""

    // Warning counter for the active obstacle encounter (1 or 2, max 2)
    var warningCount: Int = 0
        private set

    // Timestamp when the last warning was vocalized
    private var lastWarningTimestamp: Long = 0L

    // Minimum delay between Warning 1 and Warning 2 (4 seconds)
    private val warningIntervalMs: Long = 4000L

    // Debounce counter for detecting obstacle clearance across consecutive frames
    private var consecutiveClearFrames: Int = 0

    fun reset() {
        hasAnnouncedClearPath = false
        isWatchingObstacle = false
        activeObstacleSignature = ""
        warningCount = 0
        lastWarningTimestamp = 0L
        consecutiveClearFrames = 0
    }

    /**
     * Evaluates live frame detections and returns speech guidance if an announcement is warranted,
     * or null if the system should remain completely quiet.
     */
    fun processDetections(
        detectedObstacles: List<DetectedObject>,
        language: AssistantLanguage
    ): GuidanceSpeechResult? {
        val now = System.currentTimeMillis()

        // ---------------------------------------------------------
        // CASE A: No Obstacles Detected in User's Walking Path
        // ---------------------------------------------------------
        if (detectedObstacles.isEmpty()) {
            // If we were previously watching an obstacle that user was navigating around:
            if (isWatchingObstacle) {
                consecutiveClearFrames++
                // Debounce across at least 2 consecutive clear frames (~500ms) to ensure obstacle was truly cleared
                if (consecutiveClearFrames >= 2) {
                    isWatchingObstacle = false
                    activeObstacleSignature = ""
                    warningCount = 0
                    consecutiveClearFrames = 0
                    hasAnnouncedClearPath = true

                    val speech = if (language == AssistantLanguage.HINDI) {
                        "बहुत बढ़िया, आप बाधा पार कर चुके हैं। रास्ता आगे एकदम क्लियर है, आप सीधे चल सकते हैं।"
                    } else {
                        "Well done, you have safely moved past the obstacle. The path ahead is clear, you can walk straight."
                    }
                    return GuidanceSpeechResult(speech, PriorityLevel.HIGH)
                }
                return null // Debouncing clearance
            }

            // If path was never announced as clear yet, announce once as per requirement:
            if (!hasAnnouncedClearPath) {
                hasAnnouncedClearPath = true
                val speech = if (language == AssistantLanguage.HINDI) {
                    "रास्ता आगे एकदम क्लियर है, आप सीधे चल सकते हैं। जैसे ही कोई वस्तु बीच में आती है मैं आपको वार्न करुँगी।"
                } else {
                    "The path ahead is completely clear, you can walk straight. As soon as any object comes in the way, I will warn you."
                }
                return GuidanceSpeechResult(speech, PriorityLevel.HIGH)
            }

            // Path is already clear and announced -> Remain completely SILENT
            return null
        }

        // ---------------------------------------------------------
        // CASE B: Obstacles ARE Detected (Chair, Table, or Pillar)
        // ---------------------------------------------------------
        consecutiveClearFrames = 0

        // Create a signature of the current obstacle set (e.g. "chair_center_3ft|table_left_4ft")
        val currentSignature = detectedObstacles
            .sortedBy { it.distanceFeet }
            .joinToString("|") { "${it.label.lowercase()}_${it.position.name}_${it.distanceFeet}ft" }

        // Check if this is a distinctly new obstacle encounter (different obstacle or moved into a different zone)
        val isNewObstacleSet = !isWatchingObstacle || hasObstacleChangedSignificantly(currentSignature)

        if (isNewObstacleSet) {
            isWatchingObstacle = true
            activeObstacleSignature = currentSignature
            warningCount = 0
            lastWarningTimestamp = 0L
            hasAnnouncedClearPath = false
        }

        // ---------------------------------------------------------
        // Warning 1: Initial Warning & Evasion Path Recommendation
        // ---------------------------------------------------------
        if (warningCount == 0) {
            val speech = buildInitialWarningSpeech(detectedObstacles, language)
            warningCount = 1
            lastWarningTimestamp = now
            val isCritical = detectedObstacles.any { it.distanceFeet <= 3 || it.priority == PriorityLevel.CRITICAL }
            return GuidanceSpeechResult(speech, if (isCritical) PriorityLevel.CRITICAL else PriorityLevel.HIGH, isCollisionAlert = isCritical)
        }

        // ---------------------------------------------------------
        // Warning 2: Follow-up Reminder if Obstacle is Still in Path
        // ---------------------------------------------------------
        if (warningCount == 1) {
            val elapsed = now - lastWarningTimestamp
            if (elapsed >= warningIntervalMs) {
                val speech = buildFollowupWarningSpeech(detectedObstacles, language)
                warningCount = 2
                lastWarningTimestamp = now
                val isCritical = detectedObstacles.any { it.distanceFeet <= 3 || it.priority == PriorityLevel.CRITICAL }
                return GuidanceSpeechResult(speech, if (isCritical) PriorityLevel.CRITICAL else PriorityLevel.HIGH, isCollisionAlert = isCritical)
            }
            // Cooldown period between warning 1 and 2 -> Remain quiet
            return null
        }

        // ---------------------------------------------------------
        // After 2 Warnings: STERN SILENCE WHILE WATCHING
        // "Voice Assisting Guidence Should Warn the User Only twice
        // and should Watch The User Has Successfully Move Forward
        // Without Any Collision.. After Detecting and Warning and Further
        // Guiding the User To The Right Path the Voice Assistive should
        // be Quite rather Than Speaking Unnecessarily Till It Found Another Obstacle"
        // ---------------------------------------------------------
        return null
    }

    private fun hasObstacleChangedSignificantly(newSig: String): Boolean {
        if (activeObstacleSignature.isEmpty()) return true
        val oldLabels = activeObstacleSignature.split("|").map { it.substringBefore("_") }.toSet()
        val newLabels = newSig.split("|").map { it.substringBefore("_") }.toSet()
        // If the types of obstacles differ (e.g. Chair vs Table vs Pillar), treat as new encounter
        return oldLabels != newLabels
    }

    /**
     * Builds the first comprehensive warning.
     * If multiple obstacles are detected simultaneously, warns about each one by one professionally,
     * then recommends the safest open corridor.
     */
    fun buildInitialWarningSpeech(
        obstacles: List<DetectedObject>,
        language: AssistantLanguage
    ): String {
        val isHindi = language == AssistantLanguage.HINDI
        val sorted = obstacles.sortedBy { it.distanceFeet }

        if (sorted.size == 1) {
            val single = sorted.first()
            return if (isHindi) {
                val posStr = when (single.position) {
                    SpatialPosition.CENTER -> "सामने"
                    SpatialPosition.LEFT -> "बाईं ओर"
                    SpatialPosition.RIGHT -> "दाईं ओर"
                }
                val evasionStr = when (single.position) {
                    SpatialPosition.CENTER -> "टकराने से बचने के लिए कृपया 2 से 3 फीट दाईं ओर हटकर आगे बढ़ें।"
                    SpatialPosition.LEFT -> "टकराने से बचने के लिए कृपया 2 से 3 फीट दाईं ओर हटें।"
                    SpatialPosition.RIGHT -> "टकराने से बचने के लिए कृपया 2 से 3 फीट बाईं ओर हटें।"
                }
                "सावधान! $posStr ${single.distanceFeet} फीट पर ${single.hindiLabel} है। $evasionStr"
            } else {
                val posStr = when (single.position) {
                    SpatialPosition.CENTER -> "directly ahead"
                    SpatialPosition.LEFT -> "on your left"
                    SpatialPosition.RIGHT -> "on your right"
                }
                val evasionStr = when (single.position) {
                    SpatialPosition.CENTER -> "Please move aside by 2 to 3 feet to your right to avoid collision."
                    SpatialPosition.LEFT -> "Please move aside by 2 to 3 feet to your right to avoid collision."
                    SpatialPosition.RIGHT -> "Please move aside by 2 to 3 feet to your left to avoid collision."
                }
                "Warning! ${single.label} ${single.distanceFeet} feet $posStr. $evasionStr"
            }
        }

        // Multiple obstacles detected simultaneously (Requirement 8)
        val hasLeft = sorted.any { it.position == SpatialPosition.LEFT }
        val hasCenter = sorted.any { it.position == SpatialPosition.CENTER }
        val hasRight = sorted.any { it.position == SpatialPosition.RIGHT }

        if (isHindi) {
            val descriptions = sorted.map { obj ->
                val pos = when (obj.position) {
                    SpatialPosition.CENTER -> "सामने"
                    SpatialPosition.LEFT -> "बाईं ओर"
                    SpatialPosition.RIGHT -> "दाईं ओर"
                }
                "$pos ${obj.distanceFeet} फीट पर ${obj.hindiLabel}"
            }

            val itemsText = descriptions.joinToString(", ")

            val bestPathAdvice = when {
                hasCenter && hasLeft && !hasRight -> "कृपया 2 से 3 फीट दाईं ओर से होकर आगे बढ़ें।"
                hasCenter && hasRight && !hasLeft -> "कृपया 2 से 3 फीट बाईं ओर से होकर आगे बढ़ें।"
                hasCenter && hasLeft && hasRight -> {
                    val leftDist = sorted.firstOrNull { it.position == SpatialPosition.LEFT }?.distanceFeet ?: 0
                    val rightDist = sorted.firstOrNull { it.position == SpatialPosition.RIGHT }?.distanceFeet ?: 0
                    if (rightDist >= leftDist) "कृपया 2 से 3 फीट दाईं ओर हटकर सावधानी से रास्ता लें।"
                    else "कृपया 2 से 3 फीट बाईं ओर हटकर सावधानी से रास्ता लें।"
                }
                !hasCenter -> "सामने का रास्ता साफ़ है, दोनों तरफ से 2 से 3 फीट की दूरी बनाते हुए सीधे आगे बढ़ें।"
                else -> "टकराने से बचने के लिए कृपया 2 से 3 फीट दाईं ओर हटकर आगे बढ़ें।"
            }

            return "सावधान! रास्ते में बाधाएं हैं: $itemsText। $bestPathAdvice"
        } else {
            val descriptions = sorted.map { obj ->
                val pos = when (obj.position) {
                    SpatialPosition.CENTER -> "ahead"
                    SpatialPosition.LEFT -> "on your left"
                    SpatialPosition.RIGHT -> "on your right"
                }
                "${obj.label} ${obj.distanceFeet} feet $pos"
            }

            val itemsText = descriptions.joinToString(", ")

            val bestPathAdvice = when {
                hasCenter && hasLeft && !hasRight -> "Please move aside by 2 to 3 feet to your right to proceed safely."
                hasCenter && hasRight && !hasLeft -> "Please move aside by 2 to 3 feet to your left to proceed safely."
                hasCenter && hasLeft && hasRight -> {
                    val leftDist = sorted.firstOrNull { it.position == SpatialPosition.LEFT }?.distanceFeet ?: 0
                    val rightDist = sorted.firstOrNull { it.position == SpatialPosition.RIGHT }?.distanceFeet ?: 0
                    if (rightDist >= leftDist) "Please move aside by 2 to 3 feet to your right to proceed carefully."
                    else "Please move aside by 2 to 3 feet to your left to proceed carefully."
                }
                !hasCenter -> "Center path is clear. Maintain 2 to 3 feet clearance and walk straight."
                else -> "Please move aside by 2 to 3 feet to your right to avoid collision."
            }

            return "Caution! Multiple obstacles in your path: $itemsText. $bestPathAdvice"
        }
    }

    /**
     * Builds the second and final warning reminder.
     */
    fun buildFollowupWarningSpeech(
        obstacles: List<DetectedObject>,
        language: AssistantLanguage
    ): String {
        val isHindi = language == AssistantLanguage.HINDI
        val primary = obstacles.minByOrNull { it.distanceFeet } ?: obstacles.first()

        return if (isHindi) {
            val posStr = when (primary.position) {
                SpatialPosition.CENTER -> "सामने"
                SpatialPosition.LEFT -> "बाईं ओर"
                SpatialPosition.RIGHT -> "दाईं ओर"
            }
            val evadeStr = when (primary.position) {
                SpatialPosition.CENTER -> "दाईं ओर 2 से 3 फीट हटकर रास्ता लें।"
                SpatialPosition.LEFT -> "दाईं ओर 2 से 3 फीट हटें।"
                SpatialPosition.RIGHT -> "बाईं ओर 2 से 3 फीट हटें।"
            }
            "ध्यान दें! $posStr अभी भी ${primary.distanceFeet} फीट पर ${primary.hindiLabel} है। कृपया $evadeStr"
        } else {
            val posStr = when (primary.position) {
                SpatialPosition.CENTER -> "ahead"
                SpatialPosition.LEFT -> "on your left"
                SpatialPosition.RIGHT -> "on your right"
            }
            val evadeStr = when (primary.position) {
                SpatialPosition.CENTER -> "move 2 to 3 feet to your right."
                SpatialPosition.LEFT -> "move 2 to 3 feet to your right."
                SpatialPosition.RIGHT -> "move 2 to 3 feet to your left."
            }
            "Attention! ${primary.label} is still $posStr at ${primary.distanceFeet} feet. Please $evadeStr"
        }
    }
}
