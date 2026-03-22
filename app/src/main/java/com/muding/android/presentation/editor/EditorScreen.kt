package com.muding.android.presentation.editor

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixOff
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muding.android.R
import com.muding.android.domain.model.DrawingPath
import com.muding.android.domain.model.DrawingTool

@Composable
fun AnnotationEditorScreenContent(
    bitmap: Bitmap,
    documentState: EditorDocumentState,
    toolPanelState: EditorToolPanelState,
    screenActions: EditorScreenActions,
    viewportState: EditorViewportState,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onPin: () -> Unit,
    onShare: () -> Unit,
    onRecrop: () -> Unit,
    onCanvasSizeChanged: (Size) -> Unit
) {
    Scaffold(
        topBar = {
            EditorTopBar(
                onClose = onClose,
                onSave = onSave,
                onPin = onPin,
                onShare = onShare,
                onRecrop = onRecrop,
                canUndo = documentState.canUndo,
                canRedo = documentState.canRedo,
                onUndo = screenActions.onUndo,
                onRedo = screenActions.onRedo,
                canDeleteSelection = documentState.selectedPathIndex != null,
                onDeleteSelection = screenActions.onDeleteSelection,
                onResetViewport = { viewportState.reset() }
            )
        },
        bottomBar = {
            EditorBottomBar(
                documentState = documentState,
                toolPanelState = toolPanelState,
                screenActions = screenActions
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
        ) {
            val imageAspect = if (bitmap.height == 0) 1f else bitmap.width.toFloat() / bitmap.height.toFloat()
            val containerAspect = maxWidth.value / maxHeight.value
            val density = LocalDensity.current
            val imageModifier = if (containerAspect > imageAspect) {
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(imageAspect)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(imageAspect)
            }
            val contentWidthPx = with(density) {
                if (containerAspect > imageAspect) {
                    maxHeight.toPx() * imageAspect
                } else {
                    maxWidth.toPx()
                }
            }
            val contentHeightPx = with(density) {
                if (containerAspect > imageAspect) {
                    maxHeight.toPx()
                } else {
                    maxWidth.toPx() / imageAspect
                }
            }
            val canTransformCanvas = currentToolAllowsViewportTransform(tool = documentState.currentTool)

            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(imageModifier),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = viewportState.scale
                            scaleY = viewportState.scale
                            translationX = viewportState.offset.x
                            translationY = viewportState.offset.y
                        }
                        .pointerInput(
                            canTransformCanvas,
                            contentWidthPx,
                            contentHeightPx
                        ) {
                            if (canTransformCanvas) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    val previousScale = viewportState.scale
                                    val newScale = (previousScale * zoom).coerceIn(1f, 6f)
                                    val pivot = Offset(contentWidthPx / 2f, contentHeightPx / 2f)
                                    val focusCompensation = (centroid - pivot) * (previousScale - newScale)
                                    val maxOffsetX = ((contentWidthPx * newScale) - contentWidthPx) / 2f
                                    val maxOffsetY = ((contentHeightPx * newScale) - contentHeightPx) / 2f
                                    viewportState.scale = newScale
                                    viewportState.offset = Offset(
                                        x = (viewportState.offset.x + pan.x + focusCompensation.x)
                                            .coerceIn(-maxOffsetX, maxOffsetX),
                                        y = (viewportState.offset.y + pan.y + focusCompensation.y)
                                            .coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                }
                            }
                        }
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.editor_canvas_description),
                        modifier = Modifier.fillMaxSize()
                    )

                    DrawingCanvas(
                        paths = documentState.paths,
                        currentTool = documentState.currentTool,
                        currentColor = documentState.currentColor,
                        strokeWidth = documentState.strokeWidth,
                        eraserSize = documentState.eraserSize,
                        eraserMode = documentState.eraserMode,
                        textSize = documentState.textSize,
                        textOutlineEnabled = documentState.textOutlineEnabled,
                        shapeFilled = documentState.shapeFilled,
                        selectedPathIndex = documentState.selectedPathIndex,
                        onPathAdded = screenActions.onAddPath,
                        onPathUpdated = screenActions.onUpdatePath,
                        onPathReplaced = screenActions.onReplacePath,
                        onPathRemoved = screenActions.onRemovePath,
                        onPathSelectionChanged = screenActions.onSelectPath,
                        onCanvasSizeChanged = onCanvasSizeChanged,
                        onViewportResetRequested = { viewportState.reset() }
                    )
                }
            }
        }
    }
}

