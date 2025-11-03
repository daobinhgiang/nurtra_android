package com.example.nurtra_android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nurtra_android.auth.AuthViewModel
import com.example.nurtra_android.auth.LoginScreen
import com.example.nurtra_android.auth.SignUpScreen
import com.example.nurtra_android.blocking.AppBlockingManager
import com.example.nurtra_android.blocking.BlockingOverlay
import com.example.nurtra_android.data.NotificationHelper
import com.example.nurtra_android.data.FCMTokenManager
import com.example.nurtra_android.data.FirestoreManager
import com.example.nurtra_android.data.BingeFreePeriod
import com.example.nurtra_android.theme.NurtraTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private var showBlockingOverlay = mutableStateOf(false)
    private var blockedAppPackage = mutableStateOf<String?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create notification channels
        val notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannels()
        
        // Log FCM token on app startup for debugging
        logFCMToken()
        
        // Check if we're being launched because of a blocked app
        checkForBlockedApp(intent)
        
        setContent {
            NurtraTheme {
                AppContent(
                    showBlockingOverlay = showBlockingOverlay.value,
                    blockedAppPackage = blockedAppPackage.value,
                    onDismissBlockingOverlay = {
                        showBlockingOverlay.value = false
                        blockedAppPackage.value = null
                    }
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkForBlockedApp(intent)
    }
    
    private fun checkForBlockedApp(intent: Intent?) {
        if (intent?.getBooleanExtra("show_blocking_message", false) == true) {
            showBlockingOverlay.value = true
            blockedAppPackage.value = intent.getStringExtra("blocked_app")
        }
    }
    
    /**
     * Logs the current FCM token for debugging purposes
     * This helps you verify the token is being generated correctly
     */
    private fun logFCMToken() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fcmTokenManager = FCMTokenManager()
                val token = fcmTokenManager.getToken()
                
                if (token != null) {
                    Log.i(TAG, "✅ App has valid FCM token")
                    Log.i(TAG, "Copy this token to test notifications:")
                    Log.i(TAG, token)
                } else {
                    Log.w(TAG, "⚠️ FCM token not available yet")
                    Log.w(TAG, "Token will be generated shortly and logged when ready")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error logging FCM token: ${e.message}", e)
            }
        }
    }
}

@Composable
fun AppContent(
    showBlockingOverlay: Boolean = false,
    blockedAppPackage: String? = null,
    onDismissBlockingOverlay: () -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val authUiState by authViewModel.uiState.collectAsState()
    
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var showSignUp by remember { mutableStateOf(false) }
    var hasRequestedNotificationPermission by remember { mutableStateOf(false) }
    
    // Screen state for HomeScreen - needed to navigate to Camera from blocking overlay
    var homeScreenState by remember { mutableStateOf<Screen?>(null) }
    
    // Load blocked apps from Firestore when user logs in
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val firestoreManager = FirestoreManager()
                    val userResult = firestoreManager.getUser(currentUser!!.uid)
                    
                    userResult.onSuccess { nurtraUser ->
                        nurtraUser?.let {
                            // Update AppBlockingManager with the blocked apps
                            val blockingManager = AppBlockingManager.getInstance(context)
                            blockingManager.updateBlockedApps(it.blockedApps)
                            Log.d("MainActivity", "Loaded ${it.blockedApps.size} blocked apps from Firestore")
                        }
                    }.onFailure { error ->
                        Log.e("MainActivity", "Error loading blocked apps: ${error.message}", error)
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Exception loading blocked apps: ${e.message}", e)
                }
            }
        }
    }
    
    // Request notification permission for Android 13+ (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted - notifications will work
        } else {
            // Permission denied - notifications won't be shown
            // You could show a dialog explaining the importance of notifications
        }
    }
    
    // Listen to auth state changes
    LaunchedEffect(Unit) {
        auth.addAuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
    }
    
    // Request notification permission when user is authenticated (Android 13+)
    LaunchedEffect(currentUser) {
        if (currentUser != null && !hasRequestedNotificationPermission) {
            hasRequestedNotificationPermission = true
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Check if permission is already granted
                val permission = Manifest.permission.POST_NOTIFICATIONS
                if (ContextCompat.checkSelfPermission(context, permission) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // Request permission
                    notificationPermissionLauncher.launch(permission)
                }
            }
        }
    }
    
    // Show blocking overlay if needed
    if (showBlockingOverlay) {
        val blockingManager = AppBlockingManager.getInstance(context)
        BlockingOverlay(
            blockedAppName = blockedAppPackage,
            onDismiss = {
                // Dismiss the overlay
                onDismissBlockingOverlay()
                // If blocking is still active (user is in craving session), navigate to Camera screen
                if (blockingManager.isBlockingActive()) {
                    homeScreenState = Screen.Camera
                }
            }
        )
    } else {
        // Normal app flow
        when {
            currentUser == null -> {
                // Not authenticated - show login or sign up
                if (showSignUp) {
                    SignUpScreen(
                        onNavigateToLogin = { showSignUp = false },
                        onSignUpSuccess = { showSignUp = false }
                    )
                } else {
                    LoginScreen(
                        onNavigateToSignUp = { showSignUp = true },
                        onLoginSuccess = { }
                    )
                }
            }
            else -> {
                // Authenticated - check onboarding status
                // Show loading while checking onboarding status
                if (!authUiState.isInitialLoadComplete) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading...",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    // Check if onboarding is completed
                    val onboardingCompleted = authUiState.nurtraUser?.onboardingCompleted ?: false
                    
                    if (onboardingCompleted) {
                        // Onboarding completed - show main app
                        HomeScreen(
                            initialScreen = homeScreenState,
                            onScreenChanged = { screen -> homeScreenState = screen }
                        )
                    } else {
                        // Onboarding not completed - show survey
                        OnboardingSurveyScreen(
                            onComplete = {
                                // After completing onboarding, refresh user data
                                authViewModel.refreshNurtraUser()
                            },
                            authViewModel = authViewModel
                        )
                    }
                }
            }
        }
    }
}

