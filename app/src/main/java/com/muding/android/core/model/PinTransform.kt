package com.muding.android.core.model

data class PinTransform(
    val uniformScale: Float = 1f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val lockedAspectRatio: Boolean = true,
    val shadowEnabled: Boolean = true,
    val cornerRadius: Float = 0f
)
