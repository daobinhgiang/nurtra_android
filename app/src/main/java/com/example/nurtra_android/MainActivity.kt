package com.example.nurtra_android

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nurtra_android.auth.AuthViewModel
import com.example.nurtra_android.auth.LoginScreen
import com.example.nurtra_android.auth.SignUpScreen
import com.example.nurtra_android.theme.NurtraTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NurtraTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val auth = FirebaseAuth.getInstance()
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var showSignUp by remember { mutableStateOf(false) }
    
    // Listen to auth state changes
    LaunchedEffect(Unit) {
        auth.addAuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
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
            // Authenticated - show main app
            HomeScreen()
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
                    onNavigateToCamera = { currentScreen = Screen.Camera }
                )
            }
            is Screen.Camera -> {
                CameraScreen(
                    timerViewModel = timerViewModel,
                    onNavigateBack = { currentScreen = Screen.Timer },
                    onOvercome = { currentScreen = Screen.Timer },
                    onBinged = {
                        timerViewModel.pauseStopwatch()
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
    onNavigateToCamera: () -> Unit
) {
    val uiState by timerViewModel.uiState.collectAsState()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                        MaterialTheme.colorScheme.secondary
                    }
                )
            ) {
                Text(
                    text = if (uiState.isTimerRunning) "I'm Craving!" else "Start Urge timer",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

// Motivational quotes list
val motivationalQuotes = listOf(
    "You are stronger than your cravings.",
    "Every moment you resist is a victory.",
    "Take a deep breath. You've got this.",
    "Progress, not perfection.",
    "Your future self will thank you.",
    "This feeling will pass. Stay strong.",
    "You are capable of amazing things.",
    "One moment at a time.",
    "You've overcome challenges before. You can do it again.",
    "Be kind to yourself. You're doing great."
)

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener(
            {
                continuation.resume(future.get())
            },
            ContextCompat.getMainExecutor(this)
        )
    }
}

@Composable
fun CameraScreen(
    timerViewModel: TimerViewModel,
    onNavigateBack: () -> Unit,
    onOvercome: () -> Unit,
    onBinged: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by timerViewModel.uiState.collectAsState()
    
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        hasCameraPermission = granted
    }
    
    var currentQuoteIndex by remember { mutableStateOf(0) }
    
    // Rotate quotes every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            currentQuoteIndex = (currentQuoteIndex + 1) % motivationalQuotes.size
        }
    }
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            // Camera Preview
            CameraPreviewView(
                modifier = Modifier.fillMaxSize(),
                lifecycleOwner = lifecycleOwner
            )
        } else {
            // Show message if permission not granted
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera permission is required",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            }
        }
        
        // Back button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        
        // Timer overlay at top
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    MaterialTheme.shapes.medium
                ),
            color = Color.Transparent
        ) {
            Text(
                text = formatTime(uiState.elapsedTimeInMillis),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Green,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
        
        // Motivational quote overlay at bottom
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 32.dp, end = 32.dp, bottom = 160.dp)
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    MaterialTheme.shapes.large
                ),
            color = Color.Transparent
        ) {
            Text(
                text = motivationalQuotes[currentQuoteIndex],
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(20.dp),
                lineHeight = 24.sp
            )
        }
        
        // Action buttons at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // "I just binged" button (red)
            Button(
                onClick = onBinged,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC3545) // Red
                )
            ) {
                Text(
                    text = "I just binged",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // "I overcame it" button (blue)
            Button(
                onClick = onOvercome,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007BFF) // Blue
                )
            ) {
                Text(
                    text = "I overcame it",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraProviderState = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    
    LaunchedEffect(previewView, lifecycleOwner) {
        try {
            val cameraProvider = context.getCameraProvider()
            cameraProviderState.value = cameraProvider
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            // Handle camera binding error
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // Clean up camera when composable is disposed
            try {
                cameraProviderState.value?.unbindAll()
                cameraProviderState.value = null
            } catch (e: Exception) {
                // Handle cleanup error
            }
        }
    }
    
    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
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
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
            
            // Progress bar
            LinearProgressIndicator(
                progress = 1f,
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