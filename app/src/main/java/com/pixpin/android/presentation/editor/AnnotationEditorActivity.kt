package com.pixpin.android.presentation.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.pixpin.android.domain.usecase.AnnotationRenderer
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.pixpin.android.R
import com.pixpin.android.domain.model.DrawingTool
import com.pixpin.android.domain.usecase.AnnotationSession
import com.pixpin.android.domain.usecase.AnnotationSessionStore
import com.pixpin.android.domain.usecase.ImageSaver
import com.pixpin.android.domain.usecase.CacheImageStore
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.domain.usecase.CaptureFlowSettings
import com.pixpin.android.domain.usecase.PinHistorySourceType
import com.pixpin.android.presentation.crop.RegionCropActivity
import com.pixpin.android.presentation.theme.PixPinTheme
import com.pixpin.android.service.PinOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnnotationEditorActivity : ComponentActivity() {

    private val viewModel: AnnotationViewModel by viewModels()
    private lateinit var imageSaver: ImageSaver
    private lateinit var cacheImageStore: CacheImageStore
    private lateinit var captureFlowSettings: CaptureFlowSettings
    private var sourceImageUriString: String? = null
    private var capturedBitmap: Bitmap? = null
    private var editorCanvasSize: Size = Size.Zero
    private val annotationRenderer = AnnotationRenderer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageSaver = ImageSaver(this)
        cacheImageStore = CacheImageStore(this)
        captureFlowSettings = CaptureFlowSettings(this)

        val sessionId = intent.getStringExtra(EXTRA_ANNOTATION_SESSION_ID)
        val restoredSession = sessionId?.let { AnnotationSessionStore.get(this, it) }
        val uriString = restoredSession?.sourceImageUri ?: intent.getStringExtra(EXTRA_IMAGE_URI)
        if (uriString.isNullOrBlank()) {
            Toast.makeText(this, "无法加载截图", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        sourceImageUriString = uriString

        try {
            val uri = Uri.parse(uriString)
            contentResolver.openInputStream(uri)?.use { input ->
                capturedBitmap = BitmapFactory.decodeStream(input)
            }
            if (capturedBitmap == null) throw Exception("Bitmap could not be decoded.")
            restoredSession?.let { session ->
                editorCanvasSize = session.canvasSize
                viewModel.replacePaths(session.paths)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "加载截图失败: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            PixPinTheme {
                AnnotationEditorScreen()
            }
        }
    }

    @Composable
    fun AnnotationEditorScreen() {
        val bitmap = capturedBitmap ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

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
                    onRedo = { viewModel.redo() }
                )
            },
            bottomBar = {
                EditorBottomBar(
                    viewModel = viewModel
                )
            }
        ) { paddingValues ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val imageAspect = if (bitmap.height == 0) 1f else bitmap.width.toFloat() / bitmap.height.toFloat()
                val containerAspect = maxWidth.value / maxHeight.value
                val imageModifier = if (containerAspect > imageAspect) {
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(imageAspect)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(imageAspect)
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .then(imageModifier)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "截图",
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
                        selectedTextIndex = viewModel.selectedTextIndex.value,
                        onPathAdded = { path ->
                            val index = viewModel.addPath(path)
                            if (path is com.pixpin.android.domain.model.DrawingPath.TextPath) {
                                viewModel.selectTextPath(index, path)
                            }
                        },
                        onPathUpdated = { index, path -> viewModel.updatePath(index, path) },
                        onPathReplaced = { index, replacements -> viewModel.replacePath(index, replacements) },
                        onPathRemoved = { index -> viewModel.removePath(index) },
                        onTextSelectionChanged = { index, path ->
                            viewModel.selectTextPath(index, path)
                            if (index != null) {
                                viewModel.selectTool(DrawingTool.TEXT)
                            }
                        },
                        onCanvasSizeChanged = { editorCanvasSize = it }
                    )
                }
            }
        }
    }

    private fun saveImage(originalBitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val bitmap = createAnnotatedBitmap(originalBitmap)
                imageSaver.saveToGallery(bitmap)
                Toast.makeText(this@AnnotationEditorActivity, R.string.screenshot_saved, Toast.LENGTH_SHORT).show()
                closeScreenshotFlow()
            } catch (e: Exception) {
                Toast.makeText(this@AnnotationEditorActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pinImage(originalBitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val bitmap = createAnnotatedBitmap(originalBitmap)
                val uri = cacheImageStore.writePngToCache(bitmap, "pinned", "pinned_image")
                val sessionId = sourceImageUriString?.let { imageUri ->
                    AnnotationSessionStore.put(
                        this@AnnotationEditorActivity,
                        AnnotationSession(
                            sourceImageUri = imageUri,
                            canvasSize = editorCanvasSize,
                            paths = viewModel.paths.toList()
                        )
                    )
                }
                AnnotationSessionStore.prune(
                    this@AnnotationEditorActivity,
                    maxCount = captureFlowSettings.getMaxSessionCount(),
                    maxDays = captureFlowSettings.getRetainDays()
                )
                val intent = Intent(this@AnnotationEditorActivity, PinOverlayService::class.java).apply {
                    putExtra(PinOverlayService.EXTRA_IMAGE_URI, uri.toString())
                    putExtra(PinOverlayService.EXTRA_ANNOTATION_SESSION_ID, sessionId)
                    putExtra(PinOverlayService.EXTRA_HISTORY_SOURCE, PinHistorySourceType.EDITOR_EXPORT.value)
                }
                startService(intent)
                Toast.makeText(this@AnnotationEditorActivity, R.string.image_pinned, Toast.LENGTH_SHORT).show()
                closeScreenshotFlow()
            } catch (e: Exception) {
                Toast.makeText(this@AnnotationEditorActivity, "贴图失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareImage(originalBitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val bitmap = createAnnotatedBitmap(originalBitmap)
                imageSaver.shareImage(bitmap)
            } catch (e: Exception) {
                Toast.makeText(this@AnnotationEditorActivity, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    cacheImageStore.writePngToCache(current, "screenshots", "recrop")
                }
                current.recycle()

                val intent = Intent(this@AnnotationEditorActivity, RegionCropActivity::class.java).apply {
                    putExtra(RegionCropActivity.EXTRA_IMAGE_URI, uri.toString())
                    putExtra(RegionCropActivity.EXTRA_FORCE_RESULT_ACTION, CaptureResultAction.OPEN_EDITOR.value)
                }
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AnnotationEditorActivity, "重新选区失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
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
    onRedo: () -> Unit
) {
    TopAppBar(
        title = { Text("标注编辑") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "关闭")
            }
        },
        actions = {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.Default.Undo, contentDescription = stringResource(R.string.action_undo))
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.Default.Redo, contentDescription = stringResource(R.string.action_redo))
            }
            IconButton(onClick = onRecrop) {
                Icon(Icons.Default.Crop, contentDescription = "重新选区")
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
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorBottomBar(viewModel: AnnotationViewModel) {
    val showTextControls =
        viewModel.currentTool.value == DrawingTool.TEXT || viewModel.selectedTextIndex.value != null
    val showEraserControls = viewModel.currentTool.value == DrawingTool.ERASER

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ToolButton(
                    icon = Icons.Default.Edit,
                    text = "画笔",
                    isSelected = viewModel.currentTool.value == DrawingTool.PEN,
                    onClick = { viewModel.selectTool(DrawingTool.PEN) }
                )
                ToolButton(
                    icon = Icons.Default.AutoFixOff,
                    text = "橡皮擦",
                    isSelected = viewModel.currentTool.value == DrawingTool.ERASER,
                    onClick = { viewModel.selectTool(DrawingTool.ERASER) }
                )
                ToolButton(
                    icon = Icons.Default.TrendingUp,
                    text = "箭头",
                    isSelected = viewModel.currentTool.value == DrawingTool.ARROW,
                    onClick = { viewModel.selectTool(DrawingTool.ARROW) }
                )
                ToolButton(
                    icon = Icons.Default.CropSquare,
                    text = "矩形",
                    isSelected = viewModel.currentTool.value == DrawingTool.RECTANGLE,
                    onClick = { viewModel.selectTool(DrawingTool.RECTANGLE) }
                )
                ToolButton(
                    icon = Icons.Default.Circle,
                    text = "圆形",
                    isSelected = viewModel.currentTool.value == DrawingTool.CIRCLE,
                    onClick = { viewModel.selectTool(DrawingTool.CIRCLE) }
                )
                ToolButton(
                    icon = Icons.Default.TextFields,
                    text = "文字",
                    isSelected = viewModel.currentTool.value == DrawingTool.TEXT,
                    onClick = { viewModel.selectTool(DrawingTool.TEXT) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showTextControls) {
                Text(
                    text = "文字大小：${viewModel.textSize.value.toInt()}",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = viewModel.textSize.value,
                    onValueChange = { viewModel.selectTextSize(it) },
                    valueRange = 14f..72f
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "文字描边",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Switch(
                        checked = viewModel.textOutlineEnabled.value,
                        onCheckedChange = { viewModel.selectTextOutlineEnabled(it) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (showEraserControls) {
                Text(
                    text = "橡皮大小：${viewModel.eraserSize.value.toInt()}",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = viewModel.eraserSize.value,
                    onValueChange = { viewModel.selectEraserSize(it) },
                    valueRange = 12f..96f
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = viewModel.eraserMode.value == EraserMode.OBJECT,
                        onClick = { viewModel.selectEraserMode(EraserMode.OBJECT) },
                        label = { Text("整笔擦除") }
                    )
                    FilterChip(
                        selected = viewModel.eraserMode.value == EraserMode.PARTIAL,
                        onClick = { viewModel.selectEraserMode(EraserMode.PARTIAL) },
                        label = { Text("局部擦除") }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.availableColors) { color ->
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

@Composable
fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = text,
                    tint = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
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
            .size(if (isSelected) 48.dp else 40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}
