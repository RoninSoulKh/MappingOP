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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roninsoulkh.mappingop.data.parser.ExcelParser
import com.roninsoulkh.mappingop.domain.models.Consumer
import com.roninsoulkh.mappingop.ui.screens.*
import com.roninsoulkh.mappingop.domain.models.WorkResult
import com.roninsoulkh.mappingop.domain.models.Worksheet
import com.roninsoulkh.mappingop.ui.theme.MappingOPTheme
import com.roninsoulkh.mappingop.data.repository.AppRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.InputStream
import kotlinx.coroutines.async

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val repository by lazy { AppRepository(this) }

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
fun AppNavigation(repository: AppRepository) {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Worksheets) }
    var selectedWorksheetId by remember { mutableStateOf<String?>(null) }
    var selectedConsumer by remember { mutableStateOf<Consumer?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showSuccessToast by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var showErrorToast by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(showSuccessToast) {
        if (showSuccessToast) {
            android.widget.Toast.makeText(context, successMessage, android.widget.Toast.LENGTH_SHORT).show()
            showSuccessToast = false
        }
    }

    LaunchedEffect(showErrorToast) {
        if (showErrorToast) {
            android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
            showErrorToast = false
        }
    }

    var worksheets by remember { mutableStateOf<List<Worksheet>>(emptyList()) }

    LaunchedEffect(Unit) {
        repository.getAllWorksheetsFlow().collect { worksheetsList ->
        worksheets = worksheetsList
        }
    }

    var worksheetConsumers by remember { mutableStateOf<List<Consumer>>(emptyList()) }

    LaunchedEffect(selectedWorksheetId) {
        selectedWorksheetId?.let { worksheetId ->
            repository.getConsumersFlow(worksheetId).collect { consumers ->
                worksheetConsumers = consumers
            }
        } ?: run {
            worksheetConsumers = emptyList()
        }
    }

    var workResultForSelectedConsumer by remember { mutableStateOf<WorkResult?>(null) }

    LaunchedEffect(selectedConsumer) {
        workResultForSelectedConsumer = selectedConsumer?.let { consumer ->
            repository.getWorkResultByConsumerId(consumer.id)
        }
    }

    when (currentScreen) {
        AppScreen.Worksheets -> {
            WorksheetsScreen(
                worksheets = worksheets,
                onWorksheetClick = { worksheet ->
                    selectedWorksheetId = worksheet.id
                    coroutineScope.launch {
                        repository.getConsumersByWorksheetId(worksheet.id)
                    }
                    currentScreen = AppScreen.ConsumerList
                },
                onAddWorksheet = {
                    currentScreen = AppScreen.ImportExcel
                },
                onBackClick = { /* Выход из приложения */ },
                onViewResults = {  // НОВЫЙ ПАРАМЕТР
                    currentScreen = AppScreen.WorkResults
                }
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
                        val consumerToSave = selectedConsumer
                        if (consumerToSave != null) {
                            coroutineScope.launch {
                                try {
                                    repository.saveWorkResult(consumerToSave.id, result)

                                    withContext(Dispatchers.Main) {
                                        selectedConsumer = null
                                        currentScreen = AppScreen.ConsumerList
                                        successMessage = "✅ Дані збережено успішно!"
                                        showSuccessToast = true
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        errorMessage = "❌ Помилка збереження: ${e.message}"
                                        showErrorToast = true
                                    }
                                    e.printStackTrace()
                                }
                            }
                        } else {
                            errorMessage = "❌ Помилка: споживач не знайдений"
                            showErrorToast = true
                        }
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
                    currentScreen = AppScreen.Worksheets
                },
                onBackClick = {
                    currentScreen = AppScreen.Worksheets
                }
            )
        }

        AppScreen.WorkResults -> {
            WorkResultsScreen(
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
    repository: AppRepository,
    context: android.content.Context,
    onImportComplete: () -> Unit,
    onBackClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { fileUri ->
                isLoading = true
                errorMessage = null

                coroutineScope.launch {
                    kotlin.runCatching {
                        context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                            val parser = ExcelParser()
                            val fileName = getFileName(fileUri)

                            // ГЕНЕРИРУЕМ worksheetId ОДИН РАЗ
                            val worksheetId = "worksheet_${System.currentTimeMillis()}_${fileName.hashCode()}"

                            println("📱 MainActivity: начат импорт, fileName=$fileName, worksheetId=$worksheetId")

                            val parsedConsumers = parser.parseWorkbook(inputStream, worksheetId)

                            println("📱 MainActivity: спарсено ${parsedConsumers.size} потребителей")

                            if (parsedConsumers.isEmpty()) {
                                errorMessage = "Файл не містить даних споживачів"
                                isLoading = false
                            } else {
                                // Сохраняем в базу
                                repository.addWorksheet(fileName, parsedConsumers)

                                // ПРОВЕРКА: получаем данные обратно из базы
                                val savedConsumers = repository.getConsumersByWorksheetId(worksheetId)
                                println("📱 MainActivity: после сохранения, в базе ${savedConsumers.size} потребителей")

                                android.widget.Toast.makeText(
                                    context,
                                    "✅ Завантажено ${parsedConsumers.size} споживачів (в базе: ${savedConsumers.size})",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()

                                isLoading = false
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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // Запускаем корутину для вызова suspend-функции
                        coroutineScope.launch {
                            try {
                                // getAllWorksheets() - suspend функция, требует корутины
                                val count = repository.getAllWorksheets().size

                                val info = if (count > 0) {
                                    "✅ В репозитории $count ведомость(ей) (СОХРАНЯЕТСЯ НА ДИСК!)"
                                } else {
                                    "❌ В репозитории 0 ведомостей"
                                }

                                // Toast нужно показывать в основном потоке (UI)
                                withContext(Dispatchers.Main) {
                                    android.widget.Toast.makeText(
                                        context,
                                        info,
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                // Обработка ошибок
                                withContext(Dispatchers.Main) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "❌ Ошибка проверки: ${e.message}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Filled.Info, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("🔍 Проверить сохранение")
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

sealed class AppScreen {
    object Worksheets : AppScreen()
    object ConsumerList : AppScreen()
    object ConsumerDetail : AppScreen()
    object ProcessConsumer : AppScreen()
    object ImportExcel : AppScreen()
    object WorkResults : AppScreen()
}