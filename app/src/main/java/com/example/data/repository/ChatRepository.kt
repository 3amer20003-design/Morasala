package com.example.data.repository

import com.example.data.local.MessageEntity
import com.example.data.local.UserEntity
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository {
    
    private val firestore = FirebaseFirestore.getInstance()

    fun searchUsers(query: String): Flow<List<UserEntity>> = callbackFlow {
        if (query.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val db = firestore.collection("users")
        val dListener = db
            .orderBy("displayName")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(20)
            .addSnapshotListener { dSnapshot, _ ->
                val displayUsers = dSnapshot?.documents?.mapNotNull { it.toObject(UserEntity::class.java) } ?: emptyList()
                db.orderBy("username")
                    .startAt(query)
                    .endAt(query + "\uf8ff")
                    .limit(20)
                    .get()
                    .addOnSuccessListener { uSnapshot ->
                        val usernameUsers = uSnapshot?.documents?.mapNotNull { it.toObject(UserEntity::class.java) } ?: emptyList()
                        val combined = (displayUsers + usernameUsers).distinctBy { it.id }
                        trySend(combined)
                    }
                    .addOnFailureListener {
                        trySend(displayUsers)
                    }
            }
        awaitClose { dListener.remove() }
    }

    fun getRecentChats(currentUserId: String): Flow<List<MessageEntity>> = callbackFlow {
        val listener = firestore.collection("messages")
            .where(Filter.or(
                Filter.equalTo("senderId", currentUserId),
                Filter.equalTo("receiverId", currentUserId)
            ))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(150) // Paginated subset to keep reads very low
            .addSnapshotListener { snapshot, _ ->
                val allMessages = snapshot?.documents?.mapNotNull { it.toObject(MessageEntity::class.java) } ?: emptyList()
                
                // Group by peer and get the latest message
                val recent = allMessages.groupBy { 
                    if (it.senderId == currentUserId) it.receiverId else it.senderId 
                }.map { it.value.first() }.sortedByDescending { it.timestamp }
                
                trySend(recent)
            }
        awaitClose { listener.remove() }
    }

    fun getChatMessages(user1Id: String, user2Id: String, limit: Long = 50): Flow<List<MessageEntity>> = callbackFlow {
        val listener = firestore.collection("messages")
            .where(Filter.or(
                Filter.and(
                    Filter.equalTo("senderId", user1Id),
                    Filter.equalTo("receiverId", user2Id)
                ),
                Filter.and(
                    Filter.equalTo("senderId", user2Id),
                    Filter.equalTo("receiverId", user1Id)
                )
            ))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, _ ->
                val msgs = snapshot?.documents?.mapNotNull { it.toObject(MessageEntity::class.java) } ?: emptyList()
                // Return sorted ascending for standard top-to-bottom chat bubble flow
                trySend(msgs.sortedBy { it.timestamp })
            }
        awaitClose { listener.remove() }
    }

    fun getTypingState(userId: String, peerId: String): Flow<String> = callbackFlow {
        val docRef = firestore.collection("typing_states").document("${userId}_${peerId}")
        val listener = docRef.addSnapshotListener { snapshot, _ ->
            val status = snapshot?.getString("status") ?: "IDLE"
            trySend(status)
        }
        awaitClose { listener.remove() }
    }

    suspend fun setTypingState(userId: String, peerId: String, status: String) {
        try {
            val docRef = firestore.collection("typing_states").document("${userId}_${peerId}")
            val data = mapOf(
                "userId" to userId,
                "peerId" to peerId,
                "status" to status,
                "timestamp" to System.currentTimeMillis()
            )
            docRef.set(data).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getUserFlow(id: String): Flow<UserEntity?> = callbackFlow {
        val listener = firestore.collection("users").document(id)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(UserEntity::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun getUserById(id: String): UserEntity? {
        return try {
            firestore.collection("users").document(id).get().await().toObject(UserEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getUnreadCount(currentUserId: String, peerId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection("messages")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("senderId", peerId)
            .whereNotEqualTo("status", "READ")
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markMessagesAsRead(currentUserId: String, peerId: String) {
        try {
            val unread = firestore.collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("senderId", peerId)
                .whereNotEqualTo("status", "READ")
                .get().await()
                
            firestore.runBatch { batch ->
                unread.documents.forEach { doc ->
                    batch.update(doc.reference, "status", "READ")
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sendMessage(senderId: String, receiverId: String, content: String, type: String = "TEXT") {
        val id = UUID.randomUUID().toString()
        val message = MessageEntity(
            id = id,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            timestamp = System.currentTimeMillis(),
            type = type,
            status = "SENT"
        )
        try {
            firestore.collection("messages").document(id).set(message).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
