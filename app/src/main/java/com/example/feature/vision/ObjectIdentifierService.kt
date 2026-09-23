package com.example.feature.vision

import android.content.Context
import android.graphics.Bitmap
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

    suspend fun identifyObject(bitmap: Bitmap, language: AssistantLanguage): ObjectIdentityResult = withContext(Dispatchers.IO) {
        // High-accuracy On-Device ML (Works 100% Offline and Private)
        identifyWithOnDeviceML(bitmap, language)
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

    fun close() {
        try { imageLabeler.close() } catch (_: Exception) {}
        try { faceDetector.close() } catch (_: Exception) {}
        try { textRecognizer.close() } catch (_: Exception) {}
    }
}
