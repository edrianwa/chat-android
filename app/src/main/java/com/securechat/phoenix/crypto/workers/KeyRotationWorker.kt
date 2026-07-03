package com.securechat.phoenix.crypto.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.securechat.phoenix.crypto.KeyManager
import com.securechat.phoenix.crypto.network.KeyBundleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic worker that rotates the signed pre-key weekly.
 * Also checks and replenishes one-time pre-keys if they're running low.
 */
@HiltWorker
class KeyRotationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val keyManager: KeyManager,
    private val keyBundleRepository: KeyBundleRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Rotate signed pre-key
            val newSignedPreKey = keyManager.rotateSignedPreKey()
            // TODO: Upload new signed pre-key to server
            // This would be a dedicated endpoint or part of the bundle update

            // Check pre-key count and replenish if low
            val countResult = keyBundleRepository.getKeyCount()
            countResult.onSuccess { keyCount ->
                if (keyCount.count < KeyManager.LOW_KEY_THRESHOLD) {
                    val newKeys = keyManager.generateReplenishmentKeys()
                    keyBundleRepository.replenishKeys(newKeys)
                }
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "key_rotation_work"
    }
}
