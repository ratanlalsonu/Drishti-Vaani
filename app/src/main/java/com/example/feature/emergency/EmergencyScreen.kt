package com.example.feature.emergency

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.AccessibleBlack
import com.example.ui.theme.AccessibleDarkSurface
import com.example.ui.theme.HighContrastCyan
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.HighContrastRed
import com.example.ui.theme.HighContrastYellow

@Composable
fun EmergencyScreen(
    uiState: EmergencyUiState,
    emergencyViewModel: EmergencyViewModel,
    onBackClick: () -> Unit,
    onMicClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

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
            // Top Bar
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
                        .testTag("emergency_back_button")
                        .semantics { contentDescription = "Back to home screen" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = HighContrastYellow,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Voice mic button
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(HighContrastCyan)
                        .testTag("emergency_mic_button")
                        .semantics { contentDescription = "Boliye: Bol kar command dein ya dashboard par jayein" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = AccessibleBlack,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = { showAddDialog = !showAddDialog },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AccessibleDarkSurface)
                        .testTag("emergency_add_contact_button")
                        .semantics { contentDescription = "Add new emergency contact" }
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = HighContrastYellow,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }


            // Central Massive SOS Trigger
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clickable {
                        emergencyViewModel.triggerEmergencySOS()
                    }
                    .testTag("sos_trigger_card")
                    .semantics {
                        contentDescription = "Double tap to trigger Emergency SOS call and alerts"
                    },
                colors = CardDefaults.cardColors(containerColor = HighContrastRed),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AccessibleBlack,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TRIGGER SOS (आपातकाल)",
                        color = AccessibleBlack,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Tap or say 'Emergency'",
                        color = AccessibleBlack,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Direct Call Confirm Card
            if (uiState.primaryContact != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AccessibleDarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, HighContrastGreen)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PRIMARY CONTACT",
                            color = HighContrastGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.primaryContact.name} (${uiState.primaryContact.phoneNumber})",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Button(
                            onClick = { emergencyViewModel.makeEmergencyCall() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HighContrastGreen,
                                contentColor = AccessibleBlack
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("direct_call_contact_button")
                                .semantics {
                                    contentDescription = "Call ${uiState.primaryContact.name} now"
                                }
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = null)
                            Text(
                                text = " CALL ${uiState.primaryContact.name.uppercase()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Add Contact Panel (Inline high-contrast input)
            if (showAddDialog) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AccessibleDarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, HighContrastYellow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Add Emergency Contact",
                            color = HighContrastYellow,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Name (e.g. Papa, Rahul)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = HighContrastYellow,
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { newPhone = it },
                            label = { Text("Phone Number") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = HighContrastYellow,
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                emergencyViewModel.saveContact(newName, newPhone, isPrimary = true)
                                newName = ""
                                newPhone = ""
                                showAddDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HighContrastYellow, contentColor = AccessibleBlack),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SAVE CONTACT", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Registered Contacts List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(uiState.contacts) { contact ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                emergencyViewModel.makeEmergencyCall(contact)
                            },
                        colors = CardDefaults.cardColors(containerColor = AccessibleDarkSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = contact.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(text = contact.phoneNumber, color = HighContrastCyan, fontSize = 13.sp)
                            }
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = HighContrastGreen)
                        }
                    }
                }
            }
        }
    }
}
