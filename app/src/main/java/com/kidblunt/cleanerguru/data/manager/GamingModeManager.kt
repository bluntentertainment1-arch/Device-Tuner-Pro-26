package com.kidblunt.cleanerguru.manager

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GamingModeState(
    val isEnabled: Boolean = false,
    val cpuBoostEnabled: Boolean = false,
    val enabledAt: Long = 0L,
    val duration: Long = 45 * 60 * 1000L // 45 minutes in milliseconds
)

class GamingModeManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gaming_mode_prefs", Context.MODE_PRIVATE)
    
    private val _gamingModeState = MutableStateFlow(loadGamingModeState())
    val gamingModeState: StateFlow<GamingModeState> = _gamingModeState.asStateFlow()
    
    init {
        // Check if gaming mode should be disabled due to timeout
        checkGamingModeTimeout()
    }
    
    private fun loadGamingModeState(): GamingModeState {
        return GamingModeState(
            isEnabled = prefs.getBoolean("is_enabled", false),
            cpuBoostEnabled = prefs.getBoolean("cpu_boost_enabled", false),
            enabledAt = prefs.getLong("enabled_at", 0L),
            duration = prefs.getLong("duration", 45 * 60 * 1000L)
        )
    }
    
    private fun saveGamingModeState(state: GamingModeState) {
        prefs.edit().apply {
            putBoolean("is_enabled", state.isEnabled)
            putBoolean("cpu_boost_enabled", state.cpuBoostEnabled)
            putLong("enabled_at", state.enabledAt)
            putLong("duration", state.duration)
            apply()
        }
    }
    
    fun enableGamingMode() {
        val currentTime = System.currentTimeMillis()
        val newState = _gamingModeState.value.copy(
            isEnabled = true,
            enabledAt = currentTime
        )
        _gamingModeState.value = newState
        saveGamingModeState(newState)
    }
    
    fun disableGamingMode() {
        val newState = _gamingModeState.value.copy(
            isEnabled = false,
            enabledAt = 0L
        )
        _gamingModeState.value = newState
        saveGamingModeState(newState)
    }
    
    fun toggleCpuBoost(enabled: Boolean) {
        val newState = _gamingModeState.value.copy(cpuBoostEnabled = enabled)
        _gamingModeState.value = newState
        saveGamingModeState(newState)
    }
    
    fun checkGamingModeTimeout() {
        val currentState = _gamingModeState.value
        if (currentState.isEnabled && currentState.enabledAt > 0) {
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - currentState.enabledAt
            
            if (elapsedTime >= currentState.duration) {
                // Gaming mode has expired, disable it
                disableGamingMode()
            }
        }
    }
    
    fun getActiveDuration(): Long {
        val currentState = _gamingModeState.value
        if (!currentState.isEnabled || currentState.enabledAt == 0L) {
            return 0L
        }
        
        val currentTime = System.currentTimeMillis()
        return currentTime - currentState.enabledAt
    }
    
    fun getRemainingTime(): Long {
        val currentState = _gamingModeState.value
        if (!currentState.isEnabled || currentState.enabledAt == 0L) {
            return 0L
        }
        
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - currentState.enabledAt
        val remainingTime = currentState.duration - elapsedTime
        
        return if (remainingTime > 0) remainingTime else 0L
    }
    
    fun formatActiveDuration(): String {
        val durationMs = getActiveDuration()
        if (durationMs <= 0) return "Not active"
        
        val hours = durationMs / (60 * 60 * 1000)
        val minutes = (durationMs % (60 * 60 * 1000)) / (60 * 1000)
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m active"
            minutes > 0 -> "${minutes}m active"
            else -> "Less than 1m active"
        }
    }
    
    fun formatRemainingTime(): String {
        val remainingMs = getRemainingTime()
        if (remainingMs <= 0) return "Expired"
        
        val hours = remainingMs / (60 * 60 * 1000)
        val minutes = (remainingMs % (60 * 60 * 1000)) / (60 * 1000)
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m remaining"
            minutes > 0 -> "${minutes}m remaining"
            else -> "Less than 1m remaining"
        }
    }
}
