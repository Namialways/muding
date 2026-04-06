package com.muding.android.data.settings

data class OnboardingGuideProgress(
    val hasSeenHomeGuide: Boolean,
    val hasSeenFloatingBallHint: Boolean,
    val hasSeenPinOverlayHint: Boolean,
    val hasSeenEditorHint: Boolean
)
