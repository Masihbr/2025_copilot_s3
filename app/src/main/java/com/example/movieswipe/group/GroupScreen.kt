package com.example.movieswipe.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.movieswipe.voting.VotingViewModel
import com.example.movieswipe.voting.VotingSessionUiState
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import kotlinx.coroutines.launch

@Composable
fun GroupScreen(
    viewModel: GroupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val inviteCode by viewModel.inviteCode.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var groupName by remember { mutableStateOf(TextFieldValue("")) }
    var showDialog by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<Group?>(null) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf(TextFieldValue("")) }
    val joinGroupUiState by viewModel.joinGroupUiState.collectAsState()
    val genrePrefUiState by viewModel.genrePrefUiState.collectAsState()
    var showGenreDialog by remember { mutableStateOf(false) }
    var selectedGenres by remember { mutableStateOf(mockedGenres.map { it.copy(weight = 5) }) }
    // Add VotingViewModel for session management
    val votingViewModel: VotingViewModel = composeViewModel()
    var expandedGroupId by remember { mutableStateOf<String?>(null) }

    // FloatingActionButton: Add group or join group
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        FloatingActionButton(onClick = { showDialog = true }, modifier = Modifier.padding(8.dp)) {
            Text("+")
        }
        FloatingActionButton(onClick = { showJoinDialog = true }, modifier = Modifier.padding(8.dp)) {
            Text("Join")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Your Groups") }, actions = {
                IconButton(onClick = { viewModel.loadGroups() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            })
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            when (uiState) {
                is GroupUiState.Loading -> CircularProgressIndicator()
                is GroupUiState.Error -> Text((uiState as GroupUiState.Error).message, color = MaterialTheme.colorScheme.error)
                is GroupUiState.Empty -> Text("No groups yet. Create one!")
                is GroupUiState.Success -> {
                    val groups = (uiState as GroupUiState.Success).groups
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(groups) { group ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(group.name, style = MaterialTheme.typography.titleMedium)
                                    Text("Invite code: ${group.invitationCode}")
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Button(onClick = {
                                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(group.invitationCode))
                                        }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy invite code")
                                            Spacer(Modifier.width(4.dp))
                                            Text("Copy")
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        if (group.ownerId == group.members.firstOrNull()?.userId) {
                                            Button(onClick = { viewModel.generateInviteCode(group.id) }) {
                                                Text("New Code")
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Button(onClick = {
                                                groupToDelete = group
                                                showDialog = true
                                            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete group")
                                                Spacer(Modifier.width(4.dp))
                                                Text("Delete")
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        // Expand/collapse voting session controls
                                        Button(onClick = {
                                            expandedGroupId = if (expandedGroupId == group.id) null else group.id
                                            votingViewModel.loadSession(group.id)
                                        }) {
                                            Text("Voting Session")
                                        }
                                    }
                                    // Voting session controls (expanded)
                                    if (expandedGroupId == group.id) {
                                        val sessionState by votingViewModel.sessionState.collectAsState()
                                        VotingSessionControls(
                                            groupId = group.id,
                                            isOwner = group.ownerId == group.members.firstOrNull()?.userId,
                                            sessionState = sessionState,
                                            votingViewModel = votingViewModel
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (inviteCode != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearInviteCode() },
                    title = { Text("New Invitation Code") },
                    text = { Text(inviteCode ?: "") },
                    confirmButton = {
                        Button(onClick = {
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(inviteCode ?: ""))
                            viewModel.clearInviteCode()
                        }) { Text("Copy & Close") }
                    }
                )
            }
            if (showDialog) {
                if (groupToDelete == null) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Create Group") },
                        text = {
                            OutlinedTextField(
                                value = groupName,
                                onValueChange = { groupName = it },
                                label = { Text("Group Name") }
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (groupName.text.isNotBlank()) {
                                    viewModel.createGroup(groupName.text)
                                    groupName = TextFieldValue("")
                                    showDialog = false
                                }
                            }) { Text("Create") }
                        },
                        dismissButton = {
                            Button(onClick = { showDialog = false }) { Text("Cancel") }
                        }
                    )
                } else {
                    AlertDialog(
                        onDismissRequest = { showDialog = false; groupToDelete = null },
                        title = { Text("Delete Group") },
                        text = { Text("Are you sure you want to delete '${groupToDelete?.name}'?") },
                        confirmButton = {
                            Button(onClick = {
                                groupToDelete?.let { viewModel.deleteGroup(it.id) }
                                showDialog = false
                                groupToDelete = null
                            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
                        },
                        dismissButton = {
                            Button(onClick = { showDialog = false; groupToDelete = null }) { Text("Cancel") }
                        }
                    )
                }
            }
            // Join Group Dialog
            if (showJoinDialog) {
                AlertDialog(
                    onDismissRequest = { showJoinDialog = false; viewModel.resetJoinGroupFlow() },
                    title = { Text("Join Group") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { joinCode = it },
                                label = { Text("Invitation Code") },
                                singleLine = true
                            )
                            if (joinGroupUiState is GroupViewModel.JoinGroupUiState.Error) {
                                Text((joinGroupUiState as GroupViewModel.JoinGroupUiState.Error).message, color = MaterialTheme.colorScheme.error)
                            }
                            if (joinGroupUiState is GroupViewModel.JoinGroupUiState.InviteDetails) {
                                val details = (joinGroupUiState as GroupViewModel.JoinGroupUiState.InviteDetails).details
                                Text("Group: ${'$'}{details.groupName} (${details.memberCount} members)")
                            }
                        }
                    },
                    confirmButton = {
                        Row {
                            Button(onClick = {
                                viewModel.getInviteDetails(joinCode.text)
                            }) { Text("Check") }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = {
                                viewModel.joinGroup(joinCode.text)
                            }, enabled = joinGroupUiState is GroupViewModel.JoinGroupUiState.InviteDetails) { Text("Join") }
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showJoinDialog = false; viewModel.resetJoinGroupFlow() }) { Text("Cancel") }
                    }
                )
            }

            // Genre Preferences Dialog
            if (joinGroupUiState is GroupViewModel.JoinGroupUiState.Joined && !showGenreDialog) {
                showGenreDialog = true
            }
            if (showGenreDialog) {
                AlertDialog(
                    onDismissRequest = { showGenreDialog = false; viewModel.resetJoinGroupFlow(); viewModel.loadGroups() },
                    title = { Text("Set Genre Preferences for ${viewModel.joinedGroupName ?: "Group"}") },
                    text = {
                        Column {
                            Text("Adjust your preference for each genre (1-10):")
                            Spacer(Modifier.height(8.dp))
                            LazyColumn(modifier = Modifier.height(200.dp)) {
                                items(selectedGenres.size) { idx ->
                                    val genre = selectedGenres[idx]
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(genre.genreName, modifier = Modifier.width(100.dp))
                                        Slider(
                                            value = genre.weight.toFloat(),
                                            onValueChange = { newVal ->
                                                selectedGenres = selectedGenres.toMutableList().also { it[idx] = it[idx].copy(weight = newVal.toInt()) }
                                            },
                                            valueRange = 1f..10f,
                                            steps = 8,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text("${'$'}{genre.weight}", modifier = Modifier.width(24.dp))
                                    }
                                }
                            }
                            if (genrePrefUiState is GroupViewModel.GenrePrefUiState.Error) {
                                Text((genrePrefUiState as GroupViewModel.GenrePrefUiState.Error).message, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.setGenrePreferences(selectedGenres)
                        }, enabled = genrePrefUiState !is GroupViewModel.GenrePrefUiState.Loading) { Text("Save") }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showGenreDialog = false
                            viewModel.resetJoinGroupFlow()
                            viewModel.loadGroups()
                        }) { Text("Cancel") }
                    }
                )
                if (genrePrefUiState is GroupViewModel.GenrePrefUiState.Success) {
                    showGenreDialog = false
                    viewModel.resetJoinGroupFlow()
                    viewModel.loadGroups()
                }
            }
        }
    }
}

// Mocked TMDB genre list
val mockedGenres = listOf(
    GenrePreference(28, "Action", 5),
    GenrePreference(12, "Adventure", 5),
    GenrePreference(16, "Animation", 5),
    GenrePreference(35, "Comedy", 5),
    GenrePreference(80, "Crime", 5),
    GenrePreference(99, "Documentary", 5),
    GenrePreference(18, "Drama", 5),
    GenrePreference(10751, "Family", 5),
    GenrePreference(14, "Fantasy", 5),
    GenrePreference(36, "History", 5),
    GenrePreference(27, "Horror", 5),
    GenrePreference(10402, "Music", 5),
    GenrePreference(9648, "Mystery", 5),
    GenrePreference(10749, "Romance", 5),
    GenrePreference(878, "Science Fiction", 5),
    GenrePreference(10770, "TV Movie", 5),
    GenrePreference(53, "Thriller", 5),
    GenrePreference(10752, "War", 5),
    GenrePreference(37, "Western", 5)
)
