package com.roninsoulkh.mappingop.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.roninsoulkh.mappingop.domain.models.*
import com.roninsoulkh.mappingop.utils.openMediaFile
import com.roninsoulkh.mappingop.ui.components.* import com.roninsoulkh.mappingop.ui.theme.CyanAction
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

    // Стани полів вводу
    var meterReading by remember { mutableStateOf(initialResult?.meterReading?.toString() ?: "") }
    var newPhone by remember { mutableStateOf(initialResult?.newPhone ?: "") }
    var comment by remember { mutableStateOf(initialResult?.comment ?: "") }

    // Фото
    val photoPaths = remember { mutableStateListOf<String>() }

    // Ініціалізація фото при редагуванні
    LaunchedEffect(initialResult) {
        if (initialResult != null && photoPaths.isEmpty()) {
            photoPaths.addAll(initialResult.photos)
        }
    }

    // Стани випадаючих списків
    var selectedBuildingCondition by remember { mutableStateOf(initialResult?.buildingCondition ?: BuildingCondition.UNKNOWN) }
    var selectedConsumerType by remember { mutableStateOf(initialResult?.consumerType) }
    var selectedWorkType by remember { mutableStateOf(initialResult?.workType) }

    // Діалог вибору медіа
    var showMediaSourceDialog by remember { mutableStateOf(false) }
    var isVideoMode by remember { mutableStateOf(false) }
    var currentPhotoPath by remember { mutableStateOf<String?>(null) }

    // --- ЛОГІКА КАМЕРИ ТА ГАЛЕРЕЇ ---

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

    fun launchCamera() {
        val (uri, path) = createMediaFile(context, isVideoMode)
        currentPhotoPath = path
        if (isVideoMode) {
            cameraVideoLauncher.launch(uri)
        } else {
            cameraPhotoLauncher.launch(uri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(context, "Потрібен дозвіл на камеру", Toast.LENGTH_LONG).show()
        }
    }

    // --- ЕКРАН ---
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Опрацювання: ОР ${consumer.orNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        // 🔥 Головний контейнер Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // 1. СКРОЛЛ-ЗОНА (Займає все місце, крім кнопок знизу)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // 1. ІНФО (Компактніше)
                MappingCard {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = consumer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = consumer.rawAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    }
                }

                // 2. ПОЛЯ ВВОДУ
                MappingTextField(
                    value = meterReading,
                    onValueChange = { meterReading = it },
                    label = "Показники лічильника",
                    icon = Icons.Filled.Speed,
                    keyboardType = KeyboardType.Number
                )

                MappingTextField(
                    value = newPhone,
                    onValueChange = { newPhone = it },
                    label = "Новий номер телефону",
                    icon = Icons.Filled.Phone,
                    keyboardType = KeyboardType.Phone
                )

                // 3. DROPDOWNS
                MappingDropdownField(
                    label = "Стан будівлі",
                    selectedValue = getBuildingConditionText(selectedBuildingCondition),
                    items = BuildingCondition.values().filter { it != BuildingCondition.UNKNOWN }.toList(),
                    itemToString = { getBuildingConditionText(it) },
                    onItemSelected = { selectedBuildingCondition = it },
                    icon = Icons.Filled.HomeWork
                )

                MappingDropdownField(
                    label = "Класифікатор споживача",
                    selectedValue = selectedConsumerType?.let { getConsumerTypeText(it) } ?: "Не вибрано",
                    items = ConsumerType.values().toList(),
                    itemToString = { getConsumerTypeText(it) },
                    onItemSelected = { selectedConsumerType = it },
                    icon = Icons.Filled.PersonSearch
                )

                MappingDropdownField(
                    label = "Тип відпрацювання",
                    selectedValue = selectedWorkType?.let { getWorkTypeText(it) } ?: "Не вибрано",
                    items = WorkType.values().toList(),
                    itemToString = { getWorkTypeText(it) },
                    onItemSelected = { selectedWorkType = it },
                    icon = Icons.Filled.AssignmentTurnedIn
                )

                MappingTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = "Коментар",
                    icon = Icons.Filled.Comment
                )

                // 4. МЕДІА
                Column {
                    Text(
                        text = "Фото та Відео фіксація",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().height(90.dp)
                    ) {
                        item {
                            AddMediaButton(onClick = { showMediaSourceDialog = true })
                        }

                        items(photoPaths) { path ->
                            val isVideo = path.endsWith(".mp4", ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { openMediaFile(context, path) }
                            ) {
                                if (isVideo) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.PlayCircle, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                    }
                                } else {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = File(path)),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                // Кнопка видалення
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                                        .clickable { photoPaths.remove(path) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // 🔥 2. КОМПАКТНА ПАНЕЛЬ ЗНИЗУ
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        // 🔥 ВИПРАВЛЕНО ТУТ:
                        .navigationBarsPadding() // Сначала отступ системы
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 0.dp) // Знизу 0.dp
                ) {
                    MappingGradientButton(
                        text = "ЗБЕРЕГТИ",
                        icon = Icons.Filled.Save,
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
                    )
                }
            }
        }
    }

    // ДІАЛОГ ВИБОРУ МЕДІА (Без змін)
    if (showMediaSourceDialog) {
        MappingCustomDialog(
            title = "Додати медіа",
            onDismiss = { showMediaSourceDialog = false }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        showMediaSourceDialog = false
                        isVideoMode = false
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchCamera()
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = CyanAction
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Фото", style = MaterialTheme.typography.bodyMedium)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        showMediaSourceDialog = false
                        isVideoMode = true
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchCamera()
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.Videocam,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = CyanAction
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Відео", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    showMediaSourceDialog = false
                    galleryLauncher.launch("*/*")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Вибрати з Галереї", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// --- ХЕЛПЕРИ ---

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

// --- ENUM ТЕКСТИ ---

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