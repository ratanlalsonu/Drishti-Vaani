package com.example.feature.emergency

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.accessibility.HapticFeedbackManager
import com.example.core.model.AssistantLanguage
import com.example.core.model.PriorityLevel
import com.example.data.local.DrishtiDatabase
import com.example.data.local.entity.EmergencyContact
import com.example.feature.voice.TTSManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmergencyUiState(
    val contacts: List<EmergencyContact> = emptyList(),
    val primaryContact: EmergencyContact? = null,
    val isConfirmingCall: Boolean = false,
    val statusMessage: String = "Emergency Mode Active"
)

class EmergencyViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DrishtiDatabase.getDatabase(application)
    private val hapticManager = HapticFeedbackManager(application)
    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    private var ttsManager: TTSManager? = null
    private var currentLanguage = AssistantLanguage.HINDI

    init {
        loadContacts()
    }

    fun attachTTS(tts: TTSManager, language: AssistantLanguage) {
        this.ttsManager = tts
        this.currentLanguage = language
    }

    private fun loadContacts() {
        viewModelScope.launch {
            database.emergencyContactDao().getAllContacts().collect { list ->
                val primary = list.firstOrNull { it.isPrimary } ?: list.firstOrNull()
                _uiState.update { it.copy(contacts = list, primaryContact = primary) }
            }
        }
    }

    fun saveContact(name: String, phone: String, isPrimary: Boolean = false) {
        if (name.isBlank() || phone.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            database.emergencyContactDao().insertContact(
                EmergencyContact(
                    name = name,
                    phoneNumber = phone,
                    isPrimary = isPrimary
                )
            )
        }
    }

    fun triggerEmergencySOS() {
        hapticManager.triggerCriticalHazard()
        val contact = _uiState.value.primaryContact
        if (contact != null) {
            _uiState.update { it.copy(isConfirmingCall = true) }
            val prompt = if (currentLanguage == AssistantLanguage.HINDI) {
                "Emergency! Kya aap ${contact.name} ko call lagana chahte hain? Confirm karne ke liye 'Call' button dabayein ya 'Yes' boliye."
            } else {
                "Emergency! Do you want to call ${contact.name}? Confirm by pressing Call button or saying 'Yes'."
            }
            ttsManager?.speak(prompt, PriorityLevel.CRITICAL)
        } else {
            val alert = if (currentLanguage == AssistantLanguage.HINDI) {
                "Koi emergency contact save nahi hai. Kripya niche emergency contact jodein."
            } else {
                "No emergency contact saved. Please add an emergency contact below."
            }
            ttsManager?.speak(alert, PriorityLevel.CRITICAL)
        }
    }

    fun makeEmergencyCall(specificContact: EmergencyContact? = null) {
        val contact = specificContact ?: _uiState.value.primaryContact ?: return
        hapticManager.triggerConfirmation()
        val announcement = if (currentLanguage == AssistantLanguage.HINDI) {
            "${contact.name} ko turant call lagayi ja rahi hai."
        } else {
            "Calling ${contact.name} now."
        }
        ttsManager?.speak(announcement, PriorityLevel.CRITICAL)

        try {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CALL_PHONE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val action = if (hasPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(action).apply {
                data = Uri.parse("tel:${contact.phoneNumber}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            ttsManager?.speak("Could not place phone call: ${e.localizedMessage}", PriorityLevel.HIGH)
        }
    }

    fun callContactByName(name: String) {
        viewModelScope.launch {
            val contact = database.emergencyContactDao().findContactByName(name)
            if (contact != null) {
                makeEmergencyCall(contact)
            } else {
                val primary = database.emergencyContactDao().getPrimaryContact()
                if (primary != null) {
                    val msg = if (currentLanguage == AssistantLanguage.HINDI) {
                        "'$name' nahi mila. Primary contact ${primary.name} ko call lagane ke liye 'Call ${primary.name}' boliye."
                    } else {
                        "Contact '$name' not found. Say 'Call ${primary.name}' to call primary contact."
                    }
                    ttsManager?.speak(msg, PriorityLevel.HIGH)
                }
            }
        }
    }
}
