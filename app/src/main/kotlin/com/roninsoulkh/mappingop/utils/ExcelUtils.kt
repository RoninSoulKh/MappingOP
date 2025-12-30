package com.roninsoulkh.mappingop.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import com.roninsoulkh.mappingop.domain.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelUtils {

    // --- ИМПОРТ (ОСТАВЛЯЕМ КАК БЫЛО) ---
    suspend fun readExcelFromUri(context: Context, uri: Uri, statusCallback: (String) -> Unit): List<Consumer> {
        return withContext(Dispatchers.IO) {
            val consumers = mutableListOf<Consumer>()
            var inputStream: InputStream? = null
            val fileName = getFileName(context, uri)
            Log.d("ExcelUtils", "Чтение файла: $fileName")

            try {
                inputStream = context.contentResolver.openInputStream(uri)
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                val totalRows = sheet.physicalNumberOfRows

                for (i in 1 until totalRows) {
                    val row = sheet.getRow(i) ?: continue
                    val account = getCellValue(row.getCell(0))
                    val addressVal = getCellValue(row.getCell(1))
                    val name = getCellValue(row.getCell(2))

                    val phoneBase = if (row.lastCellNum > 3) getCellValue(row.getCell(3)) else null
                    val meterBase = if (row.lastCellNum > 4) getCellValue(row.getCell(4)) else null
                    val debtBaseStr = if (row.lastCellNum > 5) getCellValue(row.getCell(5)) else "0.0"
                    val debtBase = debtBaseStr.toDoubleOrNull() ?: 0.0

                    if (addressVal.isBlank()) continue
                    statusCallback("Обробка: $addressVal")

                    consumers.add(Consumer(
                        id = UUID.randomUUID().toString(),
                        worksheetId = "",
                        orNumber = account,
                        name = name,
                        rawAddress = addressVal,
                        phone = phoneBase,
                        meterNumber = meterBase,
                        debtAmount = debtBase,
                        latitude = 0.0, longitude = 0.0
                    ))
                }
                workbook.close()
            } catch (e: Exception) {
                Log.e("ExcelUtils", "Ошибка: ${e.message}")
                statusCallback("Помилка: ${e.message}")
            } finally { inputStream?.close() }
            return@withContext consumers
        }
    }

    // --- ЭКСПОРТ (ИСПРАВЛЕНО ПОД ТЗ) ---
    fun exportReport(
        context: Context,
        originalFileName: String,
        data: List<Pair<Consumer, WorkResult?>>
    ) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Звіт")

        val headerStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply { bold = true; fontHeightInPoints = 11.toShort() })
            fillForegroundColor = IndexedColors.LIGHT_CORNFLOWER_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderBottom = BorderStyle.THIN; borderTop = BorderStyle.THIN
            borderRight = BorderStyle.THIN; borderLeft = BorderStyle.THIN
        }
        val dataStyle = workbook.createCellStyle().apply {
            borderBottom = BorderStyle.THIN; borderTop = BorderStyle.THIN
            borderRight = BorderStyle.THIN; borderLeft = BorderStyle.THIN
            wrapText = true
        }

        // ПОРЯДОК КОЛОНОК
        val headers = listOf(
            "Номер ОР", "ПІБ", "Адреса", "Телефон (База)", "Лічильник (База)", "Сума боргу",
            "Дата виконання робіт", // <--- НОВАЯ КОЛОНКА ТУТ
            "Телефон (Факт)", "Показники", "Стан будівлі", "Фото/Відео", "Класифікатор", "Тип відпрацювання", "Коментар"
        )

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, title ->
            headerRow.createCell(i).apply { setCellValue(title); cellStyle = headerStyle }
            sheet.setColumnWidth(i, if (i == 2) 40 * 256 else 18 * 256)
        }

        val dateFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        data.forEachIndexed { index, (consumer, result) ->
            val row = sheet.createRow(index + 1)
            for (i in headers.indices) row.createCell(i).cellStyle = dataStyle

            // БАЗА
            row.getCell(0).setCellValue(consumer.orNumber)
            row.getCell(1).setCellValue(consumer.name)
            row.getCell(2).setCellValue(consumer.rawAddress)
            row.getCell(3).setCellValue(consumer.phone ?: "")
            row.getCell(4).setCellValue(consumer.meterNumber ?: "")
            row.getCell(5).setCellValue(consumer.debtAmount ?: 0.0)

            // ОТРАБОТКА (или пустота)
            if (result != null) {
                row.getCell(6).setCellValue(dateFormatter.format(Date(result.processedAt))) // Дата
                row.getCell(7).setCellValue(result.newPhone ?: "")
                row.getCell(8).setCellValue(result.meterReading?.toString() ?: "")
                row.getCell(9).setCellValue(getBuildingText(result.buildingCondition))
                row.getCell(10).setCellValue(if (result.photos.isNotEmpty()) "Так" else "Ні")
                row.getCell(11).setCellValue(result.consumerType?.let { getConsumerTypeText(it) } ?: "")
                row.getCell(12).setCellValue(result.workType?.let { getWorkTypeText(it) } ?: "")
                row.getCell(13).setCellValue(result.comment ?: "")
            }
        }

        val cleanName = originalFileName.replace(".xlsx", "", ignoreCase = true).trim()
        val file = File(context.cacheDir, "Звіт_$cleanName.xlsx")

        try {
            FileOutputStream(file).use { workbook.write(it) }
            workbook.close()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Надіслати звіт"))
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Helpers
    private fun getCellValue(cell: org.apache.poi.ss.usermodel.Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.NUMERIC -> if (cell.numericCellValue % 1 == 0.0) cell.numericCellValue.toLong().toString() else cell.numericCellValue.toString()
            else -> cell.toString().trim()
        }
    }
    private fun getFileName(context: Context, uri: Uri): String {
        var res: String? = null
        if (uri.scheme == "content") context.contentResolver.query(uri, null, null, null, null)?.use { if (it.moveToFirst()) res = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) }
        return res ?: uri.lastPathSegment ?: "file.xlsx"
    }
    private fun getBuildingText(c: BuildingCondition?) = when(c) {
        BuildingCondition.LIVING -> "Мешкають"; BuildingCondition.EMPTY -> "Пустка"; BuildingCondition.DESTROYED -> "Зруйнований"; BuildingCondition.NOT_LIVING -> "Не мешкають"; else -> ""
    }
    private fun getConsumerTypeText(t: ConsumerType) = when(t) { ConsumerType.CIVILIAN -> "Цивільний"; ConsumerType.VPO -> "ВПО"; else -> "Інші" }
    private fun getWorkTypeText(w: WorkType) = when(w) { WorkType.HANDED -> "Вручено"; WorkType.NOTE -> "Записка"; WorkType.REFUSAL -> "Відмова"; else -> "Оплата" }
}