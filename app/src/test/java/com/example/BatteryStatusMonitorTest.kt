package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.battery.BatteryInfo
import com.example.core.battery.BatteryStatusMonitor
import com.example.core.model.AssistantLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatteryStatusMonitorTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testCriticalBatteryWarningTriggeredAtThresholds() {
        val warningsReceived = mutableListOf<String>()
        var lastInfo: BatteryInfo? = null

        val monitor = BatteryStatusMonitor(
            context = context,
            onCriticalBatteryWarning = { info, hiMsg, _ ->
                lastInfo = info
                warningsReceived.add(hiMsg)
            }
        )

        // 1. Normal battery (75%) - No warning
        monitor.simulateBatteryState(percent = 75, isCharging = false)
        assertEquals(0, warningsReceived.size)
        assertFalse(monitor.batteryState.value.isLow)
        assertFalse(monitor.batteryState.value.isCriticallyLow)

        // 2. Battery drops to 15% - Low Warning triggered
        monitor.simulateBatteryState(percent = 15, isCharging = false)
        assertEquals(1, warningsReceived.size)
        assertTrue(warningsReceived[0].contains("15%"))
        assertTrue(monitor.batteryState.value.isLow)
        assertFalse(monitor.batteryState.value.isCriticallyLow)

        // 3. Battery drops to 10% - Critical Warning triggered
        monitor.simulateBatteryState(percent = 10, isCharging = false)
        assertEquals(2, warningsReceived.size)
        assertTrue(warningsReceived[1].contains("10%"))
        assertTrue(monitor.batteryState.value.isCriticallyLow)

        // 4. Battery drops to 4% - Emergency Warning triggered
        monitor.simulateBatteryState(percent = 4, isCharging = false)
        assertEquals(3, warningsReceived.size)
        assertTrue(warningsReceived[2].contains("4%"))
        assertTrue(monitor.batteryState.value.isEmergencyLow)

        // 5. Charger plugged in - Warnings cease and milestone resets
        monitor.simulateBatteryState(percent = 6, isCharging = true)
        assertEquals(3, warningsReceived.size) // No extra warning while charging
        assertTrue(monitor.batteryState.value.isCharging)
    }

    @Test
    fun testBatteryStatusDescriptionFormatting() {
        val monitor = BatteryStatusMonitor(
            context = context,
            onCriticalBatteryWarning = { _, _, _ -> }
        )

        monitor.simulateBatteryState(percent = 85, isCharging = false)
        val descHi = monitor.getBatteryStatusDescription(AssistantLanguage.HINDI)
        val descEn = monitor.getBatteryStatusDescription(AssistantLanguage.ENGLISH_IN)
        assertTrue(descHi.contains("85%"))
        assertTrue(descEn.contains("85%"))

        monitor.simulateBatteryState(percent = 45, isCharging = true)
        val chargingHi = monitor.getBatteryStatusDescription(AssistantLanguage.HINDI)
        val chargingEn = monitor.getBatteryStatusDescription(AssistantLanguage.ENGLISH_IN)
        assertTrue(chargingHi.contains("चार्ज"))
        assertTrue(chargingEn.contains("charging"))

        monitor.simulateBatteryState(percent = 8, isCharging = false)
        val critHi = monitor.getBatteryStatusDescription(AssistantLanguage.HINDI)
        assertTrue(critHi.contains("कम") || critHi.contains("चार्ज"))
    }
}
