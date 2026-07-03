package com.example.data.repository

import com.example.data.local.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

import com.google.firebase.storage.FirebaseStorage
import android.net.Uri

class AuthRepository {
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): Result<String> {
        return try {
            val storageRef = storage.reference.child("profile_pictures/$userId.jpg")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(Exception("فشل رفع الصورة: ${e.message}"))
        }
    }

    val currentUserIdFlow: Flow<String?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    suspend fun login(email: String, passwordHash: String): Result<UserEntity> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, passwordHash).await()
            val uid = result.user?.uid ?: throw Exception("User not found")
            
            val updates = mapOf(
                "isOnline" to true,
                "lastSeen" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid).update(updates).await()
            
            val document = firestore.collection("users").document(uid).get().await()
            val user = document.toObject(UserEntity::class.java) ?: throw Exception("User data not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("البريد الإلكتروني أو كلمة المرور غير صحيحة"))
        }
    }

    suspend fun register(displayName: String, username: String, email: String, passwordHash: String): Result<UserEntity> {
        return try {
            // First create the authentication user so that subsequent Firestore calls are authenticated
            val result = auth.createUserWithEmailAndPassword(email, passwordHash).await()
            val user = result.user ?: throw Exception("فشل إنشاء الحساب")
            val uid = user.uid

            // Now that we are authenticated, we can query Firestore without security rule violations
            val existing = firestore.collection("users").whereEqualTo("username", username).get().await()
            if (!existing.isEmpty) {
                // Username is taken, clean up the auth user to keep the state consistent
                try {
                    user.delete().await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                auth.signOut()
                return Result.failure(Exception("اسم المستخدم محجوز مسبقاً"))
            }
            
            val newUser = UserEntity(
                id = uid,
                username = username,
                displayName = displayName,
                email = email,
                createdAt = System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis(),
                isOnline = true,
                isVerified = false
            )
            
            firestore.collection("users").document(uid).set(newUser).await()
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "حدث خطأ أثناء التسجيل"))
        }
    }

    suspend fun logout() {
        try {
            auth.currentUser?.uid?.let { uid ->
                val updates = mapOf(
                    "isOnline" to false,
                    "lastSeen" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid).update(updates).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            auth.signOut()
        }
    }

    suspend fun updateProfile(userId: String, displayName: String, username: String, bio: String, photoUrl: String): Result<Unit> {
        return try {
            val currentUserDoc = firestore.collection("users").document(userId).get().await()
            val currentUsername = currentUserDoc.getString("username")
            
            if (currentUsername != username) {
                val existing = firestore.collection("users").whereEqualTo("username", username).get().await()
                if (!existing.isEmpty) {
                    return Result.failure(Exception("اسم المستخدم محجوز مسبقاً"))
                }
            }

            val updates = mapOf(
                "displayName" to displayName,
                "username" to username,
                "bio" to bio,
                "photoUrl" to photoUrl
            )
            firestore.collection("users").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "حدث خطأ أثناء تحديث الملف الشخصي"))
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("البريد الإلكتروني غير مسجل أو حدث خطأ"))
        }
    }
}
