package com.example.feature.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.AccessibleBlack
import com.example.ui.theme.AccessibleDarkSurface
import com.example.ui.theme.HighContrastCyan
import com.example.ui.theme.HighContrastYellow

data class VoiceCommandHelpItem(
    val englishCmd: String,
    val hindiCmd: String,
    val purpose: String
)

@Composable
fun VoiceHelpScreen(
    onBackClick: () -> Unit,
    onSpeakHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val commandsList = listOf(
        VoiceCommandHelpItem("Go to Dashboard", "Dashboard par le chalo / Home wapas jao", "Kahin se bhi seedhe main dashboard par wapas lene ke liye"),
        VoiceCommandHelpItem("Open Vision", "Vision par le chalo / Camera chalu karo", "Real-time camera rukavat detection shuru karein"),
        VoiceCommandHelpItem("Identify Object", "Vastu pehchano / Yeh kaun si vastu hai / Vastu ka naam batao", "Samne rakhi kisi bhi vastu ka sahi naam, brand aur pehchan batata hai"),
        VoiceCommandHelpItem("Set Range 10 Meters", "10 meter range / Das meter", "10 meter tak ki doori ke objects detect karein (Door ki cheezein filter karein)"),
        VoiceCommandHelpItem("Set Range 5 Meters", "5 meter range / Paanch meter", "Ghar ke andar 5 meter ke daayre ke objects detect karein"),
        VoiceCommandHelpItem("What is ahead?", "Samne kya hai / Aas paas kya hai", "Samne dikh rahe objects ke naam aur unki doori suniye"),
        VoiceCommandHelpItem("Read Text", "Text reader par le chalo / Isko padho", "Kitab, kagaz ya board ka text sunne ke liye"),
        VoiceCommandHelpItem("Where am I?", "Navigation par le chalo / Main kahan hoon?", "GPS sthiti aur rasta jaan ne ke liye"),
        VoiceCommandHelpItem("What is in front of me?", "Samne kya hai?", "Samne ki rukavaton aur logon ki jaankari"),
        VoiceCommandHelpItem("Direct Call by Name", "Call [Name] / [Name] ko call karo", "Kisi bhi contact ka naam bol kar turant seedhi phone call lagayein (Jaise: 'Call Papa')"),
        VoiceCommandHelpItem("Direct Emergency Call", "Call Emergency / 112 par call karo", "Bina kisi click ke turant emergency helpline ya primary contact ko call karein"),
        VoiceCommandHelpItem("Emergency SOS", "Emergency par le chalo / Bachao / Madad", "Emergency screen par jaane ke liye"),
        VoiceCommandHelpItem("Battery Level", "Battery kitni hai?", "Phone ki battery percentage bolkar batata hai"),
        VoiceCommandHelpItem("Current Time", "Kitne baje hain? / Samay kya hai?", "Vartaman samay bolkar batata hai"),
        VoiceCommandHelpItem("Repeat", "Phir se bolo", "Aakhri boli gayi jaankari dobara sunayein"),
        VoiceCommandHelpItem("Stop speaking", "Chup ho jao / Shant raho", "Assistant ki aawaz turant band karein")
    )


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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AccessibleDarkSurface)
                        .testTag("help_back_button")
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
                    text = "VOICE COMMANDS GUIDE",
                    color = HighContrastYellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onSpeakHelp,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(HighContrastYellow)
                        .testTag("speak_help_button")
                        .semantics { contentDescription = "Speak full voice command instructions aloud" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = AccessibleBlack,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Text(
                text = "Bolkar kaam karein (Natural Voice Commands):",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(commandsList) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Command: ${item.englishCmd} or ${item.hindiCmd}. Action: ${item.purpose}"
                            },
                        colors = CardDefaults.cardColors(containerColor = AccessibleDarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "• \"${item.englishCmd}\"",
                                color = HighContrastYellow,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "  या: \"${item.hindiCmd}\"",
                                color = HighContrastCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.purpose,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
