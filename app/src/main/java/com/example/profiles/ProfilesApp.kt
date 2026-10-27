package com.example.profiles

import android.app.Application
import com.example.profiles.data.AppDb
import com.example.profiles.data.PhotoStorage
import com.example.profiles.data.ProfilesRepository
import com.example.profiles.notifications.BirthdayWorker
import com.example.profiles.notifications.NotificationHelper

class ProfilesApp : Application() {

    val database: AppDb by lazy { AppDb.create(this) }
    val repository: ProfilesRepository by lazy { ProfilesRepository(database) }
    val photos: PhotoStorage by lazy { PhotoStorage(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        BirthdayWorker.schedule(this)
        BirthdayWorker.runNow(this)
    }
}