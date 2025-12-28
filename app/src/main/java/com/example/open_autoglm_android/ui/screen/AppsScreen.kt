package com.example.open_autoglm_android.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.open_autoglm_android.data.AppInfo
import com.example.open_autoglm_android.ui.viewmodel.AppsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = viewModel()
) {
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSelectionMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("应用列表") },
                actions = {
                    // 刷新按钮
                    IconButton(onClick = { viewModel.loadInstalledApps() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    
                    // 选择操作菜单
                    Box {
                        IconButton(onClick = { showSelectionMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "选择操作")
                        }
                        DropdownMenu(
                            expanded = showSelectionMenu,
                            onDismissRequest = { showSelectionMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("选择全部") },
                                onClick = {
                                    viewModel.selectAllApps()
                                    showSelectionMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("全部不选") },
                                onClick = {
                                    viewModel.deselectAllApps()
                                    showSelectionMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("反向选择") },
                                onClick = {
                                    viewModel.invertSelection()
                                    showSelectionMenu = false
                                }
                            )
                        }
                    }
                    
                    // 筛选设置按钮
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "筛选设置")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("搜索应用名称或包名") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true
            )
            
            // 应用列表
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (apps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "未找到匹配的应用" else "未找到已安装的应用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        Text(
                            text = "共 ${apps.size} 个应用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    items(apps, key = { it.packageName }) { app ->
                        AppListItem(
                            app = app,
                            onToggleEnabled = { enabled ->
                                viewModel.toggleAppEnabled(app.packageName, enabled)
                            }
                        )
                    }
                }
            }
            
            // 底部提示信息
            if (!isLoading && apps.size < 3) {
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "部分机型可能需要手动授予获取应用列表的权限",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = {
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("打开应用权限设置")
                    }
                }
            }
        }
    }
    
    // 筛选对话框
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("筛选设置") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleShowSystemApps() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showSystemApps,
                            onCheckedChange = { viewModel.toggleShowSystemApps() }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("显示系统应用")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun AppListItem(
    app: AppInfo,
    modifier: Modifier = Modifier,
    onToggleEnabled: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val iconSizePx = with(density) { 48.dp.roundToPx() }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 应用图标
        if (app.icon != null) {
            val bitmap = remember(app.icon, iconSizePx) {
                app.icon.toBitmap(width = iconSizePx, height = iconSizePx)
            }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = app.appName,
                modifier = Modifier.size(48.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = app.appName,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 应用信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (app.isSystemApp) {
                Text(
                    text = "系统应用",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 启用/禁用开关
        Switch(
            checked = app.isEnabled,
            onCheckedChange = onToggleEnabled
        )
    }
    
    HorizontalDivider()
}
