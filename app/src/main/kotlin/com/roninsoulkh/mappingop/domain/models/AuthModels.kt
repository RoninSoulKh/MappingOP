package com.roninsoulkh.mappingop.domain.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("user_id")
    val user_id: Int,
    @SerializedName("username")
    val username: String,
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("requires_password_change")
    val requires_password_change: Boolean
)

data class RegisterRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

data class VerifyCodeRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("code")
    val code: String
)

data class ChangePasswordRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("old_password")
    val old_password: String,
    @SerializedName("new_password")
    val new_password: String,
    @SerializedName("confirm_password")
    val confirm_password: String
)

data class ChangePasswordResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("requires_password_change")
    val requires_password_change: Boolean
)

data class VersionResponse(
    @SerializedName("version")
    val version: String
)
