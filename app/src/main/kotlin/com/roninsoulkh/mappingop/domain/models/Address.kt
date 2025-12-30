package com.roninsoulkh.mappingop.domain.models

data class Address(
    val id: Int,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val completed: Boolean = false
)