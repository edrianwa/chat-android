package com.securechat.phoenix.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

data class PasscodeEntry(
    val passcode: String, // masked: "••••56"
    val destination: String, // "Secure Chat" or "Flying Phoenix"
    val screenId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasscodeSettingsScreen(
    passcodes: List<PasscodeEntry>,
    onBack: () -> Unit,
    onChangePasscode: (String) -> Unit = {},
    onSwapDestinations: () -> Unit = {},
    onDeletePasscode: (String) -> Unit = {},
    onAddPasscode: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Passcode & Security", color = Color.White, fontWeight = FontWeight.Bold) },
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
            item {
                Text("Passcode Mappings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Each passcode opens a different screen. Minimum 2 required.",
                    fontSize = 13.sp,
                    color = ChatColors.TextSecondary
                )
                Spacer(Modifier.height(12.dp))
            }

            items(passcodes) { entry ->
                PasscodeCard(
                    entry = entry,
                    canDelete = passcodes.size > 2,
                    onChangePasscode = { onChangePasscode(entry.screenId) },
                    onDelete = { onDeletePasscode(entry.screenId) }
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        onClick = onSwapDestinations,
                        colors = ButtonDefaults.buttonColors(containerColor = ChatColors.Teal)
                    ) {
                        Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.padding(end = 4.dp))
                        Text("Swap Destinations")
                    }
                    Button(
                        onClick = onAddPasscode,
                        colors = ButtonDefaults.buttonColors(containerColor = ChatColors.Green)
                    ) {
                        Text("+ Add")
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                Text("Security Notes", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "• Two passcodes must always exist (can't delete all)\n" +
                    "• Different passcodes can't point to the same screen\n" +
                    "• The decoy passcode shows the game to anyone watching\n" +
                    "• Your real chat is only accessible with the correct code",
                    fontSize = 12.sp,
                    color = ChatColors.TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun PasscodeCard(
    entry: PasscodeEntry,
    canDelete: Boolean,
    onChangePasscode: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.passcode, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "→ ${entry.destination}",
                    fontSize = 14.sp,
                    color = ChatColors.TealLight,
                    fontWeight = FontWeight.Medium
                )
            }
            Button(
                onClick = onChangePasscode,
                colors = ButtonDefaults.buttonColors(containerColor = ChatColors.Teal.copy(alpha = 0.1f))
            ) {
                Text("Change", color = ChatColors.Teal, fontSize = 12.sp)
            }
            if (canDelete) {
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}
