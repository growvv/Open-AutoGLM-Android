package com.example.open_autoglm_android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.open_autoglm_android.data.database.Conversation

@Composable
fun AppDrawerContent(
    conversations: List<Conversation>,
    currentConversationId: String?,
    onNavigateSettings: () -> Unit,
    onNewTask: () -> Unit,
    onTaskSelected: (String, String) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    ModalDrawerSheet(modifier = Modifier.width(320.dp).fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxHeight()) {
            DrawerTopBar(
                query = query,
                onQueryChange = { query = it },
                onNewTask = {
                    onNewTask()
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "历史记录",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val sorted =
                remember(conversations) { conversations.sortedByDescending { it.updatedAt } }
            val filtered =
                remember(sorted, query) {
                    if (query.isBlank()) sorted
                    else
                        sorted.filter {
                            it.title.contains(query, ignoreCase = true)
                        }
                }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(filtered, key = { _, c -> c.id }) { _, conversation ->
                    ConversationItem(
                        conversation = conversation,
                        isSelected = conversation.id == currentConversationId,
                        onClick = {
                            onTaskSelected(conversation.id, conversation.title)
                        },
                        onDelete = { onDeleteTask(conversation.id) }
                    )
                }
            }

            DrawerBottomBar(
                userName = "包子",
                userSubtitle = "默认用户",
                onSettings = {
                    onNavigateSettings()
                }
            )
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            singleLine = true,
            placeholder = { Text("搜索…", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            textStyle = MaterialTheme.typography.bodyMedium,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        IconButton(
            onClick = onNewTask,
            modifier = Modifier.size(44.dp).testTag("drawer_new_task")
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary),
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
    userSubtitle: String,
    onSettings: () -> Unit
) {
    HorizontalDivider()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = userSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
