package com.roninsoulkh.mappingop.data.repository

import android.util.Log
import com.roninsoulkh.mappingop.domain.models.Consumer
import com.roninsoulkh.mappingop.domain.models.WorkResult
import com.roninsoulkh.mappingop.domain.models.Worksheet

class AppDataRepository {
    // Храним данные в памяти
    private val worksheets = mutableListOf<Worksheet>()
    private val consumers = mutableMapOf<String, List<Consumer>>() // worksheetId -> List<Consumer>
    private val workResults = mutableMapOf<String, WorkResult>() // consumerId -> WorkResult

    // Получить все ведомости
    fun getAllWorksheets(): List<Worksheet> {
        Log.d("📋 REPOSITORY", "getAllWorksheets вызван. Всего ведомостей: ${worksheets.size}")
        if (worksheets.isNotEmpty()) {
            worksheets.forEachIndexed { index, worksheet ->
                Log.d("📋 REPOSITORY", "Ведомость $index: ${worksheet.fileName}, ID: ${worksheet.id}")
            }
        }
        return worksheets.toList()
    }

    // Добавить новую ведомость
    fun addWorksheet(fileName: String, parsedConsumers: List<Consumer>) {
        Log.d("➕ REPOSITORY", "ДОБАВЛЯЕМ ВЕДОМОСТЬ: $fileName")
        Log.d("➕ REPOSITORY", "Количество потребителей: ${parsedConsumers.size}")
        Log.d("➕ REPOSITORY", "Всего ведомостей до добавления: ${worksheets.size}")

        val worksheet = Worksheet(
            fileName = fileName,
            totalConsumers = parsedConsumers.size
        )

        worksheets.add(worksheet)
        Log.d("➕ REPOSITORY", "Ведомость создана: ID=${worksheet.id}, имя=${worksheet.displayName}")

        consumers[worksheet.id] = parsedConsumers.map {
            it.copy(worksheetId = worksheet.id)
        }

        Log.d("➕ REPOSITORY", "Потребители сохранены для ведомости ${worksheet.id}")
        Log.d("➕ REPOSITORY", "Всего ведомостей после добавления: ${worksheets.size}")
    }

    // Получить потребителей ведомости
    fun getConsumersByWorksheetId(worksheetId: String): List<Consumer> {
        val consumersList = consumers[worksheetId] ?: emptyList()
        Log.d("👥 REPOSITORY", "getConsumersByWorksheetId: $worksheetId, найдено: ${consumersList.size}")
        return consumersList
    }

    // Получить ведомость по ID
    fun getWorksheetById(worksheetId: String): Worksheet? {
        val worksheet = worksheets.find { it.id == worksheetId }
        Log.d("🔍 REPOSITORY", "getWorksheetById: $worksheetId, результат: ${worksheet?.fileName ?: "не найдено"}")
        return worksheet
    }

    // Сохранить результат отработки
    fun saveWorkResult(result: WorkResult) {
        Log.d("💾 REPOSITORY", "saveWorkResult: consumerId=${result.consumerId}")
        workResults[result.consumerId] = result

        // Обновляем счетчик обработанных
        updateWorksheetProgress(result.worksheetId)
    }

    // Обновить прогресс ведомости
    private fun updateWorksheetProgress(worksheetId: String) {
        val worksheet = worksheets.find { it.id == worksheetId } ?: return
        val worksheetConsumers = consumers[worksheetId] ?: emptyList()

        val processedCount = worksheetConsumers.count { consumer ->
            workResults[consumer.id] != null || consumer.isProcessed
        }

        val index = worksheets.indexOfFirst { it.id == worksheetId }
        if (index != -1) {
            worksheets[index] = worksheet.copy(processedCount = processedCount)
            Log.d("📊 REPOSITORY", "Обновлен прогресс ведомости ${worksheet.fileName}: $processedCount/${worksheet.totalConsumers}")
        }
    }

    // Удалить ведомость
    fun removeWorksheet(worksheetId: String) {
        Log.d("🗑️ REPOSITORY", "Удаление ведомости: $worksheetId")
        worksheets.removeAll { it.id == worksheetId }
        consumers.remove(worksheetId)

        // Удаляем результаты отработки для этой ведомости
        val consumersToRemove = consumers[worksheetId] ?: emptyList()
        consumersToRemove.forEach { consumer ->
            workResults.remove(consumer.id)
        }

        Log.d("🗑️ REPOSITORY", "Ведомость удалена. Осталось: ${worksheets.size}")
    }

    // Получить результат отработки по ID потребителя
    fun getWorkResultByConsumerId(consumerId: String): WorkResult? {
        return workResults[consumerId]
    }
}