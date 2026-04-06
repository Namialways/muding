package com.muding.android.feature.onboarding

import com.muding.android.data.settings.OnboardingGuideProgress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingGuideStateTest {

    @Test
    fun homeGuide_showsOnlyWhenUserHasNotSeenItAndIsOnHomeScreen() {
        val state = OnboardingGuideState.fromProgress(
            OnboardingGuideProgress(
                hasSeenHomeGuide = false,
                hasSeenFloatingBallHint = false,
                hasSeenPinOverlayHint = false,
                hasSeenEditorHint = false
            )
        )

        assertTrue(state.shouldShowHomeGuide(isOnHomeDestination = true))
        assertFalse(state.shouldShowHomeGuide(isOnHomeDestination = false))
        assertFalse(state.markHomeGuideSeen().shouldShowHomeGuide(isOnHomeDestination = true))
    }

    @Test
    fun editorHint_showsOnlyUntilItIsMarkedSeen() {
        val state = OnboardingGuideState.fromProgress(
            OnboardingGuideProgress(
                hasSeenHomeGuide = true,
                hasSeenFloatingBallHint = false,
                hasSeenPinOverlayHint = false,
                hasSeenEditorHint = false
            )
        )

        assertTrue(state.shouldShowEditorHint())
        assertFalse(state.markEditorHintSeen().shouldShowEditorHint())
    }
}
