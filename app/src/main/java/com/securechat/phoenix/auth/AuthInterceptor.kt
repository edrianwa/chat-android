package com.securechat.phoenix.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.create
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that:
 * 1. Adds Bearer token to all API requests
 * 2. Auto-refreshes expired tokens on 401
 * 3. Re-authenticates via /auth/device if refresh fails
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    private val noAuthPaths = listOf("/api/auth/login", "/api/auth/register", "/api/auth/refresh", "/api/auth/device")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // Skip auth for auth endpoints
        if (noAuthPaths.any { path.contains(it) }) {
            return chain.proceed(request)
        }

        // Add token
        val token = runBlocking { tokenManager.getAccessToken() }
        val authenticatedRequest = if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        val response = chain.proceed(authenticatedRequest)

        // If 401, try to refresh token and retry
        if (response.code == 401 && token != null) {
            response.close()

            val newToken = runBlocking { refreshToken() }
            if (newToken != null) {
                val retryRequest = request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                return chain.proceed(retryRequest)
            }
        }

        return response
    }

    /**
     * Attempt to refresh the access token using the refresh token.
     * If refresh fails, tries device re-authentication.
     */
    private suspend fun refreshToken(): String? {
        val refreshToken = tokenManager.getRefreshToken() ?: return reAuthenticate()

        try {
            val client = okhttp3.OkHttpClient()
            val body = """{"refreshToken":"$refreshToken"}"""
                .toByteArray()
                .let { okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), it) }
            val request = okhttp3.Request.Builder()
                .url("http://10.0.2.2:3000/api/auth/refresh")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: return reAuthenticate()
                val accessToken = extractJsonField(json, "accessToken")
                val newRefresh = extractJsonField(json, "refreshToken")
                if (accessToken != null && newRefresh != null) {
                    tokenManager.saveTokens(accessToken, newRefresh)
                    return accessToken
                }
            }
        } catch (_: Exception) {}

        return reAuthenticate()
    }

    private suspend fun reAuthenticate(): String? {
        try {
            val client = okhttp3.OkHttpClient()
            val body = """{"deviceId":"auto-reauth","displayName":"Phoenix User"}"""
                .toByteArray()
                .let { okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), it) }
            val request = okhttp3.Request.Builder()
                .url("http://10.0.2.2:3000/api/auth/device")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: return null
                val accessToken = extractJsonField(json, "accessToken")
                val refreshTk = extractJsonField(json, "refreshToken")
                if (accessToken != null && refreshTk != null) {
                    tokenManager.saveTokens(accessToken, refreshTk)
                    return accessToken
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun extractJsonField(json: String, field: String): String? {
        val pattern = """"$field"\s*:\s*"([^"]+)""""
        val match = Regex(pattern).find(json)
        return match?.groupValues?.get(1)
    }
}
