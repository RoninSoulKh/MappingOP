package com.roninsoulkh.mappingop.domain.models

import com.google.gson.annotations.SerializedName

data class Address(
    @SerializedName("id")
    val id: Int,
    @SerializedName("address")
    val address: String,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("completed")
    val completed: Boolean = false
)
