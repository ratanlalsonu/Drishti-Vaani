package com.example.feature.vision

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.accessibility.HapticFeedbackManager
import com.example.core.model.AssistantLanguage
import com.example.core.model.DetectedObject
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
    val cameraStatus: String = "Initializing..."
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

    fun onDetectionsReceived(objects: List<DetectedObject>, width: Int, height: Int) {
        _uiState.update {
            it.copy(
                detections = objects,
                previewWidth = width,
                previewHeight = height,
                isAnalyzing = true,
                cameraStatus = "Active (${objects.size} objects)"
            )
        }

        if (objects.isEmpty()) return

        // Sort by priority first (CRITICAL > HIGH > NORMAL), then by proximity
        val prioritySorted = objects.sortedByDescending { it.priority.ordinal }

        for (obj in prioritySorted) {
            if (throttler.shouldAnnounce(obj)) {
                announceObject(obj)
                recordDetection(obj)
                break // Announce highest priority object per cycle to avoid speech collision
            }
        }
    }

    private fun announceObject(obj: DetectedObject) {
        val pan = when (obj.position) {
            SpatialPosition.LEFT -> -0.85f
            SpatialPosition.RIGHT -> 0.85f
            SpatialPosition.CENTER -> 0.0f
        }

        val speech = if (currentLanguage == AssistantLanguage.HINDI) {
            when (obj.priority) {
                PriorityLevel.CRITICAL -> {
                    hapticManager.triggerCriticalHazard()
                    "Savdhan! ${obj.hindiLabel} bahut paas ${obj.position.spokenLabelHi} hai!"
                }
                PriorityLevel.HIGH -> {
                    hapticManager.triggerMediumAlert()
                    "${obj.hindiLabel} ${obj.position.spokenLabelHi}."
                }
                else -> {
                    "${obj.hindiLabel} ${obj.position.spokenLabelHi}."
                }
            }
        } else {
            when (obj.priority) {
                PriorityLevel.CRITICAL -> {
                    hapticManager.triggerCriticalHazard()
                    "Warning! ${obj.label} very close ${obj.position.spokenLabelEn}!"
                }
                PriorityLevel.HIGH -> {
                    hapticManager.triggerMediumAlert()
                    "${obj.label} ${obj.position.spokenLabelEn}."
                }
                else -> {
                    "${obj.label} ${obj.position.spokenLabelEn}."
                }
            }
        }

        _uiState.update { it.copy(lastVocalized = speech) }
        ttsManager?.speak(speech, obj.priority, pan)
    }

    private fun recordDetection(obj: DetectedObject) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.detectionHistoryDao().insertDetection(
                    DetectionRecord(
                        objectLabel = obj.label,
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
                "Samne koi pramukh rukavat nahi dikh rahi hai."
            } else {
                "No major obstacles detected directly ahead."
            }
            ttsManager?.speak(emptyMsg, PriorityLevel.HIGH)
            return
        }

        val summary = if (currentLanguage == AssistantLanguage.HINDI) {
            val items = currentDetections.take(3).joinToString(", ") { "${it.hindiLabel} (${it.position.spokenLabelHi})" }
            "Samne dikh raha hai: $items."
        } else {
            val items = currentDetections.take(3).joinToString(", ") { "${it.label} ${it.position.spokenLabelEn}" }
            "In front of you: $items."
        }
        ttsManager?.speak(summary, PriorityLevel.HIGH)
    }
}
