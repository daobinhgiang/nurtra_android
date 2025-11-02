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
        Log.d(TAG, "==========================================")
        Log.d(TAG, "NEW FCM TOKEN GENERATED")
        Log.d(TAG, "Full Token: $token")
        Log.d(TAG, "Token Length: ${token.length} characters")
        Log.d(TAG, "==========================================")
        
        // Save the token to Firestore for the current user
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Saving token for user: ${currentUser.uid}")
            saveTokenToFirestore(currentUser.uid, token)
        } else {
            Log.w(TAG, "No authenticated user to save FCM token")
            Log.w(TAG, "Token will be saved after user logs in")
        }
    }

    /**
     * Called when a message is received while the app is in the foreground
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received: ${message.messageId}")
        
        // Log message data for debugging
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${message.data}")
        }
        
        // Display notification
        message.notification?.let { notification ->
            Log.d(TAG, "Message title: ${notification.title}")
            Log.d(TAG, "Message body: ${notification.body}")
            
            // Create notification helper and display the notification
            val notificationHelper = NotificationHelper(applicationContext)
            val title = notification.title ?: "Nurtra"
            val body = notification.body ?: ""
            
            // Display notification based on type
            when (message.data["type"]) {
                "motivational" -> {
                    notificationHelper.showMotivationalNotification(title, body, message.data)
                }
                else -> {
                    notificationHelper.showGeneralNotification(title, body)
                }
            }
        } ?: run {
            // Handle data-only message (no notification payload)
            if (message.data.isNotEmpty()) {
                val notificationHelper = NotificationHelper(applicationContext)
                val title = message.data["title"] ?: "Nurtra"
                val body = message.data["body"] ?: "You have a new message"
                
                when (message.data["type"]) {
                    "motivational" -> {
                        notificationHelper.showMotivationalNotification(title, body, message.data)
                    }
                    else -> {
                        notificationHelper.showGeneralNotification(title, body)
                    }
                }
            }
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
