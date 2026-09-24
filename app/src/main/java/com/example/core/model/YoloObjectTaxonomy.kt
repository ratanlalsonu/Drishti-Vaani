package com.example.core.model

import java.util.Locale
import java.util.regex.Pattern

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
    val isCriticalHazard: Boolean = false,
    val isFlora: Boolean = false
) {
    val isLivingBeing: Boolean
        get() = (category == ObjectCategory.PERSON || category == ObjectCategory.ANIMAL || isFlora)
}

object YoloObjectTaxonomy {

    // Comprehensive real-world dictionary mapping COCO, ML Kit, and everyday objects
    // Strictly separated into Living Beings and Inanimate Objects
    private val taxonomyMap: Map<String, ObjectMeta> = mapOf(
        // ==========================================
        // 1. NON-LIVING: ELECTRONICS & APPLIANCES (निर्जीव वस्तुएं)
        // ==========================================
        "microwave" to ObjectMeta("Microwave", "माइक्रोवेव", ObjectCategory.ELECTRONICS, 0.35f, 0.5f),
        "microwave oven" to ObjectMeta("Microwave Oven", "माइक्रोवेव ओवन", ObjectCategory.ELECTRONICS, 0.35f, 0.5f),
        "oven" to ObjectMeta("Oven", "ओवन", ObjectCategory.ELECTRONICS, 0.45f, 0.6f),
        "toaster" to ObjectMeta("Toaster", "टोस्टर", ObjectCategory.ELECTRONICS, 0.2f, 0.25f),
        "refrigerator" to ObjectMeta("Refrigerator", "फ्रिज", ObjectCategory.ELECTRONICS, 1.7f, 0.8f),
        "fridge" to ObjectMeta("Refrigerator", "फ्रिज", ObjectCategory.ELECTRONICS, 1.7f, 0.8f),
        "freezer" to ObjectMeta("Freezer", "फ्रीजर / फ्रिज", ObjectCategory.ELECTRONICS, 1.5f, 0.8f),
        "laptop" to ObjectMeta("Laptop", "लैपटॉप", ObjectCategory.ELECTRONICS, 0.25f, 0.35f),
        "notebook computer" to ObjectMeta("Laptop", "लैपटॉप", ObjectCategory.ELECTRONICS, 0.25f, 0.35f),
        "computer" to ObjectMeta("Computer", "कंप्यूटर", ObjectCategory.ELECTRONICS, 0.45f, 0.5f),
        "personal computer" to ObjectMeta("Computer", "कंप्यूटर", ObjectCategory.ELECTRONICS, 0.45f, 0.5f),
        "desktop" to ObjectMeta("Desktop Computer", "कंप्यूटर", ObjectCategory.ELECTRONICS, 0.45f, 0.5f),
        "pc" to ObjectMeta("Computer", "कंप्यूटर", ObjectCategory.ELECTRONICS, 0.45f, 0.5f),
        "cell phone" to ObjectMeta("Mobile Phone", "मोबाइल फोन", ObjectCategory.ELECTRONICS, 0.15f, 0.08f),
        "mobile phone" to ObjectMeta("Mobile Phone", "मोबाइल फोन", ObjectCategory.ELECTRONICS, 0.15f, 0.08f),
        "phone" to ObjectMeta("Mobile Phone", "मोबाइल फोन", ObjectCategory.ELECTRONICS, 0.15f, 0.08f),
        "smartphone" to ObjectMeta("Mobile Phone", "मोबाइल फोन", ObjectCategory.ELECTRONICS, 0.15f, 0.08f),
        "telephone" to ObjectMeta("Telephone", "टेलीफोन", ObjectCategory.ELECTRONICS, 0.15f, 0.2f),
        "tablet" to ObjectMeta("Tablet Screen", "टैबलेट", ObjectCategory.ELECTRONICS, 0.25f, 0.18f),
        "ipad" to ObjectMeta("Tablet Screen", "टैबलेट", ObjectCategory.ELECTRONICS, 0.25f, 0.18f),
        "tv" to ObjectMeta("TV", "टीवी स्क्रीन", ObjectCategory.ELECTRONICS, 0.7f, 1.1f),
        "television" to ObjectMeta("Television", "टीवी स्क्रीन", ObjectCategory.ELECTRONICS, 0.7f, 1.1f),
        "monitor" to ObjectMeta("Computer Monitor", "मॉनिटर स्क्रीन", ObjectCategory.ELECTRONICS, 0.5f, 0.6f),
        "screen" to ObjectMeta("Display Screen", "स्क्रीन", ObjectCategory.ELECTRONICS, 0.5f, 0.6f),
        "display" to ObjectMeta("Display Screen", "स्क्रीन", ObjectCategory.ELECTRONICS, 0.5f, 0.6f),
        "remote" to ObjectMeta("Remote Control", "रिमोट कंट्रोल", ObjectCategory.ELECTRONICS, 0.18f, 0.05f),
        "remote control" to ObjectMeta("Remote Control", "रिमोट कंट्रोल", ObjectCategory.ELECTRONICS, 0.18f, 0.05f),
        "keyboard" to ObjectMeta("Keyboard", "कीबोर्ड", ObjectCategory.ELECTRONICS, 0.03f, 0.45f),
        "computer keyboard" to ObjectMeta("Keyboard", "कीबोर्ड", ObjectCategory.ELECTRONICS, 0.03f, 0.45f),
        "mouse" to ObjectMeta("Computer Mouse", "कंप्यूटर माउस", ObjectCategory.ELECTRONICS, 0.04f, 0.06f),
        "computer mouse" to ObjectMeta("Computer Mouse", "कंप्यूटर माउस", ObjectCategory.ELECTRONICS, 0.04f, 0.06f),
        "charger" to ObjectMeta("Phone Charger", "फ़ोन चार्जर", ObjectCategory.ELECTRONICS, 0.06f, 0.05f),
        "phone charger" to ObjectMeta("Phone Charger", "फ़ोन चार्जर", ObjectCategory.ELECTRONICS, 0.06f, 0.05f),
        "power bank" to ObjectMeta("Power Bank", "पावर बैंक", ObjectCategory.ELECTRONICS, 0.12f, 0.07f),
        "battery" to ObjectMeta("Battery", "बैटरी", ObjectCategory.ELECTRONICS, 0.05f, 0.08f),
        "cable" to ObjectMeta("Cable / Wire", "केबल या तार", ObjectCategory.ELECTRONICS, 0.02f, 0.3f),
        "wire" to ObjectMeta("Wire / Cable", "तार", ObjectCategory.ELECTRONICS, 0.02f, 0.3f),
        "headphones" to ObjectMeta("Headphones", "हेडफोन", ObjectCategory.ELECTRONICS, 0.18f, 0.16f),
        "earphones" to ObjectMeta("Earphones", "इयरफोन", ObjectCategory.ELECTRONICS, 0.05f, 0.05f),
        "earbuds" to ObjectMeta("Earbuds", "इयरबड्स", ObjectCategory.ELECTRONICS, 0.04f, 0.04f),
        "speaker" to ObjectMeta("Speaker", "स्पीकर", ObjectCategory.ELECTRONICS, 0.2f, 0.15f),
        "fan" to ObjectMeta("Fan", "पंखा", ObjectCategory.ELECTRONICS, 0.5f, 0.5f),
        "ceiling fan" to ObjectMeta("Ceiling Fan", "छत का पंखा", ObjectCategory.ELECTRONICS, 0.5f, 0.9f),
        "light" to ObjectMeta("Light / Bulb", "बल्ब या लाइट", ObjectCategory.ELECTRONICS, 0.12f, 0.06f),
        "lamp" to ObjectMeta("Lamp", "लैंप", ObjectCategory.ELECTRONICS, 0.45f, 0.25f),
        "torch" to ObjectMeta("Torch", "टॉर्च", ObjectCategory.ELECTRONICS, 0.15f, 0.04f),
        "flashlight" to ObjectMeta("Flashlight", "टॉर्च", ObjectCategory.ELECTRONICS, 0.15f, 0.04f),
        "plug" to ObjectMeta("Electric Plug", "बिजली का प्लग (सावधानी)", ObjectCategory.HAZARD, 0.06f, 0.06f, isCriticalHazard = true),
        "switch" to ObjectMeta("Switch Board", "स्विच बोर्ड", ObjectCategory.ENVIRONMENT, 0.12f, 0.12f),
        "socket" to ObjectMeta("Electric Socket", "बिजली का सॉकेट", ObjectCategory.HAZARD, 0.1f, 0.1f, isCriticalHazard = true),

        // ==========================================
        // 2. NON-LIVING: FURNITURE & FIXTURES (फर्नीचर)
        // ==========================================
        "chair" to ObjectMeta("Chair", "कुर्सी", ObjectCategory.FURNITURE, 0.85f, 0.5f),
        "armchair" to ObjectMeta("Armchair", "कुर्सी", ObjectCategory.FURNITURE, 0.85f, 0.7f),
        "office chair" to ObjectMeta("Office Chair", "कुर्सी", ObjectCategory.FURNITURE, 0.9f, 0.6f),
        "wheelchair" to ObjectMeta("Wheelchair", "व्हीलचेयर", ObjectCategory.FURNITURE, 0.9f, 0.7f),
        "table" to ObjectMeta("Table", "मेज़", ObjectCategory.FURNITURE, 0.75f, 1.2f),
        "desk" to ObjectMeta("Desk", "डेस्क या मेज़", ObjectCategory.FURNITURE, 0.75f, 1.1f),
        "dining table" to ObjectMeta("Dining Table", "डाइनिंग मेज़", ObjectCategory.FURNITURE, 0.75f, 1.4f),
        "coffee table" to ObjectMeta("Coffee Table", "छोटी मेज़", ObjectCategory.FURNITURE, 0.45f, 0.9f),
        "bench" to ObjectMeta("Bench", "बेंच", ObjectCategory.FURNITURE, 0.7f, 1.4f),
        "stool" to ObjectMeta("Stool", "स्टूल", ObjectCategory.FURNITURE, 0.45f, 0.35f),
        "sofa" to ObjectMeta("Sofa", "सोफ़ा", ObjectCategory.FURNITURE, 0.85f, 1.8f),
        "couch" to ObjectMeta("Sofa", "सोफ़ा", ObjectCategory.FURNITURE, 0.85f, 1.8f),
        "bed" to ObjectMeta("Bed", "बिस्तर या पलंग", ObjectCategory.FURNITURE, 0.6f, 1.9f),
        "mattress" to ObjectMeta("Mattress", "गद्दा", ObjectCategory.FURNITURE, 0.25f, 1.8f),
        "wardrobe" to ObjectMeta("Wardrobe", "अलमारी", ObjectCategory.FURNITURE, 1.8f, 1.0f),
        "cupboard" to ObjectMeta("Cupboard", "अलमारी", ObjectCategory.FURNITURE, 1.5f, 0.9f),
        "cabinet" to ObjectMeta("Cabinet", "कैबिनेट या अलमारी", ObjectCategory.FURNITURE, 1.2f, 0.9f),
        "almirah" to ObjectMeta("Almirah", "अलमारी", ObjectCategory.FURNITURE, 1.7f, 0.9f),
        "shelf" to ObjectMeta("Shelf", "रैक या शेल्फ", ObjectCategory.FURNITURE, 1.2f, 0.8f),
        "bookshelf" to ObjectMeta("Bookshelf", "किताबों की अलमारी", ObjectCategory.FURNITURE, 1.5f, 0.8f),
        "drawer" to ObjectMeta("Drawer", "दराज", ObjectCategory.FURNITURE, 0.3f, 0.5f),
        "pillow" to ObjectMeta("Pillow", "तकिया", ObjectCategory.FURNITURE, 0.3f, 0.5f),
        "cushion" to ObjectMeta("Cushion", "कुशन या तकिया", ObjectCategory.FURNITURE, 0.35f, 0.35f),
        "blanket" to ObjectMeta("Blanket", "कंबल या चादर", ObjectCategory.FURNITURE, 0.4f, 0.5f),
        "bedsheet" to ObjectMeta("Bedsheet", "चादर", ObjectCategory.FURNITURE, 0.3f, 0.5f),
        "quilt" to ObjectMeta("Quilt", "रजाई", ObjectCategory.FURNITURE, 0.4f, 0.5f),
        "carpet" to ObjectMeta("Carpet", "कालीन या कारपेट", ObjectCategory.FURNITURE, 0.05f, 1.5f),
        "rug" to ObjectMeta("Rug", "कालीन या चटाई", ObjectCategory.FURNITURE, 0.05f, 1.2f),
        "mat" to ObjectMeta("Mat", "चटाई या पायदान", ObjectCategory.FURNITURE, 0.03f, 0.6f),
        "doormat" to ObjectMeta("Doormat", "पायदान", ObjectCategory.FURNITURE, 0.03f, 0.6f),
        "curtain" to ObjectMeta("Curtain", "पर्दा", ObjectCategory.ENVIRONMENT, 1.8f, 1.0f),
        "blinds" to ObjectMeta("Window Blinds", "खिड़की का पर्दा", ObjectCategory.ENVIRONMENT, 1.4f, 0.9f),
        "mirror" to ObjectMeta("Mirror", "शीशा या दर्पण", ObjectCategory.EVERYDAY, 0.6f, 0.4f),

        // ==========================================
        // 3. NON-LIVING: EVERYDAY & KITCHEN ITEMS (दैनिक वस्तुएं व बर्तन)
        // ==========================================
        "bottle" to ObjectMeta("Bottle", "बोतल", ObjectCategory.EVERYDAY, 0.25f, 0.08f),
        "water bottle" to ObjectMeta("Water Bottle", "पानी की बोतल", ObjectCategory.EVERYDAY, 0.25f, 0.08f),
        "thermos" to ObjectMeta("Thermos Flask", "थर्मस", ObjectCategory.EVERYDAY, 0.28f, 0.1f),
        "flask" to ObjectMeta("Flask", "फ्लास्क / बोतल", ObjectCategory.EVERYDAY, 0.25f, 0.09f),
        "cup" to ObjectMeta("Cup", "कप", ObjectCategory.EVERYDAY, 0.12f, 0.1f),
        "coffee cup" to ObjectMeta("Coffee Cup", "कॉफी कप", ObjectCategory.EVERYDAY, 0.12f, 0.1f),
        "tea cup" to ObjectMeta("Tea Cup", "चाय का कप", ObjectCategory.EVERYDAY, 0.1f, 0.09f),
        "mug" to ObjectMeta("Mug", "मग", ObjectCategory.EVERYDAY, 0.12f, 0.1f),
        "glass" to ObjectMeta("Drinking Glass", "पानी का गिलास", ObjectCategory.EVERYDAY, 0.14f, 0.08f),
        "tumbler" to ObjectMeta("Tumbler", "गिलास", ObjectCategory.EVERYDAY, 0.14f, 0.08f),
        "plate" to ObjectMeta("Plate", "थाली या प्लेट", ObjectCategory.EVERYDAY, 0.03f, 0.25f),
        "dish" to ObjectMeta("Dish", "प्लेट या बर्तन", ObjectCategory.EVERYDAY, 0.05f, 0.25f),
        "saucer" to ObjectMeta("Saucer", "तश्तरी", ObjectCategory.EVERYDAY, 0.02f, 0.14f),
        "tray" to ObjectMeta("Tray", "ट्रे", ObjectCategory.EVERYDAY, 0.04f, 0.35f),
        "bowl" to ObjectMeta("Bowl", "कटोरी या कटोरा", ObjectCategory.EVERYDAY, 0.08f, 0.12f),
        "spoon" to ObjectMeta("Spoon", "चम्मच", ObjectCategory.EVERYDAY, 0.15f, 0.04f),
        "fork" to ObjectMeta("Fork", "कांटा चम्मच", ObjectCategory.EVERYDAY, 0.15f, 0.04f),
        "knife" to ObjectMeta("Knife", "चाकू (सावधानी)", ObjectCategory.HAZARD, 0.2f, 0.04f, isCriticalHazard = true),
        "pan" to ObjectMeta("Pan", "तवा या कड़ाही", ObjectCategory.EVERYDAY, 0.1f, 0.3f),
        "frying pan" to ObjectMeta("Frying Pan", "कड़ाही या पैन", ObjectCategory.EVERYDAY, 0.1f, 0.3f),
        "pot" to ObjectMeta("Cooking Pot", "पतीला या बर्तन", ObjectCategory.EVERYDAY, 0.2f, 0.25f),
        "cooker" to ObjectMeta("Pressure Cooker", "प्रेशर कुकर", ObjectCategory.EVERYDAY, 0.25f, 0.25f),
        "pressure cooker" to ObjectMeta("Pressure Cooker", "प्रेशर कुकर", ObjectCategory.EVERYDAY, 0.25f, 0.25f),
        "kettle" to ObjectMeta("Kettle", "केतली", ObjectCategory.EVERYDAY, 0.22f, 0.18f),
        "kitchenware" to ObjectMeta("Kitchen Utensil", "रसोई का बर्तन", ObjectCategory.EVERYDAY, 0.15f, 0.2f),
        "cookware" to ObjectMeta("Cookware", "रसोई का बर्तन", ObjectCategory.EVERYDAY, 0.15f, 0.2f),
        "utensil" to ObjectMeta("Kitchen Utensil", "बर्तन", ObjectCategory.EVERYDAY, 0.15f, 0.15f),
        "tableware" to ObjectMeta("Tableware", "बर्तन या प्लेट", ObjectCategory.EVERYDAY, 0.15f, 0.2f),
        "tap" to ObjectMeta("Water Tap", "नल", ObjectCategory.ENVIRONMENT, 0.15f, 0.1f),
        "faucet" to ObjectMeta("Faucet / Tap", "नल", ObjectCategory.ENVIRONMENT, 0.15f, 0.1f),
        "sink" to ObjectMeta("Sink", "वॉश बेसिन या सिंक", ObjectCategory.ENVIRONMENT, 0.4f, 0.5f),

        // Personal accessories & bags
        "bag" to ObjectMeta("Bag", "बैग", ObjectCategory.EVERYDAY, 0.4f, 0.35f),
        "backpack" to ObjectMeta("Backpack", "बस्ता या बैग", ObjectCategory.EVERYDAY, 0.45f, 0.35f),
        "handbag" to ObjectMeta("Handbag", "पर्स या हैंडबैग", ObjectCategory.EVERYDAY, 0.3f, 0.35f),
        "purse" to ObjectMeta("Purse", "पर्स", ObjectCategory.EVERYDAY, 0.2f, 0.25f),
        "wallet" to ObjectMeta("Wallet", "बटुआ या पर्स", ObjectCategory.EVERYDAY, 0.1f, 0.12f),
        "suitcase" to ObjectMeta("Suitcase", "सूटकेस", ObjectCategory.EVERYDAY, 0.6f, 0.45f),
        "luggage" to ObjectMeta("Luggage", "सामान या सूटकेस", ObjectCategory.EVERYDAY, 0.6f, 0.45f),
        "umbrella" to ObjectMeta("Umbrella", "छाता", ObjectCategory.EVERYDAY, 0.8f, 0.8f),
        "box" to ObjectMeta("Box", "डिब्बा या बॉक्स", ObjectCategory.EVERYDAY, 0.25f, 0.3f),
        "carton" to ObjectMeta("Carton Box", "गत्ते का डिब्बा", ObjectCategory.EVERYDAY, 0.35f, 0.4f),

        // Stationery & Books
        "book" to ObjectMeta("Book", "किताब", ObjectCategory.EVERYDAY, 0.25f, 0.18f),
        "notebook" to ObjectMeta("Notebook", "कॉपी या नोटबुक", ObjectCategory.EVERYDAY, 0.25f, 0.18f),
        "diary" to ObjectMeta("Diary", "डायरी", ObjectCategory.EVERYDAY, 0.2f, 0.15f),
        "pen" to ObjectMeta("Pen", "कलम या पेन", ObjectCategory.EVERYDAY, 0.14f, 0.015f),
        "pencil" to ObjectMeta("Pencil", "पेंसिल", ObjectCategory.EVERYDAY, 0.16f, 0.01f),
        "paper" to ObjectMeta("Paper", "कागज़", ObjectCategory.EVERYDAY, 0.3f, 0.21f),
        "document" to ObjectMeta("Document", "दस्तावेज़", ObjectCategory.EVERYDAY, 0.3f, 0.21f),
        "newspaper" to ObjectMeta("Newspaper", "अखबार", ObjectCategory.EVERYDAY, 0.35f, 0.28f),
        "scissors" to ObjectMeta("Scissors", "कैंची (सावधानी)", ObjectCategory.HAZARD, 0.18f, 0.08f, isCriticalHazard = true),

        // Wearables & Clothing
        "glasses" to ObjectMeta("Glasses", "चश्मा", ObjectCategory.EVERYDAY, 0.05f, 0.14f),
        "spectacles" to ObjectMeta("Spectacles", "चश्मा", ObjectCategory.EVERYDAY, 0.05f, 0.14f),
        "sunglasses" to ObjectMeta("Sunglasses", "धूप का चश्मा", ObjectCategory.EVERYDAY, 0.05f, 0.14f),
        "watch" to ObjectMeta("Wristwatch", "हाथ की घड़ी", ObjectCategory.EVERYDAY, 0.06f, 0.05f),
        "clock" to ObjectMeta("Clock", "दीवार घड़ी", ObjectCategory.EVERYDAY, 0.3f, 0.3f),
        "shoe" to ObjectMeta("Shoe", "जूता", ObjectCategory.EVERYDAY, 0.12f, 0.28f),
        "shoes" to ObjectMeta("Shoes", "जूते", ObjectCategory.EVERYDAY, 0.12f, 0.35f),
        "footwear" to ObjectMeta("Footwear", "जूते या चप्पल", ObjectCategory.EVERYDAY, 0.12f, 0.3f),
        "slipper" to ObjectMeta("Slipper", "चप्पल", ObjectCategory.EVERYDAY, 0.08f, 0.25f),
        "slippers" to ObjectMeta("Slippers", "चप्पल", ObjectCategory.EVERYDAY, 0.08f, 0.3f),
        "sandal" to ObjectMeta("Sandal", "सैंडल", ObjectCategory.EVERYDAY, 0.08f, 0.25f),
        "clothes" to ObjectMeta("Clothes", "कपड़े", ObjectCategory.EVERYDAY, 0.6f, 0.5f),
        "clothing" to ObjectMeta("Clothing", "कपड़े", ObjectCategory.EVERYDAY, 0.6f, 0.5f),
        "shirt" to ObjectMeta("Shirt", "कमीज या शर्ट", ObjectCategory.EVERYDAY, 0.6f, 0.5f),
        "t-shirt" to ObjectMeta("T-Shirt", "टी-शर्ट", ObjectCategory.EVERYDAY, 0.6f, 0.5f),
        "pants" to ObjectMeta("Pants", "पैंट", ObjectCategory.EVERYDAY, 0.9f, 0.4f),
        "trousers" to ObjectMeta("Trousers", "पैंट", ObjectCategory.EVERYDAY, 0.9f, 0.4f),
        "jacket" to ObjectMeta("Jacket", "जैकेट", ObjectCategory.EVERYDAY, 0.7f, 0.5f),
        "hat" to ObjectMeta("Hat / Cap", "टोपी", ObjectCategory.EVERYDAY, 0.15f, 0.2f),
        "cap" to ObjectMeta("Cap", "टोपी", ObjectCategory.EVERYDAY, 0.15f, 0.2f),

        // Personal Hygiene & Medicine
        "towel" to ObjectMeta("Towel", "तौलिया", ObjectCategory.EVERYDAY, 0.6f, 0.4f),
        "toothbrush" to ObjectMeta("Toothbrush", "टूथब्रश", ObjectCategory.EVERYDAY, 0.18f, 0.02f),
        "toothpaste" to ObjectMeta("Toothpaste", "टूथपेस्ट", ObjectCategory.EVERYDAY, 0.18f, 0.05f),
        "soap" to ObjectMeta("Soap", "साबुन", ObjectCategory.EVERYDAY, 0.04f, 0.08f),
        "shampoo" to ObjectMeta("Shampoo Bottle", "शैम्पू", ObjectCategory.EVERYDAY, 0.2f, 0.07f),
        "comb" to ObjectMeta("Comb", "कंघी", ObjectCategory.EVERYDAY, 0.18f, 0.04f),
        "sanitizer" to ObjectMeta("Hand Sanitizer", "सैनिटाइज़र", ObjectCategory.EVERYDAY, 0.15f, 0.06f),
        "medicine" to ObjectMeta("Medicine", "दवा", ObjectCategory.EVERYDAY, 0.1f, 0.08f),
        "pill" to ObjectMeta("Medicine Tablet", "दवा की गोली", ObjectCategory.EVERYDAY, 0.02f, 0.02f),
        "tablet" to ObjectMeta("Medicine Tablet", "दवा की गोली", ObjectCategory.EVERYDAY, 0.05f, 0.1f),
        "syrup" to ObjectMeta("Medicine Syrup", "दवा की सिरप", ObjectCategory.EVERYDAY, 0.15f, 0.06f),
        "bandage" to ObjectMeta("Bandage", "पट्टी या बैंडेज", ObjectCategory.EVERYDAY, 0.05f, 0.1f),
        "thermometer" to ObjectMeta("Thermometer", "थर्मामीटर", ObjectCategory.EVERYDAY, 0.15f, 0.03f),

        // Keys & Money
        "key" to ObjectMeta("Key", "चाबी", ObjectCategory.EVERYDAY, 0.06f, 0.03f),
        "keys" to ObjectMeta("Keys", "चाबियां", ObjectCategory.EVERYDAY, 0.08f, 0.05f),
        "lock" to ObjectMeta("Lock", "ताला", ObjectCategory.EVERYDAY, 0.1f, 0.07f),
        "money" to ObjectMeta("Money / Cash", "रुपया या पैसे", ObjectCategory.EVERYDAY, 0.08f, 0.15f),
        "banknote" to ObjectMeta("Currency Note", "रुपये का नोट", ObjectCategory.EVERYDAY, 0.08f, 0.15f),
        "cash" to ObjectMeta("Cash", "रुपया", ObjectCategory.EVERYDAY, 0.08f, 0.15f),
        "coin" to ObjectMeta("Coin", "सिक्का", ObjectCategory.EVERYDAY, 0.03f, 0.03f),

        // Waste & Cleaning
        "dustbin" to ObjectMeta("Dustbin", "कूड़ेदान", ObjectCategory.EVERYDAY, 0.4f, 0.3f),
        "trash can" to ObjectMeta("Dustbin", "कूड़ेदान", ObjectCategory.EVERYDAY, 0.4f, 0.3f),
        "wastebasket" to ObjectMeta("Dustbin", "कूड़ेदान", ObjectCategory.EVERYDAY, 0.35f, 0.28f),
        "broom" to ObjectMeta("Broom", "झाड़ू", ObjectCategory.EVERYDAY, 0.9f, 0.2f),
        "mop" to ObjectMeta("Mop", "पोछा", ObjectCategory.EVERYDAY, 1.1f, 0.25f),
        "bucket" to ObjectMeta("Bucket", "बाल्टी", ObjectCategory.EVERYDAY, 0.35f, 0.32f),

        // Food & Edibles
        "food" to ObjectMeta("Food Item", "खाद्य पदार्थ", ObjectCategory.EVERYDAY, 0.15f, 0.15f),
        "fruit" to ObjectMeta("Fruit", "फल", ObjectCategory.EVERYDAY, 0.1f, 0.1f),
        "apple" to ObjectMeta("Apple", "सेब", ObjectCategory.EVERYDAY, 0.08f, 0.08f),
        "banana" to ObjectMeta("Banana", "केला", ObjectCategory.EVERYDAY, 0.04f, 0.18f),
        "orange" to ObjectMeta("Orange", "संतरा", ObjectCategory.EVERYDAY, 0.08f, 0.08f),
        "bread" to ObjectMeta("Bread", "रोटी या ब्रेड", ObjectCategory.EVERYDAY, 0.06f, 0.15f),
        "biscuit" to ObjectMeta("Biscuit", "बिस्कुट", ObjectCategory.EVERYDAY, 0.04f, 0.1f),

        // ==========================================
        // 4. NON-LIVING: ARCHITECTURE & VEHICLES (संरचना व वाहन)
        // ==========================================
        "door" to ObjectMeta("Door", "दरवाज़ा", ObjectCategory.ENVIRONMENT, 2.0f, 0.9f),
        "door handle" to ObjectMeta("Door Handle", "दरवाजे का हैंडल", ObjectCategory.ENVIRONMENT, 0.15f, 0.1f),
        "doorknob" to ObjectMeta("Doorknob", "दरवाजे का हैंडल", ObjectCategory.ENVIRONMENT, 0.08f, 0.08f),
        "handle" to ObjectMeta("Handle", "हैंडल", ObjectCategory.ENVIRONMENT, 0.12f, 0.08f),
        "window" to ObjectMeta("Window", "खिड़की", ObjectCategory.ENVIRONMENT, 1.2f, 1.0f),
        "wall" to ObjectMeta("Wall", "दीवार", ObjectCategory.ENVIRONMENT, 2.5f, 3.0f),
        "gate" to ObjectMeta("Gate", "गेट या फाटक", ObjectCategory.ENVIRONMENT, 2.0f, 1.5f),
        "floor" to ObjectMeta("Floor", "फर्श", ObjectCategory.ENVIRONMENT, 0.1f, 2.0f),
        "ceiling" to ObjectMeta("Ceiling", "छत", ObjectCategory.ENVIRONMENT, 0.1f, 2.0f),
        "stairs" to ObjectMeta("Stairs", "सीढ़ियां (सावधानी)", ObjectCategory.HAZARD, 1.2f, 1.2f, isCriticalHazard = true),
        "steps" to ObjectMeta("Steps", "सीढ़ियां (सावधानी)", ObjectCategory.HAZARD, 1.0f, 1.2f, isCriticalHazard = true),
        "step" to ObjectMeta("Step", "कदम या पायदान", ObjectCategory.HAZARD, 0.3f, 1.0f, isCriticalHazard = true),
        "pole" to ObjectMeta("Pole", "खंभा (Pole)", ObjectCategory.HAZARD, 2.5f, 0.3f, isCriticalHazard = true),
        "post" to ObjectMeta("Post", "खंभा", ObjectCategory.HAZARD, 2.0f, 0.3f),
        "pothole" to ObjectMeta("Pothole", "सड़क पर गड्ढा", ObjectCategory.HAZARD, 0.3f, 0.8f, isCriticalHazard = true),
        "traffic light" to ObjectMeta("Traffic Light", "ट्रैफिक लाइट", ObjectCategory.HAZARD, 1.0f, 0.4f),
        "stop sign" to ObjectMeta("Stop Sign", "स्टॉप साइन", ObjectCategory.HAZARD, 0.8f, 0.8f),

        // Vehicles
        "car" to ObjectMeta("Car", "कार", ObjectCategory.VEHICLE, 1.5f, 1.8f, isCriticalHazard = true),
        "automobile" to ObjectMeta("Car", "कार", ObjectCategory.VEHICLE, 1.5f, 1.8f, isCriticalHazard = true),
        "vehicle" to ObjectMeta("Vehicle", "गाड़ी", ObjectCategory.VEHICLE, 1.6f, 1.9f, isCriticalHazard = true),
        "bicycle" to ObjectMeta("Bicycle", "साइकिल", ObjectCategory.VEHICLE, 1.0f, 1.5f),
        "bike" to ObjectMeta("Bicycle / Bike", "साइकिल या बाइक", ObjectCategory.VEHICLE, 1.0f, 1.5f),
        "motorcycle" to ObjectMeta("Motorcycle", "मोटरसाइकिल", ObjectCategory.VEHICLE, 1.1f, 1.8f, isCriticalHazard = true),
        "motorbike" to ObjectMeta("Motorcycle", "मोटरसाइकिल", ObjectCategory.VEHICLE, 1.1f, 1.8f, isCriticalHazard = true),
        "scooter" to ObjectMeta("Scooter", "स्कूटर", ObjectCategory.VEHICLE, 1.1f, 1.6f, isCriticalHazard = true),
        "auto rickshaw" to ObjectMeta("Auto Rickshaw", "ऑटो रिक्शा", ObjectCategory.VEHICLE, 1.8f, 1.4f, isCriticalHazard = true),
        "rickshaw" to ObjectMeta("Rickshaw", "रिक्शा", ObjectCategory.VEHICLE, 1.7f, 1.3f),
        "bus" to ObjectMeta("Bus", "बस", ObjectCategory.VEHICLE, 3.2f, 2.5f, isCriticalHazard = true),
        "truck" to ObjectMeta("Truck", "ट्रक", ObjectCategory.VEHICLE, 3.5f, 2.5f, isCriticalHazard = true),

        // ML Kit Coarse Fallback Classes (Strictly Inanimate)
        "home good" to ObjectMeta("Household Item", "घरेलू सामान", ObjectCategory.FURNITURE, 0.8f, 0.8f),
        "fashion good" to ObjectMeta("Fashion Item", "वस्त्र या सामान", ObjectCategory.EVERYDAY, 0.5f, 0.5f),
        "place" to ObjectMeta("Structure", "दीवार या ढांचा", ObjectCategory.ENVIRONMENT, 2.0f, 2.0f),

        // ==========================================
        // 5. LIVING BEINGS: HUMANS (सजीव मनुष्य)
        // ==========================================
        "person" to ObjectMeta("Person", "व्यक्ति", ObjectCategory.PERSON, 1.7f, 0.5f),
        "human" to ObjectMeta("Person", "व्यक्ति", ObjectCategory.PERSON, 1.7f, 0.5f),
        "people" to ObjectMeta("People", "लोग या व्यक्ति", ObjectCategory.PERSON, 1.7f, 1.2f),
        "crowd" to ObjectMeta("Crowd", "भीड़", ObjectCategory.PERSON, 1.7f, 2.0f),
        "man" to ObjectMeta("Man", "व्यक्ति", ObjectCategory.PERSON, 1.75f, 0.5f),
        "men" to ObjectMeta("Men", "लोग", ObjectCategory.PERSON, 1.75f, 1.0f),
        "woman" to ObjectMeta("Woman", "महिला", ObjectCategory.PERSON, 1.6f, 0.5f),
        "women" to ObjectMeta("Women", "महिलाएं", ObjectCategory.PERSON, 1.6f, 1.0f),
        "lady" to ObjectMeta("Woman", "महिला", ObjectCategory.PERSON, 1.6f, 0.5f),
        "gentleman" to ObjectMeta("Man", "व्यक्ति", ObjectCategory.PERSON, 1.75f, 0.5f),
        "child" to ObjectMeta("Child", "बच्चा", ObjectCategory.PERSON, 1.1f, 0.4f),
        "children" to ObjectMeta("Children", "बच्चे", ObjectCategory.PERSON, 1.1f, 0.8f),
        "kid" to ObjectMeta("Child", "बच्चा", ObjectCategory.PERSON, 1.1f, 0.4f),
        "kids" to ObjectMeta("Children", "बच्चे", ObjectCategory.PERSON, 1.1f, 0.8f),
        "boy" to ObjectMeta("Boy", "लड़का", ObjectCategory.PERSON, 1.3f, 0.4f),
        "girl" to ObjectMeta("Girl", "लड़की", ObjectCategory.PERSON, 1.3f, 0.4f),
        "baby" to ObjectMeta("Baby", "छोटा बच्चा", ObjectCategory.PERSON, 0.7f, 0.35f),
        "infant" to ObjectMeta("Infant", "छोटा बच्चा", ObjectCategory.PERSON, 0.6f, 0.35f),
        "toddler" to ObjectMeta("Toddler", "छोटा बच्चा", ObjectCategory.PERSON, 0.8f, 0.35f),
        "face" to ObjectMeta("Person's Face", "व्यक्ति का चेहरा", ObjectCategory.PERSON, 0.25f, 0.18f),
        "human face" to ObjectMeta("Person's Face", "व्यक्ति का चेहरा", ObjectCategory.PERSON, 0.25f, 0.18f),
        "pedestrian" to ObjectMeta("Pedestrian", "राहगीर / व्यक्ति", ObjectCategory.PERSON, 1.7f, 0.5f),

        // ==========================================
        // 6. LIVING BEINGS: ANIMALS & BIRDS (सजीव पशु-पक्षी)
        // ==========================================
        "dog" to ObjectMeta("Dog", "कुत्ता", ObjectCategory.ANIMAL, 0.6f, 0.8f),
        "puppy" to ObjectMeta("Puppy", "पिल्ला या छोटा कुत्ता", ObjectCategory.ANIMAL, 0.3f, 0.4f),
        "hound" to ObjectMeta("Dog", "कुत्ता", ObjectCategory.ANIMAL, 0.65f, 0.85f),
        "canine" to ObjectMeta("Dog", "कुत्ता", ObjectCategory.ANIMAL, 0.6f, 0.8f),
        "cat" to ObjectMeta("Cat", "बिल्ली", ObjectCategory.ANIMAL, 0.3f, 0.4f),
        "kitten" to ObjectMeta("Kitten", "बिल्ली का बच्चा", ObjectCategory.ANIMAL, 0.2f, 0.25f),
        "feline" to ObjectMeta("Cat", "बिल्ली", ObjectCategory.ANIMAL, 0.3f, 0.4f),
        "cow" to ObjectMeta("Cow", "गाय (सावधानी)", ObjectCategory.ANIMAL, 1.5f, 1.8f, isCriticalHazard = true),
        "cattle" to ObjectMeta("Cattle / Cow", "गाय या बैल", ObjectCategory.ANIMAL, 1.5f, 1.8f, isCriticalHazard = true),
        "bull" to ObjectMeta("Bull", "बैल (सावधानी)", ObjectCategory.ANIMAL, 1.6f, 2.0f, isCriticalHazard = true),
        "ox" to ObjectMeta("Ox", "बैल", ObjectCategory.ANIMAL, 1.5f, 1.9f, isCriticalHazard = true),
        "buffalo" to ObjectMeta("Buffalo", "भैंस", ObjectCategory.ANIMAL, 1.5f, 2.0f, isCriticalHazard = true),
        "goat" to ObjectMeta("Goat", "बकरी", ObjectCategory.ANIMAL, 0.7f, 0.8f),
        "sheep" to ObjectMeta("Sheep", "भेड़", ObjectCategory.ANIMAL, 0.75f, 0.9f),
        "lamb" to ObjectMeta("Lamb", "भेड़ का बच्चा", ObjectCategory.ANIMAL, 0.4f, 0.5f),
        "horse" to ObjectMeta("Horse", "घोड़ा", ObjectCategory.ANIMAL, 1.6f, 2.0f, isCriticalHazard = true),
        "donkey" to ObjectMeta("Donkey", "गधा", ObjectCategory.ANIMAL, 1.2f, 1.4f),
        "monkey" to ObjectMeta("Monkey", "बंदर (सावधानी)", ObjectCategory.ANIMAL, 0.6f, 0.5f, isCriticalHazard = true),
        "ape" to ObjectMeta("Monkey", "बंदर", ObjectCategory.ANIMAL, 0.8f, 0.6f),
        "pig" to ObjectMeta("Pig", "सूअर", ObjectCategory.ANIMAL, 0.6f, 0.9f),
        "camel" to ObjectMeta("Camel", "ऊंट", ObjectCategory.ANIMAL, 2.2f, 2.4f, isCriticalHazard = true),
        "elephant" to ObjectMeta("Elephant", "हाथी (सावधानी)", ObjectCategory.ANIMAL, 3.0f, 3.5f, isCriticalHazard = true),
        "deer" to ObjectMeta("Deer", "हिरण", ObjectCategory.ANIMAL, 1.2f, 1.4f),
        "rabbit" to ObjectMeta("Rabbit", "खरगोश", ObjectCategory.ANIMAL, 0.3f, 0.3f),
        "squirrel" to ObjectMeta("Squirrel", "गिलहरी", ObjectCategory.ANIMAL, 0.15f, 0.2f),
        "rat" to ObjectMeta("Rat", "चूहा", ObjectCategory.ANIMAL, 0.1f, 0.18f),
        "rodent" to ObjectMeta("Rodent", "चूहा", ObjectCategory.ANIMAL, 0.12f, 0.15f),
        "animal" to ObjectMeta("Animal", "सजीव प्राणी / जानवर", ObjectCategory.ANIMAL, 0.8f, 1.0f),
        "pet" to ObjectMeta("Pet Animal", "पालतू जानवर", ObjectCategory.ANIMAL, 0.5f, 0.6f),
        "mammal" to ObjectMeta("Mammal Animal", "जानवर", ObjectCategory.ANIMAL, 0.8f, 1.0f),

        // Birds
        "bird" to ObjectMeta("Bird", "पक्षी या चिड़िया", ObjectCategory.ANIMAL, 0.2f, 0.25f),
        "pigeon" to ObjectMeta("Pigeon", "कबूतर", ObjectCategory.ANIMAL, 0.25f, 0.25f),
        "dove" to ObjectMeta("Dove", "कबूतर या फाख्ता", ObjectCategory.ANIMAL, 0.25f, 0.25f),
        "sparrow" to ObjectMeta("Sparrow", "गौरैया चिड़िया", ObjectCategory.ANIMAL, 0.12f, 0.12f),
        "crow" to ObjectMeta("Crow", "कौवा", ObjectCategory.ANIMAL, 0.3f, 0.35f),
        "parrot" to ObjectMeta("Parrot", "तोता", ObjectCategory.ANIMAL, 0.25f, 0.15f),
        "duck" to ObjectMeta("Duck", "बतख", ObjectCategory.ANIMAL, 0.35f, 0.4f),
        "chicken" to ObjectMeta("Chicken", "मुर्गी", ObjectCategory.ANIMAL, 0.35f, 0.35f),
        "hen" to ObjectMeta("Hen", "मुर्गी", ObjectCategory.ANIMAL, 0.35f, 0.35f),
        "rooster" to ObjectMeta("Rooster", "मुर्गा", ObjectCategory.ANIMAL, 0.4f, 0.35f),
        "peacock" to ObjectMeta("Peacock", "मोर", ObjectCategory.ANIMAL, 0.8f, 1.2f),
        "eagle" to ObjectMeta("Eagle", "चील या बाज", ObjectCategory.ANIMAL, 0.5f, 0.8f),

        // Insects
        "insect" to ObjectMeta("Insect", "कीड़ा", ObjectCategory.ANIMAL, 0.03f, 0.03f),
        "butterfly" to ObjectMeta("Butterfly", "तितली", ObjectCategory.ANIMAL, 0.06f, 0.08f),
        "bee" to ObjectMeta("Bee", "मधुमक्खी", ObjectCategory.ANIMAL, 0.02f, 0.02f),
        "spider" to ObjectMeta("Spider", "मकड़ी", ObjectCategory.ANIMAL, 0.04f, 0.04f),

        // ==========================================
        // 7. LIVING FLORA: PLANTS & TREES (सजीव वनस्पति)
        // ==========================================
        "plant" to ObjectMeta("Plant", "पौधा या गमला", ObjectCategory.ENVIRONMENT, 0.7f, 0.5f, isFlora = true),
        "houseplant" to ObjectMeta("Houseplant", "घर का पौधा", ObjectCategory.ENVIRONMENT, 0.6f, 0.5f, isFlora = true),
        "potted plant" to ObjectMeta("Potted Plant", "गमला व पौधा", ObjectCategory.ENVIRONMENT, 0.6f, 0.5f, isFlora = true),
        "flowerpot" to ObjectMeta("Flowerpot", "फूल का गमला", ObjectCategory.ENVIRONMENT, 0.5f, 0.4f, isFlora = true),
        "tree" to ObjectMeta("Tree", "पेड़ या वृक्ष", ObjectCategory.ENVIRONMENT, 3.5f, 2.0f, isCriticalHazard = true, isFlora = true),
        "shrub" to ObjectMeta("Shrub", "झाड़ी या पौधा", ObjectCategory.ENVIRONMENT, 0.8f, 0.8f, isFlora = true),
        "bush" to ObjectMeta("Bush", "झाड़ी", ObjectCategory.ENVIRONMENT, 0.8f, 0.8f, isFlora = true),
        "flower" to ObjectMeta("Flower", "फूल", ObjectCategory.ENVIRONMENT, 0.15f, 0.15f, isFlora = true),
        "rose" to ObjectMeta("Rose", "गुलाब का फूल", ObjectCategory.ENVIRONMENT, 0.12f, 0.12f, isFlora = true),
        "grass" to ObjectMeta("Grass", "घास", ObjectCategory.ENVIRONMENT, 0.08f, 1.0f, isFlora = true),
        "foliage" to ObjectMeta("Foliage", "हरियाली या पौधे", ObjectCategory.ENVIRONMENT, 1.0f, 1.0f, isFlora = true)
    )

