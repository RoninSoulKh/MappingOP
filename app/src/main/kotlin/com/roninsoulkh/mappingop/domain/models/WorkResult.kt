package com.roninsoulkh.mappingop.domain.models

import java.util.UUID

data class WorkResult(
    val id: String = UUID.randomUUID().toString(),
    val consumerId: String,
    val worksheetId: String,

    // Показатели счётчика
    val meterReading: Double? = null,

    // Контактная информация
    val newPhone: String? = null,

    // Состояние здания
    val buildingCondition: BuildingCondition? = null,

    // Классификатор потребителя
    val consumerType: ConsumerType? = null,

    // Тип отработки
    val workType: WorkType? = null,

    // Комментарий
    val comment: String? = null,

    // Дата отработки
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