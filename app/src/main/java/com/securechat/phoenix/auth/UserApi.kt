package com.securechat.phoenix.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface UserApi {

    @GET("api/users/search/{idNumber}")
    suspend fun searchUser(@Path("idNumber") idNumber: String): Response<SearchUserResponse>

    @GET("api/users/{id}/profile")
    suspend fun getUserProfile(@Path("id") userId: String): Response<UserProfileResponse>

    @GET("api/users/me/profile")
    suspend fun getMyProfile(): Response<UserProfileResponse>

    @PATCH("api/users/me/profile")
    suspend fun updateProfile(@Body body: Map<String, String>): Response<UserProfileResponse>
}

data class SearchUserResponse(
    val id: String,
    val uniqueId: String,
    val displayName: String,
    val avatarUrl: String?,
    val isOnline: Boolean
)

data class UserProfileResponse(
    val id: String,
    val uniqueId: String,
    val displayName: String,
    val avatarUrl: String?,
    val about: String?,
    val isOnline: Boolean?,
    val lastSeen: Long?
)
