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
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.Locale

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

    // ML Kit Face Detector: instantly and reliably detects living humans (people, children, adults)
    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setMinFaceSize(0.12f)
        .build()

    private val faceDetector = FaceDetection.getClient(faceDetectorOptions)

    // ML Kit Image Labeler (detects 400+ everyday categories)
    private val imageLabelerOptions = ImageLabelerOptions.Builder()
        .setConfidenceThreshold(0.40f)
        .build()

    private val imageLabeler = ImageLabeling.getClient(imageLabelerOptions)

    @Volatile
    private var isAnalyzingFrame = false

    @Volatile
    private var isClosed = false

    private var lastAnalyzedTimestamp = 0L
    private val frameIntervalMs = 250L // ~4 FPS for smooth real-time response

    // Periodic scene classification cache
    private var lastSceneLabelTimestamp = 0L
    @Volatile
    private var cachedSceneLabels: List<Pair<String, Float>> = emptyList()

    private val ignoredBackgroundLabels = setOf(
        "photography", "snapshot", "rectangle", "circle", "line", "font", "pattern",
        "parallel", "material property", "symmetry", "design", "monochrome", "sky",
        "flooring", "wood", "floor", "ceiling", "architecture", "interior design",
        "room", "indoor", "outdoor", "lighting", "fixture", "flash photography",
        "black-and-white", "selfie"
    )

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

        // Refresh scene-level labels periodically
        if (currentTimestamp - lastSceneLabelTimestamp > 500L && !isClosed) {
            lastSceneLabelTimestamp = currentTimestamp
            imageLabeler.process(inputImage)
                .addOnSuccessListener { labels ->
                    if (!isClosed) {
                        cachedSceneLabels = labels
                            .filter { it.confidence >= 0.40f && !ignoredBackgroundLabels.contains(it.text.trim().lowercase(Locale.ROOT)) }
                            .map { Pair(it.text, it.confidence) }
                    }
                }
                .addOnFailureListener {
                    // Ignore background labeler cancel errors
                }
        }

        // Run ML Kit Object Detector and Face Detector concurrently
        val objectTask = objectDetector.process(inputImage)
        val faceTask = faceDetector.process(inputImage)

        Tasks.whenAllComplete(objectTask, faceTask)
            .addOnCompleteListener {
                if (isClosed) {
                    try { imageProxy.close() } catch (_: Exception) {}
                    isAnalyzingFrame = false
                    return@addOnCompleteListener
                }

                val detectedObjects = if (objectTask.isSuccessful) objectTask.result ?: emptyList() else emptyList()
                val detectedFaces = if (faceTask.isSuccessful) faceTask.result ?: emptyList() else emptyList()
                val dominantLabels = cachedSceneLabels

                val results = mutableListOf<DetectedObject>()
                val matchedFaceCenters = mutableListOf<Float>()

                // 1. Process Detected Living Humans (Faces)
                for (face in detectedFaces) {
                    val faceBox = face.boundingBox
                    val centerXRatio = faceBox.centerX().toFloat() / imageWidth.toFloat()
                    val position = when {
                        centerXRatio < 0.33f -> SpatialPosition.LEFT
                        centerXRatio > 0.66f -> SpatialPosition.RIGHT
                        else -> SpatialPosition.CENTER
                    }

                    // A human head/face has average height ~0.24 meters
                    val faceHeightRatio = (faceBox.height().toFloat() / imageHeight.toFloat()).coerceIn(0.015f, 1.0f)

                    val meta = YoloObjectTaxonomy.resolveLabel("person")
                    val estimatedDistanceMeters = ((0.24f * 1.15f) / faceHeightRatio).coerceIn(0.4f, 30.0f)
                    val roundedDistance = (kotlin.math.round(estimatedDistanceMeters * 10f) / 10f)

                    if (roundedDistance <= maxRangeMeters) {
                        matchedFaceCenters.add(centerXRatio)

                        // Extrapolate approximate full body bounding box for person
                        val bodyLeft = (faceBox.left - faceBox.width() * 0.5f).coerceAtLeast(0f)
                        val bodyRight = (faceBox.right + faceBox.width() * 0.5f).coerceAtMost(imageWidth.toFloat())
                        val bodyTop = faceBox.top.toFloat().coerceAtLeast(0f)
                        val bodyBottom = (faceBox.top + faceBox.height() * 5.0f).coerceAtMost(imageHeight.toFloat())
                        val rectF = RectF(bodyLeft, bodyTop, bodyRight, bodyBottom)

                        val proximity = when {
                            roundedDistance < 1.5f -> ProximityTier.VERY_CLOSE
                            roundedDistance < 4.5f -> ProximityTier.NEARBY
                            else -> ProximityTier.FAR
                        }
                        val priority = determinePriority(meta, proximity, roundedDistance)

                        results.add(
                            DetectedObject(
                                label = meta.englishName,
                                hindiLabel = meta.hindiName,
                                confidence = 0.92f,
                                boundingBox = rectF,
                                position = position,
                                proximity = proximity,
                                priority = priority,
                                estimatedDistanceMeters = roundedDistance,
                                distanceDescriptionHi = YoloObjectTaxonomy.getDistanceDescription(roundedDistance, isHindi = true),
                                distanceDescriptionEn = YoloObjectTaxonomy.getDistanceDescription(roundedDistance, isHindi = false),
                                category = ObjectCategory.PERSON,
                                isBeyondThreshold = false
                            )
                        )
                    }
                }

                // 2. Process Detected Real Objects with Bounding Boxes (Zero synthetic data!)
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

                    // Avoid duplicate if a verified human face was already mapped here
                    val isAlreadyPerson = matchedFaceCenters.any { kotlin.math.abs(it - centerX) < 0.22f }
                    if (isAlreadyPerson && results.any { it.category == ObjectCategory.PERSON && it.position == position }) {
                        continue
                    }

                    val heightRatio = rectF.height() / imageHeight.toFloat()
                    val widthRatio = rectF.width() / imageWidth.toFloat()

                    val detectorLabel = obj.labels.maxByOrNull { it.confidence }?.text?.trim()

                    // Resolve label without fake living data:
                    val resolvedLabel = when {
                        // A. Coarse "Home good" is strictly inanimate furniture/household
                        detectorLabel.equals("Home good", ignoreCase = true) -> {
                            val matchingItem = dominantLabels.firstOrNull { pair ->
                                val m = YoloObjectTaxonomy.resolveLabel(pair.first)
                                m.category == ObjectCategory.FURNITURE ||
                                m.category == ObjectCategory.ELECTRONICS ||
                                m.category == ObjectCategory.EVERYDAY
                            }
                            matchingItem?.first ?: "Household Item"
                        }

                        // B. Coarse "Fashion good" is clothing, bag, or footwear
                        detectorLabel.equals("Fashion good", ignoreCase = true) -> {
                            val matchingFashion = dominantLabels.firstOrNull { pair ->
                                val m = YoloObjectTaxonomy.resolveLabel(pair.first)
                                m.category == ObjectCategory.EVERYDAY
                            }
                            matchingFashion?.first ?: "Clothing Item"
                        }

                        // C. Food
                        detectorLabel.equals("Food", ignoreCase = true) -> {
                            val matchingFood = dominantLabels.firstOrNull { pair ->
                                val m = YoloObjectTaxonomy.resolveLabel(pair.first)
                                m.englishName.equals("Food Item", ignoreCase = true) ||
                                pair.first.contains("food", ignoreCase = true) ||
                                pair.first.contains("fruit", ignoreCase = true)
                            }
                            matchingFood?.first ?: "Food"
                        }

                        // D. Plant
                        detectorLabel.equals("Plant", ignoreCase = true) -> {
                            "Plant"
                        }

                        // E. Place (wall/door/structure)
                        detectorLabel.equals("Place", ignoreCase = true) -> {
                            val matchingStructure = dominantLabels.firstOrNull { pair ->
                                val m = YoloObjectTaxonomy.resolveLabel(pair.first)
                                m.category == ObjectCategory.ENVIRONMENT && !m.isFlora
                            }
                            matchingStructure?.first ?: "Structure"
                        }

                        // F. Specific non-empty label directly from object detector
                        !detectorLabel.isNullOrBlank() -> detectorLabel

                        // G. Detector had no coarse label; check if high-confidence object label is in dominantLabels
                        dominantLabels.isNotEmpty() -> {
                            val topCandidate = dominantLabels.firstOrNull()
                            if (topCandidate != null && topCandidate.second >= 0.60f) {
                                val metaCheck = YoloObjectTaxonomy.resolveLabel(topCandidate.first)
                                // Never classify as animal from background label unless confidence is very high (>= 0.75f)
                                if (metaCheck.category == ObjectCategory.ANIMAL) {
                                    if (topCandidate.second >= 0.75f) topCandidate.first else "Obstacle"
                                } else {
                                    topCandidate.first
                                }
                            } else {
                                "Obstacle"
                            }
                        }

                        else -> "Obstacle"
                    }

                    val meta = YoloObjectTaxonomy.resolveLabel(resolvedLabel)
                    val estimatedDistanceMeters = YoloObjectTaxonomy.estimateDistance(
                        meta = meta,
                        heightRatio = heightRatio,
                        widthRatio = widthRatio
                    )

                    if (estimatedDistanceMeters <= maxRangeMeters) {
                        val proximity = when {
                            estimatedDistanceMeters < 1.5f -> ProximityTier.VERY_CLOSE
                            estimatedDistanceMeters < 4.5f -> ProximityTier.NEARBY
                            else -> ProximityTier.FAR
                        }
                        val priority = determinePriority(meta, proximity, estimatedDistanceMeters)

                        results.add(
                            DetectedObject(
                                label = meta.englishName,
                                hindiLabel = meta.hindiName,
                                confidence = obj.labels.maxByOrNull { it.confidence }?.confidence ?: 0.75f,
                                boundingBox = rectF,
                                position = position,
                                proximity = proximity,
                                priority = priority,
                                estimatedDistanceMeters = estimatedDistanceMeters,
                                distanceDescriptionHi = YoloObjectTaxonomy.getDistanceDescription(estimatedDistanceMeters, isHindi = true),
                                distanceDescriptionEn = YoloObjectTaxonomy.getDistanceDescription(estimatedDistanceMeters, isHindi = false),
                                category = meta.category,
                                isBeyondThreshold = false
                            )
                        )
                    }
                }

                if (!isClosed) {
                    onDetectionsReady(results, imageWidth, imageHeight)
                }

                try {
                    imageProxy.close()
                } catch (_: Exception) {}
                isAnalyzingFrame = false
            }
            .addOnFailureListener { exception ->
                if (!isClosed && exception.message?.contains("cancel", ignoreCase = true) != true) {
                    onError(exception)
                }
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
            distanceMeters < 1.2f -> PriorityLevel.CRITICAL
            meta.isCriticalHazard && distanceMeters < 3.5f -> PriorityLevel.CRITICAL
            meta.isCriticalHazard -> PriorityLevel.HIGH
            meta.category == ObjectCategory.PERSON && distanceMeters < 4.0f -> PriorityLevel.HIGH
            meta.category == ObjectCategory.ANIMAL && distanceMeters < 3.0f -> PriorityLevel.HIGH
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
            faceDetector.close()
        } catch (_: Exception) {}
        try {
            imageLabeler.close()
        } catch (_: Exception) {}
    }
}
