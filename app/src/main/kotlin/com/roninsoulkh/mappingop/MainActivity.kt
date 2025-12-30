package com.roninsoulkh.mappingop

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

import com.roninsoulkh.mappingop.data.parser.ExcelParser
import com.roninsoulkh.mappingop.domain.models.Consumer
import com.roninsoulkh.mappingop.domain.models.WorkResult
import com.roninsoulkh.mappingop.domain.models.Worksheet
import com.roninsoulkh.mappingop.ui.screens.*
import com.roninsoulkh.mappingop.ui.theme.MappingOPTheme
import com.roninsoulkh.mappingop.data.repository.AppRepository
import com.roninsoulkh.mappingop.utils.AddressHelper
import com.roninsoulkh.mappingop.utils.SettingsManager
import com.roninsoulkh.mappingop.presentation.viewmodels.BatchGeocodingService

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val repository by lazy { AppRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        org.osmdroid.config.Configuration.getInstance().load(
            this,
            android.preference.PreferenceManager.getDefaultSharedPreferences(this)
        )
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var currentThemeSetting by remember { mutableStateOf(SettingsManager.getTheme(context)) }
            val useDarkTheme = when (currentThemeSetting) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as ComponentActivity).window
                    window.statusBarColor = Color.Transparent.toArgb()
                    window.navigationBarColor = Color.Transparent.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
                }
            }
            MappingOPTheme(darkTheme = useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        repository = repository,
                        currentTheme = currentThemeSetting,
                        onThemeChanged = { newTheme ->
                            SettingsManager.saveTheme(context, newTheme)
                            currentThemeSetting = newTheme
                        }
                    )
                }
            }
        }
    }

    companion object {
        init {
            System.setProperty("org.apache.poi.util.POILogger", "org.apache.poi.util.NullLogger")
        }
    }
}

// --- УМНЫЙ СБОРЩИК АДРЕСА (Для ручного поиска через AddressHelper) ---
fun constructSmartAddress(raw: String): String {
    val parts = raw.split(",").map { it.trim() }

    var zipCode = ""
    var settlement = ""
    var street = ""
    var houseNumber = ""
    var region = ""

    for (part in parts) {
        val lower = part.lowercase()
        if (part.matches(Regex("\\d{5}"))) { zipCode = part; continue }
        if (lower.contains("обл")) { region = part; continue }
        if (lower.contains("вул") || lower.contains("пров") || lower.contains("просп") ||
            lower.contains("в'їзд") || lower.contains("майдан") || lower.contains("бульвар") ||
            lower.contains("наб") || lower.contains("узвіз") || lower.contains("тупик")) {
            street = part
            continue
        }
        if (lower.contains("с.") || lower.contains("м.") || lower.contains("смт") || lower.contains("селище")) {
            settlement = part.replace("с.", "").replace("м.", "").replace("смт", "").replace("селище", "").trim()
            continue
        }
        if ((lower.contains("буд") || (part.isNotEmpty() && part[0].isDigit())) && part.length < 6) {
            houseNumber = part.replace("буд.", "").replace("буд", "").replace("кв.", "").trim()
            continue
        }
    }

    val addressParts = mutableListOf<String>()
    if (street.isNotEmpty()) addressParts.add(street)
    if (houseNumber.isNotEmpty()) addressParts.add(houseNumber)
    if (settlement.isNotEmpty()) addressParts.add(settlement)
    if (region.isNotEmpty()) addressParts.add(region)
    if (zipCode.isNotEmpty()) addressParts.add(zipCode)

    if (addressParts.size < 2) return raw
    return addressParts.joinToString(" ")
}