    // Precomputed multi-word keys sorted by descending word count and length
    private val multiWordKeys: List<String> = taxonomyMap.keys
        .filter { it.contains(" ") }
        .sortedWith(compareByDescending<String> { it.split(" ").size }.thenByDescending { it.length })

    /**
     * Accurately resolves a raw label from ML Kit or Object Detector.
     * Uses strict whole-token and boundary matching so that common substrings
     * (e.g. 'pet' in 'carpet', 'crow' in 'microwave', 'rat' in 'refrigerator',
     *  'hen' in 'kitchen', 'man' in 'ottoman') NEVER cause false positive living classifications!
     */
    fun resolveLabel(rawText: String): ObjectMeta {
        val clean = rawText.trim().lowercase(Locale.ROOT)
        if (clean.isBlank()) {
            return ObjectMeta("Obstacle", "सामने रुकावट", ObjectCategory.OBSTACLE, 0.8f, 0.8f)
        }

        // 1. Direct exact match in dictionary
        taxonomyMap[clean]?.let { return it }

        // 2. Multi-word phrase matching with word boundaries (\b...\b)
        for (multiWord in multiWordKeys) {
            val pattern = "\\b" + Pattern.quote(multiWord) + "\\b"
            if (Pattern.compile(pattern).matcher(clean).find()) {
                taxonomyMap[multiWord]?.let { return it }
            }
        }

        // 3. Tokenize by non-alphanumeric delimiters into discrete words
        val tokens = clean.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }

