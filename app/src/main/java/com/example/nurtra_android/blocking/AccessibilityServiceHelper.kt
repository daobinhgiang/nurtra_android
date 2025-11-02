package com.example.nurtra_android.blocking

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Helper class for managing Accessibility Service permissions
 */
object AccessibilityServiceHelper {
    
    /**
     * Check if the AppBlockingService is enabled
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        
        val serviceClassName = AppBlockingService::class.java.name
        val packageName = context.packageName
        
        return enabledServices.any { service ->
            val serviceInfo = service.resolveInfo.serviceInfo
            serviceInfo.packageName == packageName && 
            serviceInfo.name == serviceClassName
        }
    }
    
    /**
     * Open Accessibility Settings where user can enable the service
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    /**
     * Get the service name for display purposes
     */
    fun getServiceName(context: Context): String {
        return "Nurtra App Blocking"
    }
}

/**
 * Dialog to request accessibility service permission
 */
@Composable
fun AccessibilityServicePermissionDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    serviceName: String = "Nurtra App Blocking"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Enable App Blocking")
        },
        text = {
            Text(
                "To block distracting apps during cravings, Nurtra needs accessibility permission.\n\n" +
                "This permission allows Nurtra to detect when you try to open blocked apps and redirect you back.\n\n" +
                "Your privacy is protected - we only monitor apps while blocking is active during cravings."
            )
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}