// ФУНКЦИЯ startMassGeocoding УДАЛЕНА - ОНА БОЛЬШЕ НЕ НУЖНА!

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: AppRepository,
    currentTheme: String,
    onThemeChanged: (String) -> Unit
) {
    var currentTab by remember { mutableStateOf(BottomTab.HOME) }
    var currentWorksheetsScreen by remember { mutableStateOf<AppScreen>(AppScreen.Worksheets) }

    var isNavigatedFromMap by remember { mutableStateOf(false) }

    var selectedWorksheetId by remember { mutableStateOf<String?>(null) }
    var selectedConsumer by remember { mutableStateOf<Consumer?>(null) }
    var selectedMapWorksheetId by remember { mutableStateOf<String?>(null) }

    // --- ПОДКЛЮЧЕНИЕ НОВОГО СЕРВИСА ГЕОКОДИНГА ---
    // Это заменяет старую логику с startMassGeocoding
    val geoService = remember { BatchGeocodingService(repository) }
    val isGeocoding by geoService.isGeocoding.collectAsState()
    val geoProgress by geoService.progress.collectAsState()
    val failedList by geoService.failedList.collectAsState()

    // Слушаем изменения в базе для карты
    val mapConsumersState = remember(selectedMapWorksheetId) {
        if (selectedMapWorksheetId != null) {
            repository.getConsumersFlow(selectedMapWorksheetId!!)
        } else {
            flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val mapConsumersList = mapConsumersState.value
    val totalConsumersCount = mapConsumersList.size
    val foundCoordinatesCount = mapConsumersList.count { it.latitude != null && it.latitude != 0.0 }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var launchImport by remember { mutableStateOf(false) }

    var showSuccessToast by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var showErrorToast by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(showSuccessToast) { if (showSuccessToast) { Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show(); showSuccessToast = false } }
    LaunchedEffect(showErrorToast) { if (showErrorToast) { Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show(); showErrorToast = false } }

    var worksheets by remember { mutableStateOf<List<Worksheet>>(emptyList()) }
    LaunchedEffect(Unit) { repository.getAllWorksheetsFlow().collect { worksheets = it } }

    var worksheetConsumers by remember { mutableStateOf<List<Consumer>>(emptyList()) }
    LaunchedEffect(selectedWorksheetId) {
        selectedWorksheetId?.let { id -> repository.getConsumersFlow(id).collect { worksheetConsumers = it } } ?: run { worksheetConsumers = emptyList() }
    }

    var workResultForSelectedConsumer by remember { mutableStateOf<WorkResult?>(null) }
    LaunchedEffect(selectedConsumer, currentWorksheetsScreen) {
        workResultForSelectedConsumer = selectedConsumer?.let { repository.getWorkResultByConsumerId(it.id) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { fileUri ->
            coroutineScope.launch {
                kotlin.runCatching {
                    context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                        val rawFileName = getFileNameFromUri(context, fileUri)
                        val fileName = rawFileName.replace(".xlsx", "", ignoreCase = true)
                        val worksheetId = "worksheet_${System.currentTimeMillis()}"
                        val consumers = ExcelParser().parseWorkbook(inputStream, worksheetId)
                        if (consumers.isNotEmpty()) {
                            repository.addWorksheet(fileName, consumers)
                            successMessage = "Файл завантажено."; showSuccessToast = true
                            currentTab = BottomTab.LIST
                            currentWorksheetsScreen = AppScreen.Worksheets
                        } else {
                            errorMessage = "Файл пустий"; showErrorToast = true
                        }
                    }
                }.onFailure { errorMessage = "Помилка: ${it.message}"; showErrorToast = true }
            }
        }
        launchImport = false
    }

    if (launchImport) {
        LaunchedEffect(Unit) { filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }
    }

    // --- ДИАЛОГ С ОШИБКАМИ ГЕОКОДИНГА ---
    if (failedList.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { geoService.clearErrors() },
            title = { Text("Звіт пошуку") },
            text = {
                Column {
                    Text("Не знайдено адрес: ${failedList.size}")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(failedList) { addr ->
                            Text("- $addr", fontSize = 12.sp)
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { geoService.clearErrors() }) { Text("OK") }
            }
        )
    }

    Scaffold(
        floatingActionButtonPosition = FabPosition.Start,
        bottomBar = {
            if (currentTab != BottomTab.LIST || (currentWorksheetsScreen != AppScreen.ConsumerDetail && currentWorksheetsScreen != AppScreen.ProcessConsumer && currentWorksheetsScreen !is AppScreen.EditLocation)) {
                NavigationBar {
                    BottomTab.values().forEach { tab ->
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            selected = currentTab == tab,
                            onClick = { currentTab = tab }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentTab == BottomTab.LIST && currentWorksheetsScreen == AppScreen.ConsumerDetail) {
                FloatingActionButton(
                    modifier = Modifier.padding(16.dp),
                    onClick = {
                        selectedConsumer?.let { consumer ->
                            Toast.makeText(context, "Пошук...", Toast.LENGTH_SHORT).show()
                            val smartAddress = constructSmartAddress(consumer.rawAddress)
                            AddressHelper.searchAddress(smartAddress) { location ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    if (location == null) {
                                        errorMessage = "Не знайдено!"; showErrorToast = true
                                    } else {
                                        consumer.latitude = location.lat
                                        consumer.longitude = location.lng
                                        repository.updateConsumer(consumer)
                                        successMessage = "Знайдено!"; showSuccessToast = true
                                    }
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) { Icon(Icons.Filled.LocationSearching, contentDescription = null) }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            AnimatedContent(targetState = currentTab, label = "TabAnim") { targetTab ->
                when (targetTab) {
                    BottomTab.HOME -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Mapping OP", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(48.dp))
                            Button(
                                onClick = { launchImport = true },
                                modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Завантажити Excel", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    currentTab = BottomTab.LIST
                                    currentWorksheetsScreen = AppScreen.WorkResults
                                },
                                modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.List, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Звіти та Експорт", fontSize = 18.sp)
                            }
                        }
                    }

                    BottomTab.MAP -> {
                        MapScreen(
                            worksheets = worksheets,
                            consumers = mapConsumersList,
                            totalCount = totalConsumersCount,
                            foundCount = foundCoordinatesCount,
                            onWorksheetSelected = { worksheet ->
                                selectedMapWorksheetId = worksheet.id
                                // ЗАПУСКАЕМ СЕРВИС ВМЕСТО СТАРОЙ ФУНКЦИИ
                                geoService.startForWorksheet(worksheet.id)
                                Toast.makeText(context, "Запуск пошуку...", Toast.LENGTH_SHORT).show()
                            },
                            onConsumerClick = { consumer ->
                                selectedConsumer = consumer
                                isNavigatedFromMap = true
                                currentTab = BottomTab.LIST
                                currentWorksheetsScreen = AppScreen.ConsumerDetail
                            },
                            // Передаем состояние прогресса в карту
                            isGeocoding = isGeocoding,
                            progress = geoProgress
                        )
                    }

                    BottomTab.PROFILE -> ProfileScreen(currentTheme = currentTheme, onThemeSelected = onThemeChanged)

                    BottomTab.LIST -> {
                        AnimatedContent(targetState = currentWorksheetsScreen, label = "ListAnim") { targetScreen ->
                            when (targetScreen) {
                                AppScreen.Worksheets -> WorksheetsScreen(
                                    worksheets = worksheets,
                                    onWorksheetClick = { ws ->
                                        selectedWorksheetId = ws.id
                                        isNavigatedFromMap = false
                                        currentWorksheetsScreen = AppScreen.ConsumerList
                                    },
                                    onAddWorksheet = { launchImport = true },
                                    onDeleteWorksheet = { ws -> coroutineScope.launch { repository.deleteWorksheet(ws) } },
                                    onRenameWorksheet = { ws, name -> coroutineScope.launch { repository.renameWorksheet(ws, name) } },
                                    onViewResults = { currentWorksheetsScreen = AppScreen.WorkResults },
                                    onBackClick = {}
                                )
                                AppScreen.ConsumerList -> ConsumerListScreen(
                                    consumers = worksheetConsumers,
                                    onConsumerClick = { c ->
                                        selectedConsumer = c
                                        isNavigatedFromMap = false
                                        currentWorksheetsScreen = AppScreen.ConsumerDetail
                                    },
                                    onBackClick = { currentWorksheetsScreen = AppScreen.Worksheets }
                                )
                                AppScreen.ConsumerDetail -> ConsumerDetailScreen(
                                    consumer = selectedConsumer!!,
                                    workResult = workResultForSelectedConsumer,
                                    onBackClick = {
                                        if (isNavigatedFromMap) {
                                            currentTab = BottomTab.MAP
                                        } else {
                                            currentWorksheetsScreen = AppScreen.ConsumerList
                                        }
                                    },
                                    onProcessClick = { currentWorksheetsScreen = AppScreen.ProcessConsumer },
                                    onManualLocationClick = {
                                        currentWorksheetsScreen = AppScreen.EditLocation(selectedConsumer!!)
                                    }
                                )
                                is AppScreen.EditLocation -> EditLocationScreen(
                                    consumer = targetScreen.consumer,
                                    onSave = { lat, lng ->
                                        val c = targetScreen.consumer
                                        c.latitude = lat
                                        c.longitude = lng
                                        coroutineScope.launch {
                                            repository.updateConsumer(c)
                                            successMessage = "Координати змінено вручну"; showSuccessToast = true
                                            currentWorksheetsScreen = AppScreen.ConsumerDetail
                                        }
                                    },
                                    onCancel = { currentWorksheetsScreen = AppScreen.ConsumerDetail }
                                )
                                AppScreen.ProcessConsumer -> ProcessConsumerScreen(
                                    consumer = selectedConsumer!!,
                                    initialResult = workResultForSelectedConsumer,
                                    onSave = { result ->
                                        coroutineScope.launch {
                                            repository.saveWorkResult(selectedConsumer!!.id, result)
                                            withContext(Dispatchers.Main) {
                                                workResultForSelectedConsumer = result
                                                currentWorksheetsScreen = AppScreen.ConsumerDetail
                                                successMessage = "Збережено"; showSuccessToast = true
                                            }
                                        }
                                    },
                                    onCancel = { currentWorksheetsScreen = AppScreen.ConsumerDetail }
                                )
                                AppScreen.WorkResults -> WorkResultsScreen(onBackClick = { currentWorksheetsScreen = AppScreen.Worksheets })
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = it.getString(index)
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1 && cut != null) result = result?.substring(cut + 1)
    }
    return result ?: "imported_file.xlsx"
}

sealed class AppScreen {
    object Worksheets : AppScreen()
    object ConsumerList : AppScreen()
    object ConsumerDetail : AppScreen()
    object ProcessConsumer : AppScreen()
    object ImportExcel : AppScreen()
    object WorkResults : AppScreen()
    data class EditLocation(val consumer: Consumer) : AppScreen()
}

enum class BottomTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Головна", Icons.Filled.Home),
    LIST("Відомості", Icons.Filled.List),
    MAP("Мапа", Icons.Filled.Map),
    PROFILE("Профіль", Icons.Filled.Person)
}