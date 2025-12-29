package com.lfr.baozi.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lfr.baozi.data.database.Conversation
import com.lfr.baozi.data.database.ConversationStatus
import com.lfr.baozi.ui.viewmodel.ChatViewModel
import com.lfr.baozi.ui.viewmodel.MessageRole
import com.lfr.baozi.ui.viewmodel.StepTiming
import com.lfr.baozi.util.ActivityLaunchUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var userInput by remember { mutableStateOf("") }

    // 图片预览状态
    var previewImageIndex by remember { mutableStateOf<Int?>(null) }
    val allImageMessages =
        remember(uiState.messages) { uiState.messages.filter { it.imagePath != null } }

    val taskMessage =
        remember(uiState.messages) { uiState.messages.firstOrNull { it.role == MessageRole.USER } }
    val stepMessages =
        remember(uiState.messages) { uiState.messages.filter { it.role == MessageRole.ASSISTANT } }
    val timeFormatter = remember { SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault()) }
    val startedAt = uiState.taskStartedAt ?: taskMessage?.timestamp
    val endedAt = uiState.taskEndedAt

    val showResultCard =
        !uiState.isLoading &&
            uiState.taskStatus != ConversationStatus.RUNNING &&
            (endedAt != null || !uiState.taskResultMessage.isNullOrBlank())

    val showRecommendations = !uiState.isLoading && uiState.taskStatus != ConversationStatus.RUNNING

    var previousStatus by remember { mutableStateOf<ConversationStatus?>(null) }

    LaunchedEffect(uiState.currentConversationId) {
        previousStatus = null
        listState.scrollToItem(0)
    }

    LaunchedEffect(
        uiState.currentConversationId,
        uiState.messages.size,
        showResultCard,
        showRecommendations,
        uiState.isLoading,
        uiState.taskStatus
    ) {
        val base = 1 + stepMessages.size // header + steps
        val loadingExtra = if (uiState.isLoading) 1 else 0
        val resultExtra = if (showResultCard) 1 else 0
        val recExtra = if (showRecommendations) 1 else 0
        val lastIndex = (base + loadingExtra + resultExtra + recExtra - 1).coerceAtLeast(0)

        val justFinished =
            previousStatus == ConversationStatus.RUNNING &&
                uiState.taskStatus != ConversationStatus.RUNNING
        previousStatus = uiState.taskStatus

        val shouldAutoScroll =
            uiState.isLoading ||
                uiState.taskStatus == ConversationStatus.RUNNING ||
                justFinished

        if (shouldAutoScroll) {
            listState.scrollToItem(lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 信息流（任务 -> 步骤 -> 结果）
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(key = "task_header") {
                TaskHeaderCard(
                    taskText = taskMessage?.content,
                    startedAt = startedAt,
                    formatter = timeFormatter,
                    status = uiState.taskStatus
                )
            }

            itemsIndexed(stepMessages, key = { _, m -> m.id }) { index, message ->
                StepCard(
                    stepNumber = index + 1,
                    message = message,
                    stepTiming = uiState.stepTimings.firstOrNull { it.step == index },
                    onImageClick = { path ->
                        val imageIndex = allImageMessages.indexOfFirst { it.imagePath == path }
                        if (imageIndex != -1) previewImageIndex = imageIndex
                    }
                )
            }

            if (uiState.isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (uiState.isPaused) Color(0xFFFFF9C4)
                                    else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.isPaused) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFFBC02D)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "任务已暂停",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF827717)
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "执行中…（步骤 ${stepMessages.size + 1}）",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            if (showResultCard) {
                item(key = "task_result") {
                    TaskResultCard(
                        status = uiState.taskStatus,
                        startedAt = startedAt,
                        endedAt = endedAt,
                        message = uiState.taskResultMessage
                    )
                }
            }

            if (showRecommendations) {
                item(key = "recommendations") {
                    RecommendationsSection(
                        onPick = { text ->
                            scope.launch {
                                val started = viewModel.sendMessage(text)
                                if (started) userInput = ""
                            }
                        }
                    )
                }
            }
        }

        ChatInputBar(
            value = userInput,
            enabled = !uiState.isLoading,
            isLoading = uiState.isLoading,
            isPaused = uiState.isPaused,
            onValueChange = { userInput = it },
            onSend = {
                val input = userInput
                scope.launch {
                    val started = viewModel.sendMessage(input)
                    if (started) userInput = ""
                }
            },
            onPauseToggle = { viewModel.togglePause() },
            onStop = { viewModel.stopTask() }
        )
    }

    // 全屏图片预览
    previewImageIndex?.let { initialIndex ->
        ImagePreviewDialog(
            imageMessages = allImageMessages,
            initialIndex = initialIndex,
            onDismiss = { previewImageIndex = null }
        )
    }

    if (uiState.showAccessibilityEnableDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAccessibilityEnableDialog() },
            title = { Text("需要开启无障碍服务") },
            text = { Text("执行任务需要无障碍服务权限，请前往“设置”页开启后再发送任务。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissAccessibilityEnableDialog()
                        try {
                            val intent =
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            ActivityLaunchUtils.startActivityNoAnimation(context, intent)
                        } catch (_: Exception) {
                            onNavigateToSettings()
                        }
                    }
                ) { Text("去开启") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAccessibilityEnableDialog() }) { Text("取消") }
            }
        )
    }

    if (uiState.showAppsPermissionGuideDialog) {
        val detectedCount = uiState.appsDetectedCountForGuide ?: 0
        AlertDialog(
            onDismissRequest = { viewModel.dismissAppsPermissionGuideDialog() },
            title = { Text("需要应用列表权限") },
            text = {
                Text(
                    "检测到可获取的应用数量较少（$detectedCount）。部分机型需要手动开启“获取应用列表”权限，否则可能无法正确选择/控制目标应用。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissAppsPermissionGuideDialog()
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        ActivityLaunchUtils.startActivityNoAnimation(context, intent)
                    }
                ) { Text("去授权") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.dismissAppsPermissionGuideDialog() }) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = {
                            viewModel.dismissAppsPermissionGuideDialog()
                            val input = userInput
                            scope.launch {
                                val started =
                                    viewModel.sendMessage(
                                        input,
                                        skipAppsPermissionGuide = true
                                    )
                                if (started) userInput = ""
                            }
                        }
                    ) { Text("继续执行") }
                }
            }
        )
    }
}

