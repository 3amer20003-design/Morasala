package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.MessageEntity
import com.example.data.local.UserEntity
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val currentUser: UserEntity? = null,
    val recentChats: List<ChatPreview> = emptyList(),
    val searchResults: List<UserEntity> = emptyList(),
    val isSearching: Boolean = false
)

data class ChatPreview(
    val peerUser: UserEntity,
    val lastMessage: MessageEntity,
    val unreadCount: Int = 0
)

@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val currentUserId: String,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.getUserFlow(currentUserId).collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
        
        viewModelScope.launch {
            chatRepository.getRecentChats(currentUserId).collect { messages ->
                val previews = messages.mapNotNull { msg ->
                    val peerId = if (msg.senderId == currentUserId) msg.receiverId else msg.senderId
                    val peerUser = chatRepository.getUserById(peerId)
                    if (peerUser != null) {
                        // For a real app, unread count should be observed as a flow and combined,
                        // but for simplicity we fetch the first emitted value here.
                        val unreadCount = chatRepository.getUnreadCount(currentUserId, peerId).first()
                        ChatPreview(peerUser, msg, unreadCount)
                    } else null
                }
                _uiState.update { it.copy(recentChats = previews) }
            }
        }

        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isNotBlank()) {
                        chatRepository.searchUsers(query)
                    } else {
                        flowOf(emptyList())
                    }
                }
                .collect { users ->
                    val filtered = users.filter { it.id != currentUserId }
                    _uiState.update { 
                        it.copy(
                            searchResults = filtered, 
                            isSearching = _searchQuery.value.isNotBlank()
                        ) 
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

class HomeViewModelFactory(
    private val currentUserId: String,
    private val repository: ChatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(currentUserId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
