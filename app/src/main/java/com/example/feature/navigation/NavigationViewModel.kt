package com.example.feature.navigation

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.accessibility.HapticFeedbackManager
import com.example.core.model.AssistantLanguage
import com.example.core.model.PriorityLevel
import com.example.feature.vision.DirectionOrientationTracker
import com.example.feature.voice.TTSManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val currentAddress: String = "GPS से लोकेशन खोजी जा रही है...",
    val currentDirectionHi: String = "जांच की जा रही है...",
    val currentDirectionEn: String = "Determining heading...",
    val currentAzimuth: Float = 0f,
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

    // Track first-time open announcements: exactly 2 times
    private var hasInitialAnnouncementsCompleted = false
    private var initialRepeatJob: Job? = null

    // Compass and Direction Tracker
    private var directionTracker: DirectionOrientationTracker? = null
    private var currentDirectionHi = ""
    private var currentDirectionEn = ""
    private var currentAzimuth = 0f

    init {
        try {
            directionTracker = DirectionOrientationTracker(
                context = application,
                onDirectionMoved = {},
                onDirectionSettled = { azimuth, _, dirHi, dirEn ->
                    currentAzimuth = azimuth
                    currentDirectionHi = dirHi
                    currentDirectionEn = dirEn
                    _uiState.update {
                        it.copy(
                            currentAzimuth = azimuth,
                            currentDirectionHi = dirHi,
                            currentDirectionEn = dirEn
                        )
                    }
                },
                onAzimuthChanged = { azimuth, dirHi, dirEn ->
                    currentAzimuth = azimuth
                    currentDirectionHi = dirHi
                    currentDirectionEn = dirEn
                    _uiState.update {
                        it.copy(
                            currentAzimuth = azimuth,
                            currentDirectionHi = dirHi,
                            currentDirectionEn = dirEn
                        )
                    }
                }
            )
            directionTracker?.start()
        } catch (_: Exception) {}
    }

    fun attachTTS(tts: TTSManager, language: AssistantLanguage) {
        this.ttsManager = tts
        this.currentLanguage = language
    }

    /**
     * Called when the Navigation screen is opened.
     * Behavior:
     * - First time open: Speaks the user's location & facing direction exactly 2 times.
     * - Subsequent opens: Updates silently; user speaks "दिशा" or "नेविगेशन" to hear on-demand.
     */
    fun onNavigationScreenOpened() {
        directionTracker?.start()
        if (!hasInitialAnnouncementsCompleted) {
            requestLocationWithInitialRepetition()
        } else {
            silentLocationUpdate()
        }
    }

    fun onNavigationScreenClosed() {
        initialRepeatJob?.cancel()
        initialRepeatJob = null
        directionTracker?.stop()
    }

    private fun getFacingDirectionSpeech(isHindi: Boolean): String {
        return if (isHindi) {
            if (currentDirectionHi.isNotBlank() && !currentDirectionHi.contains("जांच")) {
                "आपका मुख $currentDirectionHi की ओर है।"
            } else {
                val (hi, _) = DirectionOrientationTracker.getDirectionNames(currentAzimuth)
                "आपका मुख $hi की ओर है।"
            }
        } else {
            if (currentDirectionEn.isNotBlank() && !currentDirectionEn.contains("Determining")) {
                "You are facing $currentDirectionEn."
            } else {
                val (_, en) = DirectionOrientationTracker.getDirectionNames(currentAzimuth)
                "You are facing $en."
            }
        }
    }

    /**
     * Automatically called ONLY on the first-time navigation open:
     * Announces location & facing direction exactly 2 times, then permanently stops automatic speech.
     */
    @SuppressLint("MissingPermission")
    private fun requestLocationWithInitialRepetition() {
        _uiState.update { it.copy(isLocating = true, currentAddress = "GPS से लोकेशन खोजी जा रही है...") }

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
                    viewModelScope.launch(Dispatchers.IO) {
                        val address = resolveAddress(lat, lng)
                        withContext(Dispatchers.Main) {
                            _uiState.update { it.copy(currentAddress = address) }
                            hapticManager.triggerConfirmation()

                            val isHindi = currentLanguage == AssistantLanguage.HINDI
                            val dirSpeech = getFacingDirectionSpeech(isHindi)

                            // 1st Announcement
                            val announcement1 = if (isHindi) {
                                "आपकी वर्तमान स्थिति है: $address। $dirSpeech"
                            } else {
                                "Your current location is: $address. $dirSpeech"
                            }
                            _uiState.update { it.copy(spokenInstruction = announcement1) }
                            ttsManager?.speak(announcement1, PriorityLevel.HIGH)

                            // 2nd Announcement (Repetition for clarity after 3.8s)
                            initialRepeatJob?.cancel()
                            initialRepeatJob = viewModelScope.launch {
                                delay(3800L)
                                val announcement2 = if (isHindi) {
                                    "दोहराया जा रहा है: $address। $dirSpeech दोबारा जानने के लिए 'नेविगेशन' या 'दिशा' बोलें।"
                                } else {
                                    "Repeating: $address. $dirSpeech To hear again, say 'Navigation' or 'Direction'."
                                }
                                _uiState.update { it.copy(spokenInstruction = announcement2) }
                                ttsManager?.speak(announcement2, PriorityLevel.HIGH)
                                hasInitialAnnouncementsCompleted = true
                            }
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            currentAddress = "GPS सिग्नल नहीं मिला। कृपया फ़ोन का GPS ऑन रखें।"
                        )
                    }
                    val isHindi = currentLanguage == AssistantLanguage.HINDI
                    val dirSpeech = getFacingDirectionSpeech(isHindi)
                    val msg = if (isHindi) {
                        "GPS लोकेशन प्राप्त नहीं हो सकी। $dirSpeech"
                    } else {
                        "GPS location unavailable. $dirSpeech"
                    }
                    ttsManager?.speak(msg, PriorityLevel.HIGH)
                }
            }
            .addOnFailureListener { error ->
                _uiState.update {
                    it.copy(
                        isLocating = false,
                        currentAddress = "GPS त्रुटि: ${error.localizedMessage}"
                    )
                }
            }
    }

    /**
     * Called whenever user speaks ("navigation", "disha", "location", "kahan hoon", etc.)
     * or taps the Location/Compass button.
     * Tells the current location and facing direction on-demand.
     */
    @SuppressLint("MissingPermission")
    fun requestLocationSpokenOnDemand() {
        hapticManager.triggerConfirmation()

        val currentLat = _uiState.value.latitude
        val currentLng = _uiState.value.longitude
        val currentAddr = _uiState.value.currentAddress
        val isHindi = currentLanguage == AssistantLanguage.HINDI
        val dirSpeech = getFacingDirectionSpeech(isHindi)

        // If we already have a resolved address, announce it immediately to avoid latency
        if (currentLat != null && currentLng != null &&
            !currentAddr.startsWith("GPS से") && !currentAddr.startsWith("Acquiring")) {
            val announcement = if (isHindi) {
                "आपकी वर्तमान स्थिति है: $currentAddr। $dirSpeech"
            } else {
                "Your current location is: $currentAddr. $dirSpeech"
            }
            _uiState.update { it.copy(spokenInstruction = announcement) }
            ttsManager?.speak(announcement, PriorityLevel.HIGH)
        } else {
            val waitMsg = if (isHindi) {
                "GPS लोकेशन जांची जा रही है... $dirSpeech"
            } else {
                "Checking GPS location... $dirSpeech"
            }
            _uiState.update { it.copy(spokenInstruction = waitMsg) }
            ttsManager?.speak(waitMsg, PriorityLevel.HIGH)
        }

        // Fetch fresh coordinates in background
        _uiState.update { it.copy(isLocating = true) }
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                _uiState.update { it.copy(isLocating = false) }
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    _uiState.update { it.copy(latitude = lat, longitude = lng) }
                    viewModelScope.launch(Dispatchers.IO) {
                        val freshAddress = resolveAddress(lat, lng)
                        withContext(Dispatchers.Main) {
                            val hadNoPrior = currentLat == null || currentLng == null ||
                                    currentAddr.startsWith("GPS से") || currentAddr.startsWith("Acquiring")
                            _uiState.update { it.copy(currentAddress = freshAddress) }
                            if (hadNoPrior) {
                                val freshDirSpeech = getFacingDirectionSpeech(isHindi)
                                val announcement = if (isHindi) {
                                    "आपकी वर्तमान स्थिति है: $freshAddress। $freshDirSpeech"
                                } else {
                                    "Your current location is: $freshAddress. $freshDirSpeech"
                                }
                                _uiState.update { it.copy(spokenInstruction = announcement) }
                                ttsManager?.speak(announcement, PriorityLevel.HIGH)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
                _uiState.update { it.copy(isLocating = false) }
            }
    }

    @SuppressLint("MissingPermission")
    private fun silentLocationUpdate() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    _uiState.update { it.copy(latitude = lat, longitude = lng) }
                    viewModelScope.launch(Dispatchers.IO) {
                        val address = resolveAddress(lat, lng)
                        withContext(Dispatchers.Main) {
                            _uiState.update { it.copy(currentAddress = address) }
                        }
                    }
                }
            }
    }

    private fun resolveAddress(lat: Double, lng: Double): String {
        return try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            addresses?.firstOrNull()?.getAddressLine(0) ?: "Lat: ${"%.4f".format(lat)}, Lng: ${"%.4f".format(lng)}"
        } catch (_: Exception) {
            "Lat: ${"%.4f".format(lat)}, Lng: ${"%.4f".format(lng)}"
        }
    }

    override fun onCleared() {
        super.onCleared()
        initialRepeatJob?.cancel()
        try { directionTracker?.stop() } catch (_: Exception) {}
    }
}
