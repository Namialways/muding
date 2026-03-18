package com.pixpin.android.feature.pin.runtime

import android.graphics.Bitmap

data class PinOverlaySnapshot(
    val id: String,
    val bitmap: Bitmap,
    val visible: Boolean
)
