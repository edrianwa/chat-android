package com.securechat.phoenix.crypto.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules periodic key rotation work.
 * Runs weekly when the device has network connectivity.
 */
object KeyWorkScheduler {

    fun scheduleKeyRotation(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<KeyRotationWorker>(
            7, TimeUnit.DAYS,
            1, TimeUnit.DAYS // Flex interval
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            KeyRotationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelKeyRotation(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(KeyRotationWorker.WORK_NAME)
    }
}
