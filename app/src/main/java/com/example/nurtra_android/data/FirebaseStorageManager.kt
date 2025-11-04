package com.example.nurtra_android.data

import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

/**
 * Manages Firebase Storage operations for audio files
 */
class FirebaseStorageManager {
    private val storage = FirebaseStorage.getInstance()
    private val TAG = "FirebaseStorageManager"

    /**
     * Uploads an audio file to Firebase Storage
     * @param userId The user ID to organize files
     * @param quoteId The quote ID for the audio file
     * @param audioData The audio data as ByteArray
     * @return Result containing the download URL as String
     */
    suspend fun uploadQuoteAudio(
        userId: String,
        quoteId: String,
        audioData: ByteArray
    ): Result<String> = try {
        val storagePath = "users/$userId/quotes/$quoteId.mp3"
        Log.d(TAG, "Starting upload for quote $quoteId to path: $storagePath")
        Log.d(TAG, "Audio data size: ${audioData.size} bytes")
        
        // Create reference: users/{userId}/quotes/{quoteId}.mp3
        val storageRef: StorageReference = storage.reference
            .child("users")
            .child(userId)
            .child("quotes")
            .child("$quoteId.mp3")

        Log.d(TAG, "Uploading audio data to Firebase Storage...")
        val startTime = System.currentTimeMillis()
        
        // Upload the audio data
        val uploadTask = storageRef.putBytes(audioData)
        
        // Monitor upload progress
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
            Log.d(TAG, "Upload progress for quote $quoteId: ${String.format("%.1f", progress)}%")
        }
        
        uploadTask.await()
        val uploadDuration = System.currentTimeMillis() - startTime
        Log.d(TAG, "Upload completed for quote $quoteId in ${uploadDuration}ms")
        
        // Get the download URL
        Log.d(TAG, "Fetching download URL for quote $quoteId...")
        val downloadUrl = storageRef.downloadUrl.await()
        
        Log.d(TAG, "Successfully uploaded audio for quote $quoteId")
        Log.d(TAG, "Download URL: $downloadUrl")
        Log.d(TAG, "Total time: ${System.currentTimeMillis() - startTime}ms")
        Result.success(downloadUrl.toString())
    } catch (e: Exception) {
        Log.e(TAG, "Error uploading audio for quote $quoteId: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Deletes all quote audio files for a user
     * @param userId The user ID
     */
    suspend fun deleteUserQuoteAudios(userId: String): Result<Unit> = try {
        val storageRef: StorageReference = storage.reference
            .child("users")
            .child(userId)
            .child("quotes")

        // List all files in the quotes directory
        val listResult = storageRef.listAll().await()
        
        // Delete each file
        listResult.items.forEach { item ->
            item.delete().await()
        }
        
        Log.d(TAG, "Successfully deleted ${listResult.items.size} quote audio files for user $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting quote audios for user $userId: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Deletes a specific quote audio file
     * @param userId The user ID
     * @param quoteId The quote ID
     */
    suspend fun deleteQuoteAudio(userId: String, quoteId: String): Result<Unit> = try {
        val storageRef: StorageReference = storage.reference
            .child("users")
            .child(userId)
            .child("quotes")
            .child("$quoteId.mp3")

        storageRef.delete().await()
        
        Log.d(TAG, "Successfully deleted audio for quote $quoteId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting audio for quote $quoteId: ${e.message}", e)
        Result.failure(e)
    }
}

