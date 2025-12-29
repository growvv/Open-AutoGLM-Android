package com.lfr.baozi.ui.viewmodel

import android.app.Application
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lfr.baozi.data.InputMode
import com.lfr.baozi.data.PreferencesRepository
import com.lfr.baozi.data.PreferencesRepository.Companion.DEFAULT_BASE_URL
import com.lfr.baozi.data.PreferencesRepository.Companion.DEFAULT_MAX_STEPS
import com.lfr.baozi.data.PreferencesRepository.Companion.DEFAULT_MODEL_NAME
import com.lfr.baozi.service.FloatingWindowService
import com.lfr.baozi.util.AccessibilityServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val customApiKey: String = "",
    val customBaseUrl: String = "",
    val modelName: String = DEFAULT_MODEL_NAME,
    val maxStepsInput: String = DEFAULT_MAX_STEPS.toString(),
    val isAccessibilityEnabled: Boolean = false,
    val isAccessibilityServiceRunning: Boolean = false,
    val floatingWindowEnabled: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val inputMode: InputMode = InputMode.SET_TEXT,
    val isImeEnabled: Boolean = false,
    val isImeSelected: Boolean = false,
    val imageCompressionEnabled: Boolean = false,
    val imageCompressionLevel: Int = 50,
    val isLoading: Boolean = false,
    val saveSuccess: Boolean? = null,
    val error: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val preferencesRepository = PreferencesRepository(application)
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
        checkAccessibilityService()
        checkOverlayPermission()
        checkImeStatus()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            preferencesRepository.customApiKey.collect { apiKey ->
                _uiState.value = _uiState.value.copy(customApiKey = apiKey)
            }
        }
        viewModelScope.launch {
            preferencesRepository.customBaseUrl.collect { baseUrl ->
                _uiState.value = _uiState.value.copy(customBaseUrl = baseUrl)
            }
        }
        viewModelScope.launch {
            preferencesRepository.modelName.collect { modelName ->
                _uiState.value = _uiState.value.copy(modelName = modelName ?: DEFAULT_MODEL_NAME)
            }
        }
        viewModelScope.launch {
            preferencesRepository.maxSteps.collect { maxSteps ->
                _uiState.value = _uiState.value.copy(maxStepsInput = maxSteps.toString())
            }
        }
        viewModelScope.launch {
            preferencesRepository.floatingWindowEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(floatingWindowEnabled = enabled)
                updateFloatingWindowService(enabled)
            }
        }
        viewModelScope.launch {
            preferencesRepository.inputMode.collect { mode ->
                _uiState.value = _uiState.value.copy(inputMode = mode)
            }
        }
        viewModelScope.launch {
            preferencesRepository.imageCompressionEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(imageCompressionEnabled = enabled)
            }
        }
        viewModelScope.launch {
            preferencesRepository.imageCompressionLevel.collect { level ->
                _uiState.value = _uiState.value.copy(imageCompressionLevel = level)
            }
        }
    }
    
    fun checkAccessibilityService() {
        val enabledInSettings = AccessibilityServiceHelper.isAccessibilityServiceEnabled(getApplication())
        val serviceRunning = AccessibilityServiceHelper.isServiceRunning()
        _uiState.value =
            _uiState.value.copy(
                isAccessibilityEnabled = enabledInSettings,
                isAccessibilityServiceRunning = serviceRunning
            )
    }
    
    fun checkOverlayPermission() {
        val hasPermission = FloatingWindowService.hasOverlayPermission(getApplication())
        _uiState.value = _uiState.value.copy(hasOverlayPermission = hasPermission)
    }

    fun checkImeStatus() {
        val context = getApplication<Application>()
        val imm = context.getSystemService(Application.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledMethods = imm.enabledInputMethodList
        val myPackageName = context.packageName
        
        val isEnabled = enabledMethods.any { it.packageName == myPackageName }
        val currentIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val isSelected = currentIme?.contains(myPackageName) == true
        
        _uiState.value = _uiState.value.copy(
            isImeEnabled = isEnabled,
            isImeSelected = isSelected
        )
    }
    
    fun setFloatingWindowEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.saveFloatingWindowEnabled(enabled)
            _uiState.value = _uiState.value.copy(floatingWindowEnabled = enabled)
            updateFloatingWindowService(enabled)
        }
    }

    fun setInputMode(mode: InputMode) {
        viewModelScope.launch {
            preferencesRepository.saveInputMode(mode)
            _uiState.value = _uiState.value.copy(inputMode = mode)
        }
    }

    fun setImageCompressionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.saveImageCompressionEnabled(enabled)
            _uiState.value = _uiState.value.copy(imageCompressionEnabled = enabled)
        }
    }

    fun setImageCompressionLevel(level: Int) {
        viewModelScope.launch {
            preferencesRepository.saveImageCompressionLevel(level)
            _uiState.value = _uiState.value.copy(imageCompressionLevel = level)
        }
    }
    
    private fun updateFloatingWindowService(enabled: Boolean) {
        val context = getApplication<Application>()
        if (enabled && FloatingWindowService.hasOverlayPermission(context)) {
            FloatingWindowService.startService(context)
        } else {
            FloatingWindowService.stopService(context)
        }
    }
    
    fun updateApiKey(apiKey: String) {
        _uiState.value = _uiState.value.copy(customApiKey = apiKey)
    }
    
    fun updateBaseUrl(baseUrl: String) {
        _uiState.value = _uiState.value.copy(customBaseUrl = baseUrl)
    }
    
    fun updateModelName(modelName: String) {
        _uiState.value = _uiState.value.copy(modelName = modelName)
    }

    fun updateMaxStepsInput(maxStepsInput: String) {
        _uiState.value = _uiState.value.copy(maxStepsInput = maxStepsInput)
    }
    
    fun saveSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, saveSuccess = false)
            try {
                val parsedMaxSteps = _uiState.value.maxStepsInput.trim().toIntOrNull() ?: DEFAULT_MAX_STEPS
                val clampedMaxSteps = parsedMaxSteps.coerceIn(1, 500)
                preferencesRepository.saveModelName(_uiState.value.modelName)
                preferencesRepository.saveMaxSteps(clampedMaxSteps)
                _uiState.value = _uiState.value.copy(isLoading = false, saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "保存失败: ${e.message}")
            }
        }
    }

    fun saveBackendOverrides() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, saveSuccess = false)
            try {
                preferencesRepository.saveCustomApiKey(_uiState.value.customApiKey)
                preferencesRepository.saveCustomBaseUrl(_uiState.value.customBaseUrl)
                _uiState.value = _uiState.value.copy(isLoading = false, saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "保存失败: ${e.message}")
            }
        }
    }

    fun resetBackendOverrides() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, saveSuccess = false)
            try {
                preferencesRepository.clearCustomBackendOverrides()
                _uiState.value =
                    _uiState.value.copy(
                        customApiKey = "",
                        customBaseUrl = "",
                        isLoading = false,
                        saveSuccess = true
                    )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "恢复失败: ${e.message}")
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, saveSuccess = false)
    }
}
