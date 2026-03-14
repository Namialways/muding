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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.pixpin.android.R
import com.pixpin.android.domain.model.DrawingTool
import com.pixpin.android.domain.usecase.ImageSaver
import com.pixpin.android.domain.usecase.CacheImageStore
import com.pixpin.android.domain.usecase.CaptureResultAction
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
    private var capturedBitmap: Bitmap? = null
    private val annotationRenderer = AnnotationRenderer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageSaver = ImageSaver(this)
        cacheImageStore = CacheImageStore(this)

        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (uriString.isNullOrBlank()) {
            Toast.makeText(this, "无法加载截图", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            val uri = Uri.parse(uriString)
            contentResolver.openInputStream(uri)?.use { input ->
                capturedBitmap = BitmapFactory.decodeStream(input)
            }
            if (capturedBitmap == null) throw Exception("Bitmap could not be decoded.")
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Screenshot",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                DrawingCanvas(
                    paths = viewModel.paths,
                    currentTool = viewModel.currentTool.value,
                    currentColor = viewModel.currentColor.value,
                    strokeWidth = viewModel.strokeWidth.value,
                    onPathAdded = { path ->
                        viewModel.addPath(path)
                    }
                )
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
                val intent = Intent(this@AnnotationEditorActivity, PinOverlayService::class.java).apply {
                    putExtra(PinOverlayService.EXTRA_IMAGE_URI, uri.toString())
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
        return annotationRenderer.render(originalBitmap, viewModel.paths)
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

@Composable
fun EditorBottomBar(viewModel: AnnotationViewModel) {
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
            }

            Spacer(modifier = Modifier.height(16.dp))

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
