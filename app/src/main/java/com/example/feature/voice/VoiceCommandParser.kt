package com.example.feature.voice

import com.example.core.model.VoiceCommand

object VoiceCommandParser {
    fun parse(rawQuery: String): VoiceCommand {
        val normalized = rawQuery.trim().lowercase()

        return when {
            // Universal Dashboard / Home navigation commands
            normalized.contains("dashboard") ||
            normalized.contains("home par") ||
            normalized.contains("home screen") ||
            normalized.contains("home le chalo") ||
            normalized.contains("main screen") ||
            normalized.contains("wapas chalo") ||
            normalized.contains("back to home") ||
            normalized.contains("go home") ||
            normalized.contains("go back") ||
            normalized.contains("shuruat par") ||
            normalized == "home" ||
            normalized == "back" -> VoiceCommand.GoHome

            // Direct jumping to Vision from ANY screen
            normalized.contains("vision par le chalo") ||
            normalized.contains("vision par jao") ||
            normalized.contains("vision screen") ||
            normalized.contains("camera par le chalo") ||
            normalized.contains("camera kholo") ||
            normalized.contains("start vision") ||
            normalized.contains("vision start") ||
            normalized.contains("camera chalu") ||
            normalized.contains("shuru karo") ||
            normalized.contains("start camera") ||
            normalized.contains("camera on") -> VoiceCommand.StartVision

            // Stop Vision intents
            normalized.contains("stop vision") ||
            normalized.contains("vision band") ||
            normalized.contains("camera band") ||
            normalized.contains("rok do") ||
            normalized.contains("band karo") ||
            normalized.contains("close camera") -> VoiceCommand.StopVision

            // Query Surroundings intents
            normalized.contains("what is in front") ||
            normalized.contains("what is ahead") ||
            normalized.contains("samne kya hai") ||
            normalized.contains("aas paas kya hai") ||
            normalized.contains("kya dikh raha hai") ||
            normalized.contains("what is around me") ||
            normalized.contains("describe this") -> VoiceCommand.QuerySurroundings

            // Direct jumping to Text Reading / OCR from ANY screen
            normalized.contains("text reader par") ||
            normalized.contains("ocr par le chalo") ||
            normalized.contains("kitab padho") ||
            normalized.contains("reading screen") ||
            normalized.contains("read this") ||
            normalized.contains("read text") ||
            normalized.contains("isko padho") ||
            normalized.contains("ye kya likha hai") ||
            normalized.contains("padh ke sunao") ||
            normalized.contains("kya likha hai") ||
            normalized.contains("scan document") -> VoiceCommand.ReadText

            // OCR controls
            normalized.contains("pause") ||
            normalized.contains("ruko") ||
            normalized.contains("ruk jao") -> VoiceCommand.PauseReading

            normalized.contains("continue") ||
            normalized.contains("resume") ||
            normalized.contains("aage padho") ||
            normalized.contains("padhna jari rakho") -> VoiceCommand.ResumeReading

            // Direct jumping to Location / GPS Navigation from ANY screen
            normalized.contains("navigation par le chalo") ||
            normalized.contains("navigation screen") ||
            normalized.contains("gps kholo") ||
            normalized.contains("rasta dikhao") ||
            normalized.contains("where am i") ||
            normalized.contains("main kahan hoon") ||
            normalized.contains("current location") ||
            normalized.contains("meri jagah") -> VoiceCommand.WhereAmI

            // Find Nearby Place intents
            normalized.contains("hospital") -> VoiceCommand.FindNearby("hospital")
            normalized.contains("pharmacy") || normalized.contains("chemist") || normalized.contains("dawa") -> VoiceCommand.FindNearby("pharmacy")
            normalized.contains("atm") || normalized.contains("bank") -> VoiceCommand.FindNearby("atm")
            normalized.contains("police") || normalized.contains("thana") -> VoiceCommand.FindNearby("police")
            normalized.contains("bus stop") || normalized.contains("railway") || normalized.contains("station") -> VoiceCommand.FindNearby("transit_station")

            // Direct jumping to Emergency SOS from ANY screen
            normalized.contains("emergency") ||
            normalized.contains("help me") ||
            normalized.contains("bachao") ||
            normalized.contains("sos") -> VoiceCommand.Emergency

            // Navigation
            normalized.contains("take me home") || normalized.contains("ghar le chalo") -> VoiceCommand.NavigateTo("Home")
            normalized.startsWith("navigate to") -> {
                val dest = normalized.removePrefix("navigate to").trim()
                VoiceCommand.NavigateTo(dest)
            }
            normalized.contains("le chalo") && !normalized.contains("vision") && !normalized.contains("home") && !normalized.contains("camera") && !normalized.contains("ocr") && !normalized.contains("reader") && !normalized.contains("emergency") && !normalized.contains("dashboard") -> {
                val dest = normalized.replace("mujhe", "").replace("le chalo", "").trim()
                VoiceCommand.NavigateTo(dest)
            }


            // Battery & Time status check
            normalized.contains("battery") || normalized.contains("charge") -> VoiceCommand.CheckBattery
            normalized.contains("time") || normalized.contains("samay") || normalized.contains("kitne baje") -> VoiceCommand.CheckTime

            // Speech playback controls
            normalized.contains("repeat") ||
            normalized.contains("phir se bolo") ||
            normalized.contains("dobara bolo") -> VoiceCommand.RepeatSpeech

            normalized.contains("stop speaking") ||
            normalized.contains("chup ho jao") ||
            normalized.contains("shant raho") ||
            normalized.contains("chup") -> VoiceCommand.StopSpeech

            // Settings
            normalized.contains("settings par le chalo") ||
            normalized.contains("settings kholo") ||
            normalized.contains("settings") -> VoiceCommand.OpenSettings

            // Help
            normalized.contains("help") ||
            normalized.contains("madad") ||
            normalized.contains("commands") -> VoiceCommand.Help

            else -> VoiceCommand.Unknown(rawQuery)
        }

    }
}
