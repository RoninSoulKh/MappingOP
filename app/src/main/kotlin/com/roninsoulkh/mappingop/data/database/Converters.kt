package com.roninsoulkh.mappingop.data.database

import androidx.room.TypeConverter
import com.roninsoulkh.mappingop.domain.models.BuildingCondition
import com.roninsoulkh.mappingop.domain.models.ConsumerType
import com.roninsoulkh.mappingop.domain.models.WorkType

class Converters {

    // ============ BuildingCondition ============
    @TypeConverter
    fun fromBuildingCondition(value: BuildingCondition?): String? {
        return value?.name
    }

    @TypeConverter
    fun toBuildingCondition(value: String?): BuildingCondition? {
        return if (value == null) null else BuildingCondition.valueOf(value)
    }

    // ============ ConsumerType ============
    @TypeConverter
    fun fromConsumerType(value: ConsumerType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toConsumerType(value: String?): ConsumerType? {
        return if (value == null) null else ConsumerType.valueOf(value)
    }

    // ============ WorkType ============
    @TypeConverter
    fun fromWorkType(value: WorkType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toWorkType(value: String?): WorkType? {
        return if (value == null) null else WorkType.valueOf(value)
    }
}