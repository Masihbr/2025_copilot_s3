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
}

@Composable
fun MovieSwipeNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Auth.route) {
        composable(Screen.Auth.route) { AuthScreen() }
        composable(Screen.Group.route) { GroupScreen() }
        composable(Screen.Voting.route) { VotingScreen() }
        composable(Screen.Movie.route) { MovieScreen() }
    }
}

