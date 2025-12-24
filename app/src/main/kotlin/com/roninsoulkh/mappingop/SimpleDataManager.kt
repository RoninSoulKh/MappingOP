package com.roninsoulkh.mappingop

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SimpleDataManager(private val context: Context) {
    private val gson = Gson()

    suspend fun <T> saveData(fileName: String, data: T): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, fileName)
                file.writeText(gson.toJson(data))
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun <T> loadData(fileName: String, type: TypeToken<T>): T? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, fileName)
                if (!file.exists()) return@withContext null

                val json = file.readText()
                gson.fromJson(json, type.type)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun saveStatements(statements: List<StatementEntity>): Boolean {
        return saveData("statements.json", statements)
    }

    suspend fun loadStatements(): List<StatementEntity> {
        val type = object : TypeToken<List<StatementEntity>>() {}
        return loadData("statements.json", type) ?: emptyList()
    }

    suspend fun saveConsumers(statementId: String, consumers: List<ConsumerEntity>): Boolean {
        return saveData("consumers_$statementId.json", consumers)
    }

    suspend fun loadConsumers(statementId: String): List<ConsumerEntity> {
        val type = object : TypeToken<List<ConsumerEntity>>() {}
        return loadData("consumers_$statementId.json", type) ?: emptyList()
    }
}