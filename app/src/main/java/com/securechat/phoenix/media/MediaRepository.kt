package com.securechat.phoenix.media

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.securechat.phoenix.media.compression.ImageCompressor
import com.securechat.phoenix.media.crypto.MediaCrypto
import com.securechat.phoenix.media.network.MediaApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository managing the full media lifecycle:
 * compress → encrypt → upload → store key in message payload.
 * download → decrypt → display.
 */
@Singleton
class MediaRepository @Inject constructor(
    private val mediaApi: MediaApi,
    private val context: Context
) {
    private val _uploadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val uploadProgress: StateFlow<Map<String, Float>> = _uploadProgress

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress

    /**
     * Full upload flow: compress → encrypt → upload → return MediaPayload for message.
     */
    suspend fun uploadImage(
        uri: Uri,
        chatId: String,
        messageId: String
    ): MediaPayload? = withContext(Dispatchers.IO) {
        try {
            updateUploadProgress(messageId, 0.1f)

            // Compress
            val compressed = ImageCompressor.compressImage(context, uri)
                ?: return@withContext null
            updateUploadProgress(messageId, 0.3f)

            // Encrypt
            val key = MediaCrypto.generateKey()
            val encrypted = MediaCrypto.encrypt(compressed.fullBytes, key)
            updateUploadProgress(messageId, 0.5f)

            // Upload
            val requestBody = encrypted.encryptedBytes.toRequestBody()
            val response = mediaApi.upload(
                chatId = chatId,
                fileName = "$messageId.webp.enc",
                contentType = "application/octet-stream",
                body = requestBody
            )
            updateUploadProgress(messageId, 0.9f)

            if (!response.isSuccessful || response.body() == null) {
                updateUploadProgress(messageId, -1f)
                return@withContext null
            }

            updateUploadProgress(messageId, 1f)

            val uploadResult = response.body()!!
            MediaPayload(
                mediaId = uploadResult.id,
                mediaUrl = uploadResult.url,
                encryptionKey = Base64.encodeToString(key, Base64.NO_WRAP),
                mimeType = compressed.mimeType,
                fileSize = compressed.fullBytes.size,
                width = compressed.width,
                height = compressed.height,
                thumbnail = Base64.encodeToString(compressed.thumbnailBytes, Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            updateUploadProgress(messageId, -1f)
            null
        }
    }

    /**
     * Download and decrypt a media file.
     */
    suspend fun downloadMedia(
        mediaId: String,
        encryptionKey: String
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            updateDownloadProgress(mediaId, 0.2f)

            val response = mediaApi.download(mediaId)
            if (!response.isSuccessful || response.body() == null) {
                updateDownloadProgress(mediaId, -1f)
                return@withContext null
            }

            updateDownloadProgress(mediaId, 0.7f)

            val encryptedBytes = response.body()!!.bytes()
            val key = Base64.decode(encryptionKey, Base64.NO_WRAP)
            val decrypted = MediaCrypto.decrypt(encryptedBytes, key)

            updateDownloadProgress(mediaId, 1f)
            decrypted
        } catch (e: Exception) {
            e.printStackTrace()
            updateDownloadProgress(mediaId, -1f)
            null
        }
    }

    /**
     * Delete media from server.
     */
    suspend fun deleteMedia(mediaId: String): Boolean {
        return try {
            val response = mediaApi.delete(mediaId)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Set per-chat media TTL.
     */
    suspend fun setChatTTL(chatId: String, ttlDays: Int?): Boolean {
        return try {
            val response = mediaApi.setTTL(chatId, com.securechat.phoenix.media.network.TTLRequest(ttlDays))
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun updateUploadProgress(messageId: String, progress: Float) {
        _uploadProgress.value = _uploadProgress.value.toMutableMap().apply { put(messageId, progress) }
    }

    private fun updateDownloadProgress(mediaId: String, progress: Float) {
        _downloadProgress.value = _downloadProgress.value.toMutableMap().apply { put(mediaId, progress) }
    }
}

/**
 * Media payload sent as part of the encrypted message to the recipient.
 * Contains everything needed to download and decrypt the media.
 */
data class MediaPayload(
    val mediaId: String,
    val mediaUrl: String,
    val encryptionKey: String,  // Base64 AES-256 key
    val mimeType: String,
    val fileSize: Int,
    val width: Int,
    val height: Int,
    val thumbnail: String       // Base64 blurred low-res preview
)
