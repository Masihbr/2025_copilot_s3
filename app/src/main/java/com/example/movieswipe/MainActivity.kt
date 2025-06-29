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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

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

@JsonClass(generateAdapter = true)
data class SelectedMovie(
    val movieId: String,
    val title: String,
    val year: Int?,
    val genres: List<String>,
    val posterUrl: String?,
    val score: Double?,
    val likeCount: Int?,
    val dislikeCount: Int?,
    val totalVotes: Int?,
    val reason: String?,
    val isWinner: Boolean
)

@JsonClass(generateAdapter = true)
data class MovieResults(
    val sessionId: String,
    val results: List<SelectedMovie>,
    val winner: SelectedMovie?,
    val totalMovies: Int
)

@JsonClass(generateAdapter = true)
data class SelectedMovieResponse(
    val success: Boolean,
    val data: SelectedMovie?,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class MovieResultsResponse(
    val success: Boolean,
    val data: MovieResults?,
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userVotes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var votingStats by remember { mutableStateOf<VotingStats?>(null) }
    var wsSocket by remember { mutableStateOf<Socket?>(null) }
    var selectedMovie by remember { mutableStateOf<SelectedMovie?>(null) }
    var showResults by remember { mutableStateOf(false) }
    var isCompleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val jwtToken = remember { AuthPrefs.getTokenFlow(context) }
    val userId = remember { getUserIdFromToken(jwtToken.firstOrNull() ?: "") }
    val isOwner = session.createdBy == userId

    // Fetch user votes on session load
    LaunchedEffect(session.id) {
        val token = AuthPrefs.getTokenFlow(context).firstOrNull()
        if (token != null) {
            fetchUserVotes(token, session.id) { votes ->
                userVotes = votes.associate { it.movieId to it.vote }
            }
            fetchVotingStats(token, session.id) { stats ->
                votingStats = stats
            }
        }
    }

    // WebSocket for real-time updates
    LaunchedEffect(session.id) {
        val token = AuthPrefs.getTokenFlow(context).firstOrNull()
        if (token != null) {
            val opts = IO.Options()
            opts.auth = mapOf("token" to token)
            val socket = IO.socket("http://localhost:3000", opts)
            wsSocket = socket
            socket.on("session-completed") { args ->
                val data = args[0] as JSONObject
                val selected = data.optJSONObject("selectedMovie")
                if (selected != null) {
                    val moshi = Moshi.Builder().build()
                    selectedMovie = moshi.adapter(SelectedMovie::class.java).fromJson(selected.toString())
                    showResults = true
                }
            }
            socket.connect()
            socket.emit("join-group", session.groupId)
        }
        onDispose { wsSocket?.disconnect() }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        Text("Voting Session Status: ${session.status}", style = MaterialTheme.typography.subtitle1)
        Text("Started: ${session.startedAt ?: "-"}")
        if (session.status == "active") {
            VotingSwipeStack(
                session = session,
                userVotes = userVotes,
                onVote = { movieId, vote ->
                    val token = AuthPrefs.getTokenFlow(context).firstOrNull()
                    if (token != null) {
                        submitVote(token, session.id, movieId, vote) { success ->
                            if (success) {
                                userVotes = userVotes.toMutableMap().apply { put(movieId, vote) }
                            }
                        }
                    }
                }
            )
            votingStats?.let {
                Text("Participation: ${it.votedMembers}/${it.totalMembers} (${it.participationRate}%)")
                Text("Likes: ${it.voteBreakdown.likes}, Dislikes: ${it.voteBreakdown.dislikes}")
            }
            if (isOwner) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    isCompleting = true
                    val token = AuthPrefs.getTokenFlow(context).firstOrNull()
                    if (token != null) {
                        completeVotingSession(token, session.id, onSuccess = {
                            selectedMovie = it
                            showResults = true
                            isCompleting = false
                        }, onError = {
                            error = it
                            isCompleting = false
                        })
                    }
                }, enabled = !isCompleting) {
                    Text(if (isCompleting) "Completing..." else "End Voting & Show Winner")
                }
            }
        }
        if (session.status == "completed" || showResults) {
            if (selectedMovie != null) {
                MovieWinnerDisplay(selectedMovie!!)
            } else {
                val token = AuthPrefs.getTokenFlow(context).firstOrNull()
                if (token != null) {
                    LaunchedEffect(session.id) {
                        fetchSelectedMovie(token, session.id, onSuccess = {
                            selectedMovie = it
                        }, onError = {
                            error = it
                        })
                    }
                }
                if (selectedMovie != null) {
                    MovieWinnerDisplay(selectedMovie!!)
                } else {
                    Text("Fetching winner...")
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colors.error) }
    }
}

