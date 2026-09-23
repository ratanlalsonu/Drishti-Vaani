package com.example.core.ai

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class GeminiConfigManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("gemini_ai_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
    }

    fun getEffectiveApiKey(): String {
        val customKey = prefs.getString(KEY_CUSTOM_API_KEY, "")?.trim().orEmpty()
        if (customKey.isNotBlank() && customKey != "your_api_key_here" && customKey != "DEFAULT_API_KEY") {
            return customKey
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
        if (buildKey.isNotBlank() && buildKey != "your_api_key_here" && buildKey != "DEFAULT_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    fun clearCustomApiKey() {
        prefs.edit().remove(KEY_CUSTOM_API_KEY).apply()
    }

    fun getCustomApiKey(): String {
        return prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
    }

    fun isConfigured(): Boolean {
        return getEffectiveApiKey().isNotBlank()
    }
}
