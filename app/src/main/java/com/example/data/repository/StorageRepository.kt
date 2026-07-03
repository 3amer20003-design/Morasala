package com.example.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StorageRepository {
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<String> {
        return try {
            val ref = storage.reference.child("avatars/$userId.jpg")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadVoiceMessage(chatId: String, audioUri: Uri): Result<String> {
        return try {
            val fileName = UUID.randomUUID().toString() + ".m4a"
            val ref = storage.reference.child("chats/$chatId/voice/$fileName")
            ref.putFile(audioUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
