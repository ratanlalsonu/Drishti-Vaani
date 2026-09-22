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
    val rangeLimit: DetectionRangeLimit = DetectionRangeLimit.STANDARD_10M
)

class VisionViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DrishtiDatabase.getDatabase(application)
    private val throttler = SpeechThrottler()
    private val hapticManager = HapticFeedbackManager(application)

    private val _uiState = MutableStateFlow(VisionUiState())
    val uiState: StateFlow<VisionUiState> = _uiState.asStateFlow()

    private var ttsManager: TTSManager? = null
    private var currentLanguage = AssistantLanguage.HINDI

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

        if (validRangeObjects.isEmpty()) return

        // Sort by priority first (CRITICAL > HIGH > NORMAL), then by distance (closest first)
        val prioritySorted = validRangeObjects.sortedWith(
            compareByDescending<DetectedObject> { it.priority.ordinal }
                .thenBy { it.estimatedDistanceMeters }
        )

        for (obj in prioritySorted) {
            if (throttler.shouldAnnounce(obj)) {
                announceObject(obj)
                recordDetection(obj)
                break // Announce single most important object per cycle to prevent speech overlap
            }
        }
    }

    private fun announceObject(obj: DetectedObject) {
        val pan = when (obj.position) {
            SpatialPosition.LEFT -> -0.85f
            SpatialPosition.RIGHT -> 0.85f
            SpatialPosition.CENTER -> 0.0f
        }

        val roundedDist = kotlin.math.round(obj.estimatedDistanceMeters * 10f) / 10f
        val distStrHi = if (roundedDist < 1.0f) "1 मीटर से कम" else "${roundedDist} मीटर"
        val distStrEn = if (roundedDist < 1.0f) "under 1m" else "${roundedDist}m"

        val speech = if (currentLanguage == AssistantLanguage.HINDI) {
            when (obj.priority) {
                PriorityLevel.CRITICAL -> {
                    hapticManager.triggerCriticalHazard()
                    "सावधान! ${obj.hindiLabel}, $distStrHi ${obj.position.spokenLabelHi}!"
                }
                PriorityLevel.HIGH -> {
                    hapticManager.triggerMediumAlert()
                    "${obj.hindiLabel}, $distStrHi ${obj.position.spokenLabelHi}"
                }
                else -> {
                    "${obj.hindiLabel}, $distStrHi ${obj.position.spokenLabelHi}"
                }
            }
        } else {
            when (obj.priority) {
                PriorityLevel.CRITICAL -> {
                    hapticManager.triggerCriticalHazard()
                    "Warning! ${obj.label}, $distStrEn ${obj.position.spokenLabelEn}!"
                }
                PriorityLevel.HIGH -> {
                    hapticManager.triggerMediumAlert()
                    "${obj.label}, $distStrEn ${obj.position.spokenLabelEn}"
                }
                else -> {
                    "${obj.label}, $distStrEn ${obj.position.spokenLabelEn}"
                }
            }
        }

        _uiState.update { it.copy(lastVocalized = speech) }
        ttsManager?.speakImmediate(speech, obj.priority, pan)
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

    fun summarizeCurrentView() {
        val currentDetections = _uiState.value.detections
        if (currentDetections.isEmpty()) {
            val emptyMsg = if (currentLanguage == AssistantLanguage.HINDI) {
                "${_uiState.value.rangeLimit.maxMeters.toInt()} मीटर के दायरे में सामने कोई रुकावट नहीं है।"
            } else {
                "No obstacles within ${_uiState.value.rangeLimit.maxMeters.toInt()} meters ahead."
            }
            ttsManager?.speak(emptyMsg, PriorityLevel.HIGH)
            return
        }

        val summary = if (currentLanguage == AssistantLanguage.HINDI) {
            val items = currentDetections.take(3).joinToString(", ") {
                "${it.hindiLabel} (${it.distanceDescriptionHi}, ${it.position.spokenLabelHi})"
            }
            "सामने स्थित है: $items."
        } else {
            val items = currentDetections.take(3).joinToString(", ") {
                "${it.label} (${it.distanceDescriptionEn}, ${it.position.spokenLabelEn})"
            }
            "In front of you: $items."
        }
        ttsManager?.speak(summary, PriorityLevel.HIGH)
    }
}
