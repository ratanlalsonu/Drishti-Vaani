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

class SpeechRecognizerManager(
    private val context: Context,
    private val onCommandReceived: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onErrorOccurred: (String) -> Unit
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

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("SpeechRecognizerManager", "Speech recognition not available on device")
            onErrorOccurred("Voice recognition service not available")
            return
        }

        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    onListeningStateChanged(true)
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    onListeningStateChanged(false)
                }

                override fun onError(error: Int) {
                    isListening = false
                    onListeningStateChanged(false)

                    val isSilenceTimeout = error == SpeechRecognizer.ERROR_NO_MATCH ||
                            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT

                    if (isSilenceTimeout) {
                        // Normal silence when user is not speaking; loop back seamlessly
                        scheduleRestart(350L)
                    } else if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                        try {
                            speechRecognizer?.cancel()
                        } catch (_: Exception) {}
                        scheduleRestart(600L)
                    } else {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_NETWORK -> "Network issue in voice recognition"
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission needed"
                            else -> "Recognition error: $error"
                        }
                        onErrorOccurred(message)
                        scheduleRestart(1200L)
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    onListeningStateChanged(false)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0]
                        onCommandReceived(spokenText)
                    }

                    // Loop back to auto-listening unless TTS takes over
                    scheduleRestart(500L)
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
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
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, localeTag)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }

            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("SpeechRecognizerManager", "Error starting listening", e)
                try {
                    speechRecognizer?.cancel()
                } catch (_: Exception) {}
                scheduleRestart(800L)
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
        }
    }

    fun resumeAfterTTS() {
        isPausedForTTS = false
        if (isAutoListening && !isDestroyed) {
            scheduleRestart(300L)
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
