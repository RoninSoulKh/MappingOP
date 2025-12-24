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
import com.roninsoulkh.mappingop.domain.models.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkResultsScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📋 Результати відпрацювання")
                        Text(
                            text = "Всього записів: 0", // Пока заглушка
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Екран результатів")
                Text(
                    "Тут будуть зберігатися всі опрацьовані споживачі",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WorkResultCard(
    workResult: WorkResult
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    text = "Результат відпрацювання",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
                        .format(Date(workResult.processedAt)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Тип отработки
            workResult.workType?.let { workType ->
                Text(
                    text = when (workType) {
                        WorkType.HANDED -> "✅ Вручено в руки"
                        WorkType.NOTE -> "📝 Шпарина (записка)"
                        WorkType.REFUSAL -> "❌ Відмова"
                        WorkType.PAYMENT -> "💰 Оплата поточного"
                    },
                    fontWeight = FontWeight.Medium
                )
            }

            // Классификатор потребителя
            workResult.consumerType?.let { consumerType ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Тип споживача: ${
                        when (consumerType) {
                            ConsumerType.CIVILIAN -> "Цивільний"
                            ConsumerType.VPO -> "ВПО"
                            ConsumerType.OTHER -> "Інші особи"
                        }
                    }",
                    fontSize = 14.sp
                )
            }

            // Состояние здания
            workResult.buildingCondition?.let { buildingCondition ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Стан будівлі: ${
                        when (buildingCondition) {
                            BuildingCondition.LIVING -> "Мешкають"
                            BuildingCondition.EMPTY -> "Пустка"
                            BuildingCondition.PARTIALLY_DESTROYED -> "Напівзруйнований"
                            BuildingCondition.DESTROYED -> "Зруйнований"
                            BuildingCondition.NOT_LIVING -> "Не мешкають"
                            BuildingCondition.FORBIDDEN -> "Заборона"
                            BuildingCondition.UNKNOWN -> "Невідомо"
                        }
                    }",
                    fontSize = 14.sp
                )
            }

            // Показания счетчика
            workResult.meterReading?.let { reading ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Показник лічильника: $reading",
                    fontSize = 14.sp
                )
            }

            // Новый телефон
            workResult.newPhone?.let { phone ->
                if (phone.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Новий телефон: $phone",
                        fontSize = 14.sp
                    )
                }
            }

            // Комментарий
            workResult.comment?.let { comment ->
                if (comment.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Коментар: $comment",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}