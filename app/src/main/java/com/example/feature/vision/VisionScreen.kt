package com.example.feature.vision

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.model.PriorityLevel
import com.example.feature.camera.CameraManager
import com.example.ui.theme.AccessibleBlack
import com.example.ui.theme.AccessibleDarkSurface
import com.example.ui.theme.HighContrastCyan
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.HighContrastRed
import com.example.ui.theme.HighContrastYellow

@Composable
fun VisionScreen(
    uiState: VisionUiState,
    onBackClick: () -> Unit,
    onQuerySurroundings: () -> Unit,
    onMicClick: () -> Unit = {},
    onDetectionsReceived: (List<com.example.core.model.DetectedObject>, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val detectorHelper = remember {
        ObjectDetectorHelper(
            onDetectionsReady = { objects, width, height ->
                onDetectionsReceived(objects, width, height)
            }
        )
    }

    val cameraManager = remember {
        CameraManager(context, lifecycleOwner)
    }

    var previewViewRef by remember { androidx.compose.runtime.mutableStateOf<PreviewView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.shutdown(previewViewRef)
            detectorHelper.close()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AccessibleBlack)
            .semantics {
                contentDescription = "Live Vision Screen. Point phone forward. Voice announcements are active."
            }
    ) {
        // CameraX Preview View
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewViewRef = this
                    cameraManager.startCamera(
                        previewView = this,
                        analyzer = detectorHelper
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )


        // Bounding Boxes Canvas Overlay (for sighted helpers / testers)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val previewW = uiState.previewWidth.toFloat()
            val previewH = uiState.previewHeight.toFloat()

            if (previewW > 0 && previewH > 0) {
                val scaleX = canvasW / previewW
                val scaleY = canvasH / previewH

                for (obj in uiState.detections) {
                    val box = obj.boundingBox
                    val left = box.left * scaleX
                    val top = box.top * scaleY
                    val width = box.width() * scaleX
                    val height = box.height() * scaleY

                    val strokeColor = when (obj.priority) {
                        PriorityLevel.CRITICAL -> HighContrastRed
                        PriorityLevel.HIGH -> HighContrastYellow
                        else -> HighContrastGreen
                    }

                    drawRect(
                        color = strokeColor,
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        style = Stroke(width = 6f)
                    )
                }
            }
        }

        // Top Control Bar
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
                    .testTag("vision_back_button")
                    .semantics { contentDescription = "Back to home screen" }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = HighContrastYellow,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Voice Command Mic button
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(HighContrastCyan)
                    .testTag("vision_mic_button")
                    .semantics { contentDescription = "Boliye: Bol kar command dein ya dashboard par jayein" }
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = AccessibleBlack,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Summary trigger button
            IconButton(
                onClick = onQuerySurroundings,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(HighContrastYellow)
                    .testTag("vision_summary_button")
                    .semantics { contentDescription = "What is around me? Speak scene summary" }
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AccessibleBlack,
                    modifier = Modifier.size(32.dp)
                )
            }

        }

        // Bottom High-Contrast Spoken Feedback Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
                .semantics {
                    contentDescription = "Latest vision announcement: ${uiState.lastVocalized}"
                },
            colors = CardDefaults.cardColors(
                containerColor = AccessibleDarkSurface.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "LIVE VISION ASSISTANT",
                        color = HighContrastGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.cameraStatus,
                        color = HighContrastCyan,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = uiState.lastVocalized.ifEmpty { "Scanning surroundings..." },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
