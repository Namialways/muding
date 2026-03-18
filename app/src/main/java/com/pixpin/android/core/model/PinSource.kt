package com.pixpin.android.core.model

enum class PinSourceType {
    SCREENSHOT,
    GALLERY_IMAGE,
    CLIPBOARD_TEXT,
    HISTORY_RESTORE,
    EDITOR_EXPORT
}

sealed interface PinSourcePayload {
    data class ImageUri(val uri: String) : PinSourcePayload
    data class ClipboardText(val text: String) : PinSourcePayload
}

data class PinSource(
    val type: PinSourceType,
    val payload: PinSourcePayload
)
