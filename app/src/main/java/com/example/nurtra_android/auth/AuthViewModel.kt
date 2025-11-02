package com.example.nurtra_android.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.AuthCredential
import com.example.nurtra_android.data.FirestoreManager
import com.example.nurtra_android.data.FCMTokenManager
import com.example.nurtra_android.data.NurtraUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val nurtraUser: NurtraUser? = null,
    val errorMessage: String? = null,
    val isInitialLoadComplete: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val TAG = "AuthViewModel"
    private val auth = FirebaseAuth.getInstance()
    private val firestoreManager = FirestoreManager()
    private val fcmTokenManager = FCMTokenManager()
    
    private val _uiState = MutableStateFlow(AuthUiState(user = auth.currentUser))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        // Listen to auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            _uiState.value = _uiState.value.copy(user = currentUser)
            
            // Load Firestore user data if authenticated
            if (currentUser != null) {
                loadNurtraUser(currentUser.uid)
            } else {
                _uiState.value = _uiState.value.copy(
                    nurtraUser = null,
                    isInitialLoadComplete = true
                )
            }
        }
        
        // Handle initial state if user is already authenticated
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadNurtraUser(currentUser.uid)
        } else {
            _uiState.value = _uiState.value.copy(isInitialLoadComplete = true)
        }
    }
    
    private fun loadNurtraUser(userId: String) {
        viewModelScope.launch {
            val result = firestoreManager.getUser(userId)
            result.onSuccess { nurtraUser ->
                _uiState.value = _uiState.value.copy(
                    nurtraUser = nurtraUser,
                    isInitialLoadComplete = true
                )
            }.onFailure { error ->
                // Log error but don't fail - user may not have Firestore document yet
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Could not load user profile: ${error.message}",
                    isInitialLoadComplete = true
                )
            }
        }
    }
    
    fun signInWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                
                // Create or update Firestore user document
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    firestoreManager.createOrUpdateUser(
                        userId = firebaseUser.uid,
                        email = firebaseUser.email ?: email,
                        name = firebaseUser.displayName
                    )
                    
                    // Get and save FCM token
                    val fcmToken = fcmTokenManager.getToken()
                    if (fcmToken != null) {
                        Log.d(TAG, "Saving FCM token to Firestore after sign-in")
                        firestoreManager.updateFCMToken(firebaseUser.uid, fcmToken)
                    } else {
                        Log.w(TAG, "FCM token not available during sign-in")
                    }
                }
                
                _uiState.value = _uiState.value.copy(isLoading = false)
                onResult(true, null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
                onResult(false, e.message)
            }
        }
    }
    
    fun signUpWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                
                // Create Firestore user document
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    firestoreManager.createOrUpdateUser(
                        userId = firebaseUser.uid,
                        email = firebaseUser.email ?: email,
                        name = firebaseUser.displayName
                    )
                    
                    // Get and save FCM token
                    val fcmToken = fcmTokenManager.getToken()
                    if (fcmToken != null) {
                        Log.d(TAG, "Saving FCM token to Firestore after sign-up")
                        firestoreManager.updateFCMToken(firebaseUser.uid, fcmToken)
                    } else {
                        Log.w(TAG, "FCM token not available during sign-up")
                    }
                }
                
                _uiState.value = _uiState.value.copy(isLoading = false)
                onResult(true, null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
                onResult(false, e.message)
            }
        }
    }
    
    fun signInWithGoogleCredential(credential: AuthCredential, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                val authResult = auth.signInWithCredential(credential).await()
                
                // Create or update Firestore user document
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    firestoreManager.createOrUpdateUser(
                        userId = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = firebaseUser.displayName
                    )
                    
                    // Get and save FCM token
                    val fcmToken = fcmTokenManager.getToken()
                    if (fcmToken != null) {
                        Log.d(TAG, "Saving FCM token to Firestore after Google sign-in")
                        firestoreManager.updateFCMToken(firebaseUser.uid, fcmToken)
                    } else {
                        Log.w(TAG, "FCM token not available during Google sign-in")
                    }
                }
                
                _uiState.value = _uiState.value.copy(isLoading = false)
                onResult(true, null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
                onResult(false, e.message)
            }
        }
    }
    
    fun signOut() {
        auth.signOut()
        _uiState.value = AuthUiState()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Refreshes the NurtraUser data from Firestore
     * This should be called after operations that update user data in Firestore
     */
    fun refreshNurtraUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                val result = firestoreManager.getUser(currentUser.uid)
                result.onSuccess { nurtraUser ->
                    _uiState.value = _uiState.value.copy(nurtraUser = nurtraUser)
                }.onFailure { error ->
                    // Log error but don't fail - user may not have Firestore document yet
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Could not load user profile: ${error.message}"
                    )
                }
            }
        }
    }
}

