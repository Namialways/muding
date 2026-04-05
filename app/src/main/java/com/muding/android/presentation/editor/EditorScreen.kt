package com.muding.android.presentation.editor

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.muding.android.R
import com.muding.android.domain.model.DrawingTool
import java.util.Locale

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
        val topInset by animateDpAsState(
            targetValue = (paddingValues.calculateTopPadding() - 18.dp).coerceAtLeast(0.dp),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "editorTopInset"
        )
        val bottomInset by animateDpAsState(
            targetValue = (paddingValues.calculateBottomPadding() - 12.dp).coerceAtLeast(0.dp),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "editorBottomInset"
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        )
                    )
                )
                .padding(top = topInset, bottom = bottomInset)
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
                if (containerAspect > imageAspect) maxHeight.toPx() * imageAspect else maxWidth.toPx()
            }
            val contentHeightPx = with(density) {
                if (containerAspect > imageAspect) maxHeight.toPx() else maxWidth.toPx() / imageAspect
            }
            val canTransformCanvas = currentToolAllowsViewportTransform(tool = documentState.currentTool)

            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(imageModifier),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 2.dp,
                shadowElevation = 10.dp
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
                        .pointerInput(canTransformCanvas, contentWidthPx, contentHeightPx) {
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

private data class EditorActionSpec(
    val icon: ImageVector,
    val labelRes: Int,
    val enabled: Boolean = true,
    val emphasized: Boolean = false,
    val onClick: () -> Unit
)

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
    val actions = listOf(
        EditorActionSpec(Icons.Default.Undo, R.string.action_undo, enabled = canUndo, onClick = onUndo),
        EditorActionSpec(Icons.Default.Redo, R.string.action_redo, enabled = canRedo, onClick = onRedo),
        EditorActionSpec(Icons.Default.Refresh, R.string.action_reset_view, onClick = onResetViewport),
        EditorActionSpec(Icons.Default.Delete, R.string.action_delete, enabled = canDeleteSelection, onClick = onDeleteSelection),
        EditorActionSpec(Icons.Default.Crop, R.string.action_recrop, onClick = onRecrop),
        EditorActionSpec(Icons.Default.Share, R.string.action_share, onClick = onShare),
        EditorActionSpec(Icons.Default.PushPin, R.string.action_pin, emphasized = true, onClick = onPin),
        EditorActionSpec(Icons.Default.Save, R.string.action_save, emphasized = true, onClick = onSave)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EditorIconButton(
                icon = Icons.Default.Close,
                contentDescription = stringResource(R.string.action_close),
                onClick = onClose
            )
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 2.dp)
            ) {
                items(actions, key = { it.labelRes }) { action ->
                    EditorActionButton(action = action)
                }
            }
        }
    }
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
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 6.dp,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(tools, key = { it.tool }) { spec ->
                    EditorToolButton(
                        spec = spec,
                        selected = toolPanelState.currentTool == spec.tool,
                        onClick = { screenActions.onSelectTool(spec.tool) }
                    )
                }
            }

            EditorControlCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .heightIn(max = 252.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(currentToolLabelRes(toolPanelState.currentTool)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.editor_color_palette),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (toolPanelState.showStrokeControls) {
                        EditorSliderRow(
                            label = stringResource(R.string.editor_stroke_width, documentState.strokeWidth.toInt()),
                            valueLabel = documentState.strokeWidth.toInt().toString(),
                            value = documentState.strokeWidth,
                            valueRange = 2f..32f,
                            onValueChange = screenActions.onSelectStrokeWidth
                        )
                    }

                    if (toolPanelState.showTextControls) {
                        EditorSliderRow(
                            label = stringResource(R.string.editor_text_size, documentState.textSize.toInt()),
                            valueLabel = documentState.textSize.toInt().toString(),
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
                            label = stringResource(R.string.editor_eraser_size, documentState.eraserSize.toInt()),
                            valueLabel = documentState.eraserSize.toInt().toString(),
                            value = documentState.eraserSize,
                            valueRange = 12f..96f,
                            onValueChange = screenActions.onSelectEraserSize
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EditorModeButton(
                                label = stringResource(R.string.editor_eraser_object),
                                selected = documentState.eraserMode == EraserMode.OBJECT,
                                onClick = { screenActions.onSelectEraserMode(EraserMode.OBJECT) }
                            )
                            EditorModeButton(
                                label = stringResource(R.string.editor_eraser_partial),
                                selected = documentState.eraserMode == EraserMode.PARTIAL,
                                onClick = { screenActions.onSelectEraserMode(EraserMode.PARTIAL) }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    )

                    EditorColorRail(
                        currentColor = documentState.currentColor,
                        quickAccessColors = documentState.quickAccessColors,
                        onSelectColor = screenActions.onSelectColor,
                        onOpenCustomColor = { showColorPaletteDialog = true }
                    )
                }
            }
        }
    }

    if (showColorPaletteDialog) {
        ColorPaletteDialog(
            initialColor = documentState.currentColor,
            favoriteColors = documentState.favoriteColors,
            recentColors = documentState.recentColors,
            onDismiss = { showColorPaletteDialog = false },
            onToggleFavorite = screenActions.onToggleFavoriteColor,
            onConfirm = { color ->
                screenActions.onApplyColor(color)
                showColorPaletteDialog = false
            }
        )
    }
}

