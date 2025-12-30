package com.roninsoulkh.mappingop

data class ConsumerEntity(
    val orNumber: String,
    val statementId: String,
    val fullName: String,
    val address: String,
    val phone: String,
    val debt: String,
    val counterNumber: String = "",

    // Поля отработки
    val processed: Boolean = false,
    val processingDate: Long? = null,
    val meterReading: String = "",
    val newPhone: String = "",
    val houseCondition: String = "", // жилой, разрушен, пустышка
    val consumerType: String = "", // гражданский, ВПО, другие
    val processingType: String = "", // вручено, шпарина, отказ, оплата
    val comment: String = "",
    val photos: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null
)