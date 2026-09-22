package com.example.feature.navigation

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.accessibility.HapticFeedbackManager
import com.example.core.model.AssistantLanguage
import com.example.core.model.PriorityLevel
import com.example.feature.voice.TTSManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class NavigationUiState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val currentAddress: String = "Locating via device GPS...",
    val nearbyResults: List<String> = emptyList(),
    val isLocating: Boolean = false,
    val isNavigating: Boolean = false,
    val activeDestination: String = "",
    val activeDestinationHi: String = "",
    val activeQuery: String = "",
    val spokenInstruction: String = ""
)

class NavigationViewModel(application: Application) : AndroidViewModel(application) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)
    private val geocoder = Geocoder(application, Locale.getDefault())
    private val hapticManager = HapticFeedbackManager(application)

    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    private var ttsManager: TTSManager? = null
    private var currentLanguage = AssistantLanguage.HINDI

    private var pendingFacilityType: String? = null

    fun attachTTS(tts: TTSManager, language: AssistantLanguage) {
        this.ttsManager = tts
        this.currentLanguage = language
    }

    @SuppressLint("MissingPermission")
    fun requestCurrentLocation() {
        _uiState.update { it.copy(isLocating = true, currentAddress = "Acquiring real GPS coordinates...") }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    _uiState.update {
                        it.copy(
                            latitude = lat,
                            longitude = lng,
                            isLocating = false
                        )
                    }
                    reverseGeocode(lat, lng)

                    // If user requested a facility while GPS was acquiring, route immediately
                    val pending = pendingFacilityType
                    if (pending != null) {
                        pendingFacilityType = null
                        findNearbyFacility(pending)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            currentAddress = "Could not retrieve GPS fix. Ensure device location is turned on."
                        )
                    }
                    val msg = if (currentLanguage == AssistantLanguage.HINDI) {
                        "Aapki location prapt nahi ho saki. Kripya phone ka GPS on karein."
                    } else {
                        "Could not determine current location. Please verify GPS is enabled."
                    }
                    ttsManager?.speak(msg, PriorityLevel.HIGH)
                }
            }
            .addOnFailureListener { error ->
                _uiState.update {
                    it.copy(
                        isLocating = false,
                        currentAddress = "GPS error: ${error.localizedMessage}"
                    )
                }
            }
    }

    private fun reverseGeocode(lat: Double, lng: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val address = addresses?.firstOrNull()?.getAddressLine(0) ?: "Lat: $lat, Lng: $lng"
                handleAddressResult(address)
            } catch (e: Exception) {
                handleAddressResult("Location: $lat, $lng")
            }
        }
    }

    private suspend fun handleAddressResult(address: String) {
        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(currentAddress = address) }
            hapticManager.triggerConfirmation()

            val announcement = if (currentLanguage == AssistantLanguage.HINDI) {
                "Aapki vartaman sthiti hai: $address"
            } else {
                "Your current location is: $address"
            }
            _uiState.update { it.copy(spokenInstruction = announcement) }
            ttsManager?.speak(announcement, PriorityLevel.HIGH)
        }
    }

    fun findNearbyFacility(facilityType: String) {
        val lat = _uiState.value.latitude
        val lng = _uiState.value.longitude

        val (query, nameHi, nameEn) = when (facilityType.lowercase().trim()) {
            "police", "thana", "police station" -> Triple("Police Station", "पुलिस थाना", "Police Station")
            "hospital", "clinic" -> Triple("Hospital Clinic", "अस्पताल", "Hospital")
            "pharmacy", "chemist", "dawa" -> Triple("Pharmacy Chemist", "दवा की दुकान", "Pharmacy")
            "atm", "bank" -> Triple("ATM Bank", "एटीएम बैंक", "ATM")
            "bus stop", "railway", "station", "transit_station" -> Triple("Bus Stop", "बस स्टॉप", "Bus Stop")
            "home", "ghar" -> Triple("Home", "घर", "Home")
            else -> Triple(facilityType, facilityType, facilityType)
        }

        _uiState.update {
            it.copy(
                isNavigating = true,
                activeDestination = nameEn,
                activeDestinationHi = nameHi,
                activeQuery = query
            )
        }

        hapticManager.triggerConfirmation()

        if (lat == null || lng == null) {
            pendingFacilityType = facilityType
            val waitingMsg = if (currentLanguage == AssistantLanguage.HINDI) {
                "GPS लोकेशन प्राप्त की जा रही है, फिर नज़दीकी $nameHi का रास्ता शुरू होगा..."
            } else {
                "Acquiring GPS location, then routing to nearest $nameEn..."
            }
            _uiState.update { it.copy(spokenInstruction = waitingMsg) }
            ttsManager?.speak(waitingMsg, PriorityLevel.HIGH)
            requestCurrentLocation()
            return
        }

        val spokenMsg = if (currentLanguage == AssistantLanguage.HINDI) {
            "नज़दीकी $nameHi के लिए वॉकिंग नेविगेशन शुरू किया जा रहा है। Google Maps में आवाज़ द्वारा मुड़ने के निर्देश मिलेंगे।"
        } else {
            "Starting walking navigation to nearest $nameEn. Google Maps will provide turn-by-turn spoken guidance."
        }
        _uiState.update { it.copy(spokenInstruction = spokenMsg) }
        ttsManager?.speak(spokenMsg, PriorityLevel.CRITICAL)

        launchNavigationIntent(query)
    }

    fun launchNavigationIntent(query: String? = null) {
        val targetQuery = query ?: _uiState.value.activeQuery
        if (targetQuery.isBlank()) return

        val lat = _uiState.value.latitude
        val lng = _uiState.value.longitude
        val context = getApplication<Application>().applicationContext

        hapticManager.triggerConfirmation()

        try {
            // Method 1: Google Maps Walking Navigation (ideal for visually impaired pedestrian)
            val gmmUri = Uri.parse("google.navigation:q=${Uri.encode(targetQuery)}&mode=w")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmUri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                return
            }

            // Method 2: Standard Geo URI with query
            val geoUri = if (lat != null && lng != null) {
                Uri.parse("geo:$lat,$lng?q=${Uri.encode(targetQuery)}")
            } else {
                Uri.parse("geo:0,0?q=${Uri.encode(targetQuery)}")
            }
            val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (geoIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(geoIntent)
                return
            }

            // Method 3: Browser Maps fallback
            val webUri = if (lat != null && lng != null) {
                Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$lat,$lng&destination=${Uri.encode(targetQuery)}&travelmode=walking")
            } else {
                Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(targetQuery)}")
            }
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        } catch (e: Exception) {
            Log.e("NavigationVM", "Could not launch navigation: ${e.message}")
            val errorMsg = if (currentLanguage == AssistantLanguage.HINDI) {
                "Maps app open nahi ho saka: ${e.localizedMessage}"
            } else {
                "Could not open maps application: ${e.localizedMessage}"
            }
            ttsManager?.speak(errorMsg, PriorityLevel.HIGH)
        }
    }

    fun stopNavigation() {
        _uiState.update {
            it.copy(
                isNavigating = false,
                activeDestination = "",
                activeDestinationHi = "",
                activeQuery = ""
            )
        }
        hapticManager.triggerConfirmation()
        val msg = if (currentLanguage == AssistantLanguage.HINDI) {
            "नेविगेशन समाप्त किया गया।"
        } else {
            "Navigation stopped."
        }
        _uiState.update { it.copy(spokenInstruction = msg) }
        ttsManager?.speak(msg, PriorityLevel.HIGH)
    }
}
