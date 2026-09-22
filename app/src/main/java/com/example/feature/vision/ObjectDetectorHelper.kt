package com.example.feature.vision

import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.core.model.DetectedObject
import com.example.core.model.DetectionRangeLimit
import com.example.core.model.ObjectCategory
import com.example.core.model.PriorityLevel
import com.example.core.model.ProximityTier
import com.example.core.model.SpatialPosition
import com.example.core.model.YoloObjectTaxonomy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class ObjectDetectorHelper(
    private val onDetectionsReady: (List<DetectedObject>, Int, Int) -> Unit,
    private val onError: (Exception) -> Unit = {}
) : ImageAnalysis.Analyzer {

    // Range threshold: default 10 meters as requested by user
    @Volatile
    var maxRangeMeters: Float = DetectionRangeLimit.STANDARD_10M.maxMeters

    // Stream-mode ML Kit Object Detector with classification
    private val objectDetectorOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()

    private val objectDetector = ObjectDetection.getClient(objectDetectorOptions)

    // ML Kit Image Labeler (provides 400+ specific everyday YOLO-like classes)
    private val imageLabelerOptions = ImageLabelerOptions.Builder()
        .setConfidenceThreshold(0.50f)
        .build()

    private val imageLabeler = ImageLabeling.getClient(imageLabelerOptions)

    @Volatile
    private var isAnalyzingFrame = false

    @Volatile
    private var isClosed = false

    private var lastAnalyzedTimestamp = 0L
    private val frameIntervalMs = 250L // ~4 FPS for smooth real-time response

    // Periodic scene classification cache (refreshed periodically to avoid multiple TFLite tasks per frame)
    private var lastSceneLabelTimestamp = 0L
    @Volatile
    private var cachedSceneLabels: List<String> = emptyList()

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isClosed || isAnalyzingFrame) {
            imageProxy.close()
            return
        }

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

        isAnalyzingFrame = true

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        val imageWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width
        val imageHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height

        // Refresh scene-level classes periodically in background
        if (currentTimestamp - lastSceneLabelTimestamp > 1500L && !isClosed) {
            lastSceneLabelTimestamp = currentTimestamp
            imageLabeler.process(inputImage)
                .addOnSuccessListener { labels ->
                    if (!isClosed) {
                        cachedSceneLabels = labels
                            .filter { it.confidence >= 0.50f }
                            .map { it.text }
                    }
                }
                .addOnFailureListener {
                    // Ignore background labeler cancel/lifecycle errors
                }
        }

        // Run stream object detector for real-time bounding boxes & classifications
        objectDetector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                if (isClosed) return@addOnSuccessListener

                val results = mutableListOf<DetectedObject>()
                val dominantLabels = cachedSceneLabels

                for ((index, obj) in detectedObjects.withIndex()) {
                    val box = obj.boundingBox
                    val rectF = RectF(
                        box.left.toFloat().coerceAtLeast(0f),
                        box.top.toFloat().coerceAtLeast(0f),
                        box.right.toFloat().coerceAtMost(imageWidth.toFloat()),
                        box.bottom.toFloat().coerceAtMost(imageHeight.toFloat())
                    )

                    // Spatial Position (Left, Center, Right)
                    val centerX = rectF.centerX() / imageWidth.toFloat()
                    val position = when {
                        centerX < 0.33f -> SpatialPosition.LEFT
                        centerX > 0.66f -> SpatialPosition.RIGHT
                        else -> SpatialPosition.CENTER
                    }

                    val heightRatio = rectF.height() / imageHeight.toFloat()
                    val widthRatio = rectF.width() / imageWidth.toFloat()

                    // Resolve best object label:
                    val detectorLabel = obj.labels.maxByOrNull { it.confidence }?.text
                    val chosenRawLabel = when {
                        !detectorLabel.isNullOrBlank() &&
                        !detectorLabel.equals("Home good", ignoreCase = true) &&
                        !detectorLabel.equals("Fashion good", ignoreCase = true) &&
                        !detectorLabel.equals("Place", ignoreCase = true) -> detectorLabel

                        dominantLabels.isNotEmpty() -> {
                            dominantLabels.getOrNull(index) ?: dominantLabels.first()
                        }

                        !detectorLabel.isNullOrBlank() -> detectorLabel
                        else -> "Obstacle"
                    }

                    // Match with YOLO / COCO taxonomy
                    val meta = YoloObjectTaxonomy.resolveLabel(chosenRawLabel)

                    // Calculate estimated distance in meters using pinhole geometry
                    val estimatedDistanceMeters = YoloObjectTaxonomy.estimateDistance(
                        meta = meta,
                        heightRatio = heightRatio,
                        widthRatio = widthRatio
                    )

                    // Check 10-meter threshold (or user-defined range)
                    if (estimatedDistanceMeters > maxRangeMeters) {
                        continue
                    }

                    val proximity = when {
                        estimatedDistanceMeters < 1.5f -> ProximityTier.VERY_CLOSE
                        estimatedDistanceMeters < 4.5f -> ProximityTier.NEARBY
                        else -> ProximityTier.FAR
                    }

                    val priority = determinePriority(meta, proximity, estimatedDistanceMeters)
                    val distanceDescHi = YoloObjectTaxonomy.getDistanceDescription(estimatedDistanceMeters, isHindi = true)
                    val distanceDescEn = YoloObjectTaxonomy.getDistanceDescription(estimatedDistanceMeters, isHindi = false)

                    results.add(
                        DetectedObject(
                            label = meta.englishName,
                            hindiLabel = meta.hindiName,
                            confidence = obj.labels.maxByOrNull { it.confidence }?.confidence ?: 0.70f,
                            boundingBox = rectF,
                            position = position,
                            proximity = proximity,
                            priority = priority,
                            estimatedDistanceMeters = estimatedDistanceMeters,
                            distanceDescriptionHi = distanceDescHi,
                            distanceDescriptionEn = distanceDescEn,
                            category = meta.category,
                            isBeyondThreshold = false
                        )
                    )
                }

                if (!isClosed) {
                    onDetectionsReady(results, imageWidth, imageHeight)
                }
            }
            .addOnFailureListener { exception ->
                if (!isClosed && exception.message?.contains("cancel", ignoreCase = true) != true) {
                    onError(exception)
                }
            }
            .addOnCompleteListener {
                try {
                    imageProxy.close()
                } catch (_: Exception) {}
                isAnalyzingFrame = false
            }
    }

    private fun determinePriority(
        meta: com.example.core.model.ObjectMeta,
        proximity: ProximityTier,
        distanceMeters: Float
    ): PriorityLevel {
        return when {
            // Immediate collision hazard: closer than 1.2m
            distanceMeters < 1.2f -> PriorityLevel.CRITICAL
            meta.isCriticalHazard && distanceMeters < 3.5f -> PriorityLevel.CRITICAL
            meta.isCriticalHazard -> PriorityLevel.HIGH
            meta.category == ObjectCategory.PERSON && distanceMeters < 4.0f -> PriorityLevel.HIGH
            meta.category == ObjectCategory.VEHICLE -> PriorityLevel.HIGH
            meta.category == ObjectCategory.HAZARD -> PriorityLevel.HIGH
            proximity == ProximityTier.VERY_CLOSE -> PriorityLevel.HIGH
            proximity == ProximityTier.NEARBY -> PriorityLevel.NORMAL
            else -> PriorityLevel.LOW
        }
    }

    fun close() {
        isClosed = true
        isAnalyzingFrame = false
        try {
            objectDetector.close()
        } catch (_: Exception) {}
        try {
            imageLabeler.close()
        } catch (_: Exception) {}
    }
}
