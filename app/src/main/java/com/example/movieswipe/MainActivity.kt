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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.OutlinedTextField
import androidx.compose.ui.text.input.TextFieldValue
import com.squareup.moshi.Types
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

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

@JsonClass(generateAdapter = true)
data class Group(
    val id: String,
    val name: String,
    val description: String?,
    val invitationCode: String?,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class GroupWithMembers(
    val id: String,
    val name: String,
    val description: String?,
    val invitationCode: String?,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
    val owner: UserProfile,
    val members: List<UserProfile>
)

@JsonClass(generateAdapter = true)
data class UserGroupsResponse(
    val success: Boolean,
    val data: List<GroupWithMembers>
)

@JsonClass(generateAdapter = true)
data class CreateGroupRequest(
    val name: String,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class GroupResponse(
    val success: Boolean,
    val data: Group
)

@JsonClass(generateAdapter = true)
data class JoinGroupRequest(
    val invitationCode: String
)

@JsonClass(generateAdapter = true)
data class JoinGroupResponse(
    val success: Boolean,
    val data: Group,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class AvailableGenresResponse(
    val success: Boolean,
    val data: GenresData
)

@JsonClass(generateAdapter = true)
data class GenresData(
    val genres: List<String>,
    val totalGenres: Int
)

@JsonClass(generateAdapter = true)
data class CreatePreferencesRequest(
    val genres: List<String>
)

@JsonClass(generateAdapter = true)
data class PreferencesResponse(
    val success: Boolean,
    val data: PreferenceData?,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class PreferenceData(
    val id: String,
    val userId: String,
    val groupId: String,
    val genres: List<String>,
    val createdAt: String?,
    val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class VotingSession(
    val id: String,
    val groupId: String,
    val createdBy: String,
    val status: String,
    val movieRecommendations: List<MovieRecommendation>,
    val votes: List<Vote>?,
    val results: List<VotingResult>?,
    val settings: VotingSettings?,
    val memberVoteCounts: MemberVoteCounts?,
    val startedAt: String?,
    val endedAt: String?,
    val createdAt: String?,
    val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class MovieRecommendation(
    val movieId: String,
    val title: String,
    val year: Int?,
    val genres: List<String>,
    val posterUrl: String?,
    val score: Double?,
    val reason: String?
)

@JsonClass(generateAdapter = true)
data class VotingSettings(
    val maxRecommendations: Int? = 10,
    val votingDuration: Int? = 60,
    val requireAllMembers: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class MemberVoteCounts(
    val totalMembers: Int?,
    val votedMembers: Int?,
    val pendingMembers: Int?
)

@JsonClass(generateAdapter = true)
data class Vote(
    val userId: String,
    val movieId: String,
    val vote: String,
    val timestamp: String?
)

@JsonClass(generateAdapter = true)
data class VotingResult(
    val movieId: String,
    val title: String,
    val year: Int?,
    val genres: List<String>,
    val posterUrl: String?,
    val likeCount: Int?,
    val dislikeCount: Int?,
    val neutralCount: Int?,
    val totalVotes: Int?,
    val score: Double?
)

@JsonClass(generateAdapter = true)
data class VotingSessionResponse(
    val success: Boolean,
    val data: VotingSession?,
    val message: String?
)

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Groups : Screen("groups")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            MovieSwipeTheme {
                Surface(color = MaterialTheme.colors.background) {
                    AppNavHost(navController = navController)
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
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Auth.route) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthenticated = { navController.navigate(Screen.Groups.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                } }
            )
        }
        composable(Screen.Groups.route) {
            GroupsScreen(
                onLogout = { navController.navigate(Screen.Auth.route) {
                    popUpTo(Screen.Groups.route) { inclusive = true }
                } }
            )
        }
    }
}

@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
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

    // On successful authentication and profile fetch:
    if (jwtToken != null && userProfile != null) {
        LaunchedEffect(jwtToken) { onAuthenticated() }
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

// Placeholder for GroupsScreen
@Composable
fun GroupsScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var jwtToken by remember { mutableStateOf<String?>(null) }
    var groups by remember { mutableStateOf<List<GroupWithMembers>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Observe token
    LaunchedEffect(Unit) {
        AuthPrefs.getTokenFlow(context).collectLatest { token ->
            jwtToken = token
            if (token != null) {
                fetchGroups(token, onSuccess = { groups = it }, onError = { error = it })
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { showCreateDialog = true }) { Text("Create Group") }
            Button(onClick = { showJoinDialog = true }) { Text("Join Group") }
            Button(onClick = onLogout) { Text("Logout") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            Text("Loading groups...")
        } else if (groups.isEmpty()) {
            Text("You are not a member of any groups.")
        } else {
            LazyColumn {
                items(groups) { group ->
                    GroupCard(group = group, jwtToken = jwtToken, onGroupDeleted = {
                        // Refresh groups after deletion
                        if (jwtToken != null) fetchGroups(jwtToken!!, onSuccess = { groups = it }, onError = { error = it })
                    })
                }
            }
        }
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colors.error)
        }
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            jwtToken = jwtToken,
            onDismiss = { showCreateDialog = false },
            onGroupCreated = { newGroup ->
                showCreateDialog = false
                if (jwtToken != null) fetchGroups(jwtToken!!, onSuccess = { groups = it }, onError = { error = it })
            },
            onError = { error = it }
        )
    }
    if (showJoinDialog) {
        JoinGroupDialog(
            jwtToken = jwtToken,
            onDismiss = { showJoinDialog = false },
            onGroupJoined = {
                showJoinDialog = false
                if (jwtToken != null) fetchGroups(jwtToken!!, onSuccess = { groups = it }, onError = { error = it })
            },
            onError = { error = it }
        )
    }
}

fun fetchGroups(token: String, onSuccess: (List<GroupWithMembers>) -> Unit, onError: (String) -> Unit) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://localhost:3000/api/groups")
        .addHeader("Authorization", "Bearer $token")
        .build()
    Thread {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val moshi = Moshi.Builder().build()
                val type = Types.newParameterizedType(UserGroupsResponse::class.java)
                val adapter = moshi.adapter<UserGroupsResponse>(type)
                val body = response.body?.string()
                val groupsResponse = adapter.fromJson(body ?: "")
                if (groupsResponse?.success == true && groupsResponse.data != null) {
                    onSuccess(groupsResponse.data)
                } else {
                    onError("Failed to fetch groups.")
                }
            } else {
                onError("Unauthorized or error fetching groups.")
            }
        } catch (e: Exception) {
            onError("Network error: ${e.localizedMessage}")
        }
    }.start()
}

