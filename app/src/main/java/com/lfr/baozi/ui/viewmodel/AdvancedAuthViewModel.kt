package com.example.open_autoglm_android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.open_autoglm_android.util.AuthHelper
import rikka.shizuku.Shizuku
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdvancedAuthUiState(
    val hasWriteSecureSettings: Boolean = false,
    val shizukuAvailable: Boolean = false,
    val shizukuAuthorized: Boolean = false,
    val rootAvailable: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null,
    val messageType: MessageType = MessageType.INFO
) {
    enum class MessageType {
        INFO, SUCCESS, ERROR
    }
}

class AdvancedAuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(AdvancedAuthUiState())
    val uiState: StateFlow<AdvancedAuthUiState> = _uiState.asStateFlow()
    
    private val shizukuRequestCode = 1000
    
    init {
        checkAllPermissions()
        setupShizukuListener()
    }
    
    fun checkAllPermissions() {
        val context = getApplication<Application>()
        val hasWriteSecureSettings = AuthHelper.hasWriteSecureSettingsPermission(context)
        val shizukuAvailable = AuthHelper.isShizukuAvailable()
        val shizukuAuthorized = AuthHelper.isShizukuAuthorized()
        
        _uiState.value = _uiState.value.copy(
            hasWriteSecureSettings = hasWriteSecureSettings,
            shizukuAvailable = shizukuAvailable,
            shizukuAuthorized = shizukuAuthorized
        )
        
        // 检查 Root 权限
        viewModelScope.launch {
            val rootAvailable = AuthHelper.checkRootPermission()
            _uiState.value = _uiState.value.copy(rootAvailable = rootAvailable)
        }
    }
    
    private fun setupShizukuListener() {
        if (AuthHelper.isShizukuAvailable()) {
            Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == shizukuRequestCode) {
                    val context = getApplication<Application>()
                    if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        // Shizuku 权限已授予，尝试授予 WRITE_SECURE_SETTINGS
                        val success = AuthHelper.grantWriteSecureSettingsViaShizuku(context)
                        if (success) {
                            checkAllPermissions()
                            showMessage("Shizuku 授权成功，已自动授予 WRITE_SECURE_SETTINGS 权限", AdvancedAuthUiState.MessageType.SUCCESS)
                        } else {
                            showMessage("Shizuku 授权成功，但授予 WRITE_SECURE_SETTINGS 权限失败，请手动在 Shizuku 中授权", AdvancedAuthUiState.MessageType.ERROR)
                        }
                    } else {
                        showMessage("Shizuku 授权被拒绝", AdvancedAuthUiState.MessageType.ERROR)
                    }
                }
            }
        }
    }
    
    fun requestShizukuPermission() {
        val context = getApplication<Application>()
        if (!AuthHelper.isShizukuAvailable()) {
            // 引导用户安装 Shizuku
            AuthHelper.openShizukuApp(context)
            showMessage("请先安装并启动 Shizuku", AdvancedAuthUiState.MessageType.INFO)
            return
        }
        
        if (AuthHelper.isShizukuAuthorized()) {
            // 已经授权，直接尝试授予 WRITE_SECURE_SETTINGS
            val success = AuthHelper.grantWriteSecureSettingsViaShizuku(context)
            if (success) {
                checkAllPermissions()
                showMessage("已通过 Shizuku 授予 WRITE_SECURE_SETTINGS 权限", AdvancedAuthUiState.MessageType.SUCCESS)
            } else {
                showMessage("授权失败，请在 Shizuku 应用中手动授权", AdvancedAuthUiState.MessageType.ERROR)
            }
        } else {
            // 请求 Shizuku 权限
            AuthHelper.requestShizukuPermission(context, shizukuRequestCode)
            showMessage("请在 Shizuku 应用中授权", AdvancedAuthUiState.MessageType.INFO)
        }
    }
    
    fun requestRootPermission() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val context = getApplication<Application>()
            val success = AuthHelper.grantWriteSecureSettingsViaRoot(context)
            if (success) {
                checkAllPermissions()
                showMessage("Root 授权成功，已授予 WRITE_SECURE_SETTINGS 权限", AdvancedAuthUiState.MessageType.SUCCESS)
            } else {
                showMessage("Root 授权失败，请确保已授予 Root 权限", AdvancedAuthUiState.MessageType.ERROR)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    fun copyAdbCommand() {
        val context = getApplication<Application>()
        val command = AuthHelper.getAdbGrantCommand(context)
        AuthHelper.copyToClipboard(context, command, "ADB 授权命令")
        showMessage("已复制 ADB 命令到剪贴板", AdvancedAuthUiState.MessageType.SUCCESS)
    }
    
    fun openShizukuApp() {
        val context = getApplication<Application>()
        AuthHelper.openShizukuApp(context)
    }
    
    private fun showMessage(message: String, type: AdvancedAuthUiState.MessageType) {
        _uiState.value = _uiState.value.copy(message = message, messageType = type)
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
