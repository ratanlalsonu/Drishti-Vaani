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

                // Detect specifically TABLES and structural ROOM PILLARS as requested
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

                    val heightRatio = rectF.height() / imageHeight.toFloat()
                    val widthRatio = rectF.width() / imageWidth.toFloat()

                    val detectorLabel = obj.labels.maxByOrNull { it.confidence }?.text?.trim()
                    val allObjLabels = obj.labels.map { it.text.trim().lowercase(Locale.ROOT) }

                    // Check for Table and Pillar targets
                    val isTable = isTableObject(detectorLabel, allObjLabels, dominantLabels)
                    val isPillar = !isTable && isPillarObject(detectorLabel, allObjLabels, dominantLabels, heightRatio, widthRatio)

                    if (!isTable && !isPillar) {
                        continue // Strictly ignore any non-table, non-pillar object
                    }

                    val labelEn = if (isTable) "Table" else "Pillar"
                    val labelHi = if (isTable) "मेज़" else "खंभा"
                    val metaKey = if (isTable) "table" else "pillar"
                    val meta = YoloObjectTaxonomy.resolveLabel(metaKey)

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
                        val priority = if (isPillar) {
                            if (estimatedDistanceMeters < 2.0f) PriorityLevel.CRITICAL else PriorityLevel.HIGH
                        } else {
                            determinePriority(meta, proximity, estimatedDistanceMeters)
                        }

                        val distanceFeet = kotlin.math.round(estimatedDistanceMeters * 3.28f).toInt().coerceAtLeast(1)

                        // Step-by-step avoidance guidance to safely navigate around the obstacle
                        val evasionAdviceEn = when (position) {
                            SpatialPosition.CENTER -> "Please move aside by 2 to 3 feet to your right to avoid collision."
                            SpatialPosition.LEFT -> "Please move aside by 2 to 3 feet to your right to avoid collision."
                            SpatialPosition.RIGHT -> "Please move aside by 2 to 3 feet to your left to avoid collision."
                        }

                        val evasionAdviceHi = when (position) {
                            SpatialPosition.CENTER -> "टकराने से बचने के लिए कृपया 2 से 3 फीट दाईं ओर हटें।"
                            SpatialPosition.LEFT -> "टकराने से बचने के लिए कृपया 2 से 3 फीट दाईं ओर हटें।"
                            SpatialPosition.RIGHT -> "टकराने से बचने के लिए कृपया 2 से 3 फीट बाईं ओर हटें।"
                        }

                        results.add(
                            DetectedObject(
                                label = labelEn,
                                hindiLabel = labelHi,
                                confidence = obj.labels.maxByOrNull { it.confidence }?.confidence ?: 0.85f,
                                boundingBox = rectF,
                                position = position,
                                proximity = proximity,
                                priority = priority,
                                estimatedDistanceMeters = estimatedDistanceMeters,
                                distanceDescriptionHi = "$distanceFeet फीट (${kotlin.math.round(estimatedDistanceMeters * 10f) / 10f}m)",
                                distanceDescriptionEn = "$distanceFeet feet (${kotlin.math.round(estimatedDistanceMeters * 10f) / 10f}m)",
                                category = if (isTable) ObjectCategory.FURNITURE else ObjectCategory.ENVIRONMENT,
                                isBeyondThreshold = false,
                                distanceFeet = distanceFeet,
                                evasionAdviceEn = evasionAdviceEn,
                                evasionAdviceHi = evasionAdviceHi
                            )
                        )
                    }
                }

                // If bounding box detector missed it but scene-level classifier confirms a table or pillar
                if (results.isEmpty()) {
                    val tableScene = dominantLabels.firstOrNull { (label, conf) ->
                        conf >= 0.50f && listOf("table", "desk", "dining table", "coffee table", "tabletop").any {
                            label.contains(it, ignoreCase = true)
                        }
                    }
                    val pillarScene = dominantLabels.firstOrNull { (label, conf) ->
                        conf >= 0.50f && listOf("pillar", "column", "support pillar", "structural column", "post").any {
                            label.contains(it, ignoreCase = true)
                        }
                    }

                    if (tableScene != null || pillarScene != null) {
                        val isTable = tableScene != null
                        val labelEn = if (isTable) "Table" else "Pillar"
                        val labelHi = if (isTable) "मेज़" else "खंभा"
                        val metaKey = if (isTable) "table" else "pillar"
                        val meta = YoloObjectTaxonomy.resolveLabel(metaKey)
                        val conf = tableScene?.second ?: pillarScene?.second ?: 0.70f
                        val rectF = RectF(
                            imageWidth * 0.20f,
                            imageHeight * 0.15f,
                            imageWidth * 0.80f,
                            imageHeight * 0.85f
                        )
                        val estimatedDistanceMeters = if (isTable) 1.8f else 2.4f
                        val distanceFeet = kotlin.math.round(estimatedDistanceMeters * 3.28f).toInt().coerceAtLeast(1)

                        val evasionAdviceEn = "Please move aside by 2 to 3 feet to your right to avoid collision."
                        val evasionAdviceHi = "टकराने से बचने के लिए कृपया 2-3 फीट दाईं ओर हटें।"

                        results.add(
                            DetectedObject(
                                label = labelEn,
                                hindiLabel = labelHi,
                                confidence = conf,
                                boundingBox = rectF,
                                position = SpatialPosition.CENTER,
                                proximity = ProximityTier.NEARBY,
                                priority = PriorityLevel.HIGH,
                                estimatedDistanceMeters = estimatedDistanceMeters,
                                distanceDescriptionHi = "$distanceFeet फीट (${estimatedDistanceMeters}m)",
                                distanceDescriptionEn = "$distanceFeet feet (${estimatedDistanceMeters}m)",
                                category = if (isTable) ObjectCategory.FURNITURE else ObjectCategory.ENVIRONMENT,
                                isBeyondThreshold = false,
                                distanceFeet = distanceFeet,
                                evasionAdviceEn = evasionAdviceEn,
                                evasionAdviceHi = evasionAdviceHi
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

    private fun isTableObject(
        detectorLabel: String?,
        allObjLabels: List<String>,
        dominantLabels: List<Pair<String, Float>>
    ): Boolean {
        val tableTerms = listOf("table", "desk", "dining table", "coffee table", "tabletop", "nightstand", "workbench")
        if (detectorLabel != null && tableTerms.any { detectorLabel.contains(it, ignoreCase = true) }) {
            return true
        }
        if (allObjLabels.any { label -> tableTerms.any { label.contains(it, ignoreCase = true) } }) {
            return true
        }
        val hasSceneTable = dominantLabels.any { (label, conf) ->
            conf >= 0.35f && tableTerms.any { label.contains(it, ignoreCase = true) }
        }
        if (hasSceneTable && (detectorLabel.isNullOrBlank() || detectorLabel.equals("Home good", ignoreCase = true) || detectorLabel.equals("Furniture", ignoreCase = true))) {
            return true
        }
        return false
    }

    private fun isPillarObject(
        detectorLabel: String?,
        allObjLabels: List<String>,
        dominantLabels: List<Pair<String, Float>>,
        heightRatio: Float,
        widthRatio: Float
    ): Boolean {
        val pillarTerms = listOf("pillar", "column", "post", "pole", "support pillar", "structural column", "pier", "beam")
        if (detectorLabel != null && pillarTerms.any { detectorLabel.contains(it, ignoreCase = true) }) {
            return true
        }
        if (allObjLabels.any { label -> pillarTerms.any { label.contains(it, ignoreCase = true) } }) {
            return true
        }
        val hasScenePillar = dominantLabels.any { (label, conf) ->
            conf >= 0.35f && pillarTerms.any { label.contains(it, ignoreCase = true) }
        }
        if (hasScenePillar && (detectorLabel.isNullOrBlank() || detectorLabel.equals("Place", ignoreCase = true) || detectorLabel.equals("Structure", ignoreCase = true) || detectorLabel.equals("Home good", ignoreCase = true))) {
            return true
        }
        // Vertical architectural column spanning room structure
        if (heightRatio > 0.35f && widthRatio < heightRatio * 0.85f) {
            val isArchitecture = dominantLabels.any { (label, conf) ->
                conf >= 0.35f && listOf("architecture", "interior design", "building", "column", "pillar", "wall", "room").any {
                    label.contains(it, ignoreCase = true)
                }
            }
            if (isArchitecture && (detectorLabel.isNullOrBlank() || detectorLabel.equals("Place", ignoreCase = true) || detectorLabel.equals("Structure", ignoreCase = true))) {
                return true
            }
        }
        return false
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
