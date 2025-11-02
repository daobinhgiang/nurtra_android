package com.example.nurtra_android

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nurtra_android.data.FirestoreManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

data class TimerUiState(
    val elapsedTimeInMillis: Long = 0L,
    val isTimerRunning: Boolean = false,
    val startTime: Timestamp? = null,
    val isInitialLoadComplete: Boolean = false
)

class TimerViewModel : ViewModel() {
    private val TAG = "TimerViewModel"
    private val firestoreManager = FirestoreManager()
    private val auth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()
    
    @Volatile
    private var timerUpdateJob: kotlinx.coroutines.Job? = null

    init {
        // Load timer state from Firebase on initialization
        loadTimerState()
    }

    /**
     * Loads the timer state from Firebase and starts UI updates if timer is running
     */
    private fun loadTimerState() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: run {
                // No user, mark as complete with default state
                _uiState.value = _uiState.value.copy(isInitialLoadComplete = true)
                return@launch
            }
            
            firestoreManager.getUser(userId).onSuccess { user ->
                val timerStartTime = user?.timerStartTime
                if (user?.timerIsRunning == true && timerStartTime != null) {
                    // Timer is running in Firebase, start calculating elapsed time
                    _uiState.value = _uiState.value.copy(
                        isTimerRunning = true,
                        startTime = timerStartTime,
                        isInitialLoadComplete = true
                    )
                    startTimerUpdates(timerStartTime)
                    Log.d(TAG, "Loaded running timer from Firebase, start time: $timerStartTime")
                } else {
                    // Timer is not running
                    _uiState.value = _uiState.value.copy(
                        isTimerRunning = false,
                        startTime = null,
                        elapsedTimeInMillis = 0L,
                        isInitialLoadComplete = true
                    )
                    Log.d(TAG, "No active timer in Firebase")
                }
            }.onFailure { error ->
                Log.e(TAG, "Error loading timer state: ${error.message}", error)
                // Even on error, mark as complete to avoid infinite loading
                _uiState.value = _uiState.value.copy(isInitialLoadComplete = true)
            }
        }
    }

    /**
     * Starts the timer by saving start time to Firebase
     */
    fun startStopwatch() {
        if (_uiState.value.isTimerRunning) return
        
        val userId = auth.currentUser?.uid ?: run {
            Log.e(TAG, "Cannot start timer: user not authenticated")
            return
        }
        
        viewModelScope.launch {
            firestoreManager.startTimer(userId).onSuccess { startTime ->
                _uiState.value = _uiState.value.copy(
                    isTimerRunning = true,
                    startTime = startTime,
                    elapsedTimeInMillis = 0L
                )
                startTimerUpdates(startTime)
                Log.d(TAG, "Timer started at: $startTime")
            }.onFailure { error ->
                Log.e(TAG, "Error starting timer: ${error.message}", error)
            }
        }
    }

    /**
     * Stops the timer by updating Firebase
     */
    fun stopStopwatch() {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            firestoreManager.stopTimer(userId).onSuccess {
                timerUpdateJob?.cancel()
                _uiState.value = _uiState.value.copy(isTimerRunning = false)
                Log.d(TAG, "Timer stopped")
            }.onFailure { error ->
                Log.e(TAG, "Error stopping timer: ${error.message}", error)
            }
        }
    }

    /**
     * Pauses the timer (same as stop for Firebase-based timer)
     */
    fun pauseStopwatch() {
        stopStopwatch()
    }

    /**
     * Resets the timer by clearing it in Firebase
     */
    fun resetStopwatch() {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            firestoreManager.resetTimer(userId).onSuccess {
                timerUpdateJob?.cancel()
                _uiState.value = TimerUiState()
                Log.d(TAG, "Timer reset")
            }.onFailure { error ->
                Log.e(TAG, "Error resetting timer: ${error.message}", error)
            }
        }
    }

    /**
     * Starts the coroutine that updates the UI with calculated elapsed time
     */
    private fun startTimerUpdates(startTime: Timestamp) {
        // Cancel any existing update job
        timerUpdateJob?.cancel()
        
        timerUpdateJob = viewModelScope.launch {
            while (true) {
                // Calculate elapsed time based on Firebase start time
                val now = Date()
                val startDate = startTime.toDate()
                val elapsedMillis = now.time - startDate.time
                
                _uiState.value = _uiState.value.copy(
                    elapsedTimeInMillis = elapsedMillis
                )
                
                // Update every 10ms for smooth display
                delay(10)
                
                if (!_uiState.value.isTimerRunning) break
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerUpdateJob?.cancel()
    }
}

