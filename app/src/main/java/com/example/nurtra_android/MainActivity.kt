package com.example.nurtra_android

import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nurtra_android.auth.AuthViewModel
import com.example.nurtra_android.auth.LoginScreen
import com.example.nurtra_android.auth.SignUpScreen
import com.example.nurtra_android.theme.NurtraTheme
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val viewModel: AuthViewModel = viewModel()
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // App Bar with logout
        TopAppBar(
            title = { Text("Nurtra") },
            actions = {
                TextButton(onClick = { showLogoutDialog = true }) {
                    Text("Logout", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
        
        // Timer Screen
        TimerScreen()
        
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

@Composable
fun TimerScreen() {
    var timeLeftInMillis by remember { mutableStateOf(3600000L) } // 1 hour in milliseconds
    var isTimerRunning by remember { mutableStateOf(false) }
    var countDownTimer by remember { mutableStateOf<CountDownTimer?>(null) }

    // Format time display
    fun formatTime(millis: Long): String {
        val hours = (millis / 1000) / 3600
        val minutes = ((millis / 1000) % 3600) / 60
        val seconds = (millis / 1000) % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun startTimer() {
        if (!isTimerRunning) {
            countDownTimer?.cancel()
            countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeLeftInMillis = millisUntilFinished
                }

                override fun onFinish() {
                    isTimerRunning = false
                    timeLeftInMillis = 0
                    countDownTimer = null
                }
            }.start()
            isTimerRunning = true
        }
    }

    fun pauseTimer() {
        if (isTimerRunning) {
            countDownTimer?.cancel()
            countDownTimer = null
            isTimerRunning = false
        }
    }

    fun resetTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        isTimerRunning = false
        timeLeftInMillis = 3600000L // Reset to 1 hour
    }

    // Clean up timer when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            countDownTimer?.cancel()
        }
    }

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
                text = formatTime(timeLeftInMillis),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            // Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start Button
                Button(
                    onClick = { startTimer() },
                    enabled = !isTimerRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start")
                }

                // Pause Button
                Button(
                    onClick = { pauseTimer() },
                    enabled = isTimerRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pause")
                }

                // Reset Button
                OutlinedButton(
                    onClick = { resetTimer() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
            }
        }
    }
}