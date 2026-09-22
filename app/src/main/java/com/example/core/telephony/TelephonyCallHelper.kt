package com.example.core.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

data class SimCardInfo(
    val slotIndex: Int, // 0 for SIM 1, 1 for SIM 2
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String
)

data class CallerAppInfo(
    val packageName: String,
    val appName: String,
    val isDefaultDialer: Boolean
)

class TelephonyCallHelper(private val context: Context) {

    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    private val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

    /**
     * Retrieves all active physical / eSIM subscriptions on the device.
     */
    @SuppressLint("MissingPermission")
    fun getActiveSimCards(): List<SimCardInfo> {
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPhoneState) return emptyList()

        return try {
            val list: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList
            list?.map { sub ->
                val slot = sub.simSlotIndex
                val carrier = sub.carrierName?.toString()?.takeIf { it.isNotBlank() } ?: "SIM ${slot + 1}"
                val display = sub.displayName?.toString()?.takeIf { it.isNotBlank() } ?: carrier
                SimCardInfo(
                    slotIndex = slot,
                    subscriptionId = sub.subscriptionId,
                    displayName = display,
                    carrierName = carrier
                )
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Retrieves all installed caller / dialer applications on the device.
     */
    fun getAvailableCallerApps(): List<CallerAppInfo> {
        val pm = context.packageManager
        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:123"))
        val resolveList = pm.queryIntentActivities(callIntent, PackageManager.MATCH_DEFAULT_ONLY)

        val defaultDialer = telecomManager?.defaultDialerPackage
        val apps = mutableListOf<CallerAppInfo>()
        val seenPackages = mutableSetOf<String>()

        for (resolveInfo in resolveList) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg !in seenPackages) {
                seenPackages.add(pkg)
                val label = resolveInfo.loadLabel(pm).toString()
                apps.add(
                    CallerAppInfo(
                        packageName = pkg,
                        appName = label,
                        isDefaultDialer = (pkg == defaultDialer)
                    )
                )
            }
        }
        return apps
    }

    /**
     * Directly initiates a phone call without ambiguous visual popups,
     * targeting a specific SIM card slot and dialer package.
     */
    fun placeCall(
        phoneNumber: String,
        simSlot: Int? = null,
        targetPackage: String? = null
    ): Boolean {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val action = if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val cleanNumber = phoneNumber.trim()
        val intent = Intent(action).apply {
            data = Uri.parse("tel:${Uri.encode(cleanNumber)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Direct package specification avoids Android's "Open with..." dialog
        val pkgToUse = targetPackage ?: telecomManager?.defaultDialerPackage
        if (!pkgToUse.isNullOrBlank()) {
            intent.setPackage(pkgToUse)
        }

        // Direct SIM slot specification avoids Android's "Choose SIM" dialog
        if (simSlot != null) {
            intent.putExtra("com.android.phone.extra.slot", simSlot)
            intent.putExtra("simSlot", simSlot)
            intent.putExtra("android.telecom.extra.SLOT_INDEX", simSlot)

            try {
                @SuppressLint("MissingPermission")
                val callCapableAccounts: List<PhoneAccountHandle>? = telecomManager?.callCapablePhoneAccounts
                if (!callCapableAccounts.isNullOrEmpty()) {
                    val activeSims = getActiveSimCards()
                    val targetSim = activeSims.firstOrNull { it.slotIndex == simSlot }
                    val matchedAccount = callCapableAccounts.firstOrNull { handle ->
                        val idStr = handle.id ?: ""
                        (targetSim != null && idStr.contains("${targetSim.subscriptionId}")) ||
                                idStr.contains("$simSlot")
                    } ?: callCapableAccounts.getOrNull(simSlot)

                    if (matchedAccount != null) {
                        intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, matchedAccount)
                    }
                }
            } catch (_: Exception) {}
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            // Fallback: strip package restriction if that specific package failed
            try {
                intent.setPackage(null)
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
