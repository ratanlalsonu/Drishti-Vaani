package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.model.AssistantLanguage
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
import com.example.ui.theme.AccessibleDarkSurface
import com.example.ui.theme.HighContrastCyan
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.HighContrastRed
import com.example.ui.theme.HighContrastYellow
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
    val context = LocalContext.current
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

    // Connect global voice commands for Vision controls
    LaunchedEffect(Unit) {
        assistantViewModel.onVisionQueryRequested = {
            visionViewModel.summarizeCurrentView()
        }
        assistantViewModel.onVisionRangeChanged = { limit ->
            visionViewModel.setRangeLimit(limit)
        }

        // Start auto-listening immediately if audio permission is already granted
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            assistantViewModel.startAutoListening()
        }
    }

    // Request permissions upfront in Compose
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        if (micGranted) {
            assistantViewModel.startAutoListening()
        } else {
            assistantViewModel.ttsManager.speak(
                "Microphone permission required for hands-free voice commands.",
                PriorityLevel.HIGH
            )
        }
        if (!cameraGranted) {
            assistantViewModel.ttsManager.speak(
                "Camera permission required for vision assistance.",
                PriorityLevel.HIGH
            )
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Universal Hands-Free Voice Status Banner at the top of every screen
        HandsFreeVoiceBar(
            isListening = uiState.isListening,
            isAutoListening = uiState.isAutoListeningActive,
            isSpeaking = assistantViewModel.ttsManager.isCurrentlySpeaking(),
            lastCommand = uiState.lastRecognizedText,
            activeScreen = uiState.activeScreen,
            currentLanguage = uiState.currentLanguage,
            onToggleAutoListen = {
                if (uiState.isAutoListeningActive) {
                    assistantViewModel.stopAutoListening()
                } else {
                    assistantViewModel.startAutoListening()
                }
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
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
    }
}

@Composable
fun HandsFreeVoiceBar(
    isListening: Boolean,
    isAutoListening: Boolean,
    isSpeaking: Boolean,
    lastCommand: String,
    activeScreen: String,
    currentLanguage: AssistantLanguage,
    onToggleAutoListen: () -> Unit
) {
    val borderColor = when {
        !isAutoListening -> HighContrastRed
        isSpeaking -> HighContrastCyan
        isListening -> HighContrastGreen
        else -> HighContrastYellow
    }

    val statusIcon = when {
        !isAutoListening -> Icons.Default.MicOff
        isSpeaking -> Icons.Default.VolumeUp
        else -> Icons.Default.Mic
    }

    val iconTint = when {
        !isAutoListening -> HighContrastRed
        isSpeaking -> HighContrastCyan
        isListening -> HighContrastGreen
        else -> HighContrastYellow
    }

    val mainText = when {
        !isAutoListening -> {
            if (currentLanguage == AssistantLanguage.HINDI) "आवाज़ बंद है (टैप करके चालू करें)" else "Voice Muted (Tap to enable)"
        }
        isSpeaking -> {
            if (currentLanguage == AssistantLanguage.HINDI) "दृष्टि वाणी बोल रही है..." else "Speaking..."
        }
        isListening -> {
            if (currentLanguage == AssistantLanguage.HINDI) "आवाज़ सुन रहे हैं... (कभी भी बोलें)" else "Hands-free listening... Just speak"
        }
        else -> {
            if (currentLanguage == AssistantLanguage.HINDI) "हैंड्स-फ्री सक्रिय है" else "Hands-free Ready"
        }
    }

    val subText = when {
        lastCommand.isNotBlank() -> "\"$lastCommand\""
        activeScreen == "vision" -> if (currentLanguage == AssistantLanguage.HINDI) "बोलो: 'सामने क्या है', '10 मीटर', 'Back'" else "Say: 'What is ahead', '10m', 'Back'"
        activeScreen == "ocr" -> if (currentLanguage == AssistantLanguage.HINDI) "बोलो: 'Pause', 'Resume', 'Back'" else "Say: 'Pause', 'Resume', 'Back'"
        activeScreen == "navigation" -> if (currentLanguage == AssistantLanguage.HINDI) "बोलो: 'Hospital', 'ATM', 'Back'" else "Say: 'Hospital', 'ATM', 'Back'"
        activeScreen == "emergency" -> if (currentLanguage == AssistantLanguage.HINDI) "बोलो: 'Call Emergency', 'Call [नाम]'" else "Say: 'Call Emergency', 'Call [Name]'"
        else -> if (currentLanguage == AssistantLanguage.HINDI) "बोलो: 'Vision', 'Padho', 'Ghar', 'Call'" else "Say: 'Vision', 'Read', 'Home', 'Call'"
    }

    val accessibilityDesc = if (isAutoListening) {
        "Hands free voice assistant active. Listening continuously across all pages without requiring screen taps. Status: $mainText. Last command: $lastCommand"
    } else {
        "Voice assistant muted. Tap to turn on hands free listening."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onToggleAutoListen)
            .testTag("hands_free_voice_status_bar")
            .semantics {
                contentDescription = accessibilityDesc
            },
        colors = CardDefaults.cardColors(containerColor = AccessibleDarkSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = mainText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subText,
                        color = HighContrastYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // High-contrast Mode Indicator Chip
            Box(
                modifier = Modifier
                    .background(
                        color = if (isAutoListening) HighContrastGreen.copy(alpha = 0.2f) else HighContrastRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isAutoListening) "ALWAYS ON" else "MUTED",
                    color = if (isAutoListening) HighContrastGreen else HighContrastRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
