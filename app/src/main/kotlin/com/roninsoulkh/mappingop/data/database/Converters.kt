package com.roninsoulkh.mappingop.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.roninsoulkh.mappingop.domain.models.BuildingCondition
import com.roninsoulkh.mappingop.domain.models.ConsumerType
import com.roninsoulkh.mappingop.domain.models.WorkType

class Converters {

    // ============ СПИСОК ФОТО (List<String>) ============
    // Без этого база не сможет сохранить пути к фото!
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

    // ============ BuildingCondition ============
    @TypeConverter
    fun fromBuildingCondition(value: BuildingCondition?): String? {
        return value?.name
    }

    @TypeConverter
    fun toBuildingCondition(value: String?): BuildingCondition? {
        return if (value == null) null else try {
            BuildingCondition.valueOf(value)
        } catch (e: Exception) { null }
    }

    // ============ ConsumerType ============
    @TypeConverter
    fun fromConsumerType(value: ConsumerType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toConsumerType(value: String?): ConsumerType? {
        return if (value == null) null else try {
            ConsumerType.valueOf(value)
        } catch (e: Exception) { null }
    }

    // ============ WorkType ============
    @TypeConverter
    fun fromWorkType(value: WorkType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toWorkType(value: String?): WorkType? {
        return if (value == null) null else try {
            WorkType.valueOf(value)
        } catch (e: Exception) { null }
    }
}