package com.example.core.util

import com.example.data.local.entity.EmergencyContact
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object ContactMatcher {

    /**
     * Finds the best matching emergency contact from a list of contacts given a spoken query.
     */
    fun findBestMatch(contacts: List<EmergencyContact>, rawQuery: String): EmergencyContact? {
        if (contacts.isEmpty()) return null

        val query = rawQuery.trim()

        // 1. If query is empty (user just said "phone lagao", "call lagao", etc.)
        if (query.isBlank()) {
            return contacts.firstOrNull { it.isPrimary } ?: contacts.firstOrNull()
        }

        // 2. If there's only one contact saved and user asked to call someone (not a direct number)
        if (contacts.size == 1 && !query.matches(Regex("^[0-9+]{3,14}$"))) {
            return contacts.first()
        }

        val cleanQ = query.lowercase(Locale.ROOT)

        // 3. Direct Case-Insensitive Exact Match
        val exactMatch = contacts.firstOrNull {
            it.name.trim().equals(query, ignoreCase = true) ||
            it.relationship.trim().equals(query, ignoreCase = true)
        }
        if (exactMatch != null) return exactMatch

        // 4. Case-Insensitive Substring Match (both directions)
        val substringMatch = contacts.firstOrNull {
            val contactName = it.name.trim().lowercase(Locale.ROOT)
            val rel = it.relationship.trim().lowercase(Locale.ROOT)
            (contactName.isNotBlank() && (contactName.contains(cleanQ) || cleanQ.contains(contactName))) ||
            (rel.isNotBlank() && (rel.contains(cleanQ) || cleanQ.contains(rel)))
        }
        if (substringMatch != null) return substringMatch

        // 5. Semantic Relationship Matching (e.g. "पापा" / "papa" <-> "father", "मम्मी" <-> "mother")
        val semanticMatch = findSemanticMatch(contacts, cleanQ)
        if (semanticMatch != null) return semanticMatch

        // 6. Phonetic Devanagari <-> Latin Transliteration & Normalization Match
        val normalizedQuery = toPhoneticLatin(cleanQ)
        if (normalizedQuery.isNotEmpty()) {
            // Check exact phonetic match
            val exactPhonetic = contacts.firstOrNull {
                val normName = toPhoneticLatin(it.name)
                normName == normalizedQuery
            }
            if (exactPhonetic != null) return exactPhonetic

            // Check phonetic containment
            val phoneticContainment = contacts.firstOrNull {
                val normName = toPhoneticLatin(it.name)
                val normRel = toPhoneticLatin(it.relationship)
                (normName.isNotEmpty() && (normName.contains(normalizedQuery) || normalizedQuery.contains(normName))) ||
                (normRel.isNotEmpty() && (normRel.contains(normalizedQuery) || normalizedQuery.contains(normRel)))
            }
            if (phoneticContainment != null) return phoneticContainment

            // Check token overlap (e.g. "sonu" matching "sonu kumar" or "sonu bhai")
            val queryTokens = normalizedQuery.split(" ").filter { it.length >= 2 }
            for (contact in contacts) {
                val contactTokens = toPhoneticLatin(contact.name).split(" ").filter { it.length >= 2 }
                for (qToken in queryTokens) {
                    if (contactTokens.any { cToken ->
                            cToken == qToken ||
                            (cToken.length >= 3 && qToken.length >= 3 && (cToken.contains(qToken) || qToken.contains(cToken)))
                        }) {
                        return contact
                    }
                }
            }

            // 7. Fuzzy Levenshtein Distance Match on Normalized Phonetic Strings
            var bestContact: EmergencyContact? = null
            var bestSimilarity = 0.65 // Minimum 65% phonetic similarity threshold

            for (contact in contacts) {
                val normContact = toPhoneticLatin(contact.name)
                val sim = computeSimilarity(normalizedQuery, normContact)
                if (sim > bestSimilarity) {
                    bestSimilarity = sim
                    bestContact = contact
                }
            }
            if (bestContact != null) return bestContact
        }

        return null
    }

    /**
     * Converts Devanagari or Latin text into a standardized phonetic Latin representation.
     */
    fun toPhoneticLatin(input: String): String {
        val transliterated = devanagariToLatin(input.trim().lowercase(Locale.ROOT))
        return normalizePhonetics(transliterated)
    }

    /**
     * Transliterates Hindi Devanagari script characters into phonetic English letters.
     */
    fun devanagariToLatin(input: String): String {
        val preprocessed = input
            .replace("अं", "an")
            .replace("अः", "ah")
            .replace("ड़", "d")
            .replace("ढ़", "dh")
            .replace("फ़", "f")
            .replace("ज़", "z")
            .replace("क़", "q")
            .replace("ख़", "kh")
            .replace("ग़", "gh")
            .replace("\u093C", "") // strip any decomposed nukta sign

        val sb = StringBuilder()
        for (i in preprocessed.indices) {
            val ch = preprocessed[i]
            val mapped = when (ch) {
                // Independent Vowels
                'अ' -> "a"
                'आ' -> "a"
                'इ' -> "i"
                'ई' -> "i"
                'उ' -> "u"
                'ऊ' -> "u"
                'ऋ' -> "ri"
                'ए' -> "e"
                'ऐ' -> "ai"
                'ओ' -> "o"
                'औ' -> "au"

                // Matras (Vowel signs)
                'ा' -> "a"
                'ि' -> "i"
                'ी' -> "i"
                'ु' -> "u"
                'ू' -> "u"
                'ृ' -> "ri"
                'े' -> "e"
                'ै' -> "ai"
                'ो' -> "o"
                'ौ' -> "au"
                'ं' -> "n"
                'ँ' -> "n"
                'ः' -> "h"
                'ॅ' -> "e"
                'ॉ' -> "o"
                '्' -> "" // Halant / Virama suppresses vowel

                // Consonants
                'क' -> "k"
                'ख' -> "kh"
                'ग' -> "g"
                'घ' -> "gh"
                'ङ' -> "ng"
                'च' -> "ch"
                'छ' -> "chh"
                'ज' -> "j"
                'झ' -> "jh"
                'ञ' -> "ny"
                'ट' -> "t"
                'ठ' -> "th"
                'ड' -> "d"
                'ढ' -> "dh"
                'ण' -> "n"
                'त' -> "t"
                'थ' -> "th"
                'द' -> "d"
                'ध' -> "dh"
                'न' -> "n"
                'प' -> "p"
                'फ' -> "ph"
                'ब' -> "b"
                'भ' -> "bh"
                'म' -> "m"
                'य' -> "y"
                'र' -> "r"
                'ल' -> "l"
                'व' -> "v"
                'श' -> "sh"
                'ष' -> "sh"
                'स' -> "s"
                'ह' -> "h"

                else -> ch.toString()
            }
            sb.append(mapped)
        }
        return sb.toString()
    }

    /**
     * Standardizes variant phonetic representations, collapsing duplicated vowels/consonants
     * and stripping typical Indian honorific titles (e.g. ji, bhaiya, saab).
     */
    fun normalizePhonetics(input: String): String {
        var s = input.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace("ee", "i")
            .replace("oo", "u")
            .replace("aa", "a")
            .replace("w", "v")
            .replace("ph", "f")
            .replace("sh", "s")
            .replace("z", "j")
            .replace("kh", "k")
            .replace("gh", "g")
            .replace("bh", "b")
            .replace("dh", "d")
            .replace("th", "t")
            .replace("jh", "j")
            .replace("chh", "c")
            .replace("ch", "c")
            // Collapse doubled characters e.g. "sonuu" -> "sonu", "mmmi" -> "mi"
            .replace(Regex("(.)\\1+"), "$1")

        // Strip common Indian honorifics and relational suffixes
        val honorifics = listOf("bhaiya", "bhaiyya", "bhai", "ji", "saab", "sahab", "sir", "madam", "kumar", "singh")
        for (h in honorifics) {
            s = s.replace(Regex("\\b$h\\b"), " ")
        }
        return s.replace(Regex("\\s+"), " ").trim()
    }

    private fun findSemanticMatch(contacts: List<EmergencyContact>, query: String): EmergencyContact? {
        val semanticGroups = listOf(
            setOf("papa", "pita", "pitaji", "dad", "father", "पापा", "पिता", "पिताजी"),
            setOf("mummy", "mom", "mother", "mata", "mataji", "maa", "मम्मी", "माता", "माताजी", "मां", "माँ"),
            setOf("bhai", "brother", "bhaiya", "bhaiyya", "bro", "भाई", "भैया"),
            setOf("behen", "sister", "didi", "sis", "दीदी", "बहन"),
            setOf("doctor", "dr", "chikitsak", "डॉक्टर", "डाक्टर"),
            setOf("police", "thana", "पुलिस", "थाना"),
            setOf("ambulance", "aspatal", "hospital", "एम्बुलेंस", "अस्पताल"),
            setOf("dost", "friend", "mitra", "yaar", "दोस्त", "मित्र")
        )

        for (group in semanticGroups) {
            val queryMatches = group.any { g ->
                g.isNotBlank() && (query == g || (g.length >= 3 && query.contains(g)) || (query.length >= 3 && g.contains(query)))
            }
            if (queryMatches) {
                val matched = contacts.firstOrNull { contact ->
                    val contactNameLower = contact.name.lowercase(Locale.ROOT).trim()
                    val relLower = contact.relationship.lowercase(Locale.ROOT).trim()
                    group.any { g ->
                        g.isNotBlank() && (
                            (contactNameLower.isNotBlank() && (contactNameLower == g || (g.length >= 3 && contactNameLower.contains(g)) || (contactNameLower.length >= 3 && g.contains(contactNameLower)))) ||
                            (relLower.isNotBlank() && (relLower == g || (g.length >= 3 && relLower.contains(g)) || (relLower.length >= 3 && g.contains(relLower))))
                        )
                    }
                }
                if (matched != null) return matched
            }
        }
        return null
    }

    fun computeSimilarity(s1: String, s2: String): Double {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        val distance = levenshteinDistance(s1, s2)
        val maxLen = max(s1.length, s2.length)
        return 1.0 - (distance.toDouble() / maxLen.toDouble())
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }
}
