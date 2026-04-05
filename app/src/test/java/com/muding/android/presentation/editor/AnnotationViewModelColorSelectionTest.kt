package com.muding.android.presentation.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.muding.android.data.settings.AppSettingsRepository
import com.muding.android.data.settings.CloudTranslationProvider
import com.muding.android.data.settings.FloatingBallSettings
import com.muding.android.data.settings.PinAppearanceSettings
import com.muding.android.data.settings.PinHistorySettings
import com.muding.android.data.settings.ProjectRecordSettings
import com.muding.android.data.settings.TranslationSettings
import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.domain.usecase.FloatingBallTheme
import com.muding.android.domain.usecase.PinScaleMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AnnotationViewModelColorSelectionTest {

    @Test
    fun `initial state restores favorites and recents from settings`() {
        val viewModel = AnnotationViewModel(
            settingsRepository = FakeAppSettingsRepository(
                favoriteEditorColors = listOf(GREEN),
                recentEditorColors = listOf(RED, BLUE)
            )
        )

        assertEquals(listOf(GREEN), viewModel.favoriteColors.map { it.toArgb() })
        assertEquals(listOf(RED, BLUE), viewModel.recentColors.map { it.toArgb() })
        assertEquals(RED, viewModel.currentColor.value.toArgb())
    }

    @Test
    fun `confirming a dialog color updates current color and recents`() {
        val settingsRepository = FakeAppSettingsRepository(
            recentEditorColors = listOf(RED, BLUE)
        )
        val viewModel = AnnotationViewModel(settingsRepository)

        viewModel.applyConfirmedColor(Color(GREEN))

        assertEquals(GREEN, viewModel.currentColor.value.toArgb())
        assertEquals(listOf(GREEN, RED, BLUE), viewModel.recentColors.map { it.toArgb() })
        assertEquals(listOf(GREEN, RED, BLUE), settingsRepository.savedRecentEditorColors)
    }

    @Test
    fun `toggling favorite color persists updated favorites`() {
        val settingsRepository = FakeAppSettingsRepository(
            favoriteEditorColors = listOf(RED, GREEN)
        )
        val viewModel = AnnotationViewModel(settingsRepository)

        viewModel.toggleFavoriteColor(Color(BLUE))

        assertEquals(listOf(RED, GREEN, BLUE), viewModel.favoriteColors.map { it.toArgb() })
        assertEquals(listOf(RED, GREEN, BLUE), settingsRepository.savedFavoriteEditorColors)
    }

    @Test
    fun `quick access colors prefer favorites before recents`() {
        val viewModel = AnnotationViewModel(
            settingsRepository = FakeAppSettingsRepository(
                favoriteEditorColors = listOf(GREEN),
                recentEditorColors = listOf(GREEN, RED, BLUE)
            )
        )

        assertEquals(listOf(GREEN, RED, BLUE), viewModel.quickAccessColors.map { it.toArgb() })
    }

    @Test
    fun `discarding draft leaves committed color and recents unchanged`() {
        val settingsRepository = FakeAppSettingsRepository(
            recentEditorColors = listOf(RED)
        )
        val viewModel = AnnotationViewModel(settingsRepository)
        val draft = ColorPickerDraftState.fromColor(viewModel.currentColor.value).updateHex("#112233")

        assertEquals(0xFF112233.toInt(), draft.previewColor.toArgb())
        assertEquals(RED, viewModel.currentColor.value.toArgb())
        assertEquals(listOf(RED), viewModel.recentColors.map { it.toArgb() })
        assertEquals(emptyList<Int>(), settingsRepository.savedRecentEditorColors)
    }

    private class FakeAppSettingsRepository(
        private var favoriteEditorColors: List<Int> = emptyList(),
        private var recentEditorColors: List<Int> = emptyList()
    ) : AppSettingsRepository {

        var savedFavoriteEditorColors: List<Int> = emptyList()
            private set

        var savedRecentEditorColors: List<Int> = emptyList()
            private set

        override fun getCaptureResultAction(): CaptureResultAction = CaptureResultAction.OPEN_EDITOR

        override fun setCaptureResultAction(action: CaptureResultAction) = Unit

        override fun getFavoriteEditorColors(): List<Int> = favoriteEditorColors

        override fun setFavoriteEditorColors(colors: List<Int>) {
            favoriteEditorColors = colors
            savedFavoriteEditorColors = colors
        }

        override fun getRecentEditorColors(): List<Int> = recentEditorColors

        override fun setRecentEditorColors(colors: List<Int>) {
            recentEditorColors = colors
            savedRecentEditorColors = colors
        }

        override fun getPinScaleMode(): PinScaleMode = PinScaleMode.LOCK_ASPECT

        override fun setPinScaleMode(mode: PinScaleMode) = Unit

        override fun getProjectRecordSettings(): ProjectRecordSettings {
            return ProjectRecordSettings(maxSessionCount = 50, retainDays = 7)
        }

        override fun setMaxSessionCount(count: Int) = Unit

        override fun setRetainDays(days: Int) = Unit

        override fun getPinAppearanceSettings(): PinAppearanceSettings {
            return PinAppearanceSettings(shadowEnabled = true, cornerRadiusDp = 12f)
        }

        override fun isPinShadowEnabledByDefault(): Boolean = true

        override fun setPinShadowEnabledByDefault(enabled: Boolean) = Unit

        override fun getDefaultPinCornerRadiusDp(): Float = 12f

        override fun setDefaultPinCornerRadiusDp(radiusDp: Float) = Unit

        override fun getFloatingBallSettings(): FloatingBallSettings {
            return FloatingBallSettings(sizeDp = 60, opacity = 0.92f, theme = FloatingBallTheme.BLUE_PURPLE)
        }

        override fun setFloatingBallSizeDp(sizeDp: Int) = Unit

        override fun setFloatingBallOpacity(opacity: Float) = Unit

        override fun setFloatingBallTheme(theme: FloatingBallTheme) = Unit

        override fun getPinHistorySettings(): PinHistorySettings {
            return PinHistorySettings(enabled = true, maxCount = 50, retainDays = 14)
        }

        override fun setPinHistoryEnabled(enabled: Boolean) = Unit

        override fun setMaxPinHistoryCount(count: Int) = Unit

        override fun setPinHistoryRetainDays(days: Int) = Unit

        override fun getTranslationSettings(): TranslationSettings {
            return TranslationSettings(
                localTargetLanguageTag = "ja",
                localDownloadOnWifiOnly = true,
                cloudProvider = CloudTranslationProvider.NONE,
                baiduAppId = "",
                baiduSecretKey = "",
                youdaoAppKey = "",
                youdaoAppSecret = ""
            )
        }

        override fun setLocalTranslationTargetLanguageTag(languageTag: String) = Unit

        override fun setLocalTranslationDownloadOnWifiOnly(enabled: Boolean) = Unit

        override fun setCloudTranslationProvider(provider: CloudTranslationProvider) = Unit

        override fun setBaiduTranslationCredentials(appId: String, secretKey: String) = Unit

        override fun setYoudaoTranslationCredentials(appKey: String, appSecret: String) = Unit

        override fun resetAllSettings() = Unit
    }

    companion object {
        private val RED = Color(0xFFFF453A).toArgb()
        private val GREEN = Color(0xFF7DDD28).toArgb()
        private val BLUE = Color(0xFF3B82F6).toArgb()
    }
}
