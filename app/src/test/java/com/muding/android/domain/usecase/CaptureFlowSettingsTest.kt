package com.muding.android.domain.usecase

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun floatingBallSizeDp_usesNewThirtyToSixtyRange() {
        val preferences = InMemorySharedPreferences()
        val settings = CaptureFlowSettings.forPreferences(preferences)

        settings.setFloatingBallSizeDp(12)
        assertEquals(30, settings.getFloatingBallSizeDp())

        settings.setFloatingBallSizeDp(72)
        assertEquals(60, settings.getFloatingBallSizeDp())
    }

    @Test
    fun floatingBallOpacity_usesOnePercentToFullyOpaqueRange() {
        val preferences = InMemorySharedPreferences()
        val settings = CaptureFlowSettings.forPreferences(preferences)

        settings.setFloatingBallOpacity(0f)
        assertEquals(0.01f, settings.getFloatingBallOpacity(), 0.0001f)

        settings.setFloatingBallOpacity(3f)
        assertEquals(1f, settings.getFloatingBallOpacity(), 0.0001f)
    }

    @Test
    fun legacyStoredFloatingBallValues_areClampedIntoNewRange() {
        val preferences = InMemorySharedPreferences().apply {
            edit()
                .putInt("floating_ball_size_dp", 96)
                .putFloat("floating_ball_opacity", 0f)
                .apply()
        }
        val settings = CaptureFlowSettings.forPreferences(preferences)

        assertEquals(60, settings.getFloatingBallSizeDp())
        assertTrue(settings.getFloatingBallOpacity() >= 0.01f)
    }

    @Test
    fun floatingBallAppearance_defaultsToThemeModeWithoutCustomImage() {
        val settings = CaptureFlowSettings.forPreferences(InMemorySharedPreferences())

        assertEquals(FloatingBallAppearanceMode.THEME, settings.getFloatingBallAppearanceMode())
        assertEquals(null, settings.getFloatingBallCustomImageUri())
    }

    @Test
    fun floatingBallAppearance_storesCustomImageModeAndUri() {
        val settings = CaptureFlowSettings.forPreferences(InMemorySharedPreferences())

        settings.setFloatingBallAppearanceMode(FloatingBallAppearanceMode.CUSTOM_IMAGE)
        settings.setFloatingBallCustomImageUri("content://floating-ball/custom.png")

        assertEquals(FloatingBallAppearanceMode.CUSTOM_IMAGE, settings.getFloatingBallAppearanceMode())
        assertEquals("content://floating-ball/custom.png", settings.getFloatingBallCustomImageUri())
    }

    @Test
    fun clearingFloatingBallCustomImageUri_preservesOtherFloatingBallSettings() {
        val settings = CaptureFlowSettings.forPreferences(InMemorySharedPreferences())

        settings.setFloatingBallTheme(FloatingBallTheme.EMERALD)
        settings.setFloatingBallAppearanceMode(FloatingBallAppearanceMode.CUSTOM_IMAGE)
        settings.setFloatingBallCustomImageUri("content://floating-ball/custom.png")

        settings.setFloatingBallCustomImageUri(null)

        assertEquals(FloatingBallTheme.EMERALD, settings.getFloatingBallTheme())
        assertEquals(FloatingBallAppearanceMode.CUSTOM_IMAGE, settings.getFloatingBallAppearanceMode())
        assertEquals(null, settings.getFloatingBallCustomImageUri())
    }

    @Test
    fun onboardingGuideProgress_defaultsToAllUnseen() {
        val settings = CaptureFlowSettings.forPreferences(InMemorySharedPreferences())

        assertFalse(settings.hasSeenHomeOnboardingGuide())
        assertFalse(settings.hasSeenFloatingBallHint())
        assertFalse(settings.hasSeenPinOverlayHint())
        assertFalse(settings.hasSeenEditorHint())
    }

    @Test
    fun onboardingGuideProgress_persistsEachSeenFlag() {
        val settings = CaptureFlowSettings.forPreferences(InMemorySharedPreferences())

        settings.setHomeOnboardingGuideSeen(true)
        settings.setFloatingBallHintSeen(true)
        settings.setPinOverlayHintSeen(true)
        settings.setEditorHintSeen(true)

        assertTrue(settings.hasSeenHomeOnboardingGuide())
        assertTrue(settings.hasSeenFloatingBallHint())
        assertTrue(settings.hasSeenPinOverlayHint())
        assertTrue(settings.hasSeenEditorHint())
    }

    @Test
    fun clearAll_resetsOnboardingGuideProgress() {
        val settings = CaptureFlowSettings.forPreferences(InMemorySharedPreferences())

        settings.setHomeOnboardingGuideSeen(true)
        settings.setFloatingBallHintSeen(true)
        settings.setPinOverlayHintSeen(true)
        settings.setEditorHintSeen(true)

        settings.clearAll()

        assertFalse(settings.hasSeenHomeOnboardingGuide())
        assertFalse(settings.hasSeenFloatingBallHint())
        assertFalse(settings.hasSeenPinOverlayHint())
        assertFalse(settings.hasSeenEditorHint())
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
