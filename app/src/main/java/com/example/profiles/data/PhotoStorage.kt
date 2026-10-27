package com.example.profiles.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

class PhotoStorage(private val context: Context) {
    private val dir = File(context.filesDir, "photos").apply { mkdirs() }

    fun save(uri: Uri): String {
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        return file.absolutePath
    }

    fun delete(path: String?) {
        path?.let { File(it).delete() }
    }
}