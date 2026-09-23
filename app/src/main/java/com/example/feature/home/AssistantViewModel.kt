package com.example.feature.home

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.accessibility.HapticFeedbackManager
import com.example.core.model.AssistantLanguage
import com.example.core.model.PriorityLevel
import com.example.core.model.VoiceCommand
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
    val activeScreen: String = "home"
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DrishtiDatabase.getDatabase(application)
    val hapticManager = HapticFeedbackManager(application)

    var onVisionQueryRequested: (() -> Unit)? = null
    var onVisionRangeChanged: ((com.example.core.model.DetectionRangeLimit) -> Unit)? = null
    var onIdentifyObjectRequested: (() -> Unit)? = null

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
        loadPrimaryContact()
    }

    private fun loadPrimaryContact() {
        viewModelScope.launch {
            val contact = database.emergencyContactDao().getPrimaryContact()
            _uiState.update { it.copy(primaryContact = contact) }
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
                _uiState.update { it.copy(activeScreen = "home", isVisionActive = false) }
                speakFeedback("Dashboard par wapas aa gaye hain. Aap kya karna chahte hain?", "Returned to main dashboard. What would you like to do?")
            }

            is VoiceCommand.CheckBattery -> {
                hapticManager.triggerConfirmation()
                val app = getApplication<Application>()
                val batteryManager = app.getSystemService(android.content.Context.BATTERY_SERVICE) as? android.os.BatteryManager
                val batteryLevel = batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                val batteryMsg = if (batteryLevel >= 0) {
                    "Phone ki battery $batteryLevel percent hai."
                } else {
                    "Battery level prapt nahi ho saka."
                }
                val batteryMsgEn = if (batteryLevel >= 0) "Phone battery is at $batteryLevel percent." else "Could not read battery level."
                speakFeedback(batteryMsg, batteryMsgEn)
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
                _uiState.update { it.copy(isVisionActive = true, activeScreen = "vision") }
                speakFeedback("Vision assistance shuru ho rahi hai. Live camera active.", "Vision assistance activated.")
            }

            is VoiceCommand.StopVision -> {
                hapticManager.triggerConfirmation()
                _uiState.update { it.copy(isVisionActive = false, activeScreen = "home") }
                speakFeedback("Vision assistance rok di gayi hai.", "Vision assistance stopped.")
            }

            is VoiceCommand.ReadText -> {
                hapticManager.triggerConfirmation()
                _uiState.update { it.copy(activeScreen = "ocr") }
                speakFeedback("Text reading screen active. Camera ko text ke samne rakhein.", "Text reading active. Point camera at text.")
            }

            is VoiceCommand.WhereAmI -> {
                hapticManager.triggerConfirmation()
                _uiState.update { it.copy(activeScreen = "navigation") }
                speakFeedback("Aapki location check ki ja rahi hai.", "Checking your current location.")
            }

            is VoiceCommand.FindNearby -> {
                hapticManager.triggerConfirmation()
                _uiState.update { it.copy(activeScreen = "navigation") }
                speakFeedback("Paas ke ${command.placeType} ki khoj ki ja rahi hai.", "Finding nearest ${command.placeType}.")
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
                    val matchedContact = database.emergencyContactDao().findContactByName(queryName)
                    if (matchedContact != null) {
                        initiatePhoneCall(matchedContact.phoneNumber, matchedContact.name)
                    } else if (queryName.matches(Regex("^[0-9+]{3,14}$"))) {
                        // Directly dialed phone number e.g. "Call 112" or "Call 9876543210"
                        initiatePhoneCall(queryName, queryName)
                    } else {
                        val all = database.emergencyContactDao().getAllContactsList()
                        val primary = all.firstOrNull { it.isPrimary } ?: all.firstOrNull()
                        if (primary != null) {
                            val msgHi = "'$queryName' naam ka contact nahi mila. Kya aap ${primary.name} ko call karna chahte hain? 'Call ${primary.name}' boliye."
                            val msgEn = "Contact '$queryName' not found. Say 'Call ${primary.name}' to call primary contact."
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
                    val primary = database.emergencyContactDao().getPrimaryContact()
                    if (primary != null) {
                        initiatePhoneCall(primary.phoneNumber, primary.name)
                    } else {
                        initiatePhoneCall("112", "Emergency Services (112)")
                    }
                }
            }

            is VoiceCommand.RepeatSpeech -> {
                hapticManager.triggerConfirmation()
                ttsManager.repeatLast()
            }

            is VoiceCommand.StopSpeech -> {
                ttsManager.stopSpeaking()
            }

            is VoiceCommand.OpenSettings -> {
                hapticManager.triggerConfirmation()
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
                val helpMsg = "Aap bol sakte hain: 'Start Vision', 'Read Text', 'Where am I', 'Find nearest hospital', 'Emergency', 'Repeat', ya 'Settings'."
                speakFeedback(helpMsg, helpMsg)
            }

            is VoiceCommand.QuerySurroundings -> {
                hapticManager.triggerConfirmation()
                onVisionQueryRequested?.invoke()
                speakFeedback("Surroundings scan kiye ja rahe hain. Camera samne rakhein.", "Scanning surroundings. Keep camera steady.")
            }

            is VoiceCommand.IdentifyObject -> {
                hapticManager.triggerConfirmation()
                if (_uiState.value.activeScreen != "vision") {
                    _uiState.update { it.copy(isVisionActive = true, activeScreen = "vision") }
                }
                onIdentifyObjectRequested?.invoke()
            }

            is VoiceCommand.PauseReading -> {
                speakFeedback("Reading paused.", "Reading paused.")
            }

            is VoiceCommand.ResumeReading -> {
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

    fun navigateToScreen(screenName: String) {
        _uiState.update { it.copy(activeScreen = screenName) }
        hapticManager.triggerConfirmation()
        when (screenName) {
            "vision" -> executeCommand(VoiceCommand.StartVision)
            "ocr" -> executeCommand(VoiceCommand.ReadText)
            "navigation" -> executeCommand(VoiceCommand.WhereAmI)
            "emergency" -> executeCommand(VoiceCommand.Emergency)
            "settings" -> executeCommand(VoiceCommand.OpenSettings)
            "home" -> {
                _uiState.update { it.copy(isVisionActive = false, isEmergencyActive = false) }
                speakFeedback("Home screen.", "Home screen.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        ttsManager.destroy()
    }
}
