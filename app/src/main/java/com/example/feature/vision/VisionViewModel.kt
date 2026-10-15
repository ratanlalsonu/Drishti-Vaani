package com.example.feature.vision

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.accessibility.HapticFeedbackManager
import com.example.core.model.AssistantLanguage
import com.example.core.model.DetectedObject
import com.example.core.model.DetectionRangeLimit
import com.example.core.model.PriorityLevel
import com.example.core.model.SpatialPosition
import com.example.data.local.DrishtiDatabase
import com.example.data.local.entity.DetectionRecord
import com.example.feature.voice.TTSManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VisionUiState(
    val detections: List<DetectedObject> = emptyList(),
    val previewWidth: Int = 0,
    val previewHeight: Int = 0,
    val isAnalyzing: Boolean = false,
    val lastVocalized: String = "",
    val cameraStatus: String = "Initializing...",
    val rangeLimit: DetectionRangeLimit = DetectionRangeLimit.STANDARD_10M,
    val isIdentifyingObject: Boolean = false,
    val identifiedResult: ObjectIdentityResult? = null,
    val currentDirectionHi: String = "सामने की दिशा",
    val currentDirectionEn: String = "Facing Ahead",
    val isDirectionSettled: Boolean = true,
    val hasAnnouncedCurrentDirection: Boolean = false,
    val lastDirectionSummary: String = "",
    val activeCollisionWarning: String = "",
    val evasionGuidance: String = "",
    val closestObstacle: DetectedObject? = null
)

class VisionViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DrishtiDatabase.getDatabase(application)
    val guidanceCoordinator = ObstacleGuidanceCoordinator()
    private val hapticManager = HapticFeedbackManager(application)
    private val objectIdentifier = ObjectIdentifierService(application)
    private var directionTracker: DirectionOrientationTracker? = null

    private val _uiState = MutableStateFlow(VisionUiState())
    val uiState: StateFlow<VisionUiState> = _uiState.asStateFlow()

    private var ttsManager: TTSManager? = null
    private var currentLanguage = AssistantLanguage.HINDI
    @Volatile
    private var latestBitmap: android.graphics.Bitmap? = null
    @Volatile
    var isVisionSessionActive: Boolean = false
        private set

    fun startVisionSession() {
        isVisionSessionActive = true
        guidanceCoordinator.reset()
        _uiState.update {
            it.copy(
                isAnalyzing = true,
                hasAnnouncedCurrentDirection = false,
                isDirectionSettled = true
            )
        }
        startDirectionTracking()
    }

    fun stopVisionSession() {
        isVisionSessionActive = false
        stopDirectionTracking()
        ttsManager?.stopSpeaking()
        guidanceCoordinator.reset()
        _uiState.update {
            it.copy(
                isAnalyzing = false,
                detections = emptyList(),
                hasAnnouncedCurrentDirection = false
            )
        }
    }

    fun startDirectionTracking() {
        if (directionTracker == null) {
            directionTracker = DirectionOrientationTracker(
                context = getApplication(),
                onDirectionMoved = {
                    if (!isVisionSessionActive) return@DirectionOrientationTracker
                    // Blind person moved vision / changed direction
                    _uiState.update {
                        it.copy(
                            isDirectionSettled = false,
                            hasAnnouncedCurrentDirection = false
                        )
                    }
                },
                onDirectionSettled = { _, _, hiName, enName ->
                    if (!isVisionSessionActive) return@DirectionOrientationTracker
                    // Vision settled in new direction
                    _uiState.update {
                        it.copy(
                            isDirectionSettled = true,
                            currentDirectionHi = hiName,
                            currentDirectionEn = enName
                        )
                    }
                }
            )
        }
        directionTracker?.start()
    }

    fun stopDirectionTracking() {
        directionTracker?.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopVisionSession()
    }

    fun updateLatestFrameBitmap(bitmap: android.graphics.Bitmap?) {
        this.latestBitmap = bitmap
    }

    fun identifyCurrentObject(capturedBitmap: android.graphics.Bitmap? = null) {
        val bitmapToAnalyze = capturedBitmap ?: latestBitmap
        if (bitmapToAnalyze == null) {
            val msg = if (currentLanguage == AssistantLanguage.HINDI) {
                "कैमरा सामने रखें और वस्तु को लेंस के आगे लाएं।"
            } else {
                "Please hold the camera facing the object."
            }
            ttsManager?.speak(msg, PriorityLevel.HIGH)
            return
        }

        _uiState.update { it.copy(isIdentifyingObject = true) }
        hapticManager.triggerListeningStart()

        val promptSpeak = if (currentLanguage == AssistantLanguage.HINDI) {
            "वस्तु की पहचान की जा रही है, एक क्षण..."
        } else {
            "Identifying object, one moment please..."
        }
        ttsManager?.speak(promptSpeak, PriorityLevel.HIGH)

        viewModelScope.launch {
            try {
                val result = objectIdentifier.identifyObject(bitmapToAnalyze, currentLanguage)
                _uiState.update {
                    it.copy(
                        isIdentifyingObject = false,
                        identifiedResult = result,
                        lastVocalized = result.description
                    )
                }
                hapticManager.triggerCommandSuccess()
                ttsManager?.speak(result.description, PriorityLevel.HIGH)
            } catch (e: Exception) {
                _uiState.update { it.copy(isIdentifyingObject = false) }
                val fallbackMsg = if (currentLanguage == AssistantLanguage.HINDI) {
                    "वस्तु की पहचान पूरी नहीं हो सकी। कृपया वस्तु को थोड़ा और प्रकाश में लाएं।"
                } else {
                    "Could not identify the object. Please ensure good lighting and try again."
                }
                ttsManager?.speak(fallbackMsg, PriorityLevel.HIGH)
            }
        }
    }

    fun attachTTS(tts: TTSManager, language: AssistantLanguage) {
        this.ttsManager = tts
        this.currentLanguage = language
    }

    fun setRangeLimit(range: DetectionRangeLimit) {
        _uiState.update { it.copy(rangeLimit = range) }
        guidanceCoordinator.reset()
        val speech = if (currentLanguage == AssistantLanguage.HINDI) {
            "डिटेक्शन रेंज ${range.labelHi} सेट की गई।"
        } else {
            "Detection range set to ${range.labelEn}."
        }
        ttsManager?.speak(speech, PriorityLevel.HIGH)
    }

    fun onDetectionsReceived(objects: List<DetectedObject>, width: Int, height: Int) {
        if (!isVisionSessionActive) return

        val currentMax = _uiState.value.rangeLimit.maxMeters

        // Filter out any objects that exceed user range threshold (e.g. > 10m)
        val validRangeObjects = objects.filter { it.estimatedDistanceMeters <= currentMax }

        val chairCount = validRangeObjects.count { it.label.equals("Chair", ignoreCase = true) }
        val tableCount = validRangeObjects.count { it.label.equals("Table", ignoreCase = true) }
        val pillarCount = validRangeObjects.count { it.label.equals("Pillar", ignoreCase = true) }
        val primaryObstacle = validRangeObjects.minByOrNull { it.distanceFeet }

        val status = when {
            validRangeObjects.isEmpty() -> if (guidanceCoordinator.hasAnnouncedClearPath) "✅ रास्ता आगे साफ़ है" else "निगरानी सक्रिय"
            else -> {
                val items = mutableListOf<String>()
                if (chairCount > 0) items.add("$chairCount कुर्सी")
                if (tableCount > 0) items.add("$tableCount मेज़")
                if (pillarCount > 0) items.add("$pillarCount खंभा")
                "⚠️ " + items.joinToString(", ")
            }
        }

        val collisionWarning = if (primaryObstacle != null) {
            "${primaryObstacle.hindiLabel} ${primaryObstacle.distanceFeet} FT (${primaryObstacle.position.spokenLabelHi})"
        } else ""

        val evasionText = if (primaryObstacle != null) {
            if (currentLanguage == AssistantLanguage.HINDI) primaryObstacle.evasionAdviceHi else primaryObstacle.evasionAdviceEn
        } else ""

        _uiState.update {
            it.copy(
                detections = validRangeObjects,
                previewWidth = width,
                previewHeight = height,
                isAnalyzing = true,
                cameraStatus = status,
                activeCollisionWarning = collisionWarning,
                evasionGuidance = evasionText,
                closestObstacle = primaryObstacle
            )
        }

        // Process live guidance:
        // - Announces clear path once when path is clear (Requirement 10)
        // - Warns only twice per obstacle encounter (Requirement 2)
        // - Informs accurate distance & 2-3 ft evasion (Requirements 1, 3, 9)
        // - Warns multiple objects one by one professionally (Requirement 8)
        // - Watches until user safely moves forward past obstacle (Requirement 2)
        // - Stern silence after guidance without unnecessary chatter (Requirements 5 & 6)
        val guidanceResult = guidanceCoordinator.processDetections(validRangeObjects, currentLanguage)
        if (guidanceResult != null) {
            _uiState.update {
                it.copy(
                    hasAnnouncedCurrentDirection = true,
                    lastVocalized = guidanceResult.speech,
                    lastDirectionSummary = guidanceResult.speech
                )
            }

            if (guidanceResult.isCollisionAlert) {
                hapticManager.triggerCriticalHazard()
            } else {
                hapticManager.triggerConfirmation()
            }

            ttsManager?.speak(guidanceResult.speech, guidanceResult.priority)
            validRangeObjects.take(3).forEach { recordDetection(it) }
        }
    }

    /**
     * Announces obstacle status on demand when blind person explicitly asks 'what is ahead' or taps info.
     */
    fun announceDirectionSurroundingsOnce(force: Boolean = false) {
        if (!isVisionSessionActive && !force) return

        val validRangeObjects = _uiState.value.detections.filter {
            it.estimatedDistanceMeters <= _uiState.value.rangeLimit.maxMeters
        }

        if (validRangeObjects.isEmpty()) {
            if (!force) return
            val speech = if (currentLanguage == AssistantLanguage.HINDI) {
                "रास्ता आगे एकदम क्लियर है, आप सीधे चल सकते हैं।"
            } else {
                "The path ahead is completely clear, you can walk straight."
            }
            _uiState.update {
                it.copy(
                    hasAnnouncedCurrentDirection = true,
                    lastVocalized = speech,
                    lastDirectionSummary = speech
                )
            }
            ttsManager?.speak(speech, PriorityLevel.HIGH)
            return
        }

        val speech = guidanceCoordinator.buildInitialWarningSpeech(validRangeObjects, currentLanguage)
        _uiState.update {
            it.copy(
                hasAnnouncedCurrentDirection = true,
                lastVocalized = speech,
                lastDirectionSummary = speech
            )
        }

        hapticManager.triggerConfirmation()
        ttsManager?.speak(speech, PriorityLevel.HIGH)
        validRangeObjects.take(3).forEach { recordDetection(it) }
    }

    private fun recordDetection(obj: DetectedObject) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.detectionHistoryDao().insertDetection(
                    DetectionRecord(
                        objectLabel = "${obj.label} (~${obj.estimatedDistanceMeters}m)",
                        position = obj.position.name,
                        proximity = obj.proximity.name,
                        priority = obj.priority.name
                    )
                )
            } catch (_: Exception) {}
        }
    }

    /**
     * Blind person explicitly asks "सामने क्या है" or "आगे क्या है" or presses the button.
     * Re-identifies the current view on demand in the same direction.
     */
    fun summarizeCurrentView() {
        announceDirectionSurroundingsOnce(force = true)
    }
}
