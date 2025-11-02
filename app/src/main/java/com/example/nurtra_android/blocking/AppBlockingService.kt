package com.example.nurtra_android.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.nurtra_android.MainActivity

/**
 * AccessibilityService that monitors app launches and blocks specified apps
 * when blocking is active
 */
class AppBlockingService : AccessibilityService() {
    
    private lateinit var blockingManager: AppBlockingManager
    private val TAG = "AppBlockingService"
    private var lastBlockedPackage: String? = null
    private var lastBlockedTime: Long = 0
    
    companion object {
        private const val BLOCK_COOLDOWN_MS = 1000L // Prevent rapid repeated blocking
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        blockingManager = AppBlockingManager.getInstance(this)
        Log.d(TAG, "App Blocking Service connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // Only process window state changes (app switches)
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        
        val packageName = event.packageName?.toString() ?: return
        
        // Don't block our own app
        if (packageName == this.packageName) {
            return
        }
        
        // Check if blocking is active and this app should be blocked
        if (blockingManager.shouldBlockApp(packageName)) {
            blockApp(packageName)
        }
    }
    
    /**
     * Blocks an app by bringing Nurtra to foreground
     */
    private fun blockApp(packageName: String) {
        val currentTime = System.currentTimeMillis()
        
        // Prevent rapid repeated blocking of the same app
        if (packageName == lastBlockedPackage && 
            currentTime - lastBlockedTime < BLOCK_COOLDOWN_MS) {
            return
        }
        
        lastBlockedPackage = packageName
        lastBlockedTime = currentTime
        
        Log.d(TAG, "Blocking app: $packageName")
        
        // Bring Nurtra app to foreground
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("blocked_app", packageName)
            putExtra("show_blocking_message", true)
        }
        
        startActivity(intent)
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "App Blocking Service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "App Blocking Service destroyed")
    }
}