@Composable
fun MovieWinnerDisplay(movie: SelectedMovie) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color(0xFFE3FCEC), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎉 Winner! 🎉", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
        Spacer(modifier = Modifier.height(8.dp))
        Text(movie.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Year: ${movie.year ?: "?"}")
        Text("Genres: ${movie.genres.joinToString()}")
        movie.reason?.let { Text("Why: $it", color = Color.Gray) }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Score: ${movie.score ?: 0.0}")
        Text("Likes: ${movie.likeCount ?: 0}, Dislikes: ${movie.dislikeCount ?: 0}, Total Votes: ${movie.totalVotes ?: 0}")
        movie.posterUrl?.let {
            // Optionally, show poster with Coil or similar
        }
    }
}

@Composable
fun VotingSwipeStack(
    session: VotingSession,
    userVotes: Map<String, String>,
    onVote: (String, String) -> Unit
) {
    val movies = session.movieRecommendations
    var currentIndex by remember { mutableStateOf(0) }
    val total = movies.size
    if (currentIndex >= total) {
        Text("You have voted on all movies.")
        return
    }
    val movie = movies[currentIndex]
    val vote = userVotes[movie.movieId]
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .pointerInput(movie.movieId) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            offsetX.value > 200f -> {
                                onVote(movie.movieId, "like")
                                scope.launch {
                                    offsetX.animateTo(0f, tween(300))
                                    currentIndex++
                                }
                            }
                            offsetX.value < -200f -> {
                                onVote(movie.movieId, "dislike")
                                scope.launch {
                                    offsetX.animateTo(0f, tween(300))
                                    currentIndex++
                                }
                            }
                            else -> scope.launch { offsetX.animateTo(0f, tween(300)) }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .offset(x = offsetX.value.dp)
                .scale(1f - (kotlin.math.abs(offsetX.value) / 2000f))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(movie.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Year: ${movie.year ?: "?"}")
                Text("Genres: ${movie.genres.joinToString()}")
                movie.reason?.let { Text("Why: $it", fontSize = 14.sp, color = Color.Gray) }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Swipe right for YES, left for NO", color = Color.Gray)
                vote?.let {
                    Text("Your vote: ${it.uppercase()}", color = if (it == "like") Color.Green else Color.Red)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            onVote(movie.movieId, "dislike")
            currentIndex++
        }, enabled = vote != "dislike") { Text("Dislike") }
        Spacer(modifier = Modifier.width(32.dp))
        Button(onClick = {
            onVote(movie.movieId, "like")
            currentIndex++
        }, enabled = vote != "like") { Text("Like") }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text("Movie ${currentIndex + 1} of $total")
}

// --- API and WebSocket helpers ---

@JsonClass(generateAdapter = true)
data class VotingStats(
    val totalMembers: Int,
    val votedMembers: Int,
    val pendingMembers: Int,
    val participationRate: Double,
    val totalVotes: Int,
    val voteBreakdown: VoteBreakdown,
    val sessionStatus: String
)
@JsonClass(generateAdapter = true)
data class VoteBreakdown(
    val likes: Int,
    val dislikes: Int
)

fun fetchUserVotes(token: String, sessionId: String, onResult: (List<Vote>) -> Unit) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://localhost:3000/api/voting/sessions/$sessionId/votes")
        .addHeader("Authorization", "Bearer $token")
        .build()
    Thread {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val moshi = Moshi.Builder().build()
                val json = response.body?.string()
                val obj = JSONObject(json)
                val data = obj.optJSONArray("data")
                val votes = mutableListOf<Vote>()
                if (data != null) {
                    for (i in 0 until data.length()) {
                        val v = data.getJSONObject(i)
                        votes.add(
                            Vote(
                                userId = v.getString("userId"),
                                movieId = v.getString("movieId"),
                                vote = v.getString("vote"),
                                timestamp = v.optString("timestamp")
                            )
                        )
                    }
                }
                onResult(votes)
            } else {
                onResult(emptyList())
            }
        } catch (e: Exception) {
            onResult(emptyList())
        }
    }.start()
}

