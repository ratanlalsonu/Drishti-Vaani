package com.example.feature.voice

import com.example.core.model.DetectionRangeLimit
import com.example.core.model.VoiceCommand

object VoiceCommandParser {

    /**
     * Normalizes raw spoken input by trimming, converting to lowercase,
     * stripping punctuation (including Hindi danda '।'), and collapsing multiple spaces.
     */
    fun cleanText(input: String): String {
        return input
            .trim()
            .lowercase()
            .replace(Regex("[.,?!;:।_()\"'’“”`/\\[\\]{}\\-—*~]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Parses the best matching command from a list of ASR candidate transcriptions.
     * Returns the first non-Unknown command, or Unknown with the primary candidate text.
     */
    fun parseCandidates(candidates: List<String>): VoiceCommand {
        if (candidates.isEmpty()) return VoiceCommand.Unknown("")
        for (candidate in candidates) {
            val cmd = parse(candidate)
            if (cmd !is VoiceCommand.Unknown) {
                return cmd
            }
        }
        return VoiceCommand.Unknown(candidates.first().trim())
    }

    fun parse(rawQuery: String): VoiceCommand {
        val normalized = cleanText(rawQuery)
        if (normalized.isBlank()) return VoiceCommand.Unknown(rawQuery)

        return when {
            // 1. Stop Vision / Camera intents (checked before StartVision)
            containsAny(
                normalized,
                "stop vision", "stop camera", "vision band", "camera band", "band karo",
                "rok do", "ruk jao", "close camera", "exit vision", "shut camera",
                "turn off camera", "camera off", "off karo", "band kar do", "camera roko",
                "vision roko", "विजन बंद", "कैमरा बंद", "बंद करो", "रोक दो", "कैमरा रोको",
                "विजन रोको", "बंद कर दो", "कैमरा ऑफ", "ऑफ करो"
            ) -> VoiceCommand.StopVision

            // 2. Universal Dashboard / Home navigation commands
            containsAny(
                normalized,
                "dashboard", "home par", "home screen", "home le chalo", "main screen",
                "wapas", "peeche", "piche", "back to home", "go home", "go back",
                "shuruat par", "home jao", "wapas jao", "piche jao", "wapas chalo",
                "piche chalo", "shuruat", "डैशबोर्ड", "होम पर", "होम स्क्रीन",
                "होम ले चलो", "मेन स्क्रीन", "मुख्य स्क्रीन", "वापस", "पीछे", "वापस चलो",
                "पीछे चलो", "शुरुआत पर", "शुरू में जाओ", "शुरू में", "शुरुआत", "होम जाओ",
                "वापस जाओ", "पीछे जाओ", "घर चलो", "मुख्य पृष्ठ"
            ) || normalized == "home" || normalized == "back" || normalized == "होम" || normalized == "बैक" -> VoiceCommand.GoHome

            // 3. Vision Range Limit controls (e.g. 5m, 10m threshold, unlimited)
            containsAny(normalized, "5 meter", "5m", "paanch meter", "panch meter", "5 मीटर", "पाँच मीटर", "पांच मीटर") ->
                VoiceCommand.SetVisionRange(DetectionRangeLimit.SHORT_5M)

            containsAny(normalized, "10 meter", "10m", "das meter", "10 मीटर", "दस मीटर") ->
                VoiceCommand.SetVisionRange(DetectionRangeLimit.STANDARD_10M)

            containsAny(normalized, "all distance", "unlimited range", "sab doori", "sab duri", "unlimited", "सब दूरी", "सभी दूरी", "पूरी दूरी", "अनलिमिटेड") ->
                VoiceCommand.SetVisionRange(DetectionRangeLimit.UNLIMITED)

            // 4. Detailed Object Identification (Exact item name and identity)
            containsAny(
                normalized,
                "identify this object", "identify object", "what object is this", "name this object",
                "identify this", "what is this item", "what is this", "tell me this object",
                "recognize object", "identify product", "identify note", "identify currency",
                "vastu pehchano", "vastu ka naam", "ye kaun si vastu hai", "yeh kaun si vastu hai",
                "kaun si vastu hai", "vastu batao", "samne kaun si vastu hai", "cheez pehchano",
                "kaun si cheez hai", "ye kya cheez hai", "saman pehchano", "kya saman hai",
                "vastu ka naam batao", "cheez ka naam batao", "is cheez ka naam", "is vastu ka naam",
                "ye kya hai", "yeh kya hai", "ye kya cheez", "pehchano kya hai",
                "वस्तु पहचानो", "वस्तु का नाम बताओ", "यह कौन सी वस्तु है", "कौन सी वस्तु है",
                "वस्तु बताओ", "सामने कौन सी वस्तु है", "चीज पहचानो", "चीज़ पहचानो",
                "कौन सी चीज है", "कौन सी चीज़ है", "यह क्या चीज है", "सामान पहचानो",
                "क्या सामान है", "चीज का नाम बताओ", "चीज़ का नाम बताओ", "इस वस्तु का नाम",
                "यह क्या है", "ये क्या है", "पहचानो क्या है", "ऑब्जेक्ट पहचानो"
            ) -> VoiceCommand.IdentifyObject

            // 5. Query Surroundings intents (Checked before general StartVision)
            containsAny(
                normalized,
                "what is in front", "what is ahead", "what is around me", "what do you see",
                "describe this", "describe surroundings", "scan surroundings", "samne kya hai",
                "samne kya h", "samne kya", "aage kya hai", "aage kya h", "aage kya",
                "aas paas kya hai", "aaspaas kya hai", "kya dikh raha hai", "kya dikhta hai",
                "kya hai samne", "samne ka batao", "aage ka batao", "batao kya hai",
                "samne kaun hai", "aage kaun hai", "kya rakha hai", "samne kya dikhta hai",
                "batao samne", "samne dekho", "aage dekho", "tell me what is in front",
                "scan room", "kya dikh raha", "kya dikh rha", "samne batao", "aage batao",
                "rasta batao aage", "badha kya hai", "obstacle", "detect object", "identify object",
                "वस्तु पहचानो", "वस्तु बताओ", "रास्ते में क्या है", "बाधा क्या है",
                "सामने क्या है", "आगे क्या है", "क्या दिख रहा है", "क्या दिख रहा",
                "आस पास क्या है", "आसपास क्या है", "चारों तरफ क्या है", "क्या है सामने",
                "आगे क्या आ रहा है", "सामने बताओ", "आगे का बताओ", "सामने का बताओ",
                "बताओ क्या है", "सामने कौन है", "आगे कौन है", "सामने क्या रखा है",
                "चारों ओर", "स्कैन करो", "डिस्क्राइब करो", "सामने देखो", "आगे देखो",
                "पहचानो", "डिटेक्ट करो", "दिखाओ क्या है"
            ) || normalized == "सामने" || normalized == "आगे" -> VoiceCommand.QuerySurroundings

            // 5. OCR controls (Pause / Resume)
            containsAny(normalized, "pause", "ruko", "ruk jao", "rukiye", "रुको", "रुक जाओ", "पॉज़", "रुकिए") ->
                VoiceCommand.PauseReading

            containsAny(
                normalized,
                "continue", "resume", "aage padho", "padhna jari rakho", "aur padho", "jari rakho",
                "आगे पढ़ो", "जारी रखो", "फिर से पढ़ो", "कंटीन्यू", "और पढ़ो"
            ) -> VoiceCommand.ResumeReading

            // 6. Direct jumping to Text Reading / OCR from ANY screen
            containsAny(
                normalized,
                "text reader", "ocr", "kitab padho", "reading screen", "read this",
                "read text", "read book", "isko padho", "ye kya likha hai", "padh ke sunao",
                "padhke batao", "padh do", "kya likha hai", "kya likha h", "scan document",
                "scan text", "likha hua padho", "what is written", "padhiye", "read karo",
                "text padho", "akshar padho", "board padho", "kagaz padho", "sunao kya likha",
                "kya likha", "dastavez padho", "parcha padho", "board dekh kar padho",
                "पढ़ो", "पढ़िए", "किताब पढ़ो", "टेक्स्ट पढ़ो", "लिखा हुआ पढ़ो",
                "क्या लिखा है", "पढ़कर सुनाओ", "पढ़कर बताओ", "पढ़ के सुनाओ", "पढ़ दो",
                "अक्षर पढ़ो", "कागज़ पढ़ो", "कागज पढ़ो", "बोर्ड पढ़ो", "टेक्स्ट रीडर",
                "रीडिंग", "स्कैन टेक्स्ट", "लिखावट पढ़ो", "यह क्या लिखा है", "इसको पढ़ो",
                "पढ़कर", "सुनाओ क्या लिखा है", "क्या लिखा", "पढ़ना", "टेक्स्ट",
                "दस्तावेज़ पढ़ो", "दस्तावेज पढ़ो", "पर्चा पढ़ो", "दुकान का बोर्ड पढ़ो",
                "नोट पढ़ो", "रुपया पढ़ो"
            ) || normalized == "padho" || normalized == "read" || normalized == "पढ़ो" || normalized == "रीड" -> VoiceCommand.ReadText

            // 7. Direct Voice-Activated Phone Calling (Emergency or Contact)
            parseCallIntent(normalized) != null -> parseCallIntent(normalized)!!

            // 8. Emergency SOS intent
            containsAny(
                normalized,
                "emergency", "help me", "bachao", "sos", "khatra", "sahayata", "madad karo",
                "bachaao", "इमरजेंसी", "बचाओ", "मदद करो", "सहायता", "खतरा", "एसओएस", "आपातकाल", "हेल्प मी"
            ) -> VoiceCommand.Emergency

            // 9. Direct jumping to Location / GPS Navigation from ANY screen
            containsAny(
                normalized,
                "navigation", "gps", "where am i", "main kahan hoon", "main kahan hu",
                "kahan hu", "kaha hu", "kahan hoon", "kaha hoon", "location", "meri location",
                "location batao", "rasta dikhao", "rasta batao", "meri jagah", "navigation par le chalo",
                "gps kholo", "navigation screen", "kahan par hu", "kaha pe hu", "kahan pe hu",
                "kahan par hoon", "rasta", "मैं कहाँ हूँ", "मैं कहां हूं", "कहाँ हूँ",
                "कहां हूं", "मेरी लोकेशन", "लोकेशन बताओ", "कहाँ पर हूँ", "कहां पर हूं",
                "रास्ता दिखाओ", "रास्ता बताओ", "नेविगेशन", "जीपीएस", "मेरी जगह", "कहाँ हूँ मैं",
                "स्थान बताओ", "स्थान", "लोकेशन"
            ) -> VoiceCommand.WhereAmI

            // 10. Find Nearby Place intents
            containsAny(normalized, "hospital", "aspatal", "अस्पताल", "हॉस्पिटल", "दवाखाना") ->
                VoiceCommand.FindNearby("hospital")

            containsAny(normalized, "pharmacy", "chemist", "dawa", "dawai", "दवा", "दवाई", "केमिस्ट", "फार्मेसी", "मेडिकल") ->
                VoiceCommand.FindNearby("pharmacy")

            containsAny(normalized, "atm", "bank", "एटीएम", "ए टी एम", "बैंक") ->
                VoiceCommand.FindNearby("atm")

            containsAny(normalized, "police", "thana", "पुलिस", "थाना", "चौकी") ->
                VoiceCommand.FindNearby("police")

            containsAny(normalized, "bus stop", "railway", "station", "transit", "बस स्टॉप", "बस स्टैंड", "रेलवे", "स्टेशन", "मेट्रो") ->
                VoiceCommand.FindNearby("transit_station")

            // 11. Battery & Time status check
            containsAny(
                normalized,
                "battery", "charge", "battery kitni hai", "charge kitna hai", "phone ki battery",
                "battery status", "बैटरी", "चार्ज", "बैटरी कितनी है", "चार्ज कितना है", "बैटरी बताओ",
                "फोन की बैटरी"
            ) -> VoiceCommand.CheckBattery

            containsAny(
                normalized,
                "time", "samay", "kitne baje", "kitne baje hai", "kitne baje hain", "time kya hai",
                "time batao", "samay batao", "kya time hua", "what time is it", "current time",
                "समय", "टाइम", "कितने बजे हैं", "कितने बजे", "समय क्या है", "क्या समय हुआ",
                "टाइम क्या है", "टाइम बताओ", "समय बताओ"
            ) -> VoiceCommand.CheckTime

            // 12. Speech playback controls
            containsAny(
                normalized,
                "repeat", "phir se bolo", "phirse bolo", "dobara bolo", "fir se bolo",
                "say again", "repeat please", "फिर से बोलो", "दोबारा बोलो", "फिर बोलो",
                "एक बार फिर बोलो", "रिपीट करो", "रिपीट"
            ) -> VoiceCommand.RepeatSpeech

            containsAny(
                normalized,
                "stop speaking", "chup ho jao", "shant raho", "chup raho", "awaz band",
                "chup", "be quiet", "shut up", "silence", "stop talking", "चुप हो जाओ",
                "शांत रहो", "चुप रहो", "आवाज बंद करो", "आवाज़ बंद करो", "शांत", "चुप"
            ) -> VoiceCommand.StopSpeech

            // 13. Settings
            containsAny(
                normalized,
                "settings", "setting", "settings kholo", "setting kholo", "open settings",
                "preferences", "सेटिंग्स", "सेटिंग", "सेटिंग्स खोलो", "सेटिंग खोलो"
            ) -> VoiceCommand.OpenSettings

            // 14. Help
            containsAny(
                normalized,
                "help", "madad", "commands", "kya bolun", "kya bolu", "madad chahiye",
                "show commands", "help screen", "मदद", "हेल्प", "सहायता", "कमांड बताओ",
                "कमांड्स", "क्या बोलूँ", "क्या बोलूं", "मदद चाहिए"
            ) -> VoiceCommand.Help

            // 15. Navigation to specific places
            containsAny(normalized, "take me home", "ghar le chalo", "घर ले चलो", "घर चलो", "घर") ->
                VoiceCommand.NavigateTo("Home")

            normalized.startsWith("navigate to") -> {
                val dest = normalized.removePrefix("navigate to").trim()
                VoiceCommand.NavigateTo(dest)
            }

            normalized.contains("le chalo") && !containsAny(normalized, "vision", "home", "camera", "ocr", "reader", "emergency", "dashboard") -> {
                val dest = normalized.replace("mujhe", "").replace("le chalo", "").trim()
                VoiceCommand.NavigateTo(dest)
            }

            normalized.contains("ले चलो") && !containsAny(normalized, "विजन", "होम", "कैमरा", "इमरजेंसी", "डैशबोर्ड") -> {
                val dest = normalized.replace("मुझे", "").replace("ले चलो", "").trim()
                VoiceCommand.NavigateTo(dest)
            }

            // 16. Direct jumping to Vision from ANY screen (checked after specific vision queries/stops)
            containsAny(
                normalized,
                "vision", "camera", "start vision", "open camera", "start camera",
                "camera chalu", "camera kholo", "camera start", "camera open", "vision chalu",
                "vision kholo", "vision start", "shuru karo", "chalu karo", "kholo",
                "chalao", "on karo", "camera on", "vision on", "live camera", "live vision",
                "start", "dekho", "dekhna", "dekhna hai", "photo khincho", "photo lo",
                "कैमरा", "विजन", "कैमरा चालू करो", "विजन चालू करो", "कैमरा शुरू करो",
                "विजन शुरू करो", "कैमरा खोलो", "विजन खोलो", "कैमरा चलाओ", "विजन चलाओ",
                "चालू करो", "शुरू करो", "स्टार्ट विजन", "ओपन कैमरा", "कैमरा ऑन", "ऑन करो",
                "लाइव कैमरा", "लाइव विजन", "दिखाओ", "देखना है", "कैमरा चालू", "विजन चालू",
                "कैमरा शुरू", "विजन शुरू"
            ) || normalized == "camera" || normalized == "vision" || normalized == "कैमरा" || normalized == "विजन" -> VoiceCommand.StartVision

            else -> VoiceCommand.Unknown(rawQuery)
        }
    }

    private fun containsAny(text: String, vararg targets: String): Boolean {
        for (target in targets) {
            if (text.contains(target)) return true
        }
        return false
    }

    fun parseCallIntent(normalized: String): VoiceCommand? {
        // Exclude system features and actions from call parser
        if (containsAny(
                normalized,
                "camera", "vision", "कैमरा", "विजन", "ocr", "reader", "padho", "padh", "read",
                "kahan", "location", "battery", "time", "setting", "सेटिंग", "डैशबोर्ड", "dashboard",
                "home", "वस्तु", "सामने", "surrounding", "stop speaking", "chup", "phir se"
            )) {
            return null
        }

        // 1. Direct Emergency Numbers
        if (containsAny(
                normalized,
                "call emergency", "emergency call", "112 par call", "call 112", "call 100",
                "100 par call", "emergency ko call", "police ko call", "ambulance ko call",
                "इमरजेंसी कॉल", "112 पर कॉल", "100 पर कॉल", "कॉल 112", "कॉल 100",
                "पुलिस को फोन", "इमरजेंसी को कॉल", "112 लगाओ", "100 लगाओ",
                "112 par phone", "100 par phone", "112 ko phone", "100 ko phone",
                "112 ko call", "100 ko call", "112 मिलाओ", "100 मिलाओ"
            ) || normalized == "112" || normalized == "100") {
            return VoiceCommand.CallEmergency
        }

        // 2. Standalone call phrases (user asking to place a call to saved/primary contact)
        val standaloneCallPhrases = setOf(
            "phone lagao", "call lagao", "phone karo", "call karo", "phone milao", "call milao",
            "dial karo", "call", "phone", "ek call", "ek phone", "ek call lagao", "ek phone lagao",
            "ek call karo", "ek phone karo", "call kar do", "phone kar do", "call laga do", "phone laga do",
            "फोन लगाओ", "कॉल लगाओ", "फोन करो", "कॉल करो", "फोन मिलाओ", "कॉल मिलाओ", "कॉल", "फोन",
            "एक कॉल", "एक फोन", "एक कॉल लगाओ", "एक फोन लगाओ", "एक कॉल करो", "एक फोन करो",
            "कॉल कर दो", "फोन कर दो", "कॉल लगा दो", "फोन लगा दो"
        )
        if (normalized in standaloneCallPhrases) {
            return VoiceCommand.CallContact("")
        }

        // 3. Must contain at least one calling keyword to proceed
        val hasCallKeyword = containsAny(
            normalized,
            "phone lagao", "call lagao", "phone karo", "call karo", "phone milao", "call milao",
            "ko phone", "ko call", "ko lagao", "ko milao", "dial karo", "dial lagao", "call ", "phone ",
            "फोन लगाओ", "कॉल लगाओ", "फोन करो", "कॉल करो", "फोन मिलाओ", "कॉल मिलाओ",
            "को फोन", "को कॉल", "को लगाओ", "को मिलाओ", "डायल करो", "डायल लगाओ", "कॉल ", "फोन "
        ) || normalized.startsWith("phone ") || normalized.startsWith("फोन ") ||
           normalized.startsWith("call ") || normalized.startsWith("कॉल ") ||
           normalized.endsWith(" lagao") || normalized.endsWith(" लगाओ") ||
           normalized.endsWith(" karo") || normalized.endsWith(" करो")

        if (!hasCallKeyword) return null

        var text = normalized

        // Remove conversational prefixes: "please ", "kripya ", "zara ", "ek ", "mujhe ", etc.
        val convPrefixes = listOf(
            "please ", "kripya ", "कृपया ", "zara ", "जरा ", "ek ", "एक ",
            "mujhe ", "मुझे ", "zara sa ", "thoda "
        )
        for (p in convPrefixes) {
            if (text.startsWith(p)) {
                text = text.removePrefix(p).trim()
            }
        }

        // Pattern A: Action Prefix -> e.g. "phone lagao rahul", "phone lagao rahul ko", "call rahul", "कॉल सोनू"
        val actionPrefixes = listOf(
            "phone lagao ", "call lagao ", "phone karo ", "call karo ", "phone milao ", "call milao ",
            "dial karo ", "phone laga do ", "call laga do ", "phone kar do ", "call kar do ",
            "call ", "phone ",
            "फोन लगाओ ", "कॉल लगाओ ", "फोन करो ", "कॉल करो ", "फोन मिलाओ ", "कॉल मिलाओ ",
            "डायल करो ", "फोन लगा दो ", "कॉल लगा दो ", "फोन कर दो ", "कॉल कर दो ",
            "कॉल ", "फोन "
        )
        for (prefix in actionPrefixes) {
            if (text.startsWith(prefix)) {
                var target = text.removePrefix(prefix).trim()
                // Strip trailing noise words
                target = target
                    .removeSuffix(" ko").removeSuffix(" को")
                    .removeSuffix(" lagao").removeSuffix(" लगाओ")
                    .removeSuffix(" karo").removeSuffix(" करो")
                    .removeSuffix(" please").removeSuffix(" प्लीज")
                    .removeSuffix(" laga do").removeSuffix(" लगा दो")
                    .removeSuffix(" kar do").removeSuffix(" कर दो")
                    .removeSuffix(" milao").removeSuffix(" मिलाओ")
                    .trim()
                if (target.isNotBlank()) {
                    return if (containsAny(target, "emergency", "112", "100", "police", "ambulance", "इमरजेंसी", "पुलिस")) {
                        VoiceCommand.CallEmergency
                    } else {
                        VoiceCommand.CallContact(target)
                    }
                }
            }
        }

        // Pattern B: Infix with "ko" / "को" -> e.g. "rahul ko phone lagao", "rahul ko call karo", "rahul ko lagao"
        if (text.contains(" ko ") || text.contains(" को ") || text.endsWith(" ko") || text.endsWith(" को")) {
            val delimiter = if (text.contains(" ko ") || text.endsWith(" ko")) " ko" else " को"
            val parts = text.split(delimiter)
            var namePart = parts.firstOrNull()?.trim() ?: ""
            // Clean action words from namePart in case prefix was mixed
            for (prefix in actionPrefixes) {
                namePart = namePart.replace(prefix.trim(), "").trim()
            }
            if (namePart.isNotBlank()) {
                return if (containsAny(namePart, "emergency", "112", "100", "police", "ambulance", "इमरजेंसी", "पुलिस")) {
                    VoiceCommand.CallEmergency
                } else {
                    VoiceCommand.CallContact(namePart)
                }
            }
        }

        // Pattern C: Suffix -> e.g. "rahul phone lagao", "rahul call lagao", "सोनू फोन लगाओ"
        val actionSuffixes = listOf(
            " phone lagao", " call lagao", " phone karo", " call karo", " phone milao", " call milao",
            " laga do", " kar do", " lagana",
            " फोन लगाओ", " कॉल लगाओ", " फोन करो", " कॉल करो", " फोन मिलाओ", " कॉल मिलाओ",
            " लगा दो", " कर दो", " लगाना"
        )
        for (suffix in actionSuffixes) {
            if (text.endsWith(suffix)) {
                var target = text.removeSuffix(suffix).trim()
                target = target.removeSuffix(" ko").removeSuffix(" को").trim()
                if (target.isNotBlank()) {
                    return if (containsAny(target, "emergency", "112", "100", "police", "ambulance", "इमरजेंसी", "पुलिस")) {
                        VoiceCommand.CallEmergency
                    } else {
                        VoiceCommand.CallContact(target)
                    }
                }
            }
        }

        return null
    }
}
