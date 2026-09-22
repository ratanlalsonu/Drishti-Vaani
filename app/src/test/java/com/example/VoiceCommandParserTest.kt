package com.example

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
    fun testNearbyFacilityParsing() {
        val cmd = VoiceCommandParser.parse("find nearest hospital")
        assertTrue(cmd is VoiceCommand.FindNearby)
        assertEquals("hospital", (cmd as VoiceCommand.FindNearby).placeType)
    }
}
