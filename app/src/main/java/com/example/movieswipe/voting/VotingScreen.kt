package com.example.movieswipe.voting

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun VotingScreen(
    sessionId: String,
    votingViewModel: VotingViewModel = viewModel(),
    onDone: (() -> Unit)? = null
) {
    val votingState by votingViewModel.votingState.collectAsState()
    var swipeOffset by remember { mutableStateOf(0f) }
    var showDoneDialog by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        votingViewModel.loadVotingSession(sessionId)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (votingState) {
            is VotingViewModel.VotingUiState.Loading -> CircularProgressIndicator()
            is VotingViewModel.VotingUiState.Error -> Text((votingState as VotingViewModel.VotingUiState.Error).message, color = MaterialTheme.colorScheme.error)
            is VotingViewModel.VotingUiState.Ready -> {
                val state = votingState as VotingViewModel.VotingUiState.Ready
                if (state.currentIndex >= state.movies.size) {
                    showDoneDialog = true
                } else {
                    val movie = state.movies[state.currentIndex]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .shadow(8.dp)
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    swipeOffset += dragAmount
                                    if (swipeOffset > 200) {
                                        votingViewModel.vote("yes")
                                        swipeOffset = 0f
                                    } else if (swipeOffset < -200) {
                                        votingViewModel.vote("no")
                                        swipeOffset = 0f
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!movie.posterPath.isNullOrEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(movie.posterPath),
                                    contentDescription = null,
                                    modifier = Modifier.height(220.dp)
                                )
                            }
                            Text(movie.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                            Text(movie.overview, style = MaterialTheme.typography.bodyMedium, maxLines = 5, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { votingViewModel.vote("no") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                                ) {
                                    Icon(Icons.Default.ThumbDown, contentDescription = "No")
                                    Spacer(Modifier.width(4.dp))
                                    Text("No")
                                }
                                Button(
                                    onClick = { votingViewModel.vote("yes") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784))
                                ) {
                                    Icon(Icons.Default.ThumbUp, contentDescription = "Yes")
                                    Spacer(Modifier.width(4.dp))
                                    Text("Yes")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Movie ${state.currentIndex + 1} of ${state.movies.size}")
                        }
                    }
                }
            }
            VotingViewModel.VotingUiState.Idle -> {}
        }
        if (showDoneDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDoneDialog = false
                    votingViewModel.resetVoting()
                    onDone?.invoke()
                },
                title = { Text("Voting Complete") },
                text = { Text("You have voted on all movies in this session.") },
                confirmButton = {
                    Button(onClick = {
                        showDoneDialog = false
                        votingViewModel.resetVoting()
                        onDone?.invoke()
                    }) { Text("OK") }
                }
            )
        }
    }
}
