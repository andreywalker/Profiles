package com.example.profiles.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface ProfilesApi {

    @GET("api/health")
    suspend fun health(): Response<HealthResponse>

    @GET("api/backup")
    suspend fun getBackup(): Response<BackupDto>

    @PUT("api/backup")
    suspend fun putBackup(@Body backup: BackupDto): Response<BackupResultDto>
}
