package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.core.model.PriorityLevel
import com.example.core.model.VoiceCommand
import com.example.feature.emergency.EmergencyScreen
import com.example.feature.emergency.EmergencyViewModel
import com.example.feature.help.VoiceHelpScreen
import com.example.feature.home.AssistantViewModel
import com.example.feature.home.HomeScreen
import com.example.feature.navigation.NavigationScreen
import com.example.feature.navigation.NavigationViewModel
import com.example.feature.ocr.OCRScreen
import com.example.feature.ocr.OCRViewModel
import com.example.feature.settings.SettingsScreen
import com.example.feature.vision.VisionScreen
import com.example.feature.vision.VisionViewModel
import com.example.ui.theme.AccessibleBlack
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val assistantViewModel: AssistantViewModel by viewModels()
    private val visionViewModel: VisionViewModel by viewModels()
    private val ocrViewModel: OCRViewModel by viewModels()
    private val navigationViewModel: NavigationViewModel by viewModels()
    private val emergencyViewModel: EmergencyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure ML Kit local cache & acceleration storage directory exists to avoid native proto_data_store file error
        try {
            val accelDir = java.io.File(filesDir, "com.google.mlkit.acceleration")
            if (!accelDir.exists()) {
                accelDir.mkdirs()
            }
        } catch (_: Exception) {}

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AccessibleBlack)
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppRoot(
                            assistantViewModel = assistantViewModel,
                            visionViewModel = visionViewModel,
                            ocrViewModel = ocrViewModel,
                            navigationViewModel = navigationViewModel,
                            emergencyViewModel = emergencyViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppRoot(
    assistantViewModel: AssistantViewModel,
    visionViewModel: VisionViewModel,
    ocrViewModel: OCRViewModel,
    navigationViewModel: NavigationViewModel,
    emergencyViewModel: EmergencyViewModel
) {
    val uiState by assistantViewModel.uiState.collectAsState()
    val visionState by visionViewModel.uiState.collectAsState()
    val ocrState by ocrViewModel.uiState.collectAsState()
    val navState by navigationViewModel.uiState.collectAsState()
    val emergencyState by emergencyViewModel.uiState.collectAsState()

    // Attach shared TTS & Language to sub-ViewModels
    LaunchedEffect(uiState.currentLanguage) {
        visionViewModel.attachTTS(assistantViewModel.ttsManager, uiState.currentLanguage)
        ocrViewModel.attachTTS(assistantViewModel.ttsManager, uiState.currentLanguage)
        navigationViewModel.attachTTS(assistantViewModel.ttsManager, uiState.currentLanguage)
        emergencyViewModel.attachTTS(assistantViewModel.ttsManager, uiState.currentLanguage)
    }

    // Request permissions upfront in Compose
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        if (!micGranted) {
            assistantViewModel.ttsManager.speak("Microphone permission required for voice commands.", PriorityLevel.HIGH)
        }
        if (!cameraGranted) {
            assistantViewModel.ttsManager.speak("Camera permission required for vision assistance.", PriorityLevel.HIGH)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CALL_PHONE
            )
        )
    }

    when (uiState.activeScreen) {
        "vision" -> {
            VisionScreen(
                uiState = visionState,
                onBackClick = { assistantViewModel.navigateToScreen("home") },
                onQuerySurroundings = { visionViewModel.summarizeCurrentView() },
                onRangeToggle = {
                    val next = when (visionState.rangeLimit) {
                        com.example.core.model.DetectionRangeLimit.STANDARD_10M -> com.example.core.model.DetectionRangeLimit.SHORT_5M
                        com.example.core.model.DetectionRangeLimit.SHORT_5M -> com.example.core.model.DetectionRangeLimit.UNLIMITED
                        com.example.core.model.DetectionRangeLimit.UNLIMITED -> com.example.core.model.DetectionRangeLimit.STANDARD_10M
                    }
                    visionViewModel.setRangeLimit(next)
                },
                onMicClick = {
                    if (uiState.isListening) {
                        assistantViewModel.stopVoiceInput()
                    } else {
                        assistantViewModel.startVoiceInput()
                    }
                },
                onDetectionsReceived = { objects, width, height ->
                    visionViewModel.onDetectionsReceived(objects, width, height)
                }
            )
        }
        "ocr" -> {
            OCRScreen(
                uiState = ocrState,
                ocrViewModel = ocrViewModel,
                onBackClick = { assistantViewModel.navigateToScreen("home") },
                onMicClick = {
                    if (uiState.isListening) {
                        assistantViewModel.stopVoiceInput()
                    } else {
                        assistantViewModel.startVoiceInput()
                    }
                }
            )
        }
        "navigation" -> {
            NavigationScreen(
                uiState = navState,
                navigationViewModel = navigationViewModel,
                onBackClick = { assistantViewModel.navigateToScreen("home") },
                onMicClick = {
                    if (uiState.isListening) {
                        assistantViewModel.stopVoiceInput()
                    } else {
                        assistantViewModel.startVoiceInput()
                    }
                }
            )
        }
        "emergency" -> {
            EmergencyScreen(
                uiState = emergencyState,
                emergencyViewModel = emergencyViewModel,
                onBackClick = { assistantViewModel.navigateToScreen("home") },
                onMicClick = {
                    if (uiState.isListening) {
                        assistantViewModel.stopVoiceInput()
                    } else {
                        assistantViewModel.startVoiceInput()
                    }
                }
            )
        }
        "settings" -> {

            SettingsScreen(
                currentLanguage = uiState.currentLanguage,
                onLanguageSelected = { lang -> assistantViewModel.setLanguage(lang) },
                onBackClick = { assistantViewModel.navigateToScreen("home") },
                onSpeechRateChanged = { rate -> assistantViewModel.ttsManager.setSpeechRate(rate) }
            )
        }
        "help" -> {
            VoiceHelpScreen(
                onBackClick = { assistantViewModel.navigateToScreen("home") },
                onSpeakHelp = { assistantViewModel.executeCommand(VoiceCommand.Help) }
            )
        }
        else -> {
            HomeScreen(
                uiState = uiState,
                onMicClick = {
                    if (uiState.isListening) {
                        assistantViewModel.stopVoiceInput()
                    } else {
                        assistantViewModel.startVoiceInput()
                    }
                },
                onNavigate = { screen ->
                    assistantViewModel.navigateToScreen(screen)
                },
                onRepeatClick = {
                    assistantViewModel.executeCommand(VoiceCommand.RepeatSpeech)
                }
            )
        }
    }
}