@Composable
private fun EditorActionButton(action: EditorActionSpec) {
    val containerColor = if (action.emphasized) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
    }
    val contentColor = if (action.emphasized) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .alpha(if (action.enabled) 1f else 0.42f)
            .clickable(enabled = action.enabled, onClick = action.onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = stringResource(action.labelRes),
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EditorToolButton(
    spec: EditorToolSpec,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = containerColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = spec.icon,
                contentDescription = stringResource(spec.labelRes),
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EditorModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EditorColorRail(
    currentColor: Color,
    quickAccessColors: List<Color>,
    onSelectColor: (Color) -> Unit,
    onOpenCustomColor: () -> Unit
) {
    val displayedColors = remember(quickAccessColors) {
        quickAccessColors
            .distinctBy { it.toArgb() }
            .take(3)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val color = displayedColors.getOrNull(index)
                if (color != null) {
                    ColorButton(
                        color = color,
                        isSelected = currentColor.toArgb() == color.toArgb(),
                        onClick = { onSelectColor(color) },
                        size = 36.dp
                    )
                } else {
                    EditorEmptyColorSlot()
                }
            }
        }
        EditorPaletteButton(
            color = currentColor,
            onClick = onOpenCustomColor
        )
    }
}

@Composable
private fun EditorIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EditorPaletteButton(
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(width = 54.dp, height = 38.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.5.dp,
                    color = color.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = stringResource(R.string.editor_color_palette),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EditorEmptyColorSlot() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                shape = CircleShape
            )
    )
}

@Composable
private fun EditorControlCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

