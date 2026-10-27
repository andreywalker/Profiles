package com.example.profiles.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.profiles.R
import androidx.core.net.toUri

private fun handle(value: String) = value.trim().removePrefix("@")
private fun isLink(value: String) = value.startsWith("http://") || value.startsWith("https://")

fun phoneUri(phone: String) = "tel:" + phone.filter { it.isDigit() || it == '+' }

fun telegramUrl(value: String) =
    if (isLink(value.trim())) value.trim() else "https://t.me/${handle(value)}"

fun instagramUrl(value: String) =
    if (isLink(value.trim())) value.trim() else "https://instagram.com/${handle(value)}"

fun webUrl(value: String) =
    value.trim().let { if (isLink(it)) it else "https://$it" }

fun Context.openUri(uri: String, action: String = Intent.ACTION_VIEW) {
    try {
        startActivity(Intent(action, uri.toUri()))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, getString(R.string.no_app_to_open), Toast.LENGTH_SHORT).show()
    }
}