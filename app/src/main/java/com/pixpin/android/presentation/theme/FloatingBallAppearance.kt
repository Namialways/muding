package com.pixpin.android.presentation.theme

import androidx.compose.ui.graphics.Color
import com.pixpin.android.domain.usecase.FloatingBallTheme

data class FloatingBallThemeColors(
    val start: Color,
    val end: Color
)

fun floatingBallThemeColors(theme: FloatingBallTheme): FloatingBallThemeColors {
    return when (theme) {
        FloatingBallTheme.BLUE_PURPLE -> FloatingBallThemeColors(
            start = Color(0xFF667EEA),
            end = Color(0xFF764BA2)
        )

        FloatingBallTheme.SUNSET -> FloatingBallThemeColors(
            start = Color(0xFFFF8A65),
            end = Color(0xFFFF5E62)
        )

        FloatingBallTheme.EMERALD -> FloatingBallThemeColors(
            start = Color(0xFF1DD1A1),
            end = Color(0xFF10AC84)
        )
    }
}
