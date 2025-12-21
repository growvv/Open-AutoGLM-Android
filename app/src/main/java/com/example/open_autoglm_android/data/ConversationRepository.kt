package com.example.open_autoglm_android.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 单个对话
 */
data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<SavedChatMessage> = emptyList()
)

/**
 * 保存的聊天消息
 */
data class SavedChatMessage(
    val id: String,
    val role: String, // "USER" or "ASSISTANT"
    val content: String,
    val thinking: String? = null,
    val action: String? = null,
    val imagePath: String? = null, // 新增：保存标记过动作的截图路径
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 对话仓库 - 管理多个对话的存储和切换
 */
class ConversationRepository(private val context: Context) {
    
    private val gson = Gson()
    private val conversationsFile: File
        get() = File(context.filesDir, "conversations.json")
    
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()
    
    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()
    
    init {
        // 初始化时加载对话列表
        loadConversations()
    }
    
    private fun loadConversations() {
        try {
            if (conversationsFile.exists()) {
                val json = conversationsFile.readText()
                val type = object : TypeToken<List<Conversation>>() {}.type
                val loaded: List<Conversation> = gson.fromJson(json, type) ?: emptyList()
                _conversations.value = loaded.sortedByDescending { it.updatedAt }
                
                // 如果有对话，默认选中最近的一个
                if (loaded.isNotEmpty() && _currentConversationId.value == null) {
                    _currentConversationId.value = loaded.maxByOrNull { it.updatedAt }?.id
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _conversations.value = emptyList()
        }
    }
    
    private suspend fun saveConversations() {
        withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(_conversations.value)
                conversationsFile.writeText(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 创建新对话
     */
    suspend fun createConversation(title: String = "新对话"): Conversation {
        val conversation = Conversation(title = title)
        _conversations.value = listOf(conversation) + _conversations.value
        _currentConversationId.value = conversation.id
        saveConversations()
        return conversation
    }
    
    /**
     * 切换当前对话
     */
    fun switchConversation(conversationId: String) {
        if (_conversations.value.any { it.id == conversationId }) {
            _currentConversationId.value = conversationId
        }
    }
    
    /**
     * 获取当前对话
     */
    fun getCurrentConversation(): Conversation? {
        val id = _currentConversationId.value ?: return null
        return _conversations.value.find { it.id == id }
    }
    
    /**
     * 更新对话消息
     */
    suspend fun updateConversationMessages(conversationId: String, messages: List<SavedChatMessage>) {
        val updatedList = _conversations.value.map { conv ->
            if (conv.id == conversationId) {
                val newTitle = if (messages.isNotEmpty() && conv.title == "新对话") {
                    // 使用第一条用户消息作为标题
                    messages.firstOrNull { it.role == "USER" }?.content?.take(20) ?: conv.title
                } else {
                    conv.title
                }
                conv.copy(
                    title = newTitle,
                    messages = messages,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                conv
            }
        }.sortedByDescending { it.updatedAt }
        
        _conversations.value = updatedList
        saveConversations()
    }
    
    /**
     * 删除对话
     */
    suspend fun deleteConversation(conversationId: String) {
        // 删除该对话关联的所有图片
        _conversations.value.find { it.id == conversationId }?.messages?.forEach { msg ->
            msg.imagePath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        _conversations.value = _conversations.value.filter { it.id != conversationId }
        
        // 如果删除的是当前对话，切换到最近的对话
        if (_currentConversationId.value == conversationId) {
            _currentConversationId.value = _conversations.value.firstOrNull()?.id
        }
        
        saveConversations()
    }
    
    /**
     * 重命名对话
     */
    suspend fun renameConversation(conversationId: String, newTitle: String) {
        _conversations.value = _conversations.value.map { conv ->
            if (conv.id == conversationId) {
                conv.copy(title = newTitle)
            } else {
                conv
            }
        }
        saveConversations()
    }
}
