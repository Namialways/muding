package com.muding.android.presentation.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.muding.android.R
import com.muding.android.app.AppGraph
import com.muding.android.core.model.PinImageAsset
import com.muding.android.core.model.PinSourceType
import com.muding.android.data.image.CachedImageRepository
import com.muding.android.data.image.ImageExportRepository
import com.muding.android.data.repository.AnnotationSessionRepository
import com.muding.android.data.settings.AppSettingsRepository
import com.muding.android.domain.model.DrawingPath
import com.muding.android.domain.model.DrawingTool
import com.muding.android.domain.usecase.AnnotationRenderer
import com.muding.android.domain.usecase.AnnotationSession
import com.muding.android.domain.usecase.CaptureResultAction
import com.muding.android.feature.pin.creation.PinCreationCoordinator
import com.muding.android.presentation.crop.ImageCropActivity
import com.muding.android.presentation.theme.MudingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnnotationEditorActivity : ComponentActivity() {

    private val viewModel: AnnotationViewModel by viewModels()
    private lateinit var imageExportRepository: ImageExportRepository
    private lateinit var cachedImageRepository: CachedImageRepository
    private lateinit var settingsRepository: AppSettingsRepository
    private lateinit var annotationSessionRepository: AnnotationSessionRepository
    private lateinit var pinCreationCoordinator: PinCreationCoordinator
    private var sourceImageUriString: String? = null
    private var capturedBitmap: Bitmap? = null
    private var editorCanvasSize: Size = Size.Zero
    private val annotationRenderer = AnnotationRenderer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageExportRepository = AppGraph.imageExportRepository(this)
        cachedImageRepository = AppGraph.cachedImageRepository(this)
        settingsRepository = AppGraph.appSettingsRepository(this)
        annotationSessionRepository = AppGraph.annotationSessionRepository(this)
        pinCreationCoordinator = AppGraph.pinCreationCoordinator(this)

        val sessionId = intent.getStringExtra(EXTRA_ANNOTATION_SESSION_ID)
        val restoredSession = sessionId?.let { annotationSessionRepository.get(it) }
        val uriString = restoredSession?.sourceImageUri ?: intent.getStringExtra(EXTRA_IMAGE_URI)
        if (uriString.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.editor_loading_failed), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        sourceImageUriString = uriString

        try {
            val uri = Uri.parse(uriString)
            contentResolver.openInputStream(uri)?.use { input ->
                capturedBitmap = BitmapFactory.decodeStream(input)
            }
            if (capturedBitmap == null) throw IllegalStateException("Bitmap decode failed")
            restoredSession?.let { session ->
                editorCanvasSize = session.canvasSize
                viewModel.replacePaths(session.paths)
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.editor_loading_failed_with_reason, e.message ?: ""),
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        setContent {
            MudingTheme {
                AnnotationEditorScreen()
            }
        }
    }

    @Composable
    private fun AnnotationEditorScreen() {
        val bitmap = capturedBitmap ?: return
        val viewportState = remember { EditorViewportState() }

        Scaffold(
            topBar = {
                EditorTopBar(
                    onClose = { closeScreenshotFlow() },
                    onSave = { saveImage(bitmap) },
                    onPin = { pinImage(bitmap) },
                    onShare = { shareImage(bitmap) },
                    onRecrop = { recropImage(bitmap) },
                    canUndo = viewModel.canUndo(),
                    canRedo = viewModel.canRedo(),
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    canDeleteSelection = viewModel.selectedPathIndex.value != null,
                    onDeleteSelection = { viewModel.deleteSelectedPath() },
                    onResetViewport = { viewportState.reset() }
                )
            },
            bottomBar = { EditorBottomBar(viewModel = viewModel) }
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
                val canTransformCanvas = currentToolAllowsViewportTransform(
                    tool = viewModel.currentTool.value
                )

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
                                    detectTransformGestures(
                                    ) { centroid, pan, zoom, _ ->
                                        val previousScale = viewportState.scale
                                        val newScale = (previousScale * zoom).coerceIn(1f, 6f)
                                        val pivot = Offset(contentWidthPx / 2f, contentHeightPx / 2f)
                                        val focusCompensation = (centroid - pivot) * (previousScale - newScale)
                                        val maxOffsetX = ((contentWidthPx * newScale) - contentWidthPx) / 2f
                                        val maxOffsetY = ((contentHeightPx * newScale) - contentHeightPx) / 2f
                                        val nextOffset = Offset(
                                            x = (viewportState.offset.x + pan.x + focusCompensation.x).coerceIn(-maxOffsetX, maxOffsetX),
                                            y = (viewportState.offset.y + pan.y + focusCompensation.y).coerceIn(-maxOffsetY, maxOffsetY)
                                        )
                                        viewportState.scale = newScale
                                        viewportState.offset = nextOffset
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
                            paths = viewModel.paths,
                            currentTool = viewModel.currentTool.value,
                            currentColor = viewModel.currentColor.value,
                            strokeWidth = viewModel.strokeWidth.value,
                            eraserSize = viewModel.eraserSize.value,
                            eraserMode = viewModel.eraserMode.value,
                            textSize = viewModel.textSize.value,
                            textOutlineEnabled = viewModel.textOutlineEnabled.value,
                            shapeFilled = viewModel.shapeFilled.value,
                            selectedPathIndex = viewModel.selectedPathIndex.value,
                            onPathAdded = { path ->
                                val index = viewModel.addPath(path)
                                if (path is DrawingPath.TextPath) {
                                    viewModel.selectPath(index, path)
                                } else {
                                    viewModel.clearSelection()
                                }
                            },
                            onPathUpdated = { index, path -> viewModel.updatePath(index, path) },
                            onPathReplaced = { index, replacements -> viewModel.replacePath(index, replacements) },
                            onPathRemoved = { index -> viewModel.removePath(index) },
                            onPathSelectionChanged = { index, path -> viewModel.selectPath(index, path) },
                            onCanvasSizeChanged = { editorCanvasSize = it },
                            onViewportResetRequested = { viewportState.reset() }
                        )
                    }
                }
            }
        }
    }

    private fun saveImage(originalBitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val bitmap = createAnnotatedBitmap(originalBitmap)
                imageExportRepository.saveToGallery(bitmap)
                Toast.makeText(this@AnnotationEditorActivity, R.string.screenshot_saved, Toast.LENGTH_SHORT).show()
                closeScreenshotFlow()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AnnotationEditorActivity,
                    getString(R.string.editor_save_failed, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun pinImage(originalBitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val bitmap = createAnnotatedBitmap(originalBitmap)
                val uri = cachedImageRepository.writePngToCache(bitmap, "pinned", "pinned_image")
                val sessionId = sourceImageUriString?.let { imageUri ->
                    annotationSessionRepository.save(
                        AnnotationSession(
                            sourceImageUri = imageUri,
                            canvasSize = editorCanvasSize,
                            paths = viewModel.paths.toList()
                        )
                    )
                }
                val projectRecordSettings = settingsRepository.getProjectRecordSettings()
                annotationSessionRepository.prune(
                    maxCount = projectRecordSettings.maxSessionCount,
                    maxDays = projectRecordSettings.retainDays
                )
                pinCreationCoordinator.startPinOverlay(
                    this@AnnotationEditorActivity,
                    pinCreationCoordinator.createImageRequest(
                        sourceType = PinSourceType.EDITOR_EXPORT,
                        imageAsset = PinImageAsset(uri = uri.toString()),
                        annotationSessionId = sessionId
                    )
                )
                Toast.makeText(this@AnnotationEditorActivity, R.string.image_pinned, Toast.LENGTH_SHORT).show()
                closeScreenshotFlow()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AnnotationEditorActivity,
                    getString(R.string.editor_pin_failed, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun shareImage(originalBitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val bitmap = createAnnotatedBitmap(originalBitmap)
                imageExportRepository.shareImage(bitmap)
            } catch (e: Exception) {
                Toast.makeText(
                    this@AnnotationEditorActivity,
                    getString(R.string.editor_share_failed, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun recropImage(originalBitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val current = withContext(Dispatchers.Default) {
                    createAnnotatedBitmap(originalBitmap)
                }
                val uri = withContext(Dispatchers.IO) {
                    cachedImageRepository.writePngToCache(current, "screenshots", "recrop")
                }
                current.recycle()

                val intent = Intent(this@AnnotationEditorActivity, ImageCropActivity::class.java).apply {
                    putExtra(ImageCropActivity.EXTRA_IMAGE_URI, uri.toString())
                    putExtra(ImageCropActivity.EXTRA_FORCE_RESULT_ACTION, CaptureResultAction.OPEN_EDITOR.value)
                }
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AnnotationEditorActivity,
                    getString(R.string.editor_recrop_failed, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun createAnnotatedBitmap(originalBitmap: Bitmap): Bitmap {
        return annotationRenderer.render(
            originalBitmap = originalBitmap,
            paths = viewModel.paths,
            sourceCanvasSize = editorCanvasSize,
            scaledDensity = resources.displayMetrics.scaledDensity
        )
    }

    private fun closeScreenshotFlow() {
        moveTaskToBack(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        capturedBitmap?.recycle()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_ANNOTATION_SESSION_ID = "extra_annotation_session_id"
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
fun EditorBottomBar(viewModel: AnnotationViewModel) {
    val currentTool = viewModel.currentTool.value
    val selectedPath = viewModel.selectedPath()
    var showColorPaletteDialog by remember { mutableStateOf(false) }
    val showStrokeControls =
        currentTool in setOf(DrawingTool.PEN, DrawingTool.ARROW, DrawingTool.RECTANGLE, DrawingTool.CIRCLE) ||
            selectedPath is DrawingPath.PenPath ||
            selectedPath is DrawingPath.ArrowPath ||
            selectedPath is DrawingPath.RectanglePath ||
            selectedPath is DrawingPath.CirclePath
    val showTextControls = currentTool == DrawingTool.TEXT || selectedPath is DrawingPath.TextPath
    val showFillControls =
        currentTool == DrawingTool.RECTANGLE ||
            currentTool == DrawingTool.CIRCLE ||
            selectedPath is DrawingPath.RectanglePath ||
            selectedPath is DrawingPath.CirclePath
    val showEraserControls = currentTool == DrawingTool.ERASER

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
                        selected = currentTool == spec.tool,
                        onClick = { viewModel.selectTool(spec.tool) },
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
                    if (showStrokeControls) {
                        EditorSliderRow(
                            title = stringResource(R.string.editor_stroke_width, viewModel.strokeWidth.value.toInt()),
                            value = viewModel.strokeWidth.value,
                            valueRange = 2f..32f,
                            onValueChange = viewModel::selectStrokeWidth
                        )
                    }

                    if (showTextControls) {
                        EditorSliderRow(
                            title = stringResource(R.string.editor_text_size, viewModel.textSize.value.toInt()),
                            value = viewModel.textSize.value,
                            valueRange = 14f..72f,
                            onValueChange = viewModel::selectTextSize
                        )
                        EditorSwitchRow(
                            title = stringResource(R.string.editor_text_outline),
                            checked = viewModel.textOutlineEnabled.value,
                            onCheckedChange = viewModel::selectTextOutlineEnabled
                        )
                    }

                    if (showFillControls) {
                        EditorSwitchRow(
                            title = stringResource(R.string.editor_shape_fill),
                            checked = viewModel.shapeFilled.value,
                            onCheckedChange = viewModel::selectShapeFilled
                        )
                    }

                    if (showEraserControls) {
                        EditorSliderRow(
                            title = stringResource(R.string.editor_eraser_size, viewModel.eraserSize.value.toInt()),
                            value = viewModel.eraserSize.value,
                            valueRange = 12f..96f,
                            onValueChange = viewModel::selectEraserSize
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = viewModel.eraserMode.value == EraserMode.OBJECT,
                                onClick = { viewModel.selectEraserMode(EraserMode.OBJECT) },
                                label = { Text(stringResource(R.string.editor_eraser_object)) }
                            )
                            FilterChip(
                                selected = viewModel.eraserMode.value == EraserMode.PARTIAL,
                                onClick = { viewModel.selectEraserMode(EraserMode.PARTIAL) },
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
                            color = viewModel.currentColor.value,
                            isSelected = true,
                            onClick = { showColorPaletteDialog = true }
                        )
                        OutlinedButton(onClick = { showColorPaletteDialog = true }) {
                            Text("色板")
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(viewModel.availableColors, key = { it.hashCode() }) { color ->
                            ColorButton(
                                color = color,
                                isSelected = viewModel.currentColor.value == color,
                                onClick = { viewModel.selectColor(color) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showColorPaletteDialog) {
        ColorPaletteDialog(
            initialColor = viewModel.currentColor.value,
            onDismiss = { showColorPaletteDialog = false },
            onConfirm = { color ->
                viewModel.selectColor(color)
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
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
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
