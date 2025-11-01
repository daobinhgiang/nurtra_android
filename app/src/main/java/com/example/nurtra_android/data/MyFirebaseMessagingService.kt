package com.example.nurtra_android.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging service that handles:
 * - Token refresh callbacks
 * - Incoming push messages
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "FCMService"
    private val firestoreManager = FirestoreManager()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Called when a new FCM token is generated
     * This happens when the app is first installed, when the user clears app data,
     * or when the token needs to be rotated
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: ${token.take(10)}...")
        
        // Save the token to Firestore for the current user
        val currentUser = auth.currentUser
        if (currentUser != null) {
            saveTokenToFirestore(currentUser.uid, token)
        } else {
            Log.w(TAG, "No authenticated user to save FCM token")
        }
    }

    /**
     * Called when a message is received while the app is in the foreground
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received: ${message.messageId}")
        
        // Handle the message (you can display a notification, etc.)
        // For now, we'll just log it
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${message.data}")
        }
        
        message.notification?.let {
            Log.d(TAG, "Message title: ${it.title}")
            Log.d(TAG, "Message body: ${it.body}")
        }
    }

    /**
     * Saves the FCM token to Firestore for the current user
     */
    private fun saveTokenToFirestore(userId: String, token: String) {
        CoroutineScope(Dispatchers.Default).launch {
            val result = firestoreManager.updateFCMToken(userId, token)
            result.onSuccess {
                Log.d(TAG, "FCM token saved to Firestore for user: $userId")
            }.onFailure { error ->
                Log.e(TAG, "Failed to save FCM token: ${error.message}", error)
            }
        }
    }
}
