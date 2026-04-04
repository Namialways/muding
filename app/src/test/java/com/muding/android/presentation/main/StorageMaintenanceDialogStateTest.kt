package com.muding.android.presentation.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StorageMaintenanceDialogStateTest {

    @Test
    fun `requesting work record cleanup exposes matching confirmation copy`() {
        val state = StorageMaintenanceDialogState()

        state.request(StorageMaintenanceAction.CLEAR_WORK_RECORDS)

        assertEquals(StorageMaintenanceAction.CLEAR_WORK_RECORDS, state.pendingAction)
        assertEquals("清空工作记录", state.dialogTitle)
        assertEquals("这会删除贴图历史、可继续编辑数据和临时图片缓存。", state.dialogMessage)
    }

    @Test
    fun `confirm invokes pending action and closes dialog`() {
        val invoked = mutableListOf<StorageMaintenanceAction>()
        val state = StorageMaintenanceDialogState()

        state.request(StorageMaintenanceAction.RESET_APPLICATION)
        state.confirm { action -> invoked += action }

        assertEquals(listOf(StorageMaintenanceAction.RESET_APPLICATION), invoked)
        assertNull(state.pendingAction)
        assertNull(state.dialogTitle)
        assertNull(state.dialogMessage)
    }

    @Test
    fun `dismiss closes dialog without invoking action`() {
        val invoked = mutableListOf<StorageMaintenanceAction>()
        val state = StorageMaintenanceDialogState()

        state.request(StorageMaintenanceAction.CLEAR_WORK_RECORDS)
        state.dismiss()
        state.confirm { action -> invoked += action }

        assertEquals(emptyList<StorageMaintenanceAction>(), invoked)
        assertNull(state.pendingAction)
    }
}
