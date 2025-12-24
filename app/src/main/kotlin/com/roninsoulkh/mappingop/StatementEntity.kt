package com.roninsoulkh.mappingop

data class StatementEntity(
    val id: String,
    val name: String,
    val importDate: Long,
    val totalConsumers: Int,
    val processedConsumers: Int = 0
)