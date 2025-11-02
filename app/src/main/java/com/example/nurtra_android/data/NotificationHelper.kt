package com.example.nurtra_android.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.nurtra_android.MainActivity
import com.example.nurtra_android.R

/**
 * Helper class to manage notification channels and display notifications
 * Handles Android 8.0+ notification channel requirements
 */
class NotificationHelper(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID_MOTIVATIONAL = "motivational_notifications"
        const val CHANNEL_ID_GENERAL = "general_notifications"
        private const val NOTIFICATION_ID_MOTIVATIONAL = 1001
    }
    
    /**
     * Creates all required notification channels
     * Must be called before attempting to show notifications on Android 8.0+
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Motivational notifications channel
            val motivationalChannel = NotificationChannel(
                CHANNEL_ID_MOTIVATIONAL,
                "Motivational Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Personalized motivational messages to help you stay strong"
                enableVibration(true)
                enableLights(true)
            }
            
            // General notifications channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "General Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app updates and information"
                enableVibration(true)
            }
            
            // Register channels
            notificationManager.createNotificationChannel(motivationalChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }
    
    /**
     * Shows a motivational notification with the given title and message
     */
    fun showMotivationalNotification(title: String, message: String, notificationData: Map<String, String>? = null) {
        // Create intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Add any extra data from the notification payload
            notificationData?.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            pendingIntentFlags
        )
        
        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MOTIVATIONAL)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_notification) // We'll create this icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Dismiss notification when tapped
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        
        // Show the notification
        notificationManager.notify(NOTIFICATION_ID_MOTIVATIONAL, notification)
    }
    
    /**
     * Shows a general notification with the given title and message
     */
    fun showGeneralNotification(title: String, message: String, notificationId: Int = 2001) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            pendingIntentFlags
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Checks if notification permission is granted
     * For Android 13+ (API 33+)
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
    
    /**
     * Cancels all notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}

