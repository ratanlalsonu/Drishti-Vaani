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

            @OptIn(ExperimentalGetImage::class)
            override fun analyze(imageProxy: ImageProxy) {
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
                        val cleanText = visionText.text.trim()
                        if (cleanText.isNotEmpty() && cleanText != lastExtractedText && !isPaused) {
                            lastExtractedText = cleanText
                            _uiState.update {
                                it.copy(
                                    detectedText = cleanText,
                                    isReading = true,
                                    statusMessage = "Text detected and reading aloud"
                                )
                            }
                            hapticManager.triggerConfirmation()
                            readAloud(cleanText)
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        }
    }

    private fun readAloud(text: String) {
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

    override fun onCleared() {
        super.onCleared()
        try {
            recognizer.close()
        } catch (_: Exception) {}
    }
}
