package com.example.feature.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccessibleBlack
import com.example.ui.theme.AccessibleDarkSurface
import com.example.ui.theme.HighContrastCyan
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.HighContrastYellow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NavigationScreen(
    uiState: NavigationUiState,
    navigationViewModel: NavigationViewModel,
    onBackClick: () -> Unit,
    onMicClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    LaunchedEffect(Unit) {
        navigationViewModel.onNavigationScreenOpened()
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(AccessibleDarkSurface)
                        .testTag("nav_back_button")
                        .semantics { contentDescription = "Back to home dashboard" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = HighContrastYellow,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "लोकेशन व दिशा",
                        color = HighContrastYellow,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "LOCATION & DIRECTION",
                        color = HighContrastCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Voice mic button
                    IconButton(
                        onClick = onMicClick,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(HighContrastCyan)
                            .testTag("nav_mic_button")
                            .semantics { contentDescription = "Voice Assistant: Speak any command" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = AccessibleBlack,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // GPS Refresh & Speak Button
                    IconButton(
                        onClick = { navigationViewModel.requestLocationSpokenOnDemand() },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(HighContrastYellow)
                            .testTag("nav_refresh_gps_button")
                            .semantics { contentDescription = "Hear current location and facing compass direction" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = AccessibleBlack,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // 1. Facing Direction & Compass Card (दिशा व कंपास)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_facing_direction")
                        .semantics {
                            contentDescription = "Facing direction: ${uiState.currentDirectionHi}, Azimuth ${uiState.currentAzimuth.toInt()} degrees"
                        },
                    colors = CardDefaults.cardColors(containerColor = AccessibleDarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, HighContrastYellow)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = null,
                                    tint = HighContrastYellow,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "FACING DIRECTION (आपकी दिशा)",
                                    color = HighContrastYellow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "${uiState.currentAzimuth.toInt()}°",
                                color = HighContrastCyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Large Direction Name
                        Text(
                            text = if (uiState.currentDirectionHi.isNotBlank()) uiState.currentDirectionHi else "दिशा जांची जा रही है...",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "आपका मुख इस दिशा की ओर है",
                            color = HighContrastGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Visual High-Contrast Compass Dial
                        CompassDial(
                            azimuth = uiState.currentAzimuth,
                            modifier = Modifier
                                .size(160.dp)
                                .testTag("compass_dial")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 8-Direction Quick Guide
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DirectionChip(label = "उ (North)", isActive = isHeadingNear(uiState.currentAzimuth, 0f))
                            DirectionChip(label = "पू (East)", isActive = isHeadingNear(uiState.currentAzimuth, 90f))
                            DirectionChip(label = "द (South)", isActive = isHeadingNear(uiState.currentAzimuth, 180f))
                            DirectionChip(label = "प (West)", isActive = isHeadingNear(uiState.currentAzimuth, 270f))
                        }
                    }
                }

                // 2. Real GPS Location Card (वर्तमान स्थिति)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_current_location")
                        .semantics {
                            contentDescription = "Current GPS location: ${uiState.currentAddress}"
                        },
                    colors = CardDefaults.cardColors(containerColor = AccessibleDarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, if (uiState.isLocating) HighContrastGreen else HighContrastCyan)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.isLocating) "● GPS ढूँढ रहा है..." else "● वर्तमान स्थिति (CURRENT LOCATION)",
                                color = if (uiState.isLocating) HighContrastGreen else HighContrastCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (uiState.isLocating) HighContrastGreen else HighContrastCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Full Address Text
                        Text(
                            text = uiState.currentAddress,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 26.sp
                        )

                        if (uiState.latitude != null && uiState.longitude != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccessibleBlack, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Lat: ${"%.5f".format(uiState.latitude)}",
                                    color = HighContrastYellow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Lng: ${"%.5f".format(uiState.longitude)}",
                                    color = HighContrastYellow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "GPS High Accuracy",
                                    color = HighContrastGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 3. Hear Now Button (Large Touch Target > 56dp)
                Button(
                    onClick = { navigationViewModel.requestLocationSpokenOnDemand() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_speak_location_direction"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HighContrastYellow,
                        contentColor = AccessibleBlack
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = AccessibleBlack,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "लोकेशन व दिशा बोलकर सुनें",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // 4. Voice Guidance Instruction Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_voice_tip"),
                    colors = CardDefaults.cardColors(containerColor = AccessibleDarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, HighContrastCyan)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "💡 आवाज़ से पूछने का तरीक़ा:",
                            color = HighContrastCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "किसी भी समय बोलें: 'नेविगेशन', 'दिशा' या 'मेरी लोकेशन' — ऐप तुरंत आपका स्थान और आप किस दिशा में मुख किए हैं, बोलकर बताएगा।",
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "स्क्रीन पहली बार खोलने पर 2 बार सुनाता है, उसके बाद सिर्फ़ पूछने पर सुनाता है।",
                            color = HighContrastGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Visual Compass Dial Composable
 * Draws a high-contrast compass ring with cardinal markers and a needle oriented to device azimuth.
 */
@Composable
private fun CompassDial(
    azimuth: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 8.dp.toPx()

            // Outer Circle
            drawCircle(
                color = HighContrastYellow,
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Inner Background Circle
            drawCircle(
                color = AccessibleBlack,
                radius = radius - 2.dp.toPx(),
                center = center
            )

            // Minor tick marks around dial (every 45 degrees)
            for (i in 0 until 8) {
                val angleDeg = i * 45.0
                val angleRad = Math.toRadians(angleDeg)
                val outerX = center.x + (radius - 2.dp.toPx()) * sin(angleRad).toFloat()
                val outerY = center.y - (radius - 2.dp.toPx()) * cos(angleRad).toFloat()
                val innerX = center.x + (radius - 12.dp.toPx()) * sin(angleRad).toFloat()
                val innerY = center.y - (radius - 12.dp.toPx()) * cos(angleRad).toFloat()
                drawLine(
                    color = if (i % 2 == 0) HighContrastYellow else Color.Gray,
                    start = Offset(innerX, innerY),
                    end = Offset(outerX, outerY),
                    strokeWidth = if (i % 2 == 0) 3.dp.toPx() else 1.5.dp.toPx()
                )
            }

            // Rotating Needle pointing to facing heading
            rotate(degrees = azimuth, pivot = center) {
                val needlePath = Path().apply {
                    // North pointer (Yellow / Red high-contrast tip)
                    moveTo(center.x, center.y - radius + 14.dp.toPx())
                    lineTo(center.x - 12.dp.toPx(), center.y)
                    lineTo(center.x + 12.dp.toPx(), center.y)
                    close()
                }
                drawPath(
                    path = needlePath,
                    color = HighContrastYellow
                )

                // South pointer (Cyan bottom)
                val southPath = Path().apply {
                    moveTo(center.x, center.y + radius - 14.dp.toPx())
                    lineTo(center.x - 12.dp.toPx(), center.y)
                    lineTo(center.x + 12.dp.toPx(), center.y)
                    close()
                }
                drawPath(
                    path = southPath,
                    color = HighContrastCyan
                )

                // Center pivot cap
                drawCircle(
                    color = AccessibleBlack,
                    radius = 8.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = center
                )
            }
        }
    }
}

@Composable
private fun DirectionChip(
    label: String,
    isActive: Boolean
) {
    Surface(
        color = if (isActive) HighContrastYellow else AccessibleDarkSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isActive) HighContrastYellow else Color.DarkGray)
    ) {
        Text(
            text = label,
            color = if (isActive) AccessibleBlack else Color.White,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun isHeadingNear(azimuth: Float, target: Float): Boolean {
    val norm = (azimuth % 360f + 360f) % 360f
    val diff = kotlin.math.abs(norm - target)
    return diff <= 22.5f || (360f - diff) <= 22.5f
}
