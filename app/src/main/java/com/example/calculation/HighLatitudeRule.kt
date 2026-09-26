package com.example.calculation

enum class HighLatitudeRule(
    val title: String,
    val description: String
) {
    NONE(
        title = "None / Standard",
        description = "No higher latitude twilight adjustment."
    ),
    MIDNIGHT(
        title = "Middle of the Night",
        description = "Twilight duration cannot exceed half of the night between Sunset and Sunrise."
    ),
    ONE_SEVENTH(
        title = "One-Seventh of the Night",
        description = "Twilight duration cannot exceed 1/7th of the night."
    ),
    ANGLE_BASED(
        title = "Angle-Based",
        description = "Twilight duration is proportioned to twilight angle divided by 60."
    )
}
