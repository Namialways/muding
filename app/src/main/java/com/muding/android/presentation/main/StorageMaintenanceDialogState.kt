package com.muding.android.presentation.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class StorageMaintenanceAction(
    val dialogTitle: String,
    val dialogMessage: String,
    val confirmLabel: String
) {
    CLEAR_WORK_RECORDS(
        dialogTitle = "清空工作记录",
        dialogMessage = "这会删除贴图历史、可继续编辑数据和临时图片缓存。",
        confirmLabel = "确认清空"
    ),
    RESET_APPLICATION(
        dialogTitle = "恢复初始状态",
        dialogMessage = "这会清空工作记录，并重置软件设置、翻译配置和本地翻译模型。",
        confirmLabel = "确认重置"
    )
}

class StorageMaintenanceDialogState {
    var pendingAction by mutableStateOf<StorageMaintenanceAction?>(null)
        private set

    val dialogTitle: String?
        get() = pendingAction?.dialogTitle

    val dialogMessage: String?
        get() = pendingAction?.dialogMessage

    val confirmLabel: String?
        get() = pendingAction?.confirmLabel

    fun request(action: StorageMaintenanceAction) {
        pendingAction = action
    }

    fun dismiss() {
        pendingAction = null
    }

    fun confirm(onConfirm: (StorageMaintenanceAction) -> Unit) {
        val action = pendingAction ?: return
        pendingAction = null
        onConfirm(action)
    }
}
