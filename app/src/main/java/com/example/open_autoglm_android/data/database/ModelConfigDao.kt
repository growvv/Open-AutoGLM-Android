package com.example.open_autoglm_android.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 模型配置数据访问对象，定义对模型配置的增删改查操作
 */
@Dao
interface ModelConfigDao {
    // 获取所有模型配置
    @Query("SELECT * FROM model_configs")
    fun getAllModelConfigs(): Flow<List<ModelConfig>>

    // 根据ID获取模型配置
    @Query("SELECT * FROM model_configs WHERE id = :id")
    suspend fun getModelConfigById(id: Long): ModelConfig?

    // 获取当前选中的模型配置
    @Query("SELECT * FROM model_configs WHERE isSelected = 1")
    fun getSelectedModelConfig(): Flow<ModelConfig?>

    // 插入模型配置
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModelConfig(modelConfig: ModelConfig): Long

    // 更新模型配置
    @Update
    suspend fun updateModelConfig(modelConfig: ModelConfig)

    // 删除模型配置
    @Delete
    suspend fun deleteModelConfig(modelConfig: ModelConfig)

    // 更新所有模型的选中状态为未选中
    @Query("UPDATE model_configs SET isSelected = 0")
    suspend fun unselectAllModels()

    // 根据ID选中模型
    @Query("UPDATE model_configs SET isSelected = 1 WHERE id = :id")
    suspend fun selectModelById(id: Long)
}