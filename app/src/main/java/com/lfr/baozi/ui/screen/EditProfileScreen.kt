package com.lfr.baozi.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lfr.baozi.R
import com.lfr.baozi.ui.viewmodel.AccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var nicknameDraft by rememberSaveable { mutableStateOf("") }
    var avatarDraft by rememberSaveable { mutableStateOf("") }
    val inviteCode = uiState.inviteCode

    LaunchedEffect(uiState.nickname, uiState.avatarUri) {
        if (nicknameDraft.isBlank()) nicknameDraft = uiState.nickname
        if (avatarDraft.isBlank()) avatarDraft = uiState.avatarUri
    }

    val pickAvatarLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }
                avatarDraft = uri.toString()
            }
        }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("个人资料", style = MaterialTheme.typography.titleSmall) },
                navigationIcon = { TextButton(onClick = onCancel) { Text("取消") } },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveProfile(
                                nickname = nicknameDraft,
                                avatarUri = avatarDraft,
                                onSuccess = onDone
                            )
                        },
                        enabled = !uiState.isSaving
                    ) { Text("完成") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .clickable { pickAvatarLauncher.launch(arrayOf("image/*")) }
                ) {
                    AsyncImage(
                        model = avatarDraft.ifBlank { R.drawable.avator },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(text = "昵称", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = nicknameDraft,
                onValueChange = { nicknameDraft = it },
                modifier = Modifier.fillMaxWidth().testTag("edit_profile_nickname"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
            )

            Text(text = "邀请码", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = inviteCode.ifBlank { "未填写" },
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().testTag("edit_profile_invite"),
                enabled = false,
                readOnly = true,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledBorderColor = Color.Transparent,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            )

            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = error, color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = viewModel::clearError) { Text("关闭") }
                    }
                }
            }
        }
    }
}
