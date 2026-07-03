package com.example.data.local

data class MessageEntity(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val type: String = "TEXT", // "TEXT", "VOICE"
    val status: String = "SENT" // "SENT", "DELIVERED", "READ"
)
