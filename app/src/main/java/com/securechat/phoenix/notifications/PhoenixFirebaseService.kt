package com.securechat.phoenix.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.securechat.phoenix.R
import com.securechat.phoenix.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging service.
 * Handles data messages in background/killed state.
 * FCM payload: { type, notification_id, sender_id } — NO plaintext ever.
 */
class PhoenixFirebaseService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_MESSAGES = "phoenix_messages"
        const val CHANNEL_CALLS = "phoenix_calls"
        const val CHANNEL_ADMIN = "phoenix_admin"
        const val KEY_REPLY = "key_notification_reply"

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)

                val messagesChannel = NotificationChannel(
                    CHANNEL_MESSAGES, "Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "New message notifications"
                    enableVibration(true)
                }

                val callsChannel = NotificationChannel(
                    CHANNEL_CALLS, "Calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Incoming call notifications"
                    enableVibration(true)
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), null)
                }

                val adminChannel = NotificationChannel(
                    CHANNEL_ADMIN, "Admin Alerts",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Admin notifications" }

                manager.createNotificationChannels(listOf(messagesChannel, callsChannel, adminChannel))
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            NotificationTokenManager.onTokenRefresh(applicationContext, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val type = data["type"] ?: return

        when (type) {
            "message" -> handleMessageNotification(data)
            "call" -> handleCallNotification(data)
            "admin" -> handleAdminNotification(data)
        }
    }

    private fun handleMessageNotification(data: Map<String, String>) {
        val senderId = data["sender_id"] ?: return
        val notificationId = data["notification_id"] ?: return

        // Get user's notification privacy preference
        val prefs = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val privacyMode = prefs.getString("privacy_mode", "show_content") ?: "show_content"

        val (title, body) = when (privacyMode) {
            "show_content" -> {
                // In production: fetch encrypted message from server, decrypt locally
                // For now show sender name
                "New Message" to "You have a new message"
            }
            "show_sender" -> "New Message" to "sent a message"
            else -> "New Message" to ""
        }

        // Deep link intent to open specific chat
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("deep_link", "chat/$senderId")
            putExtra("sender_id", senderId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Inline reply action
        val remoteInput = RemoteInput.Builder(KEY_REPLY)
            .setLabel("Reply")
            .build()
        val replyIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_REPLY"
            putExtra("sender_id", senderId)
            putExtra("notification_id", notificationId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            this, notificationId.hashCode() + 1, replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send, "Reply", replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // Mark as read action
        val readIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_MARK_READ"
            putExtra("sender_id", senderId)
        }
        val readPendingIntent = PendingIntent.getBroadcast(
            this, notificationId.hashCode() + 2, readIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val readAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view, "Mark as Read", readPendingIntent
        ).build()

        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(replyAction)
            .addAction(readAction)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationId.hashCode(), notification)
    }

    private fun handleCallNotification(data: Map<String, String>) {
        val callerId = data["caller_id"] ?: return
        val callType = data["call_type"] ?: "voice"

        // Full-screen intent for incoming call
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("deep_link", "incoming_call")
            putExtra("caller_id", callerId)
            putExtra("call_type", callType)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, callerId.hashCode(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Answer action
        val answerIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_ANSWER_CALL"
            putExtra("caller_id", callerId)
            putExtra("call_type", callType)
        }
        val answerPendingIntent = PendingIntent.getBroadcast(
            this, callerId.hashCode() + 10, answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reject action
        val rejectIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_REJECT_CALL"
            putExtra("caller_id", callerId)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            this, callerId.hashCode() + 11, rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val typeLabel = if (callType == "video") "Video" else "Voice"

        val notification = NotificationCompat.Builder(this, CHANNEL_CALLS)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Incoming $typeLabel Call")
            .setContentText("From user")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reject", rejectPendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Answer", answerPendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(CALL_NOTIFICATION_ID, notification)
    }

    private fun handleAdminNotification(data: Map<String, String>) {
        val alertType = data["alert_type"] ?: "info"
        val message = data["message"] ?: "Admin notification"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("deep_link", "admin")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ADMIN)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Phoenix Admin")
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(alertType.hashCode(), notification)
    }
}

private const val CALL_NOTIFICATION_ID = 9999
