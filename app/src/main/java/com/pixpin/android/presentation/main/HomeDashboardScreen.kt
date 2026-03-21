package com.pixpin.android.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.domain.usecase.FloatingBallTheme
import com.pixpin.android.domain.usecase.PinScaleMode
import com.pixpin.android.presentation.theme.floatingBallThemeColors
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeDashboardScreen(
    modifier: Modifier = Modifier,
    permissionGranted: Boolean,
    selectedAction: CaptureResultAction,
    selectedScaleMode: PinScaleMode,
    defaultPinShadowEnabled: Boolean,
    defaultPinCornerRadiusDp: Float,
    floatingBallTheme: FloatingBallTheme,
    floatingBallSizeDp: Int,
    onOpenGalleryPin: () -> Unit,
    onOpenGalleryOcr: () -> Unit,
    onOpenClipboardTextPin: () -> Unit,
    onOpenTranslationSettings: () -> Unit,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = floatingBallThemeColors(floatingBallTheme)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                listOf(
                                    colors.start.copy(alpha = 0.20f),
                                    colors.end.copy(alpha = 0.10f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (permissionGranted) {
                                            Icons.Default.Check
                                        } else {
                                            Icons.Default.Settings
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "PixPin 工作台",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (permissionGranted) {
                                        "悬浮工作流已就绪"
                                    } else {
                                        "先完成悬浮窗授权"
                                    },
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = if (permissionGranted) {
                                        "单击截图，长按展开菜单。"
                                    } else {
                                        "授权后可使用悬浮球。"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryPill(
                                text = if (permissionGranted) "悬浮权限已开启" else "等待悬浮权限",
                                emphasized = true
                            )
                            SummaryPill(
                                text = if (selectedAction == CaptureResultAction.PIN_DIRECTLY) {
                                    "截图后直接贴图"
                                } else {
                                    "截图后进入编辑"
                                }
                            )
                            SummaryPill(
                                text = if (selectedScaleMode == PinScaleMode.LOCK_ASPECT) {
                                    "默认等比例缩放"
                                } else {
                                    "默认自由缩放"
                                }
                            )
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
                                    Text("重启悬浮球")
                                }
                            }
                            OutlinedButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("查看设置")
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "快速创建"
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PhotoLibrary,
                        title = "相册贴图",
                        description = "从相册选图后直接生成贴图。",
                        onClick = onOpenGalleryPin
                    )
                    HomeActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TextFields,
                        title = "相册 OCR",
                        description = "从相册选图后识别文字并进入结果页。",
                        onClick = onOpenGalleryOcr
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TextFields,
                        title = "文字贴图",
                        description = "直接把剪贴板文本渲染成贴图。",
                        onClick = onOpenClipboardTextPin
                    )
                    HomeActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Translate,
                        title = "翻译设置",
                        description = "管理本地模型和百度、有道云翻译密钥。",
                        onClick = onOpenTranslationSettings
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "当前默认"
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "缩放模型",
                    value = if (selectedScaleMode == PinScaleMode.LOCK_ASPECT) "等比例" else "自由",
                    hint = "贴图运行时默认行为"
                )
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "圆角",
                    value = "${defaultPinCornerRadiusDp.roundToInt()}dp",
                    hint = if (defaultPinCornerRadiusDp <= 0.5f) "当前接近直角" else "当前使用圆角"
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "悬浮球",
                    value = "${floatingBallSizeDp}dp",
                    hint = floatingBallThemeLabel(floatingBallTheme)
                )
                MetricsCard(
                    modifier = Modifier.weight(1f),
                    title = "贴图阴影",
                    value = if (defaultPinShadowEnabled) "开启" else "关闭",
                    hint = "新贴图的默认外观"
                )
            }
        }

    }
}
