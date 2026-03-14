package com.pixpin.android.domain.usecase

import android.content.Context

enum class CaptureResultAction(val value: String) {
    PIN_DIRECTLY("pin_directly"),
    OPEN_EDITOR("open_editor")
}

enum class PinScaleMode(val value: String) {
    LOCK_ASPECT("lock_aspect"),
    FREE_SCALE("free_scale")
}

class CaptureFlowSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getResultAction(): CaptureResultAction {
        return when (prefs.getString(KEY_RESULT_ACTION, CaptureResultAction.OPEN_EDITOR.value)) {
            CaptureResultAction.PIN_DIRECTLY.value -> CaptureResultAction.PIN_DIRECTLY
            else -> CaptureResultAction.OPEN_EDITOR
        }
    }

    fun setResultAction(action: CaptureResultAction) {
        prefs.edit().putString(KEY_RESULT_ACTION, action.value).apply()
    }

    fun getPinScaleMode(): PinScaleMode {
        return when (prefs.getString(KEY_PIN_SCALE_MODE, PinScaleMode.LOCK_ASPECT.value)) {
            PinScaleMode.FREE_SCALE.value -> PinScaleMode.FREE_SCALE
            else -> PinScaleMode.LOCK_ASPECT
        }
    }

    fun setPinScaleMode(mode: PinScaleMode) {
        prefs.edit().putString(KEY_PIN_SCALE_MODE, mode.value).apply()
    }

    companion object {
        private const val PREFS_NAME = "pixpin_capture_flow"
        private const val KEY_RESULT_ACTION = "result_action"
        private const val KEY_PIN_SCALE_MODE = "pin_scale_mode"
    }
}
