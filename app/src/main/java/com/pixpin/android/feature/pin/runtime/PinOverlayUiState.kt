package com.pixpin.android.feature.pin.runtime

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class PinOverlayUiState(defaultShadowEnabled: Boolean) {
    var uniformScale by mutableStateOf(1f)
    var freeScaleX by mutableStateOf(1f)
    var freeScaleY by mutableStateOf(1f)
    var locked by mutableStateOf(false)
    var controlsVisible by mutableStateOf(false)
    var shadowEnabled by mutableStateOf(defaultShadowEnabled)
    var cornerRadius by mutableStateOf(0f)
    var cornerControlsVisible by mutableStateOf(false)
}
