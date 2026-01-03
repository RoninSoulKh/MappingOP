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
import com.roninsoulkh.mappingop.ui.components.MappingCard
import com.roninsoulkh.mappingop.ui.components.MappingCustomDialog
import com.roninsoulkh.mappingop.ui.components.MappingGradientButton
import com.roninsoulkh.mappingop.ui.theme.*
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
    onManualLocationClick: () -> Unit,
    onMapClick: () -> Unit
) {
    var showResultDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Деталі споживача",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onMapClick) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = "На карту",
                            tint = CyanAction
                        )
                    }
                    if (workResult != null) {
                        IconButton(onClick = { showResultDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = "Історія",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Статус
                MappingCard {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (consumer.isProcessed) StatusGreen.copy(alpha = 0.2f)
                                else StatusRed.copy(alpha = 0.2f)
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (consumer.isProcessed) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                contentDescription = null,
                                tint = if (consumer.isProcessed) StatusGreen else StatusRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (consumer.isProcessed) "ОПРАЦЬОВАНО" else "НЕ ОПРАЦЬОВАНО",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (consumer.isProcessed) StatusGreen else StatusRed
                            )
                        }
                    }
                }

                // Головна інфо
                MappingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Numbers,
                                contentDescription = null,
                                tint = CyanAction,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Номер ОР",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = consumer.orNumber,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 28.dp, top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = null,
                                tint = CyanAction,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Адреса",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = consumer.rawAddress,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = CyanAction,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Контрагент",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = consumer.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Додаткова інфо
                MappingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DetailRow(
                            icon = Icons.Filled.Phone,
                            label = "Телефон",
                            value = consumer.phone ?: "не вказано"
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Сума боргу",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${consumer.debtAmount ?: 0.0} грн",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if ((consumer.debtAmount ?: 0.0) > 0) StatusRed else StatusGreen
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Номер лічильника",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = consumer.meterNumber ?: "не вказано",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onManualLocationClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.EditLocation,
                        contentDescription = null,
                        tint = CyanAction,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Змінити координати вручну",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 0.dp)
                        .navigationBarsPadding()
                ) {
                    MappingGradientButton(
                        text = if (consumer.isProcessed) "РЕДАГУВАТИ" else "ОПРАЦЮВАТИ",
                        icon = if (consumer.isProcessed) Icons.Filled.Edit else Icons.Filled.CheckCircle,
                        onClick = onProcessClick
                    )
                }
            }
        }
    }

    if (showResultDialog && workResult != null) {
        MappingCustomDialog(
            title = "Результат відпрацювання",
            onDismiss = { showResultDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(workResult.processedAt))}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(8.dp))

                ResultRow("Тип:", workResult.workType?.let { getWorkTypeText(it) } ?: "-")
                ResultRow("Стан:", workResult.buildingCondition?.let { getBuildingConditionText(it) } ?: "-")
                ResultRow("Лічильник:", workResult.meterReading?.toString() ?: "-")

                if (!workResult.newPhone.isNullOrEmpty()) {
                    ResultRow("Новий телефон:", workResult.newPhone)
                }
                if (!workResult.comment.isNullOrEmpty()) {
                    ResultRow("Коментар:", workResult.comment)
                }

                if (workResult.photos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Медіа файли (${workResult.photos.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(top = 8.dp)
                    ) {
                        items(workResult.photos) { path ->
                            val isVideo = path.endsWith(".mp4", ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { openMediaFile(context, path) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isVideo) {
                                    Icon(Icons.Filled.PlayCircle, null, tint = Color.White)
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

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = { showResultDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Закрити", color = CyanAction, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- ДОПОМІЖНІ КОМПОНЕНТИ ---

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanAction,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
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
fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
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