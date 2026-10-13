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

        val tableTerms = listOf("table", "desk", "dining table", "coffee table", "tabletop", "nightstand", "workbench")
        val isTable = rawLabels.any { label -> tableTerms.any { label.text.contains(it, ignoreCase = true) } }

        val significantText = recognizedText.lines()
            .map { it.trim() }
            .filter { line -> line.length in 3..30 && line.any { ch -> ch.isLetterOrDigit() } }
            .take(2)
            .joinToString(", ")

        val description = if (isTable) {
            if (language == AssistantLanguage.HINDI) {
                if (significantText.isNotBlank()) "सामने मेज़ (Table) उपस्थित है। लिखा है: $significantText" else "सामने मेज़ (Table) उपस्थित है।"
            } else {
                if (significantText.isNotBlank()) "Table detected in front. Text reads: $significantText" else "Table detected in front."
            }
        } else {
            if (language == AssistantLanguage.HINDI) {
                "सामने कोई मेज़ नहीं दिखी।"
            } else {
                "No table detected in front."
            }
        }

        val title = if (isTable) {
            if (language == AssistantLanguage.HINDI) "मेज़ (Table)" else "Table"
        } else {
            if (language == AssistantLanguage.HINDI) "कोई मेज़ नहीं" else "No Table"
        }

        return ObjectIdentityResult(
            title = title,
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
