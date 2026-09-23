package com.example.core.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.example.core.model.AssistantLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryInfo(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false,
    val isLow: Boolean = false,             // <= 15%
    val isCriticallyLow: Boolean = false,   // <= 10%
    val isEmergencyLow: Boolean = false     // <= 5%
)

/**
 * Monitors device battery level and charging state in real-time.
 * Automatically issues audible Text-to-Speech warnings when the battery
 * is critically low (15%, 10%, and 5% milestones) to protect visually impaired users
 * from sudden unexpected device shutdown.
 */
class BatteryStatusMonitor(
    private val context: Context,
    private val onCriticalBatteryWarning: (info: BatteryInfo, messageHi: String, messageEn: String) -> Unit,
    private val onBatteryStateChanged: (info: BatteryInfo) -> Unit = {}
) {

    private val _batteryState = MutableStateFlow(BatteryInfo())
    val batteryState: StateFlow<BatteryInfo> = _batteryState.asStateFlow()

    private var isRegistered = false
    private var lastWarnedMilestone = 100
    private var lastWarningTimestamp = 0L

    // Milestone thresholds
    private val milestoneLow = 15
    private val milestoneCritical = 10
    private val milestoneEmergency = 5

    // Minimum 4 minutes before repeating the exact same critical warning if still uncharged
    private val repeatWarningCooldownMs = 4 * 60 * 1000L

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED,
                Intent.ACTION_BATTERY_LOW,
                Intent.ACTION_BATTERY_OKAY,
                Intent.ACTION_POWER_CONNECTED,
                Intent.ACTION_POWER_DISCONNECTED -> {
                    processBatteryIntent(intent)
                }
            }
        }
    }

    fun start() {
        if (isRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        try {
            val initialIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(batteryReceiver, filter)
            }
            isRegistered = true
            initialIntent?.let { processBatteryIntent(it) }
        } catch (e: Exception) {
            Log.e("BatteryStatusMonitor", "Failed to register battery receiver", e)
        }
    }

    fun stop() {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
        isRegistered = false
    }

    private fun processBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val percent = if (level >= 0 && scale > 0) {
            ((level * 100) / scale)
        } else {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val isLow = percent <= milestoneLow && !isCharging
        val isCritical = percent <= milestoneCritical && !isCharging
        val isEmergency = percent <= milestoneEmergency && !isCharging

        val updatedInfo = BatteryInfo(
            levelPercent = percent,
            isCharging = isCharging,
            isLow = isLow,
            isCriticallyLow = isCritical,
            isEmergencyLow = isEmergency
        )

        _batteryState.value = updatedInfo
        onBatteryStateChanged(updatedInfo)

        evaluateBatteryWarnings(updatedInfo)
    }

    private fun evaluateBatteryWarnings(info: BatteryInfo) {
        val now = System.currentTimeMillis()

        // When charging, reset milestone tracking so warnings trigger anew when unplugged later
        if (info.isCharging) {
            if (lastWarnedMilestone < 100) {
                lastWarnedMilestone = 100
            }
            return
        }

        val percent = info.levelPercent

        // Tier 3: Emergency <= 5%
        if (percent <= milestoneEmergency) {
            if (lastWarnedMilestone > milestoneEmergency || (now - lastWarningTimestamp > repeatWarningCooldownMs)) {
                lastWarnedMilestone = milestoneEmergency
                lastWarningTimestamp = now
                val hi = "आपातकालीन चेतावनी! बैटरी केवल $percent% बची है। फोन किसी भी समय बंद हो सकता है, कृपया तुरंत चार्जर से जोड़ें!"
                val en = "Emergency warning! Battery is at $percent%. The device will shut down very soon, please connect your charger immediately!"
                onCriticalBatteryWarning(info, hi, en)
            }
        }
        // Tier 2: Critically Low <= 10%
        else if (percent <= milestoneCritical) {
            if (lastWarnedMilestone > milestoneCritical || (now - lastWarningTimestamp > repeatWarningCooldownMs)) {
                lastWarnedMilestone = milestoneCritical
                lastWarningTimestamp = now
                val hi = "सावधान! फोन की बैटरी बहुत कम है, केवल $percent% बची है। कृपया तुरंत फोन को चार्ज पर लगाएं।"
                val en = "Warning! Battery is critically low, only $percent% remaining. Please plug in your charger now."
                onCriticalBatteryWarning(info, hi, en)
            }
        }
        // Tier 1: Low <= 15%
        else if (percent <= milestoneLow) {
            if (lastWarnedMilestone > milestoneLow) {
                lastWarnedMilestone = milestoneLow
                lastWarningTimestamp = now
                val hi = "सूचना: फोन की बैटरी कम हो रही है, $percent% बची है। कृपया चार्जर तैयार रखें।"
                val en = "Notice: Battery is getting low, $percent% remaining. Please have your charger ready."
                onCriticalBatteryWarning(info, hi, en)
            }
        } else {
            // Above 15% - reset milestone tracker
            lastWarnedMilestone = 100
        }
    }

    /**
     * Returns a spoken description of the battery state for voice commands ("battery kitni hai?").
     */
    fun getBatteryStatusDescription(language: AssistantLanguage): String {
        val info = _batteryState.value
        val percent = info.levelPercent

        return if (language == AssistantLanguage.HINDI) {
            when {
                info.isCharging && percent >= 98 -> "फोन चार्जर से जुड़ा है और बैटरी पूरी तरह चार्ज है (100%)।"
                info.isCharging -> "फोन चार्ज हो रहा है, बैटरी $percent% है।"
                info.isEmergencyLow -> "बैटरी आपातकालीन स्तर पर है, केवल $percent% बची है! कृपया तुरंत चार्ज करें।"
                info.isCriticallyLow -> "बैटरी बहुत कम है, केवल $percent% बची है। कृपया चार्जर से जोड़ें।"
                info.isLow -> "बैटरी $percent% है, कुछ देर में चार्ज करना होगा।"
                else -> "फोन की बैटरी $percent% है।"
            }
        } else {
            when {
                info.isCharging && percent >= 98 -> "Phone is plugged in and fully charged (100%)."
                info.isCharging -> "Phone is currently charging, battery is at $percent%."
                info.isEmergencyLow -> "Emergency! Battery is only $percent%! Please connect your charger immediately."
                info.isCriticallyLow -> "Battery is critically low at $percent%. Please plug in your charger."
                info.isLow -> "Battery is at $percent%, please charge soon."
                else -> "Phone battery is at $percent%."
            }
        }
    }

    /**
     * Allows simulated test events for Robolectric testing and verification.
     */
    fun simulateBatteryState(percent: Int, isCharging: Boolean) {
        val isLow = percent <= milestoneLow && !isCharging
        val isCritical = percent <= milestoneCritical && !isCharging
        val isEmergency = percent <= milestoneEmergency && !isCharging

        val info = BatteryInfo(
            levelPercent = percent,
            isCharging = isCharging,
            isLow = isLow,
            isCriticallyLow = isCritical,
            isEmergencyLow = isEmergency
        )
        _batteryState.value = info
        onBatteryStateChanged(info)
        evaluateBatteryWarnings(info)
    }
}
