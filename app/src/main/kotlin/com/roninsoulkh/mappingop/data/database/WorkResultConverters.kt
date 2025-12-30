package com.roninsoulkh.mappingop.data.database

import androidx.room.TypeConverter
import com.roninsoulkh.mappingop.domain.models.BuildingCondition
import com.roninsoulkh.mappingop.domain.models.ConsumerType
import com.roninsoulkh.mappingop.domain.models.WorkType

class WorkResultConverters {
    @TypeConverter
    fun fromBuildingCondition(value: BuildingCondition?): String? = value?.name
    @TypeConverter
    fun toBuildingCondition(value: String?): BuildingCondition? = value?.let { BuildingCondition.valueOf(it) }

    @TypeConverter
    fun fromConsumerType(value: ConsumerType?): String? = value?.name
    @TypeConverter
    fun toConsumerType(value: String?): ConsumerType? = value?.let { ConsumerType.valueOf(it) }

    @TypeConverter
    fun fromWorkType(value: WorkType?): String? = value?.name
    @TypeConverter
    fun toWorkType(value: String?): WorkType? = value?.let { WorkType.valueOf(it) }
}