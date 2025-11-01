package com.example.nurtra_android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimerUiState(
    val elapsedTimeInMillis: Long = 0L,
    val isTimerRunning: Boolean = false
)

class TimerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()
    
    @Volatile
    private var timerJob: kotlinx.coroutines.Job? = null

    fun startStopwatch() {
        if (_uiState.value.isTimerRunning) return
        
        // Cancel any existing timer
        timerJob?.cancel()
        
        _uiState.value = _uiState.value.copy(
            elapsedTimeInMillis = 0L,
            isTimerRunning = true
        )
        
        timerJob = viewModelScope.launch {
            var elapsed = 0L
            while (true) {
                delay(10) // Update every 10ms for smooth stopwatch
                if (!_uiState.value.isTimerRunning) break
                elapsed += 10
                _uiState.value = _uiState.value.copy(
                    elapsedTimeInMillis = elapsed
                )
            }
        }
    }

    fun pauseStopwatch() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(isTimerRunning = false)
    }

    fun resumeStopwatch() {
        if (_uiState.value.isTimerRunning) return
        
        val currentTime = _uiState.value.elapsedTimeInMillis
        _uiState.value = _uiState.value.copy(isTimerRunning = true)
        
        timerJob = viewModelScope.launch {
            var elapsed = currentTime
            while (true) {
                delay(10)
                if (!_uiState.value.isTimerRunning) break
                elapsed += 10
                _uiState.value = _uiState.value.copy(elapsedTimeInMillis = elapsed)
            }
        }
    }

    fun resetStopwatch() {
        timerJob?.cancel()
        _uiState.value = TimerUiState()
    }
}

