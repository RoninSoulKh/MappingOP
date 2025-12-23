package com.roninsoulkh.mappingop.domain.models

data class Consumer(
    val id: String,
    val worksheetId: String,
    val orNumber: String,
    val name: String,
    val phone: String? = null,
    val rawAddress: String,
    val shortAddress: String,
    val debtAmount: Double? = null,
    val meterNumber: String? = null,
    val isProcessed: Boolean = false
) {
    fun getDisplayTitle(): String = "[ОР $orNumber] $shortAddress"
}