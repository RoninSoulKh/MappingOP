package com.roninsoulkh.mappingop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roninsoulkh.mappingop.domain.models.Worksheet

@Composable
fun HomeScreen(
    worksheets: List<Worksheet>,
    onImportClick: () -> Unit,
    onExportClick: (Worksheet) -> Unit // Передаем выбранную ведомость
) {
    var showExportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Вітаємо в MappingOP!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Ваш помічник у роботі з абонентами",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // КНОПКА ИМПОРТА
        LargeButton(
            text = "Імпорт Excel",
            icon = Icons.Filled.UploadFile,
            onClick = onImportClick,
            color = MaterialTheme.colorScheme.primaryContainer
        )

        Spacer(modifier = Modifier.height(16.dp))

        // КНОПКА ЭКСПОРТА
        LargeButton(
            text = "Експорт звіту",
            icon = Icons.Filled.FileUpload,
            onClick = { showExportDialog = true },
            color = MaterialTheme.colorScheme.secondaryContainer
        )
    }

    // ДИАЛОГ ВЫБОРА ВЕДОМОСТИ ДЛЯ ЭКСПОРТА
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Оберіть відомість для звіту") },
            text = {
                if (worksheets.isEmpty()) {
                    Text("Немає завантажених відомостей.")
                } else {
                    Column {
                        worksheets.forEach { worksheet ->
                            TextButton(
                                onClick = {
                                    showExportDialog = false
                                    onExportClick(worksheet)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(worksheet.fileName)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Скасувати") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, color: androidx.compose.ui.graphics.Color) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
    }
}