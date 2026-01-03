package com.roninsoulkh.mappingop.domain.models

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val status: String,
    val user_id: Int,
    val username: String,
    val token: String? = null,
    val requires_password_change: Boolean
)

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
    val new_password: String,
    val confirm_password: String
)

data class ChangePasswordResponse(
    val status: String,
    val message: String,
    val token: String? = null,
    val requires_password_change: Boolean
)

data class VersionResponse(
    val version: String
)