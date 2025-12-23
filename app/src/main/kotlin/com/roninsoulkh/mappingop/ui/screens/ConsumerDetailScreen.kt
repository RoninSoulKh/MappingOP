package com.roninsoulkh.mappingop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roninsoulkh.mappingop.domain.models.Consumer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumerDetailScreen(
    consumer: Consumer,
    onBackClick: () -> Unit,
    onProcessClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Деталі споживача") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
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
                    Button(onClick = onProcessClick) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Опрацьовано")
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
                    onClick = {},
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
                    )
                )
            }

            // Основная информация - УДАЛИЛ "Коротка адреса"
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

            // Дополнительная информация
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Додаткова інформація",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "ID запису: ${consumer.id}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "ID ведомості: ${consumer.worksheetId}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
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