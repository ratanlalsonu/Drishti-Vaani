package com.example.core.model

enum class ObjectCategory(val displayLabelEn: String, val displayLabelHi: String) {
    PERSON("Person", "व्यक्ति"),
    VEHICLE("Vehicle", "वाहन"),
    FURNITURE("Furniture", "फर्नीचर"),
    HAZARD("Safety Hazard", "सुरक्षा जोखिम"),
    ELECTRONICS("Electronics", "इलेक्ट्रॉनिक्स"),
    ANIMAL("Animal", "जानवर"),
    EVERYDAY("Everyday Item", "दैनिक वस्तु"),
    ENVIRONMENT("Environment", "आस-पास का परिवेश"),
    OBSTACLE("Obstacle", "रुकावट")
}

data class ObjectMeta(
    val englishName: String,
    val hindiName: String,
    val category: ObjectCategory,
    val typicalHeightMeters: Float = 1.0f,
    val typicalWidthMeters: Float = 0.8f,
    val isCriticalHazard: Boolean = false
)

object YoloObjectTaxonomy {

    // Comprehensive dictionary mapping YOLO/COCO and ML Kit everyday labels to Hindi & Real dimensions
    private val taxonomyMap = mapOf(
        // People & Living
        "person" to ObjectMeta("Person", "व्यक्ति", ObjectCategory.PERSON, typicalHeightMeters = 1.7f, typicalWidthMeters = 0.5f),
        "human" to ObjectMeta("Person", "व्यक्ति", ObjectCategory.PERSON, typicalHeightMeters = 1.7f, typicalWidthMeters = 0.5f),
        "man" to ObjectMeta("Man", "व्यक्ति", ObjectCategory.PERSON, typicalHeightMeters = 1.75f, typicalWidthMeters = 0.5f),
        "woman" to ObjectMeta("Woman", "महिला", ObjectCategory.PERSON, typicalHeightMeters = 1.6f, typicalWidthMeters = 0.5f),
        "child" to ObjectMeta("Child", "बच्चा", ObjectCategory.PERSON, typicalHeightMeters = 1.1f, typicalWidthMeters = 0.4f),

        // Vehicles & Transport (Road safety)
        "car" to ObjectMeta("Car", "कार", ObjectCategory.VEHICLE, typicalHeightMeters = 1.5f, typicalWidthMeters = 1.8f, isCriticalHazard = true),
        "automobile" to ObjectMeta("Car", "कार", ObjectCategory.VEHICLE, typicalHeightMeters = 1.5f, typicalWidthMeters = 1.8f, isCriticalHazard = true),
        "vehicle" to ObjectMeta("Vehicle", "गाड़ी", ObjectCategory.VEHICLE, typicalHeightMeters = 1.6f, typicalWidthMeters = 1.9f, isCriticalHazard = true),
        "bicycle" to ObjectMeta("Bicycle", "साइकिल", ObjectCategory.VEHICLE, typicalHeightMeters = 1.0f, typicalWidthMeters = 1.5f),
        "bike" to ObjectMeta("Bicycle", "साइकिल", ObjectCategory.VEHICLE, typicalHeightMeters = 1.0f, typicalWidthMeters = 1.5f),
        "motorcycle" to ObjectMeta("Motorcycle", "मोटरसाइकिल", ObjectCategory.VEHICLE, typicalHeightMeters = 1.1f, typicalWidthMeters = 1.8f, isCriticalHazard = true),
        "motorbike" to ObjectMeta("Motorcycle", "मोटरसाइकिल", ObjectCategory.VEHICLE, typicalHeightMeters = 1.1f, typicalWidthMeters = 1.8f, isCriticalHazard = true),
        "scooter" to ObjectMeta("Scooter", "स्कूटर", ObjectCategory.VEHICLE, typicalHeightMeters = 1.1f, typicalWidthMeters = 1.6f, isCriticalHazard = true),
        "bus" to ObjectMeta("Bus", "बस", ObjectCategory.VEHICLE, typicalHeightMeters = 3.2f, typicalWidthMeters = 2.5f, isCriticalHazard = true),
        "truck" to ObjectMeta("Truck", "ट्रक", ObjectCategory.VEHICLE, typicalHeightMeters = 3.5f, typicalWidthMeters = 2.5f, isCriticalHazard = true),
        "auto rickshaw" to ObjectMeta("Auto Rickshaw", "ऑटो रिक्शा", ObjectCategory.VEHICLE, typicalHeightMeters = 1.8f, typicalWidthMeters = 1.4f, isCriticalHazard = true),
        "rickshaw" to ObjectMeta("Rickshaw", "रिक्शा", ObjectCategory.VEHICLE, typicalHeightMeters = 1.7f, typicalWidthMeters = 1.3f),

        // Critical Hazards & Wayfinding
        "stairs" to ObjectMeta("Stairs", "सीढ़ियां", ObjectCategory.HAZARD, typicalHeightMeters = 1.2f, typicalWidthMeters = 1.2f, isCriticalHazard = true),
        "stair" to ObjectMeta("Stairs", "सीढ़ियां", ObjectCategory.HAZARD, typicalHeightMeters = 1.2f, typicalWidthMeters = 1.2f, isCriticalHazard = true),
        "steps" to ObjectMeta("Steps", "सीढ़ियां/कदम", ObjectCategory.HAZARD, typicalHeightMeters = 1.0f, typicalWidthMeters = 1.2f, isCriticalHazard = true),
        "step" to ObjectMeta("Step", "कदम/पायदान", ObjectCategory.HAZARD, typicalHeightMeters = 0.5f, typicalWidthMeters = 1.0f, isCriticalHazard = true),
        "door" to ObjectMeta("Door", "दरवाज़ा", ObjectCategory.ENVIRONMENT, typicalHeightMeters = 2.0f, typicalWidthMeters = 0.9f),
        "pole" to ObjectMeta("Pole", "खंभा (Pole)", ObjectCategory.HAZARD, typicalHeightMeters = 2.5f, typicalWidthMeters = 0.3f, isCriticalHazard = true),
        "post" to ObjectMeta("Post", "खंभा", ObjectCategory.HAZARD, typicalHeightMeters = 2.0f, typicalWidthMeters = 0.3f),
        "pothole" to ObjectMeta("Pothole", "गड्ढा", ObjectCategory.HAZARD, typicalHeightMeters = 0.5f, typicalWidthMeters = 0.8f, isCriticalHazard = true),
        "traffic light" to ObjectMeta("Traffic Light", "ट्रैफिक लाइट", ObjectCategory.HAZARD, typicalHeightMeters = 1.0f, typicalWidthMeters = 0.4f),
        "stop sign" to ObjectMeta("Stop Sign", "स्टॉप साइन", ObjectCategory.HAZARD, typicalHeightMeters = 0.8f, typicalWidthMeters = 0.8f),

        // Indoor Furniture (navigation indoors)
        "chair" to ObjectMeta("Chair", "कुर्सी", ObjectCategory.FURNITURE, typicalHeightMeters = 0.85f, typicalWidthMeters = 0.5f),
        "couch" to ObjectMeta("Sofa", "सोफ़ा", ObjectCategory.FURNITURE, typicalHeightMeters = 0.85f, typicalWidthMeters = 1.8f),
        "sofa" to ObjectMeta("Sofa", "सोफ़ा", ObjectCategory.FURNITURE, typicalHeightMeters = 0.85f, typicalWidthMeters = 1.8f),
        "bench" to ObjectMeta("Bench", "बेंच", ObjectCategory.FURNITURE, typicalHeightMeters = 0.7f, typicalWidthMeters = 1.4f),
        "table" to ObjectMeta("Table", "मेज़", ObjectCategory.FURNITURE, typicalHeightMeters = 0.75f, typicalWidthMeters = 1.2f),
        "dining table" to ObjectMeta("Dining Table", "डाइनिंग मेज़", ObjectCategory.FURNITURE, typicalHeightMeters = 0.75f, typicalWidthMeters = 1.4f),
        "desk" to ObjectMeta("Desk", "डेस्क/मेज़", ObjectCategory.FURNITURE, typicalHeightMeters = 0.75f, typicalWidthMeters = 1.1f),
        "bed" to ObjectMeta("Bed", "बिस्तर/पलंग", ObjectCategory.FURNITURE, typicalHeightMeters = 0.6f, typicalWidthMeters = 1.9f),
        "wardrobe" to ObjectMeta("Wardrobe", "अलमारी", ObjectCategory.FURNITURE, typicalHeightMeters = 1.8f, typicalWidthMeters = 1.0f),
        "cupboard" to ObjectMeta("Cupboard", "अलमारी", ObjectCategory.FURNITURE, typicalHeightMeters = 1.5f, typicalWidthMeters = 0.9f),

        // Animals
        "dog" to ObjectMeta("Dog", "कुत्ता", ObjectCategory.ANIMAL, typicalHeightMeters = 0.6f, typicalWidthMeters = 0.8f),
        "cat" to ObjectMeta("Cat", "बिल्ली", ObjectCategory.ANIMAL, typicalHeightMeters = 0.3f, typicalWidthMeters = 0.4f),
        "cow" to ObjectMeta("Cow", "गाय", ObjectCategory.ANIMAL, typicalHeightMeters = 1.5f, typicalWidthMeters = 1.8f, isCriticalHazard = true),

        // Electronics & Appliances
        "tv" to ObjectMeta("TV", "टीवी स्क्रीन", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.7f, typicalWidthMeters = 1.1f),
        "monitor" to ObjectMeta("Computer Monitor", "मॉनिटर", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.5f, typicalWidthMeters = 0.6f),
        "laptop" to ObjectMeta("Laptop", "लैपटॉप", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.25f, typicalWidthMeters = 0.35f),
        "cell phone" to ObjectMeta("Mobile Phone", "मोबाइल फोन", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.15f, typicalWidthMeters = 0.08f),
        "mobile phone" to ObjectMeta("Mobile Phone", "मोबाइल फोन", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.15f, typicalWidthMeters = 0.08f),
        "refrigerator" to ObjectMeta("Refrigerator", "फ्रिज", ObjectCategory.ELECTRONICS, typicalHeightMeters = 1.7f, typicalWidthMeters = 0.8f),

        // Everyday Items
        "bottle" to ObjectMeta("Bottle", "बोतल", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.25f, typicalWidthMeters = 0.08f),
        "cup" to ObjectMeta("Cup", "कप", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.12f, typicalWidthMeters = 0.1f),
        "backpack" to ObjectMeta("Backpack", "बस्ता/बैग", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.45f, typicalWidthMeters = 0.35f),
        "bag" to ObjectMeta("Bag", "बैग", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.4f, typicalWidthMeters = 0.35f),
        "umbrella" to ObjectMeta("Umbrella", "छाता", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.8f, typicalWidthMeters = 0.8f),
        "suitcase" to ObjectMeta("Suitcase", "सूटकेस", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.6f, typicalWidthMeters = 0.45f),

        // Generic ML Kit classes fallback
        "home good" to ObjectMeta("Household Item", "घरेलू सामान", ObjectCategory.FURNITURE, typicalHeightMeters = 0.8f, typicalWidthMeters = 0.8f),
        "fashion good" to ObjectMeta("Clothing Item", "वस्त्र या सामान", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.5f, typicalWidthMeters = 0.5f),
        "food" to ObjectMeta("Food", "खाद्य सामग्री", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.2f, typicalWidthMeters = 0.2f),
        "place" to ObjectMeta("Structure", "संरचना / दीवार", ObjectCategory.ENVIRONMENT, typicalHeightMeters = 2.0f, typicalWidthMeters = 2.0f),
        "plant" to ObjectMeta("Plant", "पौधा / गमला", ObjectCategory.ENVIRONMENT, typicalHeightMeters = 0.7f, typicalWidthMeters = 0.5f)
    )

