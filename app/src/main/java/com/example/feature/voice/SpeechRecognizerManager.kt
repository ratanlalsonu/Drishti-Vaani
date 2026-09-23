package com.example.feature.voice

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.core.model.AssistantLanguage
import kotlin.math.max
import kotlin.math.min

class SpeechRecognizerManager(
    private val context: Context,
    private val onCommandReceived: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onErrorOccurred: (String) -> Unit,
    private val onCandidatesReceived: ((List<String>) -> Unit)? = null,
    private val onPartialResultReceived: ((String) -> Unit)? = null,
    private val onAudioLevelChanged: ((Float) -> Unit)? = null
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var isListening = false

    @Volatile
    var isAutoListening = true // Continuous auto-listening enabled by default for blind users

    @Volatile
    private var isPausedForTTS = false

    private var currentLanguage = AssistantLanguage.HINDI
    private var isDestroyed = false
    private var consecutiveErrors = 0

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("SpeechRecognizerManager", "Speech recognition service not available on device")
            onErrorOccurred("Voice recognition service not available")
            return
        }

        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        consecutiveErrors = 0
                        onListeningStateChanged(true)
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize -2dB..10dB to 0.0f..1.0f range for audio level pulse UI
                        val normalized = max(0f, min(1f, (rmsdB + 2f) / 12f))
                        onAudioLevelChanged?.invoke(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                        onListeningStateChanged(false)
                        onAudioLevelChanged?.invoke(0f)
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        onListeningStateChanged(false)
                        onAudioLevelChanged?.invoke(0f)

                        val isSilenceTimeout = error == SpeechRecognizer.ERROR_NO_MATCH ||
                                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT

                        if (isSilenceTimeout) {
                            // User did not speak; loop back smoothly
                            consecutiveErrors = 0
                            scheduleRestart(300L)
                        } else {
                            consecutiveErrors++
                            if (consecutiveErrors >= 3 || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                                // Reset recognizer instance if OS service is wedged
                                recreateRecognizer()
                                scheduleRestart(600L)
                            } else {
                                val message = when (error) {
                                    SpeechRecognizer.ERROR_NETWORK -> "Network issue in voice recognition"
                                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission needed"
                                    else -> "Recognition issue: $error"
                                }
                                onErrorOccurred(message)
                                scheduleRestart(1000L)
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        consecutiveErrors = 0
                        onListeningStateChanged(false)
                        onAudioLevelChanged?.invoke(0f)

                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            if (onCandidatesReceived != null) {
                                onCandidatesReceived.invoke(matches)
                            } else {
                                onCommandReceived(matches[0])
                            }
                        }

                        // Loop back to continuous auto-listening unless TTS is active
                        scheduleRestart(400L)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!partials.isNullOrEmpty()) {
                            val text = partials.firstOrNull()?.trim() ?: ""
                            if (text.isNotEmpty()) {
                                onPartialResultReceived?.invoke(text)
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } catch (e: Exception) {
            Log.e("SpeechRecognizerManager", "Failed to create SpeechRecognizer", e)
        }
    }

    private fun recreateRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        initRecognizer()
        consecutiveErrors = 0
    }

    private fun scheduleRestart(delayMs: Long = 400L) {
        if (!isAutoListening || isPausedForTTS || isDestroyed) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isAutoListening && !isPausedForTTS && !isDestroyed && !isListening) {
                startListening()
            }
        }, delayMs)
    }

    fun startListening() {
        if (isDestroyed || isPausedForTTS) return

        if (!hasRecordAudioPermission()) {
            return
        }

        mainHandler.post {
            if (speechRecognizer == null) {
                initRecognizer()
            }

            val localeTag = when (currentLanguage) {
                AssistantLanguage.HINDI -> "hi-IN"
                AssistantLanguage.ENGLISH_IN -> "en-IN"
                AssistantLanguage.ENGLISH_US -> "en-US"
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                // Add additional multi-language fallback tags so bilingual Hindi/English speech is detected
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US"))
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

                // Tuning speech silence and length to prevent premature cut-off when blind user pauses
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1400L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1100L)
            }

            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("SpeechRecognizerManager", "Error starting listening", e)
                try {
                    speechRecognizer?.cancel()
                } catch (_: Exception) {}
                recreateRecognizer()
                scheduleRestart(600L)
            }
        }
    }

    fun stopListening() {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e("SpeechRecognizerManager", "Error stopping listening", e)
            }
            isListening = false
            onListeningStateChanged(false)
            onAudioLevelChanged?.invoke(0f)
        }
    }

    fun pauseForTTS() {
        isPausedForTTS = true
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            isListening = false
            onListeningStateChanged(false)
            onAudioLevelChanged?.invoke(0f)
        }
    }

    fun resumeAfterTTS() {
        isPausedForTTS = false
        if (isAutoListening && !isDestroyed) {
            scheduleRestart(250L)
        }
    }

    fun startAutoListening() {
        isAutoListening = true
        if (!isPausedForTTS) {
            startListening()
        }
    }

    fun stopAutoListening() {
        isAutoListening = false
        stopListening()
    }

    fun setLanguage(language: AssistantLanguage) {
        currentLanguage = language
        if (isListening) {
            stopListening()
            scheduleRestart(200L)
        }
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun destroy() {
        isDestroyed = true
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
    }
}
