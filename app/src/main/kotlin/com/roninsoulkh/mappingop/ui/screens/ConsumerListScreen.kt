package com.roninsoulkh.mappingop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    // --- СОСТОЯНИЯ ФИЛЬТРОВ ---
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(StatusFilter.ALL) }
    var sortOption by remember { mutableStateOf(SortOption.ADDRESS_AZ) }
    var selectedCity by remember { mutableStateOf<String?>(null) } // null = Все населенные пункты

    // --- ЛОГИКА "УМНОГО" СПИСКА ГОРОДОВ ---
    val availableCities = remember(consumers) {
        consumers.map { extractCityFromAddress(it.rawAddress) }
            .distinct()
            .sorted()
    }

    // --- ГЛАВНАЯ ЛОГИКА ФИЛЬТРАЦИИ ---
    val filteredConsumers = remember(consumers, searchQuery, statusFilter, sortOption, selectedCity) {
        consumers.filter { consumer ->
            // 1. Поиск (по всем полям)
            val query = searchQuery.lowercase().trim()
            val matchesSearch = query.isEmpty() ||
                    consumer.name.lowercase().contains(query) ||
                    consumer.orNumber.contains(query) ||
                    consumer.rawAddress.lowercase().contains(query) ||
                    (consumer.meterNumber?.contains(query) == true) ||
                    (consumer.phone?.contains(query) == true)

            // 2. Статус (Обработан/Нет)
            val matchesStatus = when (statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.PROCESSED -> consumer.isProcessed
                StatusFilter.UNPROCESSED -> !consumer.isProcessed
            }

            // 3. Населенный пункт
            val matchesCity = selectedCity == null || extractCityFromAddress(consumer.rawAddress) == selectedCity

            matchesSearch && matchesStatus && matchesCity
        }.sortedWith(
            // 4. Сортировка
            when (sortOption) {
                SortOption.ADDRESS_AZ -> compareBy { it.rawAddress }
                SortOption.DEBT_DESC -> compareByDescending { it.debtAmount ?: 0.0 }
                SortOption.DEBT_ASC -> compareBy { it.debtAmount ?: 0.0 }
            }
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding() // Отступ от статус-бара (часы/зарядка)
            ) {
                // Верхняя панель с кнопкой Назад и Поиском
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Назад")
                    }

                    // Поле поиска (теперь занимает всё место, так как кнопки экспорта нет)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Пошук (ПІБ, Адреса, ОР)") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, "Очистити")
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                }

                // Панель фильтров (Горизонтальная прокрутка)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Кнопка Сортировки
                    item {
                        SortFilterChip(currentSort = sortOption, onSortSelected = { sortOption = it })
                    }

                    // 2. Кнопка Статуса
                    item {
                        StatusFilterChip(currentStatus = statusFilter, onStatusSelected = { statusFilter = it })
                    }

                    // 3. Кнопка Городов (Показываем только если городов больше 1)
                    if (availableCities.size > 1) {
                        item {
                            CityFilterChip(
                                cities = availableCities,
                                selectedCity = selectedCity,
                                onCitySelected = { selectedCity = it }
                            )
                        }
                    }
                }

                Divider()
            }
        }
    ) { paddingValues ->
        if (filteredConsumers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.SearchOff, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Нічого не знайдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                item {
                    Text(
                        text = "Знайдено: ${filteredConsumers.size} (Всього: ${consumers.size})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(filteredConsumers) { consumer ->
                    ConsumerCard(
                        consumer = consumer,
                        onClick = { onConsumerClick(consumer) }
                    )
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

// --- ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ ---

@Composable
fun SortFilterChip(currentSort: SortOption, onSortSelected: (SortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = true, // Всегда активен
            onClick = { expanded = true },
            label = { Text(currentSort.title) },
            leadingIcon = { Icon(Icons.Filled.Sort, null) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) }
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOption.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.title) },
                    onClick = {
                        onSortSelected(option)
                        expanded = false
                    },
                    leadingIcon = {
                        if (option == currentSort) Icon(Icons.Filled.Check, null)
                    }
                )
            }
        }
    }
}

@Composable
fun StatusFilterChip(currentStatus: StatusFilter, onStatusSelected: (StatusFilter) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = currentStatus != StatusFilter.ALL,
            onClick = { expanded = true },
            label = { Text(currentStatus.title) },
            leadingIcon = { Icon(Icons.Filled.FilterList, null) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) }
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            StatusFilter.values().forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.title) },
                    onClick = {
                        onStatusSelected(status)
                        expanded = false
                    },
                    leadingIcon = {
                        if (status == currentStatus) Icon(Icons.Filled.Check, null)
                    }
                )
            }
        }
    }
}

@Composable
fun CityFilterChip(cities: List<String>, selectedCity: String?, onCitySelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = selectedCity != null,
            onClick = { expanded = true },
            label = { Text(selectedCity ?: "Всі нас. пункти") },
            leadingIcon = { Icon(Icons.Filled.LocationCity, null) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 300.dp) // Ограничиваем высоту
        ) {
            DropdownMenuItem(
                text = { Text("Всі населені пункти") },
                onClick = {
                    onCitySelected(null)
                    expanded = false
                },
                leadingIcon = {
                    if (selectedCity == null) Icon(Icons.Filled.Check, null)
                }
            )
            Divider()
            cities.forEach { city ->
                DropdownMenuItem(
                    text = { Text(city) },
                    onClick = {
                        onCitySelected(city)
                        expanded = false
                    },
                    leadingIcon = {
                        if (city == selectedCity) Icon(Icons.Filled.Check, null)
                    }
                )
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
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    Icon(Icons.Filled.CheckCircle, "Обработан", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(consumer.shortAddress, fontSize = 14.sp)
            Text(consumer.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Борг: ${consumer.debtAmount ?: 0.0} грн",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Ліч: ${consumer.meterNumber ?: "немає"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- ЛОГИКА ПАРСИНГА АДРЕСА ---
fun extractCityFromAddress(address: String): String {
    val parts = address.split(",").map { it.trim() }

    // Ищем часть, которая начинается с маркера населенного пункта
    val cityPart = parts.find { part ->
        part.startsWith("с. ", ignoreCase = true) ||
                part.startsWith("м. ", ignoreCase = true) ||
                part.startsWith("смт ", ignoreCase = true) ||
                part.startsWith("сел. ", ignoreCase = true) ||
                part.startsWith("с-ще ", ignoreCase = true)
    }

    return cityPart ?: "Інше"
}

// --- ENUMS ---
enum class StatusFilter(val title: String) {
    ALL("Всі"),
    PROCESSED("Опрацьовані"),
    UNPROCESSED("Не опрацьовані")
}

enum class SortOption(val title: String) {
    ADDRESS_AZ("Адреса (А-Я)"),
    DEBT_DESC("Борг (Більше-Менше)"),
    DEBT_ASC("Борг (Менше-Більше)")
}