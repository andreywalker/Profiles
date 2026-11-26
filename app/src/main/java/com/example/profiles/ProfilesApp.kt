package com.example.profiles

import android.app.Application
import com.example.profiles.data.AppDb
import com.example.profiles.data.BackupRepository
import com.example.profiles.data.PhotoStorage
import com.example.profiles.data.ProfilesRepository
import com.example.profiles.notifications.BirthdayWorker
import com.example.profiles.notifications.NotificationHelper
import com.example.profiles.sync.SyncManager
import com.example.profiles.sync.SyncWorker
import com.example.profiles.utils.Messenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ProfilesApp : Application() {

    /** Lives as long as the app; used for work that must not stop with a screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDb by lazy { AppDb.create(this) }
    val repository: ProfilesRepository by lazy { ProfilesRepository(database) }
    val photos: PhotoStorage by lazy { PhotoStorage(this) }
    val messenger: Messenger by lazy { Messenger() }
    val backupRepository: BackupRepository by lazy { BackupRepository(repository) }
    val syncManager: SyncManager by lazy {
        SyncManager(backupRepository, messenger, database, appScope)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        BirthdayWorker.schedule(this)
        BirthdayWorker.runNow(this)
        SyncWorker.schedule(this)
    }
}