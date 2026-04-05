package com.muding.android.domain.usecase

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureFlowSettingsTest {

    @Test
    fun getResultAction_defaultsToDirectPinWhenUnset() {
        val settings = CaptureFlowSettings.forPreferences(InMemorySharedPreferences())

        assertEquals(CaptureResultAction.PIN_DIRECTLY, settings.getResultAction())
    }

    @Test
    fun getFloatingBallSizeDp_defaultsToFortySixWhenUnset() {
        val settings = CaptureFlowSettings.forPreferences(InMemorySharedPreferences())

        assertEquals(46, settings.getFloatingBallSizeDp())
    }

    @Test
    fun storedSettingsOverrideDefaults() {
        val preferences = InMemorySharedPreferences().apply {
            edit()
                .putString("result_action", CaptureResultAction.OPEN_EDITOR.value)
                .putInt("floating_ball_size_dp", 58)
                .apply()
        }
        val settings = CaptureFlowSettings.forPreferences(preferences)

        assertEquals(CaptureResultAction.OPEN_EDITOR, settings.getResultAction())
        assertEquals(58, settings.getFloatingBallSizeDp())
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val values = linkedMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()

        override fun getString(key: String?, defValue: String?): String? {
            return values[key] as? String ?: defValue
        }

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return (values[key] as? Set<String>)?.toMutableSet() ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int {
            return values[key] as? Int ?: defValue
        }

        override fun getLong(key: String?, defValue: Long): Long {
            return values[key] as? Long ?: defValue
        }

        override fun getFloat(key: String?, defValue: Float): Float {
            return values[key] as? Float ?: defValue
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return values[key] as? Boolean ?: defValue
        }

        override fun contains(key: String?): Boolean = values.containsKey(key)

        override fun edit(): SharedPreferences.Editor = Editor(values)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        private class Editor(
            private val values: MutableMap<String, Any?>
        ) : SharedPreferences.Editor {
            private val pending = linkedMapOf<String, Any?>()
            private var clearRequested = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = applyChange(key, value)

            override fun putStringSet(
                key: String?,
                values: MutableSet<String>?
            ): SharedPreferences.Editor = applyChange(key, values?.toSet())

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = applyChange(key, value)

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = applyChange(key, value)

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = applyChange(key, value)

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = applyChange(key, value)

            override fun remove(key: String?): SharedPreferences.Editor = applyChange(key, null)

            override fun clear(): SharedPreferences.Editor {
                clearRequested = true
                pending.clear()
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearRequested) {
                    values.clear()
                }
                pending.forEach { (key, value) ->
                    if (value == null) {
                        values.remove(key)
                    } else {
                        values[key] = value
                    }
                }
                pending.clear()
                clearRequested = false
            }

            private fun applyChange(key: String?, value: Any?): SharedPreferences.Editor {
                if (key != null) {
                    pending[key] = value
                }
                return this
            }
        }
    }
}
