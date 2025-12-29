package com.lfr.baozi.data.database

enum class ConversationStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    ABORTED,
    ENDED;

    fun displayName(): String =
        when (this) {
            IDLE -> "未开始"
            RUNNING -> "进行中"
            COMPLETED -> "完成"
            ABORTED -> "中止"
            ENDED -> "结束"
        }

    companion object {
        fun fromRaw(raw: String?): ConversationStatus =
            entries.firstOrNull { it.name == raw } ?: IDLE
    }
}

