package com.example.open_autoglm_android.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.open_autoglm_android.data.database.ModelConfig
import com.example.open_autoglm_android.ui.viewmodel.ModelConfigViewModel

// URL验证函数
private fun validateUrl(url: String): String {
    if (url.isBlank()) {
        return "URL不能为空"
    }
    val urlPattern =
        "^https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?$"
    if (!Regex(urlPattern).matches(url)) {
        return "URL格式无效，请输入以http://或https://开头的URL"
    }
    return ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelConfigScreen(
    modifier: Modifier = Modifier,
    viewModel: ModelConfigViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // 编辑对话框状态
    var showEditDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<ModelConfig?>(null) }

    // 新建对话框状态
    var showAddDialog by remember { mutableStateOf(false) }
    var newConfigName by remember { mutableStateOf("") }
    var newApiKey by remember { mutableStateOf("") }
    var newBaseUrl by remember { mutableStateOf("") }
    var newModelName by remember { mutableStateOf("") }
    var newBaseUrlError by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模型配置管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "添加模型")
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = modifier
                .padding(16.dp)
                .padding(it)
        ) {
            items(uiState.modelConfigs) {
                ModelConfigItem(
                    modelConfig = it,
                    onEdit = { config ->
                        editingConfig = config
                        showEditDialog = true
                    },
                    onDelete = { config ->
                        viewModel.deleteModelConfig(config)
                    },
                    onSelect = { config ->
                        viewModel.selectModel(config.id)
                    },
                    isSelected = uiState.selectedModel?.id == it.id
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // 添加模型对话框
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加新模型") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newConfigName,
                        onValueChange = { newConfigName = it },
                        label = { Text("配置名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newApiKey,
                        onValueChange = { newApiKey = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newBaseUrl,
                        onValueChange = { newBaseUrl = it },
                        label = { Text(text = "Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
//                        keyboardType = KeyboardType.Uri,
                        isError = newBaseUrlError.isNotEmpty(),
                        supportingText = {
                            if (newBaseUrlError.isNotEmpty()) {
                                Text(text = newBaseUrlError)
                            } else {
                                null
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newModelName,
                        onValueChange = { newModelName = it },
                        label = { Text("模型名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 验证URL
                        newBaseUrlError = validateUrl(newBaseUrl)
                        if (newBaseUrlError.isEmpty() && newConfigName.isNotBlank() && newApiKey.isNotBlank() && newModelName.isNotBlank()) {
                            viewModel.insertModelConfig(
                                ModelConfig(
                                    name = newConfigName,
                                    apiKey = newApiKey,
                                    baseUrl = newBaseUrl,
                                    modelName = newModelName
                                )
                            )
                            newConfigName = ""
                            newApiKey = ""
                            newBaseUrl = ""
                            newModelName = ""
                            newBaseUrlError = ""
                            showAddDialog = false
                        }
                    },
                    enabled = newConfigName.isNotBlank() && newApiKey.isNotBlank() && newBaseUrl.isNotBlank() && newModelName.isNotBlank() && newBaseUrlError.isEmpty()
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                Button(onClick = { showAddDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 编辑模型对话框
    if (showEditDialog && editingConfig != null) {
        val config = editingConfig!!
        var editConfigName by remember { mutableStateOf(config.name) }
        var editApiKey by remember { mutableStateOf(config.apiKey) }
        var editBaseUrl by remember { mutableStateOf(config.baseUrl) }
        var editModelName by remember { mutableStateOf(config.modelName) }
        var editBaseUrlError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("编辑模型") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editConfigName,
                        onValueChange = { editConfigName = it },
                        label = { Text("配置名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editApiKey,
                        onValueChange = { editApiKey = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editBaseUrl,
                        onValueChange = { editBaseUrl = it },
                        label = { Text("Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = editBaseUrlError.isNotEmpty(),
                        supportingText = { if (editBaseUrlError.isNotEmpty()) Text(editBaseUrlError) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editModelName,
                        onValueChange = { editModelName = it },
                        label = { Text("模型名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 验证URL
                        editBaseUrlError = validateUrl(editBaseUrl)
                        if (editBaseUrlError.isEmpty() && editConfigName.isNotBlank() && editApiKey.isNotBlank() && editModelName.isNotBlank()) {
                            viewModel.updateModelConfig(
                                config.copy(
                                    name = editConfigName,
                                    apiKey = editApiKey,
                                    baseUrl = editBaseUrl,
                                    modelName = editModelName
                                )
                            )
                            showEditDialog = false
                        }
                    },
                    enabled = editConfigName.isNotBlank() && editApiKey.isNotBlank() && editBaseUrl.isNotBlank() && editModelName.isNotBlank() && editBaseUrlError.isEmpty()
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                Button(onClick = { showEditDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ModelConfigItem(
    modelConfig: ModelConfig,
    onEdit: (ModelConfig) -> Unit,
    onDelete: (ModelConfig) -> Unit,
    onSelect: (ModelConfig) -> Unit,
    isSelected: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = modelConfig.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = modelConfig.modelName,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                RadioButton(
                    selected = isSelected,
                    onClick = { onSelect(modelConfig) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Base URL: ${modelConfig.baseUrl}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "API Key: ${modelConfig.apiKey.take(10)}...",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onEdit(modelConfig) }) {
                    Text("编辑")
                }
                TextButton(onClick = { onDelete(modelConfig) }) {
                    Text("删除")
                }
            }
        }
    }
}