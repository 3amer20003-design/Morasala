package com.example.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.MessageEntity
import com.example.data.local.UserEntity
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val peerUser: UserEntity? = null,
    val messages: List<MessageEntity> = emptyList(),
    val peerTypingState: String = "IDLE", // "IDLE", "TYPING", "RECORDING"
    val isLoading: Boolean = true
)

class ChatViewModel(
    private val currentUserId: String,
    private val peerId: String,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var typingJob: kotlinx.coroutines.Job? = null

    init {
        // Collect real-time changes to the peer user (including isOnline and lastSeen)
        viewModelScope.launch {
            chatRepository.getUserFlow(peerId).collect { peer ->
                _uiState.update { it.copy(peerUser = peer) }
            }
        }
        
        // Collect real-time messages
        viewModelScope.launch {
            chatRepository.getChatMessages(currentUserId, peerId).collect { msgs ->
                _uiState.update { it.copy(messages = msgs, isLoading = false) }
                // Mark messages as read
                chatRepository.markMessagesAsRead(currentUserId, peerId)
            }
        }

        // Collect real-time typing/recording state of the peer
        viewModelScope.launch {
            chatRepository.getTypingState(peerId, currentUserId).collect { state ->
                _uiState.update { it.copy(peerTypingState = state) }
            }
        }
    }

    fun sendMessage(content: String, type: String = "TEXT") {
        viewModelScope.launch {
            // Cancel typing job & set status to IDLE
            typingJob?.cancel()
            chatRepository.setTypingState(currentUserId, peerId, "IDLE")
            chatRepository.sendMessage(currentUserId, peerId, content, type)
        }
    }

    fun updateTypingState(isTyping: Boolean) {
        typingJob?.cancel()
        if (isTyping) {
            viewModelScope.launch {
                chatRepository.setTypingState(currentUserId, peerId, "TYPING")
            }
            // Debounce typing status to return to IDLE after 2.5 seconds of inactivity
            typingJob = viewModelScope.launch {
                kotlinx.coroutines.delay(2500)
                chatRepository.setTypingState(currentUserId, peerId, "IDLE")
            }
        } else {
            viewModelScope.launch {
                chatRepository.setTypingState(currentUserId, peerId, "IDLE")
            }
        }
    }

    fun setTypingState(state: String) {
        typingJob?.cancel()
        viewModelScope.launch {
            chatRepository.setTypingState(currentUserId, peerId, state)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Reset current user's typing state when leaving the chat
        viewModelScope.launch {
            chatRepository.setTypingState(currentUserId, peerId, "IDLE")
        }
    }
}

class ChatViewModelFactory(
    private val currentUserId: String,
    private val peerId: String,
    private val repository: ChatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(currentUserId, peerId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
