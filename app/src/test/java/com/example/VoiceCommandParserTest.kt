package com.example

import com.example.core.model.DetectionRangeLimit
import com.example.core.model.VoiceCommand
import com.example.feature.voice.VoiceCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandParserTest {

    @Test
    fun testUniversalNavigationCommands() {
        // Universal return to dashboard / home
        assertEquals(VoiceCommand.GoHome, VoiceCommandParser.parse("dashboard par le chalo"))
        assertEquals(VoiceCommand.GoHome, VoiceCommandParser.parse("home screen"))
        assertEquals(VoiceCommand.GoHome, VoiceCommandParser.parse("wapas chalo"))
        assertEquals(VoiceCommand.GoHome, VoiceCommandParser.parse("go back"))
        assertEquals(VoiceCommand.GoHome, VoiceCommandParser.parse("dashboard"))

        // Jumping directly to any feature from anywhere
        assertEquals(VoiceCommand.StartVision, VoiceCommandParser.parse("vision par le chalo"))
        assertEquals(VoiceCommand.StartVision, VoiceCommandParser.parse("camera par le chalo"))
        assertEquals(VoiceCommand.ReadText, VoiceCommandParser.parse("text reader par le chalo"))
        assertEquals(VoiceCommand.ReadText, VoiceCommandParser.parse("ocr par le chalo"))
        assertEquals(VoiceCommand.WhereAmI, VoiceCommandParser.parse("navigation par le chalo"))
        assertEquals(VoiceCommand.Emergency, VoiceCommandParser.parse("emergency par le chalo"))

        // System queries
        assertEquals(VoiceCommand.CheckBattery, VoiceCommandParser.parse("battery kitni hai"))
        assertEquals(VoiceCommand.CheckTime, VoiceCommandParser.parse("kitne baje hain"))
    }

    @Test
    fun testEnglishVoiceCommands() {
        assertEquals(VoiceCommand.StartVision, VoiceCommandParser.parse("start vision"))
        assertEquals(VoiceCommand.StopVision, VoiceCommandParser.parse("stop vision"))
        assertEquals(VoiceCommand.QuerySurroundings, VoiceCommandParser.parse("what is in front of me"))
        assertEquals(VoiceCommand.IdentifyObject, VoiceCommandParser.parse("identify this object"))
        assertEquals(VoiceCommand.IdentifyObject, VoiceCommandParser.parse("what is this item"))
        assertEquals(VoiceCommand.IdentifyObject, VoiceCommandParser.parse("name this object"))
        assertEquals(VoiceCommand.ReadText, VoiceCommandParser.parse("read this"))
        assertEquals(VoiceCommand.WhereAmI, VoiceCommandParser.parse("where am I"))
        assertEquals(VoiceCommand.Emergency, VoiceCommandParser.parse("emergency"))
        assertEquals(VoiceCommand.StopSpeech, VoiceCommandParser.parse("stop speaking"))
        assertEquals(VoiceCommand.RepeatSpeech, VoiceCommandParser.parse("repeat please"))
        assertEquals(VoiceCommand.OpenSettings, VoiceCommandParser.parse("open settings"))
        assertEquals(VoiceCommand.Help, VoiceCommandParser.parse("show commands"))
    }

    @Test
    fun testHindiVoiceCommands() {
        assertEquals(VoiceCommand.StartVision, VoiceCommandParser.parse("vision shuru karo"))
        assertEquals(VoiceCommand.StopVision, VoiceCommandParser.parse("camera band karo"))
        assertEquals(VoiceCommand.QuerySurroundings, VoiceCommandParser.parse("samne kya hai"))
        assertEquals(VoiceCommand.ReadText, VoiceCommandParser.parse("isko padho"))
        assertEquals(VoiceCommand.WhereAmI, VoiceCommandParser.parse("main kahan hoon"))
        assertEquals(VoiceCommand.Emergency, VoiceCommandParser.parse("bachao"))
        assertEquals(VoiceCommand.StopSpeech, VoiceCommandParser.parse("chup ho jao"))
        assertEquals(VoiceCommand.RepeatSpeech, VoiceCommandParser.parse("phir se bolo"))
        assertEquals(VoiceCommand.OpenSettings, VoiceCommandParser.parse("settings kholo"))
    }

    @Test
    fun testDevanagariHindiCommands() {
        // Blind user speaking pure Hindi (Devanagari transcription from hi-IN ASR)
        assertEquals(VoiceCommand.StartVision, VoiceCommandParser.parse("कैमरा चालू करो"))
        assertEquals(VoiceCommand.StartVision, VoiceCommandParser.parse("कैमरा खोलो"))
        assertEquals(VoiceCommand.StartVision, VoiceCommandParser.parse("विजन शुरू करो"))
        assertEquals(VoiceCommand.StartVision, VoiceCommandParser.parse("कैमरा"))

        assertEquals(VoiceCommand.StopVision, VoiceCommandParser.parse("कैमरा बंद करो"))
        assertEquals(VoiceCommand.StopVision, VoiceCommandParser.parse("विजन बंद करो"))

        assertEquals(VoiceCommand.QuerySurroundings, VoiceCommandParser.parse("सामने क्या है"))
        assertEquals(VoiceCommand.IdentifyObject, VoiceCommandParser.parse("वस्तु पहचानो"))
        assertEquals(VoiceCommand.IdentifyObject, VoiceCommandParser.parse("यह कौन सी वस्तु है"))
        assertEquals(VoiceCommand.IdentifyObject, VoiceCommandParser.parse("वस्तु का नाम बताओ"))
        assertEquals(VoiceCommand.IdentifyObject, VoiceCommandParser.parse("चीज पहचानो"))
        assertEquals(VoiceCommand.QuerySurroundings, VoiceCommandParser.parse("आगे क्या है"))
        assertEquals(VoiceCommand.QuerySurroundings, VoiceCommandParser.parse("क्या दिख रहा है"))

        assertEquals(VoiceCommand.ReadText, VoiceCommandParser.parse("किताब पढ़ो"))
        assertEquals(VoiceCommand.ReadText, VoiceCommandParser.parse("लिखा हुआ पढ़ो"))
        assertEquals(VoiceCommand.ReadText, VoiceCommandParser.parse("क्या लिखा है"))
        assertEquals(VoiceCommand.ReadText, VoiceCommandParser.parse("पढ़ो"))

        assertEquals(VoiceCommand.WhereAmI, VoiceCommandParser.parse("मैं कहाँ हूँ"))
        assertEquals(VoiceCommand.WhereAmI, VoiceCommandParser.parse("मेरी लोकेशन"))
        assertEquals(VoiceCommand.WhereAmI, VoiceCommandParser.parse("रास्ता दिखाओ"))
        assertEquals(VoiceCommand.WhereAmI, VoiceCommandParser.parse("दिशा"))
        assertEquals(VoiceCommand.WhereAmI, VoiceCommandParser.parse("दिशा बताओ"))
        assertEquals(VoiceCommand.WhereAmI, VoiceCommandParser.parse("नेविगेशन"))
        assertEquals(VoiceCommand.WhereAmI, VoiceCommandParser.parse("disa"))
        assertEquals(VoiceCommand.WhereAmI, VoiceCommandParser.parse("disha"))
        assertEquals(VoiceCommand.WhereAmI, VoiceCommandParser.parse("disa batao"))
        assertEquals(VoiceCommand.WhereAmI, VoiceCommandParser.parse("navigation"))
        assertEquals(VoiceCommand.WhereAmI, VoiceCommandParser.parse("location"))

        assertEquals(VoiceCommand.Emergency, VoiceCommandParser.parse("मदद करो"))
        assertEquals(VoiceCommand.Emergency, VoiceCommandParser.parse("बचाओ"))
        assertEquals(VoiceCommand.Emergency, VoiceCommandParser.parse("आपातकाल"))

        assertEquals(VoiceCommand.CallEmergency, VoiceCommandParser.parse("112 पर कॉल करो"))
        assertEquals(VoiceCommand.CallEmergency, VoiceCommandParser.parse("इमरजेंसी कॉल"))

        val papaCall = VoiceCommandParser.parse("पापा को कॉल करो")
        assertTrue(papaCall is VoiceCommand.CallContact)
        assertEquals("पापा", (papaCall as VoiceCommand.CallContact).targetName)

        assertEquals(VoiceCommand.GoHome, VoiceCommandParser.parse("वापस जाओ"))
        assertEquals(VoiceCommand.GoHome, VoiceCommandParser.parse("डैशबोर्ड"))
        assertEquals(VoiceCommand.GoHome, VoiceCommandParser.parse("होम स्क्रीन"))

        assertEquals(VoiceCommand.CheckBattery, VoiceCommandParser.parse("बैटरी कितनी है"))
        assertEquals(VoiceCommand.CheckTime, VoiceCommandParser.parse("कितने बजे हैं"))
        assertEquals(VoiceCommand.CheckTime, VoiceCommandParser.parse("समय क्या है"))

        assertEquals(VoiceCommand.RepeatSpeech, VoiceCommandParser.parse("फिर से बोलो"))
        assertEquals(VoiceCommand.StopSpeech, VoiceCommandParser.parse("चुप हो जाओ"))
        assertEquals(VoiceCommand.OpenSettings, VoiceCommandParser.parse("सेटिंग्स खोलो"))
        assertEquals(VoiceCommand.Help, VoiceCommandParser.parse("मदद"))

        assertEquals(VoiceCommand.SetVisionRange(DetectionRangeLimit.STANDARD_10M), VoiceCommandParser.parse("10 मीटर"))
        assertEquals(VoiceCommand.SetVisionRange(DetectionRangeLimit.SHORT_5M), VoiceCommandParser.parse("5 मीटर"))
        assertEquals(VoiceCommand.SetVisionRange(DetectionRangeLimit.UNLIMITED), VoiceCommandParser.parse("सब दूरी"))
    }

    @Test
    fun testPunctuationAndColloquialSpeech() {
        assertEquals(VoiceCommand.QuerySurroundings, VoiceCommandParser.parse("सामने क्या है?"))
        assertEquals(VoiceCommand.ReadText, VoiceCommandParser.parse("किताब पढ़ो।"))
        assertEquals(VoiceCommand.StartVision, VoiceCommandParser.parse("Start vision!"))
        assertEquals(VoiceCommand.GoHome, VoiceCommandParser.parse("Home."))
        assertEquals(VoiceCommand.Emergency, VoiceCommandParser.parse("Help me!"))

        // Colloquial Hinglish variations
        assertEquals(VoiceCommand.StartVision, VoiceCommandParser.parse("camera chalu"))
        assertEquals(VoiceCommand.StartVision, VoiceCommandParser.parse("camera on karo"))
        assertEquals(VoiceCommand.QuerySurroundings, VoiceCommandParser.parse("samne kya h"))
        assertEquals(VoiceCommand.QuerySurroundings, VoiceCommandParser.parse("aage kya h"))
        assertEquals(VoiceCommand.ReadText, VoiceCommandParser.parse("kya likha h"))
        assertEquals(VoiceCommand.WhereAmI, VoiceCommandParser.parse("kahan hu"))
    }

    @Test
    fun testCandidatesParsing() {
        // First candidate is noisy/unrecognized, second candidate is recognized
        val cmd = VoiceCommandParser.parseCandidates(listOf("कुछ आवाज", "कैमरा चालू करो"))
        assertEquals(VoiceCommand.StartVision, cmd)

        val cmd2 = VoiceCommandParser.parseCandidates(listOf("noise123", "samne kya hai"))
        assertEquals(VoiceCommand.QuerySurroundings, cmd2)

        val unknownCmd = VoiceCommandParser.parseCandidates(listOf("random noise", "bla bla"))
        assertTrue(unknownCmd is VoiceCommand.Unknown)
    }

    @Test
    fun testNearbyFacilityParsing() {
        val cmd = VoiceCommandParser.parse("find nearest hospital")
        assertTrue(cmd is VoiceCommand.FindNearby)
        assertEquals("hospital", (cmd as VoiceCommand.FindNearby).placeType)

        val cmdHindi = VoiceCommandParser.parse("पास का अस्पताल")
        assertTrue(cmdHindi is VoiceCommand.FindNearby)
        assertEquals("hospital", (cmdHindi as VoiceCommand.FindNearby).placeType)
    }

    @Test
    fun testContactCallCommands() {
        // Various ways blind users say "phone lagao" / "call lagao"
        val cmd1 = VoiceCommandParser.parse("sonu ko phone lagao")
        assertTrue(cmd1 is VoiceCommand.CallContact)
        assertEquals("sonu", (cmd1 as VoiceCommand.CallContact).targetName)

        val cmd2 = VoiceCommandParser.parse("phone lagao sonu")
        assertTrue(cmd2 is VoiceCommand.CallContact)
        assertEquals("sonu", (cmd2 as VoiceCommand.CallContact).targetName)

        val cmd3 = VoiceCommandParser.parse("phone lagao sonu ko")
        assertTrue(cmd3 is VoiceCommand.CallContact)
        assertEquals("sonu", (cmd3 as VoiceCommand.CallContact).targetName)

        val cmd4 = VoiceCommandParser.parse("sonu phone lagao")
        assertTrue(cmd4 is VoiceCommand.CallContact)
        assertEquals("sonu", (cmd4 as VoiceCommand.CallContact).targetName)

        val cmd5 = VoiceCommandParser.parse("sonu ko call lagao")
        assertTrue(cmd5 is VoiceCommand.CallContact)
        assertEquals("sonu", (cmd5 as VoiceCommand.CallContact).targetName)

        val cmd6 = VoiceCommandParser.parse("sonu ko lagao")
        assertTrue(cmd6 is VoiceCommand.CallContact)
        assertEquals("sonu", (cmd6 as VoiceCommand.CallContact).targetName)

        val cmd7 = VoiceCommandParser.parse("call sonu")
        assertTrue(cmd7 is VoiceCommand.CallContact)
        assertEquals("sonu", (cmd7 as VoiceCommand.CallContact).targetName)

        // Devanagari Hindi commands
        val cmdHi1 = VoiceCommandParser.parse("सोनू को फोन लगाओ")
        assertTrue(cmdHi1 is VoiceCommand.CallContact)
        assertEquals("सोनू", (cmdHi1 as VoiceCommand.CallContact).targetName)

        val cmdHi2 = VoiceCommandParser.parse("फोन लगाओ सोनू")
        assertTrue(cmdHi2 is VoiceCommand.CallContact)
        assertEquals("सोनू", (cmdHi2 as VoiceCommand.CallContact).targetName)

        val cmdHi3 = VoiceCommandParser.parse("फोन लगाओ सोनू को")
        assertTrue(cmdHi3 is VoiceCommand.CallContact)
        assertEquals("सोनू", (cmdHi3 as VoiceCommand.CallContact).targetName)

        val cmdHi4 = VoiceCommandParser.parse("सोनू फोन लगाओ")
        assertTrue(cmdHi4 is VoiceCommand.CallContact)
        assertEquals("सोनू", (cmdHi4 as VoiceCommand.CallContact).targetName)

        val cmdHi5 = VoiceCommandParser.parse("सोनू को लगाओ")
        assertTrue(cmdHi5 is VoiceCommand.CallContact)
        assertEquals("सोनू", (cmdHi5 as VoiceCommand.CallContact).targetName)

        // Generic "phone lagao" without contact name triggers primary/default contact call
        val cmdGeneric = VoiceCommandParser.parse("phone lagao")
        assertTrue(cmdGeneric is VoiceCommand.CallContact)
        assertEquals("", (cmdGeneric as VoiceCommand.CallContact).targetName)

        val cmdGenericHi = VoiceCommandParser.parse("फोन लगाओ")
        assertTrue(cmdGenericHi is VoiceCommand.CallContact)
        assertEquals("", (cmdGenericHi as VoiceCommand.CallContact).targetName)
    }
}
