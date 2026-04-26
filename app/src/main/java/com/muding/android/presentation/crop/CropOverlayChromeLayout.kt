package com.muding.android.presentation.crop

internal object CropOverlayChromeLayout {
    const val actionButtonTouchTargetDp = 48
    const val actionButtonEdgePaddingDp = 8
    const val hintSideReserveDp = 80

    fun hintMaxWidthDp(screenWidthDp: Int): Int {
        return (screenWidthDp - hintSideReserveDp * 2).coerceAtLeast(0)
    }
}
