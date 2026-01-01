package com.roninsoulkh.mappingop.data.parser

import android.content.Context
import com.roninsoulkh.mappingop.domain.models.BuildingCondition
import com.roninsoulkh.mappingop.domain.models.Consumer
import com.roninsoulkh.mappingop.domain.models.ConsumerType
import com.roninsoulkh.mappingop.domain.models.WorkResult
import com.roninsoulkh.mappingop.domain.models.WorkType
import org.apache.poi.ss.usermodel.BorderStyle
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

    // --- ЧТЕНИЕ (Без изменений) ---
    fun parseWorkbook(inputStream: InputStream, worksheetId: String): List<Consumer> {
        val consumers = mutableListOf<Consumer>()
        println("🔍 ExcelParser: начал парсинг, worksheetId=$worksheetId")

        try {
            inputStream.use { stream ->
                val workbook = WorkbookFactory.create(stream) as XSSFWorkbook
                val sheet = workbook.getSheetAt(0)

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
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Помилка парсингу: ${e.message}")
        }
        return consumers
    }

    // --- ЭКСПОРТ (ИСПРАВЛЕННЫЙ) ---
    fun exportWorksheet(context: Context, fileName: String, data: List<Pair<Consumer, WorkResult?>>): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Звіт")

        // 1. Стили
        val headerStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply { bold = true }
            setFont(font)
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val dataStyle = workbook.createCellStyle().apply {
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        // 2. Заголовки (14 колонок)
        val headers = listOf(
            "Номер ОР",          // 0
            "ПІБ",               // 1
            "Адреса",            // 2
            "Номер лічильника",  // 3
            "Телефон (База)",    // 4
            "Борг",              // 5
            "Дата виконаних робіт", // 6
            "Зафіксовані показники",// 7
            "Телефон (Факт)",    // 8
            "Стан будівлі",      // 9
            "Фото/Відео",        // 10
            "Класифікатор",      // 11
            "Тип відпрацювання", // 12
            "Коментар"           // 13
        )

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        // 3. Заполнение
        data.forEachIndexed { index, (consumer, result) ->
            val row = sheet.createRow(index + 1)

            // Хелперы для записи строк и чисел
            fun cell(col: Int, valStr: String) {
                row.createCell(col).apply { setCellValue(valStr); cellStyle = dataStyle }
            }
            fun cellNum(col: Int, valNum: Double) {
                row.createCell(col).apply { setCellValue(valNum); cellStyle = dataStyle }
            }

            // Данные потребителя (База)
            cell(0, consumer.orNumber)
            cell(1, consumer.name)
            cell(2, consumer.rawAddress)
            cell(3, consumer.meterNumber ?: "")
            cell(4, consumer.phone ?: "")
            cellNum(5, consumer.debtAmount ?: 0.0)

            // Результаты работы (Факт)
            if (result != null) {
                // Дата
                cell(6, sdf.format(Date(result.processedAt)))

                // --- 🔥 ИСПРАВЛЕНИЕ ПОКАЗАНИЙ ---
                // Если null -> пустая строка. Если 0.0 -> пишем 0.0
                if (result.meterReading != null) {
                    cellNum(7, result.meterReading)
                } else {
                    cell(7, "")
                }

                // Телефон новый
                cell(8, result.newPhone ?: "")

                // Стан будівлі
                val conditionStr = when(result.buildingCondition) {
                    BuildingCondition.LIVING -> "Мешкають"
                    BuildingCondition.EMPTY -> "Пустка"
                    BuildingCondition.PARTIALLY_DESTROYED -> "Напівзруйнований"
                    BuildingCondition.DESTROYED -> "Зруйнований"
                    BuildingCondition.NOT_LIVING -> "Не мешкають"
                    BuildingCondition.FORBIDDEN -> "Заборона"
                    else -> ""
                }
                cell(9, conditionStr)

                // Фото
                val hasPhoto = if (result.photos.isNotEmpty()) "Так" else "Ні"
                cell(10, hasPhoto)

                // Классификатор
                val typeStr = when(result.consumerType) {
                    ConsumerType.CIVILIAN -> "Цивільний"
                    ConsumerType.VPO -> "ВПО"
                    ConsumerType.OTHER -> "Інші особи"
                    else -> ""
                }
                cell(11, typeStr)

                // Тип отработки
                val workStr = when(result.workType) {
                    WorkType.HANDED -> "Вручено в руки"
                    WorkType.NOTE -> "Шпарина (записка)"
                    WorkType.REFUSAL -> "Відмова"
                    WorkType.PAYMENT -> "Оплата поточного"
                    else -> ""
                }
                cell(12, workStr)

                // Комментарий
                cell(13, result.comment ?: "")

            } else {
                // Если не обработан - заполняем пустотой (для сетки)
                for (i in 6..13) cell(i, "")
            }
        }

        // 4. Ручная ширина колонок
        sheet.setColumnWidth(0, 4000)
        sheet.setColumnWidth(1, 9000)
        sheet.setColumnWidth(2, 12000)
        sheet.setColumnWidth(3, 5000)
        sheet.setColumnWidth(4, 4000)
        sheet.setColumnWidth(5, 3000)
        sheet.setColumnWidth(6, 4000)
        sheet.setColumnWidth(7, 4000)
        sheet.setColumnWidth(8, 4000)
        sheet.setColumnWidth(9, 5000)
        sheet.setColumnWidth(10, 3000)
        sheet.setColumnWidth(11, 5000)
        sheet.setColumnWidth(12, 5000)
        sheet.setColumnWidth(13, 8000)

        // Сохранение
        val exportsDir = File(context.cacheDir, "exports")
        if (!exportsDir.exists()) exportsDir.mkdirs()

        val file = File(exportsDir, "${fileName}_Звіт.xlsx")
        if (file.exists()) file.delete()

        val fileOut = FileOutputStream(file)
        workbook.write(fileOut)
        fileOut.flush()
        fileOut.close()
        workbook.close()

        return file
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
}