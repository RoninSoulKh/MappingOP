package com.roninsoulkh.mappingop.domain.models

import java.util.UUID

data class Worksheet(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val displayName: String = fileName.replace(".xlsx", "").replace(".xls", ""),
    val importDate: Long = System.currentTimeMillis(),
    val totalConsumers: Int = 0,
    val processedCount: Int = 0
) {
    val progress: Float
        get() = if (totalConsumers > 0) processedCount.toFloat() / totalConsumers else 0f
}