package com.example.dokkani.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dokkani.data.local.entities.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

class SessionManager(private val context: Context) {

    companion object {
        val CURRENT_USER_ID = intPreferencesKey("current_user_id")
        val CURRENT_USER_ROLE = stringPreferencesKey("current_user_role")
        val CURRENT_USER_FULL_NAME = stringPreferencesKey("current_user_full_name")
    }

    val currentUserId: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_USER_ID]
    }

    val currentUserRole: Flow<UserRole?> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_USER_ROLE]?.let {
            try { UserRole.valueOf(it) } catch (e: Exception) { null }
        }
    }

    val currentUserFullName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_USER_FULL_NAME]
    }

    suspend fun saveSession(userId: Int, role: UserRole, fullName: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_USER_ID] = userId
            preferences[CURRENT_USER_ROLE] = role.name
            preferences[CURRENT_USER_FULL_NAME] = fullName
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(CURRENT_USER_ID)
            preferences.remove(CURRENT_USER_ROLE)
            preferences.remove(CURRENT_USER_FULL_NAME)
        }
    }
}
