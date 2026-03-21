package com.muding.android.feature.pin.creation

import com.muding.android.core.model.PinImageAsset
import com.muding.android.core.model.PinSource
import com.muding.android.domain.usecase.PinHistoryMetadata

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
