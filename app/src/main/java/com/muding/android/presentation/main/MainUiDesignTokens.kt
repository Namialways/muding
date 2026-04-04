package com.muding.android.presentation.main

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MainUiPalette(
    val pageBackground: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val surfaceMuted: Color,
    val surfaceAccent: Color,
    val outline: Color,
    val title: Color,
    val body: Color,
    val accent: Color,
    val accentMuted: Color,
    val danger: Color
)

@Immutable
data class MainUiSpacing(
    val pageGutter: Dp = 20.dp,
    val sectionGap: Dp = 18.dp,
    val groupGap: Dp = 14.dp,
    val rowGap: Dp = 10.dp,
    val contentPadding: Dp = 16.dp
)

@Immutable
data class MainUiCorners(
    val card: Dp = 24.dp,
    val group: Dp = 22.dp,
    val tile: Dp = 24.dp,
    val badge: Dp = 999.dp,
    val thumbnail: Dp = 18.dp
)

@Immutable
data class MainUiElevations(
    val flat: Dp = 0.dp,
    val lifted: Dp = 2.dp,
    val preview: Dp = 10.dp
)

@Immutable
data class MainUiTokens(
    val palette: MainUiPalette,
    val spacing: MainUiSpacing,
    val corners: MainUiCorners,
    val elevations: MainUiElevations
)

@Composable
fun rememberMainUiTokens(): MainUiTokens {
    val colorScheme = MaterialTheme.colorScheme
    return remember(colorScheme) {
        MainUiTokens(
            palette = MainUiPalette(
                pageBackground = colorScheme.background,
                surface = colorScheme.surface,
                surfaceStrong = colorScheme.surface,
                surfaceMuted = colorScheme.surfaceVariant.copy(alpha = 0.55f),
                surfaceAccent = colorScheme.primaryContainer.copy(alpha = 0.72f),
                outline = colorScheme.outline.copy(alpha = 0.68f),
                title = colorScheme.onSurface,
                body = colorScheme.onSurfaceVariant,
                accent = colorScheme.primary,
                accentMuted = colorScheme.primaryContainer,
                danger = colorScheme.error
            ),
            spacing = MainUiSpacing(),
            corners = MainUiCorners(),
            elevations = MainUiElevations()
        )
    }
}
