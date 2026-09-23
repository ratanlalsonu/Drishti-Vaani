package com.example.core.model

sealed class VoiceCommand {
    data object GoHome : VoiceCommand()
    data object StartVision : VoiceCommand()
    data object StopVision : VoiceCommand()
    data object QuerySurroundings : VoiceCommand()
    data object IdentifyObject : VoiceCommand()
    data object ReadText : VoiceCommand()
    data object PauseReading : VoiceCommand()
    data object ResumeReading : VoiceCommand()
    data object WhereAmI : VoiceCommand()
    data class FindNearby(val placeType: String) : VoiceCommand()
    data class NavigateTo(val destination: String) : VoiceCommand()
    data object Emergency : VoiceCommand()
    data class CallContact(val targetName: String) : VoiceCommand()
    data object CallEmergency : VoiceCommand()
    data object RepeatSpeech : VoiceCommand()
    data object StopSpeech : VoiceCommand()
    data object OpenSettings : VoiceCommand()
    data object Help : VoiceCommand()
    data object CheckBattery : VoiceCommand()
    data object CheckTime : VoiceCommand()
    data class SetVisionRange(val limit: DetectionRangeLimit) : VoiceCommand()
    data class Unknown(val rawQuery: String) : VoiceCommand()
}


enum class AssistantLanguage(val code: String, val displayName: String) {
    HINDI("hi-IN", "हिंदी (Hindi)"),
    ENGLISH_IN("en-IN", "English (India)"),
    ENGLISH_US("en-US", "English (US)")
}
