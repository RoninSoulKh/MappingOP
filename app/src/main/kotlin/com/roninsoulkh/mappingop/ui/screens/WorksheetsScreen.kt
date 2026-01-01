package com.roninsoulkh.mappingop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roninsoulkh.mappingop.domain.models.Worksheet
import com.roninsoulkh.mappingop.ui.components.WorksheetCard
import com.roninsoulkh.mappingop.ui.components.MappingCustomDialog
import com.roninsoulkh.mappingop.ui.components.MappingGradientButton
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorksheetsScreen(
    worksheets: List<Worksheet>,
    onWorksheetClick: (Worksheet) -> Unit,
    onAddWorksheet: () -> Unit,
    onDeleteWorksheet: (Worksheet) -> Unit,
    onRenameWorksheet: (Worksheet, String) -> Unit,
    onViewResults: () -> Unit,
    onBackClick: () -> Unit
) {
    var showMenuForWorksheet by remember { mutableStateOf<Worksheet?>(null) }
    var showRenameDialog by remember { mutableStateOf<Worksheet?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Мої відомості", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
        // 🔥 ПЛЮСИК ВИДАЛЕНО (FAB removed)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
        ) {
            if (worksheets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Немає відомостей.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(worksheets) { worksheet ->
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        val dateStr = dateFormat.format(Date(worksheet.importDate))

                        WorksheetCard(
                            filename = worksheet.fileName,
                            date = dateStr,
                            processedCount = worksheet.processedCount,
                            totalCount = worksheet.totalConsumers,
                            onClick = { onWorksheetClick(worksheet) },
                            onMenuClick = { showMenuForWorksheet = worksheet }
                        )
                    }
                }
            }
        }

        // 🔥 КРАСИВЕ МЕНЮ ДІЙ (Custom Dialog)
        if (showMenuForWorksheet != null) {
            MappingCustomDialog(
                title = "Дії з файлом",
                onDismiss = { showMenuForWorksheet = null }
            ) {
                // Кнопка Перейменувати
                OutlinedButton(
                    onClick = {
                        showRenameDialog = showMenuForWorksheet
                        showMenuForWorksheet = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("Перейменувати", color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Кнопка Видалити
                Button(
                    onClick = {
                        onDeleteWorksheet(showMenuForWorksheet!!)
                        showMenuForWorksheet = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("Видалити файл", color = androidx.compose.ui.graphics.Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = { showMenuForWorksheet = null }) {
                    Text("Скасувати", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 🔥 КРАСИВИЙ ДІАЛОГ ПЕРЕЙМЕНУВАННЯ
        if (showRenameDialog != null) {
            RenameDialogInternal(
                initialName = showRenameDialog!!.fileName,
                onDismiss = { showRenameDialog = null },
                onConfirm = { newName ->
                    onRenameWorksheet(showRenameDialog!!, newName)
                    showRenameDialog = null
                }
            )
        }
    }
}

@Composable
fun RenameDialogInternal(initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialName) }

    MappingCustomDialog(
        title = "Перейменувати",
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Назва файлу") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        MappingGradientButton(
            text = "ЗБЕРЕГТИ",
            onClick = { onConfirm(text) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onDismiss) {
            Text("Скасувати", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}