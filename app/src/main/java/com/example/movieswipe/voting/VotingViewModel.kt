package com.example.movieswipe.voting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class VotingSessionUiState {
    object Idle : VotingSessionUiState()
    object Loading : VotingSessionUiState()
    data class NoSession(val message: String = "No active or pending session.") : VotingSessionUiState()
    data class Pending(val session: VotingSession) : VotingSessionUiState()
    data class Active(val session: VotingSession) : VotingSessionUiState()
    data class Error(val message: String) : VotingSessionUiState()
}

class VotingViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = VotingRepository(app.applicationContext)
    private val _sessionState = MutableStateFlow<VotingSessionUiState>(VotingSessionUiState.Idle)
    val sessionState: StateFlow<VotingSessionUiState> = _sessionState

    fun loadSession(groupId: String) {
        _sessionState.value = VotingSessionUiState.Loading
        viewModelScope.launch {
            val result = repo.getActiveSession(groupId)
            _sessionState.value = when {
                result.isSuccess && result.getOrThrow() == null -> VotingSessionUiState.NoSession()
                result.isSuccess -> {
                    val session = result.getOrThrow()!!
                    when (session.status) {
                        "pending" -> VotingSessionUiState.Pending(session)
                        "active" -> VotingSessionUiState.Active(session)
                        else -> VotingSessionUiState.NoSession()
                    }
                }
                else -> VotingSessionUiState.Error(result.exceptionOrNull()?.message ?: "Failed to load session")
            }
        }
    }

    fun createSession(groupId: String) {
        _sessionState.value = VotingSessionUiState.Loading
        viewModelScope.launch {
            val result = repo.createVotingSession(groupId)
            if (result.isSuccess) {
                val session = result.getOrThrow()
                _sessionState.value = VotingSessionUiState.Pending(session)
            } else {
                _sessionState.value = VotingSessionUiState.Error(result.exceptionOrNull()?.message ?: "Failed to create session")
            }
        }
    }

    fun startSession(sessionId: String) {
        _sessionState.value = VotingSessionUiState.Loading
        viewModelScope.launch {
            val result = repo.startSession(sessionId)
            if (result.isSuccess) {
                val session = result.getOrThrow()
                _sessionState.value = VotingSessionUiState.Active(session)
            } else {
                _sessionState.value = VotingSessionUiState.Error(result.exceptionOrNull()?.message ?: "Failed to start session")
            }
        }
    }

    fun reset() {
        _sessionState.value = VotingSessionUiState.Idle
    }
}