sealed class Screen {
    object Timer : Screen()
    object Camera : Screen()
    object Survey : Screen()
    object Settings : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val authUiState by authViewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAppSelection by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
            
            if (showAppSelection) {
                // Show app selection screen
                val currentUser = FirebaseAuth.getInstance().currentUser
                val blockedApps = authUiState.nurtraUser?.blockedApps?.toSet() ?: emptySet()
                
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        com.example.nurtra_android.blocking.AppSelectionScreen(
                            selectedApps = blockedApps,
                            onSelectionChanged = { selectedApps ->
                                // Update blocked apps in Firestore
                                if (currentUser != null) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val firestoreManager = FirestoreManager()
                                        firestoreManager.updateBlockedApps(currentUser.uid, selectedApps.toList())
                                            .onSuccess {
                                                // Update AppBlockingManager
                                                val blockingManager = AppBlockingManager.getInstance(context)
                                                blockingManager.updateBlockedApps(selectedApps.toList())
                                                // Refresh user data
                                                authViewModel.refreshNurtraUser()
                                                Log.d("SettingsScreen", "Blocked apps updated successfully")
                                            }
                                            .onFailure { error ->
                                                Log.e("SettingsScreen", "Error updating blocked apps: ${error.message}", error)
                                            }
                                    }
                                }
                            }
                        )
                    }
                    
                    // Done button
                    Button(
                        onClick = { showAppSelection = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Done", modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            } else {
                // Show settings options
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // User info section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Account",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = authUiState.user?.email ?: "Not logged in",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Change blocked apps button
                    Button(
                        onClick = { showAppSelection = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Change Blocked Apps",
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Logout button
                    OutlinedButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "Logout",
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    // Delete account button
                    Button(
                        onClick = { showDeleteAccountDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = !isDeleting
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Delete Account",
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    
                    // Show error if delete failed
                    deleteError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
    
    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.signOut()
                        showLogoutDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete account confirmation dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account") },
            text = { 
                Text("Are you sure you want to delete your account? This action cannot be undone. All your data will be permanently deleted.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        showDeleteConfirmDialog = true
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Final confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Final Confirmation") },
            text = { 
                Text("This is your last chance. Are you absolutely sure you want to permanently delete your account and all associated data?") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        isDeleting = true
                        deleteError = null
                        
                        // Delete account
                        authViewModel.deleteAccount { success: Boolean, error: String? ->
                            isDeleting = false
                            if (success) {
                                // Account deleted successfully, navigate back
                                onNavigateBack()
                            } else {
                                // Show error
                                deleteError = error ?: "Failed to delete account"
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Yes, Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    initialScreen: Screen? = null,
    onScreenChanged: (Screen) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel()
    val timerViewModel: TimerViewModel = viewModel()
    var currentScreen by remember { 
        mutableStateOf<Screen>(initialScreen ?: Screen.Timer) 
    }
    
    // Sync with parent state
    LaunchedEffect(currentScreen) {
        onScreenChanged(currentScreen)
    }
    
    // Update if initialScreen changes from outside (e.g., from blocking overlay)
    LaunchedEffect(initialScreen) {
        initialScreen?.let { currentScreen = it }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // App Bar with settings icon (only show on timer screen)
        if (currentScreen is Screen.Timer) {
            TopAppBar(
                title = { Text("Nurtra") },
                actions = {
                    IconButton(onClick = { currentScreen = Screen.Settings }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
        
        // Show appropriate screen
        when (currentScreen) {
            is Screen.Timer -> {
                TimerScreen(
                    timerViewModel = timerViewModel,
                    authViewModel = viewModel,
                    onNavigateToCamera = { currentScreen = Screen.Camera }
                )
            }
            is Screen.Camera -> {
                CameraScreen(
                    timerViewModel = timerViewModel,
                    onNavigateBack = { 
                        // Deactivate blocking immediately when navigating back
                        // This ensures blocking is only active while in CameraScreen
                        val blockingManager = AppBlockingManager.getInstance(context)
                        blockingManager.deactivateBlocking()
                        Log.d("MainActivity", "App blocking deactivated when navigating back")
                        currentScreen = Screen.Timer 
                    },
                    onOvercome = {
                        // Timer continues running - user overcame the craving!
                        // Deactivate blocking IMMEDIATELY so user can access apps normally
                        val blockingManager = AppBlockingManager.getInstance(context)
                        blockingManager.deactivateBlocking()
                        Log.d("MainActivity", "App blocking deactivated - user overcame craving, apps now accessible")
                    
                        // Increment overcomeCount in Firestore
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val firestoreManager = FirestoreManager()
                                firestoreManager.incrementOvercomeCount(currentUser.uid).onSuccess {
                                    // Refresh user data in AuthViewModel to update UI
                                    viewModel.refreshNurtraUser()
                                    Log.d("MainActivity", "Overcome count incremented to: $it")
                                }.onFailure { error ->
                                    Log.e("MainActivity", "Failed to increment overcome count: ${error.message}", error)
                                }
                            }
                        }
                        // Navigate back to timer screen - blocking is already deactivated
                        currentScreen = Screen.Timer
                    },
                    onBinged = {
                        // Save binge-free period before stopping the timer
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        val timerState = timerViewModel.uiState.value
                        val startTime = timerState.startTime
                        
                        if (currentUser != null && startTime != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val firestoreManager = FirestoreManager()
                                val now = Timestamp.now()
                                val durationMillis = now.toDate().time - startTime.toDate().time
                                
                                val bingeFreePeriod = BingeFreePeriod(
                                    id = "", // Will be set by FirestoreManager
                                    startTime = startTime,
                                    endTime = now,
                                    duration = durationMillis,
                                    createdAt = now
                                )
                                
                                firestoreManager.saveBingeFreePeriod(currentUser.uid, bingeFreePeriod).onSuccess {
                                    Log.d("MainActivity", "Binge-free period saved: ${durationMillis}ms")
                                    // Refresh the binge-free periods list
                                    timerViewModel.loadBingeFreePeriods()
                                }.onFailure { error ->
                                    Log.e("MainActivity", "Failed to save binge-free period: ${error.message}", error)
                                }
                            }
                        }
                        
                        // Stop the timer when user binged - they need to start over
                        // Deactivate blocking immediately since session is ending
                        val blockingManager = AppBlockingManager.getInstance(context)
                        blockingManager.deactivateBlocking()
                        Log.d("MainActivity", "App blocking deactivated - user binged")
                        timerViewModel.stopStopwatch()
                        currentScreen = Screen.Survey
                    }
                )
            }
            is Screen.Survey -> {
                BingeSurveyScreen(
                    onComplete = { currentScreen = Screen.Timer }
                )
            }
            is Screen.Settings -> {
                SettingsScreen(
                    authViewModel = viewModel,
                    onNavigateBack = { currentScreen = Screen.Timer }
                )
            }
        }
    }
}

// Format time display for stopwatch (MM:SS.mm)
fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val milliseconds = (millis % 1000) / 10 // Show centiseconds
    return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, milliseconds)
}

// Format duration for binge-free periods display
fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return when {
        hours > 0 -> String.format(Locale.getDefault(), "%dh %dm %ds", hours, minutes, seconds)
        minutes > 0 -> String.format(Locale.getDefault(), "%dm %ds", minutes, seconds)
        else -> String.format(Locale.getDefault(), "%ds", seconds)
    }
}

// Format date for display
fun formatDate(timestamp: Timestamp): String {
    val date = timestamp.toDate()
    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    return formatter.format(date)
}

@Composable
fun BingeFreePeriodItem(period: BingeFreePeriod) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = formatDuration(period.duration),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            period.endTime?.let {
                Text(
                    text = formatDate(it),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun TimerScreen(
    timerViewModel: TimerViewModel,
    authViewModel: AuthViewModel,
    onNavigateToCamera: () -> Unit
) {
    val context = LocalContext.current
    val uiState by timerViewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    val overcomeCount = authUiState.nurtraUser?.overcomeCount ?: 0
    
    // Show loading indicator until both Firebase fetches are complete
    val isDataReady = uiState.isInitialLoadComplete && authUiState.isInitialLoadComplete
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (!isDataReady) {
            // Show loading indicator while waiting for Firebase data
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading...",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            // Show actual content once data is loaded
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
            // Overcome Count Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Times Overcome",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "$overcomeCount",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Binge-Free Periods Log
            if (uiState.latestBingeFreePeriods.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Recent Binge-Free Periods",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        uiState.latestBingeFreePeriods.forEach { period ->
                            BingeFreePeriodItem(period)
                            if (period != uiState.latestBingeFreePeriods.last()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            }
            
            // Timer Display
            Text(
                text = formatTime(uiState.elapsedTimeInMillis),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            // Single Button
            Button(
                onClick = {
                    if (uiState.isTimerRunning) {
                        // Timer is running, activate blocking immediately and navigate to camera
                        // Blocking will remain active while user is in CameraScreen
                        val blockingManager = AppBlockingManager.getInstance(context)
                        blockingManager.activateBlocking()
                        Log.d("TimerScreen", "App blocking activated when craving button clicked")
                        onNavigateToCamera()
                    } else {
                        // Start the timer
                        timerViewModel.startStopwatch()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isTimerRunning) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color(0xFF4CAF50) // Vibrant green to make it stand out
                    },
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (uiState.isTimerRunning) "I'm Craving!" else "Start Urge timer",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            }
        }
    }
}


// Survey emotion options
val surveyEmotions = listOf(
    "Guilty",
    "Ashamed",
    "Anxious",
    "Sad",
    "Numb",
    "Stressed",
    "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BingeSurveyScreen(
    onComplete: () -> Unit
) {
    var selectedEmotions by remember { mutableStateOf(setOf<String>()) }
    var showOtherInput by remember { mutableStateOf(false) }
    var otherInput by remember { mutableStateOf("") }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar with back button
            TopAppBar(
                title = { Text("Binge Survey") },
                navigationIcon = {
                    IconButton(onClick = onComplete) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
            
            // Progress bar
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Question
                Text(
                    text = "How do you feel?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                // Emotion options
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    surveyEmotions.forEach { emotion ->
                        val isSelected = selectedEmotions.contains(emotion)
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (emotion == "Other") {
                                    showOtherInput = !isSelected
                                    if (!isSelected) {
                                        selectedEmotions = selectedEmotions + emotion
                                    } else {
                                        selectedEmotions = selectedEmotions - emotion
                                        otherInput = ""
                                    }
                                } else {
                                    if (isSelected) {
                                        selectedEmotions = selectedEmotions - emotion
                                    } else {
                                        selectedEmotions = selectedEmotions + emotion
                                    }
                                }
                            },
                            label = { Text(emotion) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White
                                    )
                                }
                            } else null
                        )
                    }
                }
                
                // Other input field (if "Other" is selected)
                if (showOtherInput) {
                    OutlinedTextField(
                        value = otherInput,
                        onValueChange = { otherInput = it },
                        label = { Text("Please specify") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                // Submit button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            // TODO: Save survey responses to Firestore
                            // For now, just navigate back
                            onComplete()
                        },
                        enabled = selectedEmotions.isNotEmpty() && !(selectedEmotions.contains("Other") && otherInput.isBlank())
                    ) {
                        Text("Submit", modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}