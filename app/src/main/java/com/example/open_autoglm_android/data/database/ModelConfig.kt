package com.example.open_autoglm_android.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 模型配置实体类，用于存储多个模型的配置信息
 */
@Entity(tableName = "model_configs")
data class ModelConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // 模型名称（用于显示）
    val apiKey: String, // API密钥
    val baseUrl: String, // 基础URL
    val modelName: String, // 模型名称（用于API调用）
    val isSelected: Boolean = false // 是否为当前选中的模型
)