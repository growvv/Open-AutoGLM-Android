package com.lfr.baozi.ui.screen

import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lfr.baozi.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BackendSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "自定义服务端",
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
                item("server_title") {
                    Text(
                        text = "服务端",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                item("card") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(androidx.compose.ui.graphics.Color(0xFF2D6BFF)),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "自定义服务端",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "填写后将覆盖内置默认设置",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = uiState.customBaseUrl,
                                onValueChange = { viewModel.updateBaseUrl(it) },
                                label = { Text("Base URL") },
                                placeholder = { Text("例如：https://example.com/v1") },
                                modifier = Modifier.fillMaxWidth().testTag("settings_custom_baseUrl"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors =
                                    OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                                    )
                            )

                            OutlinedTextField(
                                value = uiState.modelName,
                                onValueChange = { viewModel.updateModelName(it) },
                                label = { Text("Model Name") },
                                placeholder = { Text("autoglm-phone-9b") },
                                modifier = Modifier.fillMaxWidth().testTag("settings_custom_modelName"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors =
                                    OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                                    )
                            )

                            OutlinedTextField(
                                value = uiState.customApiKey,
                                onValueChange = { viewModel.updateApiKey(it) },
                                label = { Text("API Key") },
                                placeholder = { Text("留空则不发送 Authorization") },
                                modifier = Modifier.fillMaxWidth().testTag("settings_custom_apiKey"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors =
                                    OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                                    )
                            )
                        }
                    }
                }

                item("save") {
                    val primaryBlue = androidx.compose.ui.graphics.Color(0xFF2D6BFF)
                    Button(
                        onClick = { viewModel.saveBackendOverrides() },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_backend_save"),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(999.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = primaryBlue,
                                contentColor = androidx.compose.ui.graphics.Color.White,
                                disabledContainerColor = primaryBlue.copy(alpha = 0.45f),
                                disabledContentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                            )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("保存")
                        }
                    }
                }

                item("reset") {
                    val primaryBlue = androidx.compose.ui.graphics.Color(0xFF2D6BFF)
                    Button(
                        onClick = { viewModel.resetBackendOverrides() },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_backend_reset"),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(999.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = primaryBlue,
                                contentColor = androidx.compose.ui.graphics.Color.White,
                                disabledContainerColor = primaryBlue.copy(alpha = 0.45f),
                                disabledContentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                            )
                    ) {
                        Text("恢复默认")
                    }
                }

                item("result") {
                    uiState.saveSuccess?.let {
                        if (it) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                            ) {
                                Text(text = "设置已保存", modifier = Modifier.padding(12.dp))
                            }
                        }
                    }

                    uiState.error?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = error)
                                Button(onClick = { viewModel.clearError() }) { Text("关闭") }
                            }
                        }
                    }
                }
            }
        }
    }
}
