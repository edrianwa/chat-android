package com.securechat.phoenix.crypto.network

import com.securechat.phoenix.crypto.models.FetchedKeyBundle
import com.securechat.phoenix.crypto.models.KeyBundle
import com.securechat.phoenix.crypto.models.KeyCountResponse
import com.securechat.phoenix.crypto.models.PreKeyData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit API interface for key bundle operations.
 */
interface KeyBundleApi {

    @POST("api/keys/bundle")
    suspend fun uploadBundle(@Body bundle: KeyBundle): Response<Map<String, String>>

    @GET("api/keys/bundle/{userId}")
    suspend fun fetchBundle(@Path("userId") userId: String): Response<FetchedKeyBundle>

    @GET("api/keys/count")
    suspend fun getKeyCount(): Response<KeyCountResponse>

    @POST("api/keys/replenish")
    suspend fun replenishKeys(@Body body: ReplenishRequest): Response<ReplenishResponse>
}

data class ReplenishRequest(val preKeys: List<PreKeyData>)
data class ReplenishResponse(val message: String, val count: Int)
