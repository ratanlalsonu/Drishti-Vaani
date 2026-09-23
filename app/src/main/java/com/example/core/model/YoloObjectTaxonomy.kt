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
        "plant" to ObjectMeta("Plant", "पौधा / गमला", ObjectCategory.ENVIRONMENT, typicalHeightMeters = 0.7f, typicalWidthMeters = 0.5f),

        // Currency & Money
        "money" to ObjectMeta("Currency / Money", "रुपया / पैसे", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.08f, typicalWidthMeters = 0.15f),
        "banknote" to ObjectMeta("Currency Note", "रुपये का नोट", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.08f, typicalWidthMeters = 0.15f),
        "cash" to ObjectMeta("Cash Money", "रुपया", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.08f, typicalWidthMeters = 0.15f),
        "coin" to ObjectMeta("Coin", "सिक्का", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.03f, typicalWidthMeters = 0.03f),
        "wallet" to ObjectMeta("Wallet", "पर्स / बटुआ", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.12f, typicalWidthMeters = 0.1f),
        "purse" to ObjectMeta("Purse", "पर्स", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.2f, typicalWidthMeters = 0.25f),

        // Personal Items & Wearables
        "glasses" to ObjectMeta("Glasses", "चश्मा", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.05f, typicalWidthMeters = 0.14f),
        "spectacles" to ObjectMeta("Spectacles", "चश्मा", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.05f, typicalWidthMeters = 0.14f),
        "sunglasses" to ObjectMeta("Sunglasses", "धूप का चश्मा", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.05f, typicalWidthMeters = 0.14f),
        "watch" to ObjectMeta("Wristwatch", "हाथ की घड़ी", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.06f, typicalWidthMeters = 0.05f),
        "clock" to ObjectMeta("Clock", "दीवार घड़ी", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.3f, typicalWidthMeters = 0.3f),
        "key" to ObjectMeta("Key", "चाबी", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.06f, typicalWidthMeters = 0.03f),
        "keys" to ObjectMeta("Keys", "चाबियां", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.08f, typicalWidthMeters = 0.05f),
        "lock" to ObjectMeta("Lock", "ताला", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.1f, typicalWidthMeters = 0.07f),
        "shoe" to ObjectMeta("Shoe", "जूता", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.12f, typicalWidthMeters = 0.28f),
        "footwear" to ObjectMeta("Footwear", "जूता या चप्पल", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.12f, typicalWidthMeters = 0.28f),
        "slipper" to ObjectMeta("Slipper", "चप्पल", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.08f, typicalWidthMeters = 0.25f),
        "belt" to ObjectMeta("Belt", "बेल्ट", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.05f, typicalWidthMeters = 0.3f),
        "hat" to ObjectMeta("Cap / Hat", "टोपी", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.15f, typicalWidthMeters = 0.2f),
        "cap" to ObjectMeta("Cap", "टोपी", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.15f, typicalWidthMeters = 0.2f),

        // Medicines & Health
        "medicine" to ObjectMeta("Medicine", "दवा", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.1f, typicalWidthMeters = 0.08f),
        "pill" to ObjectMeta("Medicine Tablet", "दवा की गोली", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.02f, typicalWidthMeters = 0.02f),
        "tablet" to ObjectMeta("Medicine Tablet", "दवा की गोली", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.05f, typicalWidthMeters = 0.1f),
        "syrup" to ObjectMeta("Syrup Bottle", "दवा की सिरप", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.15f, typicalWidthMeters = 0.06f),
        "bandage" to ObjectMeta("Bandage", "पट्टी / बैंडेज", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.05f, typicalWidthMeters = 0.1f),
        "thermometer" to ObjectMeta("Thermometer", "थर्मामीटर", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.15f, typicalWidthMeters = 0.03f),

        // Kitchen & Dining Utensils
        "plate" to ObjectMeta("Plate / Thali", "थाली / प्लेट", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.03f, typicalWidthMeters = 0.25f),
        "dish" to ObjectMeta("Dish", "प्लेट / बर्तन", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.05f, typicalWidthMeters = 0.25f),
        "spoon" to ObjectMeta("Spoon", "चम्मच", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.15f, typicalWidthMeters = 0.04f),
        "fork" to ObjectMeta("Fork", "कांटा चम्मच", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.15f, typicalWidthMeters = 0.04f),
        "knife" to ObjectMeta("Knife", "चाकू (सावधानी)", ObjectCategory.HAZARD, typicalHeightMeters = 0.2f, typicalWidthMeters = 0.04f, isCriticalHazard = true),
        "bowl" to ObjectMeta("Bowl / Katori", "कटोरी", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.08f, typicalWidthMeters = 0.12f),
        "water bottle" to ObjectMeta("Water Bottle", "पानी की बोतल", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.25f, typicalWidthMeters = 0.08f),
        "glass" to ObjectMeta("Drinking Glass", "पानी का गिलास", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.14f, typicalWidthMeters = 0.08f),
        "mug" to ObjectMeta("Mug", "मग", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.12f, typicalWidthMeters = 0.1f),
        "pan" to ObjectMeta("Pan / Tawa", "तवा / कड़ाही", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.1f, typicalWidthMeters = 0.3f),
        "pot" to ObjectMeta("Pot", "पतीला / बर्तन", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.2f, typicalWidthMeters = 0.25f),

        // Food & Edibles
        "apple" to ObjectMeta("Apple", "सेब", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.08f, typicalWidthMeters = 0.08f),
        "banana" to ObjectMeta("Banana", "केला", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.04f, typicalWidthMeters = 0.18f),
        "orange" to ObjectMeta("Orange", "संतरा", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.08f, typicalWidthMeters = 0.08f),
        "fruit" to ObjectMeta("Fruit", "फल", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.1f, typicalWidthMeters = 0.1f),
        "vegetable" to ObjectMeta("Vegetable", "सब्जी", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.12f, typicalWidthMeters = 0.12f),
        "bread" to ObjectMeta("Bread / Roti", "रोटी / ब्रेड", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.05f, typicalWidthMeters = 0.15f),
        "biscuit" to ObjectMeta("Biscuit", "बिस्कुट", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.05f, typicalWidthMeters = 0.12f),
        "cookie" to ObjectMeta("Biscuit / Cookie", "बिस्कुट", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.05f, typicalWidthMeters = 0.08f),
        "tea" to ObjectMeta("Tea", "चाय", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.1f, typicalWidthMeters = 0.08f),
        "coffee" to ObjectMeta("Coffee", "कॉफी", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.1f, typicalWidthMeters = 0.08f),
        "milk" to ObjectMeta("Milk", "दूध", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.2f, typicalWidthMeters = 0.1f),

        // Stationery & Office Items
        "book" to ObjectMeta("Book", "किताब", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.25f, typicalWidthMeters = 0.18f),
        "notebook" to ObjectMeta("Notebook", "कॉपी / नोटबुक", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.25f, typicalWidthMeters = 0.18f),
        "pen" to ObjectMeta("Pen", "कलम / पेन", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.14f, typicalWidthMeters = 0.015f),
        "pencil" to ObjectMeta("Pencil", "पेंसिल", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.16f, typicalWidthMeters = 0.01f),
        "scissors" to ObjectMeta("Scissors", "कैंची (सावधानी)", ObjectCategory.HAZARD, typicalHeightMeters = 0.18f, typicalWidthMeters = 0.08f, isCriticalHazard = true),
        "paper" to ObjectMeta("Paper / Document", "कागज़ / दस्तावेज़", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.3f, typicalWidthMeters = 0.21f),
        "document" to ObjectMeta("Document", "दस्तावेज़", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.3f, typicalWidthMeters = 0.21f),
        "newspaper" to ObjectMeta("Newspaper", "अखबार", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.35f, typicalWidthMeters = 0.28f),

        // Electronics & Accessories
        "mouse" to ObjectMeta("Computer Mouse", "माउस", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.04f, typicalWidthMeters = 0.06f),
        "keyboard" to ObjectMeta("Keyboard", "कीबोर्ड", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.03f, typicalWidthMeters = 0.45f),
        "remote" to ObjectMeta("Remote Control", "रिमोट कंट्रोल", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.18f, typicalWidthMeters = 0.05f),
        "charger" to ObjectMeta("Phone Charger", "चार्जर", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.06f, typicalWidthMeters = 0.04f),
        "cable" to ObjectMeta("Cable / Wire", "केबल / तार", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.02f, typicalWidthMeters = 0.3f),
        "headphones" to ObjectMeta("Headphones", "हेडफोन", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.18f, typicalWidthMeters = 0.16f),
        "earphones" to ObjectMeta("Earphones", "इयरफोन", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.05f, typicalWidthMeters = 0.05f),
        "power bank" to ObjectMeta("Power Bank", "पावर बैंक", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.12f, typicalWidthMeters = 0.07f),
        "plug" to ObjectMeta("Electric Plug", "बिजली का प्लग", ObjectCategory.HAZARD, typicalHeightMeters = 0.06f, typicalWidthMeters = 0.06f, isCriticalHazard = true),
        "switch" to ObjectMeta("Switch Board", "स्विच बोर्ड", ObjectCategory.ENVIRONMENT, typicalHeightMeters = 0.12f, typicalWidthMeters = 0.12f),
        "fan" to ObjectMeta("Fan", "पंखा", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.5f, typicalWidthMeters = 0.5f),
        "light" to ObjectMeta("Light / Bulb", "बल्ब / लाइट", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.12f, typicalWidthMeters = 0.06f),
        "torch" to ObjectMeta("Torch", "टॉर्च", ObjectCategory.ELECTRONICS, typicalHeightMeters = 0.15f, typicalWidthMeters = 0.04f),

        // Household & Hygiene
        "towel" to ObjectMeta("Towel", "तौलिया", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.6f, typicalWidthMeters = 0.4f),
        "toothbrush" to ObjectMeta("Toothbrush", "टूथब्रश", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.18f, typicalWidthMeters = 0.02f),
        "toothpaste" to ObjectMeta("Toothpaste", "टूथपेस्ट", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.18f, typicalWidthMeters = 0.05f),
        "soap" to ObjectMeta("Soap", "साबुन", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.04f, typicalWidthMeters = 0.08f),
        "comb" to ObjectMeta("Comb", "कंघी", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.18f, typicalWidthMeters = 0.04f),
        "mirror" to ObjectMeta("Mirror", "शीशा / दर्पण", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.5f, typicalWidthMeters = 0.4f),
        "dustbin" to ObjectMeta("Dustbin", "कूड़ेदान", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.4f, typicalWidthMeters = 0.3f),
        "trash can" to ObjectMeta("Dustbin", "कूड़ेदान", ObjectCategory.EVERYDAY, typicalHeightMeters = 0.4f, typicalWidthMeters = 0.3f),
        "pillow" to ObjectMeta("Pillow", "तकिया", ObjectCategory.FURNITURE, typicalHeightMeters = 0.3f, typicalWidthMeters = 0.5f),
        "blanket" to ObjectMeta("Blanket", "कंबल / चादर", ObjectCategory.FURNITURE, typicalHeightMeters = 0.4f, typicalWidthMeters = 0.5f),
        "curtain" to ObjectMeta("Curtain", "पर्दा", ObjectCategory.ENVIRONMENT, typicalHeightMeters = 1.8f, typicalWidthMeters = 1.0f),
        "window" to ObjectMeta("Window", "खिड़की", ObjectCategory.ENVIRONMENT, typicalHeightMeters = 1.2f, typicalWidthMeters = 1.0f),
        "gate" to ObjectMeta("Gate", "गेट / फाटक", ObjectCategory.ENVIRONMENT, typicalHeightMeters = 2.0f, typicalWidthMeters = 1.5f),
        "wall" to ObjectMeta("Wall", "दीवार", ObjectCategory.ENVIRONMENT, typicalHeightMeters = 2.5f, typicalWidthMeters = 3.0f)
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
