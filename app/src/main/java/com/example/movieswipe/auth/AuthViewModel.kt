package com.example.movieswipe.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: UserProfile) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository(app.applicationContext)
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun onGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val account = task.result
                val idToken = account.idToken
                if (idToken == null) {
                    _uiState.value = AuthUiState.Error("No ID token from Google")
                    return@launch
                }
                val result = repo.authenticateWithGoogle(idToken)
                if (result.isSuccess) {
                    _uiState.value = AuthUiState.Success(result.getOrThrow().user)
                } else {
                    _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Google Sign-In failed")
            }
        }
    }

    fun logout() {
        repo.clearJwt()
        _uiState.value = AuthUiState.Idle
    }
}
