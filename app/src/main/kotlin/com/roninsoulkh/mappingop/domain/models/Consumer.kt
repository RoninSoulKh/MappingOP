package com.roninsoulkh.mappingop.domain.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "consumer",
    foreignKeys = [
        ForeignKey(
            entity = Worksheet::class,
            parentColumns = ["id"],          // Колонка ID в таблице ведомостей (Родитель)
            childColumns = ["worksheet_id"], // Колонка ID в таблице потребителей (Ребенок)
            onDelete = ForeignKey.CASCADE    // <--- ГЛАВНОЕ: Если удалили ведомость, удали и потребителей
        )
    ],
    // Индекс нужен, чтобы Room быстрее искал связи и не ругался при компиляции
    indices = [Index(value = ["worksheet_id"])]
)
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
    val isProcessed: Boolean = false,

    // Доп. поля для результатов
    val processingDate: Long? = null,
    val meterReading: String = "",
    val newPhone: String = "",
    val houseCondition: String = "",
    val consumerType: String = "",
    val processingType: String = "",
    val comment: String = "",
    val photos: List<String> = emptyList()
) {
    val shortAddress: String
        get() = rawAddress.takeIf { it.length <= 30 } ?: "${rawAddress.take(27)}..."
}