private data class EditorToolSpec(
    val tool: DrawingTool,
    val icon: ImageVector,
    val labelRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    onClose: () -> Unit,
    onSave: () -> Unit,
    onPin: () -> Unit,
    onShare: () -> Unit,
    onRecrop: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canDeleteSelection: Boolean,
    onDeleteSelection: () -> Unit,
    onResetViewport: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.editor_title)) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
            }
        },
        actions = {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.Default.Undo, contentDescription = stringResource(R.string.action_undo))
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.Default.Redo, contentDescription = stringResource(R.string.action_redo))
            }
            IconButton(onClick = onResetViewport) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_reset_view))
            }
            IconButton(onClick = onDeleteSelection, enabled = canDeleteSelection) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
            }
            IconButton(onClick = onRecrop) {
                Icon(Icons.Default.Crop, contentDescription = stringResource(R.string.action_recrop))
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.action_share))
            }
            IconButton(onClick = onPin) {
                Icon(Icons.Default.PushPin, contentDescription = stringResource(R.string.action_pin))
            }
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Save, contentDescription = stringResource(R.string.action_save))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorBottomBar(
    documentState: EditorDocumentState,
    toolPanelState: EditorToolPanelState,
    screenActions: EditorScreenActions
) {
    var showColorPaletteDialog by remember { mutableStateOf(false) }

    val tools = listOf(
        EditorToolSpec(DrawingTool.MOVE, Icons.Default.OpenWith, R.string.tool_move),
        EditorToolSpec(DrawingTool.PEN, Icons.Default.Edit, R.string.tool_pen),
        EditorToolSpec(DrawingTool.ERASER, Icons.Default.AutoFixOff, R.string.tool_eraser),
        EditorToolSpec(DrawingTool.ARROW, Icons.Default.TrendingUp, R.string.tool_arrow),
        EditorToolSpec(DrawingTool.RECTANGLE, Icons.Default.CropSquare, R.string.tool_rectangle),
        EditorToolSpec(DrawingTool.CIRCLE, Icons.Default.Circle, R.string.tool_circle),
        EditorToolSpec(DrawingTool.TEXT, Icons.Default.TextFields, R.string.tool_text)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 4.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(tools) { spec ->
                    FilterChip(
                        selected = toolPanelState.currentTool == spec.tool,
                        onClick = { screenActions.onSelectTool(spec.tool) },
                        label = { Text(stringResource(spec.labelRes)) },
                        leadingIcon = {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            EditorControlCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(184.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (toolPanelState.showStrokeControls) {
                        EditorSliderRow(
                            title = stringResource(R.string.editor_stroke_width, documentState.strokeWidth.toInt()),
                            value = documentState.strokeWidth,
                            valueRange = 2f..32f,
                            onValueChange = screenActions.onSelectStrokeWidth
                        )
                    }

                    if (toolPanelState.showTextControls) {
                        EditorSliderRow(
                            title = stringResource(R.string.editor_text_size, documentState.textSize.toInt()),
                            value = documentState.textSize,
                            valueRange = 14f..72f,
                            onValueChange = screenActions.onSelectTextSize
                        )
                        EditorSwitchRow(
                            title = stringResource(R.string.editor_text_outline),
                            checked = documentState.textOutlineEnabled,
                            onCheckedChange = screenActions.onSelectTextOutlineEnabled
                        )
                    }

                    if (toolPanelState.showFillControls) {
                        EditorSwitchRow(
                            title = stringResource(R.string.editor_shape_fill),
                            checked = documentState.shapeFilled,
                            onCheckedChange = screenActions.onSelectShapeFilled
                        )
                    }

                    if (toolPanelState.showEraserControls) {
                        EditorSliderRow(
                            title = stringResource(R.string.editor_eraser_size, documentState.eraserSize.toInt()),
                            value = documentState.eraserSize,
                            valueRange = 12f..96f,
                            onValueChange = screenActions.onSelectEraserSize
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = documentState.eraserMode == EraserMode.OBJECT,
                                onClick = { screenActions.onSelectEraserMode(EraserMode.OBJECT) },
                                label = { Text(stringResource(R.string.editor_eraser_object)) }
                            )
                            FilterChip(
                                selected = documentState.eraserMode == EraserMode.PARTIAL,
                                onClick = { screenActions.onSelectEraserMode(EraserMode.PARTIAL) },
                                label = { Text(stringResource(R.string.editor_eraser_partial)) }
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.editor_color_palette),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ColorButton(
                            color = documentState.currentColor,
                            isSelected = true,
                            onClick = { showColorPaletteDialog = true }
                        )
                        OutlinedButton(onClick = { showColorPaletteDialog = true }) {
                            Text("色板")
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(documentState.availableColors, key = { it.hashCode() }) { color ->
                            ColorButton(
                                color = color,
                                isSelected = documentState.currentColor == color,
                                onClick = { screenActions.onSelectColor(color) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showColorPaletteDialog) {
        ColorPaletteDialog(
            initialColor = documentState.currentColor,
            onDismiss = { showColorPaletteDialog = false },
            onConfirm = { color ->
                screenActions.onSelectColor(color)
                showColorPaletteDialog = false
            }
        )
    }
}

@Composable
private fun EditorControlCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            content = content
        )
    }
}

@Composable
private fun EditorSliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange
    )
}

@Composable
private fun EditorSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun ColorButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (isSelected) 42.dp else 36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ColorPaletteDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    val hsv = remember(initialColor) {
        FloatArray(3).apply {
            AndroidColor.colorToHSV(initialColor.toArgb(), this)
        }
    }
    var hue by remember(initialColor) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(hsv[1]) }
    var value by remember(initialColor) { mutableFloatStateOf(hsv[2]) }
    val previewColor = Color.hsv(hue, saturation, value)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(previewColor)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.32f),
                            RoundedCornerShape(12.dp)
                        )
                )
                EditorSliderRow(
                    title = "色相 ${hue.toInt()}",
                    value = hue,
                    valueRange = 0f..360f,
                    onValueChange = { hue = it }
                )
                EditorSliderRow(
                    title = "饱和度 ${(saturation * 100).toInt()}%",
                    value = saturation,
                    valueRange = 0f..1f,
                    onValueChange = { saturation = it }
                )
                EditorSliderRow(
                    title = "明度 ${(value * 100).toInt()}%",
                    value = value,
                    valueRange = 0f..1f,
                    onValueChange = { value = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(previewColor) }) {
                Text(stringResource(R.string.action_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun currentToolAllowsViewportTransform(tool: DrawingTool?): Boolean {
    return tool == null
}
