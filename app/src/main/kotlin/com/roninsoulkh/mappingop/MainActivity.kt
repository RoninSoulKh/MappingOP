package com.roninsoulkh.mappingop

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler // <--- ВАЖНО: Для перехвата кнопки Назад
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

    // Данные ведомостей
    var worksheets by remember { mutableStateOf<List<Worksheet>>(emptyList()) }
    LaunchedEffect(Unit) {
        repository.getAllWorksheetsFlow().collect { worksheets = it }
    }

    // Данные потребителей выбранной ведомости
    var worksheetConsumers by remember { mutableStateOf<List<Consumer>>(emptyList()) }
    LaunchedEffect(selectedWorksheetId) {
        selectedWorksheetId?.let { worksheetId ->
            repository.getConsumersFlow(worksheetId).collect { worksheetConsumers = it }
        } ?: run { worksheetConsumers = emptyList() }
    }

    // Результат работы по выбранному потребителю
    var workResultForSelectedConsumer by remember { mutableStateOf<WorkResult?>(null) }
    // Следим за изменениями selectedConsumer, чтобы подгрузить результат
    LaunchedEffect(selectedConsumer, currentScreen) { // Добавил currentScreen в триггеры, чтобы обновлялось при возврате
        workResultForSelectedConsumer = selectedConsumer?.let { consumer ->
            repository.getWorkResultByConsumerId(consumer.id)
        }
    }

    when (currentScreen) {
        AppScreen.Worksheets -> {
            // На главном экране BackHandler не нужен (выход из приложения - стандартное поведение)
            WorksheetsScreen(
                worksheets = worksheets,
                onWorksheetClick = { worksheet ->
                    selectedWorksheetId = worksheet.id
                    currentScreen = AppScreen.ConsumerList
                },
                onAddWorksheet = { currentScreen = AppScreen.ImportExcel },
                onBackClick = { /* Системный выход */ },
                onViewResults = { currentScreen = AppScreen.WorkResults },
                onDeleteWorksheet = { worksheet ->
                    coroutineScope.launch {
                        repository.deleteWorksheet(worksheet)
                        android.widget.Toast.makeText(context, "Відомість видалено", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        AppScreen.ConsumerList -> {
            // Если нажали Назад в списке -> идем к Ведомостям
            BackHandler {
                selectedWorksheetId = null
                currentScreen = AppScreen.Worksheets
            }

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
            // Если нажали Назад в деталях -> идем к Списку
            BackHandler {
                selectedConsumer = null
                currentScreen = AppScreen.ConsumerList
            }

            if (selectedConsumer != null) {
                ConsumerDetailScreen(
                    consumer = selectedConsumer!!,
                    workResult = workResultForSelectedConsumer, // <--- ПЕРЕДАЕМ РЕЗУЛЬТАТ СЮДА
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
            // Если нажали Назад при заполнении -> идем к Деталям (без сохранения)
            BackHandler { currentScreen = AppScreen.ConsumerDetail }

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
                                        // ВАЖНО: Обновляем результат вручную, чтобы он сразу появился при возврате
                                        workResultForSelectedConsumer = result
                                        currentScreen = AppScreen.ConsumerDetail // Возвращаемся в детали
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
            BackHandler { currentScreen = AppScreen.Worksheets }

            ImportExcelScreen(
                repository = repository,
                context = context,
                onImportComplete = { currentScreen = AppScreen.Worksheets },
                onBackClick = { currentScreen = AppScreen.Worksheets }
            )
        }

        AppScreen.WorkResults -> {
            BackHandler { currentScreen = AppScreen.Worksheets }

            WorkResultsScreen(
                onBackClick = { currentScreen = AppScreen.Worksheets }
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
                            // Получаем имя файла (упрощенно)
                            val fileName = fileUri.path?.substringAfterLast('/') ?: "imported_file.xlsx"
                            val worksheetId = "worksheet_${System.currentTimeMillis()}_${fileName.hashCode()}"
                            val parsedConsumers = parser.parseWorkbook(inputStream, worksheetId)

                            if (parsedConsumers.isEmpty()) {
                                errorMessage = "Файл не містить даних споживачів"
                                isLoading = false
                            } else {
                                repository.addWorksheet(fileName, parsedConsumers)
                                isLoading = false
                                onImportComplete()
                            }
                        } ?: run { errorMessage = "Не вдалося відкрити файл"; isLoading = false }
                    }.onFailure { e ->
                        errorMessage = "Помилка: ${e.message}"
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
                    IconButton(onClick = onBackClick) { Icon(Icons.Filled.ArrowBack, "Назад") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
                Text("Завантаження...", Modifier.padding(top = 16.dp))
            } else {
                Button(onClick = { filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }) {
                    Icon(Icons.Filled.UploadFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Оберіть файл Excel")
                }
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp)) }
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