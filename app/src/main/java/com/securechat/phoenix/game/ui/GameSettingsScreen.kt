package com.securechat.phoenix.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Game settings screen - makes the game look like a real indie game.
 * Includes sound toggle, difficulty selector, and high score reset.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSettingsScreen(
    onBack: () -> Unit,
    viewModel: GameSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    val darkBackground = Color(0xFF1A1A2E)
    val cardColor = Color(0xFF16213E)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBackground)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sound settings
            SettingsCard(cardColor) {
                Text(
                    "Audio",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFF6B35),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sound Effects", color = Color.White)
                    Switch(
                        checked = state.soundEnabled,
                        onCheckedChange = viewModel::onSoundToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFF6B35),
                            checkedTrackColor = Color(0x55FF6B35)
                        )
                    )
                }
            }

            // Difficulty settings
            SettingsCard(cardColor) {
                Text(
                    "Difficulty",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFF6B35),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val options = listOf("Easy", "Normal", "Hard")
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = state.difficulty == index,
                            onClick = { viewModel.onDifficultyChange(index) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Color(0xFFFF6B35),
                                activeContentColor = Color.White,
                                inactiveContainerColor = Color(0xFF0D0221),
                                inactiveContentColor = Color.White.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(label)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (state.difficulty) {
                        0 -> "Wider gaps, slower speed. Great for beginners!"
                        2 -> "Narrow gaps, faster speed. For true phoenixes only."
                        else -> "Standard challenge. The classic experience."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            // High score & data
            SettingsCard(cardColor) {
                Text(
                    "Data",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFF6B35),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("High Score", color = Color.White)
                        Text(
                            "${state.highScore}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = { showResetDialog = true }) {
                        Text("Reset", color = Color(0xFFFF4500))
                    }
                }
            }

            // About section
            SettingsCard(cardColor) {
                Text(
                    "About",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFF6B35),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Flying Phoenix v1.0.0",
                    color = Color.White
                )
                Text(
                    "A fiery endless runner",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Tap to fly, avoid the fire pillars!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }

    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset High Score?") },
            text = { Text("This will permanently clear your best score. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onResetHighScore()
                    showResetDialog = false
                }) {
                    Text("Reset", color = Color(0xFFFF4500))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(cardColor: Color, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
