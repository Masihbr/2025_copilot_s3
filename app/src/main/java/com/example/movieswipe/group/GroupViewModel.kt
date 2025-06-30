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
}
