package com.securechat.phoenix.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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

data class ChatStorageInfo(
    val chatId: String,
    val displayName: String,
    val sizeBytes: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    totalUsedBytes: Long = 0,
    maxBytes: Long = 524_288_000, // 500 MB
    perChatStorage: List<ChatStorageInfo> = emptyList(),
    onBack: () -> Unit,
    onClearCache: () -> Unit = {},
    onClearAllMedia: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage & Data", color = Color.White, fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total usage
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Storage Used", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = (totalUsedBytes.toFloat() / maxBytes.toFloat()).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = ChatColors.Teal,
                            trackColor = ChatColors.Teal.copy(alpha = 0.1f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${formatBytes(totalUsedBytes)} / ${formatBytes(maxBytes)}",
                            fontSize = 13.sp,
                            color = ChatColors.TextSecondary
                        )
                    }
                }
            }

            // Actions
            item {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onClearCache,
                        colors = ButtonDefaults.buttonColors(containerColor = ChatColors.Teal),
                        modifier = Modifier.weight(1f)
                    ) { Text("Clear Cache") }
                    Button(
                        onClick = onClearAllMedia,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                        modifier = Modifier.weight(1f)
                    ) { Text("Clear Media") }
                }
            }

            // Per-chat breakdown
            if (perChatStorage.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Per Chat", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                items(perChatStorage.sortedByDescending { it.sizeBytes }) { chat ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(chat.displayName, modifier = Modifier.weight(1f))
                        Text(formatBytes(chat.sizeBytes), fontSize = 13.sp, color = ChatColors.TextSecondary)
                    }
                    Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
