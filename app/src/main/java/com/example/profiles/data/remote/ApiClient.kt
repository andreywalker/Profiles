package com.example.profiles.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Builds the Retrofit API for the server address from Settings (rebuilt only when it changes). */
object ApiClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)    // server down -> fail fast
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private var cached: Pair<String, ProfilesApi>? = null

    @Synchronized
    fun get(serverUrl: String): ProfilesApi {
        val url = normalize(serverUrl)
        cached?.let { (cachedUrl, api) -> if (cachedUrl == url) return api }

        val api = Retrofit.Builder()
            .baseUrl(url)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProfilesApi::class.java)
        cached = url to api
        return api
    }

    /** "192.168.1.10:8000" -> "http://192.168.1.10:8000/" */
    fun normalize(url: String): String {
        var u = url.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
        if (!u.endsWith("/")) u += "/"
        return u
    }
}
