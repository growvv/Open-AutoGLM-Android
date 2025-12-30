package com.lfr.baozi.ui.screen

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.AnnotatedString
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToInputSettings: () -> Unit = {}
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
    var recommendationsNonce by
        remember(uiState.currentConversationId, uiState.taskEndedAt, uiState.taskStatus) {
            mutableIntStateOf(0)
        }

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
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
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
                        val seed =
                            (uiState.taskEndedAt ?: uiState.taskStartedAt ?: 0L) xor
                                (uiState.currentConversationId?.hashCode()?.toLong() ?: 0L) xor
                                recommendationsNonce.toLong()
                        RecommendationsSection(
                            seed = seed,
                            onPick = { text ->
                                scope.launch {
                                    val started = viewModel.sendMessage(text)
                                    if (started) userInput = ""
                                }
                            },
                            onRefresh = { recommendationsNonce++ }
                        )
                    }
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

    if (uiState.showTypeIssueDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTypeIssueDialog() },
            title = { Text("提示") },
            text = { Text("输入疑似遇到一点问题...\n 尝试切换输入方式吗") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissTypeIssueDialog()
                        onNavigateToInputSettings()
                    }
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissTypeIssueDialog() }) { Text("取消") }
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val copyText = taskText?.takeIf { it.isNotBlank() } ?: "尚未开始任务，输入描述后点击发送"

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
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier =
                        Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = {
                                clipboardManager.setText(AnnotatedString(copyText))
                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                            }
                        )
                )
            } else {
                Text(
                    text = "尚未开始任务，输入描述后点击发送",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    modifier =
                        Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = {
                                clipboardManager.setText(AnnotatedString(copyText))
                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                            }
                        )
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

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
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        clipboardManager.setText(AnnotatedString(actionText))
                                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                .padding(7.dp),
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
                                .combinedClickable(
                                    onClick = { onImageClick(path) },
                                    onLongClick = {
                                        scope.launch {
                                            val uri =
                                                saveImageToGallery(
                                                    context = context,
                                                    file = File(path)
                                                )
                                            val msg = if (uri != null) "已保存到相册" else "保存失败"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ),
                        contentScale = ContentScale.Fit
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = {
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                            }
                        )
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    modifier =
                        Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = {
                                clipboardManager.setText(AnnotatedString(message))
                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                            }
                        )
                )
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
    seed: Long,
    onPick: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val tasks =
        remember {
            listOf(
                "打开微信，查看未读消息",
                "打开微信，搜索并打开一个联系人",
                "打开微信，进入群聊并查看最新消息",
                "打开微信，给某个联系人发送“我晚点回复你”",
                "打开微信，查看朋友圈",
                "打开支付宝，查看账单",
                "打开支付宝，查看余额",
                "打开支付宝，搜索“地铁”并打开乘车码",
                "打开支付宝，搜索“信用”并查看信用分",
                "打开支付宝，打开生活缴费并查看项目",
                "打开淘宝，搜索“蓝牙耳机”，筛选销量排序",
                "打开淘宝，搜索“手机壳”，筛选包邮",
                "打开淘宝，查看订单列表",
                "打开淘宝，查看购物车并勾选一件商品",
                "打开京东，搜索“充电宝”，筛选自营",
                "打开京东，查看订单列表",
                "打开拼多多，搜索“纸巾”，按价格排序",
                "打开美团，搜索“奶茶”，选择附近门店",
                "打开美团，搜索“火锅”，看看评分最高的",
                "打开饿了么，搜索“咖啡”，看看优惠",
                "打开小红书，搜索“包子”，看热门笔记",
                "打开小红书，搜索“健身入门”，看收藏最多的",
                "打开抖音，搜索“学习方法”，看热门视频",
                "打开抖音，搜索“手机摄影”，看教程",
                "打开B站，搜索“Kotlin Compose”，看入门视频",
                "打开B站，搜索“英语听力”，找一个播放列表",
                "打开微博，查看热搜榜",
                "打开知乎，搜索“时间管理”，看看高赞回答",
                "打开今日头条，查看推荐新闻",
                "打开网易云音乐，搜索“轻音乐”，播放一首",
                "打开QQ音乐，搜索“周杰伦”，播放热门歌曲",
                "打开哔哩哔哩音乐区，看看热门",
                "打开高德地图，搜索“附近充电宝”",
                "打开高德地图，搜索“地铁站”，规划路线",
                "打开百度地图，搜索“附近停车场”",
                "打开滴滴出行，输入目的地查看价格",
                "打开12306，查询明天从北京到上海的车次",
                "打开携程，搜索“周末酒店”，按价格排序",
                "打开飞猪，搜索“机票”，查看最近日期价格",
                "打开Keep，打开一次训练计划",
                "打开微信读书，继续阅读上次的书",
                "打开得到，查看今日推荐",
                "打开日历，查看本周日程",
                "打开闹钟，新增一个明天8点的闹钟",
                "打开设置，连接 Wi‑Fi",
                "打开设置，查看电池使用情况",
                "打开设置，打开蓝牙并搜索设备",
                "打开设置，调整亮度到50%",
                "打开设置，查看存储空间",
                "打开相机，切换到人像模式",
                "打开相册，查看最近拍摄的照片",
                "打开相册，选择一张照片分享给微信",
                "打开文件管理，查看下载目录",
                "打开浏览器，搜索“今天天气”",
                "打开浏览器，搜索“附近好吃的”",
                "打开浏览器，打开一个收藏夹",
                "打开百度，搜索“包子”",
                "打开夸克浏览器，搜索“PDF转图片”",
                "打开WPS，打开最近文档",
                "打开WPS，新建一个空白文档",
                "打开邮箱，查看未读邮件",
                "打开邮箱，搜索“验证码”邮件",
                "打开微信支付分，查看免押服务",
                "打开招商银行，查看余额（不进行转账）",
                "打开支付宝，查看花呗额度",
                "打开大众点评，搜索“烧烤”并按距离排序",
                "打开大众点评，查看附近咖啡店评分",
                "打开豆瓣，搜索“电影推荐”",
                "打开豆瓣，查看“想看”的电影列表",
                "打开携程，搜索“景点门票”",
                "打开同程旅行，搜索“火车票”",
                "打开携程，查看行程订单",
                "打开滴答清单，新增“买包子”任务",
                "打开备忘录，记录“今天要完成的三件事”",
                "打开记账本，新增一笔支出",
                "打开计算器，计算 12345 × 678",
                "打开翻译，翻译“我想吃包子”到英语",
                "打开翻译，翻译一段中文到日语",
                "打开微信，打开一个小程序",
                "打开支付宝，打开一个小程序",
                "打开QQ，查看未读消息",
                "打开QQ，进入一个群聊并查看消息",
                "打开短信，查看最近短信",
                "打开电话，查看通话记录",
                "打开联系人，搜索一个联系人",
                "打开网易新闻，查看头条",
                "打开腾讯新闻，查看热点",
                "打开天气，查看未来7天预报",
                "打开股票软件，查看自选股（不交易）",
                "打开理财软件，查看收益（不交易）",
                "打开浏览器，搜索“番茄炒蛋做法”",
                "打开浏览器，搜索“低脂晚餐”",
                "打开美团，搜索“药店”，查看营业时间",
                "打开饿了么，搜索“夜宵”，看推荐",
                "打开地图，搜索“附近超市”",
                "打开地图，搜索“附近ATM”",
                "打开B站，搜索“番剧推荐”",
                "打开抖音，搜索“美食探店”",
                "打开小红书，搜索“穿搭”",
                "打开知乎，查看“热榜”",
                "打开微博，查看关注列表",
                "打开设置，查看无障碍服务状态",
                "打开设置，查看通知管理",
                "打开设置，查看应用权限管理"
            )
        }

    val suggestions =
        remember(seed) {
            val random = kotlin.random.Random(seed)
            val count = random.nextInt(from = 2, until = 7) // 2~6
            tasks.shuffled(random).take(count)
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "为你推荐",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            TextButton(onClick = onRefresh, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("换一批", style = MaterialTheme.typography.labelMedium)
            }
        }

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onDismiss() }
            )

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.82f)
                        .align(Alignment.Center),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .pointerInput(message.imagePath) {
                                        detectTapGestures(
                                            onDoubleTap = { onDismiss() },
                                            onLongPress = {
                                                scope.launch {
                                                    val uri =
                                                        saveImageToGallery(
                                                            context = context,
                                                            file = File(message.imagePath)
                                                        )
                                                    val msg = if (uri != null) "已保存到相册" else "保存失败"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // 顶部信息栏
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 28.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 页码指示器
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} / ${imageMessages.size}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // 关闭按钮
                FilledTonalIconButton(
                    onClick = onDismiss,
                    colors =
                        IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 底部动作信息
            val currentMessage = imageMessages[pagerState.currentPage]
            if (currentMessage.action != null) {
                Card(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 28.dp, start = 16.dp, end = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "动作执行",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            tonalElevation = 0.dp,
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = currentMessage.action,
                                style =
                                    MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun saveImageToGallery(context: android.content.Context, file: File): Uri? {
    return withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null

        val mimeType = when (file.extension.lowercase(Locale.getDefault())) {
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "image/png"
        }

        val resolver = context.contentResolver
        val values =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "baozi_${System.currentTimeMillis()}.${file.extension.ifBlank { "png" }}")
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + File.separator + "Baozi"
                    )
                }
            }

        var uri: Uri? = null
        try {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: return@withContext null
            return@withContext uri
        } catch (_: Exception) {
            if (uri != null) {
                try {
                    resolver.delete(uri, null, null)
                } catch (_: Exception) {
                }
            }
            return@withContext null
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
