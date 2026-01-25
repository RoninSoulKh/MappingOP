package com.roninsoulkh.mappingop.domain.models

/**
 * Точность геокодинга.
 *
 * Важно: это НЕ “тип адреса”, а честная оценка точности координаты.
 */
enum class GeoPrecision {
    HOUSE,       // точный дом
    STREET,      // улица без номера дома (примерно)
    SETTLEMENT,  // центр населённого пункта (примерно)
    UNKNOWN      // не найдено
}

/**
 * Источник координаты.
 */
enum class GeoSource {
    FIELD_CONFIRMED, // подтверждено вручную в поле
    VISICOM,         // пришло из Visicom
    NONE             // источника нет (ещё не геокодили / не нашли)
}

/**
 * Красивое название для UI (потом пригодится на ШАГЕ 2).
 */
fun GeoPrecision.toUiLabelUa(): String = when (this) {
    GeoPrecision.HOUSE -> "ДІМ"
    GeoPrecision.STREET -> "ВУЛИЦЯ"
    GeoPrecision.SETTLEMENT -> "НАСЕЛЕНИЙ ПУНКТ"
    GeoPrecision.UNKNOWN -> "НЕ ЗНАЙДЕНО"
}
