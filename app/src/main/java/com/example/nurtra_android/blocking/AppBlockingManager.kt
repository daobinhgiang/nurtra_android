package com.example.nurtra_android.blocking

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Manages the app blocking state and blocked apps list
 * Uses SharedPreferences for fast local access during blocking checks
 */
class AppBlockingManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val TAG = "AppBlockingManager"
    
    companion object {
        private const val PREFS_NAME = "app_blocking_prefs"
        private const val KEY_BLOCKING_ACTIVE = "blocking_active"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        
        @Volatile
        private var instance: AppBlockingManager? = null
        
        fun getInstance(context: Context): AppBlockingManager {
            return instance ?: synchronized(this) {
                instance ?: AppBlockingManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Check if blocking is currently active
     */
    fun isBlockingActive(): Boolean {
        return prefs.getBoolean(KEY_BLOCKING_ACTIVE, false)
    }
    
    /**
     * Activate app blocking
     */
    fun activateBlocking() {
        Log.d(TAG, "Activating app blocking")
        prefs.edit().putBoolean(KEY_BLOCKING_ACTIVE, true).apply()
    }
    
    /**
     * Deactivate app blocking
     */
    fun deactivateBlocking() {
        Log.d(TAG, "Deactivating app blocking")
        prefs.edit().putBoolean(KEY_BLOCKING_ACTIVE, false).apply()
    }
    
    /**
     * Get the list of blocked app package names
     */
    fun getBlockedApps(): Set<String> {
        val appsString = prefs.getString(KEY_BLOCKED_APPS, "") ?: ""
        return if (appsString.isEmpty()) {
            emptySet()
        } else {
            appsString.split(",").toSet()
        }
    }
    
    /**
     * Update the list of blocked apps
     * @param packageNames List of package names to block
     */
    fun updateBlockedApps(packageNames: List<String>) {
        val appsString = packageNames.joinToString(",")
        prefs.edit().putString(KEY_BLOCKED_APPS, appsString).apply()
        Log.d(TAG, "Updated blocked apps: ${packageNames.size} apps")
    }
    
    /**
     * Check if a specific app should be blocked
     * @param packageName Package name of the app to check
     * @return true if the app should be blocked
     */
    fun shouldBlockApp(packageName: String): Boolean {
        if (!isBlockingActive()) {
            return false
        }
        
        val blockedApps = getBlockedApps()
        return blockedApps.contains(packageName)
    }
    
    /**
     * Clear all blocking data
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Cleared all blocking data")
    }
}


