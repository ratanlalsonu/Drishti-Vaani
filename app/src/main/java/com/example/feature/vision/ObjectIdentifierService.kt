package com.example.feature.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
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

data class ObjectIdentityResult(
    val title: String,
    val description: String,
    val isAiVerified: Boolean,
    val detectedText: String = ""
)

class ObjectIdentifierService(private val context: Context) {

    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(0.40f).build()
    )
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun identifyObject(bitmap: Bitmap, language: AssistantLanguage): ObjectIdentityResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        // 1. Try Gemini Vision AI if API key is provided and non-empty
        if (apiKey.isNotBlank() && apiKey != "DEFAULT_API_KEY") {
            try {
                val aiResponse = callGeminiVision(bitmap, apiKey, language)
                if (!aiResponse.isNullOrBlank()) {
                    return@withContext ObjectIdentityResult(
                        title = if (language == AssistantLanguage.HINDI) "पहचानी गई वस्तु" else "Identified Object",
                        description = aiResponse.trim(),
                        isAiVerified = true
                    )
                }
            } catch (e: Exception) {
                Log.w("ObjectIdentifierService", "Gemini vision call failed, falling back to on-device ML", e)
            }
        }

        // 2. High-accuracy On-Device Fallback (Works 100% Offline)
        return@withContext identifyWithOnDeviceML(bitmap, language)
    }

    private fun callGeminiVision(bitmap: Bitmap, apiKey: String, language: AssistantLanguage): String? {
        val scaledBitmap = scaleBitmap(bitmap, maxDimension = 1024)
        val base64Image = bitmapToBase64(scaledBitmap)

        val prompt = if (language == AssistantLanguage.HINDI) {
            "आप एक दृष्टिबाधित व्यक्ति के सहायक हैं। सामने रखी वस्तु को देखकर 1-2 छोटे और स्पष्ट वाक्यों में शुद्ध हिंदी में बताएं: 1) वस्तु का सटीक नाम और पहचान क्या है (उदा. 'यह 500 रुपये का नोट है', 'यह डिटॉल साबुन है', 'यह पानी की बोतल है', 'यह क्रोसिन सिरप है', 'यह स्टील की थाली है'), 2) इसका रंग या ब्रांड यदि दिख रहा हो, 3) क्या इसे छूना सुरक्षित है या कोई जोखिम है।"
        } else {
            "You are assisting a blind user. Identify the object in front of the camera in 1-2 clear, direct sentences: 1) Exact name and identity of the item (e.g. 'This is a 500 rupee note', 'This is a water bottle', 'This is a medicine bottle of Paracetamol', 'This is a steel spoon'), 2) Its color or brand if visible, 3) Any safety notice (safe to touch or sharp/hazardous)."
        }

        val jsonRequest = JSONObject().apply {
            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("maxOutputTokens", 200)
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
                Log.e("ObjectIdentifierService", "Gemini API error code: ${response.code}")
                return null
            }
            val responseBody = response.body?.string() ?: return null
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: return null
            val firstCandidate = candidates.optJSONObject(0) ?: return null
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            val firstPart = parts.optJSONObject(0) ?: return null
            return firstPart.optString("text", "")
        }
    }

    private suspend fun identifyWithOnDeviceML(bitmap: Bitmap, language: AssistantLanguage): ObjectIdentityResult {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        // Read OCR text on the object (brand, medicine name, currency value)
        val recognizedText = try {
            val visionText = textRecognizer.process(inputImage).await()
            visionText.text.trim()
        } catch (_: Exception) {
            ""
        }

        // Run On-Device ML Kit labeler
        val labels = try {
            val mlLabels = imageLabeler.process(inputImage).await()
            mlLabels.sortedByDescending { it.confidence }
        } catch (_: Exception) {
            emptyList()
        }

        val topLabel = labels.firstOrNull {
            !it.text.equals("Home good", ignoreCase = true) &&
            !it.text.equals("Fashion good", ignoreCase = true) &&
            !it.text.equals("Place", ignoreCase = true)
        } ?: labels.firstOrNull()

        val rawName = topLabel?.text ?: "Unknown Object"
        val meta = YoloObjectTaxonomy.resolveLabel(rawName)
        val objectName = if (language == AssistantLanguage.HINDI) meta.hindiName else meta.englishName

        // Filter significant words from OCR (like "Dettol", "Paracetamol", "500", "Amul")
        val significantText = recognizedText.lines()
            .map { it.trim() }
            .filter { it.length in 3..25 && it.any { ch -> ch.isLetterOrDigit() } }
            .take(2)
            .joinToString(", ")

        val description = if (language == AssistantLanguage.HINDI) {
            val base = "सामने यह वस्तु $objectName है।"
            if (significantText.isNotBlank()) {
                "$base इस पर लिखा है: $significantText"
            } else {
                base
            }
        } else {
            val base = "The object in front is $objectName."
            if (significantText.isNotBlank()) {
                "$base Label text reads: $significantText"
            } else {
                base
            }
        }

        return ObjectIdentityResult(
            title = objectName,
            description = description,
            isAiVerified = false,
            detectedText = recognizedText
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
}
