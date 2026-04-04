package com.muding.android.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun HomeDashboardScreen(
    modifier: Modifier = Modifier,
    permissionGranted: Boolean,
    selectedAction: com.muding.android.domain.usecase.CaptureResultAction,
    selectedScaleMode: com.muding.android.domain.usecase.PinScaleMode,
    defaultPinShadowEnabled: Boolean,
    defaultPinCornerRadiusDp: Float,
    floatingBallTheme: com.muding.android.domain.usecase.FloatingBallTheme,
    floatingBallSizeDp: Int,
    onOpenGalleryPin: () -> Unit,
    onOpenGalleryOcr: () -> Unit,
    onOpenClipboardTextPin: () -> Unit,
    onOpenTranslationSettings: () -> Unit,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val tokens = rememberMainUiTokens()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = tokens.spacing.pageGutter),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = tokens.palette.surfaceAccent,
                border = androidx.compose.foundation.BorderStroke(1.dp, tokens.palette.outline)
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(tokens.spacing.contentPadding),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = MaterialTheme.shapes.large,
                            color = tokens.palette.surface
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                androidx.compose.material3.Icon(
                                    imageVector = if (permissionGranted) {
                                        Icons.Default.Check
                                    } else {
                                        Icons.Default.Settings
                                    },
                                    contentDescription = null,
                                    tint = tokens.palette.accent
                                )
                            }
                        }
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (permissionGranted) "悬浮球已准备好" else "还差一步授权",
                                style = MaterialTheme.typography.headlineSmall,
                                color = tokens.palette.title
                            )
                            Text(
                                text = if (permissionGranted) {
                                    "截图后会按当前默认设置继续工作。"
                                } else {
                                    "开启悬浮窗权限后，截图和贴图流程才会完整可用。"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.palette.body
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!permissionGranted) {
                            Button(
                                onClick = onRequestPermission,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("去授权")
                            }
                        } else {
                            Button(
                                onClick = onStartService,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("刷新悬浮球")
                            }
                        }
                        OutlinedButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("打开设置")
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(title = "快速操作")
        }

        item {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PhotoLibrary,
                        title = "相册贴图",
                        description = "从相册快速创建",
                        onClick = onOpenGalleryPin
                    )
                    HomeActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TextFields,
                        title = "相册 OCR",
                        description = "识别图片中的文字",
                        onClick = onOpenGalleryOcr
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TextFields,
                        title = "文字贴图",
                        description = "从剪贴板直接生成",
                        onClick = onOpenClipboardTextPin
                    )
                    HomeActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Translate,
                        title = "翻译设置",
                        description = "管理模型和密钥",
                        onClick = onOpenTranslationSettings
                    )
                }
            }
        }

        item {
            SettingGroup(title = "当前默认值") {
                InlineValueRow("截图结果", captureActionLabel(selectedAction))
                InlineValueRow("缩放方式", pinInteractionSettingsSummary(selectedScaleMode))
                InlineValueRow("默认圆角", "${defaultPinCornerRadiusDp.roundToInt()}dp")
                InlineValueRow(
                    "悬浮球",
                    "${floatingBallSizeDp}dp · ${floatingBallThemeLabel(floatingBallTheme)}"
                )
                InlineValueRow("贴图阴影", if (defaultPinShadowEnabled) "开启" else "关闭")
            }
        }
    }
}