    fun resolveLabel(rawText: String): ObjectMeta {
        val clean = rawText.trim().lowercase()

        // 1. Direct match
        taxonomyMap[clean]?.let { return it }

        // 2. Keyword substring matching
        for ((key, meta) in taxonomyMap) {
            if (clean.contains(key) || key.contains(clean)) {
                return meta
            }
        }

        // 3. Fallback generic obstacle
        return ObjectMeta(
            englishName = rawText.replaceFirstChar { it.uppercase() },
            hindiName = "रुकावट (${rawText.replaceFirstChar { it.uppercase() }})",
            category = ObjectCategory.OBSTACLE,
            typicalHeightMeters = 0.8f,
            typicalWidthMeters = 0.8f
        )
    }

    /**
     * Calculates distance using pinhole camera geometry and object physical scale.
     * Normalized dimensions: heightRatio = box.height / frameHeight
     * Returns estimated distance in meters.
     */
    fun estimateDistance(
        meta: ObjectMeta,
        heightRatio: Float,
        widthRatio: Float
    ): Float {
        val clampedH = heightRatio.coerceIn(0.01f, 1.0f)
        val clampedW = widthRatio.coerceIn(0.01f, 1.0f)

        // Android smartphone vertical FOV is roughly 55-65 degrees, focal factor ~ 1.0 - 1.2
        val distanceByHeight = (meta.typicalHeightMeters * 1.15f) / clampedH
        val distanceByWidth = (meta.typicalWidthMeters * 1.15f) / clampedW

        // Combine weighted towards height because width varies with viewing angle (e.g. car front vs side)
        val estimatedRaw = (distanceByHeight * 0.7f + distanceByWidth * 0.3f)

        // Clamp between 0.3m (arm's length) and 30m
        val clamped = estimatedRaw.coerceIn(0.3f, 30.0f)

        // Round to 1 decimal place (e.g. 2.3m)
        return (kotlin.math.round(clamped * 10f) / 10f)
    }

