package com.securechat.phoenix.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.phoenix.ui.theme.ChatColors

data class PrivacySettings(
    val lastSeenVisibility: String = "everyone", // everyone, contacts, nobody
    val readReceiptsEnabled: Boolean = true,
    val profilePhotoVisibility: String = "everyone"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    settings: PrivacySettings,
    onBack: () -> Unit,
    onLastSeenChange: (String) -> Unit = {},
    onReadReceiptsChange: (Boolean) -> Unit = {},
    onPhotoVisibilityChange: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ChatColors.Teal)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Last Seen
            item {
                Text("Last Seen", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("Who can see when you were last online", fontSize = 13.sp, color = ChatColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                VisibilityOptions(
                    selected = settings.lastSeenVisibility,
                    onSelect = onLastSeenChange
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                Spacer(Modifier.height(16.dp))
            }

            // Read Receipts
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Read Receipts", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "If turned off, you won't send or receive read receipts",
                            fontSize = 13.sp, color = ChatColors.TextSecondary
                        )
                    }
                    Switch(
                        checked = settings.readReceiptsEnabled,
                        onCheckedChange = onReadReceiptsChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = ChatColors.Teal, checkedTrackColor = ChatColors.Teal.copy(alpha = 0.4f))
                    )
                }
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                Spacer(Modifier.height(16.dp))
            }

            // Profile Photo
            item {
                Text("Profile Photo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("Who can see your profile photo", fontSize = 13.sp, color = ChatColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                VisibilityOptions(
                    selected = settings.profilePhotoVisibility,
                    onSelect = onPhotoVisibilityChange
                )
            }
        }
    }
}

@Composable
private fun VisibilityOptions(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("everyone" to "Everyone", "contacts" to "My Contacts", "nobody" to "Nobody")
    Column {
        options.forEach { (value, label) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    colors = RadioButtonDefaults.colors(selectedColor = ChatColors.Teal)
                )
                Text(label, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
