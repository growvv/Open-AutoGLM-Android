package com.lfr.baozi.data.database

import androidx.room.Embedded
import androidx.room.Relation


/**
 * 对话与消息的关系
 */
data class ConversationWithMessages(
    @Embedded val conversation: Conversation,
    @Relation(
        parentColumn = "id",
        entityColumn = "conversationId"
    )
    val messages: List<SavedChatMessage>
)