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
    val lastDirectionSummary: String = ""
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

    init {
        startDirectionTracking()
    }

    fun startDirectionTracking() {
        if (directionTracker == null) {
            directionTracker = DirectionOrientationTracker(
                context = getApplication(),
                onDirectionMoved = {
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
                    // Vision settled in new direction: announce once!
                    _uiState.update {
                        it.copy(
                            isDirectionSettled = true,
                            currentDirectionHi = hiName,
                            currentDirectionEn = enName
                        )
                    }
                    announceDirectionSurroundingsOnce(force = false)
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
        stopDirectionTracking()
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
        announceDirectionSurroundingsOnce(force = true)
    }

    fun onDetectionsReceived(objects: List<DetectedObject>, width: Int, height: Int) {
        val currentMax = _uiState.value.rangeLimit.maxMeters

        // Filter out any objects that exceed user range threshold (e.g. > 10m)
        val validRangeObjects = objects.filter { it.estimatedDistanceMeters <= currentMax }

        _uiState.update {
            it.copy(
                detections = validRangeObjects,
                previewWidth = width,
                previewHeight = height,
                isAnalyzing = true,
                cameraStatus = "Active (${validRangeObjects.size} in ${_uiState.value.rangeLimit.maxMeters.toInt()}m range)"
            )
        }

        // 1. If vision moved to this direction for the first time and has settled,
        // announce front, left, and right ONCE:
        if (!_uiState.value.hasAnnouncedCurrentDirection && _uiState.value.isDirectionSettled) {
            announceDirectionSurroundingsOnce(force = false)
            return
        }

        // 2. If already announced for this direction, DO NOT repeat automatically.
        // Silence is maintained so the user is not disturbed repeatedly in the same direction.
        // The ONLY safety exception: Sudden critical collision hazard very close (< 0.85m)
        val criticalCloseHazard = validRangeObjects.firstOrNull {
            it.priority == PriorityLevel.CRITICAL && it.estimatedDistanceMeters < 0.85f
        }
        if (criticalCloseHazard != null && throttler.shouldAnnounce(criticalCloseHazard)) {
            hapticManager.triggerCriticalHazard()
            val alert = if (currentLanguage == AssistantLanguage.HINDI) {
                "सावधान! बहुत पास खतरा: ${criticalCloseHazard.hindiLabel}!"
            } else {
                "Warning! Hazard very close: ${criticalCloseHazard.label}!"
            }
            ttsManager?.speak(alert, PriorityLevel.CRITICAL)
        }
    }

    /**
     * Announces what is in front (सामने), left (बाईं ओर), and right (दाईं ओर).
     * If force = false, it only announces once per direction and then remains silent.
     * If force = true (user asks by voice or taps button), it re-announces on-demand.
     */
    fun announceDirectionSurroundingsOnce(force: Boolean = false) {
        if (!force && _uiState.value.hasAnnouncedCurrentDirection) {
            // Already announced for this direction; do not repeat automatically!
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

        val speech = if (currentLanguage == AssistantLanguage.HINDI) {
            val parts = mutableListOf<String>()
            if (centerObj != null) {
                parts.add("सामने ${centerObj.hindiLabel} (${centerObj.distanceDescriptionHi})")
            }
            if (leftObj != null) {
                parts.add("बाईं ओर ${leftObj.hindiLabel} (${leftObj.distanceDescriptionHi})")
            }
            if (rightObj != null) {
                parts.add("दाईं ओर ${rightObj.hindiLabel} (${rightObj.distanceDescriptionHi})")
            }

            if (parts.isNotEmpty()) {
                parts.joinToString(", ") + "।"
            } else {
                "${_uiState.value.rangeLimit.maxMeters.toInt()} मीटर के दायरे में रास्ता साफ है।"
            }
        } else {
            val parts = mutableListOf<String>()
            if (centerObj != null) {
                parts.add("Ahead: ${centerObj.label} (${centerObj.distanceDescriptionEn})")
            }
            if (leftObj != null) {
                parts.add("On your left: ${leftObj.label} (${leftObj.distanceDescriptionEn})")
            }
            if (rightObj != null) {
                parts.add("On your right: ${rightObj.label} (${rightObj.distanceDescriptionEn})")
            }

            if (parts.isNotEmpty()) {
                parts.joinToString(", ") + "."
            } else {
                "Clear path within ${_uiState.value.rangeLimit.maxMeters.toInt()} meters."
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
