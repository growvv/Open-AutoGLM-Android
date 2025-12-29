package com.lfr.baozi.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lfr.baozi.R
import com.lfr.baozi.data.InputMode
import com.lfr.baozi.ui.viewmodel.SettingsViewModel
import com.lfr.baozi.util.ActivityLaunchUtils
import com.lfr.baozi.util.AuthHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
    onNavigateToAdvancedAuth: () -> Unit,
    onNavigateToAppsSettings: () -> Unit,
    onNavigateToModelSettings: () -> Unit,
    onNavigateToInputSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hasWriteSecureSettings = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasWriteSecureSettings.value = AuthHelper.hasWriteSecureSettingsPermission(context)
                viewModel.checkAccessibilityService()
                viewModel.checkOverlayPermission()
                viewModel.checkImeStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycleOwner.lifecycle.removeObserver(lifecycleObserver) }
    }

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "permissions") {
            SettingsGroupCard(
                title = "隐私与权限",
                items =
                    listOf(
                        SettingsItem(
                            icon = Icons.Default.AccessibilityNew,
                            iconBg = Color(0xFF4CAF50),
                            title = "无障碍服务",
                            subtitle =
                                when {
                                    !uiState.isAccessibilityEnabled -> "未启用"
                                    uiState.isAccessibilityServiceRunning -> "已启用"
                                    else -> "已启用（连接中）"
                                },
                            onClick = {
                                ActivityLaunchUtils.startActivityNoAnimation(
                                    context,
                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                )
                            }
                        ),
                        SettingsItem(
                            icon = Icons.Default.Apps,
                            iconBg = Color(0xFF7E57C2),
                            title = "应用管理",
                            subtitle = "选择允许被控制的应用",
                            tag = "settings_item_apps",
                            onClick = onNavigateToAppsSettings
                        ),
                        SettingsItem(
                            icon = Icons.Default.Settings,
                            iconBg = Color(0xFF1976D2),
                            title = "悬浮窗权限",
                            subtitle = if (uiState.hasOverlayPermission) "已授权" else "未授权",
                            onClick = {
                                val intent =
                                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                ActivityLaunchUtils.startActivityNoAnimation(context, intent)
                            }
                        )
                    )
            )
        }

        item(key = "features") {
            SettingsGroupCard(
                title = "功能设置",
                items =
                    listOf(
                        SettingsItem(
                            icon = Icons.Default.Key,
                            iconBg = Color(0xFFFFB300),
                            title = "模型与执行",
                            subtitle = "Model / Max Steps",
                            tag = "settings_item_model",
                            onClick = onNavigateToModelSettings
                        ),
                        SettingsItem(
                            icon = Icons.Default.AdminPanelSettings,
                            iconBg = Color(0xFFE53935),
                            title = "高级授权与无感保活",
                            subtitle = if (hasWriteSecureSettings.value) "已授权" else "未授权",
                            onClick = onNavigateToAdvancedAuth
                        ),
                        SettingsItem(
                            icon = Icons.Default.Security,
                            iconBg = Color(0xFF0097A7),
                            title = "输入方式",
                            subtitle =
                                when (uiState.inputMode) {
                                    InputMode.SET_TEXT -> "直接设置文本"
                                    InputMode.PASTE -> "复制粘贴（推荐）"
                                    InputMode.IME ->
                                        when {
                                            !uiState.isImeEnabled -> "输入法未启用"
                                            !uiState.isImeSelected -> "输入法未选中"
                                            else -> "输入法已启用"
                                    }
                                },
                            onClick = onNavigateToInputSettings
                        )
                    )
            )
        }

        item(key = "tips") {
            Text(
                text = "提示：首次使用请先开启无障碍服务。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

private data class SettingsItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconBg: Color,
    val title: String,
    val subtitle: String? = null,
    val tag: String? = null,
    val onClick: () -> Unit
)

@Composable
private fun SettingsGroupCard(
    title: String,
    items: List<SettingsItem>
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            items.forEachIndexed { index, item ->
                SettingsRow(item = item)
                if (index != items.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SettingsRow(item: SettingsItem) {
    val taggedModifier = if (item.tag != null) Modifier.testTag(item.tag) else Modifier
    Row(
        modifier =
            taggedModifier
                .fillMaxWidth()
                .clickable(onClick = item.onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(item.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = item.icon, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = item.title, style = MaterialTheme.typography.bodyLarge)
            item.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
