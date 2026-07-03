package com.example.data.local

import kotlinx.serialization.Serializable

@Serializable
data class UserEntity(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val createdAt: Long = 0L,
    val lastSeen: Long = 0L,
    val isOnline: Boolean = false,
    val isVerified: Boolean = false
)
