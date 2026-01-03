package com.pixpin.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixpin.android.domain.usecase.PermissionHandler
import com.pixpin.android.presentation.theme.PixPinTheme
import com.pixpin.android.service.FloatingBallService

class MainActivity : ComponentActivity() {

    private lateinit var permissionHandler: PermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        permissionHandler = PermissionHandler(this)

        setContent {
            PixPinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        hasOverlayPermission = permissionHandler.hasOverlayPermission(),
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
        // 如果已有权限，自动启动服务
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
                // 最小化应用
                moveTaskToBack(true)
            }
        }
    }
}

@Composable
fun MainScreen(
    hasOverlayPermission: Boolean,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit
) {
    var permissionGranted by remember { mutableStateOf(hasOverlayPermission) }

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
            modifier = Modifier.size(80.dp),
            tint = if (permissionGranted) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (permissionGranted) 
                "PixPin 已就绪" 
            else 
                stringResource(R.string.permission_overlay_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (permissionGranted) 
                "悬浮球正在运行中\n点击悬浮球快速截图" 
            else 
                stringResource(R.string.permission_overlay_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!permissionGranted) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = stringResource(R.string.btn_grant_permission))
            }
        } else {
            OutlinedButton(
                onClick = onStartService,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "重新启动悬浮球")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "提示：授权后应用会自动最小化到后台",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}
