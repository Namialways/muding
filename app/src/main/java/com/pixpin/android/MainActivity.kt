package com.pixpin.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixpin.android.domain.usecase.CaptureFlowSettings
import com.pixpin.android.domain.usecase.CaptureResultAction
import com.pixpin.android.domain.usecase.PinScaleMode
import com.pixpin.android.domain.usecase.PermissionHandler
import com.pixpin.android.presentation.theme.PixPinTheme
import com.pixpin.android.service.FloatingBallService

class MainActivity : ComponentActivity() {

    private lateinit var permissionHandler: PermissionHandler
    private lateinit var captureFlowSettings: CaptureFlowSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionHandler = PermissionHandler(this)
        captureFlowSettings = CaptureFlowSettings(this)

        setContent {
            PixPinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        hasOverlayPermission = permissionHandler.hasOverlayPermission(),
                        initialAction = captureFlowSettings.getResultAction(),
                        initialScaleMode = captureFlowSettings.getPinScaleMode(),
                        onActionChanged = { action -> captureFlowSettings.setResultAction(action) },
                        onScaleModeChanged = { mode -> captureFlowSettings.setPinScaleMode(mode) },
                        onRequestPermission = {
                            permissionHandler.requestOverlayPermission(this)
                        },
                        onStartService = {
                            startFloatingBallService()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (permissionHandler.hasOverlayPermission()) {
            startFloatingBallService()
        }
    }

    private fun startFloatingBallService() {
        val intent = Intent(this, FloatingBallService::class.java)
        startService(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionHandler.REQUEST_CODE_OVERLAY_PERMISSION) {
            if (permissionHandler.hasOverlayPermission()) {
                startFloatingBallService()
                moveTaskToBack(true)
            }
        }
    }
}

@Composable
fun MainScreen(
    hasOverlayPermission: Boolean,
    initialAction: CaptureResultAction,
    initialScaleMode: PinScaleMode,
    onActionChanged: (CaptureResultAction) -> Unit,
    onScaleModeChanged: (PinScaleMode) -> Unit,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit
) {
    var permissionGranted by remember { mutableStateOf(hasOverlayPermission) }
    var selectedAction by remember { mutableStateOf(initialAction) }
    var selectedScaleMode by remember { mutableStateOf(initialScaleMode) }

    LaunchedEffect(hasOverlayPermission) {
        permissionGranted = hasOverlayPermission
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (permissionGranted) Icons.Default.Check else Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.height(80.dp),
            tint = if (permissionGranted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (permissionGranted) "PixPin is ready" else "Overlay permission required",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (permissionGranted)
                "Floating ball is running. Tap it to capture."
            else
                "Grant overlay permission to enable floating capture.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "After selecting a region",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                CaptureOptionRow(
                    title = "Pin to screen directly",
                    selected = selectedAction == CaptureResultAction.PIN_DIRECTLY,
                    onSelect = {
                        selectedAction = CaptureResultAction.PIN_DIRECTLY
                        onActionChanged(CaptureResultAction.PIN_DIRECTLY)
                    }
                )
                CaptureOptionRow(
                    title = "Open editor directly",
                    selected = selectedAction == CaptureResultAction.OPEN_EDITOR,
                    onSelect = {
                        selectedAction = CaptureResultAction.OPEN_EDITOR
                        onActionChanged(CaptureResultAction.OPEN_EDITOR)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Pinned image scaling",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                CaptureOptionRow(
                    title = "Keep aspect ratio",
                    selected = selectedScaleMode == PinScaleMode.LOCK_ASPECT,
                    onSelect = {
                        selectedScaleMode = PinScaleMode.LOCK_ASPECT
                        onScaleModeChanged(PinScaleMode.LOCK_ASPECT)
                    }
                )
                CaptureOptionRow(
                    title = "Free scale (width/height)",
                    selected = selectedScaleMode == PinScaleMode.FREE_SCALE,
                    onSelect = {
                        selectedScaleMode = PinScaleMode.FREE_SCALE
                        onScaleModeChanged(PinScaleMode.FREE_SCALE)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!permissionGranted) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "Grant permission")
            }
        } else {
            OutlinedButton(
                onClick = onStartService,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "Restart floating ball")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "App will minimize automatically after permission is granted.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CaptureOptionRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}
