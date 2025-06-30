package com.example.movieswipe.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.movieswipe.auth.AuthScreen
import com.example.movieswipe.group.GroupScreen
import com.example.movieswipe.voting.VotingScreen
import com.example.movieswipe.movie.MovieScreen
import com.example.movieswipe.movie.MovieSelectionScreen
import com.example.movieswipe.voting.VotingViewModel

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Group : Screen("group")
    object Voting : Screen("voting")
    object Movie : Screen("movie")
    object VotingWithSession : Screen("voting/{sessionId}") {
        fun createRoute(sessionId: String) = "voting/$sessionId"
    }

    object MovieSelection : Screen("movie_selection/{sessionId}") {
        fun createRoute(sessionId: String) = "movie_selection/$sessionId"
    }
}

@Composable
fun MovieSwipeNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Auth.route) {
        composable(Screen.Auth.route) { AuthScreen(onAuthSuccess = { navController.navigate(Screen.Group.route) }) }
        composable(Screen.Group.route) { GroupScreen(
            onVoteSession = { sessionId -> navController.navigate(Screen.VotingWithSession.createRoute(sessionId)) }
        ) }
        composable(Screen.VotingWithSession.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            VotingScreen(sessionId = sessionId, onDone = { navController.popBackStack() })
        }
        composable(Screen.Movie.route) { MovieScreen() }
        composable(Screen.MovieSelection.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val votingViewModel: VotingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val selectionState by votingViewModel.selectionState.collectAsState()
            LaunchedEffect(sessionId) { votingViewModel.fetchSelectionResults(sessionId) }
            when (selectionState) {
                is VotingViewModel.MovieSelectionUiState.Results -> MovieSelectionScreen(
                    results = (selectionState as VotingViewModel.MovieSelectionUiState.Results).results,
                    onBack = { navController.popBackStack(Screen.Group.route, false) }
                )
                is VotingViewModel.MovieSelectionUiState.Loading -> androidx.compose.material3.CircularProgressIndicator()
                is VotingViewModel.MovieSelectionUiState.Error -> androidx.compose.material3.Text((selectionState as VotingViewModel.MovieSelectionUiState.Error).message)
                else -> {}
            }
        }
    }
}
