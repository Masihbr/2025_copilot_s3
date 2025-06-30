package com.example.movieswipe.group

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class GroupUiState {
    object Loading : GroupUiState()
    data class Success(val groups: List<Group>) : GroupUiState()
    data class Error(val message: String) : GroupUiState()
    object Empty : GroupUiState()
}

class GroupViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GroupRepository(app.applicationContext)
    private val _uiState = MutableStateFlow<GroupUiState>(GroupUiState.Loading)
    val uiState: StateFlow<GroupUiState> = _uiState

    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode

    // --- Join Group State ---
    val joinGroupUiState = MutableStateFlow<JoinGroupUiState>(JoinGroupUiState.Idle)
    val genrePrefUiState = MutableStateFlow<GenrePrefUiState>(GenrePrefUiState.Idle)
    var joinedGroupId: String? = null
        private set
    var joinedGroupName: String? = null
        private set

    init {
        loadGroups()
    }

    fun loadGroups() {
        _uiState.value = GroupUiState.Loading
        viewModelScope.launch {
            val result = repo.getGroups()
            _uiState.value = when {
                result.isSuccess && result.getOrThrow().isNotEmpty() -> GroupUiState.Success(result.getOrThrow())
                result.isSuccess -> GroupUiState.Empty
                else -> GroupUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun createGroup(name: String) {
        _uiState.value = GroupUiState.Loading
        viewModelScope.launch {
            val result = repo.createGroup(name)
            if (result.isSuccess) {
                loadGroups()
            } else {
                _uiState.value = GroupUiState.Error(result.exceptionOrNull()?.message ?: "Failed to create group")
            }
        }
    }

    fun deleteGroup(groupId: String) {
        _uiState.value = GroupUiState.Loading
        viewModelScope.launch {
            val result = repo.deleteGroup(groupId)
            if (result.isSuccess) {
                loadGroups()
            } else {
                _uiState.value = GroupUiState.Error(result.exceptionOrNull()?.message ?: "Failed to delete group")
            }
        }
    }

    fun generateInviteCode(groupId: String) {
        viewModelScope.launch {
            val result = repo.generateInviteCode(groupId)
            _inviteCode.value = if (result.isSuccess) result.getOrThrow() else null
        }
    }

    fun clearInviteCode() { _inviteCode.value = null }

    fun getInviteDetails(inviteCode: String) {
        joinGroupUiState.value = JoinGroupUiState.Loading
        viewModelScope.launch {
            val result = repo.getInviteDetails(inviteCode)
            joinGroupUiState.value = if (result.isSuccess) {
                JoinGroupUiState.InviteDetails(result.getOrThrow())
            } else {
                JoinGroupUiState.Error(result.exceptionOrNull()?.message ?: "Invalid invitation code")
            }
        }
    }

    fun joinGroup(inviteCode: String) {
        joinGroupUiState.value = JoinGroupUiState.Loading
        viewModelScope.launch {
            val result = repo.joinGroup(inviteCode)
            if (result.isSuccess) {
                val data = result.getOrThrow()
                joinedGroupId = data.groupId
                joinedGroupName = data.groupName
                joinGroupUiState.value = JoinGroupUiState.Joined(data)
            } else {
                joinGroupUiState.value = JoinGroupUiState.Error(result.exceptionOrNull()?.message ?: "Failed to join group")
            }
        }
    }

    fun setGenrePreferences(preferences: List<GenrePreference>) {
        genrePrefUiState.value = GenrePrefUiState.Loading
        val groupId = joinedGroupId ?: return
        viewModelScope.launch {
            val result = repo.setGenrePreferences(groupId, preferences)
            genrePrefUiState.value = if (result.isSuccess) {
                GenrePrefUiState.Success
            } else {
                GenrePrefUiState.Error(result.exceptionOrNull()?.message ?: "Failed to set preferences")
            }
        }
    }

    fun resetJoinGroupFlow() {
        joinGroupUiState.value = JoinGroupUiState.Idle
        genrePrefUiState.value = GenrePrefUiState.Idle
        joinedGroupId = null
        joinedGroupName = null
    }

    sealed class JoinGroupUiState {
        object Idle : JoinGroupUiState()
        object Loading : JoinGroupUiState()
        data class InviteDetails(val details: InviteDetails) : JoinGroupUiState()
        data class Joined(val result: JoinGroupResult) : JoinGroupUiState()
        data class Error(val message: String) : JoinGroupUiState()
    }

    sealed class GenrePrefUiState {
        object Idle : GenrePrefUiState()
        object Loading : GenrePrefUiState()
        object Success : GenrePrefUiState()
        data class Error(val message: String) : GenrePrefUiState()
    }
}
