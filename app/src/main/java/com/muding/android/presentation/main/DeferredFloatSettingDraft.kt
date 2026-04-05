package com.muding.android.presentation.main

import kotlin.math.abs

internal data class DeferredFloatSettingDraft(
    val previewValue: Float
) {
    fun update(value: Float): DeferredFloatSettingDraft {
        return copy(previewValue = value)
    }

    fun commitOrNull(committedValue: Float): Float? {
        return previewValue.takeIf { abs(it - committedValue) > 0.0001f }
    }

    companion object {
        fun fromCommitted(value: Float): DeferredFloatSettingDraft {
            return DeferredFloatSettingDraft(previewValue = value)
        }
    }
}
