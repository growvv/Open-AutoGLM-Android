package com.example.open_autoglm_android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.open_autoglm_android.data.database.AppDatabase
import com.example.open_autoglm_android.data.database.ModelConfig
import com.example.open_autoglm_android.data.database.ModelConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 模型配置ViewModel，管理ModelConfigScreen的状态和逻辑
 */
class ModelConfigViewModel(application: Application) : AndroidViewModel(application) {
    // 数据库和仓库初始化
    private val database = AppDatabase.getDatabase(application)
    private val modelConfigRepository = ModelConfigRepository(database.modelConfigDao())

    // UI状态数据类
    data class ModelConfigUiState(
        val modelConfigs: List<ModelConfig> = emptyList(),
        val selectedModel: ModelConfig? = null,
        val isLoading: Boolean = false
    )

    // UI状态的MutableStateFlow
    private val _uiState = MutableStateFlow(ModelConfigUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // 加载所有模型配置
        loadModelConfigs()
    }

    /**
     * 加载所有模型配置
     */
    private fun loadModelConfigs() {
        viewModelScope.launch {
            // 监听所有模型配置的变化
            modelConfigRepository.allModelConfigs.collect {
                _uiState.value = _uiState.value.copy(modelConfigs = it)
            }
        }

        // 监听当前选中模型的变化
        viewModelScope.launch {
            modelConfigRepository.selectedModelConfig.collect {
                _uiState.value = _uiState.value.copy(selectedModel = it)
            }
        }
    }

    /**
     * 插入新的模型配置
     */
    fun insertModelConfig(modelConfig: ModelConfig) {
        viewModelScope.launch {
            modelConfigRepository.insertModelConfig(modelConfig)
        }
    }

    /**
     * 更新模型配置
     */
    fun updateModelConfig(modelConfig: ModelConfig) {
        viewModelScope.launch {
            modelConfigRepository.updateModelConfig(modelConfig)
        }
    }

    /**
     * 删除模型配置
     */
    fun deleteModelConfig(modelConfig: ModelConfig) {
        viewModelScope.launch {
            modelConfigRepository.deleteModelConfig(modelConfig)
        }
    }

    /**
     * 选择指定ID的模型
     */
    fun selectModel(id: Long) {
        viewModelScope.launch {
            modelConfigRepository.selectModelById(id)
        }
    }

    /**
     * 获取当前选中的模型配置
     */
    fun getSelectedModel(): ModelConfig? {
        return _uiState.value.selectedModel
    }
}