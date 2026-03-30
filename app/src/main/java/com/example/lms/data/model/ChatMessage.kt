package com.example.lms.data.model

enum class ChatSender {
    USER,
    BOT,
    SYSTEM
}

data class ChatMessage(
    val id: String = "",
    val sessionId: String = "",
    val sender: ChatSender = ChatSender.USER,
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

