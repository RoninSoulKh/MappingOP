package com.roninsoulkh.mappingop.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.roninsoulkh.mappingop.domain.models.*
import com.roninsoulkh.mappingop.utils.openMediaFile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessConsumerScreen(
    consumer: Consumer,
    initialResult: WorkResult? = null,
    onSave: (WorkResult) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var meterReading by remember { mutableStateOf(initialResult?.meterReading?.toString() ?: "") }
    var newPhone by remember { mutableStateOf(initialResult?.newPhone ?: "") }
    var comment by remember { mutableStateOf(initialResult?.comment ?: "") }

    val photoPaths = remember { mutableStateListOf<String>() }

    LaunchedEffect(initialResult) {
        if (initialResult != null && photoPaths.isEmpty()) {
            photoPaths.addAll(initialResult.photos)
        }
    }

    var selectedBuildingCondition by remember { mutableStateOf(initialResult?.buildingCondition ?: BuildingCondition.UNKNOWN) }
    var selectedConsumerType by remember { mutableStateOf(initialResult?.consumerType) }
    var selectedWorkType by remember { mutableStateOf(initialResult?.workType) }

    var showBuildingConditionDropdown by remember { mutableStateOf(false) }
    var showConsumerTypeDropdown by remember { mutableStateOf(false) }
    var showWorkTypeDropdown by remember { mutableStateOf(false) }
    var showMediaSourceDialog by remember { mutableStateOf(false) }

    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var isVideoMode by remember { mutableStateOf(false) }
    var currentPhotoPath by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val savedFile = copyUriToInternalStorage(context, it)
            savedFile?.let { file -> photoPaths.add(file.absolutePath) }
        }
    }

    val cameraPhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhotoPath != null) {
            photoPaths.add(currentPhotoPath!!)
        }
    }

    val cameraVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success && currentPhotoPath != null) {
            photoPaths.add(currentPhotoPath!!)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (cameraGranted) {
            val (uri, path) = createMediaFile(context, isVideoMode)
            currentPhotoPath = path
            if (isVideoMode) {
                cameraVideoLauncher.launch(uri)
            } else {
                cameraPhotoLauncher.launch(uri)
            }
        } else {
            Toast.makeText(context, "Потрібен дозвіл на камеру", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Опрацьовано: ОР ${consumer.orNumber}") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Filled.ArrowBack, "Назад") }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onCancel) { Text("Скасувати") }

                    Button(
                        onClick = {
                            val result = WorkResult(
                                consumerId = consumer.id,
                                worksheetId = consumer.worksheetId,
                                meterReading = meterReading.toDoubleOrNull(),
                                newPhone = newPhone.ifEmpty { null },
                                buildingCondition = if (selectedBuildingCondition != BuildingCondition.UNKNOWN) selectedBuildingCondition else null,
                                consumerType = selectedConsumerType,
                                workType = selectedWorkType,
                                comment = comment.ifEmpty { null },
                                photos = photoPaths.toList()
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Інформація про споживача", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 16.sp)
                    Text("ОР №${consumer.orNumber}", fontSize = 14.sp)
                    Text(consumer.shortAddress, fontSize = 12.sp)
                    Text(consumer.name, fontSize = 12.sp)
                }
            }

            OutlinedTextField(
                value = meterReading,
                onValueChange = { meterReading = it },
                label = { Text("Показники лічильника") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Filled.Speed, null) }
            )

            OutlinedTextField(
                value = newPhone,
                onValueChange = { newPhone = it },
                label = { Text("Новий номер телефону") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon = { Icon(Icons.Filled.Phone, null) }
            )

            // 👇 ИЗМЕНЕНИЕ: Обернули в Box, чтобы клик работал по всей площади
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = getBuildingConditionText(selectedBuildingCondition),
                    onValueChange = { },
                    label = { Text("Стан будівлі") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) }
                )
                // Невидимый слой, ловящий клик
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showBuildingConditionDropdown = true }
                )
            }

            // 👇 ИЗМЕНЕНИЕ: То же самое для Классификатора
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedConsumerType?.let { getConsumerTypeText(it) } ?: "",
                    onValueChange = { },
                    label = { Text("Класифікатор споживача") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showConsumerTypeDropdown = true }
                )
            }

            // 👇 ИЗМЕНЕНИЕ: То же самое для Типа отработки
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedWorkType?.let { getWorkTypeText(it) } ?: "",
                    onValueChange = { },
                    label = { Text("Тип відпрацювання") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showWorkTypeDropdown = true }
                )
            }

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Коментар") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Comment, null) }
            )

            Text("Фото та Відео фіксація", style = MaterialTheme.typography.titleMedium)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(100.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.size(100.dp).clickable { showMediaSourceDialog = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.AddAPhoto, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("Додати", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }

                items(photoPaths) { path ->
                    val isVideo = path.endsWith(".mp4", ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { openMediaFile(context, path) }
                    ) {
                        if (isVideo) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.PlayCircle, null, tint = Color.White, modifier = Modifier.size(48.dp))
                            }
                        } else {
                            Image(
                                painter = rememberAsyncImagePainter(model = File(path)),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        IconButton(
                            onClick = { photoPaths.remove(path) },
                            modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(50))
                        ) {
                            Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showMediaSourceDialog) {
        AlertDialog(
            onDismissRequest = { showMediaSourceDialog = false },
            title = { Text("Додати медіа") },
            text = { Text("Що ви хочете додати?") },
            confirmButton = {
                TextButton(onClick = {
                    showMediaSourceDialog = false
                    isVideoMode = true
                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                }) {
                    Icon(Icons.Filled.Videocam, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Відео")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showMediaSourceDialog = false
                        isVideoMode = false
                        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                    }) {
                        Icon(Icons.Filled.PhotoCamera, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Фото")
                    }
                    TextButton(onClick = {
                        showMediaSourceDialog = false
                        galleryLauncher.launch("*/*")
                    }) { Text("Галерея") }
                }
            }
        )
    }

    if (showBuildingConditionDropdown) {
        AlertDialog(
            onDismissRequest = { showBuildingConditionDropdown = false },
            title = { Text("Стан будівлі") },
            text = {
                Column {
                    BuildingCondition.values().filter { it != BuildingCondition.UNKNOWN }.forEach { condition ->
                        ElevatedButton(
                            onClick = { selectedBuildingCondition = condition; showBuildingConditionDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(getBuildingConditionText(condition)) }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showConsumerTypeDropdown) {
        AlertDialog(
            onDismissRequest = { showConsumerTypeDropdown = false },
            title = { Text("Класифікатор") },
            text = {
                Column {
                    ConsumerType.values().forEach { type ->
                        ElevatedButton(
                            onClick = { selectedConsumerType = type; showConsumerTypeDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(getConsumerTypeText(type)) }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showWorkTypeDropdown) {
        AlertDialog(
            onDismissRequest = { showWorkTypeDropdown = false },
            title = { Text("Тип відпрацювання") },
            text = {
                Column {
                    WorkType.values().forEach { type ->
                        ElevatedButton(
                            onClick = { selectedWorkType = type; showWorkTypeDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(getWorkTypeText(type)) }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

fun createMediaFile(context: Context, isVideo: Boolean): Pair<Uri, String> {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val prefix = if (isVideo) "VID_" else "IMG_"
    val suffix = if (isVideo) ".mp4" else ".jpg"
    val storageDir = context.getExternalFilesDir("my_images")
    val file = File.createTempFile(prefix + timeStamp + "_", suffix, storageDir)

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    return Pair(uri, file.absolutePath)
}

fun copyUriToInternalStorage(context: Context, uri: Uri): File? {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val type = context.contentResolver.getType(uri)
    val isVideo = type?.startsWith("video/") == true
    val suffix = if (isVideo) ".mp4" else ".jpg"

    val file = File(context.filesDir, "MEDIA_${timeStamp}$suffix")
    val outputStream = FileOutputStream(file)

    inputStream.use { input -> outputStream.use { output -> input.copyTo(output) } }
    return file
}

fun getFileFromUri(context: Context, uri: Uri): File? { return null }

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