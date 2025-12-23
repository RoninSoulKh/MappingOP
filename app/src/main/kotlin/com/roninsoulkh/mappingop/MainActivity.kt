package com.roninsoulkh.mappingop

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roninsoulkh.mappingop.data.parser.ExcelParser
import com.roninsoulkh.mappingop.domain.models.Consumer
import com.roninsoulkh.mappingop.ui.screens.*
import com.roninsoulkh.mappingop.domain.models.WorkResult
import com.roninsoulkh.mappingop.domain.models.Worksheet
import com.roninsoulkh.mappingop.ui.theme.MappingOPTheme
import com.roninsoulkh.mappingop.data.repository.AppDataRepository
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val repository = AppDataRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MappingOPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(repository)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(repository: AppDataRepository) {
    // Состояние навигации
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Worksheets) }
    var selectedWorksheetId by remember { mutableStateOf<String?>(null) }
    var selectedConsumer by remember { mutableStateOf<Consumer?>(null) }

    // ДЛЯ Toast сообщений
    val context = LocalContext.current
    var showSuccessToast by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    // Показываем Toast если нужно
    LaunchedEffect(showSuccessToast) {
        if (showSuccessToast) {
            android.widget.Toast.makeText(context, successMessage, android.widget.Toast.LENGTH_SHORT).show()
            showSuccessToast = false
        }
    }

    // Получаем данные - ПРОСТОЙ ВАРИАНТ
    var worksheets by remember {
        mutableStateOf(repository.getAllWorksheets())
    }

    // Следим за изменениями экрана и обновляем данные
    LaunchedEffect(currentScreen) {
        if (currentScreen == AppScreen.Worksheets) {
            worksheets = repository.getAllWorksheets()
        }
    }

    // Получаем потребителей выбранной ведомости
    val worksheetConsumers = selectedWorksheetId?.let { worksheetId ->
        remember(worksheetId) { repository.getConsumersByWorksheetId(worksheetId) }
    } ?: emptyList()

    // Получаем результат отработки для выбранного потребителя
    val workResultForSelectedConsumer = selectedConsumer?.let { consumer ->
        remember(consumer.id) { repository.getWorkResultByConsumerId(consumer.id) }
    }

    when (currentScreen) {
        AppScreen.Worksheets -> {
            WorksheetsScreen(
                worksheets = worksheets,
                onWorksheetClick = { worksheet ->
                    selectedWorksheetId = worksheet.id
                    currentScreen = AppScreen.ConsumerList
                },
                onAddWorksheet = {
                    currentScreen = AppScreen.ImportExcel
                },
                onBackClick = { /* Выход из приложения */ }
            )
        }

        AppScreen.ConsumerList -> {
            ConsumerListScreen(
                consumers = worksheetConsumers,
                onConsumerClick = { consumer ->
                    selectedConsumer = consumer
                    currentScreen = AppScreen.ConsumerDetail
                },
                onBackClick = {
                    selectedWorksheetId = null
                    currentScreen = AppScreen.Worksheets
                }
            )
        }

        AppScreen.ConsumerDetail -> {
            if (selectedConsumer != null) {
                ConsumerDetailScreen(
                    consumer = selectedConsumer!!,
                    onBackClick = {
                        selectedConsumer = null
                        currentScreen = AppScreen.ConsumerList
                    },
                    onProcessClick = {
                        currentScreen = AppScreen.ProcessConsumer
                    }
                )
            }
        }

        AppScreen.ProcessConsumer -> {
            if (selectedConsumer != null) {
                ProcessConsumerScreen(
                    consumer = selectedConsumer!!,
                    initialResult = workResultForSelectedConsumer,
                    onSave = { result ->
                        repository.saveWorkResult(result)
                        selectedConsumer = null
                        currentScreen = AppScreen.ConsumerList

                        // Показываем Toast через состояние
                        successMessage = "Дані збережено"
                        showSuccessToast = true

                        // Обновляем список ведомостей
                        worksheets = repository.getAllWorksheets()
                    },
                    onCancel = {
                        currentScreen = AppScreen.ConsumerDetail
                    }
                )
            }
        }

        AppScreen.ImportExcel -> {
            ImportExcelScreen(
                repository = repository,
                context = context,
                onImportComplete = {
                    // После импорта возвращаемся к списку ведомостей
                    currentScreen = AppScreen.Worksheets

                    // ОБНОВЛЯЕМ данные ведомостей
                    worksheets = repository.getAllWorksheets()
                },
                onBackClick = {
                    currentScreen = AppScreen.Worksheets
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExcelScreen(
    repository: AppDataRepository,
    context: android.content.Context,
    onImportComplete: () -> Unit,
    onBackClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Функция для получения имени файла
    fun getFileName(uri: Uri): String {
        var fileName = ""
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }
        }
        return fileName.ifEmpty { uri.path?.substringAfterLast('/') ?: "Невідомий файл" }
    }

    // Лончер для выбора файла
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { fileUri ->
                isLoading = true
                errorMessage = null

                kotlin.runCatching {
                    context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                        val parser = ExcelParser()
                        val fileName = getFileName(fileUri)
                        val worksheetId = "worksheet_${System.currentTimeMillis()}_${fileName.hashCode()}"

                        val parsedConsumers = parser.parseWorkbook(inputStream, worksheetId)

                        if (parsedConsumers.isEmpty()) {
                            errorMessage = "Файл не містить даних споживачів"
                            isLoading = false
                        } else {
                            // Сохраняем в репозиторий
                            repository.addWorksheet(fileName, parsedConsumers)

                            // Показываем Toast
                            android.widget.Toast.makeText(
                                context,
                                "✅ Завантажено ${parsedConsumers.size} споживачів",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()

                            isLoading = false

                            // Возвращаемся к списку ведомостей
                            onImportComplete()
                        }
                    } ?: run {
                        errorMessage = "Не вдалося відкрити файл"
                        isLoading = false
                    }
                }.onFailure { e ->
                    errorMessage = "Помилка обробки файлу: ${e.message}"
                    e.printStackTrace()
                    isLoading = false
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📁 Імпорт Excel") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Завантаження файлу...")
            } else {
                Icon(
                    Icons.Filled.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Оберіть файл Excel для імпорту",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Файл повинен містити колонки:\n" +
                            "• Номер ОР\n• ПІБ\n• Телефон\n• Адреса\n• Заборгованість",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Icon(Icons.Filled.FileOpen, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("📁 Вибрати Excel файл")
                }

                // КНОПКА ДЛЯ ПРОВЕРКИ
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // Проверяем сколько ведомостей в репозитории
                        val count = repository.getAllWorksheets().size
                        val info = if (count > 0) {
                            "✅ В репозитории $count ведомость(ей)"
                        } else {
                            "❌ В репозитории 0 ведомостей"
                        }

                        android.widget.Toast.makeText(
                            context,
                            info,
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Filled.Info, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("🔍 Проверить репозиторий")
                }

                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Перечисление экранов приложения
sealed class AppScreen {
    object Worksheets : AppScreen()
    object ConsumerList : AppScreen()
    object ConsumerDetail : AppScreen()
    object ProcessConsumer : AppScreen()
    object ImportExcel : AppScreen()
}