@Composable
fun GroupCard(group: GroupWithMembers, jwtToken: String?, onGroupDeleted: () -> Unit) {
    var showDetails by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var votingSession by remember { mutableStateOf<VotingSession?>(null) }
    var votingError by remember { mutableStateOf<String?>(null) }
    var isSessionLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = jwtToken?.let { getUserIdFromToken(it) }

    fun fetchSession() {
        if (jwtToken != null) {
            isSessionLoading = true
            fetchActiveVotingSession(jwtToken, group.id, onSuccess = {
                votingSession = it
                isSessionLoading = false
            }, onError = {
                votingSession = null
                votingError = it
                isSessionLoading = false
            })
        }
    }

    // Fetch session when details are shown
    LaunchedEffect(showDetails) {
        if (showDetails) fetchSession()
    }

    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(group.name, style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.width(8.dp))
            if (group.isActive) Text("(Active)", color = MaterialTheme.colors.primary)
        }
        Text(group.description ?: "No description")
        Text("Owner: ${group.owner.name}")
        Text("Members: ${group.members.size}")
        Text("Invitation Code: ${group.invitationCode ?: "-"}")
        Row {
            Button(onClick = { showDetails = !showDetails }) { Text(if (showDetails) "Hide Details" else "Show Details") }
            if (jwtToken != null && group.owner.id == userId) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { showDeleteDialog = true }, enabled = group.isActive) { Text("Delete Group") }
            }
        }
        if (showDetails) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Members:")
                group.members.forEach { member ->
                    Text("- ${member.name} (${member.email})")
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Voting session UI
                if (isSessionLoading) {
                    Text("Loading voting session...")
                } else if (votingSession != null) {
                    VotingSessionStatus(votingSession!!)
                } else if (jwtToken != null && group.owner.id == userId && allMembersHavePreferences(group)) {
                    Button(onClick = {
                        isSessionLoading = true
                        startVotingSession(jwtToken, group.id, onSuccess = {
                            votingSession = it
                            isSessionLoading = false
                        }, onError = {
                            votingError = it
                            isSessionLoading = false
                        })
                    }) { Text("Start Voting Session") }
                } else if (jwtToken != null && group.owner.id == userId) {
                    Text("All members must set preferences before starting a session.", color = MaterialTheme.colors.error)
                }
                votingError?.let { Text(it, color = MaterialTheme.colors.error) }
            }
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete this group?") },
            confirmButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    if (jwtToken != null) {
                        deleteGroup(jwtToken, group.id, onSuccess = onGroupDeleted, onError = {})
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

fun allMembersHavePreferences(group: GroupWithMembers): Boolean {
    // For demo, assume all members have preferences (replace with real check if available)
    return true
}

fun fetchActiveVotingSession(token: String, groupId: String, onSuccess: (VotingSession) -> Unit, onError: (String) -> Unit) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://localhost:3000/api/voting/sessions/group/$groupId")
        .addHeader("Authorization", "Bearer $token")
        .build()
    Thread {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val moshi = Moshi.Builder().build()
                val adapter = moshi.adapter(VotingSessionResponse::class.java)
                val sessionResp = adapter.fromJson(response.body?.string() ?: "")
                if (sessionResp?.success == true && sessionResp.data != null) {
                    onSuccess(sessionResp.data)
                } else {
                    onError(sessionResp?.message ?: "No active session.")
                }
            } else if (response.code == 404) {
                onError("No active session.")
            } else {
                onError("Failed to fetch session.")
            }
        } catch (e: Exception) {
            onError("Network error: ${e.localizedMessage}")
        }
    }.start()
}

