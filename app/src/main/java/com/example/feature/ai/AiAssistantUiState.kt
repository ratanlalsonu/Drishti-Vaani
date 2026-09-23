package com.example.feature.ai

import com.example.core.ai.AiAnalysisResult
import com.example.core.ai.AiVisionMode

data class AiAssistantUiState(
    val selectedMode: AiVisionMode = AiVisionMode.SCENE,
    val isAnalyzing: Boolean = false,
    val isListeningToVoiceQuery: Boolean = false,
    val spokenQuery: String = "",
    val latestResult: AiAnalysisResult? = null,
    val statusMessage: String = "कैमरा सामने रखें और किसी भी विकल्प पर टैप करें या बोलकर पूछें",
    val isTorchOn: Boolean = false,
    val hasApiKey: Boolean = false
)
