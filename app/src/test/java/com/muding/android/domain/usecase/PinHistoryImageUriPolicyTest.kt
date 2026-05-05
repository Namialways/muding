package com.muding.android.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHistoryImageUriPolicyTest {

    @Test
    fun `history asset uri does not need to be persisted again`() {
        val uri = "content://com.muding.android.fileprovider/records/pin_history_assets/pin_history_1.png"

        assertFalse(
            PinHistoryImageUriPolicy.shouldPersistForHistory(
                imageUri = uri,
                fileProviderAuthority = "com.muding.android.fileprovider"
            )
        )
    }

    @Test
    fun `cache uri should be copied into persistent history storage`() {
        val uri = "content://com.muding.android.fileprovider/cache/screenshots/region_capture_1.png"

        assertTrue(
            PinHistoryImageUriPolicy.shouldPersistForHistory(
                imageUri = uri,
                fileProviderAuthority = "com.muding.android.fileprovider"
            )
        )
    }

    @Test
    fun `blank uri is not persisted into history`() {
        assertFalse(
            PinHistoryImageUriPolicy.shouldPersistForHistory(
                imageUri = "",
                fileProviderAuthority = "com.muding.android.fileprovider"
            )
        )
    }
}
