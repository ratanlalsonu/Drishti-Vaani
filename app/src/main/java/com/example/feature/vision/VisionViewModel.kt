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
    private val throttler = SpeechThrottler()
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
        _uiState.update {
            it.copy(
                isAnalyzing = true,
                hasAnnouncedCurrentDirection = false,
                isDirectionSettled = true
            )
        }
        throttler.clear()
        startDirectionTracking()
    }

    fun stopVisionSession() {
        isVisionSessionActive = false
        stopDirectionTracking()
        ttsManager?.stopSpeaking()
        _uiState.update {
            it.copy(
                isAnalyzing = false,
                detections = emptyList(),
                hasAnnouncedCurrentDirection = false
            )
        }
        throttler.clear()
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
                    throttler.clear()
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
                    if (_uiState.value.detections.isNotEmpty()) {
                        announceDirectionSurroundingsOnce(force = false)
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
        throttler.clear()
        val speech = if (currentLanguage == AssistantLanguage.HINDI) {
            "डिटेक्शन रेंज ${range.labelHi} सेट की गई।"
        } else {
            "Detection range set to ${range.labelEn}."
        }
        ttsManager?.speak(speech, PriorityLevel.HIGH)
        announceDirectionSurroundingsOnce(force = false)
    }

    fun onDetectionsReceived(objects: List<DetectedObject>, width: Int, height: Int) {
        if (!isVisionSessionActive) return

        val currentMax = _uiState.value.rangeLimit.maxMeters

        // Filter out any objects that exceed user range threshold (e.g. > 10m)
        val validRangeObjects = objects.filter { it.estimatedDistanceMeters <= currentMax }

        val tableCount = validRangeObjects.count { it.label.equals("Table", ignoreCase = true) }
        val pillarCount = validRangeObjects.count { it.label.equals("Pillar", ignoreCase = true) }
        val primaryObstacle = validRangeObjects.minByOrNull { it.estimatedDistanceMeters }

        val status = when {
            validRangeObjects.isEmpty() -> "Scanning: Tables & Pillars"
            tableCount > 0 && pillarCount > 0 -> "$tableCount Table, $pillarCount Pillar detected"
            pillarCount > 0 -> "$pillarCount Pillar in View"
            else -> "$tableCount Table in View"
        }

        val collisionWarning = if (primaryObstacle != null) {
            "${primaryObstacle.label} ${primaryObstacle.distanceFeet} FT (${primaryObstacle.position.name})"
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

        // Proactively alert the user whenever a Table or Pillar enters path/range
        if (primaryObstacle != null && throttler.shouldAnnounce(primaryObstacle)) {
            val alertSpeech = if (currentLanguage == AssistantLanguage.HINDI) {
                val posStr = when (primaryObstacle.position) {
                    SpatialPosition.LEFT -> "बाईं ओर"
                    SpatialPosition.RIGHT -> "दाईं ओर"
                    SpatialPosition.CENTER -> "सामने"
                }
                "सावधान! $posStr ${primaryObstacle.distanceFeet} फीट पर ${primaryObstacle.hindiLabel} है। ${primaryObstacle.evasionAdviceHi}"
            } else {
                val posStr = when (primaryObstacle.position) {
                    SpatialPosition.LEFT -> "on your left"
                    SpatialPosition.RIGHT -> "on your right"
                    SpatialPosition.CENTER -> "directly ahead"
                }
                "Warning! ${primaryObstacle.label} ${primaryObstacle.distanceFeet} feet $posStr. ${primaryObstacle.evasionAdviceEn}"
            }

            _uiState.update {
                it.copy(
                    hasAnnouncedCurrentDirection = true,
                    lastVocalized = alertSpeech
                )
            }

            // Urgent tactile feedback if very close (< 4-5 feet) to prevent collision
            if (primaryObstacle.distanceFeet <= 4 || primaryObstacle.priority == PriorityLevel.CRITICAL) {
                hapticManager.triggerCriticalHazard()
            } else {
                hapticManager.triggerConfirmation()
            }

            ttsManager?.speak(alertSpeech, PriorityLevel.HIGH)
            recordDetection(primaryObstacle)
            return
        }

        // When sweeping direction, only announce once if table or pillar was found
        if (!_uiState.value.hasAnnouncedCurrentDirection && _uiState.value.isDirectionSettled) {
            announceDirectionSurroundingsOnce(force = false)
        }
    }

    /**
     * Announces obstacle status: warns about tables and structural pillars with calculative distance
     * and step-by-step avoidance advice (move aside by 2-3 feet left or right).
     * If force = false and no obstacle is detected, it stays completely SILENT.
     * If force = true (user explicitly asks 'what is ahead' or taps info), it answers directly.
     */
    fun announceDirectionSurroundingsOnce(force: Boolean = false) {
        if (!isVisionSessionActive && !force) return
        if (!force && _uiState.value.hasAnnouncedCurrentDirection) {
            return
        }

        val validRangeObjects = _uiState.value.detections.filter {
            it.estimatedDistanceMeters <= _uiState.value.rangeLimit.maxMeters
        }

        val centerObj = validRangeObjects.filter { it.position == SpatialPosition.CENTER }
            .minByOrNull { it.estimatedDistanceMeters }
        val leftObj = validRangeObjects.filter { it.position == SpatialPosition.LEFT }
            .minByOrNull { it.estimatedDistanceMeters }
        val rightObj = validRangeObjects.filter { it.position == SpatialPosition.RIGHT }
            .minByOrNull { it.estimatedDistanceMeters }

        val hasObstacle = centerObj != null || leftObj != null || rightObj != null

        // If no table or pillar is found and not an explicit on-demand user question, REMAIN SILENT!
        if (!hasObstacle && !force) {
            _uiState.update { it.copy(hasAnnouncedCurrentDirection = true) }
            return
        }

        val speech = if (currentLanguage == AssistantLanguage.HINDI) {
            val parts = mutableListOf<String>()
            if (centerObj != null) {
                parts.add("सामने ${centerObj.distanceFeet} फीट पर ${centerObj.hindiLabel} है। ${centerObj.evasionAdviceHi}")
            }
            if (leftObj != null) {
                parts.add("बाईं ओर ${leftObj.distanceFeet} फीट पर ${leftObj.hindiLabel} है। ${leftObj.evasionAdviceHi}")
            }
            if (rightObj != null) {
                parts.add("दाईं ओर ${rightObj.distanceFeet} फीट पर ${rightObj.hindiLabel} है। ${rightObj.evasionAdviceHi}")
            }

            if (parts.isNotEmpty()) {
                parts.joinToString(" ")
            } else {
                "सामने कोई मेज़ या खंभा नहीं है।"
            }
        } else {
            val parts = mutableListOf<String>()
            if (centerObj != null) {
                parts.add("Warning: ${centerObj.label} ${centerObj.distanceFeet} feet ahead. ${centerObj.evasionAdviceEn}")
            }
            if (leftObj != null) {
                parts.add("Caution: ${leftObj.label} ${leftObj.distanceFeet} feet on your left. ${leftObj.evasionAdviceEn}")
            }
            if (rightObj != null) {
                parts.add("Caution: ${rightObj.label} ${rightObj.distanceFeet} feet on your right. ${rightObj.evasionAdviceEn}")
            }

            if (parts.isNotEmpty()) {
                parts.joinToString(" ")
            } else {
                "No table or pillar detected ahead."
            }
        }

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
