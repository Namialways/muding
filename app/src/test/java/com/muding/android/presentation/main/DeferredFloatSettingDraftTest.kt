package com.muding.android.presentation.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeferredFloatSettingDraftTest {

    @Test
    fun fromCommitted_initializesPreviewValue() {
        val draft = DeferredFloatSettingDraft.fromCommitted(12f)

        assertEquals(12f, draft.previewValue, 0.0001f)
    }

    @Test
    fun commitOrNull_returnsNullWhenUnchanged() {
        val draft = DeferredFloatSettingDraft.fromCommitted(12f)

        assertNull(draft.commitOrNull(committedValue = 12f))
    }

    @Test
    fun commitOrNull_returnsUpdatedValueWhenChanged() {
        val draft = DeferredFloatSettingDraft.fromCommitted(12f).update(24f)

        assertEquals(24f, draft.commitOrNull(committedValue = 12f) ?: error("Expected changed value"), 0.0001f)
    }
}
