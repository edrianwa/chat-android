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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.phoenix.ui.theme.ChatColors

data class UserProfile(
    val id: String = "",
    val uniqueId: String = "",
    val displayName: String = "",
    val about: String = "",
    val avatarUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: UserProfile,
    onUpdateName: (String) -> Unit,
    onUpdateAbout: (String) -> Unit,
    onChangeAvatar: () -> Unit,
    onBack: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var isEditingAbout by remember { mutableStateOf(false) }
    var nameInput by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var aboutInput by remember(profile.about) { mutableStateOf(profile.about) }
    val clipboardManager = LocalClipboardManager.current
    val isDark = MaterialTheme.colorScheme.background == ChatColors.SurfaceDark

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) ChatColors.AppBarDark else ChatColors.Teal
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // Avatar
            Box(
                modifier = Modifier.clickable(onClick = onChangeAvatar),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(ChatColors.TealLight.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        profile.displayName.take(1).uppercase(),
                        color = ChatColors.Teal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ChatColors.Teal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        "Change photo",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Name field
            ProfileField(
                icon = Icons.Default.Person,
                label = "Name",
                value = profile.displayName,
                isEditing = isEditingName,
                editValue = nameInput,
                onEditValueChange = { nameInput = it },
                onEditClick = { isEditingName = true },
                onSave = {
                    onUpdateName(nameInput)
                    isEditingName = false
                },
                onCancel = {
                    nameInput = profile.displayName
                    isEditingName = false
                }
            )

            Divider(
                modifier = Modifier.padding(start = 72.dp),
                color = MaterialTheme.colorScheme.outline,
                thickness = 0.5.dp
            )

            // About field
            ProfileField(
                icon = Icons.Default.Info,
                label = "About",
                value = profile.about.ifEmpty { "Hey there! I am using Phoenix" },
                isEditing = isEditingAbout,
                editValue = aboutInput,
                onEditValueChange = { aboutInput = it.take(256) },
                onEditClick = { isEditingAbout = true },
                onSave = {
                    onUpdateAbout(aboutInput)
                    isEditingAbout = false
                },
                onCancel = {
                    aboutInput = profile.about
                    isEditingAbout = false
                }
            )

            Divider(
                modifier = Modifier.padding(start = 72.dp),
                color = MaterialTheme.colorScheme.outline,
                thickness = 0.5.dp
            )

            // ID Number (read-only, copyable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    null,
                    tint = ChatColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(32.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Your ID Number",
                        color = ChatColors.TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        profile.uniqueId,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(profile.uniqueId))
                }) {
                    Text("Copy", color = ChatColors.TealLight)
                }
            }
        }
    }
}

@Composable
private fun ProfileField(
    icon: ImageVector,
    label: String,
    value: String,
    isEditing: Boolean,
    editValue: String,
    onEditValueChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (isEditing) 8.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = ChatColors.TextSecondary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(32.dp))

        if (isEditing) {
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = onEditValueChange,
                    label = { Text(label) },
                    singleLine = label == "Name",
                    modifier = Modifier.fillMaxWidth()
                )
                Row {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                    TextButton(onClick = onSave) { Text("Save", color = ChatColors.Teal) }
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = ChatColors.TextSecondary, fontSize = 13.sp)
                Text(
                    value,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, "Edit", tint = ChatColors.TealLight)
            }
        }
    }
}
