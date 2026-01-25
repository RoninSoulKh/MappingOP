package com.roninsoulkh.mappingop.domain.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "consumer",
    foreignKeys = [
        ForeignKey(
            entity = Worksheet::class,
            parentColumns = ["id"],
            childColumns = ["worksheet_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["worksheet_id"])]
)
data class Consumer(
    @PrimaryKey
    @SerializedName("id")
    val id: String,

    @ColumnInfo(name = "worksheet_id")
    @SerializedName("worksheet_id")
    val worksheetId: String,

    @ColumnInfo(name = "or_number")
    @SerializedName("or_number")
    val orNumber: String,

    @SerializedName("name")
    val name: String,

    @ColumnInfo(name = "raw_address")
    @SerializedName("raw_address")
    val rawAddress: String,

    @SerializedName("phone")
    val phone: String? = null,

    @ColumnInfo(name = "debt_amount")
    @SerializedName("debt_amount")
    val debtAmount: Double? = null,

    @ColumnInfo(name = "meter_number")
    @SerializedName("meter_number")
    val meterNumber: String? = null,

    @ColumnInfo(name = "is_processed")
    @SerializedName("is_processed")
    val isProcessed: Boolean = false,

    // Доп. поля для результатов
    @SerializedName("processing_date")
    val processingDate: Long? = null,

    @SerializedName("meter_reading")
    val meterReading: String = "",

    @SerializedName("new_phone")
    val newPhone: String = "",

    @SerializedName("house_condition")
    val houseCondition: String = "",

    @SerializedName("consumer_type")
    val consumerType: String = "",

    @SerializedName("processing_type")
    val processingType: String = "",

    @SerializedName("comment")
    val comment: String = "",

    @SerializedName("photos")
    val photos: List<String> = emptyList(),

    @SerializedName("latitude")
    @ColumnInfo(name = "latitude")
    var latitude: Double? = null,

    @SerializedName("longitude")
    @ColumnInfo(name = "longitude")
    var longitude: Double? = null,

    // ====== Геокодинг: честная точность и пояснение ======

    @ColumnInfo(name = "geo_precision")
    val geoPrecision: GeoPrecision = GeoPrecision.UNKNOWN,

    @ColumnInfo(name = "geo_source")
    val geoSource: GeoSource = GeoSource.NONE,

    /**
     * Категория из Visicom: adr_address / adr_street / adm_settlement
     * или "FIELD_CONFIRMED", если вручную.
     */
    @ColumnInfo(name = "geo_source_category")
    val geoSourceCategory: String? = null,

    /**
     * Сообщение для UI: например “немає нумерації будинків у базі”.
     */
    @ColumnInfo(name = "geo_message")
    val geoMessage: String? = null,

    /**
     * Если true — UI должен предложить “Прив’язати вручну на карті”.
     */
    @ColumnInfo(name = "needs_manual_pin")
    val needsManualPin: Boolean = false
) {
    val shortAddress: String
        get() = rawAddress.takeIf { it.length <= 30 } ?: "${rawAddress.take(27)}..."
}
