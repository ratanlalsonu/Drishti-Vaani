package com.example.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.core.model.AssistantLanguage
import com.example.core.model.YoloObjectTaxonomy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class AiAnalysisResult(
    val answer: String,
    val isFromGemini: Boolean,
    val mode: AiVisionMode,
    val detectedText: String = "",
    val errorMessage: String? = null
)

class GeminiVisionService(private val context: Context) {

    private val configManager = GeminiConfigManager(context)

    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(0.40f).build()
    )
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Per Gemini API Skill instructions: 60-second timeouts to avoid premature drop
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyze(
        bitmap: Bitmap?,
        mode: AiVisionMode,
        userCustomQuery: String = "",
        language: AssistantLanguage = AssistantLanguage.HINDI
    ): AiAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = configManager.getEffectiveApiKey()

        if (apiKey.isNotBlank()) {
            try {
                val answer = callGemini35Flash(bitmap, mode, userCustomQuery, language, apiKey)
                if (!answer.isNullOrBlank()) {
                    return@withContext AiAnalysisResult(
                        answer = cleanForTTS(answer),
                        isFromGemini = true,
                        mode = mode
                    )
                }
            } catch (e: Exception) {
                Log.w("GeminiVisionService", "Gemini API call failed, using fallback", e)
            }
        }

        // On-Device ML Fallback (Works 100% offline)
        if (bitmap != null) {
            return@withContext runOnDeviceFallback(bitmap, mode, language)
        } else {
            val noKeyMsg = if (language == AssistantLanguage.HINDI) {
                "AI सेवा से संपर्क नहीं हो पाया। कृपया इंटरनेट या सेटिंग्स में Gemini API Key की जांच करें।"
            } else {
                "Could not contact AI service. Please check internet connection or Gemini API key in Settings."
            }
            return@withContext AiAnalysisResult(
                answer = noKeyMsg,
                isFromGemini = false,
                mode = mode,
                errorMessage = "No key or offline"
            )
        }
    }

    private fun callGemini35Flash(
        bitmap: Bitmap?,
        mode: AiVisionMode,
        customQuery: String,
        language: AssistantLanguage,
        apiKey: String
    ): String? {
        val prompt = buildPrompt(mode, customQuery, language)
        val systemInstruction = if (language == AssistantLanguage.HINDI) {
            "आप दृष्टिबाधित (Blind) व्यक्ति के लिए 'दृष्टि-वाणी' एआई सहायक हैं। " +
            "चित्र को ध्यान से समझें और सामने उपस्थित किसी भी इंसान (व्यक्ति/महिला/बच्चे), सजीव प्राणी (कुत्ता, बिल्ली, अन्य जानवर, पक्षी), पौधे या वस्तुओं को पहचानकर 2-3 बहुत छोटे, स्पष्ट, और सरल वाक्यों में उत्तर दें। " +
            "दिशा (बाईं ओर, दाईं ओर, सामने) और दूरी का उल्लेख करें। " +
            "बोल्ड (*), बुलेट या मार्कडाउन सिंबल का उपयोग न करें क्योंकि टेक्स्ट-टू-स्पीच इसे पढ़ता है।"
        } else {
            "You are Drishti-Vaani AI assistant for a blind person. " +
            "Analyze the image carefully and identify any living humans (person/child/adult), animals/pets (dog, cat, birds), plants, or objects present in 2-3 short, clear, natural sentences. " +
            "Mention directions (left, right, ahead) and safety hazards. " +
            "Do NOT use markdown symbols, asterisks, or bullet points because TTS speaks them aloud."
        }

        val jsonRequest = JSONObject().apply {
            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", "$systemInstruction\n\nअनुरोध: $prompt"))
                        if (bitmap != null) {
                            val scaled = scaleBitmap(bitmap, maxDimension = 1024)
                            val base64 = bitmapToBase64(scaled)
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64)
                                })
                            })
                        }
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.25)
                put("maxOutputTokens", 350)
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string().orEmpty()
                Log.e("GeminiVisionService", "API error ${response.code}: $errBody")
                return null
            }
            val body = response.body?.string() ?: return null
            val root = JSONObject(body)
            val candidates = root.optJSONArray("candidates") ?: return null
            val firstCandidate = candidates.optJSONObject(0) ?: return null
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            val firstPart = parts.optJSONObject(0) ?: return null
            return firstPart.optString("text", "")
        }
    }

    private fun buildPrompt(mode: AiVisionMode, customQuery: String, language: AssistantLanguage): String {
        val isHindi = language == AssistantLanguage.HINDI
        return when (mode) {
            AiVisionMode.SCENE -> {
                if (isHindi) {
                    "इस पूरे दृश्य का विस्तृत और व्यावहारिक विवरण दें: क्या सामने कोई व्यक्ति/इंसान, कुत्ता, बिल्ली, जानवर, पक्षी या पौधा उपस्थित है? कमरे या रास्ते में क्या-क्या वस्तुएं हैं, रास्ता साफ है या कोई रुकावट है, और कौन सी चीज किस दिशा में है।"
                } else {
                    "Provide a detailed description of the scene: identify any people, dogs, cats, pets, animals, birds, or plants present. State what objects/furniture are around, whether the path is clear or obstructed, and what is located left, right, and center."
                }
            }
            AiVisionMode.CURRENCY -> {
                if (isHindi) {
                    "सामने रखे भारतीय नोट (Currency note) या सिक्के को पहचानें। इसका मूल्य (जैसे 10, 20, 50, 100, 200, 500 रुपये) स्पष्ट बताएं। यदि नोट आंशिक या मुड़ा हुआ है, तो बताएं।"
                } else {
                    "Identify the Indian Rupee currency note or coin. State the exact denomination (e.g. 10, 20, 50, 100, 200, 500 Rupees) and whether it is clearly visible."
                }
            }
            AiVisionMode.MEDICINE -> {
                if (isHindi) {
                    "सामने रखी दवाई (पट्टी, बोतल या डिब्बा) को पढ़ें। 1) दवाई का मुख्य नाम और क्षमता (mg/ml), 2) इसका सामान्य उपयोग, 3) एक्सपायरी डेट (Expiry Date) यदि दिख रही हो। यदि डेट नहीं दिख रही तो बताएं।"
                } else {
                    "Inspect the medicine package or bottle: 1) State the medicine name and dosage/strength, 2) Common indication/purpose, 3) Expiry date if visible."
                }
            }
            AiVisionMode.COLOR -> {
                if (isHindi) {
                    "सामने रखे कपड़ों या वस्तु का मुख्य रंग और पैटर्न बताएं (जैसे गहरा नीला, सफेद धारियां)। क्या यह रंग दिन या रात के हिसाब से उपयुक्त है।"
                } else {
                    "Identify the primary color, pattern (e.g. solid, striped, floral), and fabric appearance of the clothing or item in view."
                }
            }
            AiVisionMode.CUSTOM -> {
                if (customQuery.isNotBlank()) {
                    if (isHindi) "उपयोगकर्ता का सवाल: '$customQuery'। इस तस्वीर को देखकर इसका सीधा और उपयोगी उत्तर दें।"
                    else "User's question: '$customQuery'. Answer directly based on what is in the camera view."
                } else {
                    if (isHindi) "सामने क्या है, विस्तार से बताएं।" else "Describe what is right in front of the camera."
                }
            }
        }
    }

    private suspend fun runOnDeviceFallback(
        bitmap: Bitmap,
        mode: AiVisionMode,
        language: AssistantLanguage
    ): AiAnalysisResult {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val isHindi = language == AssistantLanguage.HINDI

        // Run OCR
        val text = try {
            textRecognizer.process(inputImage).await().text.trim()
        } catch (_: Exception) {
            ""
        }

        // Run ML Kit Labels
        val labels = try {
            imageLabeler.process(inputImage).await()
                .sortedByDescending { it.confidence }
        } catch (_: Exception) {
            emptyList()
        }

        val topLabel = labels.firstOrNull {
            !it.text.equals("Home good", ignoreCase = true) &&
            !it.text.equals("Fashion good", ignoreCase = true) &&
            !it.text.equals("Place", ignoreCase = true)
        } ?: labels.firstOrNull()

        val labelName = topLabel?.text ?: "Object"
        val taxonomy = YoloObjectTaxonomy.resolveLabel(labelName)
        val objectName = if (isHindi) taxonomy.hindiName else taxonomy.englishName

        val fallbackText = when (mode) {
            AiVisionMode.CURRENCY -> {
                // Check if text has numbers like 10, 20, 50, 100, 200, 500
                val detectedNumber = Regex("\\b(10|20|50|100|200|500)\\b").find(text)?.value
                if (detectedNumber != null) {
                    if (isHindi) "ऑफलाइन मोड: नोट पर ₹$detectedNumber का अंक दिखाई दे रहा है।"
                    else "Offline mode: Detected ₹$detectedNumber denomination on the note."
                } else {
                    if (isHindi) "ऑफलाइन मोड: नोट का सटीक मूल्य नहीं पढ़ा जा सका। कृपया रोशनी में नोट को सीधा रखें।"
                    else "Offline mode: Could not clearly read currency value. Please adjust lighting and hold steady."
                }
            }
            AiVisionMode.MEDICINE -> {
                val significantWords = text.lines().map { it.trim() }.filter { it.length >= 3 }.take(3).joinToString(", ")
                if (significantWords.isNotBlank()) {
                    if (isHindi) "ऑफलाइन मोड: दवाई पर लिखा है: $significantWords। पूरी जानकारी के लिए इंटरनेट से AI जांचें।"
                    else "Offline mode: Package text reads: $significantWords. Connect to internet for detailed AI check."
                } else {
                    if (isHindi) "ऑफलाइन मोड: दवाई का लेबल साफ नहीं दिखा। कैमरा थोड़ा पास लाएं।"
                    else "Offline mode: Medicine label not clear. Please hold closer."
                }
            }
            AiVisionMode.SCENE, AiVisionMode.COLOR, AiVisionMode.CUSTOM -> {
                val labelDesc = if (isHindi) "ऑफलाइन मोड: सामने $objectName दिखाई दे रहा है।"
                else "Offline mode: Detected $objectName in front."
                if (text.isNotBlank()) {
                    val sample = text.lines().firstOrNull { it.isNotBlank() } ?: ""
                    if (isHindi) "$labelDesc इस पर लिखा है: '$sample'" else "$labelDesc Text reads: '$sample'"
                } else {
                    labelDesc
                }
            }
        }

        return AiAnalysisResult(
            answer = fallbackText,
            isFromGemini = false,
            mode = mode,
            detectedText = text
        )
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun cleanForTTS(text: String): String {
        return text
            .replace(Regex("[*#_`~]"), "")
            .replace(Regex("\\n+"), " ")
            .trim()
    }
}
