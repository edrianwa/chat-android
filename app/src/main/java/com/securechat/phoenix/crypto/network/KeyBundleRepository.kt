package com.securechat.phoenix.crypto.network

import com.securechat.phoenix.crypto.models.FetchedKeyBundle
import com.securechat.phoenix.crypto.models.KeyBundle
import com.securechat.phoenix.crypto.models.KeyCountResponse
import com.securechat.phoenix.crypto.models.PreKeyData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that handles communication with the server for key operations.
 */
@Singleton
class KeyBundleRepository @Inject constructor(
    private val api: KeyBundleApi
) {
    /**
     * Upload a full key bundle to the server.
     */
    suspend fun uploadBundle(bundle: KeyBundle): Result<Unit> {
        return try {
            val response = api.uploadBundle(bundle)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch another user's key bundle for session establishment.
     */
    suspend fun fetchBundle(userId: String): Result<FetchedKeyBundle> {
        return try {
            val response = api.fetchBundle(userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Fetch failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the current one-time pre-key count from server.
     */
    suspend fun getKeyCount(): Result<KeyCountResponse> {
        return try {
            val response = api.getKeyCount()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Count fetch failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload additional one-time pre-keys to replenish the server's supply.
     */
    suspend fun replenishKeys(preKeys: List<PreKeyData>): Result<Int> {
        return try {
            val response = api.replenishKeys(ReplenishRequest(preKeys))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.count)
            } else {
                Result.failure(Exception("Replenish failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
