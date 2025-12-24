package com.roninsoulkh.mappingop.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.TypeConverters
import java.util.UUID

@Entity(tableName = "workresult")
data class WorkResult(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "consumer_id")
    val consumerId: String,

    @ColumnInfo(name = "worksheet_id")
    val worksheetId: String,

    // Показатели счётчика
    @ColumnInfo(name = "meter_reading")
    val meterReading: Double? = null,

    // Контактная информация
    @ColumnInfo(name = "new_phone")
    val newPhone: String? = null,

    // Состояние здания
    @ColumnInfo(name = "building_condition")
    val buildingCondition: BuildingCondition? = null,

    // Классификатор потребителя
    @ColumnInfo(name = "consumer_type")
    val consumerType: ConsumerType? = null,

    // Тип отработки
    @ColumnInfo(name = "work_type")
    val workType: WorkType? = null,

    // Комментарий
    @ColumnInfo(name = "comment")
    val comment: String? = null,

    // Дата отработки
    @ColumnInfo(name = "processed_at")
    val processedAt: Long = System.currentTimeMillis()
)

// Состояние здания
enum class BuildingCondition {
    LIVING,           // Мешкають
    EMPTY,            // Пустка
    PARTIALLY_DESTROYED, // Напівзруйнований
    DESTROYED,        // Зруйнований
    NOT_LIVING,       // Не мешкають
    FORBIDDEN,        // Заборона
    UNKNOWN           // Невідомо (только для внутреннего использования)
}

// Классификатор потребителя (без Невідомо)
enum class ConsumerType {
    CIVILIAN,    // Цивільний
    VPO,         // ВПО
    OTHER        // Інші особи
}

// Тип отработки (без Інше)
enum class WorkType {
    HANDED,      // Вручено в руки
    NOTE,        // Шпарина (записка)
    REFUSAL,     // Відмова
    PAYMENT      // Оплата поточного
}