@Composable
private fun EditorSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            ) {
                Text(
                    text = valueLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun EditorSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun ColorButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    size: Dp = 36.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ColorPaletteDialog(
    initialColor: Color,
    favoriteColors: List<Color>,
    recentColors: List<Color>,
    onDismiss: () -> Unit,
    onToggleFavorite: (Color) -> Unit,
    onConfirm: (Color) -> Unit
) {
    var draftState by remember(initialColor) {
        mutableStateOf(ColorPickerDraftState.fromColor(initialColor))
    }
    val previewColor = draftState.previewColor
    val isFavorite = remember(draftState.previewColor, favoriteColors) {
        favoriteColors.any { it.toArgb() == draftState.previewColor.toArgb() }
    }
    val currentHue = remember(draftState.previewColor) {
        draftState.previewColor.hueDegrees()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.editor_color_palette),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HueSpectrumField(
                    hue = currentHue,
                    onHueChange = { hue ->
                        draftState = draftState.updateColor(
                            colorFromHueChange(
                                currentColor = draftState.previewColor,
                                hue = hue
                            )
                        )
                    }
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = previewColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.editor_color_preview),
                                color = previewColor.readableForeground(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = previewColor.toHexString(),
                                color = previewColor.readableForeground().copy(alpha = 0.82f),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { onToggleFavorite(previewColor) },
                            shape = CircleShape,
                            color = previewColor.readableForeground().copy(alpha = 0.14f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = stringResource(
                                        if (isFavorite) {
                                            R.string.editor_color_remove_favorite
                                        } else {
                                            R.string.editor_color_add_favorite
                                        }
                                    ),
                                    tint = previewColor.readableForeground(),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = draftState.hexText,
                    onValueChange = { draftState = draftState.updateHex(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.editor_color_hex)) },
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ColorValueField(
                        label = stringResource(R.string.editor_color_red),
                        value = draftState.redText,
                        modifier = Modifier.weight(1f),
                        onValueChange = { draftState = draftState.updateRed(it) }
                    )
                    ColorValueField(
                        label = stringResource(R.string.editor_color_green),
                        value = draftState.greenText,
                        modifier = Modifier.weight(1f),
                        onValueChange = { draftState = draftState.updateGreen(it) }
                    )
                    ColorValueField(
                        label = stringResource(R.string.editor_color_blue),
                        value = draftState.blueText,
                        modifier = Modifier.weight(1f),
                        onValueChange = { draftState = draftState.updateBlue(it) }
                    )
                }

                EditorColorSwatchSection(
                    label = stringResource(R.string.editor_color_favorites),
                    colors = favoriteColors,
                    onSelect = { draftState = draftState.updateColor(it) }
                )
                EditorColorSwatchSection(
                    label = stringResource(R.string.editor_color_recents),
                    colors = recentColors,
                    onSelect = { draftState = draftState.updateColor(it) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(previewColor) }) {
                Text(stringResource(R.string.action_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun HueSpectrumField(
    hue: Float,
    onHueChange: (Float) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFF4D4D),
                                Color(0xFFFFB84D),
                                Color(0xFFF7F36B),
                                Color(0xFF7DDD28),
                                Color(0xFF18D7D1),
                                Color(0xFF3B82F6),
                                Color(0xFFA855F7),
                                Color(0xFFFF4D4D)
                            )
                        )
                    )
            )
            Slider(
                value = hue,
                onValueChange = onHueChange,
                valueRange = 0f..360f,
                modifier = Modifier.fillMaxSize(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun ColorValueField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun EditorColorSwatchSection(
    label: String,
    colors: List<Color>,
    onSelect: (Color) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { index ->
                val color = colors.getOrNull(index)
                if (color == null) {
                    EditorEmptyColorSlot()
                } else {
                    ColorButton(
                        color = color,
                        isSelected = false,
                        onClick = { onSelect(color) },
                        size = 32.dp
                    )
                }
            }
        }
    }
}

private fun colorFromHueChange(currentColor: Color, hue: Float): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(currentColor.toArgb(), hsv)
    hsv[0] = hue
    if (hsv[1] < 0.55f) hsv[1] = 0.72f
    if (hsv[2] < 0.55f) hsv[2] = 0.88f
    return Color(AndroidColor.HSVToColor(hsv))
}

private fun Color.hueDegrees(): Float {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(toArgb(), hsv)
    return hsv[0]
}

private fun currentToolLabelRes(tool: DrawingTool?): Int {
    return when (tool) {
        DrawingTool.MOVE -> R.string.tool_move
        DrawingTool.PEN -> R.string.tool_pen
        DrawingTool.ERASER -> R.string.tool_eraser
        DrawingTool.ARROW -> R.string.tool_arrow
        DrawingTool.RECTANGLE -> R.string.tool_rectangle
        DrawingTool.CIRCLE -> R.string.tool_circle
        DrawingTool.TEXT -> R.string.tool_text
        null -> R.string.editor_title
    }
}

private fun Color.readableForeground(): Color {
    return if (luminance() > 0.58f) Color(0xFF111827) else Color.White
}

private fun Color.toHexString(): String {
    return String.format(Locale.US, "#%06X", toArgb() and 0x00FFFFFF)
}

private fun currentToolAllowsViewportTransform(tool: DrawingTool?): Boolean {
    return tool == null
}
