package com.kidblunt.cleanerguru.manager

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatterySaverState(
    val isEnabled: Boolean = false,
    val enabledAt: Long = 0L,
    val duration: Long = 3 * 60 * 60 * 1000L, // 3 hours in milliseconds
    val backgroundRestrictionEnabled: Boolean = false,
    val autoSyncEnabled: Boolean = true,
    val locationServicesEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val isUltraSaverMode: Boolean = false
)

class BatterySaverManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("battery_saver_prefs", Context.MODE_PRIVATE)
    
    private val _batterySaverState = MutableStateFlow(loadBatterySaverState())
    val batterySaverState: StateFlow<BatterySaverState> = _batterySaverState.asStateFlow()
    
    init {
        // Check if battery saver should be disabled due to timeout
        checkBatterySaverTimeout()
    }
    
    private fun loadBatterySaverState(): BatterySaverState {
        return BatterySaverState(
            isEnabled = prefs.getBoolean("is_enabled", false),
            enabledAt = prefs.getLong("enabled_at", 0L),
            duration = prefs.getLong("duration", 3 * 60 * 60 * 1000L),
            backgroundRestrictionEnabled = prefs.getBoolean("background_restriction", false),
            autoSyncEnabled = prefs.getBoolean("auto_sync", true),
            locationServicesEnabled = prefs.getBoolean("location_services", true),
            vibrationEnabled = prefs.getBoolean("vibration", true),
            isUltraSaverMode = prefs.getBoolean("ultra_saver_mode", false)
        )
    }
    
    private fun saveBatterySaverState(state: BatterySaverState) {
        prefs.edit().apply {
            putBoolean("is_enabled", state.isEnabled)
            putLong("enabled_at", state.enabledAt)
            putLong("duration", state.duration)
            putBoolean("background_restriction", state.backgroundRestrictionEnabled)
            putBoolean("auto_sync", state.autoSyncEnabled)
            putBoolean("location_services", state.locationServicesEnabled)
            putBoolean("vibration", state.vibrationEnabled)
            putBoolean("ultra_saver_mode", state.isUltraSaverMode)
            apply()
        }
    }
    
    fun enableBatterySaver() {
        val currentTime = System.currentTimeMillis()
        val newState = _batterySaverState.value.copy(
            isEnabled = true,
            enabledAt = currentTime,
            backgroundRestrictionEnabled = true,
            autoSyncEnabled = false,
            locationServicesEnabled = true, // Keep location on for power saver
            vibrationEnabled = false,
            isUltraSaverMode = false // Power saver is not ultra saver
        )
        _batterySaverState.value = newState
        saveBatterySaverState(newState)
    }
    
    fun enableUltraSaver() {
        val currentTime = System.currentTimeMillis()
        val newState = _batterySaverState.value.copy(
            isEnabled = true,
            enabledAt = currentTime,
            backgroundRestrictionEnabled = true,
            autoSyncEnabled = false,
            locationServicesEnabled = false, // Turn off location for ultra saver
            vibrationEnabled = false,
            isUltraSaverMode = true // This is ultra saver mode
        )
        _batterySaverState.value = newState
        saveBatterySaverState(newState)
    }
    
    fun disableBatterySaver() {
        val newState = _batterySaverState.value.copy(
            isEnabled = false,
            enabledAt = 0L,
            backgroundRestrictionEnabled = false,
            autoSyncEnabled = true,
            locationServicesEnabled = true,
            vibrationEnabled = true,
            isUltraSaverMode = false
        )
        _batterySaverState.value = newState
        saveBatterySaverState(newState)
    }
    
    fun toggleSetting(setting: String, enabled: Boolean) {
        val newState = when (setting) {
            "background_restriction" -> _batterySaverState.value.copy(backgroundRestrictionEnabled = enabled)
            "auto_sync" -> _batterySaverState.value.copy(autoSyncEnabled = enabled)
            "location_services" -> _batterySaverState.value.copy(locationServicesEnabled = enabled)
            "vibration" -> _batterySaverState.value.copy(vibrationEnabled = enabled)
            else -> _batterySaverState.value
        }
        _batterySaverState.value = newState
        saveBatterySaverState(newState)
    }
    
    fun checkBatterySaverTimeout() {
        val currentState = _batterySaverState.value
        if (currentState.isEnabled && currentState.enabledAt > 0) {
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - currentState.enabledAt
            
            if (elapsedTime >= currentState.duration) {
                // Battery saver has expired, disable it
                disableBatterySaver()
            }
        }
    }
    
    fun getRemainingTime(): Long {
        val currentState = _batterySaverState.value
        if (!currentState.isEnabled || currentState.enabledAt == 0L) {
            return 0L
        }
        
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - currentState.enabledAt
        val remainingTime = currentState.duration - elapsedTime
        
        return if (remainingTime > 0) remainingTime else 0L
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
