package com.example.feature.vision

import android.graphics.Paint
import android.graphics.Typeface
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.model.DetectedObject
import com.example.core.model.DetectionRangeLimit
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
    onIdentifyObject: (android.graphics.Bitmap?) -> Unit = {},
    onFrameAvailable: (android.graphics.Bitmap?) -> Unit = {},
    onRangeToggle: () -> Unit = {},
    onMicClick: () -> Unit = {},
    onDetectionsReceived: (List<DetectedObject>, Int, Int) -> Unit,
    onStartDirectionTracking: () -> Unit = {},
    onStopDirectionTracking: () -> Unit = {},
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

    // Keep helper's threshold updated whenever UI state range changes
    LaunchedEffect(uiState.rangeLimit) {
        detectorHelper.maxRangeMeters = uiState.rangeLimit.maxMeters
    }

    val cameraManager = remember {
        CameraManager(context, lifecycleOwner)
    }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Keep latest preview bitmap available for immediate voice inspection
    LaunchedEffect(previewViewRef) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            previewViewRef?.bitmap?.let { bmp ->
                onFrameAvailable(bmp)
            }
        }
    }

    DisposableEffect(Unit) {
        onStartDirectionTracking()
        onDispose {
            cameraManager.shutdown(previewViewRef)
            detectorHelper.close()
            onStopDirectionTracking()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AccessibleBlack)
            .semantics {
                contentDescription = "Live Vision Screen. Point phone forward. Voice announcements and distance estimation active."
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

        // Bounding Boxes & Distance Canvas Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val previewW = uiState.previewWidth.toFloat()
            val previewH = uiState.previewHeight.toFloat()

            if (previewW > 0 && previewH > 0) {
                val scaleX = canvasW / previewW
                val scaleY = canvasH / previewH

                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 34f
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                val bgPaint = Paint().apply {
                    color = android.graphics.Color.argb(210, 0, 0, 0)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }

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

                    // Draw bounding box
                    drawRect(
                        color = strokeColor,
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        style = Stroke(width = 6f)
                    )

                    // Draw Object Label and Distance text badge
                    val displayText = "${obj.hindiLabel} • ~${obj.estimatedDistanceMeters}m"
                    val textWidth = textPaint.measureText(displayText)
                    val badgeHeight = 44f
                    val badgeY = (top - 8f).coerceAtLeast(badgeHeight + 10f)

                    drawContext.canvas.nativeCanvas.drawRoundRect(
                        left,
                        badgeY - badgeHeight,
                        left + textWidth + 24f,
                        badgeY + 8f,
                        10f,
                        10f,
                        bgPaint
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        displayText,
                        left + 12f,
                        badgeY - 8f,
                        textPaint
                    )
                }
            }
        }

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(AccessibleDarkSurface)
                    .testTag("vision_back_button")
                    .semantics { contentDescription = "Back to home screen" }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = HighContrastYellow,
                    modifier = Modifier.size(30.dp)
                )
            }

            // Detection Range Selector Chip (Threshold control)
            Surface(
                onClick = onRangeToggle,
                shape = RoundedCornerShape(24.dp),
                color = AccessibleDarkSurface,
                modifier = Modifier
                    .height(52.dp)
                    .testTag("vision_range_chip")
                    .semantics {
                        contentDescription = "Current detection range threshold: ${uiState.rangeLimit.labelHi}. Tap to change."
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        tint = HighContrastCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "RANGE LIMIT",
                            color = HighContrastCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.rangeLimit.labelHi,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right Action Buttons (Mic, Scene Info, Identify Object)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Identify Object Button (Exact item identification)
                IconButton(
                    onClick = { onIdentifyObject(previewViewRef?.bitmap) },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(HighContrastGreen)
                        .testTag("vision_identify_button")
                        .semantics { contentDescription = "वस्तु पहचानो। सामने रखी वस्तु का सही नाम और पहचान।" }
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = AccessibleBlack,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Voice Command Mic button
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(HighContrastCyan)
                        .testTag("vision_mic_button")
                        .semantics { contentDescription = "Voice Command. Speak to change range or navigate." }
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = AccessibleBlack,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Summary trigger button
                IconButton(
                    onClick = onQuerySurroundings,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(HighContrastYellow)
                        .testTag("vision_summary_button")
                        .semantics { contentDescription = "Speak scene summary: What objects are in range?" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AccessibleBlack,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Direction Orientation & Spatial Status Chip
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AccessibleDarkSurface.copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (!uiState.isDirectionSettled) HighContrastYellow else HighContrastCyan
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 114.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .testTag("direction_status_chip")
                .semantics {
                    contentDescription = "Direction: ${uiState.currentDirectionHi}. ${if (uiState.hasAnnouncedCurrentDirection) "Announced once. Say what is ahead to hear again." else "Scanning direction."}"
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = HighContrastYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.currentDirectionHi,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (!uiState.isDirectionSettled) {
                        "नई दिशा स्कैन हो रही है..."
                    } else if (uiState.hasAnnouncedCurrentDirection) {
                        "✓ एक बार बोला गया • 'सामने क्या है' बोलें"
                    } else {
                        "पहचान जारी है..."
                    },
                    color = if (!uiState.isDirectionSettled) HighContrastYellow else HighContrastGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Dedicated Prominent Action Bar: "IDENTIFY OBJECT / वस्तु पहचानो"
        Button(
            onClick = { onIdentifyObject(previewViewRef?.bitmap) },
            enabled = !uiState.isIdentifyingObject,
            colors = ButtonDefaults.buttonColors(
                containerColor = HighContrastGreen,
                contentColor = AccessibleBlack
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = if (uiState.identifiedResult != null || uiState.detections.isNotEmpty()) 230.dp else 160.dp)
                .height(54.dp)
                .testTag("identify_object_action_button")
                .semantics {
                    contentDescription = "Identify exact object name and brand in front of camera"
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (uiState.isIdentifyingObject) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = AccessibleBlack,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "वस्तु की पहचान की जा रही है...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "IDENTIFY OBJECT (वस्तु पहचानो)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Bottom High-Contrast Spoken Feedback & Distance Cards
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
                .semantics {
                    contentDescription = "Vision feedback: ${uiState.lastVocalized}"
                },
            colors = CardDefaults.cardColors(
                containerColor = AccessibleDarkSurface.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE VISION ASSISTANT",
                        color = HighContrastGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Threshold: ${uiState.rangeLimit.maxMeters.toInt()}m Max",
                        color = HighContrastCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Show identified exact object card if available
                if (uiState.identifiedResult != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AccessibleBlack),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, HighContrastGreen)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "पहचानी गई वस्तु: ${uiState.identifiedResult.title}",
                                    color = HighContrastGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (uiState.identifiedResult.isAiVerified) "AI Vision" else "On-Device",
                                    color = HighContrastYellow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.identifiedResult.description,
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Text(
                    text = uiState.lastVocalized.ifEmpty { "वस्तुओं और दूरी को स्कैन किया जा रहा है..." },
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // List of detected objects with distance badges
                if (uiState.detections.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (detected in uiState.detections) {
                            val badgeBorderColor = when (detected.priority) {
                                PriorityLevel.CRITICAL -> HighContrastRed
                                PriorityLevel.HIGH -> HighContrastYellow
                                else -> HighContrastGreen
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccessibleBlack)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${detected.hindiLabel} • ~${detected.estimatedDistanceMeters}m (${detected.position.spokenLabelHi})",
                                    color = badgeBorderColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
