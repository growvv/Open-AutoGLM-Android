package com.example.open_autoglm_android.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.open_autoglm_android.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    ChatScreen(
        modifier = modifier.fillMaxSize(),
        viewModel = viewModel,
        onNavigateToSettings = onNavigateToSettings
    )
}