fun startVotingSession(token: String, groupId: String, onSuccess: (VotingSession) -> Unit, onError: (String) -> Unit) {
    val client = OkHttpClient()
    val moshi = Moshi.Builder().build()
    val settings = VotingSettings() // Use defaults
    val json = "{" +
            "\"groupId\":\"$groupId\"," +
            "\"settings\":${moshi.adapter(VotingSettings::class.java).toJson(settings)}" +
            "}"
    val body = json.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url("http://localhost:3000/api/voting/sessions")
        .post(body)
        .addHeader("Authorization", "Bearer $token")
        .build()
    Thread {
        try {
            val response = client.newCall(request).execute()
            val adapter = moshi.adapter(VotingSessionResponse::class.java)
            val sessionResp = adapter.fromJson(response.body?.string() ?: "")
            if (response.isSuccessful && sessionResp?.success == true && sessionResp.data != null) {
                onSuccess(sessionResp.data)
            } else {
                onError(sessionResp?.message ?: "Failed to start session.")
            }
        } catch (e: Exception) {
            onError("Network error: ${e.localizedMessage}")
        }
    }.start()
}

@Composable
fun VotingSessionStatus(session: VotingSession) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Voting Session Status: ${session.status}", style = MaterialTheme.typography.subtitle1)
        Text("Started: ${session.startedAt ?: "-"}")
        if (session.status == "active") {
            Text("Recommended Movies:")
            session.movieRecommendations.forEach { movie ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("- ${movie.title} (${movie.year ?: "?"}) [${movie.genres.joinToString()}]")
                    movie.reason?.let { Text("  [Reason: $it]", style = MaterialTheme.typography.caption) }
                }
            }
