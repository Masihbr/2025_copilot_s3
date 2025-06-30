package com.example.movieswipe.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.movieswipe.auth.AuthScreen
import com.example.movieswipe.group.GroupScreen
import com.example.movieswipe.voting.VotingScreen
import com.example.movieswipe.movie.MovieScreen

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Group : Screen("group")
    object Voting : Screen("voting")
    object Movie : Screen("movie")
    object VotingWithSession : Screen("voting/{sessionId}") {
        fun createRoute(sessionId: String) = "voting/$sessionId"
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
    }
}
