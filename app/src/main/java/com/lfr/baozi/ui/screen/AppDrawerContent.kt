package com.lfr.baozi.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lfr.baozi.data.database.Conversation
import com.lfr.baozi.data.database.ConversationStatus
import com.lfr.baozi.R
import coil.compose.AsyncImage

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
    var contextMenuConversation by remember { mutableStateOf<Conversation?>(null) }
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }
    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }

    ModalDrawerSheet(modifier = Modifier.width(320.dp).fillMaxHeight()) {
        val blurAmount = if (contextMenuConversation != null) 14.dp else 0.dp
        Column(modifier = Modifier.fillMaxHeight().blur(blurAmount)) {
            DrawerTopBar(
                query = query,
                onQueryChange = { query = it },
                onNewTask = onNewTask
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
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
                    if (query.isBlank()) conversations
                    else
                        conversations.filter {
                            it.title.contains(query, ignoreCase = true)
                        }
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
                        onLongPress = { contextMenuConversation = conversation }
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
    }

    if (contextMenuConversation != null) {
        TaskContextMenuDialog(
            conversation = contextMenuConversation!!,
            onDismiss = { contextMenuConversation = null },
            onPinToggle = { pinned ->
                onSetPinned(contextMenuConversation!!.id, pinned)
                contextMenuConversation = null
            },
            onRename = {
                renameTarget = contextMenuConversation
                renameInput = contextMenuConversation!!.title
                showRenameDialog = true
                contextMenuConversation = null
            },
            onDelete = {
                deleteTarget = contextMenuConversation
                showDeleteDialog = true
                contextMenuConversation = null
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deleteTarget = null
            },
            title = { Text("删除任务") },
            text = { Text("确定要删除这个任务吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget?.let { onDeleteTask(it.id) }
                        showDeleteDialog = false
                        deleteTarget = null
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deleteTarget = null
                    }
                ) { Text("取消") }
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                renameTarget = null
            },
            title = { Text("重命名") },
            text = {
                TextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    colors =
                        TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = renameTarget
                        val newTitle = renameInput.trim()
                        if (target != null && newTitle.isNotBlank()) {
                            onRenameTask(target.id, newTitle)
                        }
                        showRenameDialog = false
                        renameTarget = null
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                        renameTarget = null
                    }
                ) { Text("取消") }
            }
        )
    }
}

@Composable
private fun DrawerTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onNewTask: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).heightIn(min = 40.dp),
            singleLine = true,
            placeholder = { Text("搜索…", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        IconButton(onClick = onNewTask, modifier = Modifier.size(40.dp).testTag("drawer_new_task")) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
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
    onLongPress: () -> Unit
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

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongPress)
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
private fun TaskContextMenuDialog(
    conversation: Conversation,
    onDismiss: () -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.74f),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 0.dp,
                shadowElevation = 14.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    MenuRow(
                        icon = Icons.Default.PushPin,
                        title = if (conversation.isPinned) "取消置顶" else "置顶",
                        titleColor = MaterialTheme.colorScheme.onSurface,
                        enabled = true,
                        onClick = { onPinToggle(!conversation.isPinned) }
                    )
                    HorizontalDivider()
                    MenuRow(
                        icon = Icons.Default.Edit,
                        title = "编辑对话名称",
                        titleColor = MaterialTheme.colorScheme.onSurface,
                        enabled = true,
                        onClick = onRename
                    )
                    HorizontalDivider()
                    MenuRow(
                        icon = Icons.Default.Share,
                        title = "分享对话",
                        titleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        enabled = false,
                        onClick = {}
                    )
                    HorizontalDivider()
                    MenuRow(
                        icon = Icons.Default.DeleteOutline,
                        title = "从对话列表删除",
                        titleColor = MaterialTheme.colorScheme.error,
                        enabled = true,
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    titleColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val rowModifier =
        if (enabled) Modifier.fillMaxWidth().clickable(onClick = onClick)
        else Modifier.fillMaxWidth()
    Row(
        modifier = rowModifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = titleColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = titleColor)
    }
}
