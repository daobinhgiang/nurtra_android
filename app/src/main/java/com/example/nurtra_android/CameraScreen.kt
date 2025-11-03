package com.example.nurtra_android

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.nurtra_android.blocking.AccessibilityServiceHelper
import com.example.nurtra_android.blocking.AccessibilityServicePermissionDialog
import com.example.nurtra_android.blocking.AppBlockingManager
import com.example.nurtra_android.data.FirestoreManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Default motivational quotes (fallback if personalized quotes are not available)
val defaultMotivationalQuotes = listOf(
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
    var motivationalQuotes by remember { mutableStateOf(defaultMotivationalQuotes) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    
    // Fetch personalized motivational quotes from Firebase
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val firestoreManager = FirestoreManager()
            val quotesResult = firestoreManager.getUserMotivationalQuotes(currentUser.uid)
            
            quotesResult.onSuccess { quotes ->
                if (quotes.isNotEmpty()) {
                    motivationalQuotes = quotes
                    Log.d("CameraScreen", "Loaded ${quotes.size} personalized quotes")
                } else {
                    Log.d("CameraScreen", "No personalized quotes found, using defaults")
                }
            }.onFailure { error ->
                Log.e("CameraScreen", "Failed to load quotes: ${error.message}", error)
                // Continue with default quotes
            }
        }
    }
    
    // Function to check and activate blocking
    val checkAndActivateBlocking: () -> Unit = {
        val isEnabled = AccessibilityServiceHelper.isAccessibilityServiceEnabled(context)
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        if (!isEnabled && !showAccessibilityDialog) {
            // Show dialog if not already showing
            showAccessibilityDialog = true
        } else if (isEnabled && currentUser != null) {
            // Activate blocking if service is enabled
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val blockingManager = AppBlockingManager.getInstance(context)
                    blockingManager.activateBlocking()
                    Log.d("CameraScreen", "App blocking activated")
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Error activating app blocking: ${e.message}", e)
                }
            }
        }
    }
    
    // Check when screen first appears
    LaunchedEffect(Unit) {
        delay(300) // Small delay to let screen render first
        checkAndActivateBlocking()
    }
    
    // Re-check when activity resumes (e.g., user returns from settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkAndActivateBlocking()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Deactivate blocking when leaving CameraScreen
    DisposableEffect(Unit) {
        onDispose {
            val blockingManager = AppBlockingManager.getInstance(context)
            blockingManager.deactivateBlocking()
            Log.d("CameraScreen", "App blocking deactivated")
        }
    }
    
    // Rotate quotes every 3 seconds
    LaunchedEffect(motivationalQuotes.size) {
        while (true) {
            delay(3000)
            currentQuoteIndex = (currentQuoteIndex + 1) % motivationalQuotes.size
        }
    }
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // Show accessibility service permission dialog
    if (showAccessibilityDialog) {
        AccessibilityServicePermissionDialog(
            onDismiss = { 
                showAccessibilityDialog = false
                // Re-check after dismissing (in case user enabled it manually)
                checkAndActivateBlocking()
            },
            onOpenSettings = {
                AccessibilityServiceHelper.openAccessibilitySettings(context)
                // Keep dialog hidden - will check again on resume
                showAccessibilityDialog = false
            },
            serviceName = AccessibilityServiceHelper.getServiceName(context)
        )
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
