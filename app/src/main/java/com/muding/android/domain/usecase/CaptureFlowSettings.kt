package com.muding.android.domain.usecase

import android.content.Context
import com.muding.android.data.settings.CloudTranslationProvider
import java.util.Locale

enum class CaptureResultAction(val value: String) {
    PIN_DIRECTLY("pin_directly"),
    OPEN_EDITOR("open_editor");

    companion object {
        fun fromValue(value: String?): CaptureResultAction? {
            return entries.firstOrNull { it.value == value }
        }
    }
}

enum class PinScaleMode(val value: String) {
    LOCK_ASPECT("lock_aspect"),
    FREE_SCALE("free_scale")
}

enum class FloatingBallTheme(val value: String) {
    BLUE_PURPLE("blue_purple"),
    SUNSET("sunset"),
    EMERALD("emerald")
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

    fun getRecentEditorColors(): List<Int> {
        val raw = prefs.getString(KEY_RECENT_EDITOR_COLORS, "") ?: ""
        return raw.split(',')
            .mapNotNull { value ->
                value.trim()
                    .takeIf { it.isNotEmpty() }
                    ?.toLongOrNull(16)
                    ?.toInt()
            }
            .take(MAX_RECENT_EDITOR_COLORS)
    }

    fun setRecentEditorColors(colors: List<Int>) {
        val serialized = colors
            .distinct()
            .take(MAX_RECENT_EDITOR_COLORS)
            .joinToString(",") { color ->
                String.format(Locale.US, "%08X", color)
            }
        prefs.edit().putString(KEY_RECENT_EDITOR_COLORS, serialized).apply()
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

    fun getMaxSessionCount(): Int {
        return prefs.getInt(KEY_MAX_SESSION_COUNT, 50).coerceIn(1, 500)
    }

    fun setMaxSessionCount(count: Int) {
        prefs.edit().putInt(KEY_MAX_SESSION_COUNT, count.coerceIn(1, 500)).apply()
    }

    fun getRetainDays(): Int {
        return prefs.getInt(KEY_RETAIN_DAYS, 7).coerceIn(1, 365)
    }

    fun setRetainDays(days: Int) {
        prefs.edit().putInt(KEY_RETAIN_DAYS, days.coerceIn(1, 365)).apply()
    }

    fun isPinShadowEnabledByDefault(): Boolean {
        return prefs.getBoolean(KEY_PIN_SHADOW_ENABLED, true)
    }

    fun setPinShadowEnabledByDefault(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PIN_SHADOW_ENABLED, enabled).apply()
    }

    fun getDefaultPinCornerRadiusDp(): Float {
        return prefs.getFloat(KEY_PIN_CORNER_RADIUS_DP, 0f).coerceIn(0f, 48f)
    }

    fun setDefaultPinCornerRadiusDp(radiusDp: Float) {
        prefs.edit().putFloat(KEY_PIN_CORNER_RADIUS_DP, radiusDp.coerceIn(0f, 48f)).apply()
    }

    fun isPinHistoryEnabled(): Boolean {
        return prefs.getBoolean(KEY_PIN_HISTORY_ENABLED, true)
    }

