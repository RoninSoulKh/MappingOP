package com.roninsoulkh.mappingop.data.remote

import com.roninsoulkh.mappingop.domain.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.GET

interface AuthApi {

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): Response<LoginResponse>

    // Остальное оставляем как было (если там JSON)
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("auth/verify-code")
    suspend fun verifyCode(@Body request: VerifyCodeRequest): Response<Unit>

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ChangePasswordResponse>

    @GET("system/version")
    suspend fun checkVersion(): Response<VersionResponse>
}