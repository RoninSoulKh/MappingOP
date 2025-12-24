package com.roninsoulkh.mappingop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowBack // ДОБАВЛЕН ИМПОРТ
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
fun ConsumerListScreen(
    consumers: List<Consumer>,
    onConsumerClick: (Consumer) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("👥 Список споживачів")
                        Text(
                            text = "Всього: ${consumers.size} | Опрацьовано: ${consumers.count { it.isProcessed }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack, // ИСПРАВЛЕНО
                            contentDescription = "Назад до ведомостей"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (consumers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет данных. Загрузите Excel файл.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Загружено: ${consumers.size} потребителей",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                items(consumers) { consumer ->
                    ConsumerCard(
                        consumer = consumer,
                        onClick = { onConsumerClick(consumer) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumerCard(
    consumer: Consumer,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (consumer.isProcessed) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ОР №${consumer.orNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                if (consumer.isProcessed) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Обработан",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = consumer.shortAddress,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = consumer.name,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Долг: ${consumer.debtAmount ?: 0.0} грн",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )

                Text(
                    text = "Счётчик: ${consumer.meterNumber ?: "нет"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}