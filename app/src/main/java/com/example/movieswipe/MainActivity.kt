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
            if (jwtToken != null && group.owner.id == getUserIdFromToken(jwtToken)) {
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

fun getUserIdFromToken(token: String): String? {
    // For demo: parse JWT payload (base64) and extract "id" (not secure, for display only)
    return try {
        val parts = token.split(".")
        if (parts.size == 3) {
            val payload = android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT)
            val json = String(payload)
            val regex = "\"id\":\s*\"([^"]+)\"".toRegex()
            regex.find(json)?.groupValues?.getOrNull(1)
        } else null
    } catch (e: Exception) { null }
}

fun deleteGroup(token: String, groupId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://localhost:3000/api/groups/$groupId")
        .delete()
        .addHeader("Authorization", "Bearer $token")
        .build()
    Thread {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                onSuccess()
            } else {
                onError("Failed to delete group.")
            }
        } catch (e: Exception) {
            onError("Network error: ${e.localizedMessage}")
        }
    }.start()
}

@Composable
fun CreateGroupDialog(jwtToken: String?, onDismiss: () -> Unit, onGroupCreated: (Group) -> Unit, onError: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group Name") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (optional)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (jwtToken != null && name.isNotBlank()) {
                    isLoading = true
                    createGroup(jwtToken, name, description.ifBlank { null }, onSuccess = {
                        isLoading = false
                        onGroupCreated(it)
                    }, onError = {
                        isLoading = false
                        onError(it)
                    })
                }
            }, enabled = !isLoading && name.isNotBlank()) { Text("Create") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun createGroup(token: String, name: String, description: String?, onSuccess: (Group) -> Unit, onError: (String) -> Unit) {
    val client = OkHttpClient()
    val moshi = Moshi.Builder().build()
    val req = CreateGroupRequest(name, description)
    val json = moshi.adapter(CreateGroupRequest::class.java).toJson(req)
    val body = json.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url("http://localhost:3000/api/groups")
        .post(body)
        .addHeader("Authorization", "Bearer $token")
        .build()
    Thread {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val adapter = moshi.adapter(GroupResponse::class.java)
                val groupResponse = adapter.fromJson(response.body?.string() ?: "")
                if (groupResponse?.success == true) {
                    onSuccess(groupResponse.data)
                } else {
                    onError("Failed to create group.")
                }
            } else {
                onError("Failed to create group.")
            }
        } catch (e: Exception) {
            onError("Network error: ${e.localizedMessage}")
        }
    }.start()
}

@Composable
fun JoinGroupDialog(jwtToken: String?, onDismiss: () -> Unit, onGroupJoined: () -> Unit, onError: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Group") },
        text = {
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Invitation Code") })
        },
        confirmButton = {
            Button(onClick = {
                if (jwtToken != null && code.isNotBlank()) {
                    isLoading = true
                    joinGroup(jwtToken, code, onSuccess = {
                        isLoading = false
                        onGroupJoined()
                    }, onError = {
                        isLoading = false
                        onError(it)
                    })
                }
            }, enabled = !isLoading && code.isNotBlank()) { Text("Join") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun joinGroup(token: String, code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val client = OkHttpClient()
    val moshi = Moshi.Builder().build()
    val req = JoinGroupRequest(code)
    val json = moshi.adapter(JoinGroupRequest::class.java).toJson(req)
    val body = json.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url("http://localhost:3000/api/groups/join")
        .post(body)
        .addHeader("Authorization", "Bearer $token")
        .build()
    Thread {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                onSuccess()
            } else {
                onError("Failed to join group.")
            }
        } catch (e: Exception) {
            onError("Network error: ${e.localizedMessage}")
        }
    }.start()
}
