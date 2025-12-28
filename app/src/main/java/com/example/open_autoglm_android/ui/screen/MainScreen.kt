package com.example.open_autoglm_android.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.open_autoglm_android.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit = {}
) {
    val viewModel: ChatViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationDrawer(
                conversations = uiState.conversations,
                currentConversationId = uiState.currentConversationId,
                onConversationClick = { id, title->
                    viewModel.switchConversation(id, title)
                    scope.launch { drawerState.close() }
                },
                onNewConversation = {
                    viewModel.createNewConversation()
                    scope.launch { drawerState.close() }
                },
                onDeleteConversation = { viewModel.deleteConversation(it) }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "任务列表"
                        )
                    }
                },
                title = {
                    Text(
                        uiState.currentConversationTitle ?: "AutoGLM Android",
                        style = TextStyle(fontSize = 18.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
            ChatScreen(viewModel = viewModel, onNavigateToSettings = onNavigateToSettings)
        }
    }
}