@Composable
private fun TaskHeaderCard(
    taskText: String?,
    startedAt: Long?,
    formatter: SimpleDateFormat,
    status: ConversationStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "任务",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (startedAt != null) {
                    Text(
                        text = "开始：${formatter.format(Date(startedAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (!taskText.isNullOrBlank()) {
                Text(
                    text = taskText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    text = "尚未开始任务，输入描述后点击发送",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }

            if (status == ConversationStatus.RUNNING) {
                Text(
                    text = "状态：进行中",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun StepCard(
    stepNumber: Int,
    message: com.lfr.baozi.ui.viewmodel.ChatMessage,
    stepTiming: StepTiming?,
    onImageClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "步骤 $stepNumber",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                stepTiming?.let {
                    Text(
                        text = "耗时 ${formatDuration(it.totalMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val actionText = message.action ?: message.content
            if (actionText.isNotBlank()) {
                Surface(
                    tonalElevation = 1.dp,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = actionText,
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                        modifier = Modifier.padding(7.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            stepTiming?.let {
                val breakdown =
                    buildString {
                        if (it.screenshotMs > 0) append("截图 ${formatDuration(it.screenshotMs)}  ")
                        if (it.networkMs > 0) append("模型 ${formatDuration(it.networkMs)}  ")
                        if (it.executionMs > 0) append("执行 ${formatDuration(it.executionMs)}")
                    }.trim()
                if (breakdown.isNotBlank()) {
                    Text(
                        text = breakdown,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val thinking = message.thinking?.trim().orEmpty()
            if (thinking.isNotEmpty()) {
                ExpandableTextBlock(
                    title = "思考过程",
                    text = thinking,
                    id = "thinking_${message.id}"
                )
            }

            message.imagePath?.let { path ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "截图",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AsyncImage(
                        model = File(path),
                        contentDescription = "步骤截图",
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp, max = 420.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onImageClick(path) },
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "点击图片可全屏查看",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableTextBlock(
    title: String,
    text: String,
    id: String
) {
    var expanded by rememberSaveable(id) { mutableStateOf(false) }

    Surface(
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(7.dp)) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (expanded) "收起" else "展开",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text,
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TaskResultCard(
    status: ConversationStatus,
    startedAt: Long?,
    endedAt: Long?,
    message: String?
) {
    val (containerColor, title) =
        when (status) {
            ConversationStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer to "任务完成"
            ConversationStatus.ABORTED -> MaterialTheme.colorScheme.tertiaryContainer to "任务中止"
            ConversationStatus.ENDED -> MaterialTheme.colorScheme.errorContainer to "任务结束"
            ConversationStatus.RUNNING -> MaterialTheme.colorScheme.surfaceVariant to "任务进行中"
            else -> MaterialTheme.colorScheme.surfaceVariant to "任务状态"
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        val totalMs =
            if (startedAt != null && endedAt != null && endedAt >= startedAt) endedAt - startedAt else null

        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            if (!message.isNullOrBlank()) {
                Text(text = message, style = MaterialTheme.typography.bodySmall)
            }

            totalMs?.let {
                Text(
                    text = "总耗时：${formatDuration(it)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "状态：${status.displayName()}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun RecommendationsSection(
    onPick: (String) -> Unit
) {
    val suggestions =
        remember {
            listOf(
                "打开微信，查看未读消息",
                "打开支付宝，查看账单",
                "打开小红书，搜索“包子”",
                "打开设置，连接 Wi‑Fi"
            )
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "为你推荐",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { text ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF2F3F5),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .clickable { onPick(text) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    enabled: Boolean,
    isLoading: Boolean,
    isPaused: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onPauseToggle: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "输入任务描述…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                enabled = enabled,
                maxLines = 3,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
            )

            if (isLoading) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledIconButton(
                        onClick = onPauseToggle,
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFFFBC02D)
                            )
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "继续" else "暂停"
                        )
                    }

                    FilledIconButton(
                        onClick = onStop,
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "停止任务")
                    }
                }
            } else {
                val sendEnabled = value.isNotBlank()
                FilledIconButton(
                    onClick = onSend,
                    enabled = sendEnabled,
                    modifier = Modifier.size(44.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送"
                    )
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs < 1_000) return "${durationMs}ms"
    val seconds = durationMs / 1_000.0
    if (seconds < 60) return String.format(Locale.getDefault(), "%.1fs", seconds)
    val totalSeconds = durationMs / 1_000
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return "${minutes}分${remainingSeconds}秒"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePreviewDialog(
    imageMessages: List<com.lfr.baozi.ui.viewmodel.ChatMessage>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { imageMessages.size }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp,
                beyondViewportPageCount = 1
            ) { page ->
                val message = imageMessages[page]
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = File(message.imagePath!!),
                        contentDescription = "预览图片",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = onDismiss),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // 顶部信息栏
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 页码指示器
                Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp)) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${imageMessages.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                // 关闭按钮
                IconButton(
                    onClick = onDismiss,
                    modifier =
                        Modifier.background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(24.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }

            // 底部动作信息
            val currentMessage = imageMessages[pagerState.currentPage]
            if (currentMessage.action != null) {
                Surface(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "动作执行:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentMessage.action,
                            style =
                                MaterialTheme.typography.bodySmall.copy(
                                    fontFamily =
                                        androidx.compose.ui.text.font.FontFamily
                                            .Monospace
                                ),
                            color = Color.White,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationDrawer(
    conversations: List<Conversation>,
    currentConversationId: String?,
    onConversationClick: (String, String) -> Unit,
    onNewConversation: () -> Unit,
    onDeleteConversation: (String) -> Unit
) {
    ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
        Column(modifier = Modifier.fillMaxHeight()) {
            // 标题和新建按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "任务列表",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onNewConversation) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "新建任务")
                }
            }

            HorizontalDivider()

            // 对话列表
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        isSelected = conversation.id == currentConversationId,
                        onClick = { onConversationClick(conversation.id, conversation.title) },
                        onDelete = { onDeleteConversation(conversation.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val status = remember(conversation.status) { ConversationStatus.fromRaw(conversation.status) }
    val taskTimeMs = conversation.taskStartedAt ?: conversation.updatedAt

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除任务") },
            text = { Text("确定要删除这个任务吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                )
                .clickable(onClick = onClick)
                .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateFormat.format(Date(taskTimeMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (status != ConversationStatus.IDLE) {
                    Text(
                        text = status.displayName(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
