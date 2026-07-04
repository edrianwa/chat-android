package com.securechat.phoenix.call.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.phoenix.call.CallSession
import com.securechat.phoenix.call.CallState
import com.securechat.phoenix.call.ConnectionQuality
import com.securechat.phoenix.ui.theme.ChatColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Full video call screen with remote video, local PiP preview, and controls.
 */
@Composable
fun VideoCallScreen(
    session: CallSession,
    isVideoEnabled: Boolean,
    remoteVideoEnabled: Boolean,
    connectionQuality: ConnectionQuality,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleVideo: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit
) {
    var showControls by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    // Auto-hide controls after 3 seconds
    LaunchedEffect(showControls) {
        if (showControls && session.state == CallState.CONNECTED) {
            delay(3000)
            showControls = false
        }
    }

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
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(onDrag = { _, _ -> }) // Capture to detect taps
                // Simple tap toggles controls
            }
    ) {
        // Remote video area (or avatar when video is off)
        if (remoteVideoEnabled) {
            // In production: SurfaceViewRenderer composable here
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A2E)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Remote Video",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 16.sp
                )
            }
        } else {
            // Remote has camera off — show avatar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B141A)),
                contentAlignment = Alignment.Center
            ) {
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
                    Spacer(Modifier.height(12.dp))
                    Text(session.remoteDisplayName, color = Color.White, fontSize = 20.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Camera off", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }
        }

        // Local PiP preview (draggable)
        if (isVideoEnabled) {
            DraggableLocalPreview()
        }

        // Top bar (status + quality)
        AnimatedVisibility(
            visible = showControls || session.state != CallState.CONNECTED,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(session.remoteDisplayName, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        when (session.state) {
                            CallState.OUTGOING_RINGING -> "Ringing..."
                            CallState.CONNECTING -> "Connecting..."
                            CallState.CONNECTED -> formatDuration(elapsedSeconds)
                            else -> ""
                        },
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
                // Connection quality indicator
                QualityIndicator(connectionQuality)
            }
        }

        // Bottom controls
        AnimatedVisibility(
            visible = showControls || session.state != CallState.CONNECTED,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    VideoControlButton(
                        icon = if (session.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = "Mute",
                        isActive = session.isMuted,
                        onClick = onToggleMute
                    )
                    VideoControlButton(
                        icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        label = "Camera",
                        isActive = !isVideoEnabled,
                        onClick = onToggleVideo
                    )
                    VideoControlButton(
                        icon = Icons.Default.Cameraswitch,
                        label = "Flip",
                        isActive = false,
                        onClick = onSwitchCamera
                    )
                    VideoControlButton(
                        icon = Icons.Default.VolumeUp,
                        label = "Speaker",
                        isActive = session.isSpeakerOn,
                        onClick = onToggleSpeaker
                    )
                }

                Spacer(Modifier.height(20.dp))

                // End call
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30))
                ) {
                    Icon(
                        Icons.Default.CallEnd, "End",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggableLocalPreview() {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .padding(16.dp)
            .size(width = 120.dp, height = 160.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .background(Color(0xFF1A2942))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // In production: local SurfaceViewRenderer
        Text("You", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}

@Composable
private fun QualityIndicator(quality: ConnectionQuality) {
    val (icon, color) = when (quality) {
        ConnectionQuality.GOOD -> Icons.Default.SignalCellular4Bar to ChatColors.Green
        ConnectionQuality.FAIR -> Icons.Default.SignalCellularAlt to Color(0xFFFFB300)
        ConnectionQuality.POOR -> Icons.Default.SignalCellularAlt1Bar to Color(0xFFFF3B30)
    }
    Icon(icon, "Signal: ${quality.name}", tint = color, modifier = Modifier.size(20.dp))
}

@Composable
private fun VideoControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isActive) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f))
        ) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}

private fun formatDuration(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "%02d:%02d".format(min, sec)
}
