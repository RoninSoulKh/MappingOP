package com.roninsoulkh.mappingop.data.parser

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import com.roninsoulkh.mappingop.domain.models.Consumer
import java.io.InputStream

class ExcelParser {

    fun parseWorkbook(inputStream: InputStream, worksheetId: String): List<Consumer> {
        val consumers = mutableListOf<Consumer>()

        try {
            inputStream.use { stream ->
                val workbook = WorkbookFactory.create(stream) as XSSFWorkbook
                val sheet = workbook.getSheetAt(0) // Первый лист

                // Начинаем с 3-й строки (индекс 2), так как 1-я и 2-я - заголовки
                for (rowIndex in 2..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue

                    // Извлекаем данные по колонкам (индексы с 0)
                    val orNumber = getCellValue(row, 1) // Колонка B (индекс 1)
                    if (orNumber.isBlank()) continue

                    val name = getCellValue(row, 3) // Колонка D (индекс 3)
                    val phone = getCellValue(row, 5) // Колонка F (индекс 5)
                    val rawAddress = getCellValue(row, 18) // Колонка S (индекс 18)

                    // Финансовые данные
                    val meterNumber = getCellValue(row, 23) // Колонка X (индекс 23)
                    val lastReadingStr = getCellValue(row, 24) // Колонка Y (индекс 24)
                    val warningSum = getNumericValue(row, 25) // Колонка Z (индекс 25) - Сума попередження
                    val currentDebt = getNumericValue(row, 27) // Колонка AB (индекс 27) - Сума поточного боргу

                    // Обработка показаний счетчика (могут быть разделены /)
                    val lastReading = parseMeterReading(lastReadingStr)

                    // Форматируем короткий адрес
                    val shortAddress = formatAddress(rawAddress)

                    // Создаем объект Consumer
                    // ИЗМЕНЕНИЕ: Приоритет для "Сума попередження" (колонка Z)
                    val consumer = Consumer(
                        id = "${worksheetId}_$orNumber",
                        worksheetId = worksheetId, // <-- Уже на месте
                        orNumber = orNumber,
                        name = name.ifEmpty { "Данних немає" },
                        phone = phone.ifEmpty { null },
                        rawAddress = rawAddress.ifEmpty { "Данних немає" },
                        shortAddress = shortAddress,
                        // ИЗМЕНЕНИЕ: Сначала берем warningSum (колонка Z - Сума попередження)
                        // Если warningSum null, берем currentDebt (колонка AB - Сума поточного боргу)
                        debtAmount = warningSum ?: currentDebt,
                        meterNumber = meterNumber.ifEmpty { null },
                        isProcessed = false
                    )

                    consumers.add(consumer)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Помилка парсингу Excel файлу: ${e.message}")
        }

        return consumers
    }

    private fun getCellValue(row: org.apache.poi.ss.usermodel.Row, columnIndex: Int): String {
        return row.getCell(columnIndex)?.toString()?.trim() ?: ""
    }

    private fun getNumericValue(row: org.apache.poi.ss.usermodel.Row, columnIndex: Int): Double? {
        val cell = row.getCell(columnIndex) ?: return null
        return when (cell.cellType) {
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> cell.numericCellValue
            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue.replace(",", "").toDoubleOrNull()
            else -> null
        }
    }

    private fun parseMeterReading(reading: String): Double? {
        if (reading.isBlank()) return null

        // Если есть разделитель "/", берем первое значение
        val parts = reading.split("/").map { it.trim() }
        return parts.firstOrNull()?.toDoubleOrNull()
    }

    private fun formatAddress(fullAddress: String): String {
        if (fullAddress.isBlank()) return "Данних немає"

        val parts = fullAddress.split(",").map { it.trim() }
        return when {
            parts.size >= 3 -> parts.take(3).joinToString(", ")
            parts.isNotEmpty() -> parts.joinToString(", ")
            else -> fullAddress
        }
    }
}