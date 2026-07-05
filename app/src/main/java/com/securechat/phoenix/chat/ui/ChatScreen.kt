package com.securechat.phoenix.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.phoenix.chat.data.MessageEntity
import com.securechat.phoenix.chat.data.MessageStatus
import com.securechat.phoenix.ui.theme.ChatColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMessageScreen(
    chatId: String,
    displayName: String = "",
    messages: List<MessageEntity>,
    isOnline: Boolean = false,
    lastSeen: Long? = null,
    onSendMessage: (String) -> Unit,
    onVoiceCall: () -> Unit = {},
    onVideoCall: () -> Unit = {},
    onAttachMedia: () -> Unit = {},
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isDark = MaterialTheme.colorScheme.background == ChatColors.SurfaceDark

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ChatColors.TealLight.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                chatId.take(1).uppercase().ifEmpty { displayName.take(1).uppercase() },
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                displayName.ifEmpty { chatId.takeLast(8) },
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isOnline) "online" else formatLastSeen(lastSeen),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onVideoCall) {
                        Icon(Icons.Default.Videocam, "Video call", tint = Color.White)
                    }
                    IconButton(onClick = onVoiceCall) {
                        Icon(Icons.Default.Call, "Voice call", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) ChatColors.AppBarDark else ChatColors.Teal
                )
            )
        },
        containerColor = if (isDark) ChatColors.ChatBgDark else ChatColors.ChatBgLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // Messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val grouped = groupMessagesByDate(messages)
                grouped.forEach { (dateSeparator, msgs) ->
                    item {
                        DateSeparator(dateSeparator)
                    }
                    items(msgs) { message ->
                        MessageBubble(message = message, isDark = isDark)
                    }
                }
            }

            // Input bar
            ChatInputBar(
                text = inputText,
                onTextChanged = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText.trim())
                        inputText = ""
                    }
                },
                onAttach = onAttachMedia,
                isDark = isDark
            )
        }
    }
}

@Composable
private fun DateSeparator(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(ChatColors.TextSecondary.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = ChatColors.TextSecondary
            )
        }
    }
}

@Composable
private fun MessageBubble(message: MessageEntity, isDark: Boolean) {
    val isOutgoing = message.isOutgoing
    val bubbleColor = if (isOutgoing) {
        if (isDark) ChatColors.BubbleOutDark else ChatColors.BubbleOutLight
    } else {
        if (isDark) ChatColors.BubbleInDark else ChatColors.BubbleInLight
    }
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 80.dp, max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = if (isOutgoing) 8.dp else 2.dp,
                        bottomEnd = if (isOutgoing) 2.dp else 8.dp
                    )
                )
                .background(bubbleColor)
                .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 4.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    color = if (isDark) ChatColors.TextPrimaryDark else ChatColors.TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMsgTime(message.timestamp),
                        color = ChatColors.TextSecondary,
                        fontSize = 11.sp
                    )
                    if (isOutgoing) {
                        Spacer(Modifier.width(3.dp))
                        MessageStatusIcon(message.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(status: MessageStatus) {
    val (icon, color) = when (status) {
        MessageStatus.SENDING -> null to ChatColors.TickGray
        MessageStatus.SENT -> Icons.Default.Done to ChatColors.TickGray
        MessageStatus.DELIVERED -> Icons.Default.DoneAll to ChatColors.TickGray
        MessageStatus.READ -> Icons.Default.DoneAll to ChatColors.TickBlue
    }
    if (icon != null) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    isDark: Boolean
) {
    val bgColor = if (isDark) ChatColors.AppBarDark else ChatColors.SurfaceLight
    val fieldBg = if (isDark) ChatColors.SurfaceDark else Color(0xFFF0F2F5)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(fieldBg)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.EmojiEmotions, "Emoji",
                    tint = ChatColors.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                if (text.isEmpty()) {
                    Text("Message", color = ChatColors.TextSecondary, fontSize = 16.sp)
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    textStyle = TextStyle(
                        color = if (isDark) ChatColors.TextPrimaryDark else ChatColors.TextPrimary,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(ChatColors.TealLight),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            IconButton(onClick = onAttach, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.AttachFile, "Attach",
                    tint = ChatColors.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        // Send button
        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(ChatColors.Teal)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send, "Send",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// --- Helpers ---

private fun groupMessagesByDate(messages: List<MessageEntity>): List<Pair<String, List<MessageEntity>>> {
    if (messages.isEmpty()) return emptyList()
    val groups = mutableListOf<Pair<String, MutableList<MessageEntity>>>()
    var currentLabel = ""
    for (msg in messages) {
        val label = getDateLabel(msg.timestamp)
        if (label != currentLabel) {
            currentLabel = label
            groups.add(label to mutableListOf(msg))
        } else {
            groups.last().second.add(msg)
        }
    }
    return groups
}

private fun getDateLabel(timestamp: Long): String {
    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return when {
        now.get(Calendar.DATE) == msgCal.get(Calendar.DATE) &&
                now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) -> "Today"
        now.get(Calendar.DATE) - msgCal.get(Calendar.DATE) == 1 &&
                now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) -> "Yesterday"
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatMsgTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun formatLastSeen(timestamp: Long?): String {
    if (timestamp == null) return "offline"
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "last seen just now"
        diff < 3_600_000 -> "last seen ${diff / 60_000} min ago"
        diff < 86_400_000 -> "last seen ${diff / 3_600_000}h ago"
        else -> "last seen ${SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))}"
    }
}