    fun setPinHistoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PIN_HISTORY_ENABLED, enabled).apply()
    }

    fun getMaxPinHistoryCount(): Int {
        return prefs.getInt(KEY_MAX_PIN_HISTORY_COUNT, 50).coerceIn(1, 500)
    }

    fun setMaxPinHistoryCount(count: Int) {
        prefs.edit().putInt(KEY_MAX_PIN_HISTORY_COUNT, count.coerceIn(1, 500)).apply()
    }

    fun getPinHistoryRetainDays(): Int {
        return prefs.getInt(KEY_PIN_HISTORY_RETAIN_DAYS, 14).coerceIn(1, 365)
    }

    fun setPinHistoryRetainDays(days: Int) {
        prefs.edit().putInt(KEY_PIN_HISTORY_RETAIN_DAYS, days.coerceIn(1, 365)).apply()
    }

    fun getFloatingBallSizeDp(): Int {
        return prefs.getInt(KEY_FLOATING_BALL_SIZE_DP, 60).coerceIn(44, 96)
    }

    fun setFloatingBallSizeDp(sizeDp: Int) {
        prefs.edit().putInt(KEY_FLOATING_BALL_SIZE_DP, sizeDp.coerceIn(44, 96)).apply()
    }

    fun getFloatingBallOpacity(): Float {
        return prefs.getFloat(KEY_FLOATING_BALL_OPACITY, 0.92f).coerceIn(0.4f, 1f)
    }

    fun setFloatingBallOpacity(opacity: Float) {
        prefs.edit().putFloat(KEY_FLOATING_BALL_OPACITY, opacity.coerceIn(0.4f, 1f)).apply()
    }

    fun getFloatingBallTheme(): FloatingBallTheme {
        return when (prefs.getString(KEY_FLOATING_BALL_THEME, FloatingBallTheme.BLUE_PURPLE.value)) {
            FloatingBallTheme.SUNSET.value -> FloatingBallTheme.SUNSET
            FloatingBallTheme.EMERALD.value -> FloatingBallTheme.EMERALD
            else -> FloatingBallTheme.BLUE_PURPLE
        }
    }

    fun setFloatingBallTheme(theme: FloatingBallTheme) {
        prefs.edit().putString(KEY_FLOATING_BALL_THEME, theme.value).apply()
    }

    fun getLocalTranslationTargetLanguageTag(): String {
        return prefs.getString(KEY_LOCAL_TRANSLATION_TARGET_LANGUAGE, "en") ?: "en"
    }

    fun setLocalTranslationTargetLanguageTag(languageTag: String) {
        prefs.edit().putString(KEY_LOCAL_TRANSLATION_TARGET_LANGUAGE, languageTag.ifBlank { "en" }).apply()
    }

    fun isLocalTranslationDownloadOnWifiOnly(): Boolean {
        return prefs.getBoolean(KEY_LOCAL_TRANSLATION_WIFI_ONLY, true)
    }

    fun setLocalTranslationDownloadOnWifiOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCAL_TRANSLATION_WIFI_ONLY, enabled).apply()
    }

    fun getCloudTranslationProvider(): CloudTranslationProvider {
        return when (prefs.getString(KEY_CLOUD_TRANSLATION_PROVIDER, CloudTranslationProvider.NONE.name)) {
            CloudTranslationProvider.BAIDU.name -> CloudTranslationProvider.BAIDU
            CloudTranslationProvider.YOUDAO.name -> CloudTranslationProvider.YOUDAO
            else -> CloudTranslationProvider.NONE
        }
    }

    fun setCloudTranslationProvider(provider: CloudTranslationProvider) {
        prefs.edit().putString(KEY_CLOUD_TRANSLATION_PROVIDER, provider.name).apply()
    }

    fun getBaiduTranslationAppId(): String {
        return prefs.getString(KEY_BAIDU_TRANSLATION_APP_ID, "") ?: ""
    }

    fun getBaiduTranslationSecretKey(): String {
        return prefs.getString(KEY_BAIDU_TRANSLATION_SECRET_KEY, "") ?: ""
    }

    fun setBaiduTranslationCredentials(appId: String, secretKey: String) {
        prefs.edit()
            .putString(KEY_BAIDU_TRANSLATION_APP_ID, appId.trim())
            .putString(KEY_BAIDU_TRANSLATION_SECRET_KEY, secretKey.trim())
            .apply()
    }

    fun getYoudaoTranslationAppKey(): String {
        return prefs.getString(KEY_YOUDAO_TRANSLATION_APP_KEY, "") ?: ""
    }

    fun getYoudaoTranslationAppSecret(): String {
        return prefs.getString(KEY_YOUDAO_TRANSLATION_APP_SECRET, "") ?: ""
    }

    fun setYoudaoTranslationCredentials(appKey: String, appSecret: String) {
        prefs.edit()
            .putString(KEY_YOUDAO_TRANSLATION_APP_KEY, appKey.trim())
            .putString(KEY_YOUDAO_TRANSLATION_APP_SECRET, appSecret.trim())
            .apply()
    }

    fun clearLegacyTranslationCredentials() {
        prefs.edit()
            .remove(KEY_BAIDU_TRANSLATION_APP_ID)
            .remove(KEY_BAIDU_TRANSLATION_SECRET_KEY)
            .remove(KEY_YOUDAO_TRANSLATION_APP_KEY)
            .remove(KEY_YOUDAO_TRANSLATION_APP_SECRET)
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "muding_capture_flow"
        private const val KEY_RESULT_ACTION = "result_action"
        private const val KEY_RECENT_EDITOR_COLORS = "recent_editor_colors"
        private const val KEY_PIN_SCALE_MODE = "pin_scale_mode"
        private const val KEY_MAX_SESSION_COUNT = "max_session_count"
        private const val KEY_RETAIN_DAYS = "retain_days"
        private const val KEY_PIN_SHADOW_ENABLED = "pin_shadow_enabled"
        private const val KEY_PIN_CORNER_RADIUS_DP = "pin_corner_radius_dp"
        private const val KEY_PIN_HISTORY_ENABLED = "pin_history_enabled"
        private const val KEY_MAX_PIN_HISTORY_COUNT = "max_pin_history_count"
        private const val KEY_PIN_HISTORY_RETAIN_DAYS = "pin_history_retain_days"
        private const val KEY_FLOATING_BALL_SIZE_DP = "floating_ball_size_dp"
        private const val KEY_FLOATING_BALL_OPACITY = "floating_ball_opacity"
        private const val KEY_FLOATING_BALL_THEME = "floating_ball_theme"
        private const val KEY_LOCAL_TRANSLATION_TARGET_LANGUAGE = "local_translation_target_language"
        private const val KEY_LOCAL_TRANSLATION_WIFI_ONLY = "local_translation_wifi_only"
        private const val KEY_CLOUD_TRANSLATION_PROVIDER = "cloud_translation_provider"
        private const val KEY_BAIDU_TRANSLATION_APP_ID = "baidu_translation_app_id"
        private const val KEY_BAIDU_TRANSLATION_SECRET_KEY = "baidu_translation_secret_key"
        private const val KEY_YOUDAO_TRANSLATION_APP_KEY = "youdao_translation_app_key"
        private const val KEY_YOUDAO_TRANSLATION_APP_SECRET = "youdao_translation_app_secret"
        private const val MAX_RECENT_EDITOR_COLORS = 3
    }
}
