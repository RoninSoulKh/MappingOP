package com.roninsoulkh.mappingop.domain.models

import com.google.gson.annotations.SerializedName

data class TargetLocation(
    @SerializedName("display_name")
    val displayName: String,    // Полный текст адреса

    @SerializedName("lat")
    val lat: Double,            // Широта

    @SerializedName("lng")
    val lng: Double,            // Долгота

    @SerializedName("precision")
    val precision: String,      // exact/street/settlement

    @SerializedName("is_confirmed")
    val isConfirmed: Boolean = false
) {
    val needsManualCheck: Boolean
        get() = precision != "exact"
}
