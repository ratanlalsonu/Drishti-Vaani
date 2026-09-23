package com.example.core.ai

enum class AiVisionMode(
    val titleHi: String,
    val titleEn: String,
    val iconDescription: String
) {
    SCENE(
        titleHi = "पूरा दृश्य समझें",
        titleEn = "Describe Scene",
        iconDescription = "Analyze the whole room or surroundings"
    ),
    CURRENCY(
        titleHi = "नोट / करेंसी पहचानें",
        titleEn = "Check Currency",
        iconDescription = "Identify Indian Rupee currency notes and coins"
    ),
    MEDICINE(
        titleHi = "दवा व एक्सपायरी",
        titleEn = "Medicine & Expiry",
        iconDescription = "Read medicine name, usage and expiry date"
    ),
    COLOR(
        titleHi = "रंग व कपड़े",
        titleEn = "Color & Outfit",
        iconDescription = "Detect garment colors, patterns and match"
    ),
    CUSTOM(
        titleHi = "बोलकर सवाल पूछें",
        titleEn = "Ask Custom Question",
        iconDescription = "Ask any visual question with your voice"
    )
}
