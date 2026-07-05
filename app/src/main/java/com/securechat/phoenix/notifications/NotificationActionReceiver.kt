package com.securechat.phoenix.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput

/**
 * Handles notification actions: inline reply, mark as read, answer/reject call.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ACTION_REPLY" -> handleReply(context, intent)
            "ACTION_MARK_READ" -> handleMarkRead(context, intent)
            "ACTION_ANSWER_CALL" -> handleAnswerCall(context, intent)
            "ACTION_REJECT_CALL" -> handleRejectCall(context, intent)
        }
    }

    private fun handleReply(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(PhoenixFirebaseService.KEY_REPLY)?.toString()
        val senderId = intent.getStringExtra("sender_id")
        val notificationId = intent.getStringExtra("notification_id")

        if (replyText != null && senderId != null) {
            // Send encrypted reply in background
            // In production: use WorkManager or coroutine to encrypt + send via API
            android.util.Log.d("NotifAction", "Inline reply to $senderId: $replyText")
        }

        // Dismiss notification
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(notificationId.hashCode())
    }

    private fun handleMarkRead(context: Context, intent: Intent) {
        val senderId = intent.getStringExtra("sender_id")
        if (senderId != null) {
            // Mark messages as read via API
            android.util.Log.d("NotifAction", "Marking messages from $senderId as read")
        }

        // Dismiss notification
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()
    }

    private fun handleAnswerCall(context: Context, intent: Intent) {
        val callerId = intent.getStringExtra("caller_id")
        val callType = intent.getStringExtra("call_type") ?: "voice"

        // Launch app directly into call screen
        val launchIntent = Intent(context, com.securechat.phoenix.ui.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("deep_link", "answer_call")
            putExtra("caller_id", callerId)
            putExtra("call_type", callType)
        }
        context.startActivity(launchIntent)

        // Cancel call notification
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(9999)
    }

    private fun handleRejectCall(context: Context, intent: Intent) {
        val callerId = intent.getStringExtra("caller_id")
        android.util.Log.d("NotifAction", "Rejecting call from $callerId")

        // Cancel notification
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(9999)
    }
}
