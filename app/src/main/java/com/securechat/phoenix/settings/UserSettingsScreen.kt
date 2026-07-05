package com.securechat.phoenix.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.phoenix.ui.theme.ChatColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsScreen(
    isAdmin: Boolean = false,
    onBack: () -> Unit,
    onProfileClick: () -> Unit = {},
    onPasscodeClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onChatSettingsClick: () -> Unit = {},
    onStorageClick: () -> Unit = {},
    onAdminClick: () -> Unit = {},
    onDeleteAccount: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
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
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Profile
            item {
                SettingsItem(icon = Icons.Default.Person, title = "Profile", subtitle = "Name, avatar, about", onClick = onProfileClick)
                Divider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            }

            // Passcode
            item {
                SettingsItem(icon = Icons.Default.Lock, title = "Passcode & Security", subtitle = "Change passcodes, reassign screens", onClick = onPasscodeClick)
                Divider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            }

            // Privacy
            item {
                SettingsItem(icon = Icons.Default.Visibility, title = "Privacy", subtitle = "Last seen, read receipts, profile photo", onClick = onPrivacyClick)
                Divider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            }

            // Chat settings
            item {
                SettingsItem(icon = Icons.Default.Chat, title = "Chats", subtitle = "Font size, enter sends, media download", onClick = onChatSettingsClick)
                Divider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            }

            // Notifications
            item {
                SettingsItem(icon = Icons.Default.Notifications, title = "Notifications", subtitle = "Message & call notifications")
                Divider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            }

            // Storage
            item {
                SettingsItem(icon = Icons.Default.Storage, title = "Storage & Data", subtitle = "Manage local storage", onClick = onStorageClick)
                Divider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            }

            // Admin (only if admin)
            if (isAdmin) {
                item {
                    Spacer(Modifier.height(16.dp))
                    SettingsItem(
                        icon = Icons.Default.AdminPanelSettings,
                        title = "Admin Panel",
                        subtitle = "Users, invites, server settings",
                        iconTint = Color(0xFFFF6B00),
                        onClick = onAdminClick
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                }
            }

            // Delete account
            item {
                Spacer(Modifier.height(24.dp))
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Delete Account",
                    subtitle = "Permanently delete your account",
                    iconTint = Color.Red,
                    textColor = Color.Red,
                    onClick = onDeleteAccount
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    iconTint: Color = ChatColors.TextSecondary,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textColor, fontSize = 16.sp)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = ChatColors.TextSecondary, fontSize = 13.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = ChatColors.TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
}
