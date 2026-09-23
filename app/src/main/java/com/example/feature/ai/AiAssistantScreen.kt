package com.example.feature.ai

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.ai.AiVisionMode
import com.example.feature.camera.CameraManager
import com.example.ui.theme.AccessibleBlack
import com.example.ui.theme.AccessibleDarkSurface
import com.example.ui.theme.HighContrastCyan
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.HighContrastRed
import com.example.ui.theme.HighContrastYellow

@Composable
fun AiAssistantScreen(
    uiState: AiAssistantUiState,
    aiViewModel: AiAssistantViewModel,
    onBackClick: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraManager = remember(context, lifecycleOwner) {
        CameraManager(context, lifecycleOwner)
    }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    DisposableEffect(Unit) {
        aiViewModel.startSession()
        onDispose {
            aiViewModel.stopSession()
            cameraManager.shutdown(previewViewRef)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(AccessibleBlack),
        color = AccessibleBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with Back, Title, AI Badge, Torch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(AccessibleDarkSurface)
                            .testTag("ai_screen_back_button")
                            .semantics { contentDescription = "Back to home screen" }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = HighContrastYellow,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = HighContrastCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI दृष्टि सहायक",
                                color = HighContrastYellow,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.semantics {
                                    contentDescription = "AI Vision Assistant powered by Google Gemini"
                                }
                            )
                        }
                        Text(
                            text = if (uiState.hasApiKey) "Gemini 3.5 Flash Active" else "Offline AI Mode",
                            color = if (uiState.hasApiKey) HighContrastGreen else HighContrastCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Flashlight / Torch Toggle Button
                IconButton(
                    onClick = { aiViewModel.toggleTorch(uiState.isTorchOn) },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AccessibleDarkSurface)
                        .testTag("ai_screen_torch_button")
                        .semantics {
                            contentDescription = if (uiState.isTorchOn) "Turn off flashlight" else "Turn on flashlight"
                        }
                ) {
                    Icon(
                        imageVector = if (uiState.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = null,
                        tint = if (uiState.isTorchOn) HighContrastYellow else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Camera Viewfinder Box (Compact high-contrast frame)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AccessibleDarkSurface)
                    .semantics {
                        contentDescription = "Camera viewfinder capturing environment for Gemini AI"
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            previewViewRef = this
                            cameraManager.startCamera(
                                previewView = this,
                                analyzer = object : ImageAnalysis.Analyzer {
                                    private var lastCaptureTime = 0L

                                    override fun analyze(imageProxy: ImageProxy) {
                                        val now = System.currentTimeMillis()
                                        // Capture latest frame every 800ms for fresh AI analysis
                                        if (now - lastCaptureTime > 800L) {
                                            lastCaptureTime = now
                                            val bmp = imageProxy.toBitmap()
                                            aiViewModel.updateLatestFrameBitmap(bmp)
                                        }
                                        imageProxy.close()
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // High Contrast Framing Reticle
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(2.dp, HighContrastCyan.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp)
                ) {}

                // Processing Indicator Overlay
                if (uiState.isAnalyzing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.70f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = HighContrastYellow,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Gemini AI विश्लेषण कर रहा है...",
                                color = HighContrastYellow,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AI Answer / Result Card (Scrollable & Highly Accessible)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = AccessibleDarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(2.dp, if (uiState.isAnalyzing) HighContrastGreen else HighContrastYellow)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.statusMessage,
                                color = HighContrastCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (uiState.latestResult?.isFromGemini == true) {
                                Surface(
                                    color = HighContrastGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, HighContrastGreen)
                                ) {
                                    Text(
                                        text = "Google AI",
                                        color = HighContrastGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val answerText = uiState.latestResult?.answer
                            ?: "कैमरा किसी वस्तु, नोट, दवाई या कमरे के सामने रखें और नीचे से विकल्प चुनें या माइक बटन दबाकर बोलें।"

                        Text(
                            text = answerText,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 25.sp,
                            modifier = Modifier.semantics {
                                contentDescription = "AI Response: $answerText"
                            }
                        )
                    }

                    // Answer Action Buttons: Repeat Audio & Stop
                    if (uiState.latestResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                onClick = { aiViewModel.repeatLastAnswer() },
                                shape = RoundedCornerShape(10.dp),
                                color = AccessibleBlack,
                                border = BorderStroke(1.5.dp, HighContrastYellow),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .semantics { contentDescription = "Repeat AI answer aloud" }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = HighContrastYellow,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "दोबारा सुनें",
                                        color = HighContrastYellow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Surface(
                                onClick = { aiViewModel.stopSpeaking() },
                                shape = RoundedCornerShape(10.dp),
                                color = AccessibleBlack,
                                border = BorderStroke(1.5.dp, HighContrastRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .semantics { contentDescription = "Stop speech" }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = null,
                                        tint = HighContrastRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "आवाज़ रोकें",
                                        color = HighContrastRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AI Feature Mode Buttons Row (4 Presets)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    listOf(
                        ModeItem(AiVisionMode.SCENE, "पूरा दृश्य", Icons.Default.Visibility, HighContrastCyan),
                        ModeItem(AiVisionMode.CURRENCY, "करेंसी / नोट", Icons.Default.LocalAtm, HighContrastYellow),
                        ModeItem(AiVisionMode.MEDICINE, "दवाई जांचें", Icons.Default.Medication, HighContrastGreen),
                        ModeItem(AiVisionMode.COLOR, "रंग व कपड़े", Icons.Default.Palette, HighContrastCyan)
                    )
                ) { item ->
                    val isSelected = uiState.selectedMode == item.mode
                    Card(
                        modifier = Modifier
                            .height(60.dp)
                            .clickable { aiViewModel.analyzeWithMode(item.mode) }
                            .testTag("ai_mode_${item.mode.name.lowercase()}")
                            .semantics {
                                contentDescription = "${item.title}: ${item.mode.iconDescription}. Tap to inspect."
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) item.color.copy(alpha = 0.25f) else AccessibleDarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, item.color)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = item.color,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Big Central "बोलकर सवाल पूछें (Ask by Voice)" Primary Button
            Surface(
                onClick = onMicClick,
                shape = RoundedCornerShape(16.dp),
                color = HighContrastYellow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("ai_voice_ask_button")
                    .semantics {
                        contentDescription = "Speak your custom visual question to Gemini AI. Tap to speak."
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = AccessibleBlack,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "बोलकर सवाल पूछें (Ask AI)",
                        color = AccessibleBlack,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class ModeItem(
    val mode: AiVisionMode,
    val title: String,
    val icon: ImageVector,
    val color: Color
)
