package com.muding.android.domain.usecase

enum class FloatingBallAppearanceMode(val value: String) {
    THEME("theme"),
    CUSTOM_IMAGE("custom_image");

    companion object {
        fun fromValue(value: String?): FloatingBallAppearanceMode {
            return entries.firstOrNull { it.value == value } ?: THEME
        }
    }
}
