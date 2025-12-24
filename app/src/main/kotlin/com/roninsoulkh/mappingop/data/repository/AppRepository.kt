package com.roninsoulkh.mappingop.data.repository

import android.content.Context
import com.roninsoulkh.mappingop.data.database.AppDatabase
import com.roninsoulkh.mappingop.domain.models.*
import kotlinx.coroutines.flow.Flow

class AppRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val worksheetDao = database.worksheetDao()
    private val consumerDao = database.consumerDao()
    private val workResultDao = database.workResultDao()

    // 1. ПОТОКИ ДАННЫХ (Flow)
    fun getAllWorksheetsFlow(): Flow<List<Worksheet>> = worksheetDao.getAllWorksheets()

    fun getConsumersFlow(worksheetId: String): Flow<List<Consumer>> {
        return consumerDao.getConsumersByWorksheetId(worksheetId)
    }

    // 2. СИНХРОННЫЕ МЕТОДЫ (возвращают List, а не Flow)
    suspend fun getAllWorksheets(): List<Worksheet> {
        return worksheetDao.getAllWorksheetsSync()  // Изменено!
    }

    suspend fun getConsumersByWorksheetId(worksheetId: String): List<Consumer> {
        return consumerDao.getConsumersByWorksheetIdSync(worksheetId)  // Изменено!
    }

    // 3. ОСНОВНЫЕ ОПЕРАЦИИ
    suspend fun addWorksheet(fileName: String, consumers: List<Consumer>) {
        // 1. Получаем worksheetId из ПЕРВОГО потребителя
        val worksheetId = consumers.firstOrNull()?.worksheetId
            ?: "worksheet_${System.currentTimeMillis()}_${fileName.hashCode()}"

        println("🔍 AppRepository: добавляем ведомость, worksheetId=$worksheetId, потребителей=${consumers.size}")

        // 2. Создаем Worksheet с ТЕМ ЖЕ worksheetId
        val worksheet = Worksheet(
            id = worksheetId,
            fileName = fileName,
            importDate = System.currentTimeMillis(),
            totalConsumers = consumers.size,
            processedCount = 0
        )

        // 3. Проверяем данные перед сохранением
        consumers.forEachIndexed { index, consumer ->
            println("🔍 Consumer $index: id=${consumer.id}, worksheetId=${consumer.worksheetId}, OR=${consumer.orNumber}")
        }

        worksheetDao.insertWorksheet(worksheet)
        consumerDao.insertAllConsumers(consumers)

        println("✅ AppRepository: ведомость сохранена, ID=$worksheetId")
    }

    suspend fun saveWorkResult(consumerId: String, result: WorkResult) {
        workResultDao.insertWorkResult(result)
        consumerDao.updateProcessedStatus(consumerId, isProcessed = true)
        updateWorksheetProgress(result.worksheetId)
    }

    private suspend fun updateWorksheetProgress(worksheetId: String) {
        val consumers = getConsumersByWorksheetId(worksheetId)  // Используем исправленный метод
        val processedCount = consumers.count { it.isProcessed }

        val worksheet = worksheetDao.getWorksheetById(worksheetId)
        worksheet?.let {
            val updatedWorksheet = it.copy(processedCount = processedCount)
            worksheetDao.updateWorksheet(updatedWorksheet)
        }
    }

    suspend fun getWorkResultByConsumerId(consumerId: String): WorkResult? {
        return workResultDao.getWorkResultByConsumerId(consumerId)
    }

    // 4. ОЧИСТКА ДАННЫХ (новые методы)
    suspend fun clearAllData() {
        worksheetDao.deleteAllWorksheets()
        consumerDao.deleteAllConsumers()
        workResultDao.deleteAllWorkResults()
    }
}