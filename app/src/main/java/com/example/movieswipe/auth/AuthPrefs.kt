package com.example.movieswipe.auth

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "auth_prefs")

object AuthPrefs {
    private val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")

    suspend fun saveToken(context: Context, token: String) {
        context.dataStore.edit { prefs ->
            prefs[JWT_TOKEN_KEY] = token
        }
    }

    fun getTokenFlow(context: Context): Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[JWT_TOKEN_KEY]
        }

    suspend fun clearToken(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(JWT_TOKEN_KEY)
        }
    }
}

