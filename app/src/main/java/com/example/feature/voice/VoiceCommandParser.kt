package com.example.feature.voice

import com.example.core.model.VoiceCommand

object VoiceCommandParser {
    fun parse(rawQuery: String): VoiceCommand {
        val normalized = rawQuery.trim().lowercase()

        return when {
            // Stop Vision intents (checked before StartVision)
            normalized.contains("stop vision") ||
            normalized.contains("vision band") ||
            normalized.contains("camera band") ||
            normalized.contains("rok do") ||
            normalized.contains("band karo") ||
            normalized.contains("close camera") -> VoiceCommand.StopVision

            // Universal Dashboard / Home navigation commands
            normalized.contains("dashboard") ||
            normalized.contains("home par") ||
            normalized.contains("home screen") ||
            normalized.contains("home le chalo") ||
            normalized.contains("main screen") ||
            normalized.contains("wapas") ||
            normalized.contains("peeche") ||
            normalized.contains("piche") ||
            normalized.contains("back to home") ||
            normalized.contains("go home") ||
            normalized.contains("go back") ||
            normalized.contains("shuruat par") ||
            normalized == "home" ||
            normalized == "back" -> VoiceCommand.GoHome

            // Direct jumping to Vision from ANY screen
            normalized.contains("vision") ||
            normalized.contains("camera") ||
            normalized.contains("start vision") ||
            normalized.contains("shuru karo") -> VoiceCommand.StartVision

            // Query Surroundings intents
            normalized.contains("what is in front") ||
            normalized.contains("what is ahead") ||
            normalized.contains("samne kya hai") ||
            normalized.contains("aas paas kya hai") ||
            normalized.contains("kya dikh raha hai") ||
            normalized.contains("what is around me") ||
            normalized.contains("describe this") -> VoiceCommand.QuerySurroundings

            // Vision Range Limit controls (e.g. 5m, 10m threshold)
            normalized.contains("5 meter") || normalized.contains("5m") || normalized.contains("paanch meter") ->
                VoiceCommand.SetVisionRange(com.example.core.model.DetectionRangeLimit.SHORT_5M)

            normalized.contains("10 meter") || normalized.contains("10m") || normalized.contains("das meter") ->
                VoiceCommand.SetVisionRange(com.example.core.model.DetectionRangeLimit.STANDARD_10M)

            normalized.contains("all distance") || normalized.contains("unlimited range") || normalized.contains("sab doori") ->
                VoiceCommand.SetVisionRange(com.example.core.model.DetectionRangeLimit.UNLIMITED)

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

            // Multi-SIM Voice Selection Answers
            normalized == "sim 1" || normalized == "sim 1 se" || normalized == "sim 1 se call lagao" ||
            normalized == "pehla sim" || normalized == "pehle sim se" || normalized == "sim one" ||
            normalized == "first sim" || normalized == "sim 1 se call" -> VoiceCommand.SelectSim(0)

            normalized == "sim 2" || normalized == "sim 2 se" || normalized == "sim 2 se call lagao" ||
            normalized == "doosra sim" || normalized == "dusra sim" || normalized == "dusre sim se" ||
            normalized == "sim two" || normalized == "second sim" || normalized == "sim 2 se call" -> VoiceCommand.SelectSim(1)

            // Caller App Voice Selection Answers
            normalized == "phone" || normalized == "phone app" || normalized == "phone se" || normalized == "dialer" ->
                VoiceCommand.SelectCallerApp("phone")
            normalized == "whatsapp" || normalized == "whatsapp se" || normalized == "whatsapp call" ->
                VoiceCommand.SelectCallerApp("whatsapp")
            normalized == "truecaller" || normalized == "truecaller se" ->
                VoiceCommand.SelectCallerApp("truecaller")

            // Direct Phone Call with SIM already specified (e.g. "SIM 1 se Papa ko call karo")
            (normalized.contains("sim 1") || normalized.contains("pehla sim")) && (normalized.contains("call") || normalized.contains("phone")) -> {
                val cleaned = normalized
                    .replace("sim 1", "")
                    .replace("pehla sim", "")
                    .replace("pehle sim se", "")
                    .replace("se", "")
                    .replace("karo", "")
                    .replace("lagao", "")
                    .replace("milao", "")
                    .replace("call", "")
                    .replace("phone", "")
                    .replace("ko", "")
                    .trim()
                if (cleaned.isNotBlank()) {
                    VoiceCommand.CallContactWithSim(cleaned, 0)
                } else {
                    VoiceCommand.SelectSim(0)
                }
            }

            (normalized.contains("sim 2") || normalized.contains("dusra sim") || normalized.contains("doosra sim")) && (normalized.contains("call") || normalized.contains("phone")) -> {
                val cleaned = normalized
                    .replace("sim 2", "")
                    .replace("dusra sim", "")
                    .replace("doosra sim", "")
                    .replace("dusre sim se", "")
                    .replace("se", "")
                    .replace("karo", "")
                    .replace("lagao", "")
                    .replace("milao", "")
                    .replace("call", "")
                    .replace("phone", "")
                    .replace("ko", "")
                    .trim()
                if (cleaned.isNotBlank()) {
                    VoiceCommand.CallContactWithSim(cleaned, 1)
                } else {
                    VoiceCommand.SelectSim(1)
                }
            }

            // Direct Voice-Activated Phone Calling (Hands-Free for Blind Users)
            normalized == "call emergency" || normalized == "emergency call" ||
            normalized.contains("112 par call") || normalized.contains("call 112") ||
            normalized.contains("call 100") || normalized.contains("100 par call") ||
            normalized.contains("emergency ko call") -> VoiceCommand.CallEmergency

            normalized.startsWith("call ") && !normalized.contains("camera") && !normalized.contains("vision") && !normalized.contains("settings") -> {
                val target = normalized.removePrefix("call").trim()
                if (target.isNotBlank()) VoiceCommand.CallContact(target) else VoiceCommand.CallEmergency
            }

            normalized.contains("ko call") || normalized.contains("ko phone") -> {
                val cleaned = normalized
                    .replace("karo", "")
                    .replace("lagao", "")
                    .replace("milao", "")
                    .replace("kijiye", "")
                    .trim()
                val parts = if (cleaned.contains("ko call")) {
                    cleaned.split("ko call")
                } else {
                    cleaned.split("ko phone")
                }
                val name = parts.firstOrNull()?.trim() ?: ""
                if (name.isNotBlank()) {
                    VoiceCommand.CallContact(name)
                } else {
                    VoiceCommand.CallEmergency
                }
            }

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
