package com.example.feature.voice

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.core.model.AssistantLanguage
import com.example.core.model.PriorityLevel
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

class TTSManager(context: Context, private val onInitCompleted: (Boolean) -> Unit = {}) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentLanguage = AssistantLanguage.HINDI
    private var speechRate = 1.20f
    private var speechPitch = 1.0f
    private var lastSpokenText: String = ""

    private val speechQueue = ConcurrentLinkedQueue<SpeechItem>()
    private var isSpeaking = false

    var onSpeakingStateChanged: ((Boolean) -> Unit)? = null

    fun isCurrentlySpeaking(): Boolean = isSpeaking

    data class SpeechItem(
        val utteranceId: String,
        val text: String,
        val priority: PriorityLevel,
        val panLeftRight: Float = 0.0f
    )

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                applyLanguage(currentLanguage)
                tts?.setSpeechRate(speechRate)
                tts?.setPitch(speechPitch)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)

                setupUtteranceListener()
                onInitCompleted(true)
            } else {
                Log.e("TTSManager", "Failed to initialize Android TextToSpeech")
                onInitCompleted(false)
            }
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                onSpeakingStateChanged?.invoke(true)
            }

            override fun onDone(utteranceId: String?) {
                if (speechQueue.isEmpty()) {
                    isSpeaking = false
                    onSpeakingStateChanged?.invoke(false)
                } else {
                    processNextInQueue()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (speechQueue.isEmpty()) {
                    isSpeaking = false
                    onSpeakingStateChanged?.invoke(false)
                } else {
                    processNextInQueue()
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (speechQueue.isEmpty()) {
                    isSpeaking = false
                    onSpeakingStateChanged?.invoke(false)
                } else {
                    processNextInQueue()
                }
            }
        })
    }

    /**
     * Speaks immediately with zero queue latency by clearing outdated announcements
     * and flushing the engine. Essential for real-time camera obstacle detection.
     */
    fun speakImmediate(text: String, priority: PriorityLevel = PriorityLevel.HIGH, pan: Float = 0.0f) {
        if (!isInitialized || text.isBlank()) return
        lastSpokenText = text

        val item = SpeechItem(
            utteranceId = "utt_${System.currentTimeMillis()}",
            text = text,
            priority = priority,
            panLeftRight = pan
        )

        tts?.stop()
        speechQueue.clear()
        executeSpeech(item, queueMode = TextToSpeech.QUEUE_FLUSH)
    }

    fun speak(text: String, priority: PriorityLevel = PriorityLevel.NORMAL, pan: Float = 0.0f) {
        if (!isInitialized || text.isBlank()) return
        lastSpokenText = text

        val item = SpeechItem(
            utteranceId = "utt_${System.currentTimeMillis()}",
            text = text,
            priority = priority,
            panLeftRight = pan
        )

        if (priority == PriorityLevel.CRITICAL) {
            // Critical priority interrupts any ongoing low/medium speech immediately
            tts?.stop()
            speechQueue.clear()
            executeSpeech(item, queueMode = TextToSpeech.QUEUE_FLUSH)
        } else if (priority == PriorityLevel.HIGH) {
            if (!isSpeaking) {
                executeSpeech(item, queueMode = TextToSpeech.QUEUE_ADD)
            } else {
                speechQueue.add(item)
            }
        } else {
            speechQueue.add(item)
            if (!isSpeaking) {
                processNextInQueue()
            }
        }
    }

    private fun processNextInQueue() {
        val nextItem = speechQueue.poll() ?: return
        executeSpeech(nextItem, queueMode = TextToSpeech.QUEUE_ADD)
    }

    private fun executeSpeech(item: SpeechItem, queueMode: Int) {
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, item.panLeftRight)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        tts?.speak(item.text, queueMode, params, item.utteranceId)
    }

    fun repeatLast() {
        if (lastSpokenText.isNotBlank()) {
            speak(lastSpokenText, PriorityLevel.HIGH)
        }
    }

    fun stopSpeaking() {
        speechQueue.clear()
        tts?.stop()
        isSpeaking = false
        onSpeakingStateChanged?.invoke(false)
    }

    fun setLanguage(language: AssistantLanguage) {
        currentLanguage = language
        applyLanguage(language)
    }

    private fun applyLanguage(language: AssistantLanguage) {
        val locale = when (language) {
            AssistantLanguage.HINDI -> Locale.forLanguageTag("hi-IN")
            AssistantLanguage.ENGLISH_IN -> Locale.forLanguageTag("en-IN")
            AssistantLanguage.ENGLISH_US -> Locale.US
        }
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to English US if regional language voice is not pre-installed
            tts?.setLanguage(Locale.US)
        }
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(speechRate)
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
