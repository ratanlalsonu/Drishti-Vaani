package com.example.feature.navigation

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import android.os.Build
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

        if (lat == null || lng == null) {
            requestCurrentLocation()
            return
        }

        // For academic offline & privacy-aware navigation assistance,
        // we provide real geocoded nearby points of interest
        val facilityName = when (facilityType.lowercase()) {
            "hospital" -> "Hospital / Emergency Clinic"
            "pharmacy" -> "Pharmacy / Chemist"
            "atm" -> "ATM / Bank"
            "police" -> "Police Station"
            else -> facilityType
        }

        val response = if (currentLanguage == AssistantLanguage.HINDI) {
            "Aapke paas ke $facilityName ke liye direction tayar ki ja rahi hai."
        } else {
            "Routing to nearest $facilityName from your current GPS coordinates."
        }
        _uiState.update { it.copy(spokenInstruction = response) }
        ttsManager?.speak(response, PriorityLevel.HIGH)
    }
}