    fun getDistanceDescription(distanceMeters: Float, isHindi: Boolean): String {
        return if (isHindi) {
            when {
                distanceMeters < 1.0f -> "1 मीटर से भी कम, बहुत पास"
                distanceMeters < 2.5f -> "लगभग ${distanceMeters.toInt().coerceAtLeast(1)} से 2 मीटर दूर"
                distanceMeters < 4.5f -> "लगभग ${kotlin.math.round(distanceMeters).toInt()} मीटर दूर"
                distanceMeters < 7.5f -> "लगभग ${kotlin.math.round(distanceMeters).toInt()} मीटर दूर"
                distanceMeters <= 10.0f -> "लगभग ${kotlin.math.round(distanceMeters).toInt()} मीटर दूर"
                else -> "10 मीटर से अधिक दूर"
            }
        } else {
            when {
                distanceMeters < 1.0f -> "under 1 meter, very close"
                distanceMeters < 2.5f -> "approx ${distanceMeters.toInt().coerceAtLeast(1)} to 2 meters away"
                distanceMeters < 4.5f -> "approx ${kotlin.math.round(distanceMeters).toInt()} meters away"
                distanceMeters < 7.5f -> "approx ${kotlin.math.round(distanceMeters).toInt()} meters away"
                distanceMeters <= 10.0f -> "approx ${kotlin.math.round(distanceMeters).toInt()} meters away"
                else -> "more than 10 meters away"
            }
        }
    }
}
