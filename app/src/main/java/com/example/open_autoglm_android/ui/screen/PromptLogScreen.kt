package com.example.open_autoglm_android.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.open_autoglm_android.ui.viewmodel.ChatViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptLogScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("提示词日志") }
            )
        }
    ) { paddingValues ->
        val log = viewModel.getFullPromptLog()
        val scrollState = rememberScrollState()
        
        // 添加调试信息
        Log.d("PromptLogScreen", "当前对话ID: ${uiState.currentConversationId}")
        Log.d("PromptLogScreen", "消息数量: ${uiState.messages.size}")
        Log.d("PromptLogScreen", "日志长度: ${log.length}")
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            if (log.isBlank()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "暂无日志（执行任务后将记录发送给模型的提示词）",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // 显示调试信息
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "调试信息：\n对话ID: ${uiState.currentConversationId}\n消息数: ${uiState.messages.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Text(
                    text = log,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}
