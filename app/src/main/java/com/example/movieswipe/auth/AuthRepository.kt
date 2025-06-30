package com.example.movieswipe.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.movieswipe.core.AuthConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AuthRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    suspend fun authenticateWithGoogle(idToken: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply { put("token", idToken) }
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("${AuthConfig.BACKEND_BASE_URL}/auth/google")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Backend error: ${response.code}"))
            }
            val responseBody = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val obj = JSONObject(responseBody)
            val jwt = obj.getString("token")
            val user = obj.getJSONObject("user")
            saveJwt(jwt)
            Result.success(
                AuthResult(
                    jwt = jwt,
                    user = UserProfile(
                        id = user.getString("id"),
                        googleId = user.getString("googleId"),
                        email = user.getString("email"),
                        name = user.getString("name"),
                        profilePicture = user.optString("profilePicture", null)
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun saveJwt(jwt: String) {
        prefs.edit().putString("jwt", jwt).apply()
    }

    fun getJwt(): String? = prefs.getString("jwt", null)

    fun clearJwt() {
        prefs.edit().remove("jwt").apply()
    }
}

data class AuthResult(val jwt: String, val user: UserProfile)
data class UserProfile(
    val id: String,
    val googleId: String,
    val email: String,
    val name: String,
    val profilePicture: String?
)
