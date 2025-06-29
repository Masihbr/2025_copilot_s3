package com.example.movieswipe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.movieswipe.auth.AuthPrefs
import com.example.movieswipe.ui.theme.MovieSwipeTheme
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import okhttp3.OkHttpClient
import okhttp3.Request

@JsonClass(generateAdapter = true)
data class UserProfile(
    val id: String,
    val email: String,
    val name: String,
    val picture: String?,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class UserProfileResponse(
    val success: Boolean,
    val data: UserProfile?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MovieSwipeTheme {
                Surface(color = MaterialTheme.colors.background) {
                    AuthScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun AuthScreen() {
    val context = LocalContext.current
    var jwtToken by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    val scope = rememberCoroutineScope()

    // Observe token from DataStore
    LaunchedEffect(Unit) {
        AuthPrefs.getTokenFlow(context).collectLatest { token ->
            jwtToken = token
            if (token != null) {
                // Validate token and fetch user profile
                fetchUserProfile(token, onSuccess = { user ->
                    userProfile = user
                }, onError = { err ->
                    error = err
                    userProfile = null
                })
            } else {
                userProfile = null
            }
        }
    }

    // Handle intent for OAuth callback
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        val data: Uri? = activity?.intent?.data
        if (data != null && data.scheme == "movieswipe" && data.host == "auth" && data.path == "/callback") {
            val token = data.getQueryParameter("token")
            if (!token.isNullOrEmpty()) {
                scope.launch { AuthPrefs.saveToken(context, token) }
            } else {
                error = "No token found in callback."
            }
            activity.intent.data = null
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (jwtToken == null || userProfile == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://localhost:3000/api/auth/google"))
                    context.startActivity(intent)
                }) {
                    Text("Sign in with Google")
                }
                error?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = it, color = MaterialTheme.colors.error)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Welcome, ${userProfile?.name}")
                userProfile?.picture?.let { url ->
                    // Optionally, show profile picture with Coil or similar
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    scope.launch { AuthPrefs.clearToken(context) }
                }) {
                    Text("Logout")
                }
            }
        }
    }
}

fun fetchUserProfile(token: String, onSuccess: (UserProfile) -> Unit, onError: (String) -> Unit) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://localhost:3000/api/auth/me")
        .addHeader("Authorization", "Bearer $token")
        .build()
    Thread {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val moshi = Moshi.Builder().build()
                val adapter = moshi.adapter(UserProfileResponse::class.java)
                val body = response.body?.string()
                val userProfileResponse = adapter.fromJson(body ?: "")
                if (userProfileResponse?.success == true && userProfileResponse.data != null) {
                    onSuccess(userProfileResponse.data)
                } else {
                    onError("Failed to fetch user profile.")
                }
            } else {
                onError("Unauthorized or error fetching profile.")
            }
        } catch (e: Exception) {
            onError("Network error: ${e.localizedMessage}")
        }
    }.start()
}
