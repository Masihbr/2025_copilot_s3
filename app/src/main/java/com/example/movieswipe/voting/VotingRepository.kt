package com.example.movieswipe.voting

import android.content.Context
import com.example.movieswipe.core.AuthConfig
import com.example.movieswipe.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class VotingRepository(private val context: Context) {
    private val client = OkHttpClient()
    private val authRepo = AuthRepository(context)

    suspend fun createVotingSession(groupId: String): Result<VotingSession> = withContext(Dispatchers.IO) {
        val jwt = authRepo.getJwt() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val json = JSONObject().apply { put("groupId", groupId) }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("${AuthConfig.BACKEND_BASE_URL}/voting/sessions")
            .post(body)
            .addHeader("Authorization", "Bearer $jwt")
            .build()
        try {
            val response = client.newCall(request).execute()
            if (response.code == 400) return@withContext Result.failure(Exception("Bad request"))
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to create voting session"))
            val obj = JSONObject(response.body?.string() ?: return@withContext Result.failure(Exception("Empty response")))
            val session = VotingSession.fromJson(obj.getJSONObject("data"))
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveSession(groupId: String): Result<VotingSession?> = withContext(Dispatchers.IO) {
        val jwt = authRepo.getJwt() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val request = Request.Builder()
            .url("${AuthConfig.BACKEND_BASE_URL}/voting/sessions/active/$groupId")
            .get()
            .addHeader("Authorization", "Bearer $jwt")
            .build()
        try {
            val response = client.newCall(request).execute()
            if (response.code == 404) return@withContext Result.success(null)
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to get active session"))
            val obj = JSONObject(response.body?.string() ?: return@withContext Result.failure(Exception("Empty response")))
            val session = VotingSession.fromJson(obj.getJSONObject("data"))
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startSession(sessionId: String): Result<VotingSession> = withContext(Dispatchers.IO) {
        val jwt = authRepo.getJwt() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val request = Request.Builder()
            .url("${AuthConfig.BACKEND_BASE_URL}/voting/sessions/$sessionId/start")
            .post("".toRequestBody())
            .addHeader("Authorization", "Bearer $jwt")
            .build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to start session"))
            val obj = JSONObject(response.body?.string() ?: return@withContext Result.failure(Exception("Empty response")))
            val session = VotingSession.fromJson(obj.getJSONObject("data"))
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Data models for VotingSession and Movie

data class VotingSession(
    val id: String,
    val groupId: String,
    val status: String,
    val movies: List<Movie>,
    val startTime: String?,
    val endTime: String?,
    val selectedMovie: Movie?,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromJson(obj: JSONObject): VotingSession = VotingSession(
            id = obj.getString("id"),
            groupId = obj.getString("groupId"),
            status = obj.getString("status"),
            movies = obj.optJSONArray("movies")?.let { arr ->
                (0 until arr.length()).map { Movie.fromJson(arr.getJSONObject(it)) }
            } ?: emptyList(),
            startTime = obj.optString("startTime", null),
            endTime = obj.optString("endTime", null),
            selectedMovie = obj.optJSONObject("selectedMovie")?.let { Movie.fromJson(it) },
            createdAt = obj.getString("createdAt"),
            updatedAt = obj.getString("updatedAt")
        )
    }
}

data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val genres: List<MovieGenre>,
    val voteAverage: Double?,
    val voteCount: Int?,
    val runtime: Int?,
    val tagline: String?
) {
    companion object {
        fun fromJson(obj: JSONObject): Movie = Movie(
            id = obj.getInt("id"),
            title = obj.getString("title"),
            overview = obj.getString("overview"),
            posterPath = obj.optString("posterPath", null),
            backdropPath = obj.optString("backdropPath", null),
            releaseDate = obj.optString("releaseDate", null),
            genres = obj.optJSONArray("genres")?.let { arr ->
                (0 until arr.length()).map { MovieGenre.fromJson(arr.getJSONObject(it)) }
            } ?: emptyList(),
            voteAverage = obj.optDouble("voteAverage"),
            voteCount = obj.optInt("voteCount"),
            runtime = if (obj.has("runtime") && !obj.isNull("runtime")) obj.getInt("runtime") else null,
            tagline = obj.optString("tagline", null)
        )
    }
}

data class MovieGenre(
    val id: Int,
    val name: String
) {
    companion object {
        fun fromJson(obj: JSONObject): MovieGenre = MovieGenre(
            id = obj.getInt("id"),
            name = obj.getString("name")
        )
    }
}
