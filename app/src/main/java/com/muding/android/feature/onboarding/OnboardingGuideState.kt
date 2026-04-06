package com.muding.android.feature.onboarding

import com.muding.android.data.settings.OnboardingGuideProgress

data class OnboardingGuideState(
    val hasSeenHomeGuide: Boolean,
    val hasSeenFloatingBallHint: Boolean,
    val hasSeenPinOverlayHint: Boolean,
    val hasSeenEditorHint: Boolean
) {
    fun shouldShowHomeGuide(isOnHomeDestination: Boolean): Boolean {
        return isOnHomeDestination && !hasSeenHomeGuide
    }

    fun shouldShowFloatingBallHint(): Boolean = !hasSeenFloatingBallHint

    fun shouldShowPinOverlayHint(): Boolean = !hasSeenPinOverlayHint

    fun shouldShowEditorHint(): Boolean = !hasSeenEditorHint

    fun markHomeGuideSeen(): OnboardingGuideState = copy(hasSeenHomeGuide = true)

    fun markFloatingBallHintSeen(): OnboardingGuideState = copy(hasSeenFloatingBallHint = true)

    fun markPinOverlayHintSeen(): OnboardingGuideState = copy(hasSeenPinOverlayHint = true)

    fun markEditorHintSeen(): OnboardingGuideState = copy(hasSeenEditorHint = true)

    companion object {
        fun fromProgress(progress: OnboardingGuideProgress): OnboardingGuideState {
            return OnboardingGuideState(
                hasSeenHomeGuide = progress.hasSeenHomeGuide,
                hasSeenFloatingBallHint = progress.hasSeenFloatingBallHint,
                hasSeenPinOverlayHint = progress.hasSeenPinOverlayHint,
                hasSeenEditorHint = progress.hasSeenEditorHint
            )
        }
    }
}
