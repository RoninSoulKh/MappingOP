package com.roninsoulkh.mappingop.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "consumer")
data class Consumer(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "worksheet_id")
    val worksheetId: String,
    @ColumnInfo(name = "or_number")
    val orNumber: String,
    val name: String,
    @ColumnInfo(name = "raw_address")
    val rawAddress: String,
    val phone: String? = null,
    @ColumnInfo(name = "debt_amount")
    val debtAmount: Double? = null,
    @ColumnInfo(name = "meter_number")
    val meterNumber: String? = null,
    @ColumnInfo(name = "is_processed")
    val isProcessed: Boolean = false
) {
    val shortAddress: String
        get() = rawAddress.takeIf { it.length <= 30 } ?: "${rawAddress.take(27)}..."
}