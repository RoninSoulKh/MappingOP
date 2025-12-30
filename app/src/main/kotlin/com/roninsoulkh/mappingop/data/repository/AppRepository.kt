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

    // --- Flow ---
    fun getAllWorksheetsFlow(): Flow<List<Worksheet>> = worksheetDao.getAllWorksheets()

    fun getConsumersFlow(worksheetId: String): Flow<List<Consumer>> {
        return consumerDao.getConsumersByWorksheetId(worksheetId)
    }

    // --- Suspend функции (для корутин) ---
    suspend fun getAllWorksheets(): List<Worksheet> = worksheetDao.getAllWorksheetsSync()

    suspend fun getConsumersByWorksheetId(worksheetId: String): List<Consumer> =
        consumerDao.getConsumersByWorksheetIdSync(worksheetId)

    suspend fun getWorkResultByConsumerId(consumerId: String): WorkResult? =
        workResultDao.getWorkResultByConsumerId(consumerId)

    suspend fun addWorksheet(fileName: String, consumers: List<Consumer>) {
        val worksheetId = consumers.firstOrNull()?.worksheetId
            ?: "worksheet_${System.currentTimeMillis()}"

        val worksheet = Worksheet(
            id = worksheetId,
            fileName = fileName,
            importDate = System.currentTimeMillis(),
            totalConsumers = consumers.size,
            processedCount = 0
        )
        worksheetDao.insertWorksheet(worksheet)
        consumerDao.insertAllConsumers(consumers)
    }

    suspend fun deleteWorksheet(worksheet: Worksheet) {
        worksheetDao.deleteWorksheet(worksheet)
    }

    suspend fun renameWorksheet(worksheet: Worksheet, newName: String) {
        val updatedWorksheet = worksheet.copy(fileName = newName)
        worksheetDao.updateWorksheet(updatedWorksheet)
    }

    suspend fun saveWorkResult(consumerId: String, result: WorkResult) {
        workResultDao.insertWorkResult(result)
        consumerDao.updateProcessedStatus(consumerId, isProcessed = true)
        updateWorksheetProgress(result.worksheetId)
    }

    private suspend fun updateWorksheetProgress(worksheetId: String) {
        val consumers = getConsumersByWorksheetId(worksheetId)
        val processedCount = consumers.count { it.isProcessed }
        val worksheet = worksheetDao.getWorksheetById(worksheetId)
        worksheet?.let {
            val updatedWorksheet = it.copy(processedCount = processedCount)
            worksheetDao.updateWorksheet(updatedWorksheet)
        }
    }

    // ВОТ ЭТА ФУНКЦИЯ, КОТОРУЮ МЫ ИСЩЕМ
    suspend fun updateConsumer(consumer: Consumer) {
        consumerDao.update(consumer)
    }
}