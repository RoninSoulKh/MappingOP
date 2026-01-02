package com.roninsoulkh.mappingop.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        private val SAVED_LOGIN_KEY = stringPreferencesKey("saved_login")
        private val SAVED_PASSWORD_KEY = stringPreferencesKey("saved_password")
    }

    // Слушаем статус входа
    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[IS_LOGGED_IN_KEY] ?: false }

    // Получаем сохраненные данные (Логин, Пароль)
    val savedCredentials: Flow<Pair<String, String>> = context.dataStore.data
        .map { preferences ->
            val login = preferences[SAVED_LOGIN_KEY] ?: ""
            val pass = preferences[SAVED_PASSWORD_KEY] ?: ""
            Pair(login, pass)
        }

    // Сохраняем сессию (мы вошли)
    suspend fun setLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN_KEY] = loggedIn
        }
    }

    // Сохраняем (или стираем) логин и пароль
    suspend fun saveCredentials(login: String, pass: String) {
        context.dataStore.edit { preferences ->
            preferences[SAVED_LOGIN_KEY] = login
            preferences[SAVED_PASSWORD_KEY] = pass
        }
    }

    suspend fun clearCredentials() {
        context.dataStore.edit { preferences ->
            preferences.remove(SAVED_LOGIN_KEY)
            preferences.remove(SAVED_PASSWORD_KEY)
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN_KEY] = false
        }
    }
}