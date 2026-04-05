package com.muding.android.presentation.main

import com.muding.android.domain.usecase.FloatingBallAppearanceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FloatingBallAppearanceSourceStateTest {

    @Test
    fun selectingCustomImageMode_withoutStoredImage_requestsImagePicking() {
        val state = FloatingBallAppearanceSourceState(
            mode = FloatingBallAppearanceMode.THEME,
            customImageUri = null
        )

        val transition = state.selectCustomImageMode()

        assertEquals(state, transition.state)
        assertEquals(
            FloatingBallAppearanceSourceState.Effect.RequestImagePicker,
            transition.effect
        )
    }

    @Test
    fun restoringDefaultAppearance_clearsCustomImageSettings() {
        val state = FloatingBallAppearanceSourceState(
            mode = FloatingBallAppearanceMode.CUSTOM_IMAGE,
            customImageUri = "content://floating-ball/custom.png"
        )

        val transition = state.restoreDefault()

        assertEquals(FloatingBallAppearanceMode.THEME, transition.state.mode)
        assertEquals(null, transition.state.customImageUri)
        assertEquals(
            FloatingBallAppearanceSourceState.Effect.CommitAppearance(
                mode = FloatingBallAppearanceMode.THEME,
                customImageUri = null
            ),
            transition.effect
        )
    }

    @Test
    fun successfulImageProcessing_switchesStateToCustomImage() {
        val state = FloatingBallAppearanceSourceState(
            mode = FloatingBallAppearanceMode.THEME,
            customImageUri = null
        )

        val transition = state.acceptProcessedImage("content://floating-ball/custom.png")

        assertEquals(FloatingBallAppearanceMode.CUSTOM_IMAGE, transition.state.mode)
        assertEquals("content://floating-ball/custom.png", transition.state.customImageUri)
        assertEquals(
            FloatingBallAppearanceSourceState.Effect.CommitAppearance(
                mode = FloatingBallAppearanceMode.CUSTOM_IMAGE,
                customImageUri = "content://floating-ball/custom.png"
            ),
            transition.effect
        )
    }

    @Test
    fun cancellation_leavesCommittedAppearanceUnchanged() {
        val state = FloatingBallAppearanceSourceState(
            mode = FloatingBallAppearanceMode.THEME,
            customImageUri = null
        )

        val transition = state.cancelImagePicking()

        assertEquals(state, transition.state)
        assertNull(transition.effect)
    }
}
