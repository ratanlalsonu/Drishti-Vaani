package com.example.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccessibleBlack
import com.example.ui.theme.AccessibleDarkSurface
import com.example.ui.theme.HighContrastBorder
import com.example.ui.theme.HighContrastCyan
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.HighContrastRed
import com.example.ui.theme.HighContrastYellow

@Composable
fun HomeScreen(
    uiState: AssistantUiState,
    onMicClick: () -> Unit,
    onNavigate: (String) -> Unit,
    onRepeatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(AccessibleBlack),
        color = AccessibleBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Drishti Vaani Title & Voice Status Card
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "दृष्टि वाणी",
                            color = HighContrastYellow,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics {
                                contentDescription = "Drishti Vaani Assistive Companion"
                            }
                        )
                        Text(
                            text = "Drishti Vaani Assistant",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Quick Audio Repeat Button (Accessible 48dp+ target)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(AccessibleDarkSurface)
                            .clickable(onClick = onRepeatClick)
                            .testTag("repeat_speech_button")
                            .semantics {
                                contentDescription = "Repeat last spoken feedback"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = HighContrastYellow,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Assistant Feedback Box (Read aloud and also displayed high-contrast)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Assistant message: ${uiState.assistantFeedback}"
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = AccessibleDarkSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, HighContrastCyan)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (uiState.isListening) "Listening..." else "Assistant Status",
                            color = if (uiState.isListening) HighContrastGreen else HighContrastCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.assistantFeedback.ifEmpty { "Aap bol sakte hain: 'कैमरा चालू करो', 'सामने क्या है', 'किताब पढ़ो', या 'मदद'." },
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 24.sp
                        )
                        if (uiState.partialRecognizedText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = HighContrastCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "लाइव: \"${uiState.partialRecognizedText}...\"",
                                    color = HighContrastCyan,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (uiState.lastRecognizedText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You said: \"${uiState.lastRecognizedText}\"",
                                color = HighContrastYellow,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Central Massive Microphone Voice Activation Tile with Audio Pulse Animation
            val audioScale by animateFloatAsState(
                targetValue = if (uiState.isListening) 1.0f + (uiState.audioLevel * 0.22f) else 1.0f,
                animationSpec = tween(durationMillis = 100),
                label = "audio_scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(144.dp)
                            .scale(audioScale),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glowing audio ring when listening
                        if (uiState.isListening) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(HighContrastGreen.copy(alpha = 0.20f + (uiState.audioLevel * 0.40f)))
                            )
                        }

                        // Main action microphone button
                        Box(
                            modifier = Modifier
                                .size(122.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isListening) HighContrastGreen else HighContrastYellow)
                                .clickable(onClick = onMicClick)
                                .testTag("voice_command_trigger")
                                .semantics {
                                    contentDescription = if (uiState.isListening) {
                                        "Hands free microphone active and listening. Tap to pause."
                                    } else {
                                        "Hands free microphone ready. Just speak anytime, no tap needed."
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = null,
                                tint = AccessibleBlack,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (uiState.isListening) "सुन रहे हैं... (Listening)" else "हैंड्स-फ्री एक्टिव (कभी भी बोलें)",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "टैप करने की ज़रूरत नहीं - आवाज़ अपने आप काम करेगी",
                        color = HighContrastYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Large Touch Grid (High-contrast, generous touch targets for sighted helpers / low vision)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccessibleTile(
                        title = "Start Vision",
                        subtitle = "वस्तु पहचान व बाधाएं",
                        icon = Icons.Default.Camera,
                        accentColor = HighContrastYellow,
                        modifier = Modifier.weight(1f),
                        testTag = "tile_start_vision",
                        onClick = { onNavigate("vision") }
                    )
                    AccessibleTile(
                        title = "Read Text",
                        subtitle = "OCR टेक्स्ट",
                        icon = Icons.Default.TextFields,
                        accentColor = HighContrastCyan,
                        modifier = Modifier.weight(1f),
                        testTag = "tile_read_text",
                        onClick = { onNavigate("ocr") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccessibleTile(
                        title = "Navigation",
                        subtitle = "GPS दिशा",
                        icon = Icons.Default.NearMe,
                        accentColor = HighContrastYellow,
                        modifier = Modifier.weight(1f),
                        testTag = "tile_navigation",
                        onClick = { onNavigate("navigation") }
                    )
                    AccessibleTile(
                        title = "Emergency",
                        subtitle = "SOS आपातकाल",
                        icon = Icons.Default.Emergency,
                        accentColor = HighContrastRed,
                        modifier = Modifier.weight(1f),
                        testTag = "tile_emergency",
                        onClick = { onNavigate("emergency") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccessibleTile(
                        title = "Voice Help",
                        subtitle = "मदद और कमांड",
                        icon = Icons.AutoMirrored.Filled.Help,
                        accentColor = HighContrastYellow,
                        modifier = Modifier.weight(1f),
                        testTag = "tile_help",
                        onClick = { onNavigate("help") }
                    )
                    AccessibleTile(
                        title = "Settings",
                        subtitle = "सेटिंग्स",
                        icon = Icons.Default.Settings,
                        accentColor = Color.White,
                        modifier = Modifier.weight(1f),
                        testTag = "tile_settings",
                        onClick = { onNavigate("settings") }
                    )
                }
            }
        }
    }
}

@Composable
fun AccessibleTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(76.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .semantics {
                contentDescription = "$title, $subtitle"
            },
        colors = CardDefaults.cardColors(
            containerColor = AccessibleDarkSurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, accentColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = HighContrastYellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
