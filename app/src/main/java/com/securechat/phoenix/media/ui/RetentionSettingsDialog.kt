package com.securechat.phoenix.media.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.securechat.phoenix.ui.theme.ChatColors

/**
 * Per-chat media retention settings dialog.
 * Allows user to set auto-delete TTL for media in a specific chat.
 */
@Composable
fun RetentionSettingsDialog(
    currentTTL: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(currentTTL) }

    val options = listOf(
        null to "Keep forever",
        1 to "1 day",
        7 to "7 days",
        30 to "30 days",
        90 to "90 days"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Media auto-delete") },
        text = {
            Column {
                Text(
                    "Choose how long media files are kept on the server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChatColors.TextSecondary
                )
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = value }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == value,
                            onClick = { selected = value },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = ChatColors.Teal
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }) {
                Text("Save", color = ChatColors.Teal)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
