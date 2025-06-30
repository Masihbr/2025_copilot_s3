package com.example.movieswipe.group

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

class GroupRepository(private val context: Context) {
    private val client = OkHttpClient()
    private val authRepo = AuthRepository(context)

    suspend fun getGroups(): Result<List<Group>> = withContext(Dispatchers.IO) {
        val jwt = authRepo.getJwt() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val request = Request.Builder()
            .url("${AuthConfig.BACKEND_BASE_URL}/groups")
            .get()
            .addHeader("Authorization", "Bearer $jwt")
            .build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to fetch groups"))
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val arr = JSONObject(body).getJSONArray("data")
            val groups = (0 until arr.length()).map { Group.fromJson(arr.getJSONObject(it)) }
            Result.success(groups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGroup(name: String): Result<Group> = withContext(Dispatchers.IO) {
        val jwt = authRepo.getJwt() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val json = JSONObject().apply { put("name", name) }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("${AuthConfig.BACKEND_BASE_URL}/groups")
            .post(body)
            .addHeader("Authorization", "Bearer $jwt")
            .build()
        try {
            val response = client.newCall(request).execute()
            if (response.code == 400) return@withContext Result.failure(Exception("Group name is required"))
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to create group"))
            val obj = JSONObject(response.body?.string() ?: return@withContext Result.failure(Exception("Empty response")))
            val group = Group.fromJson(obj.getJSONObject("data"))
            Result.success(group)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGroup(groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val jwt = authRepo.getJwt() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val request = Request.Builder()
            .url("${AuthConfig.BACKEND_BASE_URL}/groups/$groupId")
            .delete()
            .addHeader("Authorization", "Bearer $jwt")
            .build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to delete group"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateInviteCode(groupId: String): Result<String> = withContext(Dispatchers.IO) {
        val jwt = authRepo.getJwt() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val request = Request.Builder()
            .url("${AuthConfig.BACKEND_BASE_URL}/groups/$groupId/invite")
            .post("".toRequestBody())
            .addHeader("Authorization", "Bearer $jwt")
            .build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to generate invite code"))
            val obj = JSONObject(response.body?.string() ?: return@withContext Result.failure(Exception("Empty response")))
            val code = obj.getJSONObject("data").getString("invitationCode")
            Result.success(code)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class Group(
    val id: String,
    val name: String,
    val ownerId: String,
    val invitationCode: String,
    val members: List<GroupMember>,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromJson(obj: JSONObject): Group = Group(
            id = obj.getString("id"),
            name = obj.getString("name"),
            ownerId = obj.getString("ownerId"),
            invitationCode = obj.getString("invitationCode"),
            members = obj.optJSONArray("members")?.let { arr ->
                (0 until arr.length()).map { GroupMember.fromJson(arr.getJSONObject(it)) }
            } ?: emptyList(),
            isActive = obj.getBoolean("isActive"),
            createdAt = obj.getString("createdAt"),
            updatedAt = obj.getString("updatedAt")
        )
    }
}

data class GroupMember(
    val userId: String,
    val joinedAt: String,
    val preferences: List<GenrePreference>
) {
    companion object {
        fun fromJson(obj: JSONObject): GroupMember = GroupMember(
            userId = obj.getString("userId"),
            joinedAt = obj.getString("joinedAt"),
            preferences = obj.optJSONArray("preferences")?.let { arr ->
                (0 until arr.length()).map { GenrePreference.fromJson(arr.getJSONObject(it)) }
            } ?: emptyList()
        )
    }
}

data class GenrePreference(
    val genreId: Int,
    val genreName: String,
    val weight: Int
) {
    companion object {
        fun fromJson(obj: JSONObject): GenrePreference = GenrePreference(
            genreId = obj.getInt("genreId"),
            genreName = obj.getString("genreName"),
            weight = obj.getInt("weight")
        )
    }
}