fun submitVote(token: String, sessionId: String, movieId: String, vote: String, onResult: (Boolean) -> Unit) {
    val client = OkHttpClient()
    val json = "{" +
            "\"movieId\":\"$movieId\"," +
            "\"vote\":\"$vote\"}"
    val body = json.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url("http://localhost:3000/api/voting/sessions/$sessionId/vote")
        .post(body)
        .addHeader("Authorization", "Bearer $token")
        .build()
    Thread {
        try {
            val response = client.newCall(request).execute()
            onResult(response.isSuccessful)
        } catch (e: Exception) {
            onResult(false)
        }
    }.start()
}

fun fetchVotingStats(token: String, sessionId: String, onResult: (VotingStats?) -> Unit) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://localhost:3000/api/voting/sessions/$sessionId/stats")
        .addHeader("Authorization", "Bearer $token")
        .build()
    Thread {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val moshi = Moshi.Builder().build()
                val json = response.body?.string()
                val obj = JSONObject(json)
                val data = obj.optJSONObject("data")
                if (data != null) {
                    val stats = VotingStats(
                        totalMembers = data.optInt("totalMembers"),
                        votedMembers = data.optInt("votedMembers"),
                        pendingMembers = data.optInt("pendingMembers"),
                        participationRate = data.optDouble("participationRate"),
                        totalVotes = data.optInt("totalVotes"),
                        voteBreakdown = VoteBreakdown(
                            likes = data.optJSONObject("voteBreakdown")?.optInt("likes") ?: 0,
                            dislikes = data.optJSONObject("voteBreakdown")?.optInt("dislikes") ?: 0
                        ),
                        sessionStatus = data.optString("sessionStatus")
                    )
                    onResult(stats)
                } else {
                    onResult(null)
                }
            } else {
                onResult(null)
            }
        } catch (e: Exception) {
            onResult(null)
        }
    }.start()
}

fun completeVotingSession(token: String, sessionId: String, onSuccess: (SelectedMovie) -> Unit, onError: (String) -> Unit) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://localhost:3000/api/voting/sessions/$sessionId/complete")
        .post("".toRequestBody("application/json".toMediaType()))
        .addHeader("Authorization", "Bearer $token")
        .build()
    Thread {
        try {
            val response = client.newCall(request).execute()
            val moshi = Moshi.Builder().build()
            val obj = JSONObject(response.body?.string() ?: "")
            val data = obj.optJSONObject("data")
            val selectedMovie = data?.optJSONObject("selectedMovie")
            if (response.isSuccessful && selectedMovie != null) {
                val movie = moshi.adapter(SelectedMovie::class.java).fromJson(selectedMovie.toString())
                if (movie != null) onSuccess(movie) else onError("No winner found.")
            } else {
                onError(obj.optString("error") ?: "Failed to complete session.")
            }
        } catch (e: Exception) {
            onError("Network error: ${e.localizedMessage}")
        }
    }.start()
}

fun fetchSelectedMovie(token: String, sessionId: String, onSuccess: (SelectedMovie) -> Unit, onError: (String) -> Unit) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://localhost:3000/api/voting/sessions/$sessionId/selected-movie")
        .addHeader("Authorization", "Bearer $token")
        .build()
    Thread {
        try {
            val response = client.newCall(request).execute()
            val moshi = Moshi.Builder().build()
            val obj = JSONObject(response.body?.string() ?: "")
            val data = obj.optJSONObject("data")
            if (response.isSuccessful && data != null) {
                val movie = moshi.adapter(SelectedMovie::class.java).fromJson(data.toString())
                if (movie != null) onSuccess(movie) else onError("No winner found.")
            } else {
                onError(obj.optString("error") ?: "No winner found.")
            }
        } catch (e: Exception) {
            onError("Network error: ${e.localizedMessage}")
        }
    }.start()
}

fun fetchMovieResults(token: String, sessionId: String, onSuccess: (MovieResults) -> Unit, onError: (String) -> Unit) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://localhost:3000/api/voting/sessions/$sessionId/results")
        .addHeader("Authorization", "Bearer $token")
        .build()
    Thread {
        try {
            val response = client.newCall(request).execute()
            val moshi = Moshi.Builder().build()
            val obj = JSONObject(response.body?.string() ?: "")
            val data = obj.optJSONObject("data")
            if (response.isSuccessful && data != null) {
                val results = moshi.adapter(MovieResults::class.java).fromJson(data.toString())
                if (results != null) onSuccess(results) else onError("No results found.")
            } else {
                onError(obj.optString("error") ?: "No results found.")
            }
        } catch (e: Exception) {
            onError("Network error: ${e.localizedMessage}")
        }
    }.start()
}
