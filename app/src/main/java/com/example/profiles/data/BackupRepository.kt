package com.example.profiles.data

import com.example.profiles.data.remote.ApiClient
import com.example.profiles.data.remote.BackupDto
import com.example.profiles.data.remote.BackupResultDto
import com.example.profiles.data.remote.ErrorDto
import com.example.profiles.data.remote.FieldDefDto
import com.example.profiles.data.remote.GroupDto
import com.example.profiles.data.remote.HealthResponse
import com.example.profiles.data.remote.PersonDto
import com.example.profiles.data.remote.ProfilesApi
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

/** Result of one API call. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data object Unreachable : ApiResult<Nothing>                          // no connection / timeout
    data class HttpError(val code: Int, val message: String?) : ApiResult<Nothing>
    data class Failure(val message: String) : ApiResult<Nothing>          // anything else
}

/** The only place that talks to the backup server. ViewModels never call Retrofit directly. */
class BackupRepository(private val local: ProfilesRepository) {

    suspend fun serverUrl(): String =
        local.observeSetting(KEY_SERVER_URL).first() ?: DEFAULT_SERVER_URL

    suspend fun checkServer(): ApiResult<HealthResponse> = call { it.health() }

    suspend fun download(): ApiResult<BackupDto> = call { it.getBackup() }

    suspend fun upload(): ApiResult<BackupResultDto> {
        val data = local.exportAll().toDto()
        return call { it.putBackup(data) }
    }

    suspend fun saveLastSync(time: Long) = local.setSetting(KEY_LAST_SYNC, time.toString())

    private suspend fun <T> call(request: suspend (ProfilesApi) -> Response<T>): ApiResult<T> =
        withContext(Dispatchers.IO) {
            try {
                val response = request(ApiClient.get(serverUrl()))
                val body = response.body()
                if (response.isSuccessful && body != null) ApiResult.Success(body)
                else ApiResult.HttpError(response.code(), errorMessage(response))
            } catch (e: CancellationException) {
                throw e                                   // never swallow coroutine cancellation
            } catch (e: IOException) {
                ApiResult.Unreachable
            } catch (e: Exception) {
                ApiResult.Failure(e.message ?: e.javaClass.simpleName)
            }
        }

    private fun errorMessage(response: Response<*>): String? = try {
        response.errorBody()?.string()?.let { Gson().fromJson(it, ErrorDto::class.java)?.error }
    } catch (e: Exception) {
        null                                              // body wasn't our JSON
    }

    companion object {
        const val KEY_SERVER_URL = "server_url"
        const val KEY_LAST_SYNC = "last_sync"
        const val DEFAULT_SERVER_URL = "http://10.0.2.2:8000/"   // the computer, seen from the emulator
    }
}

/** Local Room data -> API data classes. Photos stay on the phone. */
private fun LocalData.toDto(): BackupDto {
    val valuesByPerson = values.groupBy { it.personId }
    return BackupDto(
        syncedAt = null,
        groups = groups.map { GroupDto(it.id, it.name, it.isDefault) },
        fields = fields.map {
            FieldDefDto(it.id, it.name, it.type.name, it.deletable, it.position, it.builtInKey)
        },
        persons = persons.map { p ->
            PersonDto(
                id = p.id,
                firstName = p.firstName,
                surname = p.surname,
                name = p.name,
                birthDate = p.birthDate.toString(),
                groupId = p.groupId,
                met = p.met,
                subtitle = p.subtitle,
                phone = p.phone,
                telegram = p.telegram,
                instagram = p.instagram,
                otherMedia = p.otherMedia,
                values = valuesByPerson[p.id].orEmpty().associate { it.fieldId.toString() to it.value }
            )
        }
    )
}
