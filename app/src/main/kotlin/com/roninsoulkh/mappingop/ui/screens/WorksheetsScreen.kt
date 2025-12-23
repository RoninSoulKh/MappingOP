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
import androidx.compose.ui.platform.LocalContext
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
    onBackClick: () -> Unit
) {
    // ДОБАВЛЕННЫЙ КОД ДЛЯ ОТЛАДКИ
    val context = LocalContext.current
    LaunchedEffect(worksheets) {
        if (worksheets.isNotEmpty()) {
            android.widget.Toast.makeText(
                context,
                "📱 WorksheetsScreen: ${worksheets.size} ведомостей",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Также покажем простое текстовое сообщение в UI
    var showDebugInfo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📋 Ведомости")
                        if (worksheets.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge {
                                Text(worksheets.size.toString())
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showDebugInfo = !showDebugInfo }) {
                        Icon(Icons.Filled.Info, "Информация")
                    }
                    IconButton(onClick = onAddWorksheet) {
                        Icon(Icons.Filled.Add, "Додати ведомость")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddWorksheet,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, "Додати ведомость")
            }
        }
    ) { paddingValues ->
        // Отладочная информация
        if (showDebugInfo) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 Отладка:",
                        fontWeight = FontWeight.Bold
                    )
                    Text("Количество ведомостей: ${worksheets.size}")
                    if (worksheets.isNotEmpty()) {
                        Text("Первая ведомость: ${worksheets[0].fileName}")
                        Text("Потребителей: ${worksheets[0].totalConsumers}")
                    }
                }
            }
        }

        if (worksheets.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Немає ведомостей",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Натисніть + щоб додати першу ведомость",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Кнопка для принудительной проверки
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        android.widget.Toast.makeText(
                            context,
                            "Проверка: worksheets size = ${worksheets.size}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                ) {
                    Icon(Icons.Filled.Refresh, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Проверить данные")
                }
            }
        } else {
            Column {
                // Информационная панель
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📁 Всего ведомостей: ${worksheets.size}",
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = {
                                android.widget.Toast.makeText(
                                    context,
                                    "Первая ведомость: ${worksheets[0].fileName}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Тест")
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(worksheets) { worksheet ->
                        WorksheetCard(
                            worksheet = worksheet,
                            onClick = { onWorksheetClick(worksheet) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorksheetCard(
    worksheet: Worksheet,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(worksheet.importDate) {
        dateFormatter.format(Date(worksheet.importDate))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            // Показываем Toast при нажатии
            android.widget.Toast.makeText(
                context,
                "Выбрана: ${worksheet.fileName}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            onClick()
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = worksheet.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Статус обработки
                Badge(
                    containerColor = if (worksheet.processedCount == worksheet.totalConsumers) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                ) {
                    Text("${worksheet.processedCount}/${worksheet.totalConsumers}")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Прогресс бар
            LinearProgressIndicator(
                progress = worksheet.progress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (worksheet.totalConsumers > 0) {
                    "Оброблено ${(worksheet.progress * 100).toInt()}%"
                } else {
                    "Немає споживачів"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}