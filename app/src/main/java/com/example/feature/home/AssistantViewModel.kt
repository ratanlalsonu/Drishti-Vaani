package com.example.feature.home

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.accessibility.HapticFeedbackManager
import com.example.core.battery.BatteryInfo
import com.example.core.battery.BatteryStatusMonitor
import com.example.core.model.AssistantLanguage
import com.example.core.model.PriorityLevel
import com.example.core.model.VoiceCommand
import com.example.core.util.ContactMatcher
import com.example.data.local.DrishtiDatabase
import com.example.data.local.entity.EmergencyContact
import com.example.feature.voice.SpeechRecognizerManager
import com.example.feature.voice.TTSManager
import com.example.feature.voice.VoiceCommandParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssistantUiState(
    val isListening: Boolean = false,
    val isAutoListeningActive: Boolean = true,
    val lastRecognizedText: String = "",
    val partialRecognizedText: String = "",
    val audioLevel: Float = 0f,
    val assistantFeedback: String = "",
    val currentLanguage: AssistantLanguage = AssistantLanguage.HINDI,
    val isVisionActive: Boolean = false,
    val isEmergencyActive: Boolean = false,
    val primaryContact: EmergencyContact? = null,
    val activeScreen: String = "home",
    val batteryInfo: BatteryInfo = BatteryInfo()
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DrishtiDatabase.getDatabase(application)
    val hapticManager = HapticFeedbackManager(application)

    val batteryMonitor = BatteryStatusMonitor(
        context = application,
        onCriticalBatteryWarning = { info, messageHi, messageEn ->
            val speech = if (_uiState.value.currentLanguage == AssistantLanguage.HINDI) messageHi else messageEn
            hapticManager.triggerCriticalHazard()
            _uiState.update {
                it.copy(
                    batteryInfo = info,
                    assistantFeedback = speech
                )
            }
            ttsManager.speak(speech, PriorityLevel.CRITICAL)
        },
        onBatteryStateChanged = { info ->
            _uiState.update { it.copy(batteryInfo = info) }
        }
    )

    var onVisionQueryRequested: (() -> Unit)? = null
    var onVisionRangeChanged: ((com.example.core.model.DetectionRangeLimit) -> Unit)? = null
    var onIdentifyObjectRequested: (() -> Unit)? = null
    var onLeaveVisionScreen: (() -> Unit)? = null
    var onLeaveOcrScreen: (() -> Unit)? = null
    var onOcrRepeatRequested: (() -> Unit)? = null
    var onLocationRequested: (() -> Unit)? = null
    var onLeaveNavigationScreen: (() -> Unit)? = null

    private val navigationBackStack = mutableListOf("home")

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    val ttsManager = TTSManager(application) { initialized ->
        if (initialized) {
            triggerStartupGreeting()
        }
    }

    val speechManager = SpeechRecognizerManager(
        context = application,
        onCommandReceived = { spokenQuery ->
            handleSpokenCommand(spokenQuery)
        },
        onListeningStateChanged = { listening ->
            _uiState.update { it.copy(isListening = listening, audioLevel = if (listening) it.audioLevel else 0f) }
            if (listening) {
                hapticManager.triggerListeningStart()
            }
        },
        onErrorOccurred = { errorMsg ->
            _uiState.update { it.copy(assistantFeedback = errorMsg) }
        },
        onCandidatesReceived = { candidates ->
            handleSpokenCandidates(candidates)
        },
        onPartialResultReceived = { partial ->
            _uiState.update { it.copy(partialRecognizedText = partial) }
        },
        onAudioLevelChanged = { level ->
            _uiState.update { it.copy(audioLevel = level) }
        }
    )

    init {
        ttsManager.onSpeakingStateChanged = { speaking ->
            if (speaking) {
                speechManager.pauseForTTS()
            } else {
                speechManager.resumeAfterTTS()
            }
        }
        observeContacts()
        batteryMonitor.start()
    }

    private fun observeContacts() {
        viewModelScope.launch {
            database.emergencyContactDao().getAllContacts().collect { list ->
                val primary = list.firstOrNull { it.isPrimary } ?: list.firstOrNull()
                _uiState.update { it.copy(primaryContact = primary) }
            }
        }
    }

    private fun triggerStartupGreeting() {
        val greeting = when (_uiState.value.currentLanguage) {
            AssistantLanguage.HINDI -> "नमस्ते. मैं दृष्टि वाणी हूँ, आपका विजुअल साथी. कैमरा शुरू करने के लिए 'कैमरा चालू करो' या 'हेल्प' बोलिये."
            else -> "Namaste. I am Drishti Vaani, your visual companion. Say 'Start Vision' or 'Help'."
        }
        _uiState.update { it.copy(assistantFeedback = greeting) }
        ttsManager.speak(greeting, PriorityLevel.HIGH)
    }

    fun startAutoListening() {
        _uiState.update { it.copy(isAutoListeningActive = true) }
        speechManager.startAutoListening()
    }

    fun stopAutoListening() {
        _uiState.update { it.copy(isAutoListeningActive = false) }
        speechManager.stopAutoListening()
    }

    fun startVoiceInput() {
        ttsManager.stopSpeaking()
        speechManager.startListening()
    }

    fun stopVoiceInput() {
        speechManager.stopListening()
    }

    fun handleSpokenCandidates(candidates: List<String>) {
        val bestText = candidates.firstOrNull() ?: ""
        _uiState.update { it.copy(lastRecognizedText = bestText, partialRecognizedText = "") }
        val command = VoiceCommandParser.parseCandidates(candidates)
        if (command is VoiceCommand.Unknown) {
            hapticManager.triggerNotUnderstood()
        } else {
            hapticManager.triggerCommandSuccess()
        }
        executeCommand(command)
    }

    fun handleSpokenCommand(spokenText: String) {
        handleSpokenCandidates(listOf(spokenText))
    }

    fun executeCommand(command: VoiceCommand) {
        when (command) {
            is VoiceCommand.GoHome -> {
                hapticManager.triggerConfirmation()
                cleanupScreen(_uiState.value.activeScreen)
                ttsManager.stopSpeaking()
                _uiState.update { it.copy(activeScreen = "home", isVisionActive = false, isEmergencyActive = false) }
                navigationBackStack.clear()
                navigationBackStack.add("home")
                speakFeedback("Dashboard par wapas aa gaye hain.", "Returned to main dashboard.")
            }

            is VoiceCommand.CheckBattery -> {
                hapticManager.triggerConfirmation()
                val lang = _uiState.value.currentLanguage
                val batteryMsg = batteryMonitor.getBatteryStatusDescription(lang)
                _uiState.update { it.copy(assistantFeedback = batteryMsg) }
                ttsManager.speak(batteryMsg, PriorityLevel.HIGH)
            }


            is VoiceCommand.CheckTime -> {
                hapticManager.triggerConfirmation()
                val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                val currentTime = timeFormat.format(java.util.Date())
                val timeMsg = "Abhi samay $currentTime hua hai."
                val timeMsgEn = "The current time is $currentTime."
                speakFeedback(timeMsg, timeMsgEn)
            }

            is VoiceCommand.StartVision -> {
                hapticManager.triggerConfirmation()
                if (navigationBackStack.lastOrNull() != "vision") {
                    navigationBackStack.add("vision")
                }
                _uiState.update { it.copy(isVisionActive = true, activeScreen = "vision") }
                speakFeedback("Vision assistance shuru ho rahi hai. Live camera active.", "Vision assistance activated.")
            }

            is VoiceCommand.StopVision -> {
                hapticManager.triggerConfirmation()
                cleanupScreen("vision")
                ttsManager.stopSpeaking()
                _uiState.update { it.copy(isVisionActive = false, activeScreen = "home") }
                navigationBackStack.clear()
                navigationBackStack.add("home")
                speakFeedback("Vision assistance rok di gayi hai.", "Vision assistance stopped.")
            }

            is VoiceCommand.ReadText -> {
                hapticManager.triggerConfirmation()
                if (_uiState.value.activeScreen == "ocr") {
                    // Re-read current text on demand
                    onOcrRepeatRequested?.invoke()
                } else {
                    if (navigationBackStack.lastOrNull() != "ocr") {
                        navigationBackStack.add("ocr")
                    }
                    _uiState.update { it.copy(activeScreen = "ocr") }
                    speakFeedback("Text reading screen active. Camera ko text ke samne rakhein.", "Text reading active. Point camera at text.")
                }
            }

            is VoiceCommand.WhereAmI -> {
                hapticManager.triggerConfirmation()
                if (navigationBackStack.lastOrNull() != "navigation") {
                    navigationBackStack.add("navigation")
                }
                _uiState.update { it.copy(activeScreen = "navigation") }
                onLocationRequested?.invoke()
            }

            is VoiceCommand.FindNearby -> {
                hapticManager.triggerConfirmation()
                if (navigationBackStack.lastOrNull() != "navigation") {
                    navigationBackStack.add("navigation")
                }
                _uiState.update { it.copy(activeScreen = "navigation") }
                onLocationRequested?.invoke()
            }

            is VoiceCommand.Emergency -> {
                hapticManager.triggerCriticalHazard()
                _uiState.update { it.copy(isEmergencyActive = true, activeScreen = "emergency") }
                val contact = _uiState.value.primaryContact
                val response = if (contact != null) {
                    "Emergency mode sakriya hai. ${contact.name} ko call lagane ke liye 'Call ${contact.name}' boliye ya 'Call Emergency' boliye."
                } else {
                    "Emergency mode active. Turant call lagane ke liye 'Call 112' boliye."
                }
                ttsManager.speak(response, PriorityLevel.CRITICAL)
            }

            is VoiceCommand.CallContact -> {
                val queryName = command.targetName.trim()
                viewModelScope.launch {
                    val allContacts = database.emergencyContactDao().getAllContactsList()
                    val matchedContact = ContactMatcher.findBestMatch(allContacts, queryName)

                    if (matchedContact != null) {
                        initiatePhoneCall(matchedContact.phoneNumber, matchedContact.name)
                    } else if (queryName.matches(Regex("^[0-9+]{3,14}$"))) {
                        // Directly dialed phone number e.g. "Call 112" or "Call 9876543210"
                        initiatePhoneCall(queryName, queryName)
                    } else if (queryName.isBlank()) {
                        // User just said "phone lagao" / "call lagao" without specifying a name
                        val primary = allContacts.firstOrNull { it.isPrimary } ?: allContacts.firstOrNull()
                        if (primary != null) {
                            initiatePhoneCall(primary.phoneNumber, primary.name)
                        } else {
                            val msgHi = "Koi emergency contact save nahi hai. Emergency ke liye 'Call 112' bolein ya emergency section me contact save karein."
                            val msgEn = "No emergency contact saved. Say 'Call 112' for emergency services."
                            speakFeedback(msgHi, msgEn)
                        }
                    } else {
                        if (allContacts.isNotEmpty()) {
                            val primary = allContacts.firstOrNull { it.isPrimary } ?: allContacts.firstOrNull()
                            val savedNames = allContacts.joinToString(", ") { it.name }
                            val msgHi = "'$queryName' naam ka contact nahi mila. Aapke paas $savedNames save hain. Call karne ke liye 'Call ${primary?.name ?: ""}' bolein."
                            val msgEn = "Contact '$queryName' not found. Saved contacts: $savedNames. Say 'Call ${primary?.name ?: ""}' to call."
                            speakFeedback(msgHi, msgEn)
                        } else {
                            val msgHi = "'$queryName' naam ka contact save nahi hai. Emergency ke liye 'Call 112' bolein."
                            val msgEn = "No contact found for '$queryName'. For emergency, say 'Call 112'."
                            speakFeedback(msgHi, msgEn)
                        }
                    }
                }
            }

            is VoiceCommand.CallEmergency -> {
                hapticManager.triggerCriticalHazard()
                viewModelScope.launch {
                    val allContacts = database.emergencyContactDao().getAllContactsList()
                    val primary = allContacts.firstOrNull { it.isPrimary } ?: allContacts.firstOrNull()
                    if (primary != null) {
                        initiatePhoneCall(primary.phoneNumber, primary.name)
                    } else {
                        initiatePhoneCall("112", "Emergency Services (112)")
                    }
                }
            }

            is VoiceCommand.RepeatSpeech -> {
                hapticManager.triggerConfirmation()
                if (_uiState.value.activeScreen == "ocr") {
                    onOcrRepeatRequested?.invoke()
                } else if (_uiState.value.activeScreen == "vision") {
                    onVisionQueryRequested?.invoke()
                } else {
                    ttsManager.repeatLast()
                }
            }

            is VoiceCommand.StopSpeech -> {
                ttsManager.stopSpeaking()
            }

            is VoiceCommand.OpenSettings -> {
                hapticManager.triggerConfirmation()
                if (navigationBackStack.lastOrNull() != "settings") {
                    navigationBackStack.add("settings")
                }
                _uiState.update { it.copy(activeScreen = "settings") }
                speakFeedback("Settings khul gayi hai.", "Opening settings.")
            }

            is VoiceCommand.SetVisionRange -> {
                hapticManager.triggerConfirmation()
                onVisionRangeChanged?.invoke(command.limit)
                val hindiMsg = "Vision detection range ${command.limit.labelHi} par set ki gayi."
                val englishMsg = "Vision detection range set to ${command.limit.labelEn}."
                speakFeedback(hindiMsg, englishMsg)
            }

            is VoiceCommand.Help -> {
                hapticManager.triggerConfirmation()
                if (navigationBackStack.lastOrNull() != "help") {
                    navigationBackStack.add("help")
                }
                val helpMsg = "Aap bol sakte hain: 'Start Vision', 'Read Text', 'Where am I', 'Find nearest hospital', 'Emergency', 'Repeat', ya 'Settings'."
                speakFeedback(helpMsg, helpMsg)
            }

            is VoiceCommand.QuerySurroundings -> {
                hapticManager.triggerConfirmation()
                if (_uiState.value.activeScreen != "vision") {
                    if (navigationBackStack.lastOrNull() != "vision") {
                        navigationBackStack.add("vision")
                    }
                    _uiState.update { it.copy(isVisionActive = true, activeScreen = "vision") }
                }
                onVisionQueryRequested?.invoke()
            }

            is VoiceCommand.IdentifyObject -> {
                hapticManager.triggerConfirmation()
                if (_uiState.value.activeScreen != "vision") {
                    if (navigationBackStack.lastOrNull() != "vision") {
                        navigationBackStack.add("vision")
                    }
                    _uiState.update { it.copy(isVisionActive = true, activeScreen = "vision") }
                }
                onIdentifyObjectRequested?.invoke()
            }

            is VoiceCommand.PauseReading -> {
                onLeaveOcrScreen?.invoke()
                speakFeedback("Reading paused.", "Reading paused.")
            }

            is VoiceCommand.ResumeReading -> {
                onOcrRepeatRequested?.invoke()
                speakFeedback("Reading resumed.", "Reading resumed.")
            }

            is VoiceCommand.NavigateTo -> {
                hapticManager.triggerConfirmation()
                speakFeedback("${command.destination} ke liye navigation load ho raha hai.", "Navigating to ${command.destination}.")
            }

            is VoiceCommand.Unknown -> {
                speakFeedback(
                    "Mujhe samajh nahi aaya: '${command.rawQuery}'. Aap bol sakte hain: 'Camera chalu karo', 'Samne kya hai', 'Padho' ya 'Help'.",
                    "Did not understand: '${command.rawQuery}'. Please say 'Start Vision', 'What is ahead', 'Read Text' or 'Help'."
                )
            }
        }
    }

    private fun speakFeedback(hindiText: String, englishText: String) {
        val speech = if (_uiState.value.currentLanguage == AssistantLanguage.HINDI) hindiText else englishText
        _uiState.update { it.copy(assistantFeedback = speech) }
        ttsManager.speak(speech, PriorityLevel.HIGH)
    }

    fun initiatePhoneCall(phoneNumber: String, contactName: String) {
        val app = getApplication<Application>()
        hapticManager.triggerConfirmation()

        val announcement = if (_uiState.value.currentLanguage == AssistantLanguage.HINDI) {
            "$contactName ko turant call lagayi ja rahi hai..."
        } else {
            "Calling $contactName now..."
        }
        ttsManager.speak(announcement, PriorityLevel.CRITICAL)

        try {
            val hasCallPermission = ContextCompat.checkSelfPermission(
                app,
                android.Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            val action = if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val callIntent = Intent(action).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            app.startActivity(callIntent)
        } catch (e: Exception) {
            val errorMsg = "Call nahi lag payi: ${e.localizedMessage}"
            ttsManager.speak(errorMsg, PriorityLevel.HIGH)
        }
    }

    fun setLanguage(lang: AssistantLanguage) {
        _uiState.update { it.copy(currentLanguage = lang) }
        ttsManager.setLanguage(lang)
        speechManager.setLanguage(lang)
        val feedback = if (lang == AssistantLanguage.HINDI) "Bhasha Hindi set ki gayi hai." else "Language set to English."
        ttsManager.speak(feedback, PriorityLevel.HIGH)
    }

    fun cleanupScreen(screenName: String) {
        ttsManager.stopSpeaking()
        when (screenName) {
            "vision" -> onLeaveVisionScreen?.invoke()
            "ocr" -> onLeaveOcrScreen?.invoke()
            "navigation" -> onLeaveNavigationScreen?.invoke()
        }
    }

    fun navigateToScreen(screenName: String, addToBackStack: Boolean = true) {
        val current = _uiState.value.activeScreen
        if (screenName == current) return

        cleanupScreen(current)

        if (addToBackStack && (navigationBackStack.isEmpty() || navigationBackStack.last() != screenName)) {
            navigationBackStack.add(screenName)
        }

        _uiState.update { it.copy(activeScreen = screenName) }
        hapticManager.triggerConfirmation()
        when (screenName) {
            "vision" -> executeCommand(VoiceCommand.StartVision)
            "ocr" -> executeCommand(VoiceCommand.ReadText)
            "navigation" -> executeCommand(VoiceCommand.WhereAmI)
            "emergency" -> executeCommand(VoiceCommand.Emergency)
            "settings" -> executeCommand(VoiceCommand.OpenSettings)
            "help" -> executeCommand(VoiceCommand.Help)
            "home" -> {
                _uiState.update { it.copy(isVisionActive = false, isEmergencyActive = false) }
                speakFeedback("Dashboard par wapas aa gaye hain.", "Returned to main dashboard.")
            }
        }
    }

    /**
     * Handles back button press from Android system / gesture bar:
     * Navigates to the previous screen in the stack rather than closing the entire application.
     * Returns true if back navigation was handled, or false if already at root "home".
     */
    fun navigateBack(): Boolean {
        val currentScreen = _uiState.value.activeScreen
        cleanupScreen(currentScreen)

        if (navigationBackStack.size > 1) {
            navigationBackStack.removeAt(navigationBackStack.size - 1)
            val previousScreen = navigationBackStack.lastOrNull() ?: "home"
            _uiState.update {
                it.copy(
                    activeScreen = previousScreen,
                    isVisionActive = (previousScreen == "vision"),
                    isEmergencyActive = (previousScreen == "emergency")
                )
            }
            hapticManager.triggerConfirmation()
            val feedbackHi = when (previousScreen) {
                "home" -> "Home screen par wapas aa gaye."
                "vision" -> "Vision screen par wapas aa gaye."
                "ocr" -> "Text reading screen par wapas aa gaye."
                "navigation" -> "Navigation screen par wapas aa gaye."
                "emergency" -> "Emergency screen par wapas aa gaye."
                "settings" -> "Settings screen par wapas aa gaye."
                else -> "Pichle screen par wapas aa gaye."
            }
            val feedbackEn = when (previousScreen) {
                "home" -> "Returned to home screen."
                "vision" -> "Returned to vision screen."
                "ocr" -> "Returned to text reading screen."
                "navigation" -> "Returned to navigation screen."
                "emergency" -> "Returned to emergency screen."
                "settings" -> "Returned to settings screen."
                else -> "Returned to previous screen."
            }
            speakFeedback(feedbackHi, feedbackEn)
            return true
        } else if (currentScreen != "home") {
            _uiState.update {
                it.copy(
                    activeScreen = "home",
                    isVisionActive = false,
                    isEmergencyActive = false
                )
            }
            navigationBackStack.clear()
            navigationBackStack.add("home")
            hapticManager.triggerConfirmation()
            speakFeedback("Home screen par wapas aa gaye.", "Returned to home screen.")
            return true
        }
        return false
    }

    fun checkBatteryStatus() {
        executeCommand(VoiceCommand.CheckBattery)
    }

    override fun onCleared() {
        super.onCleared()
        batteryMonitor.stop()
        speechManager.destroy()
        ttsManager.destroy()
    }
}
