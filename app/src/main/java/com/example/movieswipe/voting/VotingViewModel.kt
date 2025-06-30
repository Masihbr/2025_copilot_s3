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

    // Voting (swipe) state
    private val _votingState = MutableStateFlow<VotingUiState>(VotingUiState.Idle)
    val votingState: StateFlow<VotingUiState> = _votingState
    private var votingSessionId: String? = null
    private var votingMovies: List<Movie> = emptyList()
    private var userVotes: MutableMap<Int, String> = mutableMapOf()
    private var currentIndex = 0

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

    // Voting (swipe) logic
    fun loadVotingSession(sessionId: String) {
        _votingState.value = VotingUiState.Loading
        viewModelScope.launch {
            val result = repo.getSessionDetails(sessionId)
            if (result.isSuccess) {
                val details = result.getOrThrow()
                votingSessionId = details.id
                votingMovies = details.movies
                userVotes = details.userVotes.toMutableMap()
                currentIndex = votingMovies.indexOfFirst { userVotes[it.id] == null }
                if (currentIndex == -1) currentIndex = votingMovies.size // All voted
                _votingState.value = VotingUiState.Ready(
                    movies = votingMovies,
                    userVotes = userVotes.toMap(),
                    currentIndex = currentIndex
                )
            } else {
                _votingState.value = VotingUiState.Error(result.exceptionOrNull()?.message ?: "Failed to load voting session")
            }
        }
    }

    fun vote(vote: String) {
        val sessionId = votingSessionId ?: return
        if (currentIndex !in votingMovies.indices) return
        val movie = votingMovies[currentIndex]
        _votingState.value = VotingUiState.Loading
        viewModelScope.launch {
            val result = repo.castVote(sessionId, movie.id, vote)
            if (result.isSuccess) {
                userVotes[movie.id] = vote
                currentIndex = votingMovies.indexOfFirst { userVotes[it.id] == null }
                if (currentIndex == -1) currentIndex = votingMovies.size
                _votingState.value = VotingUiState.Ready(
                    movies = votingMovies,
                    userVotes = userVotes.toMap(),
                    currentIndex = currentIndex
                )
            } else {
                _votingState.value = VotingUiState.Error(result.exceptionOrNull()?.message ?: "Failed to cast vote")
            }
        }
    }

    fun resetVoting() {
        votingSessionId = null
        votingMovies = emptyList()
        userVotes = mutableMapOf()
        currentIndex = 0
        _votingState.value = VotingUiState.Idle
    }

    fun reset() {
        _sessionState.value = VotingSessionUiState.Idle
    }

    sealed class VotingUiState {
        object Idle : VotingUiState()
        object Loading : VotingUiState()
        data class Ready(
            val movies: List<Movie>,
            val userVotes: Map<Int, String>,
            val currentIndex: Int
        ) : VotingUiState()
        data class Error(val message: String) : VotingUiState()
    }
}
