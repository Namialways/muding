package com.muding.android.presentation.translation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.muding.android.presentation.main.rememberMainUiTokens
import com.muding.android.presentation.theme.MudingTheme

class TranslationSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MudingTheme {
                TranslationSettingsRoute(
                    onClose = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslationSettingsRoute(
    onClose: () -> Unit
) {
    val tokens = rememberMainUiTokens()

    Scaffold(
        containerColor = tokens.palette.pageBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("翻译设置") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = tokens.palette.pageBackground
                )
            )
        }
    ) { innerPadding ->
        TranslationSettingsPage(
            modifier = Modifier.padding(innerPadding),
            title = "翻译设置",
            description = "设置目标语言、本地模型和云翻译服务。"
        )
    }
}
