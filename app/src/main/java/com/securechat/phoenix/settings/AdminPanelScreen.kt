package com.securechat.phoenix.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.phoenix.ui.theme.ChatColors

data class AdminUser(
    val id: String,
    val uniqueId: String,
    val displayName: String,
    val status: String,
    val role: String,
    val lastSeen: Long? = null
)

data class AdminInvite(
    val code: String,
    val isUsed: Boolean,
    val expiresAt: String?,
    val usedBy: String?
)

data class ServerStats(
    val totalUsers: Int = 0,
    val activeToday: Int = 0,
    val totalMessages: Int = 0,
    val storageUsedMB: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    users: List<AdminUser> = emptyList(),
    invites: List<AdminInvite> = emptyList(),
    stats: ServerStats = ServerStats(),
    onBack: () -> Unit,
    onUpdateUserStatus: (String, String) -> Unit = { _, _ -> },
    onGenerateInvite: () -> Unit = {},
    onCopyCode: (String) -> Unit = {},
    onForceLogout: () -> Unit = {},
    onWipeMedia: () -> Unit = {}
) {
    var showForceLogoutDialog by remember { mutableStateOf(false) }
    var showWipeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ChatColors.Teal)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats
            item { StatsSection(stats) }

            // Users
            item {
                Text("Users", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            items(users) { user ->
                UserCard(user = user, onStatusChange = { status -> onUpdateUserStatus(user.id, status) })
            }

            // Invites
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Invite Codes", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onGenerateInvite) {
                        Icon(Icons.Default.Add, "Generate", tint = ChatColors.Teal)
                    }
                }
            }
            items(invites) { invite ->
                InviteCard(invite = invite, onCopy = { onCopyCode(invite.code) })
            }

            // Danger Zone
            item {
                Spacer(Modifier.height(16.dp))
                Text("Danger Zone", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFFF3B30))
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showForceLogoutDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Force Logout All Users") }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showWipeDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Wipe All Media") }
            }
        }
    }

    if (showForceLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showForceLogoutDialog = false },
            title = { Text("Force Logout") },
            text = { Text("This will invalidate ALL user sessions. Everyone will need to re-authenticate.") },
            confirmButton = { TextButton(onClick = { onForceLogout(); showForceLogoutDialog = false }) { Text("Confirm", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showForceLogoutDialog = false }) { Text("Cancel") } }
        )
    }
    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = { Text("Wipe All Media") },
            text = { Text("This will PERMANENTLY delete all uploaded media. This cannot be undone.") },
            confirmButton = { TextButton(onClick = { onWipeMedia(); showWipeDialog = false }) { Text("DELETE ALL", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showWipeDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatsSection(stats: ServerStats) {
    Card(colors = CardDefaults.cardColors(containerColor = ChatColors.Teal.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem("Users", stats.totalUsers.toString())
            StatItem("Active", stats.activeToday.toString())
            StatItem("Messages", stats.totalMessages.toString())
            StatItem("Storage", "${stats.storageUsedMB} MB")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ChatColors.Teal)
        Text(label, fontSize = 12.sp, color = ChatColors.TextSecondary)
    }
}

@Composable
private fun UserCard(user: AdminUser, onStatusChange: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(ChatColors.TealLight.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Text(user.displayName.take(1).uppercase(), color = ChatColors.Teal, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.displayName, fontWeight = FontWeight.Medium)
                Text("ID: ${user.uniqueId} • ${user.status}", fontSize = 12.sp, color = ChatColors.TextSecondary)
            }
            if (user.status == "active") {
                IconButton(onClick = { onStatusChange("banned") }) {
                    Icon(Icons.Default.Block, "Ban", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            } else if (user.status == "banned") {
                IconButton(onClick = { onStatusChange("active") }) {
                    Icon(Icons.Default.CheckCircle, "Activate", tint = ChatColors.Green, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun InviteCard(invite: AdminInvite, onCopy: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(invite.code, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
                Text(
                    if (invite.isUsed) "Used" else if (invite.expiresAt != null) "Expires: ${invite.expiresAt}" else "Active",
                    fontSize = 12.sp,
                    color = if (invite.isUsed) ChatColors.TextSecondary else ChatColors.Green
                )
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, "Copy", tint = ChatColors.Teal, modifier = Modifier.size(20.dp))
            }
        }
    }
}
