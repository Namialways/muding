package com.muding.android.presentation.main

import com.muding.android.domain.usecase.FloatingBallAppearanceMode

internal data class FloatingBallAppearanceSourceState(
    val mode: FloatingBallAppearanceMode,
    val customImageUri: String?
) {

    fun selectCustomImageMode(): Transition {
        if (customImageUri.isNullOrBlank()) {
            return Transition(
                state = this,
                effect = Effect.RequestImagePicker
            )
        }
        val updatedState = copy(mode = FloatingBallAppearanceMode.CUSTOM_IMAGE)
        return Transition(
            state = updatedState,
            effect = Effect.CommitAppearance(
                mode = updatedState.mode,
                customImageUri = updatedState.customImageUri
            )
        )
    }

    fun restoreDefault(): Transition {
        val updatedState = copy(
            mode = FloatingBallAppearanceMode.THEME,
            customImageUri = null
        )
        return Transition(
            state = updatedState,
            effect = Effect.CommitAppearance(
                mode = updatedState.mode,
                customImageUri = updatedState.customImageUri
            )
        )
    }

    fun acceptProcessedImage(imageUri: String): Transition {
        val normalizedUri = imageUri.takeIf { it.isNotBlank() } ?: return Transition(this)
        val updatedState = copy(
            mode = FloatingBallAppearanceMode.CUSTOM_IMAGE,
            customImageUri = normalizedUri
        )
        return Transition(
            state = updatedState,
            effect = Effect.CommitAppearance(
                mode = updatedState.mode,
                customImageUri = updatedState.customImageUri
            )
        )
    }

    fun cancelImagePicking(): Transition = Transition(this)

    internal data class Transition(
        val state: FloatingBallAppearanceSourceState,
        val effect: Effect? = null
    )

    internal sealed interface Effect {
        data object RequestImagePicker : Effect

        data class CommitAppearance(
            val mode: FloatingBallAppearanceMode,
            val customImageUri: String?
        ) : Effect
    }
}
