package com.roninsoulkh.mappingop.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.roninsoulkh.mappingop.domain.models.*
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelUtils {

    fun exportReport(
        context: Context,
        worksheetName: String,
        data: List<Pair<Consumer, WorkResult?>>
    ) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Звіт")

        // --- 1. НАСТРОЙКА СТИЛЕЙ ---

        // Стиль для ЗАГОЛОВКА
        val headerFont = workbook.createFont().apply {
            bold = true
            fontHeightInPoints = 12.toShort()
        }
        val headerCellStyle = workbook.createCellStyle().apply {
            setFont(headerFont)
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
        }

        // Стиль для ДАННЫХ
        val dataCellStyle = workbook.createCellStyle().apply {
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            wrapText = true
        }

        // --- 2. ШАПКА ТАБЛИЦЫ  ---
        val headers = listOf(
            "Номер ОР",             // 0
            "ПІБ",                  // 1
            "Адреса",               // 2
            "Телефон (База)",       // 3
            "Лічильник (База)",     // 4
            "Сума боргу",           // 5
            "Дата виконаних робіт", // 6
            "Телефон (Факт)",       // 7
            "Показники",            // 8
            "Стан будівлі",         // 9
            "Фото/Відео",           // 10
            "Класифікатор",         // 11
            "Тип відпрацювання",    // 12
            "Коментар"              // 13
        )

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerCellStyle

            // Ширина колонок
            when (index) {
                1 -> sheet.setColumnWidth(index, 30 * 256) // ФИО
                2 -> sheet.setColumnWidth(index, 40 * 256) // Адрес
                6 -> sheet.setColumnWidth(index, 15 * 256) // Дата (пошире)
                13 -> sheet.setColumnWidth(index, 30 * 256) // Комментарий
                else -> sheet.setColumnWidth(index, 15 * 256) // Остальные
            }
        }

        // Форматтер даты: ДД.ММ.ГГГГ
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        // --- 3. ЗАПОЛНЕНИЕ ДАННЫХ ---
        data.forEachIndexed { index, (consumer, result) ->
            val row = sheet.createRow(index + 1)

            // Создаем пустые ячейки с рамками (теперь их 14, от 0 до 13)
            for (i in 0..13) {
                val cell = row.createCell(i)
                cell.cellStyle = dataCellStyle
            }

            // Заполняем данными потребителя
            row.getCell(0).setCellValue(consumer.orNumber)
            row.getCell(1).setCellValue(consumer.name)
            row.getCell(2).setCellValue(consumer.rawAddress)
            row.getCell(3).setCellValue(consumer.phone ?: "")
            row.getCell(4).setCellValue(consumer.meterNumber ?: "")
            row.getCell(5).setCellValue(consumer.debtAmount ?: 0.0)

            // Данные результата (если есть)
            if (result != null) {
                // 1. Дата (Новое поле)
                val dateString = dateFormatter.format(Date(result.processedAt))
                row.getCell(6).setCellValue(dateString)

                // 2. Остальные поля (индексы сдвинулись на +1)
                row.getCell(7).setCellValue(result.newPhone ?: "")
                row.getCell(8).setCellValue(result.meterReading?.toString() ?: "")
                row.getCell(9).setCellValue(result.buildingCondition?.let { getBuildingText(it) } ?: "")

                val hasMedia = if (result.photos.isNotEmpty()) "Так" else "Ні"
                row.getCell(10).setCellValue(hasMedia)

                row.getCell(11).setCellValue(result.consumerType?.let { getConsumerTypeText(it) } ?: "")
                row.getCell(12).setCellValue(result.workType?.let { getWorkTypeText(it) } ?: "")
                row.getCell(13).setCellValue(result.comment ?: "")
            }
        }

        // --- 4. СОХРАНЕНИЕ ---
        val timeStamp = SimpleDateFormat("dd-MM-yyyy_HH-mm", Locale.getDefault()).format(Date())

        // Убираем .xlsx из имени ведомости (на всякий случай, если он там есть)
        val cleanName = worksheetName.replace(".xlsx", "", ignoreCase = true)
            .replace(" ", "_")
            .replace("/", "-")

        val fileName = "Звіт_${cleanName}_$timeStamp.xlsx"

        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { outputStream ->
            workbook.write(outputStream)
        }
        workbook.close()

        // --- 5. ОТКРЫТИЕ/ОТПРАВКА ---
        shareFile(context, file)
    }

    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Надіслати звіт Excel")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    // --- ПЕРЕВОДЧИКИ ---
    private fun getBuildingText(c: BuildingCondition) = when(c) {
        BuildingCondition.LIVING -> "Мешкають"
        BuildingCondition.EMPTY -> "Пустка"
        BuildingCondition.PARTIALLY_DESTROYED -> "Напівзруйнований"
        BuildingCondition.DESTROYED -> "Зруйнований"
        BuildingCondition.NOT_LIVING -> "Не мешкають"
        BuildingCondition.FORBIDDEN -> "Заборона"
        BuildingCondition.UNKNOWN -> ""
    }

    private fun getConsumerTypeText(t: ConsumerType) = when(t) {
        ConsumerType.CIVILIAN -> "Цивільний"
        ConsumerType.VPO -> "ВПО"
        ConsumerType.OTHER -> "Інші"
    }

    private fun getWorkTypeText(w: WorkType) = when(w) {
        WorkType.HANDED -> "Вручено"
        WorkType.NOTE -> "Записка"
        WorkType.REFUSAL -> "Відмова"
        WorkType.PAYMENT -> "Оплата"
    }
}