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

    suspend fun castVote(sessionId: String, movieId: Int, vote: String): Result<Map<Int, String>> = withContext(Dispatchers.IO) {
        val jwt = authRepo.getJwt() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val json = JSONObject().apply {
            put("movieId", movieId)
            put("vote", vote)
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("${AuthConfig.BACKEND_BASE_URL}/voting/sessions/$sessionId/votes")
            .post(body)
            .addHeader("Authorization", "Bearer $jwt")
            .build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to cast vote"))
            val obj = JSONObject(response.body?.string() ?: return@withContext Result.failure(Exception("Empty response")))
            val userVotesObj = obj.optJSONObject("userVotes") ?: JSONObject()
            val userVotes = mutableMapOf<Int, String>()
            userVotesObj.keys().forEach { key ->
                userVotes[key.toInt()] = userVotesObj.getString(key)
            }
            Result.success(userVotes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSessionDetails(sessionId: String): Result<VotingSessionDetails> = withContext(Dispatchers.IO) {
        val jwt = authRepo.getJwt() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val request = Request.Builder()
            .url("${AuthConfig.BACKEND_BASE_URL}/voting/sessions/$sessionId")
            .get()
            .addHeader("Authorization", "Bearer $jwt")
            .build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to get session details"))
            val obj = JSONObject(response.body?.string() ?: return@withContext Result.failure(Exception("Empty response")))
            val data = obj.getJSONObject("data")
            Result.success(VotingSessionDetails.fromJson(data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun endSession(sessionId: String): Result<MovieSelectionResults> = withContext(Dispatchers.IO) {
        val jwt = authRepo.getJwt() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val request = Request.Builder()
            .url("${AuthConfig.BACKEND_BASE_URL}/voting/sessions/$sessionId/end")
            .post("".toRequestBody())
            .addHeader("Authorization", "Bearer $jwt")
            .build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to end session"))
            val obj = JSONObject(response.body?.string() ?: return@withContext Result.failure(Exception("Empty response")))
            val data = obj.getJSONObject("data")
            Result.success(MovieSelectionResults.fromJson(data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovieSelectionResults(sessionId: String): Result<MovieSelectionResults> = withContext(Dispatchers.IO) {
        val jwt = authRepo.getJwt() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val request = Request.Builder()
            .url("${AuthConfig.BACKEND_BASE_URL}/voting/sessions/$sessionId/selection")
            .get()
            .addHeader("Authorization", "Bearer $jwt")
            .build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to get selection results"))
            val obj = JSONObject(response.body?.string() ?: return@withContext Result.failure(Exception("Empty response")))
            val data = obj.getJSONObject("data")
            Result.success(MovieSelectionResults.fromJson(data))
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

data class VotingSessionDetails(
    val id: String,
    val groupId: String,
    val status: String,
    val movies: List<Movie>,
    val userVotes: Map<Int, String>,
    val startTime: String?,
    val endTime: String?,
    val selectedMovie: Movie?,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromJson(obj: JSONObject): VotingSessionDetails = VotingSessionDetails(
            id = obj.getString("id"),
            groupId = obj.getString("groupId"),
            status = obj.getString("status"),
            movies = obj.optJSONArray("movies")?.let { arr ->
                (0 until arr.length()).map { Movie.fromJson(arr.getJSONObject(it)) }
            } ?: emptyList(),
            userVotes = obj.optJSONObject("userVotes")?.let { votesObj ->
                val map = mutableMapOf<Int, String>()
                votesObj.keys().forEach { key ->
                    map[key.toInt()] = votesObj.getString(key)
                }
                map
            } ?: emptyMap(),
            startTime = obj.optString("startTime", null),
            endTime = obj.optString("endTime", null),
            selectedMovie = obj.optJSONObject("selectedMovie")?.let { Movie.fromJson(it) },
            createdAt = obj.getString("createdAt"),
            updatedAt = obj.getString("updatedAt")
        )
    }
}

data class MovieSelectionResults(
    val sessionId: String,
    val groupId: String,
    val selectedMovie: Movie?,
    val votingResults: List<VotingResult>,
    val totalParticipants: Int,
    val totalVotesCast: Int,
    val endTime: String?,
    val sessionDuration: Int?
) {
    companion object {
        fun fromJson(obj: JSONObject): MovieSelectionResults = MovieSelectionResults(
            sessionId = obj.getString("sessionId"),
            groupId = obj.getString("groupId"),
            selectedMovie = obj.optJSONObject("selectedMovie")?.let { Movie.fromJson(it) },
            votingResults = obj.optJSONArray("votingResults")?.let { arr ->
                (0 until arr.length()).map { VotingResult.fromJson(arr.getJSONObject(it)) }
            } ?: emptyList(),
            totalParticipants = obj.optInt("totalParticipants"),
            totalVotesCast = obj.optInt("totalVotesCast"),
            endTime = obj.optString("endTime", null),
            sessionDuration = if (obj.has("sessionDuration") && !obj.isNull("sessionDuration")) obj.getInt("sessionDuration") else null
        )
    }
}

data class VotingResult(
    val movie: Movie,
    val yesVotes: Int,
    val noVotes: Int,
    val totalVotes: Int,
    val approvalRate: Int,
    val score: Double
) {
    companion object {
        fun fromJson(obj: JSONObject): VotingResult = VotingResult(
            movie = Movie.fromJson(obj.getJSONObject("movie")),
            yesVotes = obj.getInt("yesVotes"),
            noVotes = obj.getInt("noVotes"),
            totalVotes = obj.getInt("totalVotes"),
            approvalRate = obj.getInt("approvalRate"),
            score = obj.getDouble("score")
        )
    }
}
