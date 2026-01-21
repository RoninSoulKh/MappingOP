package com.roninsoulkh.mappingop.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.roninsoulkh.mappingop.data.local.TokenManager
import com.roninsoulkh.mappingop.data.remote.RetrofitClient
import com.roninsoulkh.mappingop.domain.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class AuthRepository(private val tokenManager: TokenManager) {

    private val api = RetrofitClient.api
    private val gson = Gson()

    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return safeApiCall {
            // 🔥 ИСПРАВЛЕНИЕ: Теперь передаем поля отдельно, так как в API стоит @Field
            val response = api.login(
                email = request.email,
                password = request.password
            )

            if (response.isSuccessful) {
                response.body()?.let {
                    if (it.token != null) tokenManager.saveToken(it.token)
                    tokenManager.setLoggedIn(true)
                    tokenManager.setRequiresPasswordChange(it.requires_password_change)
                }
            }
            response
        }
    }

    suspend fun changePassword(request: ChangePasswordRequest): Result<ChangePasswordResponse> {
        return safeApiCall {
            val response = api.changePassword(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    if (it.token != null) tokenManager.saveToken(it.token)
                    tokenManager.setRequiresPasswordChange(it.requires_password_change)
                }
            }
            response
        }
    }

    suspend fun logout() {
        tokenManager.clearSession()
    }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiCall()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = parseError(errorBody) ?: "Ошибка: ${response.code()}"
                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Ошибка сети: ${e.localizedMessage}"))
            }
        }
    }

    private fun parseError(json: String?): String? {
        if (json.isNullOrEmpty()) return "Невідома помилка"

        // 🔥 ФИКС: Если сервер ответил HTML-страницей (ошибка Nginx)
        if (json.contains("<html>") || json.contains("<title>")) {
            return "Невірний логін або пароль (або тех. роботи)"
        }

        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(json, type)
            map["detail"]?.toString() ?: json
        } catch (e: Exception) {
            "Помилка сервера (невірний формат відповіді)"
        }
    }
}