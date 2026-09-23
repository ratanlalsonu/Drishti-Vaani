package com.example.feature.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.core.model.AssistantLanguage
import com.example.core.model.YoloObjectTaxonomy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
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
        ImageLabelerOptions.Builder().setConfidenceThreshold(0.35f).build()
    )
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
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
                        title = if (language == AssistantLanguage.HINDI) "पहचान (AI)" else "Identified (AI)",
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
            "आप एक दृष्टिबाधित व्यक्ति के सहायक हैं। कैमरे के सामने उपस्थित सजीव प्राणी (इंसान, व्यक्ति, कुत्ता, बिल्ली, अन्य जानवर, पक्षी, पौधा) या वस्तु (दवाई, नोट/रुपये, डिब्बा, बोतल, फर्नीचर आदि) को देखकर 1-2 छोटे और स्पष्ट वाक्यों में शुद्ध हिंदी में बताएं: 1) सामने कौन या क्या उपस्थित है (उदा. 'सामने एक व्यक्ति खड़े हैं', 'यह एक कुत्ता/बिल्ली है', 'यह 500 रुपये का नोट है', 'यह पैरासिटामोल दवाई है', 'यह गमले में तुलसी का पौधा है'), 2) इसका रंग या मुख्य विशेषता, 3) क्या कोई सुरक्षा सावधानी आवश्यक है।"
        } else {
            "You are assisting a blind user. Identify the living being (person, man, woman, dog, cat, animal, bird, plant) or object (medicine, banknote/currency, container, bottle, furniture) in front of the camera in 1-2 concise, clear sentences: 1) Who or what is present (e.g. 'A person is standing in front', 'This is a dog', 'This is a 500 rupee note', 'This is a medicine bottle of Paracetamol', 'This is a potted houseplant'), 2) Key color or identifying feature, 3) Any immediate safety caution."
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

        // 1. Detect Living Humans (Face Detection)
        val faces = try {
            faceDetector.process(inputImage).await()
        } catch (_: Exception) {
            emptyList()
        }

        // 2. Read OCR text on the object (brand, medicine name, currency value)
        val recognizedText = try {
            val visionText = textRecognizer.process(inputImage).await()
            visionText.text.trim()
        } catch (_: Exception) {
            ""
        }

        // 3. Run On-Device ML Kit labeler for 400+ classes (animals, pets, items)
        val labels = try {
            val mlLabels = imageLabeler.process(inputImage).await()
            mlLabels.sortedByDescending { it.confidence }
        } catch (_: Exception) {
            emptyList()
        }

        // Prioritize face detection if human present
        val isHumanDetected = faces.isNotEmpty()

        // Check for living creatures among labels (dog, cat, cow, bird, animal, plant)
        val livingLabel = labels.firstOrNull {
            val m = YoloObjectTaxonomy.resolveLabel(it.text)
            m.category == com.example.core.model.ObjectCategory.PERSON ||
            m.category == com.example.core.model.ObjectCategory.ANIMAL ||
            m.category == com.example.core.model.ObjectCategory.ENVIRONMENT
        }

        val topLabel = when {
            isHumanDetected -> null
            livingLabel != null -> livingLabel
            else -> labels.firstOrNull {
                !it.text.equals("Home good", ignoreCase = true) &&
                !it.text.equals("Fashion good", ignoreCase = true) &&
                !it.text.equals("Place", ignoreCase = true)
            } ?: labels.firstOrNull()
        }

        val rawName = when {
            isHumanDetected -> "person"
            topLabel != null -> topLabel.text
            else -> "Unknown Object"
        }

        val meta = YoloObjectTaxonomy.resolveLabel(rawName)
        val objectName = if (language == AssistantLanguage.HINDI) meta.hindiName else meta.englishName

        // Filter significant words from OCR (like "Dettol", "Paracetamol", "500", "Amul")
        val significantText = recognizedText.lines()
            .map { it.trim() }
            .filter { it.length in 3..25 && it.any { ch -> ch.isLetterOrDigit() } }
            .take(2)
            .joinToString(", ")

        val description = when {
            meta.category == com.example.core.model.ObjectCategory.PERSON -> {
                if (language == AssistantLanguage.HINDI) {
                    "सामने व्यक्ति (इंसान) उपस्थित हैं।"
                } else {
                    "A person is present in front of you."
                }
            }
            meta.category == com.example.core.model.ObjectCategory.ANIMAL -> {
                if (language == AssistantLanguage.HINDI) {
                    "सामने सजीव प्राणी ($objectName) उपस्थित है।"
                } else {
                    "There is a $objectName in front of you."
                }
            }
            meta.category == com.example.core.model.ObjectCategory.ENVIRONMENT &&
            (rawName.contains("plant", ignoreCase = true) || rawName.contains("tree", ignoreCase = true) || rawName.contains("flower", ignoreCase = true)) -> {
                if (language == AssistantLanguage.HINDI) {
                    "सामने $objectName उपस्थित है।"
                } else {
                    "There is a $objectName in front of you."
                }
            }
            else -> {
                if (language == AssistantLanguage.HINDI) {
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

    fun close() {
        try { imageLabeler.close() } catch (_: Exception) {}
        try { faceDetector.close() } catch (_: Exception) {}
        try { textRecognizer.close() } catch (_: Exception) {}
    }
}
