package com.example.movieswipe.auth

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.movieswipe.auth.GoogleSignInHelper
import com.example.movieswipe.auth.AuthUiState
import com.example.movieswipe.auth.AuthViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(),
    onAuthSuccess: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val task = GoogleSignInHelper.getSignedInAccountFromIntent(data)
            viewModel.onGoogleSignInResult(task)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (uiState) {
            is AuthUiState.Idle, is AuthUiState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = {
                        val signInClient = GoogleSignInHelper.getClient(context)
                        launcher.launch(signInClient.signInIntent)
                    }) {
                        Icon(painterResource(android.R.drawable.ic_dialog_email), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign in with Google")
                    }
                    if (uiState is AuthUiState.Error) {
                        Spacer(Modifier.height(16.dp))
                        Text((uiState as AuthUiState.Error).message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            is AuthUiState.Loading -> {
                CircularProgressIndicator()
            }
            is AuthUiState.Success -> {
                val user = (uiState as AuthUiState.Success).user
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!user.profilePicture.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(user.profilePicture),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    Text("Welcome, ${user.name}")
                    Text(user.email)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.logout() }) {
                        Text("Logout")
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { onAuthSuccess?.invoke() }) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}
