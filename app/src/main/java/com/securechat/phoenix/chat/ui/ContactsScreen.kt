package com.securechat.phoenix.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.phoenix.ui.theme.ChatColors

data class ContactItem(
    val id: String,
    val uniqueId: String,
    val displayName: String,
    val isOnline: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    uiState: ContactsUiState,
    onContactClick: (String) -> Unit,
    onAddContact: (String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var idInput by remember { mutableStateOf("") }
    val isDark = MaterialTheme.colorScheme.background == ChatColors.SurfaceDark

    // Navigate to chat when contact added successfully
    val lastAddedContact = uiState.contacts.lastOrNull()
    LaunchedEffect(uiState.addedSuccess) {
        if (uiState.addedSuccess && lastAddedContact != null) {
            onContactClick(lastAddedContact.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Select contact", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${uiState.contacts.size} contacts",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) ChatColors.AppBarDark else ChatColors.Teal
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ChatColors.Green,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, "Add Contact")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            items(uiState.contacts) { contact ->
                ContactRow(
                    contact = contact,
                    onClick = { onContactClick(contact.id) }
                )
                Divider(
                    modifier = Modifier.padding(start = 76.dp),
                    color = MaterialTheme.colorScheme.outline,
                    thickness = 0.5.dp
                )
            }
        }
    }

    // Add Contact Dialog with loading/error states
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isSearching) {
                    showAddDialog = false
                    idInput = ""
                }
            },
            title = { Text("Add Contact") },
            text = {
                Column {
                    Text(
                        "Enter the user's 8-digit ID number.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = idInput,
                        onValueChange = { idInput = it.filter { c -> c.isDigit() }.take(8) },
                        label = { Text("User ID Number") },
                        placeholder = { Text("e.g. 23392481") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = !uiState.isSearching,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Loading indicator
                    if (uiState.isSearching) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = ChatColors.Teal
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Searching...", fontSize = 13.sp, color = ChatColors.TextSecondary)
                        }
                    }

                    // Error message
                    if (uiState.searchError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            uiState.searchError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (idInput.length == 8) {
                            onAddContact(idInput)
                        }
                    },
                    enabled = idInput.length == 8 && !uiState.isSearching
                ) {
                    Text("Add", color = if (idInput.length == 8) ChatColors.Teal else ChatColors.TextSecondary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false; idInput = "" },
                    enabled = !uiState.isSearching
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ContactRow(contact: ContactItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ChatColors.TealLight.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    contact.displayName.take(1).uppercase(),
                    color = ChatColors.Teal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            if (contact.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(ChatColors.OnlineGreen)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                contact.displayName,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                "ID: ${contact.uniqueId}",
                color = ChatColors.TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}
