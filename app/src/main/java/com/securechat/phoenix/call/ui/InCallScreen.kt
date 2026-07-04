package com.securechat.phoenix.call.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.phoenix.call.CallSession
import com.securechat.phoenix.call.CallState
import com.securechat.phoenix.ui.theme.ChatColors
import kotlinx.coroutines.delay

@Composable
fun InCallScreen(
    session: CallSession,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit
) {
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    // Timer
    LaunchedEffect(session.state) {
        if (session.state == CallState.CONNECTED) {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B141A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(48.dp))

            // Top section — avatar, name, status
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(ChatColors.Teal.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        session.remoteDisplayName.take(1).uppercase(),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChatColors.TealLight
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    session.remoteDisplayName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = when (session.state) {
                        CallState.OUTGOING_RINGING -> "Ringing..."
                        CallState.CONNECTING -> "Connecting..."
                        CallState.CONNECTED -> formatDuration(elapsedSeconds)
                        else -> ""
                    },
                    fontSize = 16.sp,
                    color = if (session.state == CallState.CONNECTED) ChatColors.Green
                           else Color.White.copy(alpha = 0.6f)
                )
            }

            // Bottom section — call controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Mute
                    CallControlButton(
                        icon = if (session.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (session.isMuted) "Unmute" else "Mute",
                        isActive = session.isMuted,
                        onClick = onToggleMute
                    )

                    // Speaker
                    CallControlButton(
                        icon = if (session.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        label = if (session.isSpeakerOn) "Speaker" else "Earpiece",
                        isActive = session.isSpeakerOn,
                        onClick = onToggleSpeaker
                    )
                }

                Spacer(Modifier.height(40.dp))

                // End call
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30))
                ) {
                    Icon(
                        Icons.Default.CallEnd, "End call",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isActive) Color.White.copy(alpha = 0.2f) else Color.Transparent)
        ) {
            Icon(
                icon, label,
                tint = if (isActive) ChatColors.Green else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

private fun formatDuration(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "%02d:%02d".format(min, sec)
}
