package com.example.feature.ai

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.accessibility.HapticFeedbackManager
import com.example.core.ai.AiAnalysisResult
import com.example.core.ai.AiVisionMode
import com.example.core.ai.GeminiConfigManager
import com.example.core.ai.GeminiVisionService
import com.example.core.model.AssistantLanguage
import com.example.core.model.PriorityLevel
import com.example.feature.voice.TTSManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiService = GeminiVisionService(application)
    private val configManager = GeminiConfigManager(application)
    private val hapticManager = HapticFeedbackManager(application)

    private var ttsManager: TTSManager? = null
    private var currentLanguage = AssistantLanguage.HINDI

    private var latestBitmap: Bitmap? = null

    private val _uiState = MutableStateFlow(
        AiAssistantUiState(hasApiKey = configManager.isConfigured())
    )
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    fun attachTTS(tts: TTSManager, language: AssistantLanguage) {
        this.ttsManager = tts
        this.currentLanguage = language
        refreshApiKeyStatus()
    }

    fun refreshApiKeyStatus() {
        _uiState.update { it.copy(hasApiKey = configManager.isConfigured()) }
    }

    fun updateLatestFrameBitmap(bitmap: Bitmap?) {
        this.latestBitmap = bitmap
    }

    fun selectMode(mode: AiVisionMode) {
        _uiState.update { it.copy(selectedMode = mode) }
        hapticManager.triggerConfirmation()
    }

    fun analyzeWithMode(mode: AiVisionMode) {
        _uiState.update { it.copy(selectedMode = mode) }
        runAnalysis(mode, userQuery = "")
    }

    fun analyzeWithQuery(query: String) {
        _uiState.update {
            it.copy(
                selectedMode = AiVisionMode.CUSTOM,
                spokenQuery = query
            )
        }
        runAnalysis(AiVisionMode.CUSTOM, userQuery = query)
    }

    private fun runAnalysis(mode: AiVisionMode, userQuery: String) {
        val bitmap = latestBitmap
        val isHindi = currentLanguage == AssistantLanguage.HINDI

        val startMsg = when (mode) {
            AiVisionMode.SCENE -> if (isHindi) "पूरे दृश्य का विश्लेषण हो रहा है..." else "Analyzing scene with AI..."
            AiVisionMode.CURRENCY -> if (isHindi) "नोट व करेंसी जांची जा रही है..." else "Checking currency note with AI..."
            AiVisionMode.MEDICINE -> if (isHindi) "दवाई व एक्सपायरी जांची जा रही है..." else "Checking medicine with AI..."
            AiVisionMode.COLOR -> if (isHindi) "रंग व कपड़े पहचाने जा रहे हैं..." else "Detecting color and outfit..."
            AiVisionMode.CUSTOM -> if (isHindi) "आपके सवाल का उत्तर खोजा जा रहा है..." else "Finding answer to your question..."
        }

        _uiState.update {
            it.copy(
                isAnalyzing = true,
                statusMessage = startMsg
            )
        }
        hapticManager.triggerListeningStart()
        ttsManager?.speak(startMsg, PriorityLevel.HIGH)

        viewModelScope.launch {
            try {
                val result = geminiService.analyze(
                    bitmap = bitmap,
                    mode = mode,
                    userCustomQuery = userQuery,
                    language = currentLanguage
                )

                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        latestResult = result,
                        statusMessage = if (result.isFromGemini) "Gemini AI उत्तर तैयार है" else "ऑफलाइन उत्तर तैयार है"
                    )
                }

                hapticManager.triggerConfirmation()
                ttsManager?.speak(result.answer, PriorityLevel.HIGH)
            } catch (e: Exception) {
                val errMsg = if (isHindi) {
                    "विश्लेषण में त्रुटि आई। कृपया पुनः प्रयास करें।"
                } else {
                    "Error during AI analysis. Please try again."
                }
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        statusMessage = errMsg,
                        latestResult = AiAnalysisResult(
                            answer = errMsg,
                            isFromGemini = false,
                            mode = mode,
                            errorMessage = e.localizedMessage
                        )
                    )
                }
                hapticManager.triggerNotUnderstood()
                ttsManager?.speak(errMsg, PriorityLevel.HIGH)
            }
        }
    }

    fun repeatLastAnswer() {
        val answer = _uiState.value.latestResult?.answer
        if (!answer.isNullOrBlank()) {
            hapticManager.triggerConfirmation()
            ttsManager?.speak(answer, PriorityLevel.HIGH)
        } else {
            val emptyMsg = if (currentLanguage == AssistantLanguage.HINDI) {
                "अभी कोई पिछला उत्तर नहीं है।"
            } else {
                "No previous answer to repeat."
            }
            ttsManager?.speak(emptyMsg, PriorityLevel.NORMAL)
        }
    }

    fun stopSpeaking() {
        ttsManager?.stopSpeaking()
        hapticManager.triggerConfirmation()
    }

    fun toggleTorch(current: Boolean) {
        val next = !current
        _uiState.update { it.copy(isTorchOn = next) }
        hapticManager.triggerConfirmation()
        val msg = if (currentLanguage == AssistantLanguage.HINDI) {
            if (next) "फ्लैशलाइट चालू की गई" else "फ्लैशलाइट बंद की गई"
        } else {
            if (next) "Torch turned on" else "Torch turned off"
        }
        ttsManager?.speak(msg, PriorityLevel.NORMAL)
    }

    fun startSession() {
        refreshApiKeyStatus()
        val welcomeMsg = if (currentLanguage == AssistantLanguage.HINDI) {
            "AI दृष्टि सहायक सक्रिय। दृश्य, नोट, या दवाई जानने के लिए नीचे टैप करें या सवाल बोलें।"
        } else {
            "AI Vision Assistant active. Tap below to inspect scene, currency or medicine, or speak your question."
        }
        ttsManager?.speak(welcomeMsg, PriorityLevel.NORMAL)
    }

    fun stopSession() {
        ttsManager?.stopSpeaking()
        _uiState.update {
            it.copy(
                isAnalyzing = false,
                isListeningToVoiceQuery = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }
}
