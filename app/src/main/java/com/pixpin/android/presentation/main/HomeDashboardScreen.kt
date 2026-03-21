package com.pixpin.android.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
                                    colors.start.copy(alpha = 0.18f),
                                    colors.end.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    text = if (permissionGranted) "PixPin 已就绪" else "还差一步授权",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = if (permissionGranted) {
                                        "悬浮球单击截图，长按展开创建菜单。"
                                    } else {
                                        "授予悬浮窗权限后，截图和贴图入口才会完整可用。"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryPill(
                                text = if (permissionGranted) "悬浮权限已开启" else "等待悬浮权限"
                            )
                            SummaryPill(
                                text = if (selectedAction == CaptureResultAction.PIN_DIRECTLY) {
                                    "截图后直接贴图"
                                } else {
                                    "截图后进入编辑"
                                }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryPill(
                                text = if (selectedScaleMode == PinScaleMode.LOCK_ASPECT) {
                                    "默认等比例缩放"
                                } else {
                                    "默认自由缩放"
                                }
                            )
                            SummaryPill(
                                text = if (defaultPinShadowEnabled) {
                                    "贴图默认带阴影"
                                } else {
                                    "贴图默认无阴影"
                                }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (!permissionGranted) {
                                Button(
                                    onClick = onRequestPermission,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("授予权限")
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
                title = "快速创建",
                description = "把常用入口集中在主页，减少回到设置里找按钮。"
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PhotoLibrary,
                        title = "相册贴图",
                        description = "选图后直接生成贴图",
                        onClick = onOpenGalleryPin
                    )
                    HomeActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TextFields,
                        title = "相册 OCR",
                        description = "选图识别后进入结果页",
                        onClick = onOpenGalleryOcr
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TextFields,
                        title = "文字贴图",
                        description = "直接从剪贴板生成文字贴图",
                        onClick = onOpenClipboardTextPin
                    )
                    HomeActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Translate,
                        title = "翻译设置",
                        description = "本地模型和云翻译密钥",
                        onClick = onOpenTranslationSettings
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("当前贴图风格摘要", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "圆角 ${defaultPinCornerRadiusDp.roundToInt()}dp  ·  悬浮球 ${floatingBallSizeDp}dp",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "真正的细项配置已拆到设置页里，主页只保留高频动作和状态摘要。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
