package com.example.nurtra_android

import android.Manifest
import android.content.Context
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
import com.example.nurtra_android.data.NotificationHelper
import com.example.nurtra_android.data.FCMTokenManager
import com.example.nurtra_android.data.FirestoreManager
import com.example.nurtra_android.theme.NurtraTheme
import com.google.firebase.auth.FirebaseAuth
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create notification channels
        val notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannels()
        
        // Log FCM token on app startup for debugging
        logFCMToken()
        
        setContent {
            NurtraTheme {
                AppContent()
            }
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
fun AppContent() {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val authUiState by authViewModel.uiState.collectAsState()
    
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var showSignUp by remember { mutableStateOf(false) }
    var hasRequestedNotificationPermission by remember { mutableStateOf(false) }
    
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
                    HomeScreen()
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

sealed class Screen {
    object Timer : Screen()
    object Camera : Screen()
    object Survey : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val viewModel: AuthViewModel = viewModel()
    val timerViewModel: TimerViewModel = viewModel()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Timer) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // App Bar with logout (only show on timer screen)
        if (currentScreen is Screen.Timer) {
            TopAppBar(
                title = { Text("Nurtra") },
                actions = {
                    TextButton(onClick = { showLogoutDialog = true }) {
                        Text("Logout", color = MaterialTheme.colorScheme.onSurface)
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
                    onNavigateBack = { currentScreen = Screen.Timer },
                    onOvercome = {
                        // Timer continues running - user overcame the craving!
                    
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
                        currentScreen = Screen.Timer
                    },
                    onBinged = {
                        // Stop the timer when user binged - they need to start over
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
                            viewModel.signOut()
                            showLogoutDialog = false
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

@Composable
fun TimerScreen(
    timerViewModel: TimerViewModel,
    authViewModel: AuthViewModel,
    onNavigateToCamera: () -> Unit
) {
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
                        // Timer is running, navigate to camera
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