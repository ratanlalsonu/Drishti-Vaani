package com.example

import com.example.core.util.ContactMatcher
import com.example.data.local.entity.EmergencyContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ContactMatcherTest {

    private val sampleContacts = listOf(
        EmergencyContact(id = 1, name = "Sonu", phoneNumber = "9876543210", isPrimary = true),
        EmergencyContact(id = 2, name = "Papa", phoneNumber = "9876543211", relationship = "Father", isPrimary = false),
        EmergencyContact(id = 3, name = "Rahul", phoneNumber = "9876543212", isPrimary = false),
        EmergencyContact(id = 4, name = "Mummy", phoneNumber = "9876543213", relationship = "Mother", isPrimary = false),
        EmergencyContact(id = 5, name = "Dr. Sharma", phoneNumber = "9876543214", relationship = "Doctor", isPrimary = false)
    )

    @Test
    fun testDevanagariToEnglishContactMatching() {
        // Speech Recognizer transcribes in Hindi Devanagari ("सोनू", "राहुल", "पापा", "मम्मी")
        val matchSonu = ContactMatcher.findBestMatch(sampleContacts, "सोनू")
        assertNotNull(matchSonu)
        assertEquals("Sonu", matchSonu?.name)

        val matchRahul = ContactMatcher.findBestMatch(sampleContacts, "राहुल")
        assertNotNull(matchRahul)
        assertEquals("Rahul", matchRahul?.name)

        val matchPapa = ContactMatcher.findBestMatch(sampleContacts, "पापा")
        assertNotNull(matchPapa)
        assertEquals("Papa", matchPapa?.name)

        val matchMummy = ContactMatcher.findBestMatch(sampleContacts, "मम्मी")
        assertNotNull(matchMummy)
        assertEquals("Mummy", matchMummy?.name)
    }

    @Test
    fun testHonorificsAndSuffixes() {
        // Blind user speaks with honorifics: "Sonu Bhai", "सोनू भैया", "Rahul ji"
        val matchSonuBhai = ContactMatcher.findBestMatch(sampleContacts, "Sonu Bhai")
        assertNotNull(matchSonuBhai)
        assertEquals("Sonu", matchSonuBhai?.name)

        val matchSonuBhaiya = ContactMatcher.findBestMatch(sampleContacts, "सोनू भैया")
        assertNotNull(matchSonuBhaiya)
        assertEquals("Sonu", matchSonuBhaiya?.name)

        val matchRahulJi = ContactMatcher.findBestMatch(sampleContacts, "Rahul ji")
        assertNotNull(matchRahulJi)
        assertEquals("Rahul", matchRahulJi?.name)
    }

    @Test
    fun testSemanticRelationshipMatching() {
        // Calling "father" or "पिताजी" matches Papa
        val matchFather = ContactMatcher.findBestMatch(sampleContacts, "father")
        assertNotNull(matchFather)
        assertEquals("Papa", matchFather?.name)

        val matchPitaji = ContactMatcher.findBestMatch(sampleContacts, "पिताजी")
        assertNotNull(matchPitaji)
        assertEquals("Papa", matchPitaji?.name)

        // Calling "doctor" or "डॉक्टर" matches Dr. Sharma
        val matchDoctor = ContactMatcher.findBestMatch(sampleContacts, "doctor")
        assertNotNull(matchDoctor)
        assertEquals("Dr. Sharma", matchDoctor?.name)

        val matchDoctorHindi = ContactMatcher.findBestMatch(sampleContacts, "डॉक्टर")
        assertNotNull(matchDoctorHindi)
        assertEquals("Dr. Sharma", matchDoctorHindi?.name)
    }

    @Test
    fun testEmptyQueryReturnsPrimaryContact() {
        // User just says "phone lagao" or "call lagao" without specifying a name
        val matchPrimary = ContactMatcher.findBestMatch(sampleContacts, "")
        assertNotNull(matchPrimary)
        assertEquals("Sonu", matchPrimary?.name)
    }

    @Test
    fun testSingleContactSavedAlwaysMatched() {
        val singleContactList = listOf(
            EmergencyContact(id = 1, name = "Sonu", phoneNumber = "9876543210", isPrimary = true)
        )
        // If only 1 contact exists in DB, any generic call request routes to it
        val match = ContactMatcher.findBestMatch(singleContactList, "phone lagao")
        assertNotNull(match)
        assertEquals("Sonu", match?.name)

        val matchHindi = ContactMatcher.findBestMatch(singleContactList, "सोनू")
        assertNotNull(matchHindi)
        assertEquals("Sonu", matchHindi?.name)
    }

    @Test
    fun testFuzzySpellingMatch() {
        // User ASR produces slight variant spelling "Sonoo"
        val match = ContactMatcher.findBestMatch(sampleContacts, "Sonoo")
        assertNotNull(match)
        assertEquals("Sonu", match?.name)
    }
}
