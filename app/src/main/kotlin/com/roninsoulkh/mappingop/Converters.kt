package com.roninsoulkh.mappingop

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}