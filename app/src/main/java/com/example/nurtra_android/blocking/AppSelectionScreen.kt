package com.example.nurtra_android.blocking

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Bitmap

/**
 * Data class representing an installed app
 */
data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable?
)

/**
 * Screen for selecting apps to block during cravings
 */
@Composable
fun AppSelectionScreen(
    selectedApps: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load installed apps
    LaunchedEffect(Unit) {
        isLoading = true
        installedApps = withContext(Dispatchers.IO) {
            loadInstalledApps(context.packageManager)
        }
        isLoading = false
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading apps...",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Instruction text
            Text(
                text = "Select apps to block during cravings",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Apps list
            if (installedApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No apps found",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(installedApps) { app ->
                        AppListItem(
                            app = app,
                            isSelected = selectedApps.contains(app.packageName),
                            onToggle = {
                                val newSelection = if (selectedApps.contains(app.packageName)) {
                                    selectedApps - app.packageName
                                } else {
                                    selectedApps + app.packageName
                                }
                                onSelectionChanged(newSelection)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual app list item
 */
@Composable
private fun AppListItem(
    app: InstalledApp,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    // Convert drawable to bitmap outside of composable scope
    val iconBitmap = remember(app.icon) {
        try {
            app.icon?.toBitmap()
        } catch (e: Exception) {
            null
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App icon
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap.asImageBitmap(),
                    contentDescription = "${app.appName} icon",
                    modifier = Modifier.size(40.dp)
                )
            } else {
                // Fallback if icon can't be converted or doesn't exist
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // App name
            Text(
                text = app.appName,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            
            // Checkbox
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Load installed apps from the device
 * Only includes apps that have launcher activities (visible and launchable from device interface)
 * Filters out the Nurtra app itself to prevent users from blocking their own app
 */
private fun loadInstalledApps(packageManager: PackageManager): List<InstalledApp> {
    val apps = mutableListOf<InstalledApp>()
    
    try {
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (packageInfo in packages) {
            // Skip Nurtra app itself (users shouldn't block their own app)
            if (packageInfo.packageName == "com.example.nurtra_android") {
                continue
            }
            
            // Check if this app has a launcher activity (is visible/launchable from device interface)
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageInfo.packageName)
            }
            val launcherActivities = packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            
            // Only include apps that have at least one launcher activity
            if (launcherActivities.isEmpty()) {
                continue
            }
            
            val appName = packageManager.getApplicationLabel(packageInfo).toString()
            val icon = packageManager.getApplicationIcon(packageInfo)
            
            apps.add(
                InstalledApp(
                    packageName = packageInfo.packageName,
                    appName = appName,
                    icon = icon
                )
            )
        }
        
        // Sort alphabetically by app name
        apps.sortBy { it.appName.lowercase() }
        
    } catch (e: Exception) {
        // Log error but don't crash
        android.util.Log.e("AppSelectionScreen", "Error loading apps: ${e.message}", e)
    }
    
    return apps
}

