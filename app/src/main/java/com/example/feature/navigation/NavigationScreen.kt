package com.example.feature.navigation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Security

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.home.AccessibleTile
import com.example.ui.theme.AccessibleBlack
import com.example.ui.theme.AccessibleDarkSurface
import com.example.ui.theme.HighContrastCyan
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.HighContrastYellow

@Composable
fun NavigationScreen(
    uiState: NavigationUiState,
    navigationViewModel: NavigationViewModel,
    onBackClick: () -> Unit,
    onMicClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    LaunchedEffect(Unit) {
        navigationViewModel.requestCurrentLocation()
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
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
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
                        .testTag("nav_back_button")
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
                    text = "GPS & NEARBY",
                    color = HighContrastYellow,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                // Voice mic button
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(HighContrastCyan)
                        .testTag("nav_mic_button")
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
                    onClick = { navigationViewModel.requestCurrentLocation() },

                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(HighContrastYellow)
                        .testTag("nav_refresh_gps_button")
                        .semantics { contentDescription = "Refresh GPS location" }
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = AccessibleBlack,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // Current Real Location Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Current location: ${uiState.currentAddress}"
                    },
                colors = CardDefaults.cardColors(
                    containerColor = AccessibleDarkSurface
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(2.dp, HighContrastCyan)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (uiState.isLocating) "LOCATING VIA HARDWARE GPS..." else "CURRENT POSITION",
                        color = if (uiState.isLocating) HighContrastGreen else HighContrastCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = uiState.currentAddress,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp
                    )

                    if (uiState.latitude != null && uiState.longitude != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "GPS Coordinates: ${"%.5f".format(uiState.latitude)}, ${"%.5f".format(uiState.longitude)}",
                            color = HighContrastYellow,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Active Navigation Route Card (When navigating to Police, Hospital, etc.)
            if (uiState.isNavigating && uiState.activeDestination.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_active_navigation")
                        .semantics {
                            contentDescription = "Active Walking Navigation to ${uiState.activeDestination}"
                        },
                    colors = CardDefaults.cardColors(containerColor = AccessibleDarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, HighContrastGreen)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                    contentDescription = null,
                                    tint = HighContrastGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Column {
                                    Text(
                                        text = "ACTIVE WALKING ROUTE",
                                        color = HighContrastGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${uiState.activeDestination} (${uiState.activeDestinationHi})",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(
                                onClick = { navigationViewModel.stopNavigation() },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(HighContrastYellow)
                                    .semantics { contentDescription = "Stop navigation" }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = AccessibleBlack,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { navigationViewModel.launchNavigationIntent() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_open_google_maps"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HighContrastGreen,
                                contentColor = AccessibleBlack
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "OPEN IN GOOGLE MAPS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Quick Spoken Nearby Places (Voice accessible & High contrast)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "FIND NEARBY ESSENTIALS (Bolkar bhi khojein)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccessibleTile(
                        title = "Hospital",
                        subtitle = "अस्पताल",
                        icon = Icons.Default.LocalHospital,
                        accentColor = HighContrastYellow,
                        modifier = Modifier.weight(1f),
                        testTag = "tile_nearby_hospital",
                        onClick = { navigationViewModel.findNearbyFacility("hospital") }
                    )
                    AccessibleTile(
                        title = "Pharmacy",
                        subtitle = "दवा की दुकान",
                        icon = Icons.Default.LocalPharmacy,
                        accentColor = HighContrastGreen,
                        modifier = Modifier.weight(1f),
                        testTag = "tile_nearby_pharmacy",
                        onClick = { navigationViewModel.findNearbyFacility("pharmacy") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccessibleTile(
                        title = "ATM / Bank",
                        subtitle = "एटीएम",
                        icon = Icons.Default.LocalAtm,
                        accentColor = HighContrastCyan,
                        modifier = Modifier.weight(1f),
                        testTag = "tile_nearby_atm",
                        onClick = { navigationViewModel.findNearbyFacility("atm") }
                    )
                    AccessibleTile(
                        title = "Police",
                        subtitle = "पुलिस थाना",
                        icon = Icons.Default.Security,
                        accentColor = Color.White,
                        modifier = Modifier.weight(1f),
                        testTag = "tile_nearby_police",
                        onClick = { navigationViewModel.findNearbyFacility("police") }
                    )
                }
            }

            // Bottom Spoken Instructions
            if (uiState.spokenInstruction.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AccessibleDarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = uiState.spokenInstruction,
                        color = HighContrastYellow,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
