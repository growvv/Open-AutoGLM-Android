package com.lfr.baozi.data

import android.content.Context
import com.lfr.baozi.data.database.AppDatabase
import com.lfr.baozi.data.database.Conversation
import com.lfr.baozi.data.database.ConversationStatus
import com.lfr.baozi.data.database.ConversationWithMessages
import com.lfr.baozi.data.database.SavedChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * 对话仓库 - 管理多个对话的存储和切换
 */
class ConversationRepository(context: Context) {
    
    // 创建一个安全的协程作用域
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val database = AppDatabase.getDatabase(context)
    private val conversationDao = database.conversationDao()
    
    // 从数据库获取对话列表流
    val conversations: Flow<List<Conversation>> = conversationDao.getAllConversations()
    
    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: Flow<String?> = _currentConversationId.asStateFlow()

    private val _currentConversationTitle = MutableStateFlow<String?>(null)
    val currentConversationTitle: Flow<String?> = _currentConversationTitle.asStateFlow()
    
    init {
        // 初始化时加载对话列表并设置默认选中的对话
        coroutineScope.launch {
            val allConversations = conversationDao.getAllConversations().first()
            if (allConversations.isNotEmpty()) {
                val conversation = allConversations.first()
                _currentConversationId.value = conversation.id
                _currentConversationTitle.value = conversation.title
            }
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        coroutineScope.cancel()
    }
    
    /**
     * 创建新对话
     */
    suspend fun createConversation(title: String = "新任务"): Conversation {
        val conversation = Conversation(title = title)
        conversationDao.insertConversation(conversation)
        _currentConversationId.value = conversation.id
        _currentConversationTitle.value = title
        return conversation
    }
    
    /**
     * 切换当前对话
     */
    fun switchConversation(conversationId: String) {
        _currentConversationId.value = conversationId
    }
    
    /**
     * 获取当前对话（包含消息）
     */
    suspend fun getCurrentConversation(): ConversationWithMessages? {
        val id = _currentConversationId.value ?: return null
        return conversationDao.getConversationWithMessages(id)
    }
    
    /**
     * 更新对话消息
     */
    suspend fun updateConversationMessages(conversationId: String, messages: List<SavedChatMessage>) {
        val conversation = conversationDao.getConversationById(conversationId) ?: return
        
        // 更新对话标题（如果是新对话且有用户消息）
        val newTitle =
            if (
                messages.isNotEmpty() &&
                    (conversation.title == "新对话" || conversation.title == "新任务")
            ) {
            messages.firstOrNull { it.role == "USER" }?.content?.take(20) ?: conversation.title
        } else {
            conversation.title
        }
        
        // 更新对话
        val updatedConversation = conversation.copy(
            title = newTitle,
            updatedAt = System.currentTimeMillis()
        )
        conversationDao.updateConversation(updatedConversation)
        
        // 更新消息（先删除旧消息，再插入新消息）
        conversationDao.deleteMessagesByConversationId(conversationId)
        if (messages.isNotEmpty()) {
            conversationDao.insertMessages(messages)
        }
        
        // 更新当前对话ID
        _currentConversationId.value = conversationId
    }

    /**
     * 更新对话状态
     */
    suspend fun updateConversationStatus(conversationId: String, status: ConversationStatus) {
        val conversation = conversationDao.getConversationById(conversationId) ?: return
        val updatedConversation =
            conversation.copy(
                status = status.name,
                updatedAt = System.currentTimeMillis()
            )
        conversationDao.updateConversation(updatedConversation)
    }

    /**
     * 标记任务开始（写入开始时间并清空结束信息）
     */
    suspend fun markTaskStarted(conversationId: String, startedAt: Long) {
        val conversation = conversationDao.getConversationById(conversationId) ?: return
        val updatedConversation =
            conversation.copy(
                status = ConversationStatus.RUNNING.name,
                taskStartedAt = startedAt,
                taskEndedAt = null,
                taskResultMessage = null,
                updatedAt = System.currentTimeMillis()
            )
        conversationDao.updateConversation(updatedConversation)
    }

    /**
     * 标记任务结束（写入结束时间与结果信息）
     */
    suspend fun markTaskFinished(
        conversationId: String,
        status: ConversationStatus,
        endedAt: Long,
        resultMessage: String?
    ) {
        val conversation = conversationDao.getConversationById(conversationId) ?: return
        val updatedConversation =
            conversation.copy(
                status = status.name,
                taskEndedAt = endedAt,
                taskResultMessage = resultMessage,
                updatedAt = System.currentTimeMillis()
            )
        conversationDao.updateConversation(updatedConversation)
    }
    
    /**
     * 删除对话
     */
    suspend fun deleteConversation(conversationId: String) {
        // 获取对话的消息，用于删除关联的图片
        val messages = conversationDao.getMessagesByConversationId(conversationId)
        messages.forEach { msg ->
            msg.imagePath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        // 删除对话及其消息
        conversationDao.deleteMessagesByConversationId(conversationId)
        conversationDao.deleteConversation(conversationId)
        
        // 如果删除的是当前对话，切换到最近的对话
        if (_currentConversationId.value == conversationId) {
            val remainingConversations = conversationDao.getAllConversations().first()
            _currentConversationId.value = remainingConversations.firstOrNull()?.id
            _currentConversationTitle.value = remainingConversations.firstOrNull()?.title
        }
    }
    
    /**
     * 重命名对话
     */
    suspend fun renameConversation(conversationId: String, newTitle: String) {
        val conversation = conversationDao.getConversationById(conversationId) ?: return
        val updatedConversation = conversation.copy(title = newTitle)
        _currentConversationTitle.value = newTitle
        conversationDao.updateConversation(updatedConversation)
    }
}
