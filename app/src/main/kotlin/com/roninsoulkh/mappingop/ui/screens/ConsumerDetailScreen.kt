package com.roninsoulkh.mappingop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roninsoulkh.mappingop.domain.models.Consumer
import com.roninsoulkh.mappingop.domain.models.WorkResult
import com.roninsoulkh.mappingop.domain.models.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumerDetailScreen(
    consumer: Consumer,
    workResult: WorkResult?, // <--- НОВЫЙ ПАРАМЕТР: Результат работы (может быть null)
    onBackClick: () -> Unit,
    onProcessClick: () -> Unit
) {
    // Состояние для показа диалога с результатами
    var showResultDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Деталі споживача") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    // ПОКАЗЫВАЕМ ИКОНКУ ТОЛЬКО ЕСЛИ ЕСТЬ РЕЗУЛЬТАТ
                    if (workResult != null) {
                        IconButton(onClick = { showResultDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Description, // Иконка листа/документа
                                contentDescription = "Показати результат",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    // Если уже обработано, меняем текст кнопки
                    Button(
                        onClick = onProcessClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (consumer.isProcessed)
                                MaterialTheme.colorScheme.secondary
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (consumer.isProcessed) Icons.Filled.Edit else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (consumer.isProcessed) "Редагувати" else "Опрацювати")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Статус обработки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AssistChip(
                    onClick = { if (workResult != null) showResultDialog = true },
                    label = {
                        Text(
                            if (consumer.isProcessed) "ОПРАЦЬОВАНО" else "НЕ ОПРАЦЬОВАНО",
                            fontSize = 12.sp
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (consumer.isProcessed) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    ),
                    leadingIcon = {
                        if (consumer.isProcessed) Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                    }
                )
            }

            // Основная информация
            InfoCard(title = "Номер ОР", value = consumer.orNumber, icon = Icons.Filled.Numbers)
            InfoCard(title = "Адреса", value = consumer.rawAddress, icon = Icons.Filled.Home)
            InfoCard(title = "Контрагент", value = consumer.name, icon = Icons.Filled.Person)

            // Контакты
            InfoCard(
                title = "Телефон",
                value = consumer.phone ?: "не вказано",
                icon = Icons.Filled.Phone
            )

            // Финансы и техника
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoCard(
                    title = "Сума боргу",
                    value = "${consumer.debtAmount ?: 0.0} грн",
                    icon = Icons.Filled.AttachMoney,
                    modifier = Modifier.weight(1f)
                )
                InfoCard(
                    title = "Номер лічильника",
                    value = consumer.meterNumber ?: "не вказано",
                    icon = Icons.Filled.Speed,
                    modifier = Modifier.weight(1f)
                )
            }

            // Дополнительная информация (скрыл лишнее, оставил ID для отладки если надо)
            if (!consumer.isProcessed) {
                Text(
                    text = "Натисніть «Опрацювати» щоб внести показники",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }

    // --- ДИАЛОГ С РЕЗУЛЬТАТАМИ ---
    if (showResultDialog && workResult != null) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            icon = { Icon(Icons.Filled.Description, null) },
            title = { Text("Результат відпрацювання") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Дата
                    Text(
                        text = "Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(workResult.processedAt))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider()

                    // Данные
                    ResultRow("Тип:", workResult.workType?.name ?: "-")
                    ResultRow("Лічильник:", workResult.meterReading?.toString() ?: "-")
                    if (!workResult.newPhone.isNullOrEmpty()) {
                        ResultRow("Новий телефон:", workResult.newPhone)
                    }
                    if (!workResult.comment.isNullOrEmpty()) {
                        ResultRow("Коментар:", workResult.comment)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResultDialog = false }) {
                    Text("Закрити")
                }
            }
        )
    }
}

@Composable
fun InfoCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ResultRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(value, fontSize = 14.sp)
    }
}