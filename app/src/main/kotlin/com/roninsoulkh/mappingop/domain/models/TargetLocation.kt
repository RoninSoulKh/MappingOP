package com.roninsoulkh.mappingop.domain.models

data class TargetLocation(
    val displayName: String,    // Полный текст адреса
    val lat: Double,            // Широта
    val lng: Double,            // Долгота
    val precision: String,      // Насколько точно нашли (exact, street или settlement)
    val isConfirmed: Boolean = false // Проверил ли пользователь точку вручную
) {
    // Свойство-помощник: если точность НЕ "exact", значит нужно показать предупреждение
    val needsManualCheck: Boolean
        get() = precision != "exact"
}