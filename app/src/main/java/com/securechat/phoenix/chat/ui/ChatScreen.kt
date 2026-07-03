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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMessageScreen(
    chatId: String,
    messages: List<MessageEntity>,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16213E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                chatId.take(2).uppercase(),
                                color = Color(0xFFFF6B35),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "User ${chatId.takeLast(6)}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "End-to-end encrypted",
                                color = Color(0xFFFF6B35).copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
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
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = Color(0xFF0D0221)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message = message)
                }
            }

            // Input bar
            MessageInputBar(
                text = inputText,
                onTextChanged = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText.trim())
                        inputText = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun MessageBubble(message: MessageEntity) {
    val isOutgoing = message.isOutgoing
    val bubbleColor = if (isOutgoing) Color(0xFF16213E) else Color(0xFF1A2942)
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isOutgoing) 12.dp else 4.dp,
                        bottomEnd = if (isOutgoing) 4.dp else 12.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = formatMessageTime(message.timestamp),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIcon(message.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(status: MessageStatus) {
    when (status) {
        MessageStatus.SENDING -> {
            // Clock icon or nothing
        }
        MessageStatus.SENT -> {
            Icon(
                Icons.Default.Done,
                contentDescription = "Sent",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
        MessageStatus.DELIVERED -> {
            Icon(
                Icons.Default.DoneAll,
                contentDescription = "Delivered",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
        MessageStatus.READ -> {
            Icon(
                Icons.Default.DoneAll,
                contentDescription = "Read",
                tint = Color(0xFF4FC3F7), // Blue ticks
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun MessageInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF16213E))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (text.isEmpty()) {
                Text(
                    "Type a message...",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 15.sp
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChanged,
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Color(0xFFFF6B35)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (text.isNotBlank()) Color(0xFFFF6B35) else Color(0xFF16213E)
                )
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
