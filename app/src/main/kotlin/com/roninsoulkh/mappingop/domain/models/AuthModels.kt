package com.roninsoulkh.mappingop.domain.models

// Исправили username -> email
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String?,
    val status: String?
)

// Тут тоже на всякий случай проверим, но пока оставим как было
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class VerifyCodeRequest(
    val email: String,
    val code: String
)

data class ChangePasswordRequest(
    val email: String,
    val old_password: String,
    val new_password: String
)

data class VersionResponse(
    val version: String
)