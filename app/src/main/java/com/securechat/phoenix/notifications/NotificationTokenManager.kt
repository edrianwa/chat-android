package com.securechat.phoenix.notifications

import android.content.Context
import android.provider.Settings
import com.securechat.phoenix.auth.TokenManager
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Manages FCM token registration with the server.
 */
object NotificationTokenManager {

    private const val BASE_URL = "http://10.0.2.2:3000"

    /**
     * Called when FCM token refreshes.
     * Sends the new token to the server.
     */
    suspend fun onTokenRefresh(context: Context, token: String) {
        val deviceId = getDeviceId(context)
        registerWithServer(context, token, deviceId)
    }

    /**
     * Register the current FCM token with the server.
     */
    suspend fun registerCurrentToken(context: Context, fcmToken: String, authToken: String) {
        val deviceId = getDeviceId(context)
        try {
            val client = OkHttpClient()
            val body = """{"token":"$fcmToken","deviceId":"$deviceId"}"""
                .toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$BASE_URL/api/notifications/register-token")
                .header("Authorization", "Bearer $authToken")
                .post(body)
                .build()
            client.newCall(request).execute()
        } catch (_: Exception) {}
    }

    /**
     * Unregister token on logout.
     */
    suspend fun unregisterToken(context: Context, authToken: String) {
        val deviceId = getDeviceId(context)
        try {
            val client = OkHttpClient()
            val body = """{"deviceId":"$deviceId"}"""
                .toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$BASE_URL/api/notifications/token")
                .header("Authorization", "Bearer $authToken")
                .delete(body)
                .build()
            client.newCall(request).execute()
        } catch (_: Exception) {}
    }

    private suspend fun registerWithServer(context: Context, token: String, deviceId: String) {
        // Get auth token from storage
        val prefs = context.getSharedPreferences("auth_tokens", Context.MODE_PRIVATE)
        val authToken = prefs.getString("access_token", null) ?: return

        try {
            val client = OkHttpClient()
            val body = """{"token":"$token","deviceId":"$deviceId"}"""
                .toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$BASE_URL/api/notifications/register-token")
                .header("Authorization", "Bearer $authToken")
                .post(body)
                .build()
            client.newCall(request).execute()
        } catch (_: Exception) {}
    }

    private fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }
}
