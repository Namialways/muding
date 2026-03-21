package com.muding.android.feature.pin.runtime

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun PinManagerContent(
    refreshToken: Int,
    overlays: List<PinOverlaySnapshot>,
    onShowAll: () -> Unit,
    onHideAll: () -> Unit,
    onCloseAll: () -> Unit,
    onCloseManager: () -> Unit,
    onToggleVisible: (String) -> Unit,
    onCloseOne: (String) -> Unit,
    onFocusOne: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        modifier = Modifier.width(320.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("贴图管理", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "当前贴图 ${overlays.size} 张",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledIconButton(
                    onClick = onCloseManager,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "关闭管理面板")
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onShowAll) {
                    Text("全部显示")
                }
                OutlinedButton(onClick = onHideAll) {
                    Text("全部隐藏")
                }
                OutlinedButton(onClick = onCloseAll) {
                    Text("全部关闭")
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            if (overlays.isEmpty()) {
                Text(
                    text = "当前没有可管理的贴图。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items = overlays, key = { overlay -> overlay.id + refreshToken }) { overlay ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    bitmap = overlay.bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp)
                                )
                                Column(modifier = Modifier.width(72.dp)) {
                                    Text("贴图 ${overlay.id.take(6)}", style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        text = if (overlay.visible) "显示中" else "已隐藏",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(onClick = { onFocusOne(overlay.id) }) {
                                    Text("置顶")
                                }
                                OutlinedButton(onClick = { onToggleVisible(overlay.id) }) {
                                    Text(if (overlay.visible) "隐藏" else "显示")
                                }
                                OutlinedButton(onClick = { onCloseOne(overlay.id) }) {
                                    Text("关闭")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
