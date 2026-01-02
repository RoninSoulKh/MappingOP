package com.roninsoulkh.mappingop.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.roninsoulkh.mappingop.data.local.TokenManager
import com.roninsoulkh.mappingop.data.remote.RetrofitClient
import com.roninsoulkh.mappingop.domain.models.ChangePasswordRequest
import com.roninsoulkh.mappingop.domain.models.LoginRequest
import com.roninsoulkh.mappingop.domain.models.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class AuthRepository(private val tokenManager: TokenManager) {

    private val api = RetrofitClient.api
    private val gson = Gson()

    // 1. Вход
    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return safeApiCall {
            val response = api.login(request)
            // Если сервер ответил 200 OK - значит Cookie уже в RetrofitClient.
            // Нам нужно только запомнить, что мы вошли.
            if (response.isSuccessful) {
                tokenManager.setLoggedIn(true)
            }
            response
        }
    }

    // 2. Смена пароля
    suspend fun changePassword(request: ChangePasswordRequest): Result<Unit> {
        return safeApiCall { api.changePassword(request) }
    }

    // 3. Выход
    suspend fun logout() {
        tokenManager.clearSession()
        // Тут можно еще очистить CookieManager в RetrofitClient, если нужно будет
    }

    // --- ОБРАБОТЧИК ОШИБОК (Чтобы не крашилось) ---
    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Result.success(body)
                    } else {
                        // Если тело пустое, но код 200 - это успех
                        @Suppress("UNCHECKED_CAST")
                        Result.success(Unit as T)
                    }
                } else {
                    // Парсим ошибку от FastAPI: {"detail": "..."}
                    val errorBody = response.errorBody()?.string()
                    val message = parseError(errorBody) ?: "Помилка: ${response.code()}"
                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(Exception(e.localizedMessage ?: "Помилка: ${e.javaClass.simpleName}"))
            }
        }
    }

    private fun parseError(json: String?): String? {
        if (json.isNullOrEmpty()) return null
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(json, type)
            map["detail"].toString()
        } catch (e: Exception) {
            json
        }
    }
}