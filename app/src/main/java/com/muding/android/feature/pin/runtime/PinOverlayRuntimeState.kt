package com.muding.android.feature.pin.runtime

import com.muding.android.domain.usecase.PinScaleMode

class PinOverlayRuntimeState(
    private val scaleMode: PinScaleMode,
    shadowEnabled: Boolean,
    cornerRadiusDp: Float
) {

    var shadowEnabled: Boolean = shadowEnabled
        private set

    var cornerRadiusDp: Float = cornerRadiusDp.coerceIn(MIN_CORNER_RADIUS_DP, MAX_CORNER_RADIUS_DP)
        private set

    private var uniformScale: Float = 1f
    private var freeScaleX: Float = 1f
    private var freeScaleY: Float = 1f

    fun currentScaleX(): Float {
        return if (scaleMode == PinScaleMode.LOCK_ASPECT) uniformScale else freeScaleX
    }

    fun currentScaleY(): Float {
        return if (scaleMode == PinScaleMode.LOCK_ASPECT) uniformScale else freeScaleY
    }

    fun applyScaleGesture(scaleFactorX: Float, scaleFactorY: Float) {
        if (scaleMode == PinScaleMode.LOCK_ASPECT) {
            uniformScale = (uniformScale * scaleFactorX).coerceIn(MIN_SCALE, MAX_SCALE)
            return
        }
        freeScaleX = (freeScaleX * scaleFactorX).coerceIn(MIN_SCALE, MAX_SCALE)
        freeScaleY = (freeScaleY * scaleFactorY).coerceIn(MIN_SCALE, MAX_SCALE)
    }

    fun setUniformScale(scale: Float) {
        uniformScale = scale.coerceIn(MIN_SCALE, MAX_SCALE)
    }

    fun setFreeScale(scaleX: Float, scaleY: Float) {
        freeScaleX = scaleX.coerceIn(MIN_SCALE, MAX_SCALE)
        freeScaleY = scaleY.coerceIn(MIN_SCALE, MAX_SCALE)
    }

    fun updateShadowEnabled(enabled: Boolean) {
        shadowEnabled = enabled
    }

    fun updateCornerRadiusDp(radiusDp: Float) {
        cornerRadiusDp = radiusDp.coerceIn(MIN_CORNER_RADIUS_DP, MAX_CORNER_RADIUS_DP)
    }

    companion object {
        const val MIN_SCALE = 0.25f
        const val MAX_SCALE = 6f
        const val MIN_CORNER_RADIUS_DP = 0f
        const val MAX_CORNER_RADIUS_DP = 48f
    }
}
