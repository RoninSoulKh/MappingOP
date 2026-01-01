package com.roninsoulkh.mappingop.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roninsoulkh.mappingop.domain.models.Consumer
import com.roninsoulkh.mappingop.ui.components.ConsumerItemCard
import com.roninsoulkh.mappingop.ui.components.MappingFilterChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumerListScreen(
    consumers: List<Consumer>,
    onConsumerClick: (Consumer) -> Unit,
    onBackClick: () -> Unit
) {
    // --- СТАНИ ФІЛЬТРІВ ---
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(StatusFilter.ALL) }
    var sortOption by remember { mutableStateOf(SortOption.ADDRESS_AZ) }
    var selectedCity by remember { mutableStateOf<String?>(null) }

    // --- ЛОГИКА МІСТ ---
    val availableCities = remember(consumers) {
        consumers.map { extractCityFromAddress(it.rawAddress) }
            .distinct()
            .sorted()
    }

    // --- ФІЛЬТРАЦІЯ ---
    val filteredConsumers = remember(consumers, searchQuery, statusFilter, sortOption, selectedCity) {
        consumers.filter { consumer ->
            // 1. Пошук
            val query = searchQuery.lowercase().trim()
            val matchesSearch = query.isEmpty() ||
                    consumer.name.lowercase().contains(query) ||
                    consumer.orNumber.contains(query) ||
                    consumer.rawAddress.lowercase().contains(query) ||
                    (consumer.meterNumber?.contains(query) == true) ||
                    (consumer.phone?.contains(query) == true)

            // 2. Статус
            val matchesStatus = when (statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.PROCESSED -> consumer.isProcessed
                StatusFilter.UNPROCESSED -> !consumer.isProcessed
            }

            // 3. Місто
            val matchesCity = selectedCity == null || extractCityFromAddress(consumer.rawAddress) == selectedCity

            matchesSearch && matchesStatus && matchesCity
        }.sortedWith(
            // 4. Сортування
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
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                // Хедер
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Назад")
                    }

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

                // Фільтри (Чіпси)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Сортування
                    item {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            MappingFilterChip(
                                text = sortOption.title,
                                selected = true,
                                onClick = { expanded = true }
                            )
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                SortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.title) },
                                        onClick = { sortOption = option; expanded = false }
                                    )
                                }
                            }
                        }
                    }

                    // Статуси
                    item {
                        MappingFilterChip(
                            text = "Всі",
                            selected = statusFilter == StatusFilter.ALL,
                            onClick = { statusFilter = StatusFilter.ALL }
                        )
                    }
                    item {
                        MappingFilterChip(
                            text = "Опрацьовані",
                            selected = statusFilter == StatusFilter.PROCESSED,
                            onClick = { statusFilter = StatusFilter.PROCESSED }
                        )
                    }
                    item {
                        MappingFilterChip(
                            text = "Боржники",
                            selected = statusFilter == StatusFilter.UNPROCESSED,
                            onClick = { statusFilter = StatusFilter.UNPROCESSED }
                        )
                    }

                    // Міста
                    if (availableCities.size > 1) {
                        item {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                MappingFilterChip(
                                    text = selectedCity ?: "Місто",
                                    selected = selectedCity != null,
                                    onClick = { expanded = true }
                                )
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Всі") },
                                        onClick = { selectedCity = null; expanded = false }
                                    )
                                    availableCities.forEach { city ->
                                        DropdownMenuItem(
                                            text = { Text(city) },
                                            onClick = { selectedCity = city; expanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                Text("Нічого не знайдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item {
                    Text(
                        text = "Знайдено: ${filteredConsumers.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(filteredConsumers) { consumer ->
                    // 🔥 НОВА КАРТКА
                    ConsumerItemCard(
                        address = consumer.rawAddress,
                        name = consumer.name,
                        orNumber = consumer.orNumber,
                        debt = consumer.debtAmount ?: 0.0,
                        meterNumber = consumer.meterNumber,
                        isProcessed = consumer.isProcessed,
                        onClick = { onConsumerClick(consumer) }
                    )
                }
            }
        }
    }
}

// --- УТИЛІТИ ---
fun extractCityFromAddress(address: String): String {
    val parts = address.split(",").map { it.trim() }
    val cityPart = parts.find { part ->
        part.startsWith("с. ", true) || part.startsWith("м. ", true) || part.startsWith("смт ", true)
    }
    return cityPart ?: "Інше"
}

enum class StatusFilter(val title: String) { ALL("Всі"), PROCESSED("Опрацьовані"), UNPROCESSED("Не опрацьовані") }
enum class SortOption(val title: String) { ADDRESS_AZ("А-Я"), DEBT_DESC("Борг ↓"), DEBT_ASC("Борг ↑") }