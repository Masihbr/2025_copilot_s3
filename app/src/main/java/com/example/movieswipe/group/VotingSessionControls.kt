package com.example.movieswipe.group

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.movieswipe.navigation.Screen
import com.example.movieswipe.voting.VotingViewModel

@Composable
fun VotingSessionControls(
    groupId: String,
    isOwner: Boolean,
    sessionState: VotingSessionUiState,
    votingViewModel: VotingViewModel,
    onVoteSession: ((String) -> Unit)? = null,
    onShowResults: ((String) -> Unit)? = null
) {
    val selectionState by votingViewModel.selectionState.collectAsState()
    var showEndDialog by remember { mutableStateOf(false) }
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
                Row {
                    Button(onClick = { onVoteSession?.invoke(sessionState.session.id) }) {
                        Text("Go to Voting")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { showEndDialog = true }, enabled = isOwner) {
                        Text("End Session & Show Results")
                    }
                }
                if (showEndDialog) {
                    AlertDialog(
                        onDismissRequest = { showEndDialog = false },
                        title = { Text("End Voting Session?") },
                        text = { Text("Are you sure you want to end the session and select the winning movie?") },
                        confirmButton = {
                            Button(onClick = {
                                votingViewModel.endSessionAndFetchResults(sessionState.session.id)
                                showEndDialog = false
                            }) { Text("End & Show Results") }
                        },
                        dismissButton = {
                            Button(onClick = { showEndDialog = false }) { Text("Cancel") }
                        }
                    )
                }
                if (selectionState is VotingViewModel.MovieSelectionUiState.Results) {
                    val sessionId = sessionState.session.id
                    onShowResults?.invoke(sessionId)
                }
            }
            VotingSessionUiState.Idle -> {}
        }
    }
}
