package com.example.feature.ocr

import android.app.Application
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import com.example.core.accessibility.HapticFeedbackManager
import com.example.core.model.AssistantLanguage
import com.example.core.model.PriorityLevel
import com.example.feature.voice.TTSManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OcrUiState(
    val detectedText: String = "",
    val isReading: Boolean = false,
    val statusMessage: String = "Point camera at any printed or handwritten text",
    val hasAnnouncedCurrentText: Boolean = false
)

class OCRViewModel(application: Application) : AndroidViewModel(application) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val hapticManager = HapticFeedbackManager(application)

    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    private var ttsManager: TTSManager? = null
    private var currentLanguage = AssistantLanguage.HINDI
    private var lastExtractedText = ""
    private var isPaused = false
    @Volatile
    var isOcrSessionActive: Boolean = false
        private set
    private var emptyScanCount = 0

    fun attachTTS(tts: TTSManager, language: AssistantLanguage) {
        this.ttsManager = tts
        this.currentLanguage = language
    }

    fun startReading() {
        isOcrSessionActive = true
        isPaused = false
        lastExtractedText = ""
        emptyScanCount = 0
        _uiState.update {
            it.copy(
                isReading = true,
                hasAnnouncedCurrentText = false,
                statusMessage = "Point camera at any printed or handwritten text"
            )
        }
    }

    fun stopReading() {
        isOcrSessionActive = false
        isPaused = true
        ttsManager?.stopSpeaking()
        _uiState.update {
            it.copy(
                isReading = false,
                hasAnnouncedCurrentText = false,
                statusMessage = "Text reading stopped"
            )
        }
    }

    fun createAnalyzer(): ImageAnalysis.Analyzer {
        return object : ImageAnalysis.Analyzer {
            private var lastScanTime = 0L

            @OptIn(ExperimentalGetImage::class)
            override fun analyze(imageProxy: ImageProxy) {
                if (!isOcrSessionActive || isPaused) {
                    imageProxy.close()
                    return
                }

                val now = System.currentTimeMillis()
                // Process frame every 1.5 seconds for steady OCR capture
                if (now - lastScanTime < 1500L) {
                    imageProxy.close()
                    return
                }
                lastScanTime = now

                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    return
                }

                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        if (!isOcrSessionActive || isPaused) return@addOnSuccessListener

                        val cleanText = visionText.text.trim()
                        if (cleanText.isEmpty()) {
                            emptyScanCount++
                            // If camera is pointed away from text for ~3 scans (4.5s), allow new text detection
                            if (emptyScanCount >= 3) {
                                _uiState.update { it.copy(hasAnnouncedCurrentText = false) }
                            }
                            return@addOnSuccessListener
                        }

                        emptyScanCount = 0
                        val isSameAsLast = isSimilarText(cleanText, lastExtractedText)

                        // If user hasn't moved vision away from this text and it was already announced once:
                        // DO NOT keep reading it automatically!
                        if (isSameAsLast && _uiState.value.hasAnnouncedCurrentText) {
                            return@addOnSuccessListener
                        }

                        // First time seeing this text or vision moved to a new text:
                        lastExtractedText = cleanText
                        _uiState.update {
                            it.copy(
                                detectedText = cleanText,
                                isReading = true,
                                hasAnnouncedCurrentText = true,
                                statusMessage = "Text detected and read aloud"
                            )
                        }
                        hapticManager.triggerConfirmation()
                        readAloud(cleanText)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        }
    }

    private fun isSimilarText(t1: String, t2: String): Boolean {
        if (t1.isEmpty() && t2.isEmpty()) return true
        if (t1.isEmpty() || t2.isEmpty()) return false
        val n1 = t1.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
        val n2 = t2.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
        if (n1 == n2) return true
        if (n1.startsWith(n2) || n2.startsWith(n1)) return true

        // Compute basic token overlap
        val words1 = t1.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
        val words2 = t2.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        return if (union > 0) (intersection.toDouble() / union.toDouble()) > 0.75 else false
    }

    private fun readAloud(text: String) {
        if (!isOcrSessionActive || isPaused) return
        val announcement = if (currentLanguage == AssistantLanguage.HINDI) {
            "Text mila: $text"
        } else {
            "Text detected: $text"
        }
        ttsManager?.speak(announcement, PriorityLevel.HIGH)
    }

    fun pauseReading() {
        isPaused = true
        ttsManager?.stopSpeaking()
        _uiState.update { it.copy(isReading = false, statusMessage = "Reading paused") }
    }

    fun resumeReading() {
        if (!isOcrSessionActive) {
            startReading()
        }
        isPaused = false
        if (lastExtractedText.isNotEmpty()) {
            readAloud(lastExtractedText)
            _uiState.update { it.copy(isReading = true, statusMessage = "Reading resumed") }
        }
    }

    /**
     * Re-reads the current text when blind person explicitly asks or clicks repeat.
     */
    fun repeatReading() {
        if (lastExtractedText.isNotBlank()) {
            readAloud(lastExtractedText)
        } else if (_uiState.value.detectedText.isNotBlank()) {
            readAloud(_uiState.value.detectedText)
        } else {
            val msg = if (currentLanguage == AssistantLanguage.HINDI) {
                "Abhi koi text nahi dikh raha hai. Camera ko text ke samne layein."
            } else {
                "No text visible right now. Please point camera at text."
            }
            ttsManager?.speak(msg, PriorityLevel.HIGH)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopReading()
        try {
            recognizer.close()
        } catch (_: Exception) {}
    }
}
