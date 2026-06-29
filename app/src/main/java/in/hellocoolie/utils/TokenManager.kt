package in.hellocoolie.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import in.hellocoolie.data.model.Porter
import in.hellocoolie.data.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("hellocoolie_prefs")

@Singleton
class TokenManager @Inject constructor(private val context: Context) {

    private val TOKEN    = stringPreferencesKey("token")
    private val ROLE     = stringPreferencesKey("role")
    private val USER     = stringPreferencesKey("user_json")
    private val PORTER   = stringPreferencesKey("porter_json")
    private val LANG     = stringPreferencesKey("lang")

    fun getToken(): String? = runBlocking {
        context.dataStore.data.map { it[TOKEN] }.first()
    }

    fun getRole(): String? = runBlocking {
        context.dataStore.data.map { it[ROLE] }.first()
    }

    fun getLanguage(): String = runBlocking {
        context.dataStore.data.map { it[LANG] ?: "en" }.first()
    }

    suspend fun saveUserSession(token: String, user: User) {
        context.dataStore.edit {
            it[TOKEN]  = token
            it[ROLE]   = "user"
            it[USER]   = Gson().toJson(user)
            it[LANG]   = user.preferredLang
        }
    }

    suspend fun savePorterSession(token: String, porter: Porter) {
        context.dataStore.edit {
            it[TOKEN]  = token
            it[ROLE]   = "porter"
            it[PORTER] = Gson().toJson(porter)
            it[LANG]   = porter.preferredLang
        }
    }

    fun getUser(): User? = runBlocking {
        context.dataStore.data.map { prefs ->
            prefs[USER]?.let { Gson().fromJson(it, User::class.java) }
        }.first()
    }

    fun getPorter(): Porter? = runBlocking {
        context.dataStore.data.map { prefs ->
            prefs[PORTER]?.let { Gson().fromJson(it, Porter::class.java) }
        }.first()
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    fun isLoggedIn(): Boolean = getToken() != null
}

// ── Result wrapper ────────────────────────────────────────
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// ── Safe API call ─────────────────────────────────────────
suspend fun <T> safeApiCall(call: suspend () -> retrofit2.Response<T>): Result<T> {
    return try {
        val response = call()
        if (response.isSuccessful && response.body() != null) {
            Result.Success(response.body()!!)
        } else {
            val errorBody = response.errorBody()?.string()
            val msg = try {
                Gson().fromJson(errorBody, Map::class.java)["error"] as? String ?: "Unknown error"
            } catch (e: Exception) { "Error ${response.code()}" }
            Result.Error(msg)
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error. Check your connection.")
    }
}
