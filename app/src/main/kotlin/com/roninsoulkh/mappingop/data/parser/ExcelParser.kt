package com.roninsoulkh.mappingop.data.parser

import android.content.Context
import com.roninsoulkh.mappingop.domain.models.Consumer
import com.roninsoulkh.mappingop.domain.models.WorkResult
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExcelParser {

    // --- ТВОЯ ТЕКУЩАЯ ФУНКЦИЯ ЧТЕНИЯ (Без изменений) ---
    fun parseWorkbook(inputStream: InputStream, worksheetId: String): List<Consumer> {
        val consumers = mutableListOf<Consumer>()
        println("🔍 ExcelParser: начал парсинг, worksheetId=$worksheetId")

        try {
            inputStream.use { stream ->
                val workbook = WorkbookFactory.create(stream) as XSSFWorkbook
                val sheet = workbook.getSheetAt(0)
                println("🔍 ExcelParser: лист найден, строк: ${sheet.lastRowNum}")

                for (rowIndex in 2..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    val orNumber = getCellValue(row, 1)
                    if (orNumber.isBlank()) continue

                    val name = getCellValue(row, 3)
                    val phone = getCellValue(row, 5)
                    val rawAddress = getCellValue(row, 18)

                    val meterNumber = getCellValue(row, 23)
                    val lastReadingStr = getCellValue(row, 24)
                    val warningSum = getNumericValue(row, 25)
                    val currentDebt = getNumericValue(row, 27)

                    val consumer = Consumer(
                        id = "${worksheetId}_$orNumber",
                        worksheetId = worksheetId,
                        orNumber = orNumber,
                        name = name.ifEmpty { "Данних немає" },
                        phone = phone.ifEmpty { null },
                        rawAddress = rawAddress.ifEmpty { "Данних немає" },
                        debtAmount = warningSum ?: currentDebt,
                        meterNumber = meterNumber.ifEmpty { null },
                        isProcessed = false
                    )
                    consumers.add(consumer)
                }
                println("🔍 ExcelParser: успешно спарсено ${consumers.size} потребителей")
            }
        } catch (e: Exception) {
            println("❌ ExcelParser: ОШИБКА - ${e.message}")
            e.printStackTrace()
            throw RuntimeException("Помилка парсингу Excel файлу: ${e.message}")
        }
        return consumers
    }

    // --- НОВАЯ ФУНКЦИЯ ЭКСПОРТА (ДОБАВЛЕНО) ---
    fun exportWorksheet(context: Context, fileName: String, data: List<Pair<Consumer, WorkResult?>>): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Звіт")

        // Стиль шапки (Жирный + Серый фон)
        val headerStyle = workbook.createCellStyle()
        val font = workbook.createFont()
        font.bold = true
        headerStyle.setFont(font)
        headerStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        headerStyle.fillPattern = FillPatternType.SOLID_FOREGROUND

        // Заголовки (Добавил "Дата виконання робіт")
        val headers = listOf("№ ОР", "Адреса", "ПІБ", "Статус", "Показник", "Новий телефон", "Коментар", "Дата виконання робіт")
        val headerRow = sheet.createRow(0)

        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        // Формат даты
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        // Заполняем данными
        data.forEachIndexed { index, (consumer, result) ->
            val row = sheet.createRow(index + 1)

            row.createCell(0).setCellValue(consumer.orNumber)
            row.createCell(1).setCellValue(consumer.rawAddress)
            row.createCell(2).setCellValue(consumer.name)

            if (result != null) {
                row.createCell(3).setCellValue("Опрацьовано")
                row.createCell(4).setCellValue(result.meterReading ?: 0.0)
                row.createCell(5).setCellValue(result.newPhone ?: "")
                row.createCell(6).setCellValue(result.comment ?: "")
                // ДАТА ВЫПОЛНЕНИЯ
                row.createCell(7).setCellValue(sdf.format(Date(result.processedAt)))
            } else {
                row.createCell(3).setCellValue("Не опрацьовано")
                // Остальные ячейки пустые
            }
        }

        // Автоширина колонок
        for (i in headers.indices) {
            sheet.autoSizeColumn(i)
        }

        // Сохраняем во временный файл
        val exportsDir = File(context.cacheDir, "exports")
        if (!exportsDir.exists()) exportsDir.mkdirs()

        val file = File(exportsDir, "${fileName}_Звіт.xlsx")
        val fileOut = FileOutputStream(file)
        workbook.write(fileOut)
        fileOut.close()
        workbook.close()

        return file
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ (Твои старые) ---
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