package com.example.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: UserEntity? = null,
    val isSaving: Boolean = false
)

class ProfileViewModel(
    private val userId: String,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.getUserFlow(userId).collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun updateProfile(displayName: String, username: String, bio: String, photoUrl: String, imageUri: android.net.Uri?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            var finalPhotoUrl = photoUrl
            if (imageUri != null) {
                val uploadResult = authRepository.uploadProfilePicture(userId, imageUri)
                if (uploadResult.isSuccess) {
                    finalPhotoUrl = uploadResult.getOrNull() ?: photoUrl
                }
            }
            
            val result = authRepository.updateProfile(userId, displayName, username, bio, finalPhotoUrl)
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}

class ProfileViewModelFactory(
    private val userId: String,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userId, authRepository, chatRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
