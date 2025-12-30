package com.lfr.baozi.ui.screen

import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lfr.baozi.ui.viewmodel.AdvancedAuthViewModel
import com.lfr.baozi.ui.viewmodel.AdvancedAuthUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdvancedAuthScreen(
    modifier: Modifier = Modifier,
    viewModel: AdvancedAuthViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // 显示消息提示
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            // 消息会在 UI 中显示，不需要 Toast
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "高级授权与无感保活",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item("status_title") {
                    SectionTitle(text = "权限状态")
                }

                item("status_card") {
                    val container =
                        if (uiState.hasWriteSecureSettings) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = container)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconBadge(
                                icon = Icons.Filled.Security,
                                background = if (uiState.hasWriteSecureSettings) Color(0xFF4CAF50) else Color(0xFFE53935)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(text = "WRITE_SECURE_SETTINGS", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (uiState.hasWriteSecureSettings) "已授权，无感保活可用" else "未授权，需要选择下方任一方式授权",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (uiState.hasWriteSecureSettings) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                item("desc_title") {
                    SectionTitle(text = "无感保活说明")
                }

                item("desc_card") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconBadge(icon = Icons.Filled.CheckCircle, background = Color(0xFF1976D2))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "下拉通知栏即可尝试恢复无障碍", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "授权后会在渲染快捷开关时自动检查并尝试重启无障碍服务。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text =
                                    "• 将包子快捷开关添加到通知栏快速设置面板\n" +
                                        "• 打开 Tile 内的“保活开关”，会启动前台保活服务\n" +
                                        "• 系统杀死无障碍/应用崩溃后，下拉通知栏会触发服务检查\n" +
                                        "• 已具备 WRITE_SECURE_SETTINGS 权限时，会自动尝试重新启用无障碍服务",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!uiState.hasWriteSecureSettings) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "提示：需要先完成下方任一授权方式，才能启用无感保活功能。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item("methods_title") {
                    SectionTitle(text = "授权方式")
                }

                item("shizuku") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AuthMethodHeader(
                                icon = Icons.Filled.Security,
                                iconBg = Color(0xFF4CAF50),
                                title = "Shizuku 授权（推荐，免 Root）",
                                subtitle =
                                    when {
                                        !uiState.shizukuAvailable -> "未检测到 Shizuku（请先安装并启动）"
                                        !uiState.shizukuAuthorized -> "已安装，但未授权"
                                        else -> "已授权"
                                    },
                                checked = uiState.shizukuAuthorized && uiState.hasWriteSecureSettings
                            )

                            Text(
                                text =
                                    "• 在应用商店或官网安装 Shizuku\n" +
                                        "• 按 Shizuku 引导完成一次性 ADB 启动\n" +
                                        "• 在 Shizuku 中授予包子所需的系统设置权限",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (!uiState.shizukuAvailable) {
                                    Button(
                                        onClick = { viewModel.openShizukuApp() },
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("安装")
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.requestShizukuPermission() },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        enabled = !uiState.isLoading
                                    ) {
                                        if (uiState.isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text(if (uiState.shizukuAuthorized) "重新授权" else "请求授权")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item("adb") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AuthMethodHeader(
                                icon = Icons.Filled.ContentCopy,
                                iconBg = Color(0xFF0097A7),
                                title = "ADB 授权（开发者/测试推荐）",
                                subtitle = "在电脑终端执行授权命令",
                                checked = uiState.hasWriteSecureSettings
                            )

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = com.lfr.baozi.util.AuthHelper.getAdbGrantCommand(context),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.copyAdbCommand() }) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = "复制命令")
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.copyAdbCommand() },
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("复制 ADB 命令")
                            }
                        }
                    }
                }

                item("root") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AuthMethodHeader(
                                icon = Icons.Filled.AdminPanelSettings,
                                iconBg = Color(0xFFFFB300),
                                title = "Root 授权（高级用户）",
                                subtitle = if (uiState.rootAvailable) "检测到 Root 权限可用" else "未检测到 Root 权限",
                                checked = uiState.rootAvailable && uiState.hasWriteSecureSettings
                            )

                            Text(
                                text =
                                    "• 在已 Root 的设备中，可将本应用提升为系统应用或授予系统权限\n" +
                                        "• 具体操作因 ROM 而异，建议仅高级用户尝试",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { viewModel.requestRootPermission() },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                enabled = uiState.rootAvailable && !uiState.isLoading,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("请求 Root 授权")
                                }
                            }
                        }
                    }
                }

                uiState.message?.let { message ->
                    item("message") {
                        val container =
                            when (uiState.messageType) {
                                AdvancedAuthUiState.MessageType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                                AdvancedAuthUiState.MessageType.ERROR -> MaterialTheme.colorScheme.errorContainer
                                AdvancedAuthUiState.MessageType.INFO -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = container)
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.clearMessage() }) { Text("关闭") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun IconBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color
) {
    Box(
        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AuthMethodHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    checked: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon = icon, background = iconBg)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (checked) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2E7D32)
            )
        }
    }
}
