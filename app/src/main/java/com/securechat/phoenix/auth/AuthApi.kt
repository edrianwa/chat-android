package com.securechat.phoenix.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/device")
    suspend fun deviceAuth(@Body body: DeviceAuthRequest): Response<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<TokensResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Map<String, String>>
}

data class LoginRequest(val uniqueId: String, val password: String)
data class RegisterRequest(val inviteCode: String, val displayName: String, val password: String)
data class DeviceAuthRequest(val deviceId: String, val displayName: String)
data class RefreshRequest(val refreshToken: String)

data class AuthResponse(
    val user: UserData,
    val accessToken: String,
    val refreshToken: String
)

data class TokensResponse(
    val accessToken: String,
    val refreshToken: String
)

data class UserData(
    val id: String,
    val uniqueId: String,
    val displayName: String,
    val role: String
)
