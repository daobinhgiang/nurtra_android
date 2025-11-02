package com.example.nurtra_android.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Overlay shown when a blocked app is attempted
 * Displays motivational message and option to return to Nurtra
 */
@Composable
fun BlockingOverlay(
    blockedAppName: String?,
    onDismiss: () -> Unit
) {
    // Rotate through motivational messages
    val motivationalMessages = listOf(
        "You're stronger than this craving!",
        "Stay focused. You've got this.",
        "Every moment you resist is a victory.",
        "Your future self will thank you.",
        "This feeling will pass. Stay strong.",
        "You are in control.",
        "Progress, not perfection.",
        "Take a deep breath. You can overcome this.",
        "Remember why you started.",
        "You're doing great. Keep going!"
    )
    
    var currentMessageIndex by remember { mutableStateOf((0 until motivationalMessages.size).random()) }
    
    // Rotate messages every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            currentMessageIndex = (currentMessageIndex + 1) % motivationalMessages.size
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = "App Blocked",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = "App Blocked",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // App name (if provided)
            blockedAppName?.let {
                Text(
                    text = "\"$it\" is blocked during cravings",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Motivational message card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = motivationalMessages[currentMessageIndex],
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    lineHeight = 28.sp
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Return to Nurtra button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Return to Nurtra",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Explanation text
            Text(
                text = "This app is blocked while you're working through a craving. You can access it again after you've overcome the craving or ended the session.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                lineHeight = 20.sp
            )
        }
    }
}


