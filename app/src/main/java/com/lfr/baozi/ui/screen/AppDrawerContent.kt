package com.lfr.baozi.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfr.baozi.data.database.Conversation
import com.lfr.baozi.data.database.ConversationStatus
import com.lfr.baozi.R
import coil.compose.AsyncImage

@Immutable
private data class DrawerMenuState(
    val conversation: Conversation,
    val anchorBounds: Rect
)

@Composable
fun AppDrawerContent(
    conversations: List<Conversation>,
    currentConversationId: String?,
    userName: String,
    avatarUri: String,
    onNavigateProfileSettings: () -> Unit,
    onNavigateSettings: () -> Unit,
    onTaskSelected: (String, String) -> Unit,
    onNewTask: () -> Unit,
    onRenameTask: (String, String) -> Unit,
    onSetPinned: (String, Boolean) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var menuState by remember { mutableStateOf<DrawerMenuState?>(null) }
    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }
    var renameInput by remember { mutableStateOf("") }

    ModalDrawerSheet(modifier = Modifier.width(320.dp).fillMaxHeight()) {
        val overlayActive = menuState != null || renameTarget != null || deleteTarget != null
        val blurAmount = if (overlayActive) 14.dp else 0.dp

        BoxWithConstraints(modifier = Modifier.fillMaxHeight().statusBarsPadding()) {
            val maxWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val maxHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxHeight().blur(blurAmount)) {
                    DrawerTopBar(
                        query = query,
                        onQueryChange = { query = it },
                        onNewTask = onNewTask
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "历史记录",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val filtered =
                        remember(conversations, query) {
                            val base =
                                conversations.filterNot { it.isPlaceholderDraft() }
                            if (query.isBlank()) base
                            else
                                base.filter { it.title.contains(query, ignoreCase = true) }
                        }

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(filtered, key = { _, c -> c.id }) { _, conversation ->
                            DrawerConversationItem(
                                conversation = conversation,
                                isSelected = conversation.id == currentConversationId,
                                onClick = { onTaskSelected(conversation.id, conversation.title) },
                                onLongPress = { bounds -> menuState = DrawerMenuState(conversation, bounds) }
                            )
                        }
                    }

                    DrawerBottomBar(
                        userName = userName,
                        avatarUri = avatarUri,
                        onProfile = onNavigateProfileSettings,
                        onSettings = {
                            onNavigateSettings()
                        }
                    )
                }

                if (menuState != null) {
                    val state = menuState!!
                    DrawerContextMenuOverlay(
                        maxWidthPx = maxWidthPx,
                        maxHeightPx = maxHeightPx,
                        state = state,
                        onDismiss = { menuState = null },
                        onPinToggle = { pinned ->
                            onSetPinned(state.conversation.id, pinned)
                            menuState = null
                        },
                        onRename = {
                            renameTarget = state.conversation
                            renameInput = state.conversation.title
                            menuState = null
                        },
                        onDelete = {
                            deleteTarget = state.conversation
                            menuState = null
                        }
                    )
                }

                renameTarget?.let { target ->
                    DrawerRenameDialog(
                        initial = renameInput,
                        onDismiss = {
                            renameTarget = null
                        },
                        onConfirm = { newTitle ->
                            onRenameTask(target.id, newTitle)
                            renameTarget = null
                        }
                    )
                }

                deleteTarget?.let { target ->
                    DrawerDeleteDialog(
                        title = target.title,
                        onDismiss = { deleteTarget = null },
                        onConfirm = {
                            onDeleteTask(target.id)
                            deleteTarget = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onNewTask: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.weight(1f).height(40.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF2F3F5),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isBlank()) {
                        Text(
                            text = "搜索…",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle =
                            MaterialTheme.typography.bodySmall.copy(
                                fontSize = 14.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                        cursorBrush = SolidColor(Color(0xFF2D6BFF)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        IconButton(onClick = onNewTask, modifier = Modifier.size(40.dp).testTag("drawer_new_task")) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF4CAF50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "新建任务", tint = Color.White)
            }
        }
    }
}

@Composable
private fun DrawerBottomBar(
    userName: String,
    avatarUri: String,
    onProfile: () -> Unit,
    onSettings: () -> Unit
) {
    HorizontalDivider()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            modifier =
                Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onProfile),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = avatarUri.ifBlank { R.drawable.avator },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(28.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        IconButton(
            onClick = onSettings,
            modifier = Modifier.size(40.dp).testTag("drawer_settings")
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1976D2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun DrawerConversationItem(
    conversation: Conversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: (Rect) -> Unit
) {
    val status = remember(conversation.status) { ConversationStatus.fromRaw(conversation.status) }
    val icon =
        when (status) {
            ConversationStatus.RUNNING -> Icons.Default.PlayCircle
            ConversationStatus.COMPLETED -> Icons.Default.CheckCircle
            ConversationStatus.ABORTED -> Icons.Default.Cancel
            ConversationStatus.ENDED -> Icons.Default.StopCircle
            ConversationStatus.IDLE -> Icons.Default.RadioButtonUnchecked
        }
    val iconBg =
        when (status) {
            ConversationStatus.RUNNING -> Color(0xFF1976D2)
            ConversationStatus.COMPLETED -> Color(0xFF4CAF50)
            ConversationStatus.ABORTED -> Color(0xFFFFB300)
            ConversationStatus.ENDED -> Color(0xFFE53935)
            ConversationStatus.IDLE -> Color(0xFF90A4AE)
        }

    var bounds by remember { mutableStateOf<Rect?>(null) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                )
                .onGloballyPositioned { bounds = it.boundsInRoot() }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { bounds?.let(onLongPress) }
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = conversation.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (conversation.isPinned) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = "置顶",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DrawerContextMenuOverlay(
    maxWidthPx: Float,
    maxHeightPx: Float,
    state: DrawerMenuState,
    onDismiss: () -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val consumeInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = 1.04f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "drawer_item_scale"
    )

    val menuWidthPx = with(density) { 236.dp.toPx() }
    val menuHeightPx = with(density) { 208.dp.toPx() }
    val paddingPx = with(density) { 12.dp.toPx() }
    val gapPx = with(density) { 10.dp.toPx() }

    val desiredX = state.anchorBounds.left + (state.anchorBounds.width / 2f) - (menuWidthPx / 2f)
    val xPx = desiredX.coerceIn(paddingPx, (maxWidthPx - menuWidthPx - paddingPx).coerceAtLeast(paddingPx))

    val placeAboveY = state.anchorBounds.top - menuHeightPx - gapPx
    val placeBelowY = state.anchorBounds.bottom + gapPx
    val yPx =
        if (placeAboveY >= paddingPx) placeAboveY
        else placeBelowY.coerceIn(paddingPx, (maxHeightPx - menuHeightPx - paddingPx).coerceAtLeast(paddingPx))

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
                    .clickable(onClick = onDismiss)
        )

        // Pressed item overlay (unblurred + slight scale)
        val status = ConversationStatus.fromRaw(state.conversation.status)
        val (statusIcon, statusIconBg) =
            when (status) {
                ConversationStatus.RUNNING -> Icons.Default.PlayCircle to Color(0xFF1976D2)
                ConversationStatus.COMPLETED -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
                ConversationStatus.ABORTED -> Icons.Default.Cancel to Color(0xFFFFB300)
                ConversationStatus.ENDED -> Icons.Default.StopCircle to Color(0xFFE53935)
                ConversationStatus.IDLE -> Icons.Default.RadioButtonUnchecked to Color(0xFF90A4AE)
            }
        Surface(
            modifier = Modifier
                .offset(with(density) { state.anchorBounds.left.toDp() }, with(density) { state.anchorBounds.top.toDp() })
                .width(with(density) { state.anchorBounds.width.toDp() })
                .height(with(density) { state.anchorBounds.height.toDp() })
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                )
                .clickable(indication = null, interactionSource = consumeInteraction) {},
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .clickable(indication = null, interactionSource = consumeInteraction) {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusIconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = state.conversation.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (state.conversation.isPinned) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Surface(
            modifier =
                Modifier
                    .offset(with(density) { xPx.toDp() }, with(density) { yPx.toDp() })
                    .width(236.dp)
                    .clickable(indication = null, interactionSource = consumeInteraction) {},
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 18.dp,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DrawerMenuRow(
                    icon = Icons.Default.PushPin,
                    title = if (state.conversation.isPinned) "取消置顶" else "置顶",
                    enabled = true,
                    titleColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { onPinToggle(!state.conversation.isPinned) }
                )
                HorizontalDivider()
                DrawerMenuRow(
                    icon = Icons.Default.Edit,
                    title = "编辑对话名称",
                    enabled = true,
                    titleColor = MaterialTheme.colorScheme.onSurface,
                    onClick = onRename
                )
                HorizontalDivider()
                DrawerMenuRow(
                    icon = Icons.Default.Share,
                    title = "分享对话",
                    enabled = false,
                    titleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    onClick = {}
                )
                HorizontalDivider()
                DrawerMenuRow(
                    icon = Icons.Default.DeleteOutline,
                    title = "从对话列表删除",
                    enabled = true,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun DrawerMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    enabled: Boolean,
    titleColor: Color,
    onClick: () -> Unit
) {
    val rowModifier =
        if (enabled) Modifier.fillMaxWidth().clickable(onClick = onClick)
        else Modifier.fillMaxWidth()
    Row(
        modifier = rowModifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = titleColor)
        Spacer(modifier = Modifier.weight(1f))
        Icon(imageVector = icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DrawerRenameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initial) { mutableStateOf(initial) }
    val consumeInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.18f))
                .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(0.78f)
                    .clickable(indication = null, interactionSource = consumeInteraction) {},
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 18.dp,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = "对话名称",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF2F3F5)
                ) {
                    TextField(
                        value = value,
                        onValueChange = { value = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = Color(0xFF1976D2), fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = { onConfirm(value.trim()) },
                        enabled = value.trim().isNotBlank()
                    ) {
                        Text("确定", color = Color(0xFF1976D2), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerDeleteDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val consumeInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.18f))
                .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(0.78f)
                    .clickable(indication = null, interactionSource = consumeInteraction) {},
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 18.dp,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = "从对话列表删除",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "确定要删除「$title」吗？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = Color(0xFF1976D2), fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = onConfirm) {
                        Text("删除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun Conversation.isPlaceholderDraft(): Boolean {
    if (isPinned) return false
    val isNeverUsed =
        ConversationStatus.fromRaw(status) == ConversationStatus.IDLE &&
            taskStartedAt == null &&
            taskEndedAt == null &&
            taskResultMessage.isNullOrBlank() &&
            createdAt == updatedAt
    if (!isNeverUsed) return false
    return title == "新任务" || title == "新对话"
}
