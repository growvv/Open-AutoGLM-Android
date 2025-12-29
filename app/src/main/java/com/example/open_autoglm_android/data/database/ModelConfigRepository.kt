package com.example.open_autoglm_android.data.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * 模型配置仓库类，管理模型配置的数据库操作
 */
class ModelConfigRepository(private val modelConfigDao: ModelConfigDao) {
    // 获取所有模型配置的Flow
    val allModelConfigs: Flow<List<ModelConfig>> = modelConfigDao.getAllModelConfigs()

    // 获取当前选中的模型配置的Flow
    val selectedModelConfig: Flow<ModelConfig?> = modelConfigDao.getSelectedModelConfig()

    // 根据ID获取模型配置
    suspend fun getModelConfigById(id: Long): ModelConfig? {
        return modelConfigDao.getModelConfigById(id)
    }

    // 插入模型配置
    suspend fun insertModelConfig(modelConfig: ModelConfig): Long {
        // 如果这是第一个模型，自动设置为选中
        val allConfigs = allModelConfigs.first()
        if (allConfigs.isEmpty()) {
            val configToInsert = modelConfig.copy(isSelected = true)
            return modelConfigDao.insertModelConfig(configToInsert)
        }
        return modelConfigDao.insertModelConfig(modelConfig)
    }

    // 更新模型配置
    suspend fun updateModelConfig(modelConfig: ModelConfig) {
        modelConfigDao.updateModelConfig(modelConfig)
    }

    // 删除模型配置
    suspend fun deleteModelConfig(modelConfig: ModelConfig) {
        // 如果要删除的是当前选中的模型，需要重新选择一个
        val currentSelected = selectedModelConfig.first()
        if (currentSelected?.id == modelConfig.id) {
            val allConfigs = allModelConfigs.first()
            val remainingConfigs = allConfigs.filter { it.id != modelConfig.id }
            if (remainingConfigs.isNotEmpty()) {
                // 选择剩余的第一个模型
                selectModelById(remainingConfigs.first().id)
            }
        }
        modelConfigDao.deleteModelConfig(modelConfig)
    }

    // 选择模型
    suspend fun selectModelById(id: Long) {
        // 先取消所有模型的选中状态
        modelConfigDao.unselectAllModels()
        // 然后选中指定ID的模型
        modelConfigDao.selectModelById(id)
    }

    // 同步获取当前选中的模型配置（用于非协程环境）
    fun getSelectedModelConfigSync(): ModelConfig? {
        return runBlocking { selectedModelConfig.first() }
    }
}