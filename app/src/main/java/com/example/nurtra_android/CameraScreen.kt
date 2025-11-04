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
    var audioUrls by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    
    // Fetch personalized motivational quotes and audio URLs from Firebase
    LaunchedEffect(Unit) {
        Log.d("CameraScreen", "=== Initializing CameraScreen ===")
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            Log.d("CameraScreen", "User authenticated: ${currentUser.uid}")
            val firestoreManager = FirestoreManager()
            
            // Fetch quotes
            Log.d("CameraScreen", "Fetching motivational quotes from Firestore...")
            val quotesResult = firestoreManager.getUserMotivationalQuotes(currentUser.uid)
            quotesResult.onSuccess { quotes ->
                if (quotes.isNotEmpty()) {
                    motivationalQuotes = quotes
                    Log.d("CameraScreen", "✓ Loaded ${quotes.size} personalized quotes")
                    quotes.forEachIndexed { index, quote ->
                        Log.d("CameraScreen", "  Quote ${index + 1}: ${quote.take(60)}${if (quote.length > 60) "..." else ""}")
                    }
                } else {
                    Log.d("CameraScreen", "⚠ No personalized quotes found, using ${defaultMotivationalQuotes.size} default quotes")
                }
            }.onFailure { error ->
                Log.e("CameraScreen", "✗ Failed to load quotes: ${error.message}", error)
                // Continue with default quotes
            }
            
            // Fetch audio URLs
            Log.d("CameraScreen", "Fetching audio URLs from Firestore...")
            val audioUrlsResult = firestoreManager.getUserMotivationalQuoteAudioUrls(currentUser.uid)
            audioUrlsResult.onSuccess { urls ->
                if (urls.isNotEmpty()) {
                    audioUrls = urls
                    Log.d("CameraScreen", "✓ Loaded ${urls.size} audio URLs")
                    urls.forEach { (quoteId, url) ->
                        Log.d("CameraScreen", "  Quote $quoteId: ${url.take(60)}...")
                    }
                } else {
                    Log.d("CameraScreen", "⚠ No audio URLs found - quotes will display without audio")
                }
            }.onFailure { error ->
                Log.e("CameraScreen", "✗ Failed to load audio URLs: ${error.message}", error)
            }
        } else {
            Log.w("CameraScreen", "⚠ No authenticated user - using default quotes only")
        }
        Log.d("CameraScreen", "=== CameraScreen initialization complete ===")
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
    
    // Deactivate blocking and cleanup MediaPlayer when leaving CameraScreen
    DisposableEffect(Unit) {
        onDispose {
            // Stop and release MediaPlayer
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            
            val blockingManager = AppBlockingManager.getInstance(context)
            blockingManager.deactivateBlocking()
            Log.d("CameraScreen", "App blocking deactivated and MediaPlayer cleaned up")
        }
    }
    
    // Play audio and rotate quotes based on audio duration
    LaunchedEffect(motivationalQuotes.size, currentQuoteIndex, audioUrls) {
        if (motivationalQuotes.isEmpty()) {
            Log.d("CameraScreen", "No quotes available, skipping rotation")
            return@LaunchedEffect
        }
        
        val quoteId = (currentQuoteIndex + 1).toString()
        val currentQuote = motivationalQuotes[currentQuoteIndex]
        val audioUrl = audioUrls[quoteId]
        
        Log.d("CameraScreen", "--- Displaying quote $quoteId (${currentQuoteIndex + 1}/${motivationalQuotes.size}) ---")
        Log.d("CameraScreen", "Quote text: ${currentQuote.take(80)}${if (currentQuote.length > 80) "..." else ""}")
        
        if (audioUrl != null && !isPlayingAudio) {
            // Play audio for current quote
            Log.d("CameraScreen", "Audio URL found for quote $quoteId: ${audioUrl.take(60)}...")
            try {
                isPlayingAudio = true
                val playbackStartTime = System.currentTimeMillis()
                Log.d("CameraScreen", "Initializing MediaPlayer for quote $quoteId...")
                
                val player = android.media.MediaPlayer().apply {
                    setDataSource(audioUrl)
                    setOnPreparedListener { mp ->
                        val prepareTime = System.currentTimeMillis() - playbackStartTime
                        Log.d("CameraScreen", "MediaPlayer prepared for quote $quoteId in ${prepareTime}ms")
                        val duration = mp.duration
                        Log.d("CameraScreen", "Audio duration: ${duration}ms (${duration / 1000.0}s)")
                        mp.start()
                        Log.d("CameraScreen", "▶ Started playing audio for quote $quoteId")
                    }
                    setOnCompletionListener { mp ->
                        val playbackTime = System.currentTimeMillis() - playbackStartTime
                        Log.d("CameraScreen", "✓ Audio completed for quote $quoteId (total playback time: ${playbackTime}ms)")
                        mp.release()
                        mediaPlayer = null
                        isPlayingAudio = false
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e("CameraScreen", "✗ MediaPlayer error for quote $quoteId: what=$what, extra=$extra")
                        mp.release()
                        mediaPlayer = null
                        isPlayingAudio = false
                        true
                    }
                    prepareAsync()
                }
                mediaPlayer = player
                
                // Wait for audio to complete (with timeout)
                var waitTime = 0L
                val maxWaitTime = 30000L // 30 seconds max
                Log.d("CameraScreen", "Waiting for audio to complete (max: ${maxWaitTime}ms)...")
                while (isPlayingAudio && waitTime < maxWaitTime) {
                    delay(100)
                    waitTime += 100
                    if (waitTime % 2000 == 0L) {
                        Log.d("CameraScreen", "Still waiting for audio... (${waitTime}ms)")
                    }
                }
                
                if (waitTime >= maxWaitTime) {
                    Log.w("CameraScreen", "⚠ Audio playback timeout reached (${maxWaitTime}ms)")
                }
                
                // Wait additional 1 second after audio completes
                Log.d("CameraScreen", "Waiting 1 second before rotating to next quote...")
                delay(1000)
                
            } catch (e: Exception) {
                Log.e("CameraScreen", "✗ Error playing audio for quote $quoteId: ${e.message}", e)
                e.printStackTrace()
                mediaPlayer?.release()
                mediaPlayer = null
                isPlayingAudio = false
                // Fallback to 3-second delay
                Log.d("CameraScreen", "Falling back to 3-second delay")
                delay(3000)
            }
        } else {
            // No audio available, use default 3-second delay
            if (audioUrl == null) {
                Log.d("CameraScreen", "No audio URL for quote $quoteId - using 3-second delay")
            } else {
                Log.d("CameraScreen", "Audio already playing - skipping new playback")
            }
            delay(3000)
        }
        
        // Move to next quote
        val nextIndex = (currentQuoteIndex + 1) % motivationalQuotes.size
        Log.d("CameraScreen", "Rotating from quote ${currentQuoteIndex + 1} to quote ${nextIndex + 1}")
        currentQuoteIndex = nextIndex
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
                text = formatTimeWithMillis(uiState.elapsedTimeInMillis),
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
