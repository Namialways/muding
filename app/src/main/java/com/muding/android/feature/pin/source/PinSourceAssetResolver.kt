package com.muding.android.feature.pin.source

import com.muding.android.core.model.PinImageAsset
import com.muding.android.core.model.PinSource

interface PinSourceAdapter {
    fun supports(source: PinSource): Boolean
    suspend fun resolve(source: PinSource): PinImageAsset
}

class PinSourceAssetResolver(
    private val adapters: List<PinSourceAdapter>
) {

    suspend fun resolve(source: PinSource): PinImageAsset {
        val adapter = adapters.firstOrNull { it.supports(source) }
            ?: throw IllegalArgumentException("No adapter found for source type ${source.type}")
        return adapter.resolve(source)
    }
}
