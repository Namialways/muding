package com.muding.android.feature.pin.runtime

import android.graphics.Bitmap

data class PinOverlaySnapshot(
    val id: String,
    val bitmap: Bitmap,
    val visible: Boolean
)
