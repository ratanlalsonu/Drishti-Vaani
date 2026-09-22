package com.example.feature.vision

import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.core.model.DetectedObject
import com.example.core.model.PriorityLevel
import com.example.core.model.ProximityTier
import com.example.core.model.SpatialPosition
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class ObjectDetectorHelper(
    private val onDetectionsReady: (List<DetectedObject>, Int, Int) -> Unit,
    private val onError: (Exception) -> Unit = {}
) : ImageAnalysis.Analyzer {

    // On-device real-time stream object detector with multiple objects and classification enabled
    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()

    private val detector = ObjectDetection.getClient(options)
    private var lastAnalyzedTimestamp = 0L
    private val frameIntervalMs = 250L // Process every 250ms (~4 FPS) to prevent battery drain and thermal throttling

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp < frameIntervalMs) {
            imageProxy.close()
            return
        }
        lastAnalyzedTimestamp = currentTimestamp

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        val imageWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width
        val imageHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height

        detector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                val results = mutableListOf<DetectedObject>()
                val totalFrameArea = (imageWidth * imageHeight).toFloat()

                for (obj in detectedObjects) {
                    val box = obj.boundingBox
                    val rectF = RectF(
                        box.left.toFloat().coerceAtLeast(0f),
                        box.top.toFloat().coerceAtLeast(0f),
                        box.right.toFloat().coerceAtMost(imageWidth.toFloat()),
                        box.bottom.toFloat().coerceAtMost(imageHeight.toFloat())
                    )

                    val centerX = rectF.centerX() / imageWidth.toFloat()
                    val position = when {
                        centerX < 0.33f -> SpatialPosition.LEFT
                        centerX > 0.66f -> SpatialPosition.RIGHT
                        else -> SpatialPosition.CENTER
                    }

                    val boxArea = rectF.width() * rectF.height()
                    val areaRatio = if (totalFrameArea > 0f) boxArea / totalFrameArea else 0f

                    val proximity = when {
                        areaRatio > 0.30f -> ProximityTier.VERY_CLOSE
                        areaRatio > 0.10f -> ProximityTier.NEARBY
                        else -> ProximityTier.FAR
                    }

                    // Best label from ML Kit classification
                    val bestLabel = obj.labels.maxByOrNull { it.confidence }
                    val labelText = bestLabel?.text ?: "Obstacle"
                    val confidence = bestLabel?.confidence ?: 0.65f

                    val priority = determinePriority(labelText, proximity)
                    val hindiLabel = translateToHindi(labelText)

                    results.add(
                        DetectedObject(
                            label = labelText,
                            hindiLabel = hindiLabel,
                            confidence = confidence,
                            boundingBox = rectF,
                            position = position,
                            proximity = proximity,
                            priority = priority
                        )
                    )
                }

                onDetectionsReady(results, imageWidth, imageHeight)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun determinePriority(label: String, proximity: ProximityTier): PriorityLevel {
        val lower = label.lowercase()
        return when {
            proximity == ProximityTier.VERY_CLOSE -> PriorityLevel.CRITICAL
            lower.contains("vehicle") || lower.contains("car") || lower.contains("truck") ||
            lower.contains("bus") || lower.contains("motorcycle") || lower.contains("bicycle") -> PriorityLevel.HIGH
            lower.contains("person") || lower.contains("human") -> PriorityLevel.HIGH
            lower.contains("stair") || lower.contains("step") || lower.contains("door") -> PriorityLevel.HIGH
            lower.contains("chair") || lower.contains("table") || lower.contains("furniture") -> PriorityLevel.NORMAL
            else -> PriorityLevel.LOW

        }
    }

    private fun translateToHindi(label: String): String {
        val lower = label.lowercase()
        return when {
            lower.contains("person") || lower.contains("human") -> "व्यक्ति (Person)"
            lower.contains("car") || lower.contains("vehicle") -> "गाड़ी (Vehicle)"
            lower.contains("bicycle") -> "साइकिल (Bicycle)"
            lower.contains("motorcycle") -> "मोटरसाइकिल (Motorcycle)"
            lower.contains("bus") -> "बस (Bus)"
            lower.contains("chair") -> "कुर्सी (Chair)"
            lower.contains("table") -> "मेज़ (Table)"
            lower.contains("door") -> "दरवाज़ा (Door)"
            lower.contains("stairs") -> "सीढ़ियां (Stairs)"
            else -> "रुकावट (Obstacle)"
        }
    }

    fun close() {
        try {
            detector.close()
        } catch (_: Exception) {}
    }
}
