package com.roninsoulkh.mappingop.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "worksheet")
data class Worksheet(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "import_date")
    val importDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "total_consumers")
    val totalConsumers: Int,
    @ColumnInfo(name = "processed_count")
    val processedCount: Int = 0
) {
    val displayName: String
        get() = fileName
    val progress: Float
        get() = if (totalConsumers > 0) processedCount.toFloat() / totalConsumers else 0f
}