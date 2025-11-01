package com.example.nurtra_android.auth

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nurtra_android.auth.GoogleSignInHelper.signInWithGoogle
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var addAccountIntent by remember { mutableStateOf<android.content.Intent?>(null) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Launcher for adding Google account
    val addAccountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // After adding account, show message to try again
        showAddAccountDialog = false
        errorMessage = "Account added! Please click 'Sign in with Google' again to continue."
    }
    
    LaunchedEffect(viewModel.uiState.value.errorMessage) {
        viewModel.uiState.value.errorMessage?.let {
            errorMessage = it
            viewModel.clearError()
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome Back",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Sign in to continue",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    errorMessage = null
                },
                label = { Text("Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            
            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    errorMessage = null
                },
                label = { Text("Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )
            
            // Error message
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            // Sign in button
            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        viewModel.signInWithEmail(email, password) { success, error ->
                            if (success) {
                                onLoginSuccess()
                            } else {
                                errorMessage = error ?: "Sign in failed"
                            }
                        }
                    } else {
                        errorMessage = "Please fill in all fields"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !viewModel.uiState.value.isLoading
            ) {
                if (viewModel.uiState.value.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Sign In")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Google sign in button
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        // Get Activity context for Credential Manager
                        val activity = context as? ComponentActivity
                        if (activity == null) {
                            errorMessage = "Unable to access activity context"
                            return@launch
                        }
                        
                        when (val result = signInWithGoogle(activity)) {
                            is GoogleSignInResult.Success -> {
                                viewModel.signInWithGoogleCredential(result.credential) { success, error ->
                                    if (success) {
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = error ?: "Google sign in failed"
                                    }
                                }
                            }
                            is GoogleSignInResult.Error -> {
                                errorMessage = result.message
                            }
                            is GoogleSignInResult.NoAccountsFound -> {
                                // Show dialog to add account
                                addAccountIntent = result.addAccountIntent
                                showAddAccountDialog = true
                            }
                            is GoogleSignInResult.Cancelled -> {
                                // User cancelled, don't show error
                                errorMessage = null
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !viewModel.uiState.value.isLoading
            ) {
                Text("Sign in with Google")
            }
            
            // Add account dialog
            if (showAddAccountDialog) {
                addAccountIntent?.let { intent ->
                    AlertDialog(
                        onDismissRequest = { showAddAccountDialog = false },
                        title = { Text("No Google Account Found") },
                        text = { Text("You need a Google account to sign in. Would you like to add one now?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    addAccountLauncher.launch(intent)
                                }
                            ) {
                                Text("Add Account")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showAddAccountDialog = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sign up link
            Row {
                Text(
                    text = "Don't have an account? ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateToSignUp) {
                    Text(
                        text = "Sign Up",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

