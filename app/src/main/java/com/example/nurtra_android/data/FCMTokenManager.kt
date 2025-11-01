package com.example.nurtra_android.data

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Manages Firebase Cloud Messaging (FCM) token operations
 * Handles token generation, retrieval, and token refresh callbacks
 */
class FCMTokenManager {
    private val messaging = FirebaseMessaging.getInstance()
    private val TAG = "FCMTokenManager"

    /**
     * Retrieves the current FCM token for this device
     * Returns null if token is unavailable
     */
    suspend fun getToken(): String? = try {
        val token = messaging.token.await()
        Log.d(TAG, "FCM Token retrieved: ${token.take(10)}...")
        token
    } catch (e: Exception) {
        Log.e(TAG, "Error retrieving FCM token: ${e.message}", e)
        null
    }

    /**
     * Subscribes to a topic for push notifications
     */
    suspend fun subscribeToTopic(topic: String): Result<Unit> = try {
        messaging.subscribeToTopic(topic).await()
        Log.d(TAG, "Successfully subscribed to topic: $topic")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error subscribing to topic $topic: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Unsubscribes from a topic
     */
    suspend fun unsubscribeFromTopic(topic: String): Result<Unit> = try {
        messaging.unsubscribeFromTopic(topic).await()
        Log.d(TAG, "Successfully unsubscribed from topic: $topic")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error unsubscribing from topic $topic: ${e.message}", e)
        Result.failure(e)
    }
}
