package com.kidblunt.cleanerguru.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kidblunt.cleanerguru.data.model.ApiResult
import com.kidblunt.cleanerguru.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val TAG = "AuthViewModel"
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun registerAnonymousUser() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            Log.d(TAG, "Initiating anonymous user registration")
            
            when (val result = authRepository.registerAnonymousUser()) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Registration successful")
                    _authState.value = AuthState.Success
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Registration failed: ${result.message}")
                    _authState.value = AuthState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _authState.value = AuthState.Loading
                }
            }
        }
    }

    fun checkLoginStatus(): Boolean {
        return authRepository.isUserLoggedIn()
    }
}

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(AuthRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}