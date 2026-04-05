package com.muding.android.presentation.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FloatingBallAppearanceDraftTest {

    @Test
    fun fromCommitted_initializesPreviewFromCommittedValues() {
        val draft = FloatingBallAppearanceDraft.fromCommitted(
            sizeDp = 46,
            opacity = 0.92f
        )

        assertEquals(46, draft.previewSizeDp)
        assertEquals(0.92f, draft.previewOpacity, 0.0001f)
    }

    @Test
    fun sizeCommitOrNull_returnsMappedSizeOnlyWhenDraftChanged() {
        val original = FloatingBallAppearanceDraft.fromCommitted(
            sizeDp = 46,
            opacity = 0.92f
        )

        assertNull(original.sizeCommitOrNull(committedSizeDp = 46))

        val changed = original.updateSizeProgress(100)
        assertEquals(60, changed.previewSizeDp)
        assertEquals(60, changed.sizeCommitOrNull(committedSizeDp = 46))
    }

    @Test
    fun opacityCommitOrNull_returnsMappedOpacityOnlyWhenDraftChanged() {
        val original = FloatingBallAppearanceDraft.fromCommitted(
            sizeDp = 46,
            opacity = 0.92f
        )

        assertNull(original.opacityCommitOrNull(committedOpacity = 0.92f))

        val changed = original.updateOpacityPercent(1)
        assertEquals(0.01f, changed.previewOpacity, 0.0001f)
        val committedOpacity = changed.opacityCommitOrNull(committedOpacity = 0.92f)
        assertEquals(0.01f, committedOpacity ?: error("Expected changed opacity"), 0.0001f)
    }
}
