package com.roninsoulkh.mappingop.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.roninsoulkh.mappingop.domain.models.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumerDetailScreen(
    consumer: Consumer,
    workResult: WorkResult?,
    onBackClick: () -> Unit,
    onProcessClick: () -> Unit,
    onManualLocationClick: () -> Unit // <--- Новый параметр для ручной карты
) {
    var showResultDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                    if (workResult != null) {
                        IconButton(onClick = { showResultDialog = true }) {
                            Icon(Icons.Filled.Description, "Показати результат", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onProcessClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (consumer.isProcessed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(if (consumer.isProcessed) Icons.Filled.Edit else Icons.Filled.CheckCircle, null, Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (consumer.isProcessed) "Редагувати" else "Опрацювати")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AssistChip(
                    onClick = { if (workResult != null) showResultDialog = true },
                    label = { Text(if (consumer.isProcessed) "ОПРАЦЬОВАНО" else "НЕ ОПРАЦЬОВАНО", fontSize = 12.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (consumer.isProcessed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    ),
                    leadingIcon = { if (consumer.isProcessed) Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                )
            }

            InfoCard(title = "Номер ОР", value = consumer.orNumber, icon = Icons.Filled.Numbers)
            InfoCard(title = "Адреса", value = consumer.rawAddress, icon = Icons.Filled.Home)
            InfoCard(title = "Контрагент", value = consumer.name, icon = Icons.Filled.Person)
            InfoCard(title = "Телефон", value = consumer.phone ?: "не вказано", icon = Icons.Filled.Phone)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoCard(title = "Сума боргу", value = "${consumer.debtAmount ?: 0.0} грн", icon = Icons.Filled.AttachMoney, modifier = Modifier.weight(1f))
                InfoCard(title = "Номер лічильника", value = consumer.meterNumber ?: "не вказано", icon = Icons.Filled.Speed, modifier = Modifier.weight(1f))
            }

            // --- НОВАЯ КНОПКА РУЧНОЙ КОРРЕКЦИИ ---
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onManualLocationClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
            ) {
                Icon(Icons.Filled.EditLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Змінити координати вручну")
            }
        }
    }

    if (showResultDialog && workResult != null) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            icon = { Icon(Icons.Filled.Description, null) },
            title = { Text("Результат відпрацювання") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(workResult.processedAt))}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider()

                    ResultRow("Тип:", workResult.workType?.let { getWorkTypeText(it) } ?: "-")
                    ResultRow("Стан:", workResult.buildingCondition?.let { getBuildingConditionText(it) } ?: "-")
                    ResultRow("Лічильник:", workResult.meterReading?.toString() ?: "-")

                    if (!workResult.newPhone.isNullOrEmpty()) ResultRow("Новий телефон:", workResult.newPhone)
                    if (!workResult.comment.isNullOrEmpty()) ResultRow("Коментар:", workResult.comment)

                    // ГАЛЕРЕЯ
                    if (workResult.photos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Медіа файли (${workResult.photos.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("(Натисніть для перегляду)", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        ) {
                            items(workResult.photos) { path ->
                                val isVideo = path.endsWith(".mp4", ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            openMediaFile(context, path)
                                        }
                                ) {
                                    if (isVideo) {
                                        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.PlayCircle, null, tint = Color.White)
                                        }
                                    } else {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = File(path)),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResultDialog = false }) { Text("Закрити") }
            }
        )
    }
}

fun openMediaFile(context: Context, path: String) {
    try {
        val file = File(path)
        if (!file.exists()) return
        val isVideo = path.endsWith(".mp4", ignoreCase = true)
        val mimeType = if (isVideo) "video/*" else "image/*"
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun InfoCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
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

private fun getWorkTypeText(type: WorkType): String {
    return when (type) {
        WorkType.HANDED -> "Вручено в руки"
        WorkType.NOTE -> "Шпарина (записка)"
        WorkType.REFUSAL -> "Відмова"
        WorkType.PAYMENT -> "Оплата поточного"
    }
}