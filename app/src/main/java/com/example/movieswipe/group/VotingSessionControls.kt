package com.example.movieswipe.group

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.movieswipe.voting.VotingSessionUiState
import com.example.movieswipe.voting.VotingViewModel

@Composable
fun VotingSessionControls(
    groupId: String,
    isOwner: Boolean,
    sessionState: VotingSessionUiState,
    votingViewModel: VotingViewModel
) {
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        when (sessionState) {
            is VotingSessionUiState.Loading -> CircularProgressIndicator()
            is VotingSessionUiState.Error -> Text(sessionState.message, color = MaterialTheme.colorScheme.error)
            is VotingSessionUiState.NoSession -> {
                if (isOwner) {
                    Button(onClick = { votingViewModel.createSession(groupId) }) {
                        Text("Create Voting Session")
                    }
                } else {
                    Text("No voting session available.")
                }
            }
            is VotingSessionUiState.Pending -> {
                Text("Voting session is pending.")
                if (isOwner) {
                    Button(onClick = { votingViewModel.startSession(sessionState.session.id) }) {
                        Text("Start Voting Session")
                    }
                } else {
                    Text("Waiting for owner to start the session...")
                }
            }
            is VotingSessionUiState.Active -> {
                Text("Voting session is active!")
                // You can add navigation to the voting screen here
            }
            VotingSessionUiState.Idle -> {}
        }
    }
}

