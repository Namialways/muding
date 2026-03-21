package com.pixpin.android.presentation.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
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
import com.pixpin.android.domain.usecase.PinHistoryRecord

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                    Column {
                        Text("记录详情", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = historySourceLabel(record.sourceType),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = record.displayName ?: record.imageUri.substringAfterLast('/'),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    LabeledValueRow(label = "时间", value = formatTimestamp(record.createdAt))
                    LabeledValueRow(label = "来源", value = historySourceLabel(record.sourceType))
                    LabeledValueRow(
                        label = "编辑",
                        value = if (record.annotationSessionId.isNullOrBlank()) "无工程" else "可继续编辑"
                    )
                    record.widthPx?.let { width ->
                        val height = record.heightPx ?: return@let
                        LabeledValueRow(label = "尺寸", value = "${width} × $height")
                    }
                }
            }
        }

        record.textPreview?.takeIf { it.isNotBlank() }?.let { preview ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("文字摘要", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("文件", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = record.imageUri,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { copyToClipboard(context, "image_path", record.imageUri, "已复制路径") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("复制路径")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
