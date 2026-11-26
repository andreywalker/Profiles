package com.example.profiles.data.remote

import com.google.gson.annotations.SerializedName

/** GET /api/health */
data class HealthResponse(
    val status: String,
    @SerializedName("last_sync") val lastSync: String?
)

data class GroupDto(
    val id: Long,
    val name: String,
    @SerializedName("is_default") val isDefault: Boolean
)

data class FieldDefDto(
    val id: Long,
    val name: String,
    val type: String,
    val deletable: Boolean,
    val position: Int,
    @SerializedName("built_in_key") val builtInKey: String?
)

data class PersonDto(
    val id: Long,
    @SerializedName("first_name") val firstName: String,
    val surname: String,
    val name: String,
    @SerializedName("birth_date") val birthDate: String,      // "1999-05-14"
    @SerializedName("group_id") val groupId: Long,
    val met: String,
    val subtitle: String,
    val phone: String?,
    val telegram: String?,
    val instagram: String?,
    @SerializedName("other_media") val otherMedia: String?,
    val values: Map<String, String>                            // field id -> value
)

/** GET /api/backup response, and PUT /api/backup body (syncedAt = null when sending). */
data class BackupDto(
    @SerializedName("synced_at") val syncedAt: String?,
    val groups: List<GroupDto>,
    val fields: List<FieldDefDto>,
    val persons: List<PersonDto>
)

/** PUT /api/backup response */
data class BackupResultDto(
    @SerializedName("synced_at") val syncedAt: String,
    val groups: Int,
    val fields: Int,
    val persons: Int
)

/** Body of 4xx/5xx responses: {"error": "..."} */
data class ErrorDto(val error: String?)
