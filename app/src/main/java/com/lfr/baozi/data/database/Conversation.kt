package com.example.open_autoglm_android.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 单个对话
 */
@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
    val status: String = ConversationStatus.IDLE.name,
    val taskStartedAt: Long? = null,
    val taskEndedAt: Long? = null,
    val taskResultMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
