package com.roninsoulkh.mappingop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roninsoulkh.mappingop.domain.models.Worksheet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorksheetsScreen(
    worksheets: List<Worksheet>,
    onWorksheetClick: (Worksheet) -> Unit,
    onAddWorksheet: () -> Unit,
    onBackClick: () -> Unit,
    onViewResults: () -> Unit,
    onDeleteWorksheet: (Worksheet) -> Unit,
    onRenameWorksheet: (Worksheet, String) -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var worksheetToRename by remember { mutableStateOf<Worksheet?>(null) }
    var newNameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Відомості")
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge { Text(worksheets.size.toString()) }
                    }
                },
                actions = {
                    IconButton(onClick = onAddWorksheet) {
                        Icon(Icons.Filled.Add, "Додати")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (worksheets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Description,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Список пустий", color = MaterialTheme.colorScheme.secondary)
                    Text("Натисніть +, щоб додати файл", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(worksheets) { worksheet ->
                    WorksheetCard(
                        worksheet = worksheet,
                        onClick = { onWorksheetClick(worksheet) },
                        onDelete = { onDeleteWorksheet(worksheet) },
                        onRename = {
                            worksheetToRename = worksheet
                            newNameInput = worksheet.fileName
                            showRenameDialog = true
                        }
                    )
                }
            }
        }
    }

    // Диалог переименования
    if (showRenameDialog && worksheetToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Перейменувати відомість") },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    label = { Text("Нова назва") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newNameInput.isNotBlank()) {
                        onRenameWorksheet(worksheetToRename!!, newNameInput)
                        showRenameDialog = false
                    }
                }) {
                    Text("Зберегти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
fun WorksheetCard(
    worksheet: Worksheet,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically // Выравнивание по центру, но элементы внутри колонки будут идти вниз
        ) {
            // Основная информация (Имя, Дата, Прогресс)
            Column(modifier = Modifier.weight(1f)) {
                // 1. Имя файла
                Text(
                    text = worksheet.fileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 2. Дата
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(worksheet.importDate)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 3. 👇 ПРОГРЕСС-БАР (ВЕРНУЛСЯ!)
                Spacer(modifier = Modifier.height(12.dp))
                val progress = if (worksheet.totalConsumers > 0)
                    worksheet.processedCount.toFloat() / worksheet.totalConsumers.toFloat()
                else 0f

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // 4. Текст "Оброблено X з Y"
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Оброблено ${worksheet.processedCount} з ${worksheet.totalConsumers}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Кнопки управления (справа)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                IconButton(onClick = onRename) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Перейменувати",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Видалити",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}