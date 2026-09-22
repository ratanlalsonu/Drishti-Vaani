package com.example.feature.ocr

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.feature.camera.CameraManager
import com.example.ui.theme.AccessibleBlack
import com.example.ui.theme.AccessibleDarkSurface
import com.example.ui.theme.HighContrastCyan
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.HighContrastYellow

@Composable
fun OCRScreen(
    uiState: OcrUiState,
    ocrViewModel: OCRViewModel,
    onBackClick: () -> Unit,
    onMicClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraManager = remember {
        CameraManager(context, lifecycleOwner)
    }

    var previewViewRef by remember { androidx.compose.runtime.mutableStateOf<PreviewView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.shutdown(previewViewRef)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AccessibleBlack)
            .semantics {
                contentDescription = "OCR Text Reader screen. Camera is scanning for documents and text."
            }
    ) {
        // Camera Preview Feed
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewViewRef = this
                    cameraManager.startCamera(
                        previewView = this,
                        analyzer = ocrViewModel.createAnalyzer()
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )


        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AccessibleDarkSurface)
                    .testTag("ocr_back_button")
                    .semantics { contentDescription = "Back to home screen" }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = HighContrastYellow,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Voice command Mic button
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(HighContrastCyan)
                    .testTag("ocr_mic_button")
                    .semantics { contentDescription = "Boliye: Bol kar command dein ya dashboard par jayein" }
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = AccessibleBlack,
                    modifier = Modifier.size(32.dp)
                )
            }
        }


        // Bottom OCR Results & Audio Playback Controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
                .semantics {
                    contentDescription = "OCR Extracted Text: ${uiState.detectedText.ifEmpty { uiState.statusMessage }}"
                },
            colors = CardDefaults.cardColors(
                containerColor = AccessibleDarkSurface.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "ON-DEVICE OCR READER",
                    color = HighContrastCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = uiState.detectedText.ifEmpty { uiState.statusMessage },
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Voice-first / Large button playback controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (uiState.isReading) {
                                ocrViewModel.pauseReading()
                            } else {
                                ocrViewModel.resumeReading()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HighContrastYellow,
                            contentColor = AccessibleBlack
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("ocr_pause_resume_button")
                            .semantics {
                                contentDescription = if (uiState.isReading) "Pause reading text" else "Resume reading text"
                            }
                    ) {
                        Icon(
                            imageVector = if (uiState.isReading) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Text(
                            text = if (uiState.isReading) " Pause" else " Resume",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Button(
                        onClick = { ocrViewModel.repeatReading() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HighContrastGreen,
                            contentColor = AccessibleBlack
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("ocr_repeat_button")
                            .semantics {
                                contentDescription = "Repeat reading detected text"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null
                        )
                        Text(
                            text = " Repeat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
