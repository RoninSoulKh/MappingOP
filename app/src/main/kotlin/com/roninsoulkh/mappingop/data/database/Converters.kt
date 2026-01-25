package com.roninsoulkh.mappingop.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.roninsoulkh.mappingop.domain.models.*

class Converters {

    // --- 1. СПИСОК ФОТО ---
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return Gson().toJson(value ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return try {
            Gson().fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- 2. СТАТУСЫ (ENUMS) ---
    @TypeConverter
    fun fromBuildingCondition(value: BuildingCondition?): String? {
        return value?.name
    }

    @TypeConverter
    fun toBuildingCondition(value: String?): BuildingCondition? {
        return if (value == null) null else try {
            BuildingCondition.valueOf(value)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromConsumerType(value: ConsumerType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toConsumerType(value: String?): ConsumerType? {
        return if (value == null) null else try {
            ConsumerType.valueOf(value)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromWorkType(value: WorkType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toWorkType(value: String?): WorkType? {
        return if (value == null) null else try {
            WorkType.valueOf(value)
        } catch (e: Exception) {
            null
        }
    }

    // --- 3. ГЕОКОДИНГ (НОВОЕ) ---
    @TypeConverter
    fun fromGeoPrecision(value: GeoPrecision?): String? {
        return value?.name
    }

    @TypeConverter
    fun toGeoPrecision(value: String?): GeoPrecision? {
        return if (value == null) null else try {
            GeoPrecision.valueOf(value)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromGeoSource(value: GeoSource?): String? {
        return value?.name
    }

    @TypeConverter
    fun toGeoSource(value: String?): GeoSource? {
        return if (value == null) null else try {
            GeoSource.valueOf(value)
        } catch (e: Exception) {
            null
        }
    }
}
