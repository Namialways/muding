package com.muding.android.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.muding.android.domain.usecase.FloatingBallAppearanceMode
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
    floatingBallAppearanceMode: FloatingBallAppearanceMode,
    floatingBallSizeDp: Int,
    showFirstRunGuide: Boolean,
    onOpenGalleryPin: () -> Unit,
    onOpenGalleryOcr: () -> Unit,
    onOpenClipboardTextPin: () -> Unit,
    onOpenTranslationSettings: () -> Unit,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissFirstRunGuide: () -> Unit
) {
    val tokens = rememberMainUiTokens()
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
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
                    Column(
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
                                    Icon(
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
                            Column(
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
                Column(
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
                        "${floatingBallSizeDp}dp · ${floatingBallAppearanceSummaryLabel(floatingBallAppearanceMode, floatingBallTheme)}"
                    )
                    InlineValueRow("贴图阴影", if (defaultPinShadowEnabled) "开启" else "关闭")
                }
            }
        }

        if (showFirstRunGuide) {
            FirstRunOnboardingCard(
                modifier = Modifier.align(Alignment.Center),
                onDismiss = onDismissFirstRunGuide
            )
        }
    }
}

@Composable
private fun FirstRunOnboardingCard(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "开始前先记住 3 个核心动作",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭引导"
                        )
                    }
                }

                GuideActionRow(
                    icon = Icons.Default.Settings,
                    title = "长按悬浮球",
                    description = "打开截图 / OCR / 管理等操作"
                )
                GuideActionRow(
                    icon = Icons.Default.PushPin,
                    title = "长按贴图",
                    description = "进入编辑页继续处理"
                )
                GuideActionRow(
                    icon = Icons.Default.Close,
                    title = "双击贴图",
                    description = "快速关闭当前贴图"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("开始使用")
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("稍后查看")
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
