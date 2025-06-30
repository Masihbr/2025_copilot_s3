package com.example.movieswipe.movie

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.movieswipe.voting.MovieSelectionResults
import com.example.movieswipe.voting.VotingResult

@Composable
fun MovieSelectionScreen(
    results: MovieSelectionResults,
    onBack: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Voting Results", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        results.selectedMovie?.let { movie ->
            Text("Selected Movie:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!movie.posterPath.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(movie.posterPath),
                            contentDescription = null,
                            modifier = Modifier.height(180.dp)
                        )
                    }
                    Text(movie.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(movie.overview, style = MaterialTheme.typography.bodyMedium, maxLines = 5, textAlign = TextAlign.Center)
                    Text("Release: ${movie.releaseDate ?: "-"}")
                    Text("Genres: ${movie.genres.joinToString { it.name }}")
                    Text("TMDB Rating: ${movie.voteAverage ?: "-"}")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("All Movies & Voting Stats", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(results.votingResults) { res ->
                VotingResultCard(res)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onBack?.invoke() }) { Text("Back to Groups") }
    }
}

@Composable
fun VotingResultCard(result: VotingResult) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(result.movie.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Yes: ${result.yesVotes}  No: ${result.noVotes}  Approval: ${result.approvalRate}%  Score: ${"%.2f".format(result.score)}")
        }
    }
}

