package com.example.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable data object Splash
    @Serializable data object Login
    @Serializable data object Register
    @Serializable data object ForgotPassword
    @Serializable data object Home
    @Serializable data class Chat(val peerId: String)
    @Serializable data class UserProfile(val userId: String)
    @Serializable data object Settings
}
