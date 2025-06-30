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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Your Groups") }, actions = {
                IconButton(onClick = { viewModel.loadGroups() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Text("+")
            }
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
        }
    }
}
