package com.muding.android.presentation.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muding.android.domain.usecase.PinHistoryRecord

@Composable
fun RecordDetailScreen(
    modifier: Modifier = Modifier,
    record: PinHistoryRecord,
    onBack: () -> Unit,
    onRestore: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val tokens = rememberMainUiTokens()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = tokens.spacing.pageGutter),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                SectionHeader(
                    title = "记录详情",
                    description = historySourceLabel(record.sourceType)
                )
            }
        }

        item {
            RecordThumbnail(
                imageUri = record.imageUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }

        item {
            SettingGroup(title = "基本信息") {
                Text(
                    text = record.displayName ?: record.imageUri.substringAfterLast('/'),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                InlineValueRow(label = "时间", value = formatTimestamp(record.createdAt))
                InlineValueRow(label = "来源", value = historySourceLabel(record.sourceType))
                InlineValueRow(
                    label = "编辑状态",
                    value = if (record.annotationSessionId.isNullOrBlank()) "无工程" else "可继续编辑"
                )
                record.widthPx?.let { width ->
                    val height = record.heightPx ?: return@let
                    InlineValueRow(label = "尺寸", value = "${width} x $height")
                }
            }
        }

        record.textPreview?.takeIf { it.isNotBlank() }?.let { preview ->
            item {
                SettingGroup(title = "文字摘要") {
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.palette.body
                    )
                }
            }
        }

        item {
            SettingGroup(title = "文件") {
                Text(
                    text = record.imageUri,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.palette.body
                )
                OutlinedButton(
                    onClick = {
                        copyToClipboard(
                            context = context,
                            label = "image_path",
                            value = record.imageUri,
                            message = "已复制路径"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("复制路径")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(onClick = onRestore, modifier = Modifier.weight(1f)) {
                    Text("恢复贴图")
                }
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text(if (record.annotationSessionId.isNullOrBlank()) "进入编辑" else "继续编辑")
                }
            }
        }

        item {
            OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text("删除记录")
            }
        }
    }
}

private fun copyToClipboard(
    context: Context,
    label: String,
    value: String,
    message: String
) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
