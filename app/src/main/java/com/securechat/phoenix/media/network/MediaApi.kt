package com.securechat.phoenix.media.network

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit API for media operations.
 */
interface MediaApi {

    @POST("api/media/upload")
    suspend fun upload(
        @Header("x-chat-id") chatId: String,
        @Header("x-file-name") fileName: String,
        @Header("Content-Type") contentType: String,
        @Body body: RequestBody
    ): Response<MediaUploadResponse>

    @GET("api/media/{id}")
    suspend fun download(@Path("id") mediaId: String): Response<ResponseBody>

    @DELETE("api/media/{id}")
    suspend fun delete(@Path("id") mediaId: String): Response<Map<String, String>>

    @GET("api/media/quota/me")
    suspend fun getQuota(): Response<QuotaResponse>

    @PUT("api/media/settings/{chatId}")
    suspend fun setTTL(
        @Path("chatId") chatId: String,
        @Body body: TTLRequest
    ): Response<TTLResponse>

    @GET("api/media/settings/{chatId}")
    suspend fun getTTL(@Path("chatId") chatId: String): Response<TTLResponse>
}

data class MediaUploadResponse(
    val id: String,
    val url: String,
    val fileSize: Int,
    val mimeType: String,
    val expiresAt: String?
)

data class QuotaResponse(
    val allowed: Boolean,
    val used: Long,
    val max: Long
)

data class TTLRequest(val ttlDays: Int?)
data class TTLResponse(val chatId: String, val ttlDays: Int?)
