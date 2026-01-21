package com.roninsoulkh.mappingop.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth_preferences")

class TokenManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("token")
        private val LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        private val REQUIRES_PASSWORD_CHANGE_KEY = booleanPreferencesKey("requires_password_change")
        private val SAVED_LOGIN_KEY = stringPreferencesKey("saved_login")
        private val SAVED_PASSWORD_KEY = stringPreferencesKey("saved_password")
    }

    // Токен
    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[TOKEN_KEY] = token }
    }

    val token: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }

    // Статус входа
    suspend fun setLoggedIn(isLoggedIn: Boolean) {
        context.dataStore.edit { it[LOGGED_IN_KEY] = isLoggedIn }
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[LOGGED_IN_KEY] ?: false }

    // Смена пароля
    suspend fun setRequiresPasswordChange(requires: Boolean) {
        context.dataStore.edit { it[REQUIRES_PASSWORD_CHANGE_KEY] = requires }
    }

    // Сохраненные данные (Remember Me)
    suspend fun saveCredentials(login: String, pass: String) {
        context.dataStore.edit {
            it[SAVED_LOGIN_KEY] = login
            it[SAVED_PASSWORD_KEY] = pass
        }
    }

    suspend fun clearCredentials() {
        context.dataStore.edit {
            it.remove(SAVED_LOGIN_KEY)
            it.remove(SAVED_PASSWORD_KEY)
        }
    }

    val savedCredentials: Flow<Pair<String, String>> = context.dataStore.data.map {
        Pair(it[SAVED_LOGIN_KEY] ?: "", it[SAVED_PASSWORD_KEY] ?: "")
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(TOKEN_KEY)
            it[LOGGED_IN_KEY] = false
        }
    }
}