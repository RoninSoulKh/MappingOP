package com.roninsoulkh.mappingop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onDeleteWorksheet: (Worksheet) -> Unit // <--- НОВЫЙ ПАРАМЕТР
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📋 Відомості")
                        if (worksheets.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge { Text(worksheets.size.toString()) }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Filled.ArrowBack, "Назад") }
                },
                actions = {
                    IconButton(onClick = onAddWorksheet) { Icon(Icons.Filled.Add, "Додати") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onViewResults,
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Filled.ListAlt, "Результати")
            }
        }
    ) { paddingValues ->
        if (worksheets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FolderOpen, null, Modifier.size(64.dp))
                    Text("Немає відомостей. Додайте файл.")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(worksheets) { worksheet ->
                    WorksheetCard(
                        worksheet = worksheet,
                        onClick = { onWorksheetClick(worksheet) },
                        onDelete = { onDeleteWorksheet(worksheet) } // Передаем удаление
                    )
                }
            }
        }
    }
}

@Composable
fun WorksheetCard(
    worksheet: Worksheet,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Диалог подтверждения удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Видалити відомість?") },
            text = { Text("Ви впевнені? Всі споживачі з цієї відомості будуть видалені назавжди.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Видалити") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Скасувати") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = worksheet.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = dateFormatter.format(Date(worksheet.importDate)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Кнопка-корзина
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Filled.Delete, "Видалити", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = worksheet.progress,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Оброблено ${worksheet.processedCount} з ${worksheet.totalConsumers}",
                fontSize = 12.sp
            )
        }
    }
}