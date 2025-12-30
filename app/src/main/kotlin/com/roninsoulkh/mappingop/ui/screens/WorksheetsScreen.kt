package com.roninsoulkh.mappingop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var worksheetToDelete by remember { mutableStateOf<Worksheet?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var worksheetToRename by remember { mutableStateOf<Worksheet?>(null) }
    var newNameText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мої відомості") },
                actions = {
                    IconButton(onClick = onAddWorksheet) {
                        Icon(Icons.Filled.Add, contentDescription = "Імпорт")
                    }
                }
            )
        }
    ) { padding ->
        if (worksheets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Список пустий. Натисніть +, щоб додати.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(worksheets) { worksheet ->
                    WorksheetItem(
                        worksheet = worksheet,
                        onClick = { onWorksheetClick(worksheet) },
                        onRename = {
                            worksheetToRename = worksheet
                            newNameText = worksheet.fileName
                            showRenameDialog = true
                        },
                        onDelete = {
                            worksheetToDelete = worksheet
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // --- ДИАЛОГИ ---
    if (showDeleteDialog && worksheetToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Видалити відомість?") },
            text = { Text("Ви збираєтесь видалити '${worksheetToDelete?.fileName}'.") },
            confirmButton = {
                TextButton(onClick = {
                    worksheetToDelete?.let { onDeleteWorksheet(it) }
                    showDeleteDialog = false
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Видалити") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Скасувати") } }
        )
    }

    if (showRenameDialog && worksheetToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Перейменувати") },
            text = { OutlinedTextField(value = newNameText, onValueChange = { newNameText = it }, label = { Text("Назва") }) },
            confirmButton = {
                TextButton(onClick = {
                    if (newNameText.isNotBlank()) { worksheetToRename?.let { onRenameWorksheet(it, newNameText) } }
                    showRenameDialog = false
                }) { Text("Зберегти") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Скасувати") } }
        )
    }
}

@Composable
fun WorksheetItem(
    worksheet: Worksheet,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Извлекаем дату из ID
    val dateString = remember(worksheet.id) {
        try {
            val timestamp = worksheet.id.substringAfter("_").toLong()
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            "Імпортовано: ${sdf.format(Date(timestamp))}"
        } catch (e: Exception) { "" }
    }

    // ИСПРАВЛЕНИЕ: Берем реальный прогресс из модели Worksheet
    val progress = worksheet.progress

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = worksheet.fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (dateString.isNotEmpty()) {
                        Text(text = dateString, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box {
                    IconButton(onClick = { expanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = null) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Перейменувати") }, onClick = { expanded = false; onRename() }, leadingIcon = { Icon(Icons.Filled.Edit, null) })
                        DropdownMenuItem(text = { Text("Видалити", color = Color.Red) }, onClick = { expanded = false; onDelete() }, leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color.Red) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Шкала выполнения
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.weight(1f).height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                // Текст процентов (без десятичных знаков)
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}