package com.lfr.baozi.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lfr.baozi.R
import com.lfr.baozi.ui.viewmodel.AccountViewModel

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                viewModel.updateAvatarUri(uri.toString())
            }
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "欢迎使用包子",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "登录后即可开始新任务",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier =
                Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .clickable { pickAvatarLauncher.launch(arrayOf("image/*")) }
        ) {
            AsyncImage(
                model = uiState.avatarUri.ifBlank { R.drawable.avator },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        TextButton(onClick = { pickAvatarLauncher.launch(arrayOf("image/*")) }) {
            Text("更换头像")
        }

        OutlinedTextField(
            value = uiState.nickname,
            onValueChange = viewModel::updateNickname,
            label = { Text("昵称") },
            modifier = Modifier.fillMaxWidth().testTag("login_nickname"),
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.inviteCode,
            onValueChange = viewModel::updateInviteCode,
            label = { Text("邀请码") },
            placeholder = { Text("请输入邀请码") },
            modifier = Modifier.fillMaxWidth().testTag("login_invite"),
            singleLine = true
        )

        Button(
            onClick = viewModel::login,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("login_button")
        ) {
            Text("登录")
        }

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

        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "提示：邀请码模块后续接入完整校验。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
