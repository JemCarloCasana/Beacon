package com.example.beacon.sos

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

// Define all possible SOS states
sealed class SosState {
    object Idle : SosState()
    object CountingDown : SosState()
    object Activated : SosState()
    object Cancelled : SosState()
}

// Define all possible SOS events
sealed class SosEvent {
    object PressStarted : SosEvent()
    object PressReleased : SosEvent()
    object TimerCompleted : SosEvent()
    object Reset : SosEvent()
    object SosDeactivated : SosEvent()
}

/**
 * State machine to manage SOS button behavior and prevent progress bar getting stuck.
 * Provides reliable state transitions and eliminates race conditions.
 */
class SosStateMachine {
    
    private val _state = MutableLiveData<SosState>(SosState.Idle)
    val state: LiveData<SosState> = _state
    
    private val _progress = MutableLiveData<Int>(0)
    val progress: LiveData<Int> = _progress
    
    // Callback for SOS activation
    private var onSosActivatedCallback: (() -> Unit)? = null
    
    // CountDownTimer for precise timing control
    private var countdownTimer: CountDownTimer? = null
    
    companion object {
        private const val COUNTDOWN_DURATION = 3000L // 3 seconds
        private const val UPDATE_INTERVAL = 16L // ~60 FPS updates
        private const val MAX_PROGRESS = 100
    }
    
    init {
        setupCountdownTimer()
    }
    
    private fun setupCountdownTimer() {
        countdownTimer = object : CountDownTimer(COUNTDOWN_DURATION, UPDATE_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                // Calculate progress from elapsed time
                val elapsed = COUNTDOWN_DURATION - millisUntilFinished
                val progress = ((elapsed.toFloat() / COUNTDOWN_DURATION) * MAX_PROGRESS).toInt()
                _progress.value = progress
                
                Log.d("SosStateMachine", "Progress: $progress%, Time remaining: ${millisUntilFinished}ms")
            }
            
            override fun onFinish() {
                _progress.value = MAX_PROGRESS
                Log.d("SosStateMachine", "Countdown completed")
                handleEvent(SosEvent.TimerCompleted)
            }
        }
    }
    
    /**
     * Main event handler - processes events and updates state
     */
    fun handleEvent(event: SosEvent) {
        val currentState = _state.value ?: return
        
        Log.d("SosStateMachine", "Event: $event, CurrentState: $currentState")
        
        val newState = when (currentState) {
            is SosState.Idle -> {
                when (event) {
                    is SosEvent.PressStarted -> {
                        startCountdown()
                        SosState.CountingDown
                    }
                    is SosEvent.SosDeactivated -> {
                        resetProgress()
                        SosState.Idle
                    }
                    else -> currentState
                }
            }
            
            is SosState.CountingDown -> {
                when (event) {
                    is SosEvent.PressReleased -> {
                        stopCountdown()
                        SosState.Cancelled
                    }
                    is SosEvent.TimerCompleted -> {
                        SosState.Activated
                    }
                    is SosEvent.SosDeactivated -> {
                        stopCountdown()
                        resetProgress()
                        SosState.Idle
                    }
                    else -> currentState
                }
            }
            
            is SosState.Activated -> {
                when (event) {
                    is SosEvent.Reset -> {
                        resetProgress()
                        SosState.Idle
                    }
                    is SosEvent.SosDeactivated -> {
                        resetProgress()
                        SosState.Idle
                    }
                    else -> currentState
                }
            }
            
            is SosState.Cancelled -> {
                when (event) {
                    is SosEvent.Reset -> {
                        SosState.Idle
                    }
                    is SosEvent.SosDeactivated -> {
                        SosState.Idle
                    }
                    else -> SosState.Idle // Auto-transition to Idle
                }
            }
        }
        
        // Update state if it changed
        if (newState != currentState) {
            Log.d("SosStateMachine", "State transition: $currentState -> $newState")
            _state.value = newState
            
            // Handle state entry actions
            onStateEntered(newState, currentState)
        }
    }
    
    /**
     * Handle actions when entering a new state
     */
    private fun onStateEntered(newState: SosState, previousState: SosState) {
        when (newState) {
            is SosState.Activated -> {
                // Trigger SOS activation callback
                onSosActivatedCallback?.invoke()
                Log.d("SosStateMachine", "SOS Activated!")
            }
            
            is SosState.Cancelled -> {
                // Cancelled state automatically transitions to Idle after cleanup
                // This ensures clean state management
                _state.postValue(SosState.Idle)
            }
            
            else -> {
                // No special actions for other states
            }
        }
    }
    
    /**
     * Start the countdown timer
     */
    private fun startCountdown() {
        _progress.value = 0
        countdownTimer?.start()
        Log.d("SosStateMachine", "Countdown started")
    }
    
    /**
     * Stop the countdown timer and reset progress
     */
    private fun stopCountdown() {
        countdownTimer?.cancel()
        resetProgress()
        Log.d("SosStateMachine", "Countdown stopped")
    }
    
    /**
     * Reset progress to 0
     */
    private fun resetProgress() {
        _progress.value = 0
    }
    
    /**
     * Set callback for SOS activation
     */
    fun setOnSosActivatedCallback(callback: () -> Unit) {
        onSosActivatedCallback = callback
    }
    
    /**
     * Get current progress value (synchronized to prevent race conditions)
     */
    fun getCurrentProgress(): Int {
        return _progress.value ?: 0
    }
    
    /**
     * Check if SOS is currently counting down
     */
    fun isCountingDown(): Boolean {
        return _state.value is SosState.CountingDown
    }
    
    /**
     * Check if SOS is currently active
     */
    fun isSosActive(): Boolean {
        return _state.value is SosState.Activated
    }
    
    /**
     * Reset state machine to initial state
     */
    fun reset() {
        countdownTimer?.cancel()
        resetProgress()
        _state.value = SosState.Idle
        Log.d("SosStateMachine", "State machine reset to Idle")
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        countdownTimer?.cancel()
        onSosActivatedCallback = null
    }
}