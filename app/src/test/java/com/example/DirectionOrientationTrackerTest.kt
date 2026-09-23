package com.example

import com.example.feature.vision.DirectionOrientationTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectionOrientationTrackerTest {

    @Test
    fun testAngleDifferenceCalculation() {
        assertEquals(30f, DirectionOrientationTracker.calculateAngleDifference(0f, 30f), 0.01f)
        assertEquals(30f, DirectionOrientationTracker.calculateAngleDifference(30f, 0f), 0.01f)
        // Wrap around test across 0/360 degrees
        assertEquals(20f, DirectionOrientationTracker.calculateAngleDifference(350f, 10f), 0.01f)
        assertEquals(20f, DirectionOrientationTracker.calculateAngleDifference(10f, 350f), 0.01f)
        assertEquals(90f, DirectionOrientationTracker.calculateAngleDifference(90f, 180f), 0.01f)
    }

    @Test
    fun testDirectionNames() {
        val (northHi, northEn) = DirectionOrientationTracker.getDirectionNames(0f)
        assertTrue(northHi.contains("उत्तर"))
        assertTrue(northEn.contains("North"))

        val (eastHi, eastEn) = DirectionOrientationTracker.getDirectionNames(90f)
        assertTrue(eastHi.contains("पूर्व"))
        assertTrue(eastEn.contains("East"))

        val (southHi, southEn) = DirectionOrientationTracker.getDirectionNames(180f)
        assertTrue(southHi.contains("दक्षिण"))
        assertTrue(southEn.contains("South"))

        val (westHi, westEn) = DirectionOrientationTracker.getDirectionNames(270f)
        assertTrue(westHi.contains("पश्चिम"))
        assertTrue(westEn.contains("West"))
    }
}
