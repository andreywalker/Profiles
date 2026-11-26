package com.example.profiles.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.profiles.ProfilesApp
import java.util.concurrent.TimeUnit

/** Every 2 hours: check the server and, if it's alive, upload the data. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        (applicationContext as ProfilesApp).syncManager.sync(manual = false)
        // A server that's down is normal for a standby backup: just try again in 2 hours
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(2, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("backup_sync", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
