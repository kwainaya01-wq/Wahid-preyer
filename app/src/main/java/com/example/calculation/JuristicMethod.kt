package com.example.calculation

enum class JuristicMethod(
    val title: String,
    val description: String,
    val shadowRatio: Double
) {
    SHAFI(
        title = "Standard (Shafi'i, Maliki, Hanbali)",
        description = "Asr time begins when the shadow of an object equals its length (plus noon shadow).",
        shadowRatio = 1.0
    ),
    HANAFI(
        title = "Hanafi",
        description = "Asr time begins when the shadow of an object equals twice its length (plus noon shadow).",
        shadowRatio = 2.0
    )
}
