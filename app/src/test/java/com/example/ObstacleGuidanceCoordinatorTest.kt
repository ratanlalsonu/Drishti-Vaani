package com.example

import android.graphics.RectF
import com.example.core.model.AssistantLanguage
import com.example.core.model.DetectedObject
import com.example.core.model.ObjectCategory
import com.example.core.model.PriorityLevel
import com.example.core.model.ProximityTier
import com.example.core.model.SpatialPosition
import com.example.feature.vision.ObstacleGuidanceCoordinator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ObstacleGuidanceCoordinatorTest {

    private lateinit var coordinator: ObstacleGuidanceCoordinator

    @Before
    fun setup() {
        coordinator = ObstacleGuidanceCoordinator()
    }

    private fun createObstacle(
        label: String,
        hindiLabel: String,
        position: SpatialPosition,
        distanceFeet: Int
    ): DetectedObject {
        val meters = distanceFeet / 3.28f
        return DetectedObject(
            label = label,
            hindiLabel = hindiLabel,
            confidence = 0.90f,
            boundingBox = RectF(0f, 0f, 100f, 100f),
            position = position,
            proximity = if (distanceFeet <= 4) ProximityTier.VERY_CLOSE else ProximityTier.NEARBY,
            priority = if (distanceFeet <= 3) PriorityLevel.CRITICAL else PriorityLevel.HIGH,
            estimatedDistanceMeters = meters,
            distanceDescriptionHi = "$distanceFeet फीट",
            distanceDescriptionEn = "$distanceFeet feet",
            category = ObjectCategory.FURNITURE,
            distanceFeet = distanceFeet,
            evasionAdviceEn = "Please move aside by 2 to 3 feet to your right to avoid collision.",
            evasionAdviceHi = "टकराने से बचने के लिए कृपया 2 से 3 फीट दाईं ओर हटकर आगे बढ़ें।"
        )
    }

    @Test
    fun testClearPathAnnouncementWhenNoObstacles() {
        // When path is clear and no obstacles detected, announcement must occur ONCE as per Requirement 10
        val result1 = coordinator.processDetections(emptyList(), AssistantLanguage.HINDI)
        assertNotNull(result1)
        assertTrue(result1!!.speech.contains("रास्ता आगे एकदम क्लियर है, आप सीधे चल सकते हैं"))
        assertTrue(result1.speech.contains("जैसे ही कोई वस्तु बीच में आती है मैं आपको वार्न करुँगी"))

        // Requirement 5 & 6: After announcing once, it MUST remain completely silent
        val result2 = coordinator.processDetections(emptyList(), AssistantLanguage.HINDI)
        assertNull(result2)

        val result3 = coordinator.processDetections(emptyList(), AssistantLanguage.HINDI)
        assertNull(result3)
    }

    @Test
    fun testChairDetectionAndGuidance() {
        val chair = createObstacle("Chair", "कुर्सी", SpatialPosition.CENTER, distanceFeet = 3)
        val result = coordinator.processDetections(listOf(chair), AssistantLanguage.HINDI)

        assertNotNull(result)
        // Warning contains chair, 3 feet distance, and 2-3 feet evasion guidance
        assertTrue(result!!.speech.contains("कुर्सी"))
        assertTrue(result.speech.contains("3 फीट"))
        assertTrue(result.speech.contains("2 से 3 फीट"))
    }

    @Test
    fun testWarnOnlyTwiceAndSilenceAfterSecondWarning() {
        val chair = createObstacle("Chair", "कुर्सी", SpatialPosition.CENTER, distanceFeet = 3)

        // Warning 1
        val warning1 = coordinator.processDetections(listOf(chair), AssistantLanguage.HINDI)
        assertNotNull(warning1)
        assertEquals(1, coordinator.warningCount)

        // Immediate subsequent frame within interval -> Silent
        val immediateNext = coordinator.processDetections(listOf(chair), AssistantLanguage.HINDI)
        assertNull(immediateNext)

        // Simulate time passing past interval
        Thread.sleep(4100)

        // Warning 2 (Reminder)
        val warning2 = coordinator.processDetections(listOf(chair), AssistantLanguage.HINDI)
        assertNotNull(warning2)
        assertEquals(2, coordinator.warningCount)
        assertTrue(warning2!!.speech.contains("ध्यान दें"))

        // After Warning 2: Must remain silent while watching user navigate! (Requirements 2 & 6)
        Thread.sleep(100)
        val postWarning2Frame = coordinator.processDetections(listOf(chair), AssistantLanguage.HINDI)
        assertNull(postWarning2Frame)

        Thread.sleep(4500)
        val postWarning2FrameLater = coordinator.processDetections(listOf(chair), AssistantLanguage.HINDI)
        assertNull(postWarning2FrameLater)
    }

    @Test
    fun testWatchUntilClearedObstacleAndConfirmClearPath() {
        val table = createObstacle("Table", "मेज़", SpatialPosition.LEFT, distanceFeet = 4)

        // User receives warning about table
        val warning = coordinator.processDetections(listOf(table), AssistantLanguage.HINDI)
        assertNotNull(warning)
        assertTrue(coordinator.isWatchingObstacle)

        // User moves past table: camera detects clear path for 2 frames
        val clearFrame1 = coordinator.processDetections(emptyList(), AssistantLanguage.HINDI)
        assertNull(clearFrame1) // Debouncing frame 1

        val clearFrame2 = coordinator.processDetections(emptyList(), AssistantLanguage.HINDI)
        assertNotNull(clearFrame2) // Cleared confirmation!
        assertTrue(clearFrame2!!.speech.contains("बाधा पार कर चुके हैं"))
        assertTrue(clearFrame2.speech.contains("रास्ता आगे एकदम क्लियर है"))

        // Stays quiet thereafter
        val clearFrame3 = coordinator.processDetections(emptyList(), AssistantLanguage.HINDI)
        assertNull(clearFrame3)
    }

    @Test
    fun testSimultaneousMultipleObstaclesOneByOneWarning() {
        val chair = createObstacle("Chair", "कुर्सी", SpatialPosition.CENTER, distanceFeet = 3)
        val table = createObstacle("Table", "मेज़", SpatialPosition.LEFT, distanceFeet = 4)
        val pillar = createObstacle("Pillar", "खंभा", SpatialPosition.RIGHT, distanceFeet = 6)

        // All 3 obstacles detected together
        val speech = coordinator.buildInitialWarningSpeech(listOf(chair, table, pillar), AssistantLanguage.HINDI)

        // Requirement 8: Warn about each one one by one
        assertTrue(speech.contains("कुर्सी"))
        assertTrue(speech.contains("मेज़"))
        assertTrue(speech.contains("खंभा"))
        assertTrue(speech.contains("3 फीट"))
        assertTrue(speech.contains("4 फीट"))
        assertTrue(speech.contains("6 फीट"))

        // Recommends safe corridor with 2-3 feet guidance
        assertTrue(speech.contains("2 से 3 फीट"))
    }
}
