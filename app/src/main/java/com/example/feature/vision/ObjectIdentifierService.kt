package com.example.feature.vision

import android.content.Context
import android.graphics.Bitmap
import com.example.core.model.AssistantLanguage
import com.example.core.model.ObjectCategory
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
import java.util.Locale

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
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val ignoredAbstractLabels = setOf(
        "photography", "snapshot", "rectangle", "circle", "line", "font", "pattern",
        "parallel", "material property", "symmetry", "design", "monochrome", "sky",
        "flooring", "wood", "floor", "ceiling", "architecture", "interior design",
        "room", "indoor", "outdoor", "lighting", "fixture", "flash photography",
        "black-and-white", "selfie"
    )

    suspend fun identifyObject(bitmap: Bitmap, language: AssistantLanguage): ObjectIdentityResult = withContext(Dispatchers.IO) {
        // High-accuracy On-Device ML (Works 100% Offline, Private, and Real)
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

        // 3. Run On-Device ML Kit labeler for 400+ everyday classes
        val rawLabels = try {
            val mlLabels = imageLabeler.process(inputImage).await()
            mlLabels.sortedByDescending { it.confidence }
        } catch (_: Exception) {
            emptyList()
        }

        // Filter out non-physical abstract labels
        val meaningfulLabels = rawLabels.filter { label ->
            !ignoredAbstractLabels.contains(label.text.trim().lowercase(Locale.ROOT))
        }

        val isHumanFaceDetected = faces.isNotEmpty()

        // 4. Select top real object label
        val chosenRawLabel = when {
            isHumanFaceDetected -> "person"
            meaningfulLabels.isNotEmpty() -> meaningfulLabels.first().text
            rawLabels.isNotEmpty() -> rawLabels.first().text
            else -> "Obstacle"
        }

        val meta = YoloObjectTaxonomy.resolveLabel(chosenRawLabel)
        val objectName = if (language == AssistantLanguage.HINDI) meta.hindiName else meta.englishName

        // Filter significant words from OCR (like "Dettol", "Paracetamol", "500", "Amul", "Crocin")
        val significantText = recognizedText.lines()
            .map { it.trim() }
            .filter { line -> line.length in 3..30 && line.any { ch -> ch.isLetterOrDigit() } }
            .take(2)
            .joinToString(", ")

        // Generate truthful, clear voice description distinguishing Living vs Non-Living:
        val description = when {
            // A. Living Human
            meta.category == ObjectCategory.PERSON -> {
                if (language == AssistantLanguage.HINDI) {
                    "सामने व्यक्ति (इंसान) उपस्थित हैं।"
                } else {
                    "A person is present in front of you."
                }
            }

            // B. Living Animal
            meta.category == ObjectCategory.ANIMAL -> {
                if (language == AssistantLanguage.HINDI) {
                    "सामने सजीव प्राणी ($objectName) उपस्थित है।"
                } else {
                    "There is an animal ($objectName) in front of you."
                }
            }

            // C. Living Flora (Plants / Trees)
            meta.isFlora -> {
                if (language == AssistantLanguage.HINDI) {
                    "सामने सजीव पौधा या वृक्ष ($objectName) उपस्थित है।"
                } else {
                    "There is a $objectName in front of you."
                }
            }

            // D. Non-Living Inanimate Object (Furniture, Electronics, Utensils, Household, etc.)
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
