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
    val statusMessage: String = "Point camera at any printed or handwritten text"
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

    fun attachTTS(tts: TTSManager, language: AssistantLanguage) {
        this.ttsManager = tts
        this.currentLanguage = language
    }

    fun createAnalyzer(): ImageAnalysis.Analyzer {
        return object : ImageAnalysis.Analyzer {
            private var lastScanTime = 0L
            private var consecutiveEmptyFrames = 0
            private var isProcessing = false

            @OptIn(ExperimentalGetImage::class)
            override fun analyze(imageProxy: ImageProxy) {
                val now = System.currentTimeMillis()
                // Fast 200ms scan cycle for instant detection and immediate stop when moving away
                if (now - lastScanTime < 200L || isProcessing) {
                    imageProxy.close()
                    return
                }
                lastScanTime = now

                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    return
                }

                isProcessing = true
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val cleanText = visionText.text.trim()
                        if (cleanText.isNotBlank()) {
                            consecutiveEmptyFrames = 0

                            // Check if this is new text, or we were previously stopped/reset
                            val isSpeaking = ttsManager?.isCurrentlySpeaking() == true
                            val isDifferentText = lastExtractedText.isBlank() || !isSimilarText(cleanText, lastExtractedText)

                            if (isDifferentText && !isPaused) {
                                lastExtractedText = cleanText
                                _uiState.update {
                                    it.copy(
                                        detectedText = cleanText,
                                        isReading = true,
                                        statusMessage = if (currentLanguage == AssistantLanguage.HINDI) "टेक्स्ट मिला, पढ़ रहे हैं..." else "Text found, reading..."
                                    )
                                }
                                hapticManager.triggerConfirmation()
                                readAloud(cleanText)
                            } else if (!isSpeaking && !isPaused && lastExtractedText.isBlank()) {
                                // Resume reading if it stopped previously
                                lastExtractedText = cleanText
                                _uiState.update {
                                    it.copy(
                                        detectedText = cleanText,
                                        isReading = true,
                                        statusMessage = if (currentLanguage == AssistantLanguage.HINDI) "टेक्स्ट मिला, पढ़ रहे हैं..." else "Text found, reading..."
                                    )
                                }
                                hapticManager.triggerConfirmation()
                                readAloud(cleanText)
                            }
                        } else {
                            // Camera moved away from text!
                            consecutiveEmptyFrames++

                            // After 2 consecutive frames (~400ms) without text, immediately silence voice
                            if (consecutiveEmptyFrames >= 2) {
                                val wasReading = _uiState.value.isReading || ttsManager?.isCurrentlySpeaking() == true
                                if (wasReading) {
                                    ttsManager?.stopSpeaking()
                                    hapticManager.triggerLightTick()
                                    _uiState.update {
                                        it.copy(
                                            detectedText = "",
                                            isReading = false,
                                            statusMessage = if (currentLanguage == AssistantLanguage.HINDI) "कैमरा टेक्स्ट से हटा - आवाज बंद" else "Camera moved away from text - reading stopped"
                                        )
                                    }
                                }
                                // Reset so pointing back at text starts reading immediately
                                lastExtractedText = ""
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Ignore occasional frame errors
                    }
                    .addOnCompleteListener {
                        try {
                            imageProxy.close()
                        } catch (_: Exception) {}
                        isProcessing = false
                    }
            }
        }
    }

    private fun isSimilarText(a: String, b: String): Boolean {
        if (a == b) return true
        if (a.isBlank() || b.isBlank()) return false
        val wordsA = a.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
        val wordsB = b.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
        if (wordsA.isEmpty() || wordsB.isEmpty()) return false
        val intersection = wordsA.intersect(wordsB).size
        val union = wordsA.union(wordsB).size
        return (intersection.toFloat() / union.toFloat()) > 0.70f
    }

    private fun readAloud(text: String) {
        // Read text immediately without any slow preambles
        ttsManager?.speakImmediate(text, PriorityLevel.HIGH)
    }

    fun pauseReading() {
        isPaused = true
        ttsManager?.stopSpeaking()
        _uiState.update { it.copy(isReading = false, statusMessage = "Reading paused") }
    }

    fun resumeReading() {
        isPaused = false
        if (lastExtractedText.isNotEmpty()) {
            readAloud(lastExtractedText)
            _uiState.update { it.copy(isReading = true, statusMessage = "Reading resumed") }
        }
    }

    fun repeatReading() {
        if (lastExtractedText.isNotEmpty()) {
            readAloud(lastExtractedText)
        }
    }

    fun stopReadingOnExit() {
        isPaused = false
        ttsManager?.stopSpeaking()
        lastExtractedText = ""
        _uiState.update {
            it.copy(
                isReading = false,
                detectedText = "",
                statusMessage = "Point camera at any printed or handwritten text"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopReadingOnExit()
        try {
            recognizer.close()
        } catch (_: Exception) {}
    }
}
