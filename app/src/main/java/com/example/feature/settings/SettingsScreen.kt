package com.example.feature.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AssistantLanguage
import com.example.ui.theme.AccessibleBlack
import com.example.ui.theme.AccessibleDarkSurface
import com.example.ui.theme.HighContrastCyan
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.HighContrastYellow

@Composable
fun SettingsScreen(
    currentLanguage: AssistantLanguage,
    onLanguageSelected: (AssistantLanguage) -> Unit,
    onBackClick: () -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var hapticsEnabled by remember { mutableStateOf(true) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AccessibleDarkSurface)
                        .testTag("settings_back_button")
                        .semantics { contentDescription = "Back to home screen" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = HighContrastYellow,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "SETTINGS & VOICE",
                    color = HighContrastYellow,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Language Selection
            Text(
                text = "ASSISTANT LANGUAGE (बोली)",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LanguageChip(
                    title = "हिंदी (Hindi)",
                    isSelected = currentLanguage == AssistantLanguage.HINDI,
                    onClick = { onLanguageSelected(AssistantLanguage.HINDI) },
                    modifier = Modifier.weight(1f)
                )
                LanguageChip(
                    title = "English (IN)",
                    isSelected = currentLanguage == AssistantLanguage.ENGLISH_IN,
                    onClick = { onLanguageSelected(AssistantLanguage.ENGLISH_IN) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Speech Rate Slider
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AccessibleDarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "VOICE SPEED: ${"%.1f".format(speechRate)}x",
                        color = HighContrastYellow,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            speechRate = it
                            onSpeechRateChanged(it)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = HighContrastYellow,
                            activeTrackColor = HighContrastYellow,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Voice speed slider, currently ${"%.1f".format(speechRate)}"
                        }
                    )
                }
            }

            // Haptic Feedback Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AccessibleDarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HAPTIC FEEDBACK",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vibration alerts for obstacles & hazards",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = { hapticsEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HighContrastGreen,
                            checkedTrackColor = AccessibleDarkSurface
                        )
                    )
                }
            }

            // Improved Speech Recognition Feature Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AccessibleDarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "IMPROVED SPEECH RECOGNITION (उन्नत आवाज़ पहचान)",
                        color = HighContrastGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• द्विभाषी समर्थन (Bilingual): शुद्ध हिंदी, हिंग्लिश और अंग्रेज़ी में प्राकृतिक कमांड पहचान।\n• मल्टी-कैंडिडेट पार्सिंग: शोर में भी 5 अलग-अलग वॉइस अनुमानों से सही कमांड चुनना।\n• रियल-टाइम फीडबैक: बोलते समय स्क्रीन पर लाइव शब्द और ऑडियो वेवफॉर्म।\n• स्मार्ट टाइमिंग: बोलते समय बीच में रुकने पर माइक तुरंत बंद नहीं होता।\n• स्पर्श संकेत (Haptics): माइक ऑन होने और कमांड समझने पर विशेष वाइब्रेशन।",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            // Academic Project Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AccessibleDarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PROJECT: DRISHTI VAANI",
                        color = HighContrastCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Final-Year B.Tech IT Major Project\nReal-time On-Device Computer Vision, OCR & Voice Assistance for Blind Users.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Select language: $title" },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) HighContrastYellow else AccessibleDarkSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = if (isSelected) AccessibleBlack else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