        // Check tokens against taxonomy
        for (token in tokens) {
            taxonomyMap[token]?.let { return it }
        }

        // 4. Check standard English plurals (chairs -> chair, bottles -> bottle, shoes -> shoe)
        for (token in tokens) {
            if (token.endsWith("s") && token.length > 2) {
                val singular = token.substring(0, token.length - 1)
                taxonomyMap[singular]?.let { return it }
            }
            if (token.endsWith("es") && token.length > 3) {
                val singular = token.substring(0, token.length - 2)
                taxonomyMap[singular]?.let { return it }
            }
        }

        // 5. Coarse ML Kit category fallbacks
        if (clean.contains("home good") || clean.contains("household")) {
            return taxonomyMap["home good"]!!
        }
        if (clean.contains("fashion good") || clean.contains("clothing")) {
            return taxonomyMap["fashion good"]!!
        }
        if (clean.contains("food")) {
            return taxonomyMap["food"]!!
        }
        if (clean.contains("place")) {
            return taxonomyMap["place"]!!
        }
        if (clean.contains("plant")) {
            return taxonomyMap["plant"]!!
        }

        // 6. Safe Inanimate Fallback: An unknown detected item is categorized as an OBSTACLE (निर्जीव वस्तु)
        // Never classify an unknown object as a person or animal!
        val capitalized = clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        return ObjectMeta(
            englishName = capitalized,
            hindiName = "सामने वस्तु ($capitalized)",
            category = ObjectCategory.OBSTACLE,
            typicalHeightMeters = 0.8f,
            typicalWidthMeters = 0.8f,
            isCriticalHazard = false,
            isFlora = false
        )
    }

    /**
     * Calculates distance using pinhole camera geometry and physical object scale.
     */
    fun estimateDistance(
        meta: ObjectMeta,
        heightRatio: Float,
        widthRatio: Float
    ): Float {
        val clampedH = heightRatio.coerceIn(0.01f, 1.0f)
        val clampedW = widthRatio.coerceIn(0.01f, 1.0f)

        // Android smartphone vertical FOV is roughly 55-65 degrees, focal factor ~ 1.15
        val distanceByHeight = (meta.typicalHeightMeters * 1.15f) / clampedH
        val distanceByWidth = (meta.typicalWidthMeters * 1.15f) / clampedW

        // Combine weighted towards height because width varies with viewing angle
        val estimatedRaw = (distanceByHeight * 0.7f + distanceByWidth * 0.3f)

        // Clamp between 0.3m and 30m
        val clamped = estimatedRaw.coerceIn(0.3f, 30.0f)
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
