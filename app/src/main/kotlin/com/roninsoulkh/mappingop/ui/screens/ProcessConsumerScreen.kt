package com.roninsoulkh.mappingop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roninsoulkh.mappingop.domain.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessConsumerScreen(
    consumer: Consumer,
    initialResult: WorkResult? = null,
    onSave: (WorkResult) -> Unit,
    onCancel: () -> Unit
) {
    // Состояние формы
    var meterReading by remember { mutableStateOf(initialResult?.meterReading?.toString() ?: "") }
    var newPhone by remember { mutableStateOf(initialResult?.newPhone ?: "") }
    var comment by remember { mutableStateOf(initialResult?.comment ?: "") }

    // Состояния для выпадающих списков
    var selectedBuildingCondition by remember {
        mutableStateOf(initialResult?.buildingCondition ?: BuildingCondition.UNKNOWN)
    }
    var selectedConsumerType by remember {
        mutableStateOf(initialResult?.consumerType)
    }
    var selectedWorkType by remember {
        mutableStateOf(initialResult?.workType)
    }

    // Состояния для отображения выпадающих списков
    var showBuildingConditionDropdown by remember { mutableStateOf(false) }
    var showConsumerTypeDropdown by remember { mutableStateOf(false) }
    var showWorkTypeDropdown by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Опрацьовано: ОР ${consumer.orNumber}") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.ArrowBack, "Назад")
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Скасувати")
                    }

                    Button(
                        onClick = {
                            val result = WorkResult(
                                consumerId = consumer.id,
                                worksheetId = consumer.worksheetId,
                                meterReading = meterReading.toDoubleOrNull(),
                                newPhone = newPhone.ifEmpty { null },
                                buildingCondition = if (selectedBuildingCondition != BuildingCondition.UNKNOWN)
                                    selectedBuildingCondition else null,
                                consumerType = selectedConsumerType,
                                workType = selectedWorkType,
                                comment = comment.ifEmpty { null }
                            )
                            onSave(result)
                        }
                    ) {
                        Icon(Icons.Filled.Save, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Зберегти")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Информация о потребителе
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
                        text = "Інформація про споживача",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("ОР №${consumer.orNumber}", fontSize = 14.sp)
                    Text(consumer.shortAddress, fontSize = 12.sp)
                    Text(consumer.name, fontSize = 12.sp)
                }
            }

            // 1. Показатели счётчика
            OutlinedTextField(
                value = meterReading,
                onValueChange = { meterReading = it },
                label = { Text("Показники лічильника") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Filled.Speed, null) },
                placeholder = { Text("Введіть поточні показники") },
                singleLine = true
            )

            // 2. Номер телефона (новый)
            OutlinedTextField(
                value = newPhone,
                onValueChange = { newPhone = it },
                label = { Text("Новий номер телефону") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon = { Icon(Icons.Filled.Phone, null) },
                placeholder = { Text("+380XXXXXXXXX") },
                singleLine = true
            )

            // 3. Состояние здания (выпадающий список)
            OutlinedTextField(
                value = getBuildingConditionText(selectedBuildingCondition),
                onValueChange = { },
                label = { Text("Стан будівлі") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showBuildingConditionDropdown = true }) {
                        Icon(Icons.Filled.ArrowDropDown, null)
                    }
                },
                placeholder = { Text("Оберіть стан будівлі") }
            )

            // 4. Классификатор потребителя (выпадающий список)
            OutlinedTextField(
                value = selectedConsumerType?.let { getConsumerTypeText(it) } ?: "",
                onValueChange = { },
                label = { Text("Класифікатор споживача") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showConsumerTypeDropdown = true }) {
                        Icon(Icons.Filled.ArrowDropDown, null)
                    }
                },
                placeholder = { Text("Оберіть класифікатор") }
            )

            // 5. Тип отработки (выпадающий список)
            OutlinedTextField(
                value = selectedWorkType?.let { getWorkTypeText(it) } ?: "",
                onValueChange = { },
                label = { Text("Тип відпрацювання") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showWorkTypeDropdown = true }) {
                        Icon(Icons.Filled.ArrowDropDown, null)
                    }
                },
                placeholder = { Text("Оберіть тип відпрацювання") }
            )

            // 6. Комментарий
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Коментар") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                leadingIcon = { Icon(Icons.Filled.Comment, null) },
                placeholder = { Text("Додаткові зауваження...") },
                singleLine = false,
                maxLines = 4
            )

            // Подсказка
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Не обов'язково заповнювати всі поля",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }

    // Диалог выбора состояния здания
    if (showBuildingConditionDropdown) {
        AlertDialog(
            onDismissRequest = { showBuildingConditionDropdown = false },
            title = { Text("Стан будівлі") },
            text = {
                Column {
                    BuildingCondition.values().filter { it != BuildingCondition.UNKNOWN }.forEach { condition ->
                        ElevatedButton(
                            onClick = {
                                selectedBuildingCondition = condition
                                showBuildingConditionDropdown = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (selectedBuildingCondition == condition)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(getBuildingConditionText(condition))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Кнопка "Не вибрано"
                    OutlinedButton(
                        onClick = {
                            selectedBuildingCondition = BuildingCondition.UNKNOWN
                            showBuildingConditionDropdown = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Не вибрано")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBuildingConditionDropdown = false }) {
                    Text("Закрити")
                }
            }
        )
    }

    // Диалог выбора классификатора потребителя
    if (showConsumerTypeDropdown) {
        AlertDialog(
            onDismissRequest = { showConsumerTypeDropdown = false },
            title = { Text("Класифікатор споживача") },
            text = {
                Column {
                    ConsumerType.values().forEach { type ->
                        ElevatedButton(
                            onClick = {
                                selectedConsumerType = type
                                showConsumerTypeDropdown = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (selectedConsumerType == type)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(getConsumerTypeText(type))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Кнопка "Не вибрано"
                    OutlinedButton(
                        onClick = {
                            selectedConsumerType = null
                            showConsumerTypeDropdown = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Не вибрано")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showConsumerTypeDropdown = false }) {
                    Text("Закрити")
                }
            }
        )
    }

    // Диалог выбора типа отработки
    if (showWorkTypeDropdown) {
        AlertDialog(
            onDismissRequest = { showWorkTypeDropdown = false },
            title = { Text("Тип відпрацювання") },
            text = {
                Column {
                    WorkType.values().forEach { type ->
                        ElevatedButton(
                            onClick = {
                                selectedWorkType = type
                                showWorkTypeDropdown = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (selectedWorkType == type)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(getWorkTypeText(type))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Кнопка "Не вибрано"
                    OutlinedButton(
                        onClick = {
                            selectedWorkType = null
                            showWorkTypeDropdown = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Не вибрано")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWorkTypeDropdown = false }) {
                    Text("Закрити")
                }
            }
        )
    }
}

// Функции для отображения текста
private fun getBuildingConditionText(condition: BuildingCondition): String {
    return when (condition) {
        BuildingCondition.LIVING -> "Мешкають"
        BuildingCondition.EMPTY -> "Пустка"
        BuildingCondition.PARTIALLY_DESTROYED -> "Напівзруйнований"
        BuildingCondition.DESTROYED -> "Зруйнований"
        BuildingCondition.NOT_LIVING -> "Не мешкають"
        BuildingCondition.FORBIDDEN -> "Заборона"
        BuildingCondition.UNKNOWN -> "Не вибрано"
    }
}

private fun getConsumerTypeText(type: ConsumerType): String {
    return when (type) {
        ConsumerType.CIVILIAN -> "Цивільний"
        ConsumerType.VPO -> "ВПО"
        ConsumerType.OTHER -> "Інші особи"
    }
}

private fun getWorkTypeText(type: WorkType): String {
    return when (type) {
        WorkType.HANDED -> "Вручено в руки"
        WorkType.NOTE -> "Шпарина (записка)"
        WorkType.REFUSAL -> "Відмова"
        WorkType.PAYMENT -> "Оплата поточного"
    }
}