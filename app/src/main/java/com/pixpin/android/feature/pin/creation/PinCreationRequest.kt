package com.pixpin.android.feature.pin.creation

import com.pixpin.android.core.model.PinImageAsset
import com.pixpin.android.core.model.PinSource
import com.pixpin.android.domain.usecase.PinHistoryMetadata

data class ImagePinCreationRequest(
    val source: PinSource,
    val imageAsset: PinImageAsset,
    val annotationSessionId: String? = null,
    val historyMetadata: PinHistoryMetadata = PinHistoryMetadata()
)

data class EditorLaunchRequest(
    val imageUri: String? = null,
    val annotationSessionId: String? = null
) {
    init {
        require(!imageUri.isNullOrBlank() || !annotationSessionId.isNullOrBlank()) {
            "Editor launch requires an image uri or annotation session id."
        }
    }